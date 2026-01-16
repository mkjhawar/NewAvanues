package com.augmentalis.memory

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * High-level interface for memory management in AVA.
 * Handles memory operations, decay, consolidation, and retrieval.
 */
interface MemoryManager {
    /**
     * Add a new memory to the system.
     */
    suspend fun remember(
        type: MemoryType,
        content: String,
        importance: Float = 0.5f,
        metadata: Map<String, String> = emptyMap()
    ): MemoryEntry

    /**
     * Retrieve a specific memory by ID.
     */
    suspend fun recall(id: String): MemoryEntry?

    /**
     * Search for relevant memories based on a query.
     */
    suspend fun search(query: String, limit: Int = 10): List<MemoryEntry>

    /**
     * Get conversation history (short-term memories).
     */
    suspend fun getConversationHistory(limit: Int = 50): List<MemoryEntry>

    /**
     * Get user preferences (long-term preference memories).
     */
    suspend fun getUserPreferences(): Map<String, String>

    /**
     * Update a user preference.
     */
    suspend fun setUserPreference(key: String, value: String)

    /**
     * Consolidate memories (move from short-term to long-term storage).
     * This simulates memory consolidation in human cognition.
     */
    suspend fun consolidateMemories()

    /**
     * Apply memory decay - reduce importance of old, rarely accessed memories.
     */
    suspend fun applyDecay()

    /**
     * Summarize a conversation for long-term storage.
     */
    suspend fun summarizeConversation(conversationId: String): String

    /**
     * Forget a specific memory.
     */
    suspend fun forget(id: String)

    /**
     * Clear all short-term memories.
     */
    suspend fun clearShortTerm()

    /**
     * Observe changes to memories of a specific type.
     */
    fun observeMemories(type: MemoryType): Flow<List<MemoryEntry>>
}
