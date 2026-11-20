package com.Lia.assistant.action

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AppLauncherTest {

    private lateinit var cache: Map<String, String>

    @Before fun setUp() {
        cache = mapOf(
            "youtube"          to "com.google.android.youtube",
            "whatsapp"         to "com.whatsapp",
            "google maps"      to "com.google.android.apps.maps",
            "camera"           to "com.android.camera2",
            "settings"         to "com.android.settings",
            "instagram"        to "com.instagram.android",
            "chrome"           to "com.android.chrome",
            "spotify"          to "com.spotify.music",
            "telegram"         to "org.telegram.messenger",
        )
    }

    // ── Exact match ───────────────────────────────────────────────────────────

    @Test fun `exact match returns correct package`() =
        assertEquals("com.google.android.youtube", AppLauncher.findApp("youtube", cache))

    @Test fun `exact match is case-insensitive`() =
        assertEquals("com.whatsapp", AppLauncher.findApp("WhatsApp", cache))

    // ── Key-starts-with-query ─────────────────────────────────────────────────

    @Test fun `prefix query youtube matches youtube`() =
        assertEquals("com.google.android.youtube", AppLauncher.findApp("youtu", cache))

    @Test fun `prefix query insta matches instagram`() =
        assertEquals("com.instagram.android", AppLauncher.findApp("insta", cache))

    // ── Query-starts-with-key ─────────────────────────────────────────────────

    @Test fun `query longer than key still matches`() =
        assertEquals("com.android.chrome", AppLauncher.findApp("chrome browser", cache))

    // ── Multi-word query ──────────────────────────────────────────────────────

    @Test fun `multi-word query google maps matches`() =
        assertEquals("com.google.android.apps.maps", AppLauncher.findApp("google maps", cache))

    @Test fun `partial multi-word query goo ma matches google maps`() =
        assertEquals("com.google.android.apps.maps", AppLauncher.findApp("goo ma", cache))

    // ── No match ─────────────────────────────────────────────────────────────

    @Test fun `unknown app returns null`() =
        assertNull(AppLauncher.findApp("nonexistentapp", cache))

    @Test fun `blank query returns null`() =
        assertNull(AppLauncher.findApp("   ", cache))

    @Test fun `empty cache returns null`() =
        assertNull(AppLauncher.findApp("youtube", emptyMap()))

    // ── Minimum key length guard ──────────────────────────────────────────────

    @Test fun `key shorter than 3 chars is not matched`() {
        val smallCache = mapOf("yo" to "com.example.yo")
        assertNull(AppLauncher.findApp("yo", smallCache))
    }

    // ── Length-closeness tiebreak ─────────────────────────────────────────────

    @Test fun `among multiple candidates picks closest length`() {
        val tieCache = mapOf(
            "spot"    to "com.spot.short",
            "spotify" to "com.spotify.music",
        )
        // Query "spotify" exactly matches spotify — but it can also prefix-match spot.
        // Exact match wins before candidates are evaluated.
        assertEquals("com.spotify.music", AppLauncher.findApp("spotify", tieCache))
    }
}
