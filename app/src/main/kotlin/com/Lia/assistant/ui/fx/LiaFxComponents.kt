package com.Lia.assistant.ui.fx

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import com.Lia.assistant.ui.theme.*
import com.Lia.assistant.voice.NovaOrbState
import com.Lia.assistant.voice.VoiceSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ── ListeningEdgeGlow ─────────────────────────────────────────────────────────

/**
 * A ~26 dp slim glowing border composable.
 * Rendered inside a Box that covers the full screen; the Scaffold content
 * sits behind it because the Box uses Arrangement.spacedBy with no padding.
 *
 * State → colour:
 *  IDLE / OFF   → transparent (invisible)
 *  LISTENING    → violet #8B5CF6
 *  SPEAKING     → lotus pink #FF6FAE
 *  ERROR        → red #FF7A85
 *  others       → amber #FBBF24
 */
@Composable
fun ListeningEdgeGlow(modifier: Modifier = Modifier) {
    val orbState    by VoiceSessionManager.state.collectAsState()
    val isActive    by VoiceSessionManager.isActive.collectAsState()
    val reducedMotion = LocalNovaReducedMotion.current

    if (!isActive || orbState == NovaOrbState.IDLE) return

    val glowColor = when (orbState) {
        NovaOrbState.LISTENING   -> Color(0xFF8B5CF6)
        NovaOrbState.SPEAKING    -> Color(0xFFFF6FAE)
        NovaOrbState.ERROR       -> Color(0xFFFF7A85)
        else                     -> Color(0xFFFBBF24)
    }

    val alpha by rememberInfiniteTransition(label = "edge_glow")
        .animateFloat(
            initialValue   = 0.6f,
            targetValue    = 1f,
            animationSpec  = if (reducedMotion) snap() else infiniteRepeatable(
                animation  = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "glow_alpha",
        )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                val bw = 26.dp.toPx()
                val col = glowColor.copy(alpha = alpha)
                // Top
                drawRect(col, topLeft = Offset.Zero,
                    size = Size(size.width, bw))
                // Bottom
                drawRect(col, topLeft = Offset(0f, size.height - bw),
                    size = Size(size.width, bw))
                // Left
                drawRect(col, topLeft = Offset.Zero,
                    size = Size(bw, size.height))
                // Right
                drawRect(col, topLeft = Offset(size.width - bw, 0f),
                    size = Size(bw, size.height))
            },
    )
}

// ── LiaSplash3D ───────────────────────────────────────────────────────────────

/**
 * Once-per-process 3D intro splash screen.
 * Rendered over the app content while it loads.
 * Never replays on rotation or resume — guarded by [SplashSession.played].
 *
 * The 3D animation here is a simplified Canvas orb on a dark background;
 * the full cinematic sequence can be enhanced later without changing the contract.
 */
@Composable
fun LiaSplash3D(
    assistantName: String,
    onFinished   : () -> Unit,
) {
    if (SplashSession.played) return

    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2200)
        visible = false
        SplashSession.played = true
        onFinished()
    }

    if (!visible) return

    val c = LocalNovaColors.current

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(c.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            NovaOrb(
                state        = NovaOrbState.CONNECTING,
                amplitude    = 0.4f,
                style        = NovaOrbStyle.ENERGY,
                assistantName= assistantName,
                modifier     = Modifier.size(140.dp),
            )
            androidx.compose.material3.Text(
                assistantName,
                style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
                color = c.textPrimary,
            )
        }
    }
}

// ── ForgeController (stub — Part 14) ─────────────────────────────────────────

/**
 * Coordinates website-forge navigation across the app.
 * VoiceSessionManager and ChatViewModel both call [requestOpen] when they
 * detect a build_website tool call or a ForgeIntent match.
 * Part 14 wires the full Forge UI.
 */
object ForgeController {
    private val _pendingOpen = MutableStateFlow<String?>(null)
    val pendingOpen: StateFlow<String?> = _pendingOpen

    fun requestOpen(description: String) {
        _pendingOpen.value = description
    }

    fun consume() {
        _pendingOpen.value = null
    }
}
