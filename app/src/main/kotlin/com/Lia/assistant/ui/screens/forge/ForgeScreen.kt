package com.Lia.assistant.ui.screens.forge

import android.content.Context
import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.Lia.assistant.forge.*
import com.Lia.assistant.ui.components.*
import com.Lia.assistant.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ForgeScreen(
    description: String,
    onBack     : () -> Unit,
) {
    val context = LocalContext.current
    val c       = novaColors()
    val state   by ForgeController.state.collectAsStateWithLifecycle()

    // Auto-start when the screen opens with a new description.
    LaunchedEffect(description) {
        if (state.isIdle || state.prompt != description) {
            ForgeController.start(context, description)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        when {
            state.isDone && state.partialHtml.isNotBlank() -> {
                // ── Generated site preview ────────────────────────────────────
                GeneratedSiteView(html = state.partialHtml, modifier = Modifier.fillMaxSize())
                ResultToolbar(state = state, onBack = onBack, context = context)
            }

            state.isFailed -> {
                // ── Error state ───────────────────────────────────────────────
                Column(
                    modifier              = Modifier.fillMaxSize().padding(NovaSpacing.xl),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.Center,
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = c.error, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(NovaSpacing.lg))
                    Text(state.error ?: "Build failed.", style = MaterialTheme.typography.bodyLarge,
                         color = c.textPrimary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(NovaSpacing.xl))
                    Row(horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md)) {
                        NovaButton("Back",  onClick = onBack,  style = NovaButtonStyle.SECONDARY)
                        NovaButton("Retry", onClick = { ForgeController.retry(context) })
                    }
                }
            }

            state.isCancelled -> {
                // ── Cancelled ─────────────────────────────────────────────────
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Build cancelled", color = c.textSecondary)
                        Spacer(Modifier.height(NovaSpacing.lg))
                        NovaButton("Go back", onClick = onBack)
                    }
                }
            }

            else -> {
                // ── Building — show partial HTML + HUD ────────────────────────
                if (state.partialHtml.length > 4096) {
                    GeneratedSiteView(html = state.partialHtml,
                                      modifier = Modifier.fillMaxSize())
                } else {
                    ForgePlaceholder(stage = state.stage, modifier = Modifier.fillMaxSize())
                }
                ForgeHud(state = state, onCancel = { ForgeController.cancel(); onBack() })
            }
        }
    }
}

// ── ForgeHud ──────────────────────────────────────────────────────────────────

@Composable
private fun ForgeHud(
    state   : ForgeUiState,
    onCancel: () -> Unit,
) {
    val c = novaColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.background.copy(alpha = 0.88f))
            .statusBarsPadding()
            .padding(horizontal = NovaSpacing.lg, vertical = NovaSpacing.md),
    ) {
        // Stage label with animated transition
        AnimatedContent(targetState = state.stage.displayName, label = "stage") { name ->
            Text(name, style = MaterialTheme.typography.titleMedium, color = c.accent)
        }

        Spacer(Modifier.height(NovaSpacing.sm))

        // Stage track — 6 segments
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            ForgeStage.entries.forEachIndexed { idx, stage ->
                val filled = state.stage.ordinal >= idx
                Box(
                    modifier = Modifier
                        .weight(1f).height(4.dp)
                        .background(if (filled) c.accent else c.surfaceBorder,
                                    com.Lia.assistant.ui.theme.NovaShapes.pill),
                )
            }
        }

        Spacer(Modifier.height(NovaSpacing.sm))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${state.metrics.chars / 1024} KB", style = MaterialTheme.typography.labelSmall,
                 color = c.textTertiary)
            Text("${state.metrics.sections} sections", style = MaterialTheme.typography.labelSmall,
                 color = c.textTertiary)
            Text("${state.metrics.tokensPerSec.toInt()} t/s", style = MaterialTheme.typography.labelSmall,
                 color = c.textTertiary)
            Text("${state.metrics.elapsedSec.toInt()}s", style = MaterialTheme.typography.labelSmall,
                 color = c.textTertiary)
        }

        Spacer(Modifier.height(NovaSpacing.sm))

        NovaButton(
            text     = "Cancel",
            onClick  = onCancel,
            style    = NovaButtonStyle.SECONDARY,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        )
    }
}

// ── GeneratedSiteView ─────────────────────────────────────────────────────────

@Composable
fun GeneratedSiteView(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory  = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess   = false
                settings.allowContentAccess= false
                settings.mixedContentMode  = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view   : WebView,
                        request: android.webkit.WebResourceRequest,
                    ): Boolean {
                        // Open all external links in the system browser, not inside the WebView.
                        val uri = request.url ?: return true
                        val i   = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try { ctx.startActivity(i) } catch (_: Exception) {}
                        return true
                    }
                }
                loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
            }
        },
        update   = { webView ->
            webView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
        },
        modifier = modifier,
    )
}

// ── ResultToolbar ─────────────────────────────────────────────────────────────

@Composable
private fun ResultToolbar(
    state  : ForgeUiState,
    onBack : () -> Unit,
    context: Context,
) {
    val c = novaColors()
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(4000)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter   = slideInVertically { -it } + fadeIn(),
        exit    = slideOutVertically { -it } + fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surfaceGlass)
                .statusBarsPadding()
                .padding(horizontal = NovaSpacing.sm, vertical = NovaSpacing.sm),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.textSecondary)
            }
            Row {
                // Share
                state.file?.let { file ->
                    IconButton(onClick = {
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", file)
                        val share = Intent(Intent.ACTION_SEND)
                            .setType("text/html")
                            .putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(Intent.createChooser(share, "Share website"))
                    }) {
                        Icon(Icons.Default.Share, "Share", tint = c.textSecondary)
                    }
                }
                // Edit
                IconButton(onClick = { /* Edit dialog — Part 14 expansion */ }) {
                    Icon(Icons.Default.Edit, "Edit", tint = c.textSecondary)
                }
                // Rebuild
                IconButton(onClick = { ForgeController.retry(context) }) {
                    Icon(Icons.Default.Refresh, "Rebuild", tint = c.textSecondary)
                }
            }
            // Tap anywhere on the handle to bring toolbar back
        }
    }

    // Handle to bring toolbar back
    if (!visible) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp).height(4.dp)
                    .background(c.textTertiary, com.Lia.assistant.ui.theme.NovaShapes.pill)
                    .clickableNoRipple { visible = true },
            )
        }
    }
}

// ── Forge placeholder (shown before enough HTML to render) ────────────────────

@Composable
private fun ForgePlaceholder(stage: ForgeStage, modifier: Modifier = Modifier) {
    val c = novaColors()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = c.accent)
            Spacer(Modifier.height(NovaSpacing.lg))
            Text(stage.displayName, style = MaterialTheme.typography.bodyMedium,
                 color = c.textSecondary)
        }
    }
}

// ── Helper extension ──────────────────────────────────────────────────────────

@androidx.compose.runtime.Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.then(
        androidx.compose.foundation.clickable(
            interactionSource = interactionSource,
            indication        = null,
            onClick           = onClick,
        )
    )
}
