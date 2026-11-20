package com.Lia.assistant.license

import android.content.Context
import com.Lia.assistant.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Direct flavor — access-key gate backed by a remote validation endpoint.
 *
 * Backend configuration is read from a git-ignored file. When the file is
 * absent the manager reports "licensing not configured" and fails open.
 *
 * Prefs schema (private SharedPreferences "lia_license"):
 *   lia_access_key, plan, expires_at, active, blocked, last_remote_check_ms
 */
object AccessKeyManager {

    private const val PREFS_NAME        = "lia_license"
    private const val K_KEY             = "lia_access_key"
    private const val K_PLAN            = "plan"
    private const val K_EXPIRES         = "expires_at"
    private const val K_ACTIVE          = "active"
    private const val K_BLOCKED         = "blocked"
    private const val K_LAST_CHECK      = "last_remote_check_ms"
    private const val RECHECK_INTERVAL  = 3 * 60 * 60 * 1000L   // 3 hours

    private val _licenseState = MutableStateFlow(
        LicenseInfo(active = false, plan = "", expiresAt = 0L,
                    status = "Checking…", blocked = false, hasKey = false)
    )
    val licenseState: StateFlow<LicenseInfo> = _licenseState

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun refreshInfo(context: Context) = withContext(Dispatchers.IO) {
        val p   = prefs(context)
        val key = p.getString(K_KEY, "") ?: ""
        val info = AccessKeyRules.decide(
            key        = key,
            plan       = p.getString(K_PLAN, "")       ?: "",
            expiresAtMs= p.getLong(K_EXPIRES, 0L),
            active     = p.getBoolean(K_ACTIVE, false),
            blocked    = p.getBoolean(K_BLOCKED, false),
        )
        _licenseState.value = info
    }

    suspend fun liveCheck(context: Context) = withContext(Dispatchers.IO) {
        val p         = prefs(context)
        val lastCheck = p.getLong(K_LAST_CHECK, 0L)
        val now       = System.currentTimeMillis()
        if (now - lastCheck < RECHECK_INTERVAL) return@withContext   // too soon

        val key = p.getString(K_KEY, "") ?: ""
        if (key.isBlank()) return@withContext

        val backendUrl = backendUrl(context) ?: return@withContext          // config missing — fail open

        runCatching {
            val body = JSONObject().put("key", key).put("device", deviceId(context))
                .toString().toRequestBody("application/json".toMediaType())
            val req  = Request.Builder().url("$backendUrl/check").post(body).build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching
                val json    = JSONObject(resp.body?.string() ?: return@runCatching)
                val active  = json.optBoolean("active", true)
                val blocked = json.optBoolean("blocked", false)
                val plan    = json.optString("plan", p.getString(K_PLAN, "") ?: "")
                val expires = json.optLong("expires_at", p.getLong(K_EXPIRES, 0L))
                p.edit()
                    .putBoolean(K_ACTIVE,  active)
                    .putBoolean(K_BLOCKED, blocked)
                    .putString(K_PLAN,     plan)
                    .putLong(K_EXPIRES,    expires)
                    .putLong(K_LAST_CHECK, now)
                    .apply()
                refreshInfo(context)
            }
        }
        // Network failure → fail open (keep last known state)
    }

    suspend fun activate(context: Context, key: String): Boolean = withContext(Dispatchers.IO) {
        val backendUrl = backendUrl(context) ?: run {
            // No backend configured — store key as-is and mark active (dev mode).
            prefs(context).edit()
                .putString(K_KEY, key.trim())
                .putBoolean(K_ACTIVE, true)
                .putString(K_PLAN, "dev")
                .apply()
            refreshInfo(context)
            return@withContext true
        }

        runCatching {
            val body = JSONObject().put("key", key.trim()).put("device", deviceId(context))
                .toString().toRequestBody("application/json".toMediaType())
            val req  = Request.Builder().url("$backendUrl/activate").post(body).build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching false
                val json = JSONObject(resp.body?.string() ?: return@runCatching false)
                val ok   = json.optBoolean("active", false)
                if (ok) {
                    prefs(context).edit()
                        .putString(K_KEY,      key.trim())
                        .putBoolean(K_ACTIVE,  true)
                        .putBoolean(K_BLOCKED, false)
                        .putString(K_PLAN,     json.optString("plan", "basic"))
                        .putLong(K_EXPIRES,    json.optLong("expires_at", 0L))
                        .putLong(K_LAST_CHECK, System.currentTimeMillis())
                        .apply()
                    refreshInfo(context)
                }
                ok
            }
        }.getOrDefault(false)
    }

    fun isActive(context: Context): Boolean = _licenseState.value.active

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Reads the backend URL from string resource `lia_backend_url` with an empty string fallback. */
    private fun backendUrl(context: Context): String? = runCatching {
        val resId = context.resources.getIdentifier("lia_backend_url", "string", context.packageName)
        val url = if (resId != 0) {
            context.getString(resId).trim()
        } else {
            runCatching { context.getString(R.string.lia_backend_url).trim() }.getOrDefault("")
        }
        if (url.isNotBlank()) url.removeSuffix("/") else null
    }.getOrNull()

    private fun deviceId(context: Context): String =
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
        ) ?: "unknown"
}
