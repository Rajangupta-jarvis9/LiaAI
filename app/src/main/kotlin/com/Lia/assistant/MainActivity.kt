package com.Lia.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.data.NovaPreferences
import com.Lia.assistant.data.ThemeMode
import com.Lia.assistant.ui.fx.ForgeController
import com.Lia.assistant.ui.fx.LiaSplash3D
import com.Lia.assistant.ui.fx.ListeningEdgeGlow
import com.Lia.assistant.ui.fx.SplashSession
import com.Lia.assistant.ui.navigation.NovaApp
import com.Lia.assistant.ui.theme.NovaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle intent extras (e.g. EXTRA_OPEN_FORGE from a notification or shortcut)
        intent?.getStringExtra(EXTRA_OPEN_FORGE)?.let { desc ->
            ForgeController.requestOpen(desc)
        }

        setContent {
            val context      = LocalContext.current
            val themeMode    = ThemeMode.valueOf(
                NovaPreferences.getString(context, NovaPreferences.Keys.theme_mode, "DARK")
            )
            val reducedMotion = NovaPreferences.isReducedMotion(context)
            val appState      = remember { NovaAppState(context) }

            NovaTheme(mode = themeMode, reducedMotion = reducedMotion) {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize()
                ) {
                    NovaApp(appState)
                    ListeningEdgeGlow()
                    if (!SplashSession.played) {
                        LiaSplash3D(
                            assistantName = appState.assistantName,
                            onFinished    = { /* splash flag set inside */ },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_FORGE)?.let { desc ->
            ForgeController.requestOpen(desc)
        }
    }

    companion object {
        const val EXTRA_OPEN_FORGE = "extra_open_forge"
    }
}
