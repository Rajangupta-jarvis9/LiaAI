package com.Lia.assistant.ui.screens.privacy
import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.LazyColumn; import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Modifier
import com.Lia.assistant.ui.components.*; import com.Lia.assistant.ui.theme.*
@Composable fun PrivacyScreen(onBack: () -> Unit) {
    val c = novaColors()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar("Privacy", onBack = onBack)
        LazyColumn(Modifier.padding(NovaSpacing.lg)) {
            item { Text("Data stays on your device.\n\nYour Gemini API key is stored in private SharedPreferences and never sent anywhere except Google's Gemini API.\n\nVoice audio is streamed directly to Gemini Live and is not stored by this app.\n\nConversation history lives in RAM only and is cleared when you stop the session.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary) }
        }
    }
}
