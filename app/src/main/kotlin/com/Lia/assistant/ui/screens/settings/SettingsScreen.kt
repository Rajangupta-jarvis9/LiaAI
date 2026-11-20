package com.Lia.assistant.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.Lia.assistant.data.ApiKeyStore
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.navigation.NavRoute
import com.Lia.assistant.ui.theme.*

@Composable
fun SettingsScreen(appState: NovaAppState, navController: NavController) {
    val context = LocalContext.current
    val hasKey  = ApiKeyStore.hasKey(context)
    LazyColumn(modifier = Modifier.fillMaxSize().statusBarsPadding()
        .padding(bottom = LocalBottomBarInset.current)) {
        item { NovaTopBar("Settings") }
        item { NovaSectionHeader("Gemini API") }
        item { NovaSettingsRow("API Key", icon = Icons.Default.Key,
            subtitle = if (hasKey) "Key is saved" else "Not yet set",
            onClick  = { navController.navigate(NavRoute.PERMISSIONS) }) }
        item { NovaSectionHeader("Assistant") }
        item { NovaSettingsRow("Personality", icon = Icons.Default.Person,
            value = appState.personality.displayName,
            onClick = { navController.navigate(NavRoute.PERSONALITY) }) }
        item { NovaSettingsRow("Orb Style", icon = Icons.Default.Circle,
            value = appState.orbStyle,
            onClick = { navController.navigate(NavRoute.ORB_STYLE) }) }
        item { NovaSettingsRow("Language", icon = Icons.Default.Translate,
            value = appState.language.displayName, onClick = {}) }
        item { NovaSectionHeader("Appearance") }
        item { NovaSettingsRow("Reduce motion", icon = Icons.Default.Animation,
            checked = appState.reducedMotion,
            onChecked = { appState.applyReducedMotion(it) }) }
        item { NovaSectionHeader("More") }
        item { NovaSettingsRow("Profile",     icon = Icons.Default.ManageAccounts, onClick = { navController.navigate(NavRoute.PROFILE) }) }
        item { NovaSettingsRow("Permissions", icon = Icons.Default.Security,       onClick = { navController.navigate(NavRoute.PERMISSIONS) }) }
        item { NovaSettingsRow("Privacy",     icon = Icons.Default.PrivacyTip,     onClick = { navController.navigate(NavRoute.PRIVACY) }) }
        item { NovaSettingsRow("About",       icon = Icons.Default.Info,            onClick = { navController.navigate(NavRoute.ABOUT) }) }
        item { NovaSettingsRow("Debug",       icon = Icons.Default.BugReport,       onClick = { navController.navigate(NavRoute.DEBUG) }) }
    }
}
