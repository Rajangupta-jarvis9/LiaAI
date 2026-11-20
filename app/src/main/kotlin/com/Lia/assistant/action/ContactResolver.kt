package com.Lia.assistant.action

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/**
 * Resolves a user-supplied name or phone number to a dialable number.
 *
 * ── Phone number detection ────────────────────────────────────────────────────
 * If [query] contains only digits, spaces, +, -, (, ), . and has ≥ 5 digits,
 * it is treated as a phone number and returned directly without a Contacts query.
 *
 * ── Ranking ───────────────────────────────────────────────────────────────────
 * 1. Exact name match (case-insensitive).
 * 2. Name starts with query.
 * 3. Name contains query.
 *
 * Up to 5 rows are fetched; the highest-ranked row is returned.
 */
object ContactResolver {

    sealed class Result {
        data class Found(val displayName: String, val number: String) : Result()
        object NoPermission : Result()
        object NotFound     : Result()
    }

    private val PHONE_PATTERN = Regex("""^[\d\s+\-().]+$""")
    private val DIGIT_PATTERN = Regex("""\d""")

    fun lookup(context: Context, query: String): Result {
        val q = query.trim()
        if (q.isBlank()) return Result.NotFound

        // Direct phone number — skip Contacts.
        if (PHONE_PATTERN.matches(q) && DIGIT_PATTERN.findAll(q).count() >= 5) {
            val digits = q.filter { it.isDigit() || it == '+' }
            return Result.Found(q, digits)
        }

        // Require READ_CONTACTS.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) return Result.NoPermission

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        val selection  = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args       = arrayOf("%$q%")
        val sortOrder  = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT 5"

        data class Row(val name: String, val number: String, val rank: Int)
        val rows = mutableListOf<Row>()

        context.contentResolver.query(uri, projection, selection, args, sortOrder)?.use { cursor ->
            val nameIdx   = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name   = cursor.getString(nameIdx)  ?: continue
                val number = cursor.getString(numberIdx) ?: continue
                val rank = when {
                    name.equals(q, ignoreCase = true)      -> 0
                    name.startsWith(q, ignoreCase = true)  -> 1
                    else                                    -> 2
                }
                rows += Row(name, number, rank)
            }
        }

        val best = rows.minByOrNull { it.rank } ?: return Result.NotFound
        return Result.Found(best.name, best.number.filter { it.isDigit() || it == '+' })
    }
}
