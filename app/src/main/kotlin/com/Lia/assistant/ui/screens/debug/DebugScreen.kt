package com.Lia.assistant.ui.screens.debug
import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.LazyColumn; import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Modifier
import com.Lia.assistant.ui.components.*; import com.Lia.assistant.ui.theme.*; import com.Lia.assistant.voice.VoiceLatencyTracker; import com.Lia.assistant.voice.VoiceSessionManager
@Composable fun DebugScreen(onBack: () -> Unit) {
    val c = novaColors()
    val state      by VoiceSessionManager.state.collectAsState()
    val reconnects by VoiceLatencyTracker.reconnectCount.collectAsState()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar("Debug", onBack = onBack)
        LazyColumn(Modifier.padding(NovaSpacing.lg)) {
            item { Text("Orb state: $state",                          style = MaterialTheme.typography.bodySmall, color = c.textSecondary) }
            item { Text("Reconnects: $reconnects",                    style = MaterialTheme.typography.bodySmall, color = c.textSecondary) }
            item { Text("Last connect latency: ${VoiceLatencyTracker.lastConnectMs} ms",   style = MaterialTheme.typography.bodySmall, color = c.textSecondary) }
            item { Text("Last first-audio latency: ${VoiceLatencyTracker.lastFirstAudioMs} ms", style = MaterialTheme.typography.bodySmall, color = c.textSecondary) }
        }
    }
}
