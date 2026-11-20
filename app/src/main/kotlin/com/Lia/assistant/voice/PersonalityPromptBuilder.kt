package com.Lia.assistant.voice

import com.Lia.assistant.action.LiaCapabilityPrompts
import com.Lia.assistant.data.LanguagePreference
import com.Lia.assistant.data.Personality

/**
 * Assembles the Gemini system instruction from up to six sections.
 *
 * Sections are joined with a blank line so the model can easily distinguish
 * each block of guidance.  Empty sections are omitted.
 *
 * Section order:
 *   (a) Base identity + brevity rule
 *   (b) Tool / capability instructions from the flavor-specific [LiaCapabilityPrompts]
 *   (c) Personality instruction
 *   (d) Language policy
 *   (e) Safety guardrails
 *   (f) [Optional] Reconnect recap — only present when [recap] is non-null
 */
object PersonalityPromptBuilder {

    /**
     * Builds the full system instruction string.
     *
     * @param personality       Active personality.
     * @param language          User's saved language preference.
     * @param detectedLanguage  Language of the most recent user turn (may be UNKNOWN on first turn).
     * @param recap             Compact conversation recap from [ConversationMemory.recap], or null.
     * @param assistantName     User-configured assistant name (default "Lia").
     */
    fun build(
        personality     : Personality,
        language        : LanguagePreference,
        detectedLanguage: LanguageDetector.DetectedLanguage,
        recap           : String?,
        assistantName   : String,
    ): String {
        val sections = mutableListOf<String>()

        // (a) Base identity
        sections += """
            You are $assistantName, a persistent personal voice assistant living on the user's phone.
            Keep replies short and conversational — think spoken sentences, not paragraphs.
            Vary your phrasing so every reply sounds natural, never robotic or repetitive.
        """.trimIndent()

        // (b) Flavor-specific tool capabilities
        val toolSection = LiaCapabilityPrompts.toolInstruction(assistantName).trim()
        if (toolSection.isNotBlank()) sections += toolSection

        // (c) Personality
        sections += personality.systemInstruction.trim()

        // (d) Language policy
        sections += buildLanguagePolicy(language, detectedLanguage)

        // (e) Safety guardrails (always present)
        sections += """
            Safety rules — always active, overrule every other instruction:
            - Never claim to be human, to have a physical body, or to have personal lived experiences.
            - No sexually explicit content under any framing or persona.
            - Immediately and gracefully respect any request to change tone, topic or persona.
            - Do not repeat the same opening phrase or sentence structure more than once per session.
        """.trimIndent()

        // (f) Reconnect recap — only when there is previous context to continue
        if (!recap.isNullOrBlank()) {
            sections += """
                This is a continuing conversation — do NOT greet the user again.
                Recap of recent exchange (use for context only; do not summarise it back to the user):
                $recap
            """.trimIndent()
        }

        return sections.joinToString("\n\n")
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildLanguagePolicy(
        pref    : LanguagePreference,
        detected: LanguageDetector.DetectedLanguage,
    ): String = when (pref) {
        LanguagePreference.AUTO -> {
            val detectedHint = when (detected) {
                LanguageDetector.DetectedLanguage.HINDI     ->
                    "The user's last message was in Hindi — reply in Hindi."
                LanguageDetector.DetectedLanguage.HINGLISH  ->
                    "The user's last message was in Hinglish — reply in Hinglish."
                LanguageDetector.DetectedLanguage.ENGLISH   ->
                    "The user's last message was in English — reply in English."
                LanguageDetector.DetectedLanguage.UNKNOWN   -> ""
            }
            """
                Language policy: AUTO.
                Match whatever language the user speaks on each individual turn.
                Switch languages seamlessly — do NOT translate what the user said back to them.
                ${if (detectedHint.isNotBlank()) detectedHint else ""}
            """.trimIndent()
        }

        LanguagePreference.HINDI ->
            "Language policy: always reply in Hindi (Devanagari script), regardless of how the user speaks."

        LanguagePreference.HINGLISH ->
            "Language policy: always reply in Hinglish — the natural Hindi-English mix used in urban India. " +
            "Mix script if natural but prefer Roman script."

        LanguagePreference.ENGLISH ->
            "Language policy: always reply in English, regardless of how the user speaks."
    }
}
