package com.Lia.assistant.ui.screens.orbstyle
import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.LazyColumn; import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier; import androidx.compose.ui.unit.dp
import com.Lia.assistant.data.NovaAppState; import com.Lia.assistant.ui.components.*; import com.Lia.assistant.ui.fx.*; import com.Lia.assistant.ui.theme.*; import com.Lia.assistant.voice.NovaOrbState
@Composable fun OrbStyleScreen(appState: NovaAppState, onBack: () -> Unit) {
    val c = novaColors()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar("Orb Style", onBack = onBack)
        LazyColumn { items(NovaOrbStyle.entries) { style ->
            val selected = appState.orbStyle == style.name
            NovaGlassCard(modifier = Modifier.padding(horizontal = NovaSpacing.lg, vertical = NovaSpacing.sm),
                onClick = { appState.applyOrbStyle(style.name) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md)) {
                    NovaOrb(state = NovaOrbState.IDLE, style = style, modifier = Modifier.size(48.dp))
                    Column {
                        Text(style.displayName, style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) c.accent else c.textPrimary)
                        Text(style.description, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    }
                }
            }
        } }
    }
}
