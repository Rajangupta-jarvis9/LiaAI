package com.Lia.assistant.forge

/**
 * The six build stages of a generated website.
 *
 * [ForgeStageDetector.detect] maps a partial HTML buffer to the furthest
 * stage that has unambiguously started.  Each stage is a superset of the
 * previous one — once a stage is reached it never regresses.
 */
enum class ForgeStage(val displayName: String, val progressFraction: Float) {
    READING   ("Reading idea",       0.05f),
    STRUCTURE ("Building structure", 0.25f),
    STYLING   ("Styling pages",      0.50f),
    MOTION    ("Adding motion",      0.70f),
    CONTENT   ("Filling content",    0.87f),
    FINISHING ("Finishing touches",  1.00f),
}

object ForgeStageDetector {

    /**
     * Returns the furthest [ForgeStage] that [partial] has reached.
     *
     * Rules:
     * - HEAD only (no <body>)       → READING
     * - <body> present              → STRUCTURE
     * - Any <style> or Tailwind CDN → STYLING
     * - Any <script> or GSAP        → MOTION
     * - At least 3 visible text sections (h1/h2/p/li with content) → CONTENT
     * - </html> present             → FINISHING
     */
    fun detect(partial: String): ForgeStage {
        val lower = partial.lowercase()

        if ("</html>" in lower)                                    return ForgeStage.FINISHING
        if (hasVisibleSections(lower))                             return ForgeStage.CONTENT
        if ("<script" in lower || "gsap" in lower)                 return ForgeStage.MOTION
        if ("<style" in lower || "tailwind" in lower)              return ForgeStage.STYLING
        if ("<body" in lower)                                      return ForgeStage.STRUCTURE
        return ForgeStage.READING
    }

    private fun hasVisibleSections(lower: String): Boolean {
        // Count distinct heading or paragraph tags that have non-trivial content.
        val tagRegex = Regex("""<(h[1-6]|p|li)[^>]*>([^<]{5,})""")
        return tagRegex.findAll(lower).count() >= 3
    }
}
