package com.Lia.assistant.data

import android.content.Context

/**
 * Stores and retrieves the user-supplied Gemini API key.
 *
 * Rules enforced here:
 * - The key is NEVER hardcoded.
 * - The key is NEVER written to logcat.
 * - The key is stored in private SharedPreferences only.
 * - Callers receive an empty string when no key has been saved.
 */
object ApiKeyStore {

    private const val PREFS_NAME = "nova_prefs"
    private const val KEY        = "gemini_api_key"

    /** Returns the stored key, or an empty string if none has been saved. */
    fun getKey(context: Context): String =
        prefs(context).getString(KEY, "") ?: ""

    /**
     * Saves a trimmed copy of [key].
     * Passing a blank string effectively clears the key.
     */
    fun saveKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY, key.trim()).apply()
    }

    /** Returns true when a non-blank key exists. */
    fun hasKey(context: Context): Boolean = getKey(context).isNotBlank()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
