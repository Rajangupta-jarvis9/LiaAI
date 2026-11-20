package com.Lia.assistant.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Compose-observable application state.
 *
 * A single instance is created with [remember] at the navigation root and
 * passed down the composable tree.  Every field is a [mutableStateOf] so
 * recomposition is scoped to the composable that reads it.
 *
 * Naming convention: mutators are `applyX()` (not `setX()`) to avoid
 * JVM property-clash with Compose's generated `setX` accessors.
 */
class NovaAppState(context: Context) {

    private val appContext = context.applicationContext
    private val ioScope    = CoroutineScope(Dispatchers.IO)

    // ── Theme ─────────────────────────────────────────────────────────────────

    var themeMode     by mutableStateOf(
        NovaPreferences.getString(appContext, NovaPreferences.Keys.theme_mode, "DARK")
    ); private set

    var reducedMotion by mutableStateOf(
        NovaPreferences.isReducedMotion(appContext)
    ); private set

    var orbStyle      by mutableStateOf(
        NovaPreferences.getString(appContext, NovaPreferences.Keys.orb_style, "ENERGY")
    ); private set

    fun applyThemeMode(value: String) {
        themeMode = value
        NovaPreferences.putString(appContext, NovaPreferences.Keys.theme_mode, value)
    }

    fun applyReducedMotion(value: Boolean) {
        reducedMotion = value
        NovaPreferences.putBoolean(appContext, NovaPreferences.Keys.reduced_motion, value)
    }

    fun applyOrbStyle(value: String) {
        orbStyle = value
        NovaPreferences.putString(appContext, NovaPreferences.Keys.orb_style, value)
    }

    // ── Identity ──────────────────────────────────────────────────────────────

    var assistantName by mutableStateOf(
        NovaPreferences.getAssistantName(appContext)
    ); private set

    var userName by mutableStateOf(
        NovaPreferences.getString(appContext, NovaPreferences.Keys.user_name, "")
    ); private set

    fun applyAssistantName(value: String) {
        assistantName = value
        ioScope.launch {
            PersonalityRepository.setAssistantName(appContext, value)
        }
    }

    fun applyUserName(value: String) {
        userName = value
        NovaPreferences.putString(appContext, NovaPreferences.Keys.user_name, value)
    }

    // ── Personality & language ────────────────────────────────────────────────

    var personality by mutableStateOf(
        Personality.fromName(NovaPreferences.getPersonality(appContext))
    ); private set

    var language by mutableStateOf(
        LanguagePreference.fromName(NovaPreferences.getLanguage(appContext))
    ); private set

    fun applyPersonality(value: Personality) {
        personality = value
        ioScope.launch {
            PersonalityRepository.setPersonality(appContext, value)
        }
    }

    fun applyLanguage(value: LanguagePreference) {
        language = value
        ioScope.launch {
            PersonalityRepository.setLanguage(appContext, value)
        }
    }

    // ── Voice settings ────────────────────────────────────────────────────────

    var voiceName by mutableStateOf(
        NovaPreferences.getVoiceName(appContext)
    ); private set

    var speechSpeed by mutableStateOf(
        NovaPreferences.getSpeechSpeed(appContext)
    ); private set

    var voiceVolume by mutableStateOf(
        NovaPreferences.getVoiceVolume(appContext)
    ); private set

    var voiceInputEnabled by mutableStateOf(
        NovaPreferences.getBoolean(appContext, NovaPreferences.Keys.voice_input_enabled, true)
    ); private set

    fun applyVoiceName(value: String) {
        voiceName = value
        ioScope.launch {
            PersonalityRepository.setVoiceName(appContext, value)
        }
    }

    fun applySpeechSpeed(value: Float) {
        speechSpeed = value
        NovaPreferences.putFloat(appContext, NovaPreferences.Keys.speech_speed, value)
    }

    fun applyVoiceVolume(value: Float) {
        voiceVolume = value
        NovaPreferences.putFloat(appContext, NovaPreferences.Keys.voice_volume, value)
    }

    fun applyVoiceInputEnabled(value: Boolean) {
        voiceInputEnabled = value
        NovaPreferences.putBoolean(appContext, NovaPreferences.Keys.voice_input_enabled, value)
    }

    // ── Conversation history (in-memory, for the History screen) ─────────────

    /** Lightweight summary items shown on the History screen.  Not persisted in Part 02. */
    data class ConversationSummary(
        val id         : String,
        val title      : String,
        val preview    : String,
        val timestampMs: Long,
    )

    val conversations = mutableStateListOf<ConversationSummary>()

    fun addConversation(summary: ConversationSummary) {
        conversations.add(0, summary)
        if (conversations.size > 50) conversations.removeAt(conversations.lastIndex)
    }

    fun clearConversations() = conversations.clear()
}
