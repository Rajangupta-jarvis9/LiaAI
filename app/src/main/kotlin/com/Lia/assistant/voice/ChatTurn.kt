package com.Lia.assistant.voice

/**
 * One message in a text-chat session.
 *
 * [isUser] = true  → user message   → Gemini role "user"
 * [isUser] = false → assistant reply → Gemini role "model"
 */
data class ChatTurn(
    val isUser    : Boolean,
    val text      : String,
    val timestampMs: Long = System.currentTimeMillis(),
)
