package com.Lia.assistant.action

import org.json.JSONArray

/**
 * Play flavor — 3 tools only.
 * Screen control, social agents and accessibility are excluded at compile time.
 */
object LiaToolCatalog {
    fun declarations(): JSONArray = JSONArray().apply {
        put(functionDecl(
            name        = "open_app",
            description = "Open an installed app by name. Use when the user says 'open', 'launch', or names an app.",
            params      = listOf("app_name" to "The name of the app to open, e.g. YouTube, WhatsApp, Camera."),
            required    = listOf("app_name"),
        ))
        put(functionDecl(
            name        = "call_contact",
            description = "Call a phone contact by name or dial a phone number.",
            params      = listOf("contact_name" to "Contact name as the user said it, e.g. Mummy, Rahul, or a phone number."),
            required    = listOf("contact_name"),
        ))
        put(functionDecl(
            name        = "message_contact",
            description = "Prepare a message for a contact. Opens the SMS or WhatsApp composer — the user taps Send.",
            params      = listOf(
                "contact_name" to "Contact name or phone number.",
                "message"      to "The message text to pre-fill.",
                "platform"     to "Preferred platform: 'whatsapp' or 'sms'. Default 'whatsapp'.",
            ),
            required    = listOf("contact_name", "message"),
        ))
    }
}
