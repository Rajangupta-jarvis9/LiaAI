package com.Lia.assistant.license

/**
 * Pure Kotlin rules for deciding license state from raw data.
 *
 * No Android types — safe to test on the JVM without Robolectric.
 */
object AccessKeyRules {

    /**
     * Derives [LicenseInfo] from the raw fields stored in SharedPreferences.
     *
     * @param key          The stored access key (blank = not activated).
     * @param plan         Plan name string, e.g. "basic", "pro".
     * @param expiresAtMs  Expiry epoch in milliseconds. 0 = no expiry set.
     * @param active       Whether the backend last said the key is active.
     * @param blocked      Whether the backend explicitly blocked the key.
     * @param nowMs        Current time in milliseconds (injectable for testing).
     */
    fun decide(
        key       : String,
        plan      : String,
        expiresAtMs: Long,
        active    : Boolean,
        blocked   : Boolean,
        nowMs     : Long = System.currentTimeMillis(),
    ): LicenseInfo {
        if (key.isBlank()) return LicenseInfo(
            active   = false,
            plan     = "",
            expiresAt= 0L,
            status   = "No access key — add one in Profile.",
            blocked  = false,
            hasKey   = false,
        )

        if (blocked) return LicenseInfo(
            active   = false,
            plan     = plan,
            expiresAt= expiresAtMs,
            status   = "Key blocked. Contact support.",
            blocked  = true,
            hasKey   = true,
        )

        val expired = expiresAtMs > 0L && nowMs > expiresAtMs

        if (expired) return LicenseInfo(
            active   = false,
            plan     = plan,
            expiresAt= expiresAtMs,
            status   = "Key expired. Please renew.",
            blocked  = false,
            hasKey   = true,
        )

        return LicenseInfo(
            active   = active,
            plan     = plan,
            expiresAt= expiresAtMs,
            status   = if (active) "Active — $plan" else "Key inactive.",
            blocked  = false,
            hasKey   = true,
        )
    }

    /** Returns true when this key should be refused. */
    fun isRefused(info: LicenseInfo) = !info.active || info.blocked
}

data class LicenseInfo(
    val active   : Boolean,
    val plan     : String,
    val expiresAt: Long,
    val status   : String,
    val blocked  : Boolean,
    val hasKey   : Boolean,
)
