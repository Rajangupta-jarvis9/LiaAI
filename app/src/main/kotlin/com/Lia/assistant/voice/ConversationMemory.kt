package com.Lia.assistant.voice

/**
 * In-memory rolling window of the last [MAX_TURNS] conversation turns.
 *
 * Used to inject a short recap into the system instruction when the WebSocket
 * reconnects, so Lia can continue mid-conversation without greeting again.
 *
 * Thread-safe: every mutating operation synchronises on [lock].
 * Cleared when the voice session stops — no persistent storage.
 */
class ConversationMemory {

    enum class Role { USER, ASSISTANT }

    data class Turn(val role: Role, val text: String)

    private val lock   = Any()
    private val turns  = ArrayDeque<Turn>(MAX_TURNS + 1)

    /** Appends one turn, evicting the oldest if the window is full. */
    fun add(role: Role, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        synchronized(lock) {
            turns.addLast(Turn(role, trimmed))
            if (turns.size > MAX_TURNS) turns.removeFirst()
        }
    }

    /**
     * Renders a compact recap string suitable for injection into the system
     * instruction.  The output is capped at [MAX_RECAP_CHARS] characters;
     * older turns are dropped from the front until it fits.
     *
     * Returns `null` when there are no turns (no recap needed).
     */
    fun recap(): String? {
        val snapshot: List<Turn>
        synchronized(lock) {
            if (turns.isEmpty()) return null
            snapshot = turns.toList()
        }

        // Build from newest first, prepend until we would exceed the cap,
        // then reverse so the output is chronological.
        val builder = StringBuilder()
        val lines   = snapshot.map { turn ->
            val label = if (turn.role == Role.USER) "User" else "You"
            "$label: ${turn.text}"
        }

        // Walk from the end; include as many lines as fit within the cap.
        val included = ArrayDeque<String>()
        var chars    = 0
        for (line in lines.reversed()) {
            val cost = line.length + 1  // +1 for the newline
            if (chars + cost > MAX_RECAP_CHARS && included.isNotEmpty()) break
            included.addFirst(line)
            chars += cost
        }

        return if (included.isEmpty()) null
        else included.joinToString("\n", builder::append).toString().trim()
    }

    /** Removes all stored turns.  Called when the voice session ends. */
    fun clear() = synchronized(lock) { turns.clear() }

    /** Returns the current number of stored turns. */
    fun size(): Int = synchronized(lock) { turns.size }

    companion object {
        const val MAX_TURNS       = 12
        const val MAX_RECAP_CHARS = 1600
    }
}
