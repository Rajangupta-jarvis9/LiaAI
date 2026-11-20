package com.Lia.assistant.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Thin wrapper around the shared "nova_prefs" file.
 *
 * Every setting has a typed getter/setter pair.  All writes use `apply()`
 * so they are asynchronous and never block the main thread.
 */
object NovaPreferences {

    private const val PREFS_NAME = "nova_prefs"

    /** All preference keys in one place — prevents typo-driven bugs. */
    object Keys {
        const val theme_mode           = "theme_mode"
        const val reduced_motion       = "reduced_motion"
        const val orb_style            = "orb_style"
        const val personality          = "personality"
        const val user_name            = "user_name"
        const val voice_input_enabled  = "voice_input_enabled"
        const val wake_word_enabled    = "wake_word_enabled"
        const val language             = "language"
        const val has_onboarded        = "has_onboarded"
        const val voice_name           = "voice_name"
        const val response_style       = "response_style"
        const val speech_speed         = "speech_speed"
        const val voice_volume         = "voice_volume"
        const val assistant_name       = "assistant_name"
    }

    // ── Generic accessors ────────────────────────────────────────────────────

    fun getString(context: Context, key: String, default: String = ""): String =
        prefs(context).getString(key, default) ?: default

    fun putString(context: Context, key: String, value: String) =
        prefs(context).edit().putString(key, value).apply()

    fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean =
        prefs(context).getBoolean(key, default)

    fun putBoolean(context: Context, key: String, value: Boolean) =
        prefs(context).edit().putBoolean(key, value).apply()

    fun getFloat(context: Context, key: String, default: Float = 0f): Float =
        prefs(context).getFloat(key, default)

    fun putFloat(context: Context, key: String, value: Float) =
        prefs(context).edit().putFloat(key, value).apply()

    // ── Typed convenience helpers ────────────────────────────────────────────

    fun getAssistantName(context: Context): String =
        getString(context, Keys.assistant_name, AssistantBrand.NAME)

    fun getPersonality(context: Context): String =
        getString(context, Keys.personality, Personality.NORMAL.name)

    fun getLanguage(context: Context): String =
        getString(context, Keys.language, LanguagePreference.AUTO.name)

    fun getVoiceName(context: Context): String =
        getString(context, Keys.voice_name, "Aoede")

    fun getSpeechSpeed(context: Context): Float =
        getFloat(context, Keys.speech_speed, 1.0f)

    fun getVoiceVolume(context: Context): Float =
        getFloat(context, Keys.voice_volume, 1.0f)

    fun hasOnboarded(context: Context): Boolean =
        getBoolean(context, Keys.has_onboarded, false)

    fun isReducedMotion(context: Context): Boolean =
        getBoolean(context, Keys.reduced_motion, false)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
