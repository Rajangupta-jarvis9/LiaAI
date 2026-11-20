package com.Lia.assistant.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.fx.*
import com.Lia.assistant.ui.navigation.NavRoute
import com.Lia.assistant.ui.theme.*
import com.Lia.assistant.voice.VoiceSessionManager
import java.util.Calendar

@Composable
fun HomeScreen(appState: NovaAppState, navController: NavController) {
    val c    = novaColors()
    val name = appState.assistantName
    val orbState  by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = LocalBottomBarInset.current)
            .padding(horizontal = NovaSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Greeting ──────────────────────────────────────────────────────────
        Spacer(Modifier.height(NovaSpacing.xxl))
        Text(
            timeGreeting(),
            style = MaterialTheme.typography.headlineSmall,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(NovaSpacing.sm))
        Text(
            "I'm $name",
            style = MaterialTheme.typography.displaySmall,
            color = c.textPrimary,
        )

        Spacer(Modifier.height(NovaSpacing.xxxl))

        // ── 3D / Canvas Orb ───────────────────────────────────────────────────
        LiaOrb3D(
            state         = orbState,
            amplitude     = amplitude,
            style         = NovaOrbStyle.valueOf(appState.orbStyle),
            assistantName = name,
            modifier      = Modifier.size(200.dp),
        )

        Spacer(Modifier.height(NovaSpacing.xxxl))

        // ── Action buttons ────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md)) {
            NovaButton(
                text       = "Talk",
                onClick    = { navController.navigate(NavRoute.VOICE) },
                style      = NovaButtonStyle.PRIMARY,
                leadingIcon= Icons.Default.Mic,
                modifier   = Modifier.weight(1f),
            )
            NovaButton(
                text       = "Type",
                onClick    = { navController.navigate(NavRoute.CHAT) },
                style      = NovaButtonStyle.SECONDARY,
                leadingIcon= Icons.Default.Chat,
                modifier   = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(NovaSpacing.xl))

        // ── "Try saying" suggestions ──────────────────────────────────────────
        Text("Try saying", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
        Spacer(Modifier.height(NovaSpacing.md))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm)) {
            items(QUICK_SUGGESTIONS) { suggestion ->
                NovaGlassCard(
                    onClick        = { navController.navigate(NavRoute.CHAT) },
                    contentPadding = PaddingValues(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm),
                ) {
                    Text(
                        "\"$suggestion\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
            }
        }
    }
}

private val QUICK_SUGGESTIONS = listOf(
    "YouTube kholo",
    "Mummy ko call karo",
    "Ek cafe ki website banao",
    "Aaj ka weather kya hai",
    "Rahul ko WhatsApp karo",
)

private fun timeGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else      -> "Good evening"
    }
}
