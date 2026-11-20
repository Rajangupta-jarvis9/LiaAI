package com.Lia.assistant.action

import android.content.Context
import org.json.JSONObject

/**
 * Play flavor — executes open_app, call_contact and message_contact only.
 */
object ActionExecutor {

    suspend fun execute(
        context : Context,
        toolName: String,
        args    : JSONObject,
    ): JSONObject = when (toolName) {

        "open_app" -> {
            val appName = args.optString("app_name", "").trim()
            if (appName.isBlank()) {
                JSONObject().put("error", "app_name is required")
            } else {
                val cache = InstalledAppLabelCache.get(context)
                val pkg   = AppLauncher.findApp(appName, cache)
                if (pkg != null) {
                    val result = AppLauncher.launch(context, pkg)
                    JSONObject().put("status", result).put("package", pkg)
                } else {
                    JSONObject().put("status", "not_found").put("query", appName)
                }
            }
        }

        "call_contact" -> {
            val name = args.optString("contact_name", "").trim()
            when (val resolved = ContactResolver.lookup(context, name)) {
                is ContactResolver.Result.Found ->
                    JSONObject().put("status", CommIntents.callContact(context, resolved.number))
                        .put("contact", resolved.displayName)
                is ContactResolver.Result.NoPermission ->
                    JSONObject().put("status", "no_contacts_permission")
                is ContactResolver.Result.NotFound ->
                    JSONObject().put("status", "contact_not_found").put("query", name)
            }
        }

        "message_contact" -> {
            val name     = args.optString("contact_name", "").trim()
            val message  = args.optString("message", "").trim()
            val platform = args.optString("platform", "whatsapp").trim().lowercase()
            when (val resolved = ContactResolver.lookup(context, name)) {
                is ContactResolver.Result.Found -> {
                    val status = if (platform == "sms") {
                        CommIntents.smsContact(context, resolved.number, message)
                    } else {
                        CommIntents.whatsAppContact(context, resolved.number, message)
                    }
                    JSONObject().put("status", status).put("contact", resolved.displayName)
                }
                is ContactResolver.Result.NoPermission ->
                    JSONObject().put("status", "no_contacts_permission")
                is ContactResolver.Result.NotFound ->
                    JSONObject().put("status", "contact_not_found").put("query", name)
            }
        }

        else -> JSONObject().put("error", "Unknown tool: $toolName")
    }
}
