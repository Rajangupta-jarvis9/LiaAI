package com.Lia.assistant.voice

/**
 * Detects whether a text snippet is in Hindi (Devanagari script), Hinglish
 * (romanised Hindi-English mix) or English.
 *
 * Detection runs synchronously in O(n) time on the input length —
 * safe to call from any thread, including the audio callback thread.
 */
object LanguageDetector {

    enum class DetectedLanguage { HINDI, HINGLISH, ENGLISH, UNKNOWN }

    /**
     * Hinglish marker words — high-frequency romanised Hindi words that appear
     * in Hinglish but rarely in pure English prose.
     */
    private val HINGLISH_MARKERS: Set<String> = setOf(
        // verbs / verb roots
        "hai", "hain", "ho", "hoga", "hogi", "hoge", "tha", "thi", "the",
        "karna", "karo", "karta", "karti", "kar", "kiya", "kiye",
        "raha", "rahi", "rahe", "rahega", "chahiye", "chahte", "chahti",
        "batao", "suno", "dekho", "samjho", "bolo",
        // question words
        "kya", "kyun", "kyunki", "kaise", "kab", "kahan", "kaun", "kitna",
        // negation / affirmation
        "nahi", "nahin", "mat", "haan", "accha", "theek", "bilkul",
        // pronouns / address
        "mujhe", "mujko", "tumhe", "tumko", "aapko", "mera", "meri",
        "tera", "teri", "apna", "apni", "yeh", "woh", "wahi", "yahi",
        "aap", "tum", "main", "hum",
        // connectors / discourse
        "aur", "ya", "lekin", "magar", "par", "toh", "ki", "ke", "ka",
        "phir", "abhi", "pehle", "baad", "sirf", "bhi",
        // intensifiers / fillers
        "bahut", "thoda", "kuch", "sab", "sabhi", "poora", "zyada",
        "wala", "wali", "wale",
        // colloquial / address forms
        "yaar", "bhai", "bhaiya", "dost", "matlab", "arre", "oye",
        "bolo", "sunlo", "dekh",
    )

    /**
     * Returns the most likely language of [text].
     *
     * Algorithm:
     * 1. If ANY Devanagari character (U+0900..U+097F) is found → [DetectedLanguage.HINDI].
     * 2. Tokenise into lowercase words, count hits against [HINGLISH_MARKERS].
     * 3. Short text (≤ 4 words): 1 hit → HINGLISH.
     *    Longer text         : ≥ 2 hits OR marker ratio ≥ 0.15 → HINGLISH.
     * 4. No Devanagari, no markers → [DetectedLanguage.ENGLISH].
     * 5. Blank / whitespace-only input → [DetectedLanguage.UNKNOWN].
     */
    fun detect(text: String): DetectedLanguage {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return DetectedLanguage.UNKNOWN

        // 1. Devanagari check — one char is enough.
        if (trimmed.any { it.code in 0x0900..0x097F }) return DetectedLanguage.HINDI

        // 2. Tokenise: split on anything that is not a letter or apostrophe.
        val words = trimmed.lowercase().split(Regex("[^a-zA-Z']+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return DetectedLanguage.UNKNOWN

        val hitCount = words.count { it in HINGLISH_MARKERS }

        // 3. Threshold depends on text length.
        return if (words.size <= 4) {
            if (hitCount >= 1) DetectedLanguage.HINGLISH else DetectedLanguage.ENGLISH
        } else {
            val ratio = hitCount.toFloat() / words.size
            if (hitCount >= 2 || ratio >= 0.15f) DetectedLanguage.HINGLISH
            else DetectedLanguage.ENGLISH
        }
    }
}
