package com.Lia.assistant.voice

import com.Lia.assistant.voice.LanguageDetector.DetectedLanguage.*
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageDetectorTest {

    private fun detect(text: String) = LanguageDetector.detect(text)

    // ── Blank / empty ─────────────────────────────────────────────────────────

    @Test fun `empty string returns UNKNOWN`() =
        assertEquals(UNKNOWN, detect(""))

    @Test fun `whitespace only returns UNKNOWN`() =
        assertEquals(UNKNOWN, detect("   \t\n "))

    // ── Devanagari ────────────────────────────────────────────────────────────

    @Test fun `single Devanagari char returns HINDI`() =
        assertEquals(HINDI, detect("क"))

    @Test fun `pure Hindi sentence returns HINDI`() =
        assertEquals(HINDI, detect("मुझे आज बाज़ार जाना है"))

    @Test fun `Devanagari mixed with English returns HINDI`() =
        assertEquals(HINDI, detect("मुझे coffee chahiye"))

    // ── Hinglish ──────────────────────────────────────────────────────────────

    @Test fun `classic Hinglish question returns HINGLISH`() =
        assertEquals(HINGLISH, detect("kya haal hai yaar"))

    @Test fun `short Hinglish with single marker returns HINGLISH`() =
        assertEquals(HINGLISH, detect("kya hai"))

    @Test fun `longer Hinglish sentence returns HINGLISH`() =
        assertEquals(HINGLISH, detect("mujhe aaj office jana hai but traffic bahut zyada hai"))

    @Test fun `enough ratio even in long sentence returns HINGLISH`() =
        assertEquals(HINGLISH, detect("I think yaar the weather hai pretty good today"))

    // ── English ───────────────────────────────────────────────────────────────

    @Test fun `pure English returns ENGLISH`() =
        assertEquals(ENGLISH, detect("what's the weather today"))

    @Test fun `technical English sentence returns ENGLISH`() =
        assertEquals(ENGLISH, detect("implement a binary search tree in Kotlin"))

    @Test fun `short English phrase without any marker returns ENGLISH`() =
        assertEquals(ENGLISH, detect("hello how are you"))

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test fun `numbers only return ENGLISH`() =
        assertEquals(ENGLISH, detect("123 456 789"))

    @Test fun `single English word returns ENGLISH`() =
        assertEquals(ENGLISH, detect("hello"))

    @Test fun `single Hinglish marker (short text) returns HINGLISH`() =
        assertEquals(HINGLISH, detect("hai"))
}
