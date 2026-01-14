package com.augmentalis.memory

import kotlinx.datetime.Instant

/**
 * Interface for memory storage and retrieval.
 * Platform-specific implementations will handle actual persistence.
 */
interface MemoryStore {
    /**
     * Store a memory entry.
     */
    suspend fun store(entry: MemoryEntry)

    /**
     * Retrieve a memory by ID.
     */
    suspend fun retrieve(id: String): MemoryEntry?

    /**
     * Search memories by type.
     */
    suspend fun findByType(type: MemoryType): List<MemoryEntry>

    /**
     * Search memories by time range.
     */
    suspend fun findByTimeRange(start: Instant, end: Instant): List<MemoryEntry>

    /**
     * Search memories by content (semantic search).
     * @param query Search query
     * @param limit Maximum number of results
     */
    suspend fun search(query: String, limit: Int = 10): List<MemoryEntry>

    /**
     * Delete a memory by ID.
     */
    suspend fun delete(id: String)

    /**
     * Delete all memories of a specific type.
     */
    suspend fun deleteByType(type: MemoryType)

    /**
     * Get memories with importance above threshold.
     */
    suspend fun findByImportance(threshold: Float): List<MemoryEntry>

    /**
     * Update an existing memory entry.
     */
    suspend fun update(entry: MemoryEntry)

    /**
     * Clear all memories (use with caution).
     */
    suspend fun clearAll()
}
