package com.Lia.assistant.agent.social

import com.Lia.assistant.agent.core.Blocker

// ── Supporting types ──────────────────────────────────────────────────────────

enum class Platform    { INSTAGRAM, FACEBOOK }
enum class SocialAction{ CREATE_POST, CREATE_REEL, CREATE_STORY, TEXT_POST }
enum class TaskMode    { PUBLISH, DRAFT }
enum class ControlCommand { CONFIRM, REJECT, RESUME, CANCEL, STATUS }

data class SocialTaskRequest(
    val platform     : Platform,
    val action       : SocialAction,
    val mediaUri     : String?,
    val caption      : String,
    val mode         : TaskMode,
    val targetAccount: String?,
) {
    companion object {
        fun parse(
            platform    : String,
            action      : String,
            mediaUri    : String?,
            caption     : String,
            mode        : String,
            targetAccount: String?,
        ): Result<SocialTaskRequest> = runCatching {
            SocialTaskRequest(
                platform      = Platform.valueOf(platform.uppercase()),
                action        = SocialAction.valueOf(action.uppercase()),
                mediaUri      = mediaUri?.takeIf { it.startsWith("content://") },
                caption       = caption.trim(),
                mode          = TaskMode.valueOf(mode.uppercase()),
                targetAccount = targetAccount?.trim()?.takeIf { it.isNotBlank() },
            )
        }
    }
}

// ── States ────────────────────────────────────────────────────────────────────

enum class TaskState {
    IDLE,
    OPENING_APP,
    OBSERVING,
    RESOLVING_TARGET,
    VALIDATING_TARGET,
    EXECUTING_ACTION,
    WAITING_FOR_UI,
    VERIFYING,
    NEXT_STEP,
    RETRY,
    REOBSERVE,
    WAITING_FOR_CONFIRMATION,
    WAITING_FOR_USER,
    COMPLETED,
    FAILED,
    CANCELLED;

    val isTerminal: Boolean get() = this == COMPLETED || this == FAILED || this == CANCELLED
}

data class PlanStep(val description: String, val actionSpec: String)
data class TaskReport(val state: TaskState, val message: String, val taskId: String)

// ── Transition table ──────────────────────────────────────────────────────────

private val LEGAL_TRANSITIONS: Map<TaskState, Set<TaskState>> = mapOf(
    TaskState.IDLE                   to setOf(TaskState.OPENING_APP),
    TaskState.OPENING_APP            to setOf(TaskState.OBSERVING, TaskState.FAILED, TaskState.CANCELLED),
    TaskState.OBSERVING              to setOf(TaskState.RESOLVING_TARGET, TaskState.WAITING_FOR_USER, TaskState.FAILED, TaskState.CANCELLED),
    TaskState.RESOLVING_TARGET       to setOf(TaskState.VALIDATING_TARGET, TaskState.REOBSERVE, TaskState.FAILED, TaskState.CANCELLED),
    TaskState.VALIDATING_TARGET      to setOf(TaskState.EXECUTING_ACTION, TaskState.REOBSERVE, TaskState.FAILED, TaskState.CANCELLED),
    TaskState.EXECUTING_ACTION       to setOf(TaskState.WAITING_FOR_UI, TaskState.VERIFYING, TaskState.WAITING_FOR_CONFIRMATION, TaskState.RETRY, TaskState.FAILED, TaskState.CANCELLED),
    TaskState.WAITING_FOR_UI         to setOf(TaskState.OBSERVING, TaskState.RETRY, TaskState.FAILED, TaskState.CANCELLED),
    TaskState.VERIFYING              to setOf(TaskState.NEXT_STEP, TaskState.COMPLETED, TaskState.RETRY, TaskState.FAILED, TaskState.CANCELLED),
    TaskState.NEXT_STEP              to setOf(TaskState.OBSERVING, TaskState.COMPLETED, TaskState.FAILED, TaskState.CANCELLED),
    TaskState.RETRY                  to setOf(TaskState.OBSERVING, TaskState.FAILED, TaskState.CANCELLED),
    TaskState.REOBSERVE              to setOf(TaskState.RESOLVING_TARGET, TaskState.FAILED, TaskState.CANCELLED),
    TaskState.WAITING_FOR_CONFIRMATION to setOf(TaskState.EXECUTING_ACTION, TaskState.CANCELLED),
    TaskState.WAITING_FOR_USER       to setOf(TaskState.OBSERVING, TaskState.CANCELLED, TaskState.FAILED),
    // Terminal states have no outgoing transitions.
    TaskState.COMPLETED              to emptySet(),
    TaskState.FAILED                 to emptySet(),
    TaskState.CANCELLED              to emptySet(),
)

// ── State machine ─────────────────────────────────────────────────────────────

class TaskStateMachine(val taskId: String, val request: SocialTaskRequest) {

    var currentState: TaskState = TaskState.IDLE
        private set

    val history = mutableListOf<TaskState>()

    var blocker   : Blocker? = null; private set
    var lastError : String   = ""  ; private set
    var confirmedByUser      = false; private set
    private var retryCount   = 0
    private val maxRetries   = 3

    /**
     * Moves to [next].  Throws [IllegalStateTransitionException] if the
     * transition is not in [LEGAL_TRANSITIONS].
     */
    fun moveTo(next: TaskState, message: String = "") {
        if (currentState.isTerminal) {
            throw IllegalStateTransitionException(currentState, next, "terminal state cannot transition")
        }
        val allowed = LEGAL_TRANSITIONS[currentState] ?: emptySet()
        if (next !in allowed) {
            throw IllegalStateTransitionException(currentState, next)
        }
        history.add(currentState)
        currentState = next
        if (message.isNotBlank()) lastError = message
    }

    fun setBlocker(b: Blocker) {
        blocker = b
        moveTo(TaskState.WAITING_FOR_USER, "Blocked: ${b.name}")
    }

    fun confirm()  { confirmedByUser = true }
    fun canRetry() = retryCount++ < maxRetries

    fun report() = TaskReport(currentState, lastError, taskId)
}

class IllegalStateTransitionException(
    from   : TaskState,
    to     : TaskState,
    detail : String = "",
) : Exception("Illegal transition $from → $to${if (detail.isNotEmpty()) ": $detail" else ""}")
