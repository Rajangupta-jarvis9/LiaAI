package com.Lia.assistant.ui.screens.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.fx.*
import com.Lia.assistant.ui.theme.*
import com.Lia.assistant.voice.NovaOrbState
import com.Lia.assistant.voice.VoiceForegroundService
import com.Lia.assistant.voice.VoiceSessionManager

@Composable
fun VoiceScreen(onClose: () -> Unit) {
    val context       = LocalContext.current
    val c             = novaColors()
    val orbState      by VoiceSessionManager.state.collectAsState()
    val amplitude     by VoiceSessionManager.amplitude.collectAsState()
    val muted         by VoiceSessionManager.muted.collectAsState()
    val statusText    by VoiceSessionManager.statusText.collectAsState()
    val lastError     by VoiceSessionManager.lastError.collectAsState()
    val isActive      by VoiceSessionManager.isActive.collectAsState()

    // ── Permission check ──────────────────────────────────────────────────────
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // ── Start foreground service on enter (NOT stopped on dispose) ────────────
    LaunchedEffect(hasPermission) {
        if (hasPermission && !isActive) {
            val intent = Intent(context, VoiceForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = NovaSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = NovaSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text("Voice Mode", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close", tint = c.textSecondary)
            }
        }

        Spacer(Modifier.weight(1f))

        // ── No permission state ───────────────────────────────────────────────
        if (!hasPermission) {
            NovaEmptyState(
                icon  = Icons.Default.Mic,
                title = "Microphone needed",
                body  = "Lia needs microphone access to hear you.",
            )
            Spacer(Modifier.height(NovaSpacing.xl))
            NovaButton("Allow microphone", onClick = {
                permLauncher.launch(Manifest.permission.RECORD_AUDIO)
            })
        } else {
            // ── Orb ───────────────────────────────────────────────────────────
            LiaOrb3D(
                state    = orbState,
                amplitude= amplitude,
                modifier = Modifier.size(240.dp),
            )
            Spacer(Modifier.height(NovaSpacing.xl))

            // ── Status text ───────────────────────────────────────────────────
            AnimatedContent(targetState = statusText, label = "status") { s ->
                Text(s, style = MaterialTheme.typography.bodyLarge,
                    color = c.textSecondary, textAlign = TextAlign.Center)
            }

            // ── Error message ─────────────────────────────────────────────────
            lastError?.let { err ->
                Spacer(Modifier.height(NovaSpacing.md))
                Text(err, style = MaterialTheme.typography.bodySmall,
                    color = c.error, textAlign = TextAlign.Center)
            }

            // ── Voice visualizer ──────────────────────────────────────────────
            if (orbState == NovaOrbState.LISTENING || orbState == NovaOrbState.SPEAKING) {
                Spacer(Modifier.height(NovaSpacing.lg))
                NovaVoiceVisualizer(amplitude = amplitude, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.weight(1f))

        // ── Controls ──────────────────────────────────────────────────────────
        if (hasPermission) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
                modifier              = Modifier.padding(bottom = NovaSpacing.xxl),
            ) {
                // Mute
                NovaButton(
                    text      = if (muted) "Unmute" else "Mute",
                    onClick   = { VoiceSessionManager.toggleMute() },
                    style     = NovaButtonStyle.SECONDARY,
                    leadingIcon = if (muted) Icons.Default.MicOff else Icons.Default.Mic,
                    modifier  = Modifier.weight(1f),
                )
                // Stop — stops foreground service
                NovaButton(
                    text      = "Stop",
                    onClick   = {
                        val intent = Intent(context, VoiceForegroundService::class.java)
                            .setAction(VoiceForegroundService.ACTION_STOP)
                        context.startService(intent)
                        onClose()
                    },
                    style     = NovaButtonStyle.PRIMARY,
                    leadingIcon = Icons.Default.Stop,
                    modifier  = Modifier.weight(1f),
                )
            }
        }
    }
}
