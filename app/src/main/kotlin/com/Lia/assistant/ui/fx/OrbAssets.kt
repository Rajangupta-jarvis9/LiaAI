package com.Lia.assistant.ui.fx

/** Seven visual styles for the Canvas orb (NovaOrb). */
enum class NovaOrbStyle(val displayName: String, val description: String) {
    NOVA   ("Nova",    "Soft glow with orbiting particles"),
    AURORA ("Aurora",  "Multi-colour aurora rings"),
    PLASMA ("Plasma",  "Vibrant plasma with 6 particles"),
    GLASS  ("Glass",   "Translucent sphere with highlight arc"),
    ENERGY ("Energy",  "High-energy 8-particle orbit"),
    MINIMAL("Minimal", "Clean pulse, no particles"),
    ARCHER ("Archer",  "Golden-spiral sphere of ~400 particles"),
}

/**
 * Process-lifetime flag so [LiaSplash3D] never replays on rotation or resume.
 * Set to true after the splash finishes; never reset.
 */
object SplashSession {
    @Volatile
    var played: Boolean = false
}
