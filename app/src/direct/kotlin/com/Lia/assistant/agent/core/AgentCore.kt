package com.Lia.assistant.agent.core

import android.graphics.Rect

// ── UiElement ─────────────────────────────────────────────────────────────────

data class UiElement(
    val id            : String,
    val text          : String,
    val contentDesc   : String,
    val className     : String,
    val bounds        : Rect,
    val isClickable   : Boolean,
    val isEditable    : Boolean,
    val isScrollable  : Boolean,
    val isEnabled     : Boolean,
    val isChecked     : Boolean,
    val depth         : Int,
)

// ── ScreenObservation ─────────────────────────────────────────────────────────

data class ScreenObservation(
    val observationId : String,
    val packageName   : String,
    val windowClass   : String,
    val elements      : List<UiElement>,
    val screenWidth   : Int,
    val screenHeight  : Int,
    val windowStamp   : Long = System.currentTimeMillis(),
)

// ── Action result types ───────────────────────────────────────────────────────

enum class ExecPhase   { CAPTURING, RESOLVING, EXECUTING, VERIFYING, DONE }
enum class ActionMethod{ ACCESSIBILITY_CLICK, GESTURE_ON_ELEMENT_BOUNDS, VISION_TAP, SET_TEXT }

enum class Blocker {
    LOGIN, CAPTCHA, TWO_FACTOR, SECURITY_CHECK, PERMISSION_PROMPT
}

enum class FailureReason {
    ELEMENT_NOT_FOUND, ELEMENT_NOT_CLICKABLE, STALE_OBSERVATION,
    VERIFICATION_FAILED, DEVICE_ERROR, BLOCKED, CANCELLED, TIMEOUT
}

sealed class ActionResult {
    object Success                           : ActionResult()
    data class Failed(val reason: FailureReason, val detail: String = "") : ActionResult()
    data class Blocked(val blocker: Blocker)  : ActionResult()
}

// ── ScreenDriver interface ────────────────────────────────────────────────────

enum class DriverResult { OK, FAILED, STALE, NOT_FOUND, UNSUPPORTED }

interface ScreenDriver {
    fun capture()                                          : ScreenObservation?
    fun clickElement(obs: ScreenObservation, el: UiElement): DriverResult
    fun gestureTap(x: Float, y: Float)                     : DriverResult
    fun setText(text: String)                              : DriverResult
    fun scroll(direction: String)                          : DriverResult
    fun back()                                             : DriverResult
    fun home()                                             : DriverResult
}

// ── TargetResolver ────────────────────────────────────────────────────────────

object TargetResolver {
    /**
     * Finds the best element in [obs] matching [spec] (text or contentDesc).
     * Priority: exact text > exact contentDesc > text startsWith > contentDesc startsWith > contains.
     */
    fun resolve(obs: ScreenObservation, spec: String): UiElement? {
        val lower = spec.trim().lowercase()
        return obs.elements.firstOrNull { it.text.equals(lower, ignoreCase = true) }
            ?: obs.elements.firstOrNull { it.contentDesc.equals(lower, ignoreCase = true) }
            ?: obs.elements.firstOrNull { it.text.startsWith(lower, ignoreCase = true) }
            ?: obs.elements.firstOrNull { it.contentDesc.startsWith(lower, ignoreCase = true) }
            ?: obs.elements.firstOrNull { it.text.contains(lower, ignoreCase = true) }
            ?: obs.elements.firstOrNull { it.contentDesc.contains(lower, ignoreCase = true) }
    }
}
