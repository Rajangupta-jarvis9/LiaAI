package com.Lia.assistant.voice

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Gemini REST client for text-chat replies.
 *
 * ── Model discovery ───────────────────────────────────────────────────────────
 * We never hardcode a model ID.  [resolveModel] fetches the live model list and
 * picks the first model that:
 *   1. Has "generateContent" in supportedGenerationMethods.
 *   2. Has "flash" in its name (fast, low-cost).
 *   3. Does NOT have "live", "image" or "tts" in its name.
 * Falls back to the first generateContent model if no flash model exists.
 * The result is cached for the process lifetime.
 *
 * ── What is NOT logged ────────────────────────────────────────────────────────
 * - The API key (never).
 * - The user's message text (privacy).
 * - The assistant's reply text (privacy).
 * Only HTTP status codes and Gemini's own error reason strings are logged.
 */
object GeminiTextClient {

    sealed class ChatReplyResult {
        data class Success(val text: String)  : ChatReplyResult()
        data class Error(val message: String) : ChatReplyResult()
    }

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta"

    @Volatile private var cachedModel: String? = null

    internal val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Model resolution ──────────────────────────────────────────────────────

    /**
     * Returns the model ID to use for generateContent calls.
     * Result is cached after the first successful call.
     *
     * @throws IOException on network failure (callers handle this).
     */
    internal fun resolveModel(apiKey: String): String {
        cachedModel?.let { return it }

        val url     = "$BASE/models?key=$apiKey"
        val request = Request.Builder().url(url).get().build()
        val body    = httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Models list HTTP ${resp.code}")
            resp.body?.string() ?: throw IOException("Empty models response")
        }

        val models = JSONObject(body).optJSONArray("models") ?: JSONArray()

        var flashModel    : String? = null
        var fallbackModel : String? = null

        for (i in 0 until models.length()) {
            val m       = models.getJSONObject(i)
            val name    = m.optString("name", "")        // e.g. "models/gemini-2.0-flash"
            val methods = m.optJSONArray("supportedGenerationMethods") ?: continue

            val supportsGenerate = (0 until methods.length()).any { methods.getString(it) == "generateContent" }
            if (!supportsGenerate) continue

            if (fallbackModel == null) fallbackModel = name

            val lower = name.lowercase()
            if ("flash" in lower && "live" !in lower && "image" !in lower && "tts" !in lower) {
                flashModel = name
                break
            }
        }

        val chosen = flashModel ?: fallbackModel
            ?: throw IOException("No suitable generateContent model found")

        // Strip "models/" prefix if present — the REST URL already includes it.
        val modelId = chosen.removePrefix("models/")
        cachedModel = modelId
        return modelId
    }

    // ── Chat reply ────────────────────────────────────────────────────────────

    /**
     * Sends [history] to Gemini and returns the assistant's reply.
     *
     * @param apiKey            User's Gemini API key.
     * @param systemInstruction Built by [ChatPromptBuilder].
     * @param history           Full conversation so far; must contain at least one USER turn.
     */
    fun reply(
        apiKey            : String,
        systemInstruction : String,
        history           : List<ChatTurn>,
    ): ChatReplyResult {
        if (apiKey.isBlank()) {
            return ChatReplyResult.Error("No Gemini API key yet — add one in Settings.")
        }
        if (history.isEmpty()) {
            return ChatReplyResult.Error("History is empty.")
        }

        return try {
            val modelId = resolveModel(apiKey)
            val url     = "$BASE/models/$modelId:generateContent?key=$apiKey"

            val contents = JSONArray()
            history.forEach { turn ->
                contents.put(JSONObject()
                    .put("role", if (turn.isUser) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", turn.text)))
                )
            }

            val body = JSONObject()
                .put("systemInstruction", JSONObject()
                    .put("parts", JSONArray()
                        .put(JSONObject().put("text", systemInstruction))
                    )
                )
                .put("contents", contents)
                .toString()
                .toRequestBody(JSON_TYPE)

            val request  = Request.Builder().url(url).post(body).build()
            val respBody = httpClient.newCall(request).execute().use { resp ->
                val raw = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    val reason = runCatching {
                        JSONObject(raw).optJSONObject("error")?.optString("message", "")
                    }.getOrNull() ?: ""
                    return ChatReplyResult.Error("HTTP ${resp.code}${if (reason.isNotBlank()) ": $reason" else ""}")
                }
                raw
            }

            val parsed   = JSONObject(respBody)
            val candidate = parsed
                .optJSONArray("candidates")
                ?.optJSONObject(0)

            // Blocked or empty candidate.
            val finishReason = candidate?.optString("finishReason", "") ?: ""
            if (finishReason == "SAFETY" || finishReason == "RECITATION") {
                return ChatReplyResult.Error("Reply was blocked (${finishReason}).")
            }

            val text = candidate
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text", "")
                ?.trim()

            if (text.isNullOrBlank()) {
                ChatReplyResult.Error("Gemini returned an empty reply.")
            } else {
                ChatReplyResult.Success(text)
            }

        } catch (e: IOException) {
            ChatReplyResult.Error("Couldn't reach Gemini. Check your internet connection.")
        } catch (e: Exception) {
            ChatReplyResult.Error("Unexpected error. Please try again.")
        }
    }
}
