package com.Lia.assistant.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Prevents Lia from hearing her own voice through the speaker and treating it
 * as user input.
 *
 * ── How it works ─────────────────────────────────────────────────────────────
 * A reference counter tracks how many concurrent playback chunks are active.
 * While the counter > 0, the mic is muted (isMuted = true).
 * When the counter reaches 0, muting continues for [ECHO_TAIL_MS] milliseconds
 * to absorb room echo, then the mic is unmuted.
 * A 60-second watchdog force-unmutes in case a notifyPlaybackStopped() call is
 * ever missed (e.g. a crash mid-playback).
 *
 * ── Usage ─────────────────────────────────────────────────────────────────────
 * LiveAudioIO calls [notifyPlaybackStarted] before writing a chunk to AudioTrack
 * and [notifyPlaybackStopped] after the chunk finishes.
 * The mic recording loop checks [isMicMuted] before forwarding each chunk.
 *
 * Thread-safe: all mutations are synchronised on [lock].
 * The coroutine delay uses the IO dispatcher to avoid blocking the audio thread.
 */
object EchoGuard {

    const val ECHO_TAIL_MS    = 450L
    const val WATCHDOG_MS     = 60_000L

    private val lock          = Any()
    private var activeCount   = 0
    private var muted         = false

    private val scope         = CoroutineScope(Dispatchers.IO)
    private var tailJob       : Job? = null
    private var watchdogJob   : Job? = null

    // ── Clock abstraction (for unit tests) ────────────────────────────────────

    /**
     * Overrideable delay function.  Tests inject a fake clock here to avoid
     * real 450 ms / 60 s waits.
     */
    var delayFn: suspend (Long) -> Unit = { ms -> delay(ms) }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Call BEFORE writing a PCM chunk to the speaker.
     * Increments the active-playback counter and mutes the mic immediately.
     */
    fun notifyPlaybackStarted() {
        synchronized(lock) {
            activeCount++
            muted = true
            tailJob?.cancel()       // cancel any pending unmute tail

            // (Re)arm the watchdog on every new chunk.
            watchdogJob?.cancel()
            watchdogJob = scope.launch {
                delayFn(WATCHDOG_MS)
                forceUnmute()
            }
        }
    }

    /**
     * Call AFTER the chunk has finished playing.
     * Decrements the counter; when it reaches 0, schedules the echo-tail delay.
     */
    fun notifyPlaybackStopped() {
        synchronized(lock) {
            if (activeCount > 0) activeCount--
            if (activeCount == 0) {
                // Schedule unmute after echo-tail delay.
                tailJob?.cancel()
                tailJob = scope.launch {
                    delayFn(ECHO_TAIL_MS)
                    synchronized(lock) {
                        if (activeCount == 0) {
                            muted = false
                        }
                    }
                }
            }
        }
    }

    /** Returns true while the mic should be suppressed. */
    fun isMicMuted(): Boolean = synchronized(lock) { muted }

    /**
     * Resets all state.  Call when a voice session ends or before a new
     * session starts so stale playback counts do not bleed across sessions.
     */
    fun reset() {
        synchronized(lock) {
            activeCount = 0
            muted       = false
            tailJob?.cancel();     tailJob     = null
            watchdogJob?.cancel(); watchdogJob = null
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun forceUnmute() {
        synchronized(lock) {
            activeCount = 0
            muted       = false
            tailJob?.cancel(); tailJob = null
        }
    }
}
