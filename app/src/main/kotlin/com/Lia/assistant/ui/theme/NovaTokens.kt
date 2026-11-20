package com.Lia.assistant.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Lia.assistant.R

// ── Font families ─────────────────────────────────────────────────────────────
// Place .ttf files in res/font/ and declare them in res/font/instrument_serif*.xml.
// Instrument Serif — https://fonts.google.com/specimen/Instrument+Serif  (OFL 1.1)
// Manrope          — https://fonts.google.com/specimen/Manrope            (OFL 1.1)
//
// If the font resources are not yet added, this falls back gracefully to the
// system default sans-serif (no crash, but the design fidelity is reduced).

val InstrumentSerif: FontFamily = try {
    FontFamily(
        Font(R.font.instrument_serif_regular, FontWeight.Normal,  FontStyle.Normal),
        Font(R.font.instrument_serif_italic,  FontWeight.Normal,  FontStyle.Italic),
    )
} catch (_: Throwable) { FontFamily.Serif }

val Manrope: FontFamily = try {
    FontFamily(
        Font(R.font.manrope_regular,    FontWeight.Normal),
        Font(R.font.manrope_medium,     FontWeight.Medium),
        Font(R.font.manrope_semibold,   FontWeight.SemiBold),
        Font(R.font.manrope_bold,       FontWeight.Bold),
        Font(R.font.manrope_extrabold,  FontWeight.ExtraBold),
    )
} catch (_: Throwable) { FontFamily.Default }

// ── Typography ────────────────────────────────────────────────────────────────

/** Material3 Typography mapped to Lia's type scale. */
val NovaTypography = Typography(
    // Display — Instrument Serif, used for hero text / orb-context labels
    displayLarge  = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal,
                              fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal,
                              fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal,
                              fontSize = 36.sp, lineHeight = 44.sp),

    // Headline — Instrument Serif, section intro lines
    headlineLarge  = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal,
                               fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal,
                               fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal,
                               fontSize = 24.sp, lineHeight = 32.sp),

    // Title — Manrope SemiBold, nav titles, card titles
    titleLarge  = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall  = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp, lineHeight = 20.sp),

    // Body — Manrope Regular
    bodyLarge   = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal,
                            fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium  = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal,
                            fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall   = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal,
                            fontSize = 13.sp, lineHeight = 18.sp),

    // Label — Manrope Medium, buttons, chips, captions
    labelLarge  = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium,
                            fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium,
                            fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall  = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium,
                            fontSize = 12.sp, lineHeight = 16.sp),
)

// ── Spacing ───────────────────────────────────────────────────────────────────

object NovaSpacing {
    val xs   : Dp = 4.dp
    val sm   : Dp = 8.dp
    val md   : Dp = 12.dp
    val lg   : Dp = 16.dp
    val xl   : Dp = 24.dp
    val xxl  : Dp = 32.dp
    val xxxl : Dp = 48.dp
}

// ── Shapes ────────────────────────────────────────────────────────────────────

object NovaShapes {
    val card   = RoundedCornerShape(12.dp)
    val dialog = RoundedCornerShape(20.dp)
    val sheet  = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val pill   = RoundedCornerShape(50)
}

// ── Motion ────────────────────────────────────────────────────────────────────

object NovaMotion {
    const val QUICK   = 150
    const val STANDARD= 300
    const val SLOW    = 500
}

// ── Reduced motion composition local ─────────────────────────────────────────

val LocalNovaReducedMotion = staticCompositionLocalOf { false }
