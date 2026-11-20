package com.Lia.assistant.forge

import com.Lia.assistant.voice.ForgeIntent
import org.junit.Assert.*
import org.junit.Test

// ── SseParser ─────────────────────────────────────────────────────────────────

class SseParserTest {

    @Test fun `extracts text from valid data line`() {
        val line = """data:{"candidates":[{"content":{"parts":[{"text":"Hello"}]}}]}"""
        assertEquals("Hello", SseParser.extractText(line))
    }

    @Test fun `extracts text from multi-part line`() {
        val line = """data:{"candidates":[{"content":{"parts":[{"text":"Hel"},{"text":"lo"}]}}]}"""
        assertEquals("Hello", SseParser.extractText(line))
    }

    @Test fun `returns null for DONE sentinel`() {
        assertNull(SseParser.extractText("data:[DONE]"))
    }

    @Test fun `returns null for blank line`() {
        assertNull(SseParser.extractText("data:   "))
    }

    @Test fun `extracts finishReason STOP`() {
        val line = """data:{"candidates":[{"finishReason":"STOP"}]}"""
        assertEquals("STOP", SseParser.extractFinishReason(line))
    }

    @Test fun `extracts finishReason RECITATION`() {
        val line = """data:{"candidates":[{"finishReason":"RECITATION"}]}"""
        assertEquals("RECITATION", SseParser.extractFinishReason(line))
    }

    @Test fun `extracts error message`() {
        val line = """data:{"error":{"message":"Quota exceeded"}}"""
        assertEquals("Quota exceeded", SseParser.extractError(line))
    }

    @Test fun `isDone for DONE sentinel`() {
        assertTrue(SseParser.isDone("data:[DONE]"))
    }

    @Test fun `isDone false for normal line`() {
        assertFalse(SseParser.isDone("""data:{"candidates":[]}"""))
    }
}

// ── ForgeHtml ─────────────────────────────────────────────────────────────────

class ForgeHtmlTest {

    @Test fun `stripFences removes html fences`() {
        val fenced = "```html\n<!DOCTYPE html><html></html>\n```"
        val result = ForgeHtml.stripFences(fenced)
        assertTrue(result.startsWith("<!DOCTYPE html>"))
        assertFalse("```" in result)
    }

    @Test fun `stripFences is idempotent on clean html`() {
        val html = "<!DOCTYPE html><html></html>"
        assertEquals(html, ForgeHtml.stripFences(html))
    }

    @Test fun `looksLikeHtml true for valid html`() {
        assertTrue(ForgeHtml.looksLikeHtml("<!DOCTYPE html><html><body></body></html>"))
    }

    @Test fun `looksLikeHtml false for json`() {
        assertFalse(ForgeHtml.looksLikeHtml("""{"key":"value"}"""))
    }

    @Test fun `looksLikeHtml false for plain text`() {
        assertFalse(ForgeHtml.looksLikeHtml("hello world"))
    }

    @Test fun `isLikelyTruncated true for short html`() {
        assertTrue(ForgeHtml.isLikelyTruncated("<html><body>hi</body></html>"))
    }

    @Test fun `isLikelyTruncated true when no closing html tag`() {
        val html = "<!DOCTYPE html><html><body>" + "a".repeat(2000)
        assertTrue(ForgeHtml.isLikelyTruncated(html))
    }

    @Test fun `isLikelyTruncated false for complete long html`() {
        val html = "<!DOCTYPE html><html><body>" + "a".repeat(2000) + "</body></html>"
        assertFalse(ForgeHtml.isLikelyTruncated(html))
    }

    @Test fun `repairTruncated adds missing closing tags`() {
        val html   = "<html><body><script>var x=1"
        val result = ForgeHtml.repairTruncated(html)
        assertTrue("</script>" in result)
        assertTrue("</body>"   in result)
        assertTrue("</html>"   in result)
    }

    @Test fun `withSafetyNet injects script before body close`() {
        val html   = "<html><body><p>hi</p></body></html>"
        val result = ForgeHtml.withSafetyNet(html)
        val scriptIdx = result.indexOf("<script>")
        val bodyIdx   = result.indexOf("</body>")
        assertTrue(scriptIdx < bodyIdx)
    }

    @Test fun `withSafetyNet is idempotent`() {
        val html  = "<html><body><p>hi</p></body></html>"
        val once  = ForgeHtml.withSafetyNet(html)
        val twice = ForgeHtml.withSafetyNet(once)
        assertEquals(once, twice)
    }
}

// ── ForgeStageDetector ────────────────────────────────────────────────────────

class ForgeStageDetectorTest {

    @Test fun `head only returns READING`() {
        assertEquals(ForgeStage.READING, ForgeStageDetector.detect("<html><head><title>Test</title></head>"))
    }

    @Test fun `body present returns STRUCTURE`() {
        assertEquals(ForgeStage.STRUCTURE, ForgeStageDetector.detect("<html><head></head><body><div></div>"))
    }

    @Test fun `style present returns STYLING`() {
        val html = "<html><head><style>.a{color:red}</style></head><body>"
        assertEquals(ForgeStage.STYLING, ForgeStageDetector.detect(html))
    }

    @Test fun `script present returns MOTION`() {
        val html = "<html><head><style></style></head><body><script>gsap.to()</script>"
        assertEquals(ForgeStage.MOTION, ForgeStageDetector.detect(html))
    }

    @Test fun `3+ visible sections returns CONTENT`() {
        val html = """<html><body><script></script>
            <h1>Title one</h1><p>Body text here very long</p>
            <h2>Section two</h2><p>More body text here</p>
            <h2>Section three</h2><p>Even more body text</p>"""
        assertEquals(ForgeStage.CONTENT, ForgeStageDetector.detect(html))
    }

    @Test fun `html close tag returns FINISHING`() {
        val html = "<html><head></head><body><h1>Title here!</h1></body></html>"
        assertEquals(ForgeStage.FINISHING, ForgeStageDetector.detect(html))
    }

    @Test fun `canvas alone does not count as visible section`() {
        val html = "<html><body><canvas id='c'></canvas></body>"
        assertEquals(ForgeStage.STRUCTURE, ForgeStageDetector.detect(html))
    }
}

// ── ForgeIntent ───────────────────────────────────────────────────────────────

class ForgeIntentTest {

    @Test fun `detects hindi banao trigger`() {
        assertNotNull(ForgeIntent.extract("ek cafe ki website banao"))
    }

    @Test fun `detects english create trigger`() {
        assertNotNull(ForgeIntent.extract("create a landing page for my yoga studio"))
    }

    @Test fun `detects build trigger`() {
        assertNotNull(ForgeIntent.extract("build me a website about photography"))
    }

    @Test fun `ignores question about websites`() {
        assertNull(ForgeIntent.extract("what is a website?"))
    }

    @Test fun `ignores casual mention without build verb`() {
        assertNull(ForgeIntent.extract("I love websites"))
    }

    @Test fun `description is the full original text`() {
        val text = "ek bakery ki website banao with dark theme"
        val req  = ForgeIntent.extract(text)
        assertEquals(text, req?.description)
    }
}
