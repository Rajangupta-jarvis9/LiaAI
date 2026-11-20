package com.Lia.assistant.forge

import org.json.JSONObject

/**
 * Parses individual SSE "data:" lines from the Gemini streamGenerateContent endpoint.
 *
 * Each line is a self-contained JSON object.  The parser is stateless and
 * safe to call from any thread.
 */
object SseParser {

    /** Extracts concatenated text from all parts in a streamed candidate. */
    fun extractText(dataLine: String): String? {
        val raw = dataLine.removePrefix("data:").trim()
        if (raw == "[DONE]" || raw.isBlank()) return null
        return runCatching {
            val root = JSONObject(raw)
            val parts = root
                .optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?: return@runCatching null
            buildString {
                for (i in 0 until parts.length()) {
                    val t = parts.optJSONObject(i)?.optString("text", "")
                    if (!t.isNullOrEmpty()) append(t)
                }
            }.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    /** Extracts finishReason from the candidate (e.g. "STOP", "RECITATION", "MAX_TOKENS"). */
    fun extractFinishReason(dataLine: String): String? {
        val raw = dataLine.removePrefix("data:").trim()
        if (raw == "[DONE]" || raw.isBlank()) return null
        return runCatching {
            JSONObject(raw)
                .optJSONArray("candidates")?.optJSONObject(0)
                ?.optString("finishReason")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    /** Extracts a human-readable error message from a Gemini error payload. */
    fun extractError(dataLine: String): String? {
        val raw = dataLine.removePrefix("data:").trim()
        if (raw == "[DONE]" || raw.isBlank()) return null
        return runCatching {
            JSONObject(raw).optJSONObject("error")?.optString("message")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    /** Returns true when [dataLine] is the SSE stream-end sentinel. */
    fun isDone(dataLine: String) = dataLine.trim() == "data:[DONE]" || dataLine.trim() == "[DONE]"
}
