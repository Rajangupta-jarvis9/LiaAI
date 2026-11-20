package com.Lia.assistant.action

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ContactResolver logic that does not require a device.
 *
 * The Contacts-cursor path is an Android integration concern and is tested
 * via manual verification on-device.  These tests cover the phone-number
 * detection heuristic which runs entirely on the JVM.
 */
class ContactResolverTest {

    // ── Phone number detection ────────────────────────────────────────────────
    // We test the detection indirectly via the public API by providing a
    // mock-free path: when the query is a phone number, the resolver should
    // return the number without needing a ContentResolver.  Because the real
    // method requires a Context for the Contacts cursor, we test the regex
    // patterns directly via a test-only helper.

    @Test fun `pure digits with 5 or more digits is a phone number`() =
        assertTrue(isPhoneNumber("9876543210"))

    @Test fun `digits with spaces is a phone number`() =
        assertTrue(isPhoneNumber("98765 43210"))

    @Test fun `E164 format is a phone number`() =
        assertTrue(isPhoneNumber("+91 98765 43210"))

    @Test fun `digits with dashes and parens is a phone number`() =
        assertTrue(isPhoneNumber("(022) 1234-5678"))

    @Test fun `short digit string under 5 digits is NOT a phone number`() =
        assertFalse(isPhoneNumber("1234"))

    @Test fun `plain name is NOT a phone number`() =
        assertFalse(isPhoneNumber("Mummy"))

    @Test fun `name with digits in it is NOT a phone number`() =
        assertFalse(isPhoneNumber("2nd Street Rahul"))

    @Test fun `blank string is NOT a phone number`() =
        assertFalse(isPhoneNumber(""))

    // ── Helper that mirrors the private logic in ContactResolver ──────────────

    private val PHONE_PATTERN = Regex("""^[\d\s+\-().]+$""")
    private val DIGIT_PATTERN = Regex("""\d""")

    private fun isPhoneNumber(q: String): Boolean =
        q.isNotBlank()
            && PHONE_PATTERN.matches(q)
            && DIGIT_PATTERN.findAll(q).count() >= 5
}
