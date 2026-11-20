package com.Lia.assistant.ui.fx

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.Lia.assistant.voice.NovaOrbState
import kotlin.math.*

/**
 * Canvas-based orb composable.  Works on all API levels.
 *
 * Animation periods:
 * - Breathing : 4.2 s sine, ±3.5 % radius
 * - Rotation  : 9 s (normal), 2.6 s (THINKING), 1.1 s (CONNECTING)
 */
@Composable
fun NovaOrb(
    state        : NovaOrbState  = NovaOrbState.IDLE,
    amplitude    : Float         = 0f,
    style        : NovaOrbStyle  = NovaOrbStyle.ENERGY,
    assistantName: String        = "Lia",
    modifier     : Modifier      = Modifier,
    reducedMotion: Boolean       = false,
) {
    // ── Time animations ───────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    val breathe by infiniteTransition.animateFloat(
        initialValue   = 0f,
        targetValue    = (2 * Math.PI).toFloat(),
        animationSpec  = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
        ),
        label = "breathe",
    )

    val rotatePeriod = when (state) {
        NovaOrbState.THINKING    -> 2600
        NovaOrbState.CONNECTING  -> 1100
        else                     -> 9000
    }
    val rotation by infiniteTransition.animateFloat(
        initialValue   = 0f,
        targetValue    = 360f,
        animationSpec  = infiniteRepeatable(
            animation = tween(rotatePeriod, easing = LinearEasing),
        ),
        label = "rotation",
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue   = 0f,
        targetValue    = 1f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    // ── Palette ───────────────────────────────────────────────────────────────
    val (coreColor, glowColor) = stateColors(state)

    // ── Semantics ─────────────────────────────────────────────────────────────
    val desc = "$assistantName status: ${state.name.lowercase()}"

    Canvas(
        modifier = modifier.semantics { contentDescription = desc },
    ) {
        val r         = size.minDimension / 2f
        val center    = Offset(size.width / 2f, size.height / 2f)
        val breatheR  = if (reducedMotion) r
                        else r * (1f + 0.035f * sin(breathe.toDouble()).toFloat())
        val amp       = if (reducedMotion) 0f else amplitude

        drawOrbStyle(
            style       = style,
            state       = state,
            center      = center,
            radius      = breatheR + amp * r * 0.06f,
            rotation    = if (reducedMotion) 0f else rotation,
            pulse       = if (reducedMotion) 0.5f else pulse,
            amplitude   = amp,
            coreColor   = coreColor,
            glowColor   = glowColor,
        )
    }
}

// ── State → palette ───────────────────────────────────────────────────────────

private fun stateColors(state: NovaOrbState): Pair<Color, Color> = when (state) {
    NovaOrbState.IDLE        -> Color(0xFF8B5CF6) to Color(0x668B5CF6)
    NovaOrbState.LISTENING   -> Color(0xFF3FE0D0) to Color(0x663FE0D0)
    NovaOrbState.THINKING    -> Color(0xFF60A5FA) to Color(0x6660A5FA)
    NovaOrbState.SPEAKING    -> Color(0xFFFF6FAE) to Color(0x66FF6FAE)
    NovaOrbState.CONNECTING  -> Color(0xFFFBBF24) to Color(0x66FBBF24)
    NovaOrbState.RECONNECTING-> Color(0xFFFBBF24) to Color(0x44FBBF24)
    NovaOrbState.ERROR       -> Color(0xFFF87171) to Color(0x44F87171)
}

// ── Per-style draw dispatch ───────────────────────────────────────────────────

private fun DrawScope.drawOrbStyle(
    style    : NovaOrbStyle,
    state    : NovaOrbState,
    center   : Offset,
    radius   : Float,
    rotation : Float,
    pulse    : Float,
    amplitude: Float,
    coreColor: Color,
    glowColor: Color,
) {
    if (style == NovaOrbStyle.ARCHER) {
        drawArcher(center, radius, rotation, amplitude, coreColor, glowColor)
        return
    }

    // Outer glow
    drawCircle(
        brush  = Brush.radialGradient(
            colors  = listOf(glowColor, Color.Transparent),
            center  = center,
            radius  = radius * 1.5f,
        ),
        radius = radius * 1.5f,
        center = center,
    )

    // Listening rings
    if (state == NovaOrbState.LISTENING) {
        for (i in 0..1) {
            val expand = (pulse + i * 0.5f) % 1f
            drawCircle(
                color  = coreColor.copy(alpha = (1f - expand) * 0.3f),
                radius = radius * (1f + expand * 0.5f),
                center = center,
                style  = androidx.compose.ui.graphics.drawscope.Stroke(2f),
            )
        }
    }

    // Connecting arc
    if (state == NovaOrbState.CONNECTING) {
        rotate(rotation, center) {
            drawArc(
                color      = coreColor.copy(alpha = 0.8f),
                startAngle = 0f,
                sweepAngle = 100f,
                useCenter  = false,
                topLeft    = Offset(center.x - radius, center.y - radius),
                size       = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style      = androidx.compose.ui.graphics.drawscope.Stroke(3f),
            )
        }
    }

    // Orbiting particles
    val particleCount = when {
        style == NovaOrbStyle.MINIMAL || state == NovaOrbState.ERROR -> 0
        style == NovaOrbStyle.ENERGY  -> 8
        style == NovaOrbStyle.PLASMA  -> 6
        style == NovaOrbStyle.GLASS   -> 3
        else                          -> 5
    }
    repeat(particleCount) { i ->
        val angle = Math.toRadians((rotation + i * (360f / particleCount)).toDouble())
        val pR    = radius * 1.15f + amplitude * radius * 0.08f
        val px    = center.x + (pR * cos(angle)).toFloat()
        val py    = center.y + (pR * sin(angle)).toFloat()
        drawCircle(color = coreColor.copy(alpha = 0.7f), radius = 4f, center = Offset(px, py))
    }

    // Core gradient
    drawCircle(
        brush  = Brush.radialGradient(
            colors  = listOf(Color.White.copy(alpha = 0.9f), coreColor, coreColor.copy(alpha = 0.6f)),
            center  = center,
            radius  = radius,
        ),
        radius = radius,
        center = center,
    )

    // Glass highlight arc
    if (style == NovaOrbStyle.GLASS) {
        drawArc(
            color      = Color.White.copy(alpha = 0.45f),
            startAngle = 200f,
            sweepAngle = 60f,
            useCenter  = false,
            topLeft    = Offset(center.x - radius * 0.65f, center.y - radius * 0.65f),
            size       = androidx.compose.ui.geometry.Size(radius * 1.3f, radius * 1.3f),
            style      = androidx.compose.ui.graphics.drawscope.Stroke(3f),
        )
    }

    // Error pulse ring
    if (state == NovaOrbState.ERROR) {
        drawCircle(
            color  = coreColor.copy(alpha = pulse * 0.4f),
            radius = radius * (1.1f + pulse * 0.2f),
            center = center,
            style  = androidx.compose.ui.graphics.drawscope.Stroke(2f),
        )
    }
}

// ── ARCHER style — golden spiral sphere ──────────────────────────────────────

private fun DrawScope.drawArcher(
    center   : Offset,
    radius   : Float,
    rotation : Float,
    amplitude: Float,
    coreColor: Color,
    glowColor: Color,
) {
    val goldenRatio = (1f + sqrt(5f)) / 2f
    val count       = 400
    val rotRad      = Math.toRadians(rotation.toDouble()).toFloat()

    repeat(count) { i ->
        val t     = i.toFloat() / count
        val theta = (2 * Math.PI * i / goldenRatio).toFloat()
        val phi   = acos(1f - 2f * t)

        // 3-D point on sphere
        val x3 = sin(phi) * cos(theta + rotRad)
        val y3 = sin(phi) * sin(theta + rotRad)
        val z3 = cos(phi)

        // Depth (z) → alpha + size
        val depth = (z3 + 1f) / 2f
        val alpha = 0.15f + depth * 0.75f
        val pSize = (1f + depth * 2.5f) + amplitude * 2f

        val px = center.x + x3 * radius
        val py = center.y + y3 * radius * 0.95f

        drawCircle(
            color  = coreColor.copy(alpha = alpha),
            radius = pSize,
            center = Offset(px, py),
        )
    }
}
