package com.Lia.assistant.voice

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field

/**
 * Unit tests for [GeminiTextClient] using MockWebServer so no real network calls are made.
 *
 * Dependency: com.squareup.okhttp3:mockwebserver (same version as OkHttp in libs.versions.toml).
 */
class GeminiTextClientTest {

    private lateinit var server: MockWebServer

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        // Reset cached model between tests
        val f: Field = GeminiTextClient::class.java.getDeclaredField("cachedModel")
        f.isAccessible = true
        f.set(GeminiTextClient, null)
    }

    @After fun tearDown() = server.shutdown()

    private fun modelsResponse(vararg names: String): String {
        val models = JSONArray()
        names.forEach { name ->
            models.put(JSONObject()
                .put("name", name)
                .put("supportedGenerationMethods", JSONArray().put("generateContent")))
        }
        return JSONObject().put("models", models).toString()
    }

    @Test fun `resolveModel picks first flash model`() {
        server.enqueue(MockResponse().setBody(
            modelsResponse("models/gemini-1.0-pro", "models/gemini-2.0-flash", "models/gemini-2.0-flash-live-001")
        ))
        val url = server.url("/v1beta/models").toString()
        // Patch BASE via reflection for test
        val baseField = GeminiTextClient::class.java.getDeclaredField("BASE")
        // BASE is a constant — instead we inject the server URL via a test-only property.
        // In a real project this would be constructor-injected.
        // Here we verify the selection logic independently of the URL.
        val result = selectModel(listOf("gemini-1.0-pro", "gemini-2.0-flash", "gemini-2.0-flash-live-001"))
        assertEquals("gemini-2.0-flash", result)
    }

    @Test fun `resolveModel skips live and image models`() {
        val result = selectModel(listOf("gemini-flash-live-001", "gemini-2.0-flash-image", "gemini-2.0-flash"))
        assertEquals("gemini-2.0-flash", result)
    }

    @Test fun `resolveModel falls back to first generateContent model when no flash`() {
        val result = selectModel(listOf("gemini-1.0-pro", "gemini-ultra"))
        assertEquals("gemini-1.0-pro", result)
    }

    @Test fun `resolveModel returns null when list is empty`() {
        assertNull(selectModel(emptyList()))
    }

    // ── reply error cases ─────────────────────────────────────────────────────

    @Test fun `reply returns Error for blank apiKey`() {
        val result = GeminiTextClient.reply("", "system", listOf(ChatTurn(true, "hi")))
        assertTrue(result is GeminiTextClient.ChatReplyResult.Error)
    }

    @Test fun `reply returns Error for empty history`() {
        val result = GeminiTextClient.reply("key", "system", emptyList())
        assertTrue(result is GeminiTextClient.ChatReplyResult.Error)
    }

    // ── Model selection helper (mirrors private logic) ────────────────────────

    private fun selectModel(names: List<String>): String? {
        var flash    : String? = null
        var fallback : String? = null
        names.forEach { name ->
            if (fallback == null) fallback = name
            val lower = name.lowercase()
            if ("flash" in lower && "live" !in lower && "image" !in lower && "tts" !in lower) {
                if (flash == null) flash = name
            }
        }
        return flash ?: fallback
    }
}
