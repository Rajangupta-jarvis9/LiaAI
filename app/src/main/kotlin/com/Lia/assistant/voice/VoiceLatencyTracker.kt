package com.Lia.assistant.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks per-session and per-turn latency timestamps.
 *
 * ── Events (in expected order) ───────────────────────────────────────────────
 * SESSION_START    → socket connect() called
 * SETUP_COMPLETE   → server setup response received
 * TURN_START       → user's mic chunk sent (voice-activity detected)
 * FIRST_AUDIO      → first audio chunk received for the current assistant turn
 *
 * Derived metrics exposed via [lastConnectMs] and [lastFirstAudioMs].
 *
 * Rules:
 * - Out-of-order marks are silently ignored (e.g. SETUP_COMPLETE before SESSION_START).
 * - A new SESSION_START resets all timestamps so previous metrics do not bleed through.
 * - Thread-safe: all mutations synchronise on [lock].
 */
object VoiceLatencyTracker {

    enum class Event {
        SESSION_START,
        SETUP_COMPLETE,
        TURN_START,
        FIRST_AUDIO,
    }

    private val lock = Any()

    private var sessionStartMs   : Long = -1L
    private var setupCompleteMs  : Long = -1L
    private var turnStartMs      : Long = -1L
    private var firstAudioMs     : Long = -1L

    /** How long the WebSocket handshake + Gemini setup frame took (ms). */
    var lastConnectMs    : Long = -1L
        private set

    /** How long from the end of user speech to Lia's first audio chunk (ms). */
    var lastFirstAudioMs : Long = -1L
        private set

    private val _reconnectCount = MutableStateFlow(0)
    val reconnectCount: StateFlow<Int> = _reconnectCount

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Records [event] at [nowMs] (defaults to [System.currentTimeMillis]).
     * Out-of-order marks are dropped: each event requires its predecessor to
     * have been recorded already.
     */
    fun mark(event: Event, nowMs: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            when (event) {
                Event.SESSION_START -> {
                    // Always allow SESSION_START — it resets all state.
                    sessionStartMs  = nowMs
                    setupCompleteMs = -1L
                    turnStartMs     = -1L
                    firstAudioMs    = -1L
                }
                Event.SETUP_COMPLETE -> {
                    if (sessionStartMs < 0 || nowMs < sessionStartMs) return
                    setupCompleteMs = nowMs
                    lastConnectMs   = nowMs - sessionStartMs
                }
                Event.TURN_START -> {
                    if (setupCompleteMs < 0 || nowMs < setupCompleteMs) return
                    turnStartMs  = nowMs
                    firstAudioMs = -1L   // reset so we can measure the next turn
                }
                Event.FIRST_AUDIO -> {
                    if (turnStartMs < 0 || nowMs < turnStartMs) return
                    if (firstAudioMs >= 0) return   // already recorded for this turn
                    firstAudioMs     = nowMs
                    lastFirstAudioMs = nowMs - turnStartMs
                }
            }
        }
    }

    fun incrementReconnect() {
        _reconnectCount.value++
    }

    fun resetReconnectCount() {
        _reconnectCount.value = 0
    }

    /** Resets everything.  Call when a session fully stops. */
    fun reset() {
        synchronized(lock) {
            sessionStartMs   = -1L
            setupCompleteMs  = -1L
            turnStartMs      = -1L
            firstAudioMs     = -1L
            lastConnectMs    = -1L
            lastFirstAudioMs = -1L
        }
        _reconnectCount.value = 0
    }
}
