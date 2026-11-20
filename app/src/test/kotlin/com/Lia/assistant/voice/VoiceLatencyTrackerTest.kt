package com.Lia.assistant.voice

import com.Lia.assistant.voice.VoiceLatencyTracker.Event.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VoiceLatencyTrackerTest {

    @Before fun setUp() = VoiceLatencyTracker.reset()

    @Test fun `connect latency is setupComplete minus sessionStart`() {
        VoiceLatencyTracker.mark(SESSION_START,   nowMs = 1000L)
        VoiceLatencyTracker.mark(SETUP_COMPLETE,  nowMs = 1350L)
        assertEquals(350L, VoiceLatencyTracker.lastConnectMs)
    }

    @Test fun `firstAudio latency is firstAudio minus turnStart`() {
        VoiceLatencyTracker.mark(SESSION_START,   nowMs = 0L)
        VoiceLatencyTracker.mark(SETUP_COMPLETE,  nowMs = 300L)
        VoiceLatencyTracker.mark(TURN_START,      nowMs = 500L)
        VoiceLatencyTracker.mark(FIRST_AUDIO,     nowMs = 1100L)
        assertEquals(600L, VoiceLatencyTracker.lastFirstAudioMs)
    }

    @Test fun `setup_complete before session_start is ignored`() {
        VoiceLatencyTracker.mark(SETUP_COMPLETE, nowMs = 100L)
        assertEquals(-1L, VoiceLatencyTracker.lastConnectMs)
    }

    @Test fun `turn_start before setup_complete is ignored`() {
        VoiceLatencyTracker.mark(SESSION_START,  nowMs = 0L)
        VoiceLatencyTracker.mark(TURN_START,     nowMs = 200L)
        assertEquals(-1L, VoiceLatencyTracker.lastFirstAudioMs)
    }

    @Test fun `first_audio before turn_start is ignored`() {
        VoiceLatencyTracker.mark(SESSION_START,  nowMs = 0L)
        VoiceLatencyTracker.mark(SETUP_COMPLETE, nowMs = 200L)
        VoiceLatencyTracker.mark(FIRST_AUDIO,    nowMs = 500L)
        assertEquals(-1L, VoiceLatencyTracker.lastFirstAudioMs)
    }

    @Test fun `second first_audio for same turn is ignored`() {
        VoiceLatencyTracker.mark(SESSION_START,  nowMs = 0L)
        VoiceLatencyTracker.mark(SETUP_COMPLETE, nowMs = 100L)
        VoiceLatencyTracker.mark(TURN_START,     nowMs = 200L)
        VoiceLatencyTracker.mark(FIRST_AUDIO,    nowMs = 700L)
        VoiceLatencyTracker.mark(FIRST_AUDIO,    nowMs = 900L)  // should be ignored
        assertEquals(500L, VoiceLatencyTracker.lastFirstAudioMs)
    }

    @Test fun `new session_start resets all timestamps`() {
        VoiceLatencyTracker.mark(SESSION_START,  nowMs = 0L)
        VoiceLatencyTracker.mark(SETUP_COMPLETE, nowMs = 100L)
        // New session
        VoiceLatencyTracker.mark(SESSION_START,  nowMs = 5000L)
        VoiceLatencyTracker.mark(SETUP_COMPLETE, nowMs = 5400L)
        assertEquals(400L, VoiceLatencyTracker.lastConnectMs)
    }

    @Test fun `reconnect count increments correctly`() {
        assertEquals(0, VoiceLatencyTracker.reconnectCount.value)
        VoiceLatencyTracker.incrementReconnect()
        VoiceLatencyTracker.incrementReconnect()
        assertEquals(2, VoiceLatencyTracker.reconnectCount.value)
    }

    @Test fun `reset clears reconnect count`() {
        VoiceLatencyTracker.incrementReconnect()
        VoiceLatencyTracker.reset()
        assertEquals(0, VoiceLatencyTracker.reconnectCount.value)
    }
}
