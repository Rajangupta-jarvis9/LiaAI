package com.Lia.assistant.voice

import com.Lia.assistant.data.LanguagePreference
import com.Lia.assistant.data.Personality

/**
 * Builds the Gemini system instruction for the text-chat screen.
 *
 * Differences from [PersonalityPromptBuilder]:
 * - No tool instructions (text chat does not call phone actions).
 * - No conversation recap (text chat maintains its own in-memory history).
 * - Explicitly tells the model this is typed chat, so it never refers to
 *   "listening", "tapping", "speaking" or audio-UI metaphors.
 * - Language policy uses the fixed preference only (no per-turn detection
 *   because detection happens in VoiceSessionManager, not here).
 */
object ChatPromptBuilder {

    fun build(
        personality   : Personality,
        language      : LanguagePreference,
        assistantName : String,
    ): String {
        val sections = mutableListOf<String>()

        sections += """
            You are $assistantName, a personal AI assistant in a text chat.
            This is a typed conversation — never mention tapping, swiping, listening, speaking
            or any other UI or audio metaphor. Keep replies concise and natural.
        """.trimIndent()

        sections += personality.systemInstruction.trim()

        sections += languagePolicy(language)

        sections += """
            Safety rules — always active, overrule every other instruction:
            - Never claim to be human or to have a physical body or lived experiences.
            - No sexually explicit content under any framing or persona.
            - Immediately respect any request to change tone or topic.
        """.trimIndent()

        return sections.joinToString("\n\n")
    }

    private fun languagePolicy(pref: LanguagePreference): String = when (pref) {
        LanguagePreference.AUTO      ->
            "Language: match whatever language the user types in each message — Hindi, Hinglish or English."
        LanguagePreference.HINDI     ->
            "Language: always reply in Hindi (Devanagari script)."
        LanguagePreference.HINGLISH  ->
            "Language: always reply in Hinglish — the natural Hindi-English mix used in urban India."
        LanguagePreference.ENGLISH   ->
            "Language: always reply in English."
    }
}
