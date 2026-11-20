package com.Lia.assistant.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Lia AI colour token set.
 *
 * Identity: "a pearl of light in the night."
 * Deep indigo night background. ONE warm marigold accent for all actionable things.
 * Lotus + lagoon are reserved for the orb / emotional indicators only.
 *
 * Never use raw Color(0x…) in screens — always read from LocalNovaColors.current.
 */
data class NovaColorScheme(
    // ── Backgrounds ───────────────────────────────────────────────────────────
    val background            : Color,
    val backgroundGradientTop : Color,

    // ── Surfaces ──────────────────────────────────────────────────────────────
    val surface               : Color,
    val surfaceGlass          : Color,
    val surfaceBorder         : Color,
    val surfaceRaised         : Color,

    // ── Accent ────────────────────────────────────────────────────────────────
    val accent                : Color,   // marigold — buttons, FAB, all actionable things
    val accentSecondary       : Color,   // warm orange — secondary actions
    val accentGlow            : Color,   // translucent glow behind accent elements
    val onAccent              : Color,   // text/icon colour on accent background

    // ── Text ──────────────────────────────────────────────────────────────────
    val textPrimary           : Color,
    val textSecondary         : Color,
    val textTertiary          : Color,

    // ── Semantic ──────────────────────────────────────────────────────────────
    val success               : Color,
    val warning               : Color,
    val error                 : Color,

    // ── Chat bubbles ──────────────────────────────────────────────────────────
    val userBubble            : Color,
    val assistantBubble       : Color,

    // ── Depth / orb ───────────────────────────────────────────────────────────
    val depthShadow           : Color,
    val rimLight              : Color,

    // ── Orb-exclusive palette ─────────────────────────────────────────────────
    val lagoon                : Color,   // #3FE0D0 — listening / active
    val lotus                 : Color,   // #FF6FAE — speaking / emotional
)

// ── Dark scheme ───────────────────────────────────────────────────────────────

val NovaDarkColors = NovaColorScheme(
    background            = Color(0xFF06050E),
    backgroundGradientTop = Color(0xFF15113A),
    surface               = Color(0xFF17143A),
    surfaceGlass          = Color(0x2617143A),
    surfaceBorder         = Color(0x33A7A0CC),
    surfaceRaised         = Color(0xFF221E4D),
    accent                = Color(0xFFFFB547),
    accentSecondary       = Color(0xFFFF8A3D),
    accentGlow            = Color(0x33FFB547),
    onAccent              = Color(0xFF0E0D1A),
    textPrimary           = Color(0xFFEEE9FF),
    textSecondary         = Color(0xFFA7A0CC),
    textTertiary          = Color(0xFF6B638F),
    success               = Color(0xFF4ADE80),
    warning               = Color(0xFFFBBF24),
    error                 = Color(0xFFF87171),
    userBubble            = Color(0x26FFB547),
    assistantBubble       = Color(0xFF221E4D),
    depthShadow           = Color(0xCC000000),
    rimLight              = Color(0x1AFFFFFF),
    lagoon                = Color(0xFF3FE0D0),
    lotus                 = Color(0xFFFF6FAE),
)

// ── Light scheme ──────────────────────────────────────────────────────────────

val NovaLightColors = NovaColorScheme(
    background            = Color(0xFFF3F0FB),
    backgroundGradientTop = Color(0xFFDDD8F5),
    surface               = Color(0xFFFFFFFF),
    surfaceGlass          = Color(0xB3FFFFFF),
    surfaceBorder         = Color(0x1A4B4284),
    surfaceRaised         = Color(0xFFECE9F8),
    accent                = Color(0xFFB86A00),
    accentSecondary       = Color(0xFFD4540A),
    accentGlow            = Color(0x33B86A00),
    onAccent              = Color(0xFFFFFFFF),
    textPrimary           = Color(0xFF15112E),
    textSecondary         = Color(0xFF4B4284),
    textTertiary          = Color(0xFF7B73A8),
    success               = Color(0xFF16A34A),
    warning               = Color(0xFFD97706),
    error                 = Color(0xFFDC2626),
    userBubble            = Color(0x1AB86A00),
    assistantBubble       = Color(0xFFECE9F8),
    depthShadow           = Color(0x334B4284),
    rimLight              = Color(0x0D000000),
    lagoon                = Color(0xFF0C8F85),
    lotus                 = Color(0xFFD63580),
)

val LocalNovaColors = staticCompositionLocalOf { NovaDarkColors }
