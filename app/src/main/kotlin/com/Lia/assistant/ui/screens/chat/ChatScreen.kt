package com.Lia.assistant.ui.screens.chat

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Lia.assistant.data.AssistantBrand
import com.Lia.assistant.voice.ChatTurn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// ── Colour tokens (Part 09 replaces with full design system) ─────────────────
private val BgDeep        = Color(0xFF06050E)
private val Surface1      = Color(0xFF100F1C)
private val Surface2      = Color(0xFF1A1929)
private val Marigold      = Color(0xFFFFB547)
private val Lagoon        = Color(0xFF3FE0D0)
private val TextPrimary   = Color(0xFFEAE8FF)
private val TextSecondary = Color(0xFFA7A0CC)

@Composable
fun ChatScreen(
    onBack      : () -> Unit = {},
    viewModel   : ChatViewModel = viewModel(),
) {
    val context   = LocalContext.current
    val messages  by viewModel.messages.collectAsStateWithLifecycle()
    val isTyping  by viewModel.isTyping.collectAsStateWithLifecycle()

    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()
    var inputText  by remember { mutableStateOf("") }

    // TTS — created once, shut down on dispose.
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) {
        val t = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
        }
        tts = t
        onDispose { t.stop(); t.shutdown() }
    }

    // Auto-scroll to bottom when new messages arrive.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = BgDeep) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface1)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSecondary)
                }
                Spacer(Modifier.width(8.dp))
                // Real 3D orb composable
                com.Lia.assistant.ui.fx.NovaOrb(
                    state    = if (isTyping) com.Lia.assistant.voice.NovaOrbState.THINKING
                               else com.Lia.assistant.voice.NovaOrbState.IDLE,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(AssistantBrand.NAME, color = TextPrimary,   fontSize = 16.sp)
                    Text(
                        if (isTyping) "Typing…" else "Here to help",
                        color    = TextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }

            // ── Message list ──────────────────────────────────────────────────
            LazyColumn(
                state          = listState,
                modifier       = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                itemsIndexed(messages) { index, turn ->
                    NovaMessageBubble(
                        turn     = turn,
                        onCopy   = { /* handled inside */ },
                        onRetry  = if (turn.isUser) {{ viewModel.retry(context, index) }} else null,
                        onSpeak  = { tts?.speak(turn.text, TextToSpeech.QUEUE_FLUSH, null, null) },
                    )
                }
                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // ── Input bar ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface1)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value         = inputText,
                    onValueChange = { inputText = it },
                    modifier      = Modifier.weight(1f),
                    placeholder   = { Text("Message…", color = TextSecondary) },
                    colors        = TextFieldDefaults.colors(
                        focusedContainerColor   = Surface2,
                        unfocusedContainerColor = Surface2,
                        focusedTextColor        = TextPrimary,
                        unfocusedTextColor      = TextPrimary,
                        cursorColor             = Marigold,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    shape         = RoundedCornerShape(24.dp),
                    maxLines      = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.send(context, inputText)
                            inputText = ""
                        }
                    }),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick  = {
                        if (inputText.isNotBlank()) {
                            viewModel.send(context, inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Marigold),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = BgDeep,
                    )
                }
            }
        }
    }
}

// ── Message bubble ────────────────────────────────────────────────────────────

@Composable
fun NovaMessageBubble(
    turn    : ChatTurn,
    onCopy  : () -> Unit,
    onRetry : (() -> Unit)?,
    onSpeak : () -> Unit,
) {
    val clipboard   = LocalClipboardManager.current
    val isUser      = turn.isUser
    val bubbleColor = if (isUser) Marigold.copy(alpha = 0.15f) else Surface2
    val alignment   = if (isUser) Arrangement.End else Arrangement.Start

    var displayText by remember(turn.text) { mutableStateOf(if (isUser) turn.text else "") }

    // Word-by-word reveal for assistant messages.
    LaunchedEffect(turn.text, isUser) {
        if (!isUser) {
            val words = turn.text.split(" ")
            val buffer = StringBuilder()
            words.forEachIndexed { idx, word ->
                buffer.append(if (idx == 0) word else " $word")
                displayText = buffer.toString()
                delay(20L)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart    = 16.dp,
                        topEnd      = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd   = if (isUser) 4.dp  else 16.dp,
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Text(
                text      = displayText,
                color     = TextPrimary,
                fontSize  = 15.sp,
                lineHeight = 22.sp,
            )
            // Action row — copy, retry (user only), speak (assistant only).
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick  = { clipboard.setText(AnnotatedString(turn.text)) },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, "Copy", tint = TextSecondary, modifier = Modifier.size(14.dp))
                }
                onRetry?.let {
                    IconButton(onClick = it, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Refresh, "Retry", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
                if (!isUser) {
                    IconButton(onClick = onSpeak, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.VolumeUp, "Speak", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// ── Typing indicator ──────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    var dots by remember { mutableStateOf("•") }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400); dots = "• •"
            delay(400); dots = "• • •"
            delay(400); dots = "•"
        }
    }
    Row(modifier = Modifier.padding(start = 4.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Surface2)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(dots, color = Lagoon, fontSize = 18.sp)
        }
    }
}
