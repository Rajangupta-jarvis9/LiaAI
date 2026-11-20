package com.Lia.assistant.forge

import android.content.Context
import com.Lia.assistant.data.ApiKeyStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Process-wide forge orchestrator.
 *
 * Replaces the stub in LiaFxComponents.  VoiceSessionManager and ChatViewModel
 * call [requestOpen]; the nav host observes [pendingOpen].
 */
object ForgeController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── UI state ──────────────────────────────────────────────────────────────

    private val _state = MutableStateFlow(ForgeUiState())
    val state: StateFlow<ForgeUiState> = _state

    private val _pendingOpen = MutableStateFlow<String?>(null)
    val pendingOpen: StateFlow<String?> = _pendingOpen

    fun consume() { _pendingOpen.value = null }

    // ── Public API ────────────────────────────────────────────────────────────

    fun requestOpen(description: String) {
        _pendingOpen.value = description
    }

    fun start(context: Context, prompt: String): StartResult {
        if (_state.value.isBuilding) return StartResult.Busy
        val apiKey = ApiKeyStore.getKey(context)
        if (apiKey.isBlank()) return StartResult.MissingKey("Add a Gemini API key in Settings.")
        val buildId = UUID.randomUUID().toString().take(8)
        _state.value = ForgeUiState(phase = ForgePhase.BUILDING, prompt = prompt, buildId = buildId)
        scope.launch { runBuild(context, apiKey, prompt, buildId) }
        return StartResult.Started
    }

    fun edit(context: Context, change: String): StartResult {
        val current = _state.value.file?.readText() ?: return StartResult.NothingToEdit
        val apiKey  = ApiKeyStore.getKey(context)
        if (apiKey.isBlank()) return StartResult.MissingKey("Add a Gemini API key in Settings.")
        val buildId = UUID.randomUUID().toString().take(8)
        _state.update { it.copy(phase = ForgePhase.BUILDING, isEdit = true, buildId = buildId,
                                prompt = change, error = null, partialHtml = current) }
        scope.launch {
            val instruction = ForgeSystemPrompt.forEdit(current, change)
            runBuild(context, apiKey, change, buildId, systemInstruction = instruction)
        }
        return StartResult.Started
    }

    fun retry(context: Context): StartResult {
        val prompt = _state.value.prompt.takeIf { it.isNotBlank() } ?: return StartResult.NothingToEdit
        return start(context, prompt)
    }

    fun cancel() {
        buildJob?.cancel()
        _state.update { it.copy(phase = ForgePhase.CANCELLED, error = "Cancelled.") }
    }

    fun reset() {
        buildJob?.cancel()
        _state.value = ForgeUiState()
    }

    fun openSaved(context: Context, file: File) {
        _state.value = ForgeUiState(
            phase       = ForgePhase.DONE,
            prompt      = file.name,
            file        = file,
            partialHtml = runCatching { file.readText() }.getOrDefault(""),
        )
        _pendingOpen.value = file.absolutePath
    }

    fun partialHtml(): String = _state.value.partialHtml

    // ── Private: build loop ───────────────────────────────────────────────────

    private var buildJob: Job? = null

    private suspend fun runBuild(
        context          : Context,
        apiKey           : String,
        userPrompt       : String,
        buildId          : String,
        systemInstruction: String = ForgeSystemPrompt.forNewSite(userPrompt),
    ) {
        buildJob = currentCoroutineContext()[Job]

        val startMs  = System.currentTimeMillis()
        val html     = StringBuilder()
        var retried  = false
        var sections = 0
        var lastEmitMs = 0L

        fun elapsedSec()  = (System.currentTimeMillis() - startMs) / 1000f
        fun tokensPerSec(): Float {
            val elapsed = elapsedSec().coerceAtLeast(0.01f)
            return (html.length / 4f) / elapsed   // rough token estimate (1 token ≈ 4 chars)
        }

        WebsiteForgeClient.stream(apiKey, systemInstruction, userPrompt).collect { chunk ->
            if (_state.value.isCancelled) return@collect

            when (chunk) {
                is WebsiteForgeClient.ForgeChunk.Text -> {
                    html.append(chunk.delta)
                    val stage    = ForgeStageDetector.detect(html.toString())
                    val nowMs    = System.currentTimeMillis()

                    // Count visible sections
                    sections = Regex("""data-section""").findAll(html).count()

                    // Throttle state emissions to every 120 ms
                    if (nowMs - lastEmitMs >= 120L) {
                        lastEmitMs = nowMs
                        _state.update { it.copy(
                            partialHtml = html.toString(),
                            stage       = stage,
                            progress    = stage.progressFraction,
                            metrics     = ForgeMetrics(
                                chars        = html.length,
                                sections     = sections,
                                elapsedSec   = elapsedSec(),
                                tokensPerSec = tokensPerSec(),
                            ),
                            log = (it.log + "Stage: ${stage.displayName} — ${html.length} chars").takeLast(20),
                            logSeq = it.logSeq + 1,
                        ) }
                    }
                }

                is WebsiteForgeClient.ForgeChunk.Done -> {
                    val raw     = ForgeHtml.stripFences(html.toString())
                    val finish  = chunk.finishReason

                    // Retry logic
                    if (!retried && (ForgeHtml.isLikelyTruncated(raw) || finish != "STOP")) {
                        if (!ForgeHtml.isAcceptable(raw)) {
                            // Too short — retry with completion hint
                            retried = true
                            html.clear()
                            runBuild(context, apiKey, userPrompt, buildId,
                                systemInstruction = systemInstruction +
                                "\n\nIMPORTANT: write completely original markup and code; finish the ENTIRE page; end with </html>.")
                            return@collect
                        }
                        // Long enough — repair and accept
                    }

                    val finalHtml = ForgeHtml.withSafetyNet(
                        if (ForgeHtml.isLikelyTruncated(raw) && ForgeHtml.isAcceptable(raw))
                            ForgeHtml.repairTruncated(raw)
                        else raw
                    )

                    val file = saveWebsite(context, finalHtml, buildId)
                    _state.update { it.copy(
                        phase       = ForgePhase.DONE,
                        partialHtml = finalHtml,
                        file        = file,
                        progress    = 1f,
                        metrics     = ForgeMetrics(
                            chars        = finalHtml.length,
                            sections     = sections,
                            elapsedSec   = elapsedSec(),
                            tokensPerSec = tokensPerSec(),
                        ),
                    ) }
                }

                is WebsiteForgeClient.ForgeChunk.Error -> {
                    val friendly = mapError(chunk.message)
                    _state.update { it.copy(phase = ForgePhase.FAILED, error = friendly) }
                }
            }
        }
    }

    private fun saveWebsite(context: Context, html: String, buildId: String): File {
        val dir  = File(context.filesDir, "Websites").also { it.mkdirs() }
        val ts   = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "website_${ts}_$buildId.html")
        file.writeText(html)
        return file
    }

    private fun mapError(raw: String): String = when {
        "503"        in raw || "overloaded" in raw.lowercase() ->
            "Gemini is busy right now. Tap Retry in a moment."
        "quota"      in raw.lowercase() ->
            "API quota exceeded. Check your Gemini quota and try again."
        "api key"    in raw.lowercase() ->
            "Invalid API key. Check your key in Settings."
        "network"    in raw.lowercase() || "connect" in raw.lowercase() ->
            "No internet connection."
        else -> raw.take(120)
    }
}
