package com.Lia.assistant.action

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Direct flavor — all tools including screen control and message auto-send.
 */
object ActionExecutor {

    suspend fun execute(
        context : Context,
        toolName: String,
        args    : JSONObject,
    ): JSONObject = when (toolName) {

        "open_app" -> {
            val appName = args.optString("app_name", "").trim()
            if (appName.isBlank()) JSONObject().put("error", "app_name is required")
            else {
                val cache = InstalledAppLabelCache.get(context)
                val pkg   = AppLauncher.findApp(appName, cache)
                if (pkg != null) JSONObject().put("status", AppLauncher.launch(context, pkg)).put("package", pkg)
                else             JSONObject().put("status", "not_found").put("query", appName)
            }
        }

        "call_contact" -> {
            val name = args.optString("contact_name", "").trim()
            when (val r = ContactResolver.lookup(context, name)) {
                is ContactResolver.Result.Found ->
                    JSONObject().put("status", CommIntents.callContact(context, r.number))
                        .put("contact", r.displayName)
                is ContactResolver.Result.NoPermission ->
                    JSONObject().put("status", "no_contacts_permission")
                is ContactResolver.Result.NotFound ->
                    JSONObject().put("status", "contact_not_found").put("query", name)
            }
        }

        "message_contact" -> {
            val name     = args.optString("contact_name", "").trim()
            val message  = args.optString("message", "").trim()
            val platform = args.optString("platform", "whatsapp").lowercase()
            when (val r = ContactResolver.lookup(context, name)) {
                is ContactResolver.Result.Found -> {
                    val openStatus = if (platform == "sms")
                        CommIntents.smsContact(context, r.number, message)
                    else
                        CommIntents.whatsAppContact(context, r.number, message)

                    val service = NovaAccessibilityService.instance
                    val sendStatus = if (service != null && openStatus == "composer_opened")
                        service.autoTapSend()
                    else
                        openStatus

                    JSONObject().put("status", sendStatus).put("contact", r.displayName)
                }
                is ContactResolver.Result.NoPermission ->
                    JSONObject().put("status", "no_contacts_permission")
                is ContactResolver.Result.NotFound ->
                    JSONObject().put("status", "contact_not_found").put("query", name)
            }
        }

        "read_screen" -> {
            val svc = NovaAccessibilityService.instance
                ?: return JSONObject().put("status", "accessibility_not_enabled")
            val lines = svc.readScreen()
            JSONObject().put("status", "ok").put("content", JSONArray(lines))
        }

        "tap_text" -> {
            val text = args.optString("text", "").trim()
            val svc  = NovaAccessibilityService.instance
                ?: return JSONObject().put("status", "accessibility_not_enabled")
            JSONObject().put("status", svc.tapText(text))
        }

        "type_text" -> {
            val text = args.optString("text", "").trim()
            val svc  = NovaAccessibilityService.instance
                ?: return JSONObject().put("status", "accessibility_not_enabled")
            JSONObject().put("status", svc.typeText(context, text))
        }

        "scroll_screen" -> {
            val dir = args.optString("direction", "down").trim()
            val svc = NovaAccessibilityService.instance
                ?: return JSONObject().put("status", "accessibility_not_enabled")
            JSONObject().put("status", svc.scrollScreen(dir))
        }

        "go_back" -> {
            val svc = NovaAccessibilityService.instance
                ?: return JSONObject().put("status", "accessibility_not_enabled")
            JSONObject().put("status", svc.goBack())
        }

        "go_home" -> {
            val svc = NovaAccessibilityService.instance
                ?: return JSONObject().put("status", "accessibility_not_enabled")
            JSONObject().put("status", svc.goHome())
        }

        "build_website" -> {
            // Forge wiring arrives in Part 14.
            JSONObject().put("result", "forge_started")
        }

        else -> JSONObject().put("error", "Unknown tool: $toolName")
    }
}
