package com.Lia.assistant.data

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for [ApiKeyStore].
 *
 * Uses Mockito to avoid a real Android context.
 */
class ApiKeyStoreTest {

    private lateinit var context   : Context
    private lateinit var prefs     : SharedPreferences
    private lateinit var editor    : SharedPreferences.Editor

    @Before fun setUp() {
        editor  = mock { on { putString(any(), any()) } doReturn it }
        prefs   = mock {
            on { getString("gemini_api_key", "") } doReturn ""
            on { edit() } doReturn editor
        }
        context = mock { on { getSharedPreferences("nova_prefs", Context.MODE_PRIVATE) } doReturn prefs }
    }

    @Test fun `hasKey returns false when no key saved`() {
        assertFalse(ApiKeyStore.hasKey(context))
    }

    @Test fun `saveKey trims whitespace before storing`() {
        ApiKeyStore.saveKey(context, "  AIzaSy_fakekey  ")
        verify(editor).putString("gemini_api_key", "AIzaSy_fakekey")
        verify(editor).apply()
    }

    @Test fun `saveKey with blank string stores empty string`() {
        ApiKeyStore.saveKey(context, "   ")
        verify(editor).putString("gemini_api_key", "")
    }

    @Test fun `getKey returns empty string on first call`() {
        assertEquals("", ApiKeyStore.getKey(context))
    }
}
