/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.memory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Desktop (JVM) implementation of MemoryManager.
 *
 * Provides high-level memory operations with:
 * - Automatic ID generation
 * - Memory decay simulation
 * - Consolidation from short-term to long-term
 * - Conversation history management
 * - User preference storage
 *
 * Uses FileBasedMemoryStore for persistent storage on desktop.
 */
class DefaultMemoryManager(
    private val store: MemoryStore = FileBasedMemoryStore()
) : MemoryManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Observable state for each memory type
    private val memoryFlows = mutableMapOf<MemoryType, MutableStateFlow<List<MemoryEntry>>>()

    // Decay configuration
    private val decayRate = 0.05f // 5% decay per day
    private val minImportanceThreshold = 0.1f // Memories below this are candidates for removal

    init {
        // Initialize flows for each memory type
        MemoryType.entries.forEach { type ->
            memoryFlows[type] = MutableStateFlow(emptyList())
        }

        // Load initial state
        scope.launch {
            refreshAllFlows()
        }
    }

    override suspend fun remember(
        type: MemoryType,
        content: String,
        importance: Float,
        metadata: Map<String, String>
    ): MemoryEntry {
        val entry = MemoryEntry(
            id = generateId(),
            type = type,
            content = content,
            timestamp = Clock.System.now(),
            importance = importance.coerceIn(0f, 1f),
            metadata = metadata
        )

        store.store(entry)
        refreshFlow(type)

        println("[DefaultMemoryManager] Stored ${type.name} memory: ${entry.id}")
        return entry
    }

    override suspend fun recall(id: String): MemoryEntry? {
        val entry = store.retrieve(id) ?: return null

        // Update access tracking
        val updatedEntry = entry.withAccess(Clock.System.now())
        store.update(updatedEntry)

        return updatedEntry
    }

    override suspend fun search(query: String, limit: Int): List<MemoryEntry> {
        return store.search(query, limit)
    }

    override suspend fun getConversationHistory(limit: Int): List<MemoryEntry> {
        return store.findByType(MemoryType.SHORT_TERM)
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    override suspend fun getUserPreferences(): Map<String, String> {
        val prefMemories = store.findByType(MemoryType.LONG_TERM_PREFERENCES)
        val preferences = mutableMapOf<String, String>()

        prefMemories.forEach { memory ->
            // Extract preference from metadata
            memory.metadata["preference_key"]?.let { key ->
                preferences[key] = memory.content
            }
        }

        return preferences
    }

    override suspend fun setUserPreference(key: String, value: String) {
        // Check if preference already exists
        val existing = store.findByType(MemoryType.LONG_TERM_PREFERENCES)
            .find { it.metadata["preference_key"] == key }

        if (existing != null) {
            // Update existing preference
            val updated = existing.copy(
                content = value,
                timestamp = Clock.System.now()
            )
            store.update(updated)
        } else {
            // Create new preference
            remember(
                type = MemoryType.LONG_TERM_PREFERENCES,
                content = value,
                importance = 0.8f, // Preferences are high importance
                metadata = mapOf("preference_key" to key)
            )
        }

        println("[DefaultMemoryManager] Set preference: $key = $value")
    }

    override suspend fun consolidateMemories() {
        println("[DefaultMemoryManager] Starting memory consolidation...")

        val shortTermMemories = store.findByType(MemoryType.SHORT_TERM)
        val now = Clock.System.now()
        val consolidationThreshold = 1.hours

        var consolidatedCount = 0

        shortTermMemories.forEach { memory ->
            val age = now - memory.timestamp

            // Consolidate memories older than threshold with high importance
            if (age > consolidationThreshold && memory.importance >= 0.6f) {
                // Move to episodic memory
                val episodicMemory = memory.copy(
                    id = generateId(),
                    type = MemoryType.EPISODIC,
                    timestamp = Clock.System.now(),
                    metadata = memory.metadata + ("original_id" to memory.id)
                )

                store.store(episodicMemory)
                store.delete(memory.id)
                consolidatedCount++
            }
        }

        refreshFlow(MemoryType.SHORT_TERM)
        refreshFlow(MemoryType.EPISODIC)

        println("[DefaultMemoryManager] Consolidated $consolidatedCount memories")
    }

    override suspend fun applyDecay() {
        println("[DefaultMemoryManager] Applying memory decay...")

        val now = Clock.System.now()
        var decayedCount = 0
        var removedCount = 0

        // Apply decay to all memory types except preferences
        listOf(MemoryType.SHORT_TERM, MemoryType.EPISODIC, MemoryType.SEMANTIC).forEach { type ->
            val memories = store.findByType(type)

            memories.forEach { memory ->
                val daysSinceLastAccess = if (memory.lastAccessed != null) {
                    (now - memory.lastAccessed).inWholeDays.toFloat()
                } else {
                    (now - memory.timestamp).inWholeDays.toFloat()
                }

                // Calculate decay based on time since last access
                val decay = (daysSinceLastAccess * decayRate).coerceAtMost(0.5f)
                val newImportance = (memory.importance - decay).coerceAtLeast(0f)

                when {
                    newImportance < minImportanceThreshold -> {
                        // Remove very low importance memories
                        store.delete(memory.id)
                        removedCount++
                    }
                    newImportance < memory.importance -> {
                        // Update with decayed importance
                        store.update(memory.withImportance(newImportance))
                        decayedCount++
                    }
                }
            }

            refreshFlow(type)
        }

        println("[DefaultMemoryManager] Decay applied: $decayedCount decayed, $removedCount removed")
    }

    override suspend fun summarizeConversation(conversationId: String): String {
        val conversationMemories = store.findByType(MemoryType.SHORT_TERM)
            .filter { it.metadata["conversation_id"] == conversationId }
            .sortedBy { it.timestamp }

        if (conversationMemories.isEmpty()) {
            return "No conversation found with ID: $conversationId"
        }

        // Simple summarization - in production, this would use LLM
        val messageCount = conversationMemories.size
        val firstMessage = conversationMemories.first()
        val lastMessage = conversationMemories.last()
        val duration = lastMessage.timestamp - firstMessage.timestamp

        return buildString {
            appendLine("Conversation Summary ($conversationId)")
            appendLine("- Messages: $messageCount")
            appendLine("- Duration: ${duration.inWholeMinutes} minutes")
            appendLine("- Started: ${firstMessage.timestamp}")
            appendLine("- Topics: ${extractTopics(conversationMemories)}")
        }
    }

    override suspend fun forget(id: String) {
        val entry = store.retrieve(id)
        if (entry != null) {
            store.delete(id)
            refreshFlow(entry.type)
            println("[DefaultMemoryManager] Forgot memory: $id")
        }
    }

    override suspend fun clearShortTerm() {
        store.deleteByType(MemoryType.SHORT_TERM)
        refreshFlow(MemoryType.SHORT_TERM)
        println("[DefaultMemoryManager] Cleared short-term memories")
    }

    override fun observeMemories(type: MemoryType): Flow<List<MemoryEntry>> {
        return memoryFlows[type]?.map { it } ?: MutableStateFlow(emptyList())
    }

    /**
     * Generate a unique ID for a memory entry
     */
    private fun generateId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Refresh the flow for a specific memory type
     */
    private suspend fun refreshFlow(type: MemoryType) {
        val memories = store.findByType(type)
        memoryFlows[type]?.value = memories
    }

    /**
     * Refresh all memory type flows
     */
    private suspend fun refreshAllFlows() {
        MemoryType.entries.forEach { type ->
            refreshFlow(type)
        }
    }

    /**
     * Extract topics from conversation memories (simple keyword extraction)
     */
    private fun extractTopics(memories: List<MemoryEntry>): String {
        val words = memories.flatMap { it.content.lowercase().split(" ") }
            .filter { it.length > 4 }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        return words.joinToString(", ").ifEmpty { "general" }
    }

    /**
     * Get statistics about memory usage
     */
    suspend fun getStatistics(): MemoryStatistics {
        val shortTerm = store.findByType(MemoryType.SHORT_TERM).size
        val longTerm = store.findByType(MemoryType.LONG_TERM_PREFERENCES).size
        val episodic = store.findByType(MemoryType.EPISODIC).size
        val semantic = store.findByType(MemoryType.SEMANTIC).size

        return MemoryStatistics(
            shortTermCount = shortTerm,
            longTermPreferencesCount = longTerm,
            episodicCount = episodic,
            semanticCount = semantic,
            totalCount = shortTerm + longTerm + episodic + semantic
        )
    }
}

/**
 * Memory usage statistics
 */
data class MemoryStatistics(
    val shortTermCount: Int,
    val longTermPreferencesCount: Int,
    val episodicCount: Int,
    val semanticCount: Int,
    val totalCount: Int
)
