package com.Lia.assistant.voice

import com.Lia.assistant.voice.ConversationMemory.Role.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConversationMemoryTest {

    private lateinit var memory: ConversationMemory

    @Before fun setUp() { memory = ConversationMemory() }

    @Test fun `fresh memory returns null recap`() =
        assertNull(memory.recap())

    @Test fun `adding one turn returns non-null recap`() {
        memory.add(USER, "hello")
        assertNotNull(memory.recap())
    }

    @Test fun `recap contains User label for USER role`() {
        memory.add(USER, "test message")
        assertTrue(memory.recap()!!.contains("User:"))
    }

    @Test fun `recap contains You label for ASSISTANT role`() {
        memory.add(ASSISTANT, "reply")
        assertTrue(memory.recap()!!.contains("You:"))
    }

    @Test fun `window evicts oldest when exceeding MAX_TURNS`() {
        repeat(ConversationMemory.MAX_TURNS + 5) { i ->
            memory.add(USER, "turn $i")
        }
        assertEquals(ConversationMemory.MAX_TURNS, memory.size())
    }

    @Test fun `recap length is capped at MAX_RECAP_CHARS`() {
        val longText = "a".repeat(300)
        repeat(20) { memory.add(USER, longText) }
        val recap = memory.recap()!!
        assertTrue(recap.length <= ConversationMemory.MAX_RECAP_CHARS)
    }

    @Test fun `blank text is not stored`() {
        memory.add(USER, "   ")
        assertEquals(0, memory.size())
    }

    @Test fun `clear removes all turns`() {
        memory.add(USER, "hello"); memory.add(ASSISTANT, "hi")
        memory.clear()
        assertEquals(0, memory.size())
        assertNull(memory.recap())
    }

    @Test fun `recap is chronological (oldest line first)`() {
        memory.add(USER, "first")
        memory.add(ASSISTANT, "second")
        val recap = memory.recap()!!
        val firstIdx  = recap.indexOf("first")
        val secondIdx = recap.indexOf("second")
        assertTrue(firstIdx < secondIdx)
    }
}
