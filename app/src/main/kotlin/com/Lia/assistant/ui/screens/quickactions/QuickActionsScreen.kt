package com.Lia.assistant.ui.screens.quickactions
import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.grid.*; import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier; import androidx.compose.ui.graphics.vector.ImageVector; import androidx.compose.ui.unit.dp
import androidx.navigation.NavController; import com.Lia.assistant.ui.components.*; import com.Lia.assistant.ui.navigation.NavRoute; import com.Lia.assistant.ui.theme.*
@Composable fun QuickActionsScreen(navController: NavController) {
    val actions = listOf(Icons.Default.Mic to "Voice" to NavRoute.VOICE, Icons.Default.Chat to "Chat" to NavRoute.CHAT, Icons.Default.History to "History" to NavRoute.HISTORY, Icons.Default.Settings to "Settings" to NavRoute.SETTINGS)
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar("Quick Actions", onBack = { navController.popBackStack() })
        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(NovaSpacing.lg), horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md), verticalArrangement = Arrangement.spacedBy(NovaSpacing.md)) {
            items(actions) { (iconLabel, route) -> val (icon, label) = iconLabel
                NovaGlassCard(onClick = { navController.navigate(route) }) {
                    Column(Modifier.padding(NovaSpacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, label, tint = novaColors().accent, modifier = Modifier.size(28.dp))
                        Text(label, style = MaterialTheme.typography.bodySmall, color = novaColors().textPrimary)
                    }
                }
            }
        }
    }
}
