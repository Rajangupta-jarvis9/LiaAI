package com.Lia.assistant.ui.screens.about
import androidx.compose.foundation.layout.*; import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier
import com.Lia.assistant.data.AssistantBrand; import com.Lia.assistant.ui.components.*; import com.Lia.assistant.ui.fx.*; import com.Lia.assistant.ui.theme.*; import com.Lia.assistant.voice.NovaOrbState
@Composable fun AboutScreen(onBack: () -> Unit) {
    val c = novaColors()
    Column(Modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        NovaTopBar("About", onBack = onBack)
        Spacer(Modifier.height(48.dp))
        NovaOrb(state = NovaOrbState.IDLE, style = NovaOrbStyle.GLASS, modifier = Modifier.size(100.dp))
        Spacer(Modifier.height(24.dp))
        Text(AssistantBrand.FULL_NAME, style = MaterialTheme.typography.headlineMedium, color = c.textPrimary)
        Text("v1.0.0", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
        Spacer(Modifier.height(16.dp))
        Text(AssistantBrand.TAGLINE, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
    }
}
