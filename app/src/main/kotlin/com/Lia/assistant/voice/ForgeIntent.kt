package com.Lia.assistant.voice

/**
 * Detects website-build requests in a chat message and extracts the description.
 *
 * Triggered by patterns like:
 *   "ek cafe ki website banao"
 *   "create a landing page for my restaurant"
 *   "build me a website about yoga"
 *   "make a webpage for my portfolio"
 *
 * The Forge implementation arrives in Part 14; this file is needed by ChatScreen
 * so it can intercept the intent before sending the message to Gemini.
 */
object ForgeIntent {

    data class ForgeRequest(val description: String)

    private val PATTERN = Regex(
        """(?ix)
        (?:
            (?:bana[o]?\s+do|banao|create|build|make|generate|design)\s+
            (?:me\s+|mera\s+|meri\s+|ek\s+)?
            (?:ek\s+)?
            (?:animated\s+|3d\s+|responsive\s+|modern\s+)?
            (?:website|webpage|web\s*page|landing\s*page|site|web)
        )
        |
        (?:
            (?:website|webpage|web\s*page|landing\s*page|site)
            .*
            (?:bana[o]?\s+do|banao|create|build|make|generate|design|chahiye|chahte)
        )
        """
    )

    /**
     * Returns a [ForgeRequest] when [text] matches the website-build pattern,
     * otherwise null.
     *
     * The full user message is used as the description so the Forge has all
     * context (location, style hints, colour preferences, etc.) the user mentioned.
     */
    fun extract(text: String): ForgeRequest? {
        val trimmed = text.trim()
        return if (PATTERN.containsMatchIn(trimmed)) ForgeRequest(trimmed) else null
    }
}
