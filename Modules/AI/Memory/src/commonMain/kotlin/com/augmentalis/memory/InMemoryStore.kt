package com.augmentalis.memory

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant

/**
 * In-memory implementation of MemoryStore for testing and development.
 * This implementation is not persistent - data is lost when the app closes.
 */
class InMemoryStore : MemoryStore {
    private val memories = mutableMapOf<String, MemoryEntry>()
    private val mutex = Mutex()

    override suspend fun store(entry: MemoryEntry) = mutex.withLock {
        memories[entry.id] = entry
    }

    override suspend fun retrieve(id: String): MemoryEntry? = mutex.withLock {
        memories[id]
    }

    override suspend fun findByType(type: MemoryType): List<MemoryEntry> = mutex.withLock {
        memories.values.filter { it.type == type }
    }

    override suspend fun findByTimeRange(start: Instant, end: Instant): List<MemoryEntry> = mutex.withLock {
        memories.values.filter { it.timestamp >= start && it.timestamp <= end }
    }

    override suspend fun search(query: String, limit: Int): List<MemoryEntry> = mutex.withLock {
        // Simple text search - will be replaced with semantic search
        memories.values
            .filter { it.content.contains(query, ignoreCase = true) }
            .sortedByDescending { it.importance }
            .take(limit)
    }

    override suspend fun delete(id: String) {
        mutex.withLock {
            memories.remove(id)
        }
    }

    override suspend fun deleteByType(type: MemoryType) = mutex.withLock {
        val toRemove = memories.values.filter { it.type == type }.map { it.id }
        toRemove.forEach { memories.remove(it) }
    }

    override suspend fun findByImportance(threshold: Float): List<MemoryEntry> = mutex.withLock {
        memories.values.filter { it.importance >= threshold }
    }

    override suspend fun update(entry: MemoryEntry) = mutex.withLock {
        if (memories.containsKey(entry.id)) {
            memories[entry.id] = entry
        }
    }

    override suspend fun clearAll() = mutex.withLock {
        memories.clear()
    }
}
