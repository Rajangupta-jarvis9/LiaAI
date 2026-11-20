package com.Lia.assistant.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.Lia.assistant.voice.VoiceSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// ── DataStore extension — one instance per process ───────────────────────────
private val Context.assistantDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "assistant_settings")

/**
 * Single source of truth for personality, language, assistant name and voice name.
 *
 * Reads come from DataStore (cold Flow, survives process death).
 * Writes land in DataStore AND [NovaPreferences] (so legacy code that reads
 * SharedPreferences stays in sync) AND call [VoiceSessionManager.refreshInstructions]
 * so a live session picks up the change without a full reconnect.
 */
object PersonalityRepository {

    // ── DataStore keys ────────────────────────────────────────────────────────
    private val KEY_PERSONALITY     = stringPreferencesKey("personality")
    private val KEY_LANGUAGE        = stringPreferencesKey("language")
    private val KEY_ASSISTANT_NAME  = stringPreferencesKey("assistant_name")
    private val KEY_VOICE_NAME      = stringPreferencesKey("voice_name")

    // ── Flows ─────────────────────────────────────────────────────────────────

    fun personalityFlow(context: Context): Flow<Personality> =
        context.assistantDataStore.data.map { prefs ->
            Personality.fromName(
                prefs[KEY_PERSONALITY]
                    ?: NovaPreferences.getPersonality(context)
            )
        }

    fun languageFlow(context: Context): Flow<LanguagePreference> =
        context.assistantDataStore.data.map { prefs ->
            LanguagePreference.fromName(
                prefs[KEY_LANGUAGE]
                    ?: NovaPreferences.getLanguage(context)
            )
        }

    fun assistantNameFlow(context: Context): Flow<String> =
        context.assistantDataStore.data.map { prefs ->
            prefs[KEY_ASSISTANT_NAME]
                ?: NovaPreferences.getAssistantName(context)
        }

    fun voiceNameFlow(context: Context): Flow<String> =
        context.assistantDataStore.data.map { prefs ->
            prefs[KEY_VOICE_NAME]
                ?: NovaPreferences.getVoiceName(context)
        }

    // ── Suspend getters (for one-shot reads) ─────────────────────────────────

    suspend fun getPersonality(context: Context): Personality =
        personalityFlow(context).first()

    suspend fun getLanguage(context: Context): LanguagePreference =
        languageFlow(context).first()

    suspend fun getAssistantName(context: Context): String =
        assistantNameFlow(context).first()

    suspend fun getVoiceName(context: Context): String =
        voiceNameFlow(context).first()

    // ── Setters ───────────────────────────────────────────────────────────────

    suspend fun setPersonality(context: Context, value: Personality) =
        withContext(Dispatchers.IO) {
            context.assistantDataStore.edit { it[KEY_PERSONALITY] = value.name }
            NovaPreferences.putString(context, NovaPreferences.Keys.personality, value.name)
            VoiceSessionManager.refreshInstructions(context)
        }

    suspend fun setLanguage(context: Context, value: LanguagePreference) =
        withContext(Dispatchers.IO) {
            context.assistantDataStore.edit { it[KEY_LANGUAGE] = value.name }
            NovaPreferences.putString(context, NovaPreferences.Keys.language, value.name)
            VoiceSessionManager.refreshInstructions(context)
        }

    suspend fun setAssistantName(context: Context, value: String) =
        withContext(Dispatchers.IO) {
            context.assistantDataStore.edit { it[KEY_ASSISTANT_NAME] = value.trim() }
            NovaPreferences.putString(context, NovaPreferences.Keys.assistant_name, value.trim())
            VoiceSessionManager.refreshInstructions(context)
        }

    suspend fun setVoiceName(context: Context, value: String) =
        withContext(Dispatchers.IO) {
            context.assistantDataStore.edit { it[KEY_VOICE_NAME] = value.trim() }
            NovaPreferences.putString(context, NovaPreferences.Keys.voice_name, value.trim())
            VoiceSessionManager.refreshInstructions(context)
        }
}
