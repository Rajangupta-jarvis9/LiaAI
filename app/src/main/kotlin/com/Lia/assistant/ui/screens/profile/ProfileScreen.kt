package com.Lia.assistant.ui.screens.profile
import androidx.compose.foundation.layout.*; import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Modifier; import androidx.compose.ui.platform.LocalContext; import androidx.navigation.NavController
import com.Lia.assistant.data.AssistantBrand; import com.Lia.assistant.data.NovaAppState; import com.Lia.assistant.data.NovaPreferences; import com.Lia.assistant.ui.components.*; import com.Lia.assistant.ui.theme.*
@Composable fun ProfileScreen(appState: NovaAppState, navController: NavController) {
    val c = novaColors(); val context = LocalContext.current
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar("Profile", onBack = { navController.popBackStack() })
        Column(Modifier.padding(NovaSpacing.lg), verticalArrangement = Arrangement.spacedBy(NovaSpacing.lg)) {
            NovaTextField(value = appState.userName, onValueChange = { appState.applyUserName(it) }, label = "Your name", singleLine = true)
            NovaTextField(value = appState.assistantName, onValueChange = { appState.applyAssistantName(it) }, label = "Assistant name", singleLine = true)
        }
    }
}
