package com.Lia.assistant.voice

import android.util.Base64
import com.Lia.assistant.action.LiaToolCatalog
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Full-duplex WebSocket client for the Gemini Live API.
 *
 * ── Responsibilities ──────────────────────────────────────────────────────────
 * - Opens and tears down a WebSocket with correct OkHttp settings.
 * - Sends the BidiGenerateContent setup message (model, voice, system instruction, tools).
 * - Streams 16 kHz PCM16 mic chunks to the API as base64-encoded realtimeInput.
 * - Receives both text and binary frames; parses setup, audio, transcripts,
 *   tool calls and session events.
 * - Deduplicates tool-call IDs via [ToolCallDeduplicator].
 *
 * ── Thread model ─────────────────────────────────────────────────────────────
 * OkHttp delivers all WebSocket callbacks on its own dispatcher threads.
 * Callers must dispatch to the right thread themselves (e.g. VoiceSessionManager
 * posts audio work to a single-thread executor).
 *
 * ── What NOT to do here ──────────────────────────────────────────────────────
 * - Do NOT log raw server frames.
 * - Do NOT log or print the API key.
 * - Do NOT set realtimeInputConfig.automaticActivityDetection (causes silent sessions).
 */
class GeminiLiveClient(private val listener: Listener) {

    // ── Public interface ──────────────────────────────────────────────────────

    interface Listener {
        /** Setup complete — the session is ready to accept audio. */
        fun onSetupComplete()

        /** 24 kHz PCM16 audio bytes from Gemini to play on the speaker. */
        fun onAudioChunk(bytes: ByteArray)

        /** Real-time transcript of what the user said. */
        fun onInputTranscript(text: String)

        /** Real-time transcript of what Lia is saying. */
        fun onOutputTranscript(text: String)

        /** Gemini wants to call a tool.  Call [sendToolResponse] after executing it. */
        fun onToolCall(id: String, name: String, args: JSONObject)

        /** Gemini finished its current turn. */
        fun onTurnComplete()

        /** User barged in; Gemini stopped speaking. */
        fun onInterrupted()

        /** WebSocket error — message is safe to log. */
        fun onError(message: String)

        /** WebSocket closed (code 1000 = clean, anything else = unexpected). */
        fun onClosed()
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        /**
         * Current native-audio Live model.
         * Update this ONE constant when Google releases a newer Live model.
         */
        const val MODEL  = "models/gemini-2.0-flash-live-001"
        private const val WS_BASE =
            "wss://generativelanguage.googleapis.com/ws/" +
            "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private var webSocket  : WebSocket?           = null
    private val dedup       = ToolCallDeduplicator()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)          // long-lived streaming connection
        .connectTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Opens the WebSocket and sends the BidiGenerateContent setup frame.
     *
     * @param systemInstruction  Full system prompt from [PersonalityPromptBuilder].
     * @param voiceName          Gemini voice name (default "Aoede").
     * @param apiKey             User's Gemini API key from [ApiKeyStore].
     */
    fun connect(
        systemInstruction: String,
        voiceName        : String = "Aoede",
        apiKey           : String,
    ) {
        dedup.reset()

        val url = "$WS_BASE?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                ws.send(buildSetupFrame(systemInstruction, voiceName).toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleFrame(text)
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                // Gemini may deliver frames as binary; decode to UTF-8 and handle normally.
                handleFrame(bytes.utf8())
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                listener.onError(t.message ?: "WebSocket failure")
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                listener.onClosed()
            }
        })
    }

    /**
     * Sends a 16 kHz PCM16 mono chunk to Gemini.
     *
     * [pcmBytes] must be raw little-endian 16-bit signed samples at 16 000 Hz.
     * Do NOT call this on the main thread — use a dedicated audio thread.
     */
    fun sendAudio(pcmBytes: ByteArray) {
        val b64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
        val frame = JSONObject()
            .put("realtimeInput", JSONObject()
                .put("audio", JSONObject()
                    .put("mimeType", "audio/pcm;rate=16000")
                    .put("data", b64)
                )
            )
        webSocket?.send(frame.toString())
    }

    /**
     * Sends a tool response back to Gemini after a tool has been executed.
     *
     * @param callId      The id from [Listener.onToolCall].
     * @param name        The function name from [Listener.onToolCall].
     * @param resultJson  JSON object describing the result (e.g. `{"status":"ok"}`).
     */
    fun sendToolResponse(callId: String, name: String, resultJson: JSONObject) {
        val frame = JSONObject()
            .put("toolResponse", JSONObject()
                .put("functionResponses", JSONArray()
                    .put(JSONObject()
                        .put("id",       callId)
                        .put("name",     name)
                        .put("response", resultJson)
                    )
                )
            )
        webSocket?.send(frame.toString())
    }

    /** Closes the WebSocket with a clean close code (1000). */
    fun disconnect() {
        webSocket?.close(1000, "session ended")
        webSocket = null
    }

    // ── Private: frame builder ────────────────────────────────────────────────

    private fun buildSetupFrame(systemInstruction: String, voiceName: String): JSONObject {
        val toolDeclarations = LiaToolCatalog.declarations()

        // Wrap each declaration so every param type is STRING and the outer
        // parameters wrapper is type OBJECT — required by the Live API schema.
        val wrappedDeclarations = JSONArray()
        for (i in 0 until toolDeclarations.length()) {
            wrappedDeclarations.put(toolDeclarations.getJSONObject(i))
        }

        val setup = JSONObject()
            .put("model", MODEL)
            .put("generationConfig", JSONObject()
                .put("responseModalities", JSONArray().put("AUDIO"))
                .put("speechConfig", JSONObject()
                    .put("voiceConfig", JSONObject()
                        .put("prebuiltVoiceConfig", JSONObject()
                            .put("voiceName", voiceName)
                        )
                    )
                )
            )
            .put("systemInstruction", JSONObject()
                .put("parts", JSONArray()
                    .put(JSONObject().put("text", systemInstruction))
                )
            )
            .put("inputAudioTranscription",  JSONObject())
            .put("outputAudioTranscription", JSONObject())

        if (wrappedDeclarations.length() > 0) {
            setup.put("tools", JSONArray()
                .put(JSONObject().put("functionDeclarations", wrappedDeclarations))
            )
        }

        // NOTE: do NOT set realtimeInputConfig.automaticActivityDetection.
        // Enabling it causes silent sessions where the model never responds.

        return JSONObject().put("setup", setup)
    }

    // ── Private: frame parser ─────────────────────────────────────────────────

    private fun handleFrame(raw: String) {
        // Parse errors are silently swallowed — a malformed frame should not
        // crash the session.  Unknown top-level keys are ignored (e.g.
        // sessionResumptionUpdate, goAway).
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return

        when {
            root.has("setupComplete")   -> listener.onSetupComplete()
            root.has("toolCall")        -> handleToolCall(root.getJSONObject("toolCall"))
            root.has("serverContent")   -> handleServerContent(root.getJSONObject("serverContent"))
        }
        // sessionResumptionUpdate and goAway are intentionally ignored.
    }

    private fun handleToolCall(toolCall: JSONObject) {
        val calls = toolCall.optJSONArray("functionCalls") ?: return
        for (i in 0 until calls.length()) {
            val call = calls.getJSONObject(i)
            val id   = call.optString("id",   "")
            val name = call.optString("name", "")
            val args = call.optJSONObject("args") ?: JSONObject()

            if (id.isBlank() || name.isBlank()) continue
            if (dedup.isDuplicate(id)) continue     // seen before — skip
            listener.onToolCall(id, name, args)
        }
    }

    private fun handleServerContent(serverContent: JSONObject) {
        // ── Input transcription (what the user said) ──────────────────────────
        serverContent.optJSONObject("inputTranscription")
            ?.optString("text")
            ?.takeIf { it.isNotBlank() }
            ?.let { listener.onInputTranscript(it) }

        // ── Output transcription (what Lia is saying) ─────────────────────────
        serverContent.optString("outputTranscription")
            .takeIf { it.isNotBlank() }
            ?.let { listener.onOutputTranscript(it) }

        // ── Model turn: audio chunks and inline transcripts ───────────────────
        serverContent.optJSONObject("modelTurn")
            ?.optJSONArray("parts")
            ?.let { parts ->
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)

                    // Audio inline data — must be 24 kHz PCM16
                    part.optJSONObject("inlineData")?.let { inlineData ->
                        val mime = inlineData.optString("mimeType", "")
                        val data = inlineData.optString("data",     "")
                        if (mime.startsWith("audio/pcm") && data.isNotBlank()) {
                            val pcm = runCatching {
                                Base64.decode(data, Base64.DEFAULT)
                            }.getOrNull()
                            if (pcm != null && pcm.isNotEmpty()) {
                                listener.onAudioChunk(pcm)
                            }
                        }
                    }

                    // Inline text transcript within the model turn
                    part.optString("text")
                        .takeIf { it.isNotBlank() }
                        ?.let { listener.onOutputTranscript(it) }
                }
            }

        // ── Turn / interruption signals ───────────────────────────────────────
        if (serverContent.optBoolean("turnComplete",  false)) listener.onTurnComplete()
        if (serverContent.optBoolean("interrupted",   false)) listener.onInterrupted()
    }
}
