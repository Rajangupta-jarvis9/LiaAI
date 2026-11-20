package com.Lia.assistant.forge

/**
 * HTML processing utilities for generated websites.
 *
 * All functions are pure Kotlin with no Android dependencies.
 */
object ForgeHtml {

    private const val MIN_ACCEPTABLE_CHARS = 14_000
    private const val MIN_CONTENT_CHARS    = 1_500

    // ── Fence stripping ───────────────────────────────────────────────────────

    /**
     * Strips ```html … ``` fences that models sometimes wrap output in.
     * Idempotent — already-clean HTML is returned unchanged.
     */
    fun stripFences(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```html", ignoreCase = true)) {
            s = s.removePrefix("```html").removePrefix("```HTML")
        } else if (s.startsWith("```")) {
            s = s.removePrefix("```")
        }
        if (s.endsWith("```")) s = s.removeSuffix("```")
        return s.trim()
    }

    // ── Validity checks ───────────────────────────────────────────────────────

    /** True when [html] looks like real HTML (has a tag and a body-like marker). */
    fun looksLikeHtml(html: String): Boolean {
        val lower = html.lowercase()
        return "<" in html && ("<!doctype html" in lower || "<html" in lower || "<body" in lower)
    }

    /**
     * True when [html] is long enough to be accepted as a complete page even
     * if it arrived with a non-STOP finishReason.
     */
    fun isAcceptable(html: String) = html.length >= MIN_ACCEPTABLE_CHARS

    /**
     * True when [html] is suspiciously short (model probably gave up early)
     * or ends without `</html>`.
     */
    fun isLikelyTruncated(html: String): Boolean {
        val lower = html.lowercase().trim()
        return html.length < MIN_CONTENT_CHARS || !lower.endsWith("</html>")
    }

    // ── Repair ────────────────────────────────────────────────────────────────

    /**
     * Closes unclosed `<script>`, `<style>`, `<body>` and `<html>` tags so the
     * browser can render as much of the page as possible.
     * Only called on pages that passed [isAcceptable] but are missing the closing tag.
     */
    fun repairTruncated(html: String): String {
        val lower  = html.lowercase()
        val result = StringBuilder(html)

        if ("</script>" !in lower && "<script" in lower)  result.append("\n</script>")
        if ("</style>"  !in lower && "<style"  in lower)  result.append("\n</style>")
        if ("</body>"   !in lower)                         result.append("\n</body>")
        if ("</html>"   !in lower)                         result.append("\n</html>")

        return result.toString()
    }

    // ── Safety net injection ──────────────────────────────────────────────────

    private const val SAFETY_NET_SCRIPT = """
<script>
(function(){
  var WAIT=1400,SKIP='[style*="opacity:0"],[style*="opacity: 0"]';
  function fix(root){
    var els=root.querySelectorAll('[style]');
    for(var i=0;i<els.length;i++){
      var el=els[i];
      var s=el.style;
      if(parseFloat(s.opacity)===0&&s.position!=='fixed'&&s.pointerEvents!=='none'&&!el.getAttribute('aria-hidden')&&el.tagName!=='CANVAS'){
        var r=el.getBoundingClientRect();
        if(r.width>0&&r.height>0&&r.top<window.innerHeight){
          el.style.opacity='1';
          el.style.transition='opacity 0.4s';
        }
      }
    }
  }
  setTimeout(function(){fix(document)},WAIT);
  document.addEventListener('DOMContentLoaded',function(){setTimeout(function(){fix(document)},WAIT)});
})();
</script>"""

    /**
     * Injects the safety-net script once, immediately before `</body>`.
     * Idempotent — calling it twice on the same HTML leaves only one copy.
     */
    fun withSafetyNet(html: String): String {
        if (SAFETY_NET_SCRIPT.trim().take(20) in html) return html  // already injected
        val insertPoint = html.lastIndexOf("</body>", ignoreCase = true)
        return if (insertPoint >= 0) {
            html.substring(0, insertPoint) + SAFETY_NET_SCRIPT + "\n" + html.substring(insertPoint)
        } else {
            html + SAFETY_NET_SCRIPT
        }
    }
}
