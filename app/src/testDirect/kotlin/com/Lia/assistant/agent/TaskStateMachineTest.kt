package com.Lia.assistant.agent

import com.Lia.assistant.agent.core.Blocker
import com.Lia.assistant.agent.social.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TaskStateMachineTest {

    private fun machine(mode: TaskMode = TaskMode.PUBLISH) = TaskStateMachine(
        taskId  = "t-001",
        request = SocialTaskRequest(
            platform      = Platform.INSTAGRAM,
            action        = SocialAction.CREATE_POST,
            mediaUri      = "content://media/external/images/1",
            caption       = "Test caption",
            mode          = mode,
            targetAccount = null,
        ),
    )

    // ── Legal happy path ──────────────────────────────────────────────────────

    @Test fun `happy path from IDLE to COMPLETED`() {
        val m = machine()
        m.moveTo(TaskState.OPENING_APP)
        m.moveTo(TaskState.OBSERVING)
        m.moveTo(TaskState.RESOLVING_TARGET)
        m.moveTo(TaskState.VALIDATING_TARGET)
        m.moveTo(TaskState.EXECUTING_ACTION)
        m.moveTo(TaskState.VERIFYING)
        m.moveTo(TaskState.COMPLETED)
        assertEquals(TaskState.COMPLETED, m.currentState)
    }

    @Test fun `history records every visited state`() {
        val m = machine()
        m.moveTo(TaskState.OPENING_APP)
        m.moveTo(TaskState.OBSERVING)
        assertEquals(listOf(TaskState.IDLE, TaskState.OPENING_APP), m.history)
    }

    // ── Illegal transitions throw ─────────────────────────────────────────────

    @Test(expected = IllegalStateTransitionException::class)
    fun `IDLE cannot jump directly to VERIFYING`() {
        machine().moveTo(TaskState.VERIFYING)
    }

    @Test(expected = IllegalStateTransitionException::class)
    fun `COMPLETED cannot transition to any state`() {
        val m = machine()
        m.moveTo(TaskState.OPENING_APP)
        m.moveTo(TaskState.OBSERVING)
        m.moveTo(TaskState.RESOLVING_TARGET)
        m.moveTo(TaskState.VALIDATING_TARGET)
        m.moveTo(TaskState.EXECUTING_ACTION)
        m.moveTo(TaskState.VERIFYING)
        m.moveTo(TaskState.COMPLETED)
        m.moveTo(TaskState.OPENING_APP)   // must throw
    }

    @Test(expected = IllegalStateTransitionException::class)
    fun `FAILED cannot transition to any state`() {
        val m = machine()
        m.moveTo(TaskState.OPENING_APP)
        m.moveTo(TaskState.FAILED)
        m.moveTo(TaskState.OPENING_APP)   // must throw
    }

    @Test(expected = IllegalStateTransitionException::class)
    fun `CANCELLED cannot transition to any state`() {
        val m = machine()
        m.moveTo(TaskState.OPENING_APP)
        m.moveTo(TaskState.CANCELLED)
        m.moveTo(TaskState.IDLE)          // must throw
    }

    // ── FAILED and CANCELLED reachable from every non-terminal state ──────────

    @Test fun `FAILED reachable from OPENING_APP`() {
        val m = machine(); m.moveTo(TaskState.OPENING_APP); m.moveTo(TaskState.FAILED)
        assertEquals(TaskState.FAILED, m.currentState)
    }

    @Test fun `CANCELLED reachable from WAITING_FOR_CONFIRMATION`() {
        val m = machine()
        m.moveTo(TaskState.OPENING_APP); m.moveTo(TaskState.OBSERVING)
        m.moveTo(TaskState.RESOLVING_TARGET); m.moveTo(TaskState.VALIDATING_TARGET)
        m.moveTo(TaskState.EXECUTING_ACTION); m.moveTo(TaskState.WAITING_FOR_CONFIRMATION)
        m.moveTo(TaskState.CANCELLED)
        assertEquals(TaskState.CANCELLED, m.currentState)
    }

    // ── Blocker pauses at WAITING_FOR_USER ────────────────────────────────────

    @Test fun `setBlocker moves to WAITING_FOR_USER`() {
        val m = machine()
        m.moveTo(TaskState.OPENING_APP); m.moveTo(TaskState.OBSERVING)
        m.setBlocker(Blocker.LOGIN)
        assertEquals(TaskState.WAITING_FOR_USER, m.currentState)
        assertEquals(Blocker.LOGIN, m.blocker)
    }

    // ── DRAFT never publishes ─────────────────────────────────────────────────

    @Test fun `DRAFT mode is preserved on the request`() {
        val m = machine(TaskMode.DRAFT)
        assertEquals(TaskMode.DRAFT, m.request.mode)
        assertNotEquals(TaskMode.PUBLISH, m.request.mode)
    }

    // ── PUBLISH must pause at WAITING_FOR_CONFIRMATION before irreversible tap ─

    @Test fun `PUBLISH mode can reach WAITING_FOR_CONFIRMATION`() {
        val m = machine(TaskMode.PUBLISH)
        m.moveTo(TaskState.OPENING_APP); m.moveTo(TaskState.OBSERVING)
        m.moveTo(TaskState.RESOLVING_TARGET); m.moveTo(TaskState.VALIDATING_TARGET)
        m.moveTo(TaskState.EXECUTING_ACTION); m.moveTo(TaskState.WAITING_FOR_CONFIRMATION)
        assertEquals(TaskState.WAITING_FOR_CONFIRMATION, m.currentState)
        assertFalse(m.confirmedByUser)
    }

    @Test fun `confirm sets confirmedByUser flag`() {
        val m = machine()
        m.moveTo(TaskState.OPENING_APP); m.moveTo(TaskState.OBSERVING)
        m.moveTo(TaskState.RESOLVING_TARGET); m.moveTo(TaskState.VALIDATING_TARGET)
        m.moveTo(TaskState.EXECUTING_ACTION); m.moveTo(TaskState.WAITING_FOR_CONFIRMATION)
        m.confirm()
        assertTrue(m.confirmedByUser)
    }

    // ── SocialTaskRequest.parse validates ─────────────────────────────────────

    @Test fun `parse valid args returns Success`() {
        val r = SocialTaskRequest.parse("INSTAGRAM", "CREATE_POST", null, "hi", "PUBLISH", null)
        assertTrue(r.isSuccess)
        assertEquals(Platform.INSTAGRAM, r.getOrThrow().platform)
    }

    @Test fun `parse unknown platform returns Failure`() {
        val r = SocialTaskRequest.parse("TIKTOK", "CREATE_POST", null, "hi", "PUBLISH", null)
        assertTrue(r.isFailure)
    }

    @Test fun `parse non-content uri is stripped`() {
        val r = SocialTaskRequest.parse("INSTAGRAM", "CREATE_POST", "file:///evil", "hi", "PUBLISH", null)
        assertNull(r.getOrThrow().mediaUri)
    }
}
