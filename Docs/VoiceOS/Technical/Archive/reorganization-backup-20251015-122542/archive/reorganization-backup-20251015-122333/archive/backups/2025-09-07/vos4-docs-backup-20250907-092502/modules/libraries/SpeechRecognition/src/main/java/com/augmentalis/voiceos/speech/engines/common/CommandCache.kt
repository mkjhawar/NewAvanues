/**
 * CommandCache.kt - Unified command caching for all engines
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 */
package com.augmentalis.voiceos.speech.engines.common

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Unified command cache that provides thread-safe storage and matching
 * for static commands, dynamic commands, and vocabulary.
 * 
 * Used by all speech recognition engines to maintain consistent
 * command matching behavior across the system.
 */
class CommandCache {
    // Thread-safe collections for command storage
    private val staticCommands = Collections.synchronizedList(arrayListOf<String>())
    private val dynamicCommands = Collections.synchronizedList(arrayListOf<String>())
    private val vocabularyCache = ConcurrentHashMap<String, Boolean>()
    
    // Size limits to prevent unbounded growth
    private val MAX_STATIC_COMMANDS = 500
    private val MAX_DYNAMIC_COMMANDS = 200
    private val MAX_VOCABULARY_SIZE = 1000
    
    /**
     * Set static commands (highest priority)
     * These are pre-defined commands from static_commands.json
     */
    fun setStaticCommands(commands: List<String>) {
        staticCommands.clear()
        staticCommands.addAll(
            commands.map { it.lowercase().trim() }
                .distinct()
                .take(MAX_STATIC_COMMANDS)
        )
    }
    
    /**
     * Set dynamic commands (medium priority)
     * These are UI-scraped or context-specific commands
     */
    fun setDynamicCommands(commands: List<String>) {
        dynamicCommands.clear()
        dynamicCommands.addAll(
            commands.map { it.lowercase().trim() }
                .distinct()
                .take(MAX_DYNAMIC_COMMANDS)
        )
    }
    
    /**
     * Update commands (alias for setDynamicCommands for backwards compatibility)
     */
    fun updateCommands(commands: List<String>) {
        setDynamicCommands(commands)
    }
    
    /**
     * Add a single vocabulary word to cache
     * Uses LRU eviction when at capacity
     */
    fun addVocabularyWord(word: String, isValid: Boolean = true) {
        val normalized = word.lowercase().trim()
        
        // Simple LRU: remove oldest entry if at capacity
        if (vocabularyCache.size >= MAX_VOCABULARY_SIZE && !vocabularyCache.containsKey(normalized)) {
            // Remove first entry (oldest in iteration order)
            vocabularyCache.keys.firstOrNull()?.let { 
                vocabularyCache.remove(it)
            }
        }
        
        vocabularyCache[normalized] = isValid
    }
    
    /**
     * Find matching command in cache
     * Priority: Static > Dynamic > Vocabulary
     */
    fun findMatch(text: String): String? {
        val normalized = text.lowercase().trim()
        
        // Check static first (highest priority)
        staticCommands.find { it == normalized }?.let { return it }
        
        // Then dynamic
        dynamicCommands.find { it == normalized }?.let { return it }
        
        // Then cached vocabulary
        return if (vocabularyCache[normalized] == true) normalized else null
    }
    
    /**
     * Check if a command exists in any cache
     */
    fun hasCommand(text: String): Boolean {
        return findMatch(text) != null
    }
    
    /**
     * Get all commands (for grammar building)
     * Thread-safe implementation to avoid race conditions
     */
    fun getAllCommands(): List<String> {
        // Create defensive copies to avoid concurrent modification
        val staticCopy = synchronized(staticCommands) { staticCommands.toList() }
        val dynamicCopy = synchronized(dynamicCommands) { dynamicCommands.toList() }
        val vocabKeys = vocabularyCache.filter { it.value }.keys.toList()
        
        val allCommands = mutableSetOf<String>()
        allCommands.addAll(staticCopy)
        allCommands.addAll(dynamicCopy)
        allCommands.addAll(vocabKeys)
        return allCommands.toList()
    }
    
    /**
     * Clear all caches
     */
    fun clear() {
        staticCommands.clear()
        dynamicCommands.clear()
        vocabularyCache.clear()
    }
    
    /**
     * Get cache statistics for debugging
     */
    fun getStats(): CacheStats {
        return CacheStats(
            staticCount = staticCommands.size,
            dynamicCount = dynamicCommands.size,
            vocabularyCount = vocabularyCache.size,
            totalCount = getAllCommands().size
        )
    }
    
    /**
     * Data class for cache statistics
     */
    data class CacheStats(
        val staticCount: Int,
        val dynamicCount: Int,
        val vocabularyCount: Int,
        val totalCount: Int
    )
}