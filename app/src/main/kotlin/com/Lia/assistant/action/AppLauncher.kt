package com.Lia.assistant.action

import android.content.ActivityNotFoundException
import android.content.Context
import kotlin.math.abs

/**
 * Finds and launches installed apps by user-supplied label.
 *
 * ── Matching strategy (in priority order) ────────────────────────────────────
 * 1. Exact match (lowercase).
 * 2. Candidate keys where key.length >= 3 AND one of:
 *    a. key startsWith query
 *    b. query startsWith key
 *    c. Every word in [query] has a key-word whose start matches it.
 * 3. Among candidates, pick the key closest in length to [query] (fewest extra chars).
 *
 * No loose substring matching — "you" must not match "youtube" without the prefix rule.
 * Minimum key length of 3 prevents single-letter keys from polluting results.
 */
object AppLauncher {

    /**
     * Finds the package name for [query], or null if nothing matches.
     *
     * @param cache  Pre-built label→package map from [InstalledAppLabelCache].
     */
    fun findApp(query: String, cache: Map<String, String>): String? {
        val q = query.trim().lowercase()
        if (q.isBlank()) return null

        // 1. Exact match.
        cache[q]?.let { return it }

        // 2. Prefix / multi-word candidates.
        val queryWords = q.split(Regex("\\s+")).filter { it.isNotBlank() }

        val candidates = cache.keys.filter { key ->
            if (key.length < 3) return@filter false
            key.startsWith(q)
                || q.startsWith(key)
                || allQueryWordsPrefixMatchKeyWords(queryWords, key.split(Regex("\\s+")))
        }

        if (candidates.isEmpty()) return null

        // 3. Pick candidate closest in length to query.
        val best = candidates.minByOrNull { abs(it.length - q.length) } ?: return null
        return cache[best]
    }

    /**
     * Launches [packageName].
     *
     * @return "launched" | "not_found" | "permission_denied"
     */
    fun launch(context: Context, packageName: String): String {
        val pm      = context.packageManager
        val intent  = pm.getLaunchIntentForPackage(packageName)
            ?: return "not_found"
        return try {
            context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            "launched"
        } catch (e: ActivityNotFoundException) {
            "not_found"
        } catch (e: SecurityException) {
            "permission_denied"
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun allQueryWordsPrefixMatchKeyWords(
        queryWords: List<String>,
        keyWords  : List<String>,
    ): Boolean = queryWords.all { qw ->
        keyWords.any { kw -> kw.startsWith(qw) }
    }
}
