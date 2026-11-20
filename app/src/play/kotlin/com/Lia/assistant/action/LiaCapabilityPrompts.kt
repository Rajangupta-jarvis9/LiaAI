package com.Lia.assistant.action

object LiaCapabilityPrompts {
    fun toolInstruction(assistantName: String): String = """
        You have three tools available: open_app, call_contact, and message_contact.
        - open_app: opens any installed app by name.
        - call_contact: dials a contact or phone number.
        - message_contact: pre-fills the SMS or WhatsApp composer so the user can tap Send.

        Important limitations in this build:
        - You cannot read what is on the screen.
        - You cannot tap, type, scroll or control the screen in any way.
        - You cannot post to Instagram, Facebook, or any other social app.
        - For messages, you always open the composer — you never send automatically.

        When a user asks for something you cannot do, say clearly what you can do instead.
    """.trimIndent()
}
