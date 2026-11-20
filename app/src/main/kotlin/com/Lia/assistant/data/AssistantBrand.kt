package com.Lia.assistant.data

/**
 * Compile-time identity for Lia AI.
 *
 * These values are used wherever the assistant refers to itself.
 * The user can override [AssistantBrand.NAME] at runtime via [NovaPreferences.Keys.assistant_name].
 */
object AssistantBrand {
    const val NAME      = "Lia"
    const val FULL_NAME = "Lia AI"
    const val TAGLINE   = "Your Intelligent AI Companion"
}
