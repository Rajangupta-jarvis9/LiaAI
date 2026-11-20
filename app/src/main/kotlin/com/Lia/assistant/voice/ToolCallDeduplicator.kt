package com.Lia.assistant.voice

/**
 * Prevents the same Gemini tool call from being executed more than once.
 *
 * Gemini Live may occasionally deliver the same function-call frame twice
 * (e.g. during a barge-in recovery).  This deduplicator keeps a bounded
 * [LinkedHashSet] of recently-seen call IDs and reports whether an incoming
 * ID is a duplicate.
 *
 * Thread-safe: every access synchronises on [lock].
 */
class ToolCallDeduplicator(private val maxSize: Int = MAX_SIZE) {

    private val lock   = Any()
    private val seenIds: LinkedHashSet<String> = LinkedHashSet()

    /**
     * Returns `true` if [callId] has been seen before (duplicate — skip it).
     * Returns `false` if [callId] is new — side-effect: records it.
     *
     * When the set reaches [maxSize], the oldest entry is evicted to make room.
     */
    fun isDuplicate(callId: String): Boolean {
        synchronized(lock) {
            if (seenIds.contains(callId)) return true
            if (seenIds.size >= maxSize) {
                seenIds.remove(seenIds.iterator().next())   // evict oldest
            }
            seenIds.add(callId)
            return false
        }
    }

    /** Clears all recorded IDs.  Called when a new session starts. */
    fun reset() = synchronized(lock) { seenIds.clear() }

    /** Returns the number of IDs currently recorded (for tests). */
    fun size(): Int = synchronized(lock) { seenIds.size }

    companion object {
        const val MAX_SIZE = 256
    }
}
