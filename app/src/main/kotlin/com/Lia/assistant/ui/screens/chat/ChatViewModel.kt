package com.Lia.assistant.ui.screens.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Lia.assistant.data.ApiKeyStore
import com.Lia.assistant.data.PersonalityRepository
import com.Lia.assistant.voice.ChatPromptBuilder
import com.Lia.assistant.voice.ChatTurn
import com.Lia.assistant.voice.ForgeIntent
import com.Lia.assistant.voice.GeminiTextClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel : ViewModel() {

    // ── Exposed state ─────────────────────────────────────────────────────────

    private val _messages   = MutableStateFlow<List<ChatTurn>>(emptyList())
    val messages            : StateFlow<List<ChatTurn>> = _messages

    private val _isTyping   = MutableStateFlow(false)
    val isTyping            : StateFlow<Boolean> = _isTyping

    private val _forgeRequest = MutableStateFlow<ForgeIntent.ForgeRequest?>(null)
    val forgeRequest          : StateFlow<ForgeIntent.ForgeRequest?> = _forgeRequest

    // ── Send ──────────────────────────────────────────────────────────────────

    fun send(context: Context, userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isTyping.value) return

        // Intercept website-build requests before sending to Gemini.
        ForgeIntent.extract(trimmed)?.let { req ->
            appendMessage(ChatTurn(isUser = true,  text = trimmed))
            appendMessage(ChatTurn(isUser = false, text = "Opening the Forge… 🔥"))
            _forgeRequest.value = req
            return
        }

        appendMessage(ChatTurn(isUser = true, text = trimmed))

        viewModelScope.launch {
            _isTyping.value = true
            val reply = withContext(Dispatchers.IO) {
                val apiKey       = ApiKeyStore.getKey(context)
                val personality  = PersonalityRepository.getPersonality(context)
                val language     = PersonalityRepository.getLanguage(context)
                val name         = PersonalityRepository.getAssistantName(context)
                val instruction  = ChatPromptBuilder.build(personality, language, name)
                GeminiTextClient.reply(apiKey, instruction, _messages.value)
            }
            _isTyping.value = false
            when (reply) {
                is GeminiTextClient.ChatReplyResult.Success ->
                    appendMessage(ChatTurn(isUser = false, text = reply.text))
                is GeminiTextClient.ChatReplyResult.Error ->
                    appendMessage(ChatTurn(isUser = false, text = reply.message))
            }
        }
    }

    /** Retries the user message at [index] by re-sending it. */
    fun retry(context: Context, index: Int) {
        val turn = _messages.value.getOrNull(index) ?: return
        if (!turn.isUser) return
        // Drop this message and everything after it, then resend.
        _messages.value = _messages.value.take(index)
        send(context, turn.text)
    }

    fun clearForgeRequest() { _forgeRequest.value = null }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun appendMessage(turn: ChatTurn) {
        _messages.value = _messages.value + turn
    }
}
