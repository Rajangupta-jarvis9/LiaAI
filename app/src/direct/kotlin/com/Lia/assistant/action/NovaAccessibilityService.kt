package com.Lia.assistant.action

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay

/**
 * Direct flavor — full screen-control via Android Accessibility APIs.
 *
 * Capabilities: readScreen, tapText, typeText, scrollScreen, goBack, goHome.
 * The static [instance] is set in [onServiceConnected] so [ActionExecutor] can
 * call these methods from a coroutine without needing a service reference.
 */
class NovaAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt()                                     = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    // ── Screen reader ─────────────────────────────────────────────────────────

    /**
     * BFS over the current window's view hierarchy.
     * Returns at most [maxElements] lines in format "[input] label" or "label".
     */
    fun readScreen(
        maxElements    : Int = 40,
        maxLabelLength : Int = 40,
    ): List<String> {
        val root   = rootInActiveWindow ?: return emptyList()
        val result = mutableListOf<String>()
        val queue  = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty() && result.size < maxElements) {
            val node  = queue.removeFirst()
            val label = (node.text ?: node.contentDescription)?.toString()?.trim() ?: ""
            if (label.isNotBlank()) {
                val truncated = if (label.length > maxLabelLength) label.take(maxLabelLength) + "…"
                                else label
                val prefix    = if (node.isEditable) "[input] " else ""
                val line      = "$prefix$truncated"
                if (line !in result) result.add(line)
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
            node.recycle()
        }
        return result
    }

    // ── Tapper ────────────────────────────────────────────────────────────────

    /** Finds the first node whose text or contentDescription matches [text] and clicks it. */
    fun tapText(text: String): String {
        val root = rootInActiveWindow ?: return "accessibility_not_enabled"
        val node = findNodeByText(root, text.trim())
            ?: return "element_not_found"
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        node.recycle()
        return "tapped"
    }

    // ── Typer ─────────────────────────────────────────────────────────────────

    /** Sets text on the currently focused editable field. */
    fun typeText(context: Context, text: String): String {
        val root    = rootInActiveWindow ?: return "accessibility_not_enabled"
        val focused = findFocusedInput(root) ?: return "no_focused_field"

        // Use ACTION_SET_TEXT (API 21+).
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        val ok = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        focused.recycle()
        return if (ok) "typed" else "type_failed"
    }

    // ── Scroller ─────────────────────────────────────────────────────────────

    /** Scrolls the first scrollable container in [direction] ("up"|"down"|"left"|"right"). */
    fun scrollScreen(direction: String): String {
        val root     = rootInActiveWindow ?: return "accessibility_not_enabled"
        val scrollable = findScrollable(root) ?: return "nothing_scrollable"
        val action = when (direction.lowercase()) {
            "down", "forward"  -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            "up",   "backward" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            "left"             -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            "right"            -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else               -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
        val ok = scrollable.performAction(action)
        scrollable.recycle()
        return if (ok) "scrolled" else "scroll_failed"
    }

    // ── System actions ────────────────────────────────────────────────────────

    fun goBack(): String {
        performGlobalAction(GLOBAL_ACTION_BACK)
        return "done"
    }

    fun goHome(): String {
        performGlobalAction(GLOBAL_ACTION_HOME)
        return "done"
    }

    // ── WhatsApp auto-send (direct only) ─────────────────────────────────────

    /**
     * Waits for the WhatsApp / SMS Send button and taps it.
     * Tries [maxTries] times with [waitMs] between each attempt.
     * @return "sent" | "composer_opened_send_not_found"
     */
    suspend fun autoTapSend(maxTries: Int = 12, waitMs: Long = 400L): String {
        repeat(maxTries) {
            val root = rootInActiveWindow
            if (root != null) {
                // WhatsApp sometimes shows "Continue to chat" first.
                findNodeByText(root, "Continue to chat")?.let { node ->
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    node.recycle()
                    delay(waitMs)
                    return@repeat
                }
                // Tap the Send button.
                val send = findNodeByText(root, "Send")
                    ?: findNodeByText(root, "send")
                    ?: findNodeByContentDesc(root, "Send")
                if (send != null) {
                    send.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    send.recycle()
                    return "sent"
                }
            }
            delay(waitMs)
        }
        return "composer_opened_send_not_found"
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val t    = (node.text ?: node.contentDescription)?.toString()?.trim()
            if (t.equals(text, ignoreCase = true)) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return null
    }

    private fun findNodeByContentDesc(root: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.contentDescription?.toString()?.trim().equals(desc, ignoreCase = true)) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return null
    }

    private fun findFocusedInput(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isEditable && node.isFocused) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        // Fall back to any editable field.
        val q2 = ArrayDeque<AccessibilityNodeInfo>()
        q2.add(root)
        while (q2.isNotEmpty()) {
            val node = q2.removeFirst()
            if (node.isEditable) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { q2.add(it) }
        }
        return null
    }

    private fun findScrollable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return null
    }

    companion object {
        @Volatile var instance: NovaAccessibilityService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            return enabled.contains(context.packageName, ignoreCase = true)
        }
    }
}
