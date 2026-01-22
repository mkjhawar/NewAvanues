package com.augmentalis.memory

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryStoreTest {
    @Test
    fun testStoreAndRetrieve() = runTest {
        val store = InMemoryStore()
        val now = Clock.System.now()
        val entry = MemoryEntry(
            id = "test-1",
            type = MemoryType.SHORT_TERM,
            content = "Test memory",
            timestamp = now
        )

        store.store(entry)
        val retrieved = store.retrieve("test-1")

        assertNotNull(retrieved)
        assertEquals(entry.id, retrieved.id)
        assertEquals(entry.content, retrieved.content)
    }

    @Test
    fun testFindByType() = runTest {
        val store = InMemoryStore()
        val now = Clock.System.now()

        store.store(MemoryEntry("1", MemoryType.SHORT_TERM, "Short term 1", now))
        store.store(MemoryEntry("2", MemoryType.SHORT_TERM, "Short term 2", now))
        store.store(MemoryEntry("3", MemoryType.EPISODIC, "Episodic 1", now))

        val shortTerm = store.findByType(MemoryType.SHORT_TERM)
        val episodic = store.findByType(MemoryType.EPISODIC)

        assertEquals(2, shortTerm.size)
        assertEquals(1, episodic.size)
    }

    @Test
    fun testSearch() = runTest {
        val store = InMemoryStore()
        val now = Clock.System.now()

        store.store(MemoryEntry("1", MemoryType.SHORT_TERM, "The quick brown fox", now, importance = 0.8f))
        store.store(MemoryEntry("2", MemoryType.SHORT_TERM, "jumps over the lazy dog", now, importance = 0.5f))
        store.store(MemoryEntry("3", MemoryType.SHORT_TERM, "A completely different sentence", now, importance = 0.3f))

        val results = store.search("fox")
        assertEquals(1, results.size)
        assertEquals("1", results[0].id)

        val lazyResults = store.search("lazy")
        assertEquals(1, lazyResults.size)
        assertEquals("2", lazyResults[0].id)
    }

    @Test
    fun testDelete() = runTest {
        val store = InMemoryStore()
        val now = Clock.System.now()
        val entry = MemoryEntry("test-1", MemoryType.SHORT_TERM, "Test", now)

        store.store(entry)
        assertNotNull(store.retrieve("test-1"))

        store.delete("test-1")
        assertNull(store.retrieve("test-1"))
    }

    @Test
    fun testDeleteByType() = runTest {
        val store = InMemoryStore()
        val now = Clock.System.now()

        store.store(MemoryEntry("1", MemoryType.SHORT_TERM, "Short 1", now))
        store.store(MemoryEntry("2", MemoryType.SHORT_TERM, "Short 2", now))
        store.store(MemoryEntry("3", MemoryType.EPISODIC, "Episodic 1", now))

        store.deleteByType(MemoryType.SHORT_TERM)

        assertTrue(store.findByType(MemoryType.SHORT_TERM).isEmpty())
        assertEquals(1, store.findByType(MemoryType.EPISODIC).size)
    }

    @Test
    fun testUpdate() = runTest {
        val store = InMemoryStore()
        val now = Clock.System.now()
        val entry = MemoryEntry("test-1", MemoryType.SHORT_TERM, "Original", now)

        store.store(entry)
        val updated = entry.copy(content = "Updated")
        store.update(updated)

        val retrieved = store.retrieve("test-1")
        assertNotNull(retrieved)
        assertEquals("Updated", retrieved.content)
    }

    @Test
    fun testFindByImportance() = runTest {
        val store = InMemoryStore()
        val now = Clock.System.now()

        store.store(MemoryEntry("1", MemoryType.SHORT_TERM, "Low", now, importance = 0.2f))
        store.store(MemoryEntry("2", MemoryType.SHORT_TERM, "Medium", now, importance = 0.5f))
        store.store(MemoryEntry("3", MemoryType.SHORT_TERM, "High", now, importance = 0.9f))

        val important = store.findByImportance(0.6f)
        assertEquals(1, important.size)
        assertEquals("3", important[0].id)
    }

    @Test
    fun testClearAll() = runTest {
        val store = InMemoryStore()
        val now = Clock.System.now()

        store.store(MemoryEntry("1", MemoryType.SHORT_TERM, "Memory 1", now))
        store.store(MemoryEntry("2", MemoryType.EPISODIC, "Memory 2", now))

        store.clearAll()

        assertTrue(store.findByType(MemoryType.SHORT_TERM).isEmpty())
        assertTrue(store.findByType(MemoryType.EPISODIC).isEmpty())
    }
}
