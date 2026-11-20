package com.Lia.assistant.voice

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EchoGuardTest {

    @Before fun setUp() {
        EchoGuard.reset()
        // Inject fake clock so we do not wait 450 ms / 60 s in real time.
        EchoGuard.delayFn = { ms -> kotlinx.coroutines.delay(ms) }
    }

    @After fun tearDown() {
        EchoGuard.reset()
        // Restore real delay.
        EchoGuard.delayFn = { ms -> kotlinx.coroutines.delay(ms) }
    }

    @Test fun `mic not muted initially`() =
        assertFalse(EchoGuard.isMicMuted())

    @Test fun `mic muted immediately when playback starts`() {
        EchoGuard.notifyPlaybackStarted()
        assertTrue(EchoGuard.isMicMuted())
    }

    @Test fun `mic stays muted while counter above zero`() {
        EchoGuard.notifyPlaybackStarted()
        EchoGuard.notifyPlaybackStarted()
        EchoGuard.notifyPlaybackStopped()
        assertTrue(EchoGuard.isMicMuted())    // counter still 1
    }

    @Test fun `mic still muted just after counter reaches zero (echo tail)`() = runTest {
        EchoGuard.delayFn = { ms -> kotlinx.coroutines.delay(ms) }
        EchoGuard.notifyPlaybackStarted()
        EchoGuard.notifyPlaybackStopped()
        // Counter is 0 but tail has not expired yet.
        assertTrue(EchoGuard.isMicMuted())
    }

    @Test fun `mic unmuted after echo tail expires`() = runTest {
        EchoGuard.delayFn = { ms -> kotlinx.coroutines.delay(ms) }
        EchoGuard.notifyPlaybackStarted()
        EchoGuard.notifyPlaybackStopped()
        advanceTimeBy(EchoGuard.ECHO_TAIL_MS + 50)
        assertFalse(EchoGuard.isMicMuted())
    }

    @Test fun `reset clears mute state immediately`() {
        EchoGuard.notifyPlaybackStarted()
        EchoGuard.reset()
        assertFalse(EchoGuard.isMicMuted())
    }

    @Test fun `counter does not go below zero`() {
        // Stop without a matching start — should not crash or leave negative state.
        EchoGuard.notifyPlaybackStopped()
        assertFalse(EchoGuard.isMicMuted())
    }

    @Test fun `multiple start-stop pairs resolve correctly`() = runTest {
        EchoGuard.delayFn = { ms -> kotlinx.coroutines.delay(ms) }
        repeat(5) {
            EchoGuard.notifyPlaybackStarted()
            EchoGuard.notifyPlaybackStopped()
        }
        advanceTimeBy(EchoGuard.ECHO_TAIL_MS + 50)
        assertFalse(EchoGuard.isMicMuted())
    }
}
