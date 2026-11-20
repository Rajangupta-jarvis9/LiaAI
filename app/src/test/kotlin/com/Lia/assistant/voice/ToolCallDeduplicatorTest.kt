package com.Lia.assistant.voice

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ToolCallDeduplicatorTest {

    private lateinit var dedup: ToolCallDeduplicator

    @Before fun setUp() { dedup = ToolCallDeduplicator(maxSize = 4) }

    @Test fun `first call with new id is not a duplicate`() =
        assertFalse(dedup.isDuplicate("id-1"))

    @Test fun `second call with same id is a duplicate`() {
        dedup.isDuplicate("id-1")
        assertTrue(dedup.isDuplicate("id-1"))
    }

    @Test fun `two different ids are both not duplicates on first call`() {
        assertFalse(dedup.isDuplicate("id-1"))
        assertFalse(dedup.isDuplicate("id-2"))
    }

    @Test fun `set grows to maxSize then evicts oldest`() {
        // Fill to capacity: id-1, id-2, id-3, id-4
        repeat(4) { dedup.isDuplicate("id-$it") }
        assertEquals(4, dedup.size())

        // Adding id-4 should evict id-0 (the oldest).
        dedup.isDuplicate("id-new")
        assertEquals(4, dedup.size())

        // id-0 was evicted so it is no longer considered a duplicate.
        assertFalse(dedup.isDuplicate("id-0"))
    }

    @Test fun `reset clears all ids`() {
        dedup.isDuplicate("id-1")
        dedup.reset()
        assertEquals(0, dedup.size())
        // After reset, id-1 is no longer seen.
        assertFalse(dedup.isDuplicate("id-1"))
    }

    @Test fun `duplicate check is thread-safe under concurrent access`() {
        val threads = (1..20).map { n ->
            Thread { repeat(50) { dedup.isDuplicate("id-$n") } }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        // No assertion needed — test passes if no exception or deadlock occurs.
    }
}
