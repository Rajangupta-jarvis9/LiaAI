package com.Lia.assistant.action

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Process-wide cache mapping lowercase app labels to package names.
 *
 * Built once on IO; expires after [TTL_MS] (15 minutes) and rebuilt on the
 * next [get] call.  Thread-safe via [mutex].
 *
 * Only launchable apps (those with a MAIN/LAUNCHER intent filter) are indexed.
 */
object InstalledAppLabelCache {

    private const val TTL_MS = 15 * 60 * 1000L

    private val mutex          = Mutex()
    private var cache          : Map<String, String> = emptyMap()
    private var builtAtMs      : Long = 0L

    /** Fire-and-forget prewarm.  Call at session start to avoid the first-query delay. */
    fun prewarm(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            get(context)
        }
    }

    /**
     * Returns the cache (rebuilding it if stale or empty).
     * Safe to call from any coroutine.
     */
    suspend fun get(context: Context): Map<String, String> = mutex.withLock {
        val now = System.currentTimeMillis()
        if (cache.isNotEmpty() && now - builtAtMs < TTL_MS) return@withLock cache
        cache     = build(context.applicationContext)
        builtAtMs = now
        cache
    }

    // ── Private builders ──────────────────────────────────────────────────────

    private suspend fun build(context: Context): Map<String, String> =
        withContext(Dispatchers.IO) {
            val pm      = context.packageManager
            val intent  = android.content.Intent(
                android.content.Intent.ACTION_MAIN, null
            ).addCategory(android.content.Intent.CATEGORY_LAUNCHER)

            val result  = mutableMapOf<String, String>()

            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0).forEach { info ->
                val label = info.loadLabel(pm).toString().trim().lowercase()
                val pkg   = info.activityInfo.packageName
                if (label.isNotBlank()) result[label] = pkg
            }
            result.toMap()
        }
}
