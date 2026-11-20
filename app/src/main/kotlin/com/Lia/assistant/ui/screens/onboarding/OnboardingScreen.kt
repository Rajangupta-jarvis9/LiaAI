package com.Lia.assistant.ui.screens.onboarding
import androidx.compose.foundation.layout.*; import androidx.compose.foundation.pager.*
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Alignment; import androidx.compose.ui.Modifier; import androidx.compose.ui.platform.LocalContext; import androidx.compose.ui.text.style.TextAlign; import androidx.compose.ui.unit.dp
import com.Lia.assistant.data.AssistantBrand; import com.Lia.assistant.data.NovaPreferences; import com.Lia.assistant.ui.components.*; import com.Lia.assistant.ui.fx.*; import com.Lia.assistant.ui.theme.*; import com.Lia.assistant.voice.NovaOrbState; import kotlinx.coroutines.launch
@Composable fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope(); val c = novaColors()
    val pagerState = rememberPagerState(pageCount = { 4 })
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Column(Modifier.fillMaxSize().padding(NovaSpacing.xl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                NovaOrb(state = NovaOrbState.LISTENING, style = NovaOrbStyle.ENERGY, modifier = Modifier.size(120.dp))
                Spacer(Modifier.height(32.dp))
                val (title, body) = ONBOARDING_PAGES[page]
                Text(title, style = MaterialTheme.typography.headlineSmall, color = c.textPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Center)
            }
        }
        NovaButton(if (pagerState.currentPage < 3) "Next" else "Get Started", onClick = {
            if (pagerState.currentPage < 3) { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
            else { NovaPreferences.putBoolean(context, NovaPreferences.Keys.has_onboarded, true); onFinished() }
        }, modifier = Modifier.padding(NovaSpacing.xl).fillMaxWidth())
    }
}
private val ONBOARDING_PAGES = listOf("Meet ${AssistantBrand.NAME}" to "Your personal AI that talks, types and takes action on your phone.", "Talk to it" to "Full-duplex voice in Hindi, Hinglish or English — switch mid-sentence.", "Does things" to "Open apps, call contacts, send messages, browse the web.", "Build websites" to "Say \"ek cafe ki website banao\" and watch it build live in 3D.")
    .map { it }
