package com.Lia.assistant.voice

import com.Lia.assistant.data.LanguagePreference
import com.Lia.assistant.data.Personality
import org.junit.Assert.*
import org.junit.Test

class PersonalityPromptBuilderTest {

    private fun build(
        personality     : Personality             = Personality.NORMAL,
        language        : LanguagePreference       = LanguagePreference.AUTO,
        detectedLanguage: LanguageDetector.DetectedLanguage = LanguageDetector.DetectedLanguage.ENGLISH,
        recap           : String?                  = null,
        assistantName   : String                   = "Lia",
    ) = PersonalityPromptBuilder.build(
        personality, language, detectedLanguage, recap, assistantName,
    )

    // ── All 10 personalities × 4 language prefs = 40 combinations ────────────

    @Test fun `all 40 personality-language combinations build without exception`() {
        for (p in Personality.entries) {
            for (l in LanguagePreference.entries) {
                val result = build(personality = p, language = l)
                assertTrue(
                    "Empty prompt for $p/$l",
                    result.isNotBlank(),
                )
            }
        }
    }

    // ── Base section ──────────────────────────────────────────────────────────

    @Test fun `prompt contains assistant name in base section`() {
        val result = build(assistantName = "Jarvis")
        assertTrue(result.contains("Jarvis"))
    }

    // ── Recap section ─────────────────────────────────────────────────────────

    @Test fun `recap section absent when recap is null`() {
        val result = build(recap = null)
        assertFalse(result.contains("continuing conversation"))
    }

    @Test fun `recap section absent when recap is blank`() {
        val result = build(recap = "   ")
        assertFalse(result.contains("continuing conversation"))
    }

    @Test fun `recap section present when recap has content`() {
        val result = build(recap = "User: hello\nYou: hi there")
        assertTrue(result.contains("continuing conversation"))
        assertTrue(result.contains("User: hello"))
    }

    // ── Language sections ─────────────────────────────────────────────────────

    @Test fun `AUTO policy with HINGLISH detected mentions Hinglish`() {
        val result = build(
            language         = LanguagePreference.AUTO,
            detectedLanguage = LanguageDetector.DetectedLanguage.HINGLISH,
        )
        assertTrue(result.lowercase().contains("hinglish"))
    }

    @Test fun `HINDI fixed policy mentions Hindi`() {
        val result = build(language = LanguagePreference.HINDI)
        assertTrue(result.lowercase().contains("hindi"))
    }

    @Test fun `ENGLISH fixed policy mentions English`() {
        val result = build(language = LanguagePreference.ENGLISH)
        assertTrue(result.lowercase().contains("english"))
    }

    // ── Safety section always present ─────────────────────────────────────────

    @Test fun `safety section always present`() {
        for (p in Personality.entries) {
            val result = build(personality = p)
            assertTrue(
                "Safety section missing for $p",
                result.contains("Safety rules"),
            )
        }
    }

    // ── GF_MODE and COMPANION must contain hard-limit reminder ────────────────

    @Test fun `GF_MODE contains hard limits`() {
        val result = build(personality = Personality.GF_MODE)
        assertTrue(result.contains("Hard limits"))
    }

    @Test fun `COMPANION contains hard limits`() {
        val result = build(personality = Personality.COMPANION)
        assertTrue(result.contains("Hard limits"))
    }

    // ── Personality instruction included ─────────────────────────────────────

    @Test fun `NAUTANKI instruction mentions Bollywood`() {
        val result = build(personality = Personality.NAUTANKI)
        assertTrue(result.lowercase().contains("bollywood"))
    }

    @Test fun `DEVELOPER instruction mentions code`() {
        val result = build(personality = Personality.DEVELOPER)
        assertTrue(result.lowercase().contains("code"))
    }
}
