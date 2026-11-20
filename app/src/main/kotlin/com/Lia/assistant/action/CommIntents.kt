package com.Lia.assistant.action

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

/**
 * Fires system intents for calling, SMS and WhatsApp.
 *
 * ── Play-build rules (enforced here) ─────────────────────────────────────────
 * - Calls: ACTION_CALL when CALL_PHONE granted, otherwise ACTION_DIAL (user confirms).
 * - Messages: ACTION_SENDTO or WhatsApp share URL — opens the COMPOSER only.
 *   SmsManager and SEND_SMS are NEVER used; the user taps Send themselves.
 */
object CommIntents {

    private val NEW_TASK = Intent.FLAG_ACTIVITY_NEW_TASK

    // ── Call ─────────────────────────────────────────────────────────────────

    /**
     * @return "calling" | "dialer_opened_no_permission" | "error"
     */
    fun callContact(context: Context, number: String): String {
        val cleanNumber = number.filter { it.isDigit() || it == '+' }
        return if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(context, Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")))
            "calling"
        } else {
            startActivity(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")))
            "dialer_opened_no_permission"
        }
    }

    // ── SMS ──────────────────────────────────────────────────────────────────

    /**
     * Opens the default SMS composer pre-filled with [body].
     * @return "composer_opened" | "error"
     */
    fun smsContact(context: Context, number: String, body: String): String {
        val uri    = Uri.parse("smsto:${number.filter { it.isDigit() || it == '+' }}")
        val intent = Intent(Intent.ACTION_SENDTO, uri)
            .putExtra("sms_body", body)
            .addFlags(NEW_TASK)
        return try {
            context.startActivity(intent)
            "composer_opened"
        } catch (e: ActivityNotFoundException) {
            "error"
        }
    }

    // ── WhatsApp ─────────────────────────────────────────────────────────────

    /**
     * Opens the WhatsApp chat composer for [number] pre-filled with [text].
     * Tries the native WhatsApp package intent first; falls back to the wa.me URL.
     * @return "composer_opened" | "error"
     */
    fun whatsAppContact(context: Context, number: String, text: String): String {
        val digits  = number.filter { it.isDigit() }
        val encoded = Uri.encode(text)

        // Try native packages first (regular or Business).
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            if (isPackageInstalled(context, pkg)) {
                val intent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/$digits?text=$encoded"))
                    .setPackage(pkg)
                    .addFlags(NEW_TASK)
                return try {
                    context.startActivity(intent)
                    "composer_opened"
                } catch (_: ActivityNotFoundException) { continue }
            }
        }

        // Fallback: open in browser / any handler.
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=$encoded"))
                    .addFlags(NEW_TASK)
            )
            "composer_opened"
        } catch (e: ActivityNotFoundException) {
            "error"
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun startActivity(context: Context, intent: Intent) {
        context.startActivity(intent.addFlags(NEW_TASK))
    }

    private fun isPackageInstalled(context: Context, pkg: String): Boolean = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }
}
