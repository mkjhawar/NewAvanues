package com.augmentalis.memory

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MemoryEntryTest {
    @Test
    fun testMemoryEntryCreation() {
        val now = Clock.System.now()
        val entry = MemoryEntry(
            id = "test-1",
            type = MemoryType.SHORT_TERM,
            content = "Test memory",
            timestamp = now,
            importance = 0.8f
        )

        assertEquals("test-1", entry.id)
        assertEquals(MemoryType.SHORT_TERM, entry.type)
        assertEquals("Test memory", entry.content)
        assertEquals(0.8f, entry.importance)
        assertEquals(0, entry.accessCount)
    }

    @Test
    fun testImportanceValidation() {
        val now = Clock.System.now()

        // Valid importance values
        MemoryEntry("1", MemoryType.SHORT_TERM, "test", now, importance = 0.0f)
        MemoryEntry("2", MemoryType.SHORT_TERM, "test", now, importance = 1.0f)
        MemoryEntry("3", MemoryType.SHORT_TERM, "test", now, importance = 0.5f)

        // Invalid importance values
        assertFailsWith<IllegalArgumentException> {
            MemoryEntry("4", MemoryType.SHORT_TERM, "test", now, importance = -0.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            MemoryEntry("5", MemoryType.SHORT_TERM, "test", now, importance = 1.1f)
        }
    }

    @Test
    fun testWithAccess() {
        val now = Clock.System.now()
        val entry = MemoryEntry(
            id = "test-1",
            type = MemoryType.SHORT_TERM,
            content = "Test memory",
            timestamp = now
        )

        val accessTime = Clock.System.now()
        val accessed = entry.withAccess(accessTime)

        assertEquals(1, accessed.accessCount)
        assertEquals(accessTime, accessed.lastAccessed)

        val accessedAgain = accessed.withAccess(accessTime)
        assertEquals(2, accessedAgain.accessCount)
    }

    @Test
    fun testWithImportance() {
        val now = Clock.System.now()
        val entry = MemoryEntry(
            id = "test-1",
            type = MemoryType.SHORT_TERM,
            content = "Test memory",
            timestamp = now,
            importance = 0.5f
        )

        val updated = entry.withImportance(0.9f)
        assertEquals(0.9f, updated.importance)

        // Invalid importance
        assertFailsWith<IllegalArgumentException> {
            entry.withImportance(1.5f)
        }
    }

    @Test
    fun testMetadata() {
        val now = Clock.System.now()
        val metadata = mapOf(
            "conversationId" to "conv-123",
            "userId" to "user-456"
        )

        val entry = MemoryEntry(
            id = "test-1",
            type = MemoryType.EPISODIC,
            content = "Test memory",
            timestamp = now,
            metadata = metadata
        )

        assertEquals(metadata, entry.metadata)
        assertTrue(entry.metadata.containsKey("conversationId"))
        assertEquals("conv-123", entry.metadata["conversationId"])
    }
}
