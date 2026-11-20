package com.Lia.assistant.forge

import com.Lia.assistant.voice.GeminiTextClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Streams a website from the Gemini REST streamGenerateContent endpoint.
 *
 * - Uses the same model resolution as [GeminiTextClient] (flash, no live/image/tts).
 * - For model names containing "2.5", sets thinkingBudget=0 to avoid a ~60 s cold start.
 * - One automatic retry on 503/UNAVAILABLE after 4 s.
 * - One automatic retry with an explicit completion hint when output is < 1500 chars
 *   or ends without </html>, and the current length < 14000 chars (otherwise accept it).
 *
 * Emits [ForgeChunk] values; the collector cancels the request on coroutine cancellation.
 */
object WebsiteForgeClient {

    sealed class ForgeChunk {
        data class Text(val delta: String)             : ForgeChunk()
        data class Done(val finishReason: String)      : ForgeChunk()
        data class Error(val message: String)          : ForgeChunk()
    }

    private const val BASE           = "https://generativelanguage.googleapis.com/v1beta"
    private const val OVERLOAD_DELAY = 4_000L

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    /**
     * Streams a website for [systemInstruction] + [userPrompt].
     *
     * @param apiKey            Gemini API key.
     * @param systemInstruction Built by [ForgeSystemPrompt].
     * @param userPrompt        The raw user request (passed in the user turn).
     * @param retryHint         When non-null, appended to the user turn (used on auto-retry).
     */
    fun stream(
        apiKey            : String,
        systemInstruction : String,
        userPrompt        : String,
        retryHint         : String? = null,
    ): Flow<ForgeChunk> = flow {
        if (apiKey.isBlank()) {
            emit(ForgeChunk.Error("No Gemini API key — add one in Settings."))
            return@flow
        }

        val modelId = runCatching { GeminiTextClient.resolveModel(apiKey) }.getOrElse {
            emit(ForgeChunk.Error("Couldn't fetch model list: ${it.message}"))
            return@flow
        }

        val url  = "$BASE/models/$modelId:streamGenerateContent?alt=sse&key=$apiKey"
        val body = buildRequestBody(systemInstruction, userPrompt, retryHint, modelId)

        var attempt = 0
        while (attempt < 2) {
            attempt++
            val request = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(body)
                .build()

            val call     = http.newCall(request)
            val response = try {
                call.execute()
            } catch (e: IOException) {
                emit(ForgeChunk.Error("Network error: ${e.message}"))
                return@flow
            }

            if (response.code == 503 && attempt < 2) {
                response.close()
                delay(OVERLOAD_DELAY)
                continue
            }

            if (!response.isSuccessful) {
                val reason = runCatching {
                    JSONObject(response.body?.string() ?: "")
                        .optJSONObject("error")?.optString("message", "") ?: ""
                }.getOrDefault("")
                response.close()
                emit(ForgeChunk.Error(if (reason.isNotBlank()) reason else "HTTP ${response.code}"))
                return@flow
            }

            // Stream SSE lines
            response.body?.source()?.let { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    if (SseParser.isDone(line)) break

                    SseParser.extractError(line)?.let { err ->
                        emit(ForgeChunk.Error(err)); return@flow
                    }
                    SseParser.extractText(line)?.let { text ->
                        emit(ForgeChunk.Text(text))
                    }
                    SseParser.extractFinishReason(line)?.let { reason ->
                        if (reason != "STOP") emit(ForgeChunk.Done(reason))
                        else emit(ForgeChunk.Done("STOP"))
                    }
                }
            }
            break  // success
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildRequestBody(
        systemInstruction: String,
        userPrompt       : String,
        retryHint        : String?,
        modelId          : String,
    ): okhttp3.RequestBody {
        val userContent = if (retryHint != null) "$userPrompt\n\n$retryHint" else userPrompt

        val genConfig = JSONObject().apply {
            put("maxOutputTokens", 8192)
            put("temperature", 1.0)
            // Suppress thinking budget for 2.5 models — first token delay is ~60 s otherwise.
            if ("2.5" in modelId) {
                put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
            }
        }

        return JSONObject()
            .put("systemInstruction", JSONObject()
                .put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
            .put("contents", JSONArray()
                .put(JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", userContent)))))
            .put("generationConfig", genConfig)
            .toString()
            .toRequestBody("application/json".toMediaType())
    }
}
