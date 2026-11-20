package com.Lia.assistant.data

/**
 * User's preferred response language.
 *
 * [AUTO] is the default: Lia detects each user turn individually and replies
 * in the same language — switching is seamless and requires no setting change.
 */
enum class LanguagePreference(
    val displayName : String,
    val description : String,
) {
    AUTO(
        displayName = "Auto",
        description = "Follows whatever you speak — Hindi, Hinglish or English, turn by turn.",
    ),
    HINDI(
        displayName = "Hindi",
        description = "Always replies in Hindi, regardless of how you speak.",
    ),
    HINGLISH(
        displayName = "Hinglish",
        description = "Always replies in the Hindi-English mix spoken across urban India.",
    ),
    ENGLISH(
        displayName = "English",
        description = "Always replies in English.",
    );

    companion object {
        fun fromName(name: String?): LanguagePreference =
            entries.firstOrNull { it.name == name } ?: AUTO
    }
}
