package com.Lia.assistant.license

import org.junit.Assert.*
import org.junit.Test

class AccessKeyRulesTest {

    private val now = 1_700_000_000_000L  // fixed epoch for deterministic tests

    @Test fun `blank key returns inactive with no-key status`() {
        val info = decide(key = "")
        assertFalse(info.active)
        assertFalse(info.hasKey)
        assertTrue(info.status.contains("No access key", ignoreCase = true))
    }

    @Test fun `blocked key returns inactive regardless of active flag`() {
        val info = decide(key = "abc", blocked = true, active = true)
        assertFalse(info.active)
        assertTrue(info.blocked)
    }

    @Test fun `expired key returns inactive`() {
        val info = decide(key = "abc", expiresAtMs = now - 1000, active = true)
        assertFalse(info.active)
        assertTrue(info.status.contains("expired", ignoreCase = true))
    }

    @Test fun `non-expired active key returns active`() {
        val info = decide(key = "abc", expiresAtMs = now + 1_000_000, active = true)
        assertTrue(info.active)
    }

    @Test fun `zero expiresAt means no expiry`() {
        val info = decide(key = "abc", expiresAtMs = 0L, active = true)
        assertTrue(info.active)
    }

    @Test fun `inactive key with no block or expiry returns inactive`() {
        val info = decide(key = "abc", active = false)
        assertFalse(info.active)
    }

    @Test fun `isRefused true for blocked`() {
        val info = decide(key = "abc", blocked = true)
        assertTrue(AccessKeyRules.isRefused(info))
    }

    @Test fun `isRefused false for active non-blocked`() {
        val info = decide(key = "abc", active = true)
        assertFalse(AccessKeyRules.isRefused(info))
    }

    // helper
    private fun decide(
        key        : String  = "test-key",
        plan       : String  = "basic",
        expiresAtMs: Long    = 0L,
        active     : Boolean = false,
        blocked    : Boolean = false,
    ) = AccessKeyRules.decide(key, plan, expiresAtMs, active, blocked, nowMs = now)
}
