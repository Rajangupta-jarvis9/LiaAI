package com.Lia.assistant.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

// ── Composition local for bottom bar inset ────────────────────────────────────

/** Height of the floating bottom navigation bar including nav insets. */
val LocalBottomBarInset = staticCompositionLocalOf { 80.dp }

// ── Night sky background ──────────────────────────────────────────────────────

/**
 * Draws the deep indigo night-sky gradient with a sparse star field.
 *
 * Stars are pre-seeded so they do not move across recompositions.
 * Pass [parallaxOffsetX] / [parallaxOffsetY] (derived from the accelerometer)
 * to add a subtle depth shift; defaults to zero (no parallax).
 */
fun Modifier.nightSky(
    parallaxOffsetX: Float = 0f,
    parallaxOffsetY: Float = 0f,
): Modifier = composed {
    val c = LocalNovaColors.current
    this
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(c.backgroundGradientTop, c.background),
            ),
        )
        .drawBehind {
            val rng    = Random(0xDEADBEEF.toInt())
            val starCount = 80
            repeat(starCount) {
                val x     = rng.nextFloat() * size.width  + parallaxOffsetX * 0.03f
                val y     = rng.nextFloat() * size.height + parallaxOffsetY * 0.03f
                val alpha = rng.nextFloat() * 0.55f + 0.15f
                val radius= rng.nextFloat() * 1.2f + 0.4f
                drawCircle(
                    color  = Color.White.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(x, y),
                )
            }
        }
}

// ── Depth surface modifier ────────────────────────────────────────────────────

/**
 * Applies a coloured ambient shadow behind the composable and a subtle top
 * rim light (1 dp translucent white stroke) to simulate depth.
 */
fun Modifier.depthSurface(
    shape    : Shape,
    elevation: Dp = 8.dp,
): Modifier = composed {
    val c = LocalNovaColors.current
    this
        .shadow(
            elevation       = elevation,
            shape           = shape,
            ambientColor    = c.depthShadow,
            spotColor       = c.depthShadow,
        )
        .drawBehind {
            // Rim light drawn as a thin top stroke inside the bounds.
            drawRect(
                color   = c.rimLight,
                topLeft = Offset(0f, 0f),
                size    = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()),
            )
        }
}
