package com.Lia.assistant.ui.screens.history
import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.LazyColumn; import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier
import com.Lia.assistant.data.NovaAppState; import com.Lia.assistant.ui.components.*; import com.Lia.assistant.ui.theme.*
@Composable fun HistoryScreen(appState: NovaAppState) {
    val c = novaColors()
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = LocalBottomBarInset.current)) {
        NovaTopBar("History")
        if (appState.conversations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                NovaEmptyState(Icons.Default.History, "No conversations yet", "Start talking or typing to see your history here.")
            }
        } else {
            LazyColumn { items(appState.conversations) { conv ->
                NovaGlassCard(modifier = Modifier.padding(horizontal = NovaSpacing.lg, vertical = NovaSpacing.sm)) {
                    Text(conv.title,   style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
                    Text(conv.preview, style = MaterialTheme.typography.bodySmall,  color = c.textSecondary)
                }
            } }
        }
    }
}
