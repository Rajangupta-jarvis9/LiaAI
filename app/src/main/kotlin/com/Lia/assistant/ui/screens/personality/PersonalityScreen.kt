package com.Lia.assistant.ui.screens.personality
import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.LazyColumn; import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Modifier
import com.Lia.assistant.data.NovaAppState; import com.Lia.assistant.data.Personality; import com.Lia.assistant.ui.components.*; import com.Lia.assistant.ui.theme.*
@Composable fun PersonalityScreen(appState: NovaAppState, onBack: () -> Unit) {
    val c = novaColors()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar("Personality", onBack = onBack)
        LazyColumn { items(Personality.entries) { p ->
            val selected = appState.personality == p
            NovaGlassCard(modifier = Modifier.padding(horizontal = NovaSpacing.lg, vertical = NovaSpacing.sm),
                onClick = { appState.applyPersonality(p) }) {
                Text("${p.icon}  ${p.displayName}", style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) c.accent else c.textPrimary)
                Text(p.description, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        } }
    }
}
