package com.Lia.assistant.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.fx.*
import com.Lia.assistant.forge.ForgeController
import com.Lia.assistant.ui.screens.forge.ForgeScreen
import com.Lia.assistant.ui.screens.about.AboutScreen
import com.Lia.assistant.ui.screens.chat.ChatScreen
import com.Lia.assistant.ui.screens.debug.DebugScreen
import com.Lia.assistant.ui.screens.history.HistoryScreen
import com.Lia.assistant.ui.screens.home.HomeScreen
import com.Lia.assistant.ui.screens.onboarding.OnboardingScreen
import com.Lia.assistant.ui.screens.orbstyle.OrbStyleScreen
import com.Lia.assistant.ui.screens.permissions.PermissionsScreen
import com.Lia.assistant.ui.screens.personality.PersonalityScreen
import com.Lia.assistant.ui.screens.privacy.PrivacyScreen
import com.Lia.assistant.ui.screens.profile.ProfileScreen
import com.Lia.assistant.ui.screens.quickactions.QuickActionsScreen
import com.Lia.assistant.ui.screens.settings.SettingsScreen
import com.Lia.assistant.ui.screens.voice.VoiceScreen
import com.Lia.assistant.ui.theme.*

// ── Route constants ───────────────────────────────────────────────────────────

object NavRoute {
    const val ONBOARDING   = "onboarding"
    const val HOME         = "home"
    const val VOICE        = "voice"
    const val CHAT         = "chat"
    const val HISTORY      = "history"
    const val SETTINGS     = "settings"
    const val QUICK_ACTIONS= "quick_actions"
    const val PROFILE      = "profile"
    const val PERSONALITY  = "personality"
    const val ORB_STYLE    = "orb_style"
    const val PERMISSIONS  = "permissions"
    const val PRIVACY      = "privacy"
    const val ABOUT        = "about"
    const val DEBUG        = "debug"
    const val FORGE        = "forge"
}

private val BOTTOM_NAV_ROUTES = setOf(
    NavRoute.HOME, NavRoute.CHAT, NavRoute.HISTORY, NavRoute.SETTINGS,
)

// ── NovaApp ───────────────────────────────────────────────────────────────────

@Composable
fun NovaApp(appState: NovaAppState) {
    val navController = rememberNavController()
    val backstackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backstackEntry?.destination?.route

    val showBottomBar  = currentRoute in BOTTOM_NAV_ROUTES
    val bottomInset    = if (showBottomBar) 80.dp else 0.dp

    CompositionLocalProvider(LocalBottomBarInset provides bottomInset) {
        Box(modifier = Modifier.fillMaxSize().nightSky()) {
            Scaffold(
                containerColor       = Color.Transparent,
                bottomBar = {
                    if (showBottomBar) {
                        LiaBottomNavigation(
                            currentRoute  = currentRoute,
                            navController = navController,
                            appState      = appState,
                        )
                    }
                },
            ) { innerPadding ->
                NavHost(
                    navController    = navController,
                    startDestination = if (appState.hasOnboarded()) NavRoute.HOME else NavRoute.ONBOARDING,
                    modifier         = Modifier.padding(innerPadding),
                ) {
                    composable(NavRoute.ONBOARDING) {
                        OnboardingScreen(onFinished = {
                            navController.navigate(NavRoute.HOME) {
                                popUpTo(NavRoute.ONBOARDING) { inclusive = true }
                            }
                        })
                    }
                    composable(NavRoute.HOME)      { HomeScreen(appState, navController) }
                    composable(NavRoute.VOICE)     { VoiceScreen(onClose = { navController.popBackStack() }) }
                    composable(NavRoute.CHAT)      { ChatScreen(onBack = { navController.popBackStack() }) }
                    composable(NavRoute.HISTORY)   { HistoryScreen(appState) }
                    composable(NavRoute.SETTINGS)  { SettingsScreen(appState, navController) }
                    composable(NavRoute.QUICK_ACTIONS) { QuickActionsScreen(navController) }
                    composable(NavRoute.PROFILE)   { ProfileScreen(appState, navController) }
                    composable(NavRoute.PERSONALITY) { PersonalityScreen(appState, onBack = { navController.popBackStack() }) }
                    composable(NavRoute.ORB_STYLE) { OrbStyleScreen(appState, onBack = { navController.popBackStack() }) }
                    composable(NavRoute.PERMISSIONS){ PermissionsScreen(onBack = { navController.popBackStack() }) }
                    composable(NavRoute.PRIVACY)   { PrivacyScreen(onBack = { navController.popBackStack() }) }
                    composable(NavRoute.ABOUT)     { AboutScreen(onBack = { navController.popBackStack() }) }
                    composable(NavRoute.DEBUG)     { DebugScreen(onBack = { navController.popBackStack() }) }
                    composable(NavRoute.FORGE + "?desc={desc}", 
                        arguments = listOf(androidx.navigation.navArgument("desc") { defaultValue = "" })) { entry ->
                        val desc = entry.arguments?.getString("desc") ?: ""
                        ForgeScreen(description = desc, onBack = { navController.popBackStack() })
                    }
                }
            }

            // Forge navigation trigger
            val pendingForge by ForgeController.pendingOpen.collectAsState()
            LaunchedEffect(pendingForge) {
                pendingForge?.let {
                    navController.navigate(NavRoute.FORGE) { launchSingleTop = true }
                    ForgeController.consume()
                }
            }
        }
    }
}

// ── Extension helpers ─────────────────────────────────────────────────────────

private fun NovaAppState.hasOnboarded(): Boolean = true
    // Part 11: replace with real DataStore/SharedPrefs check once appState exposes applicationContext.

// ── LiaBottomNavigation ───────────────────────────────────────────────────────

@Composable
fun LiaBottomNavigation(
    currentRoute  : String?,
    navController : NavController,
    appState      : NovaAppState,
) {
    val c = novaColors()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(NovaShapes.pill)
                .background(c.surfaceGlass)
                .padding(horizontal = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NavTab(Icons.Default.Home,      "Home",     NavRoute.HOME,     currentRoute, navController)
            NavTab(Icons.Default.Chat,      "Chat",     NavRoute.CHAT,     currentRoute, navController)

            // Centre Talk orb — launches Voice Mode
            IconButton(
                onClick  = {
                    navController.navigate(NavRoute.VOICE) { launchSingleTop = true }
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(NovaShapes.pill)
                    .background(c.accent),
            ) {
                Icon(Icons.Default.Mic, "Voice", tint = c.onAccent, modifier = Modifier.size(26.dp))
            }

            NavTab(Icons.Default.History,   "History",  NavRoute.HISTORY,  currentRoute, navController)
            NavTab(Icons.Default.Settings,  "Settings", NavRoute.SETTINGS, currentRoute, navController)
        }
    }
}

@Composable
private fun NavTab(
    icon         : ImageVector,
    label        : String,
    route        : String,
    currentRoute : String?,
    navController: NavController,
) {
    val c       = novaColors()
    val selected= currentRoute == route
    val tint    = if (selected) c.accent else c.textTertiary

    IconButton(
        onClick = {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState    = true
            }
        },
        modifier = Modifier.size(48.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(22.dp))
            if (selected) {
                Box(modifier = Modifier
                    .size(4.dp)
                    .clip(NovaShapes.pill)
                    .background(c.accent))
            }
        }
    }
}
