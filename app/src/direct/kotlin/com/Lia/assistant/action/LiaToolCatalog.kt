package com.Lia.assistant.action

import org.json.JSONArray

/**
 * Direct flavor — all 10 tools including screen control and website forge.
 */
object LiaToolCatalog {
    fun declarations(): JSONArray = JSONArray().apply {
        put(functionDecl("open_app",        "Open an installed app by name.",
            listOf("app_name" to "The app name, e.g. YouTube, Camera."), listOf("app_name")))
        put(functionDecl("call_contact",    "Call a contact or phone number.",
            listOf("contact_name" to "Contact name or phone number."), listOf("contact_name")))
        put(functionDecl("message_contact", "Send or prepare a message to a contact.",
            listOf("contact_name" to "Contact name or phone number.",
                   "message"      to "The message text.",
                   "platform"     to "whatsapp or sms. Default whatsapp."),
            listOf("contact_name", "message")))
        put(functionDecl("read_screen",     "Read the text visible on screen right now.",
            emptyList(), emptyList()))
        put(functionDecl("tap_text",        "Tap a UI element that shows specific text.",
            listOf("text" to "Exact or near-exact label of the element to tap."), listOf("text")))
        put(functionDecl("type_text",       "Type text into the focused input field.",
            listOf("text" to "The text to type."), listOf("text")))
        put(functionDecl("scroll_screen",   "Scroll the screen in a direction.",
            listOf("direction" to "up, down, left or right."), listOf("direction")))
        put(functionDecl("go_back",         "Press the system Back button.", emptyList(), emptyList()))
        put(functionDecl("go_home",         "Go to the home screen.", emptyList(), emptyList()))
        put(functionDecl("build_website",   "Build an animated website from a description.",
            listOf("description" to "What the website is for, e.g. a cafe in Bandra."),
            listOf("description")))
    }
}
