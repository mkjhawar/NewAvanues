/**
 * VoskStorage.kt - Persistent storage and caching for VOSK engine
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * SOLID Principle: Single Responsibility
 * - Manages persistent storage for learned commands and vocabulary cache
 * - Handles Room integration for VOSK-specific data
 * - Provides thread-safe storage operations
 */
package com.augmentalis.voiceos.speech.engines.vosk

import android.content.Context
import android.util.Log
import com.augmentalis.datamanager.entities.EngineType
import com.augmentalis.datamanager.repositories.RecognitionLearningRepository
import kotlinx.coroutines.*
import java.util.Collections

/**
 * Storage manager for VOSK engine.
 * Handles persistent storage of learned commands, vocabulary cache,
 * and command history using Room.
 */
class VoskStorage(
    private val context: Context,
    private val config: VoskConfig
) {
    
    companion object {
        private const val TAG = "VoskStorage"
        private const val MAX_LEARNED_COMMANDS = 1000
        private const val MAX_VOCABULARY_CACHE = 2000
        private const val CLEANUP_INTERVAL_MS = 3600000L // 1 hour
        private const val MAX_SAVE_BATCH_SIZE = 50
    }
    
    // Room integration
    private lateinit var learningRepository: RecognitionLearningRepository
    private var isInitialized = false
    
    // In-memory caches for performance
    private val learnedCommands = Collections.synchronizedMap(mutableMapOf<String, String>())
    private val vocabularyCache = Collections.synchronizedMap(mutableMapOf<String, Boolean>())
    private val commandHistory = Collections.synchronizedList(arrayListOf<CommandHistory>())
    
    // Storage metrics
    private var loadOperations = 0L
    private var saveOperations = 0L
    private var lastCleanupTime = System.currentTimeMillis()
    private var totalBytesLoaded = 0L
    private var totalBytesSaved = 0L
    
    // Coroutine management
    private val storageScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cleanupJob: Job? = null
    
    /**
     * Initialize storage system
     */
    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Initializing VOSK storage system...")
                
                // Initialize Room learning store
                learningRepository = RecognitionLearningRepository.getInstance(context)
                learningRepository.initialize()
                
                // Load cached data
                loadAllData()
                
                // Start periodic cleanup
                startPeriodicCleanup()
                
                isInitialized = true
                Log.i(TAG, "VOSK storage system initialized successfully")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize storage system: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * Load all cached data from ObjectBox
     */
    private suspend fun loadAllData() {
        try {
            loadOperations++
            val startTime = System.currentTimeMillis()
            
            // Load learned commands
            val loadedCommands = learningRepository.getLearnedCommands(EngineType.VOSK)
            learnedCommands.clear()
            learnedCommands.putAll(loadedCommands)
            
            // Load vocabulary cache
            val loadedVocabulary = learningRepository.getVocabularyCache(EngineType.VOSK)
            vocabularyCache.clear()
            vocabularyCache.putAll(loadedVocabulary)
            
            val loadTime = System.currentTimeMillis() - startTime
            totalBytesLoaded += estimateDataSize(loadedCommands, loadedVocabulary)
            
            Log.i(TAG, "Loaded ${learnedCommands.size} learned commands and ${vocabularyCache.size} vocabulary entries in ${loadTime}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cached data: ${e.message}", e)
        }
    }
    
    /**
     * Save learned command
     */
    suspend fun saveLearnedCommand(recognized: String, matched: String) {
        if (!isInitialized) {
            Log.w(TAG, "Storage not initialized, cannot save learned command")
            return
        }
        
        withContext(Dispatchers.IO) {
            try {
                val normalizedRecognized = recognized.lowercase().trim()
                val normalizedMatched = matched.lowercase().trim()
                
                if (normalizedRecognized.isBlank() || normalizedMatched.isBlank()) {
                    Log.w(TAG, "Invalid command to save: '$recognized' -> '$matched'")
                    return@withContext
                }
                
                // Update in-memory cache
                learnedCommands[normalizedRecognized] = normalizedMatched
                
                // Record command history
                recordCommandHistory(normalizedRecognized, normalizedMatched, "learned")
                
                // Clean up if cache too large
                if (learnedCommands.size > MAX_LEARNED_COMMANDS) {
                    cleanupLearnedCommands()
                }
                
                // Save to Room
                learningRepository.saveLearnedCommand(EngineType.VOSK, normalizedRecognized, normalizedMatched)
                saveOperations++
                
                Log.d(TAG, "Saved learned command: '$normalizedRecognized' -> '$normalizedMatched'")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save learned command: ${e.message}", e)
            }
        }
    }
    
    /**
     * Save vocabulary cache entry
     */
    suspend fun saveVocabularyEntry(word: String, isValid: Boolean) {
        if (!isInitialized) {
            Log.w(TAG, "Storage not initialized, cannot save vocabulary entry")
            return
        }
        
        withContext(Dispatchers.IO) {
            try {
                val normalizedWord = word.lowercase().trim()
                
                if (normalizedWord.isBlank()) {
                    Log.w(TAG, "Invalid word to save: '$word'")
                    return@withContext
                }
                
                // Update in-memory cache
                vocabularyCache[normalizedWord] = isValid
                
                // Clean up if cache too large
                if (vocabularyCache.size > MAX_VOCABULARY_CACHE) {
                    cleanupVocabularyCache()
                }
                
                Log.d(TAG, "Saved vocabulary entry: '$normalizedWord' -> $isValid")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save vocabulary entry: ${e.message}", e)
            }
        }
    }
    
    /**
     * Save vocabulary cache in batch
     */
    suspend fun saveVocabularyCacheBatch(entries: Map<String, Boolean>) {
        if (!isInitialized || entries.isEmpty()) {
            return
        }
        
        withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                var savedCount = 0
                
                // Process in chunks to avoid overwhelming ObjectBox
                entries.toList().chunked(MAX_SAVE_BATCH_SIZE).forEach { chunk ->
                    val chunkMap = chunk.associate { (word, isValid) ->
                        word.lowercase().trim() to isValid
                    }.filter { it.key.isNotBlank() }
                    
                    // Update in-memory cache
                    vocabularyCache.putAll(chunkMap)
                    savedCount += chunkMap.size
                }
                
                // Clean up if cache too large
                if (vocabularyCache.size > MAX_VOCABULARY_CACHE) {
                    cleanupVocabularyCache()
                }
                
                // Save to Room
                learningRepository.saveVocabularyCache(EngineType.VOSK, vocabularyCache.toMap())
                saveOperations++
                
                val saveTime = System.currentTimeMillis() - startTime
                totalBytesSaved += estimateVocabularySize(entries)
                
                Log.d(TAG, "Saved vocabulary cache batch: $savedCount entries in ${saveTime}ms")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save vocabulary cache batch: ${e.message}", e)
            }
        }
    }
    
    /**
     * Get learned command
     */
    fun getLearnedCommand(recognized: String): String? {
        val normalized = recognized.lowercase().trim()
        return learnedCommands[normalized]
    }
    
    /**
     * Check if word is in vocabulary cache
     */
    fun isWordInVocabulary(word: String): Boolean? {
        val normalized = word.lowercase().trim()
        return vocabularyCache[normalized]
    }
    
    /**
     * Get all learned commands
     */
    fun getAllLearnedCommands(): Map<String, String> {
        return learnedCommands.toMap()
    }
    
    /**
     * Get vocabulary cache
     */
    fun getVocabularyCache(): Map<String, Boolean> {
        return vocabularyCache.toMap()
    }
    
    /**
     * Clear learned commands
     */
    suspend fun clearLearnedCommands() {
        withContext(Dispatchers.IO) {
            try {
                learnedCommands.clear()
                learningRepository.clearLearnedCommands(EngineType.VOSK)
                Log.i(TAG, "Cleared all learned commands")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear learned commands: ${e.message}", e)
            }
        }
    }
    
    /**
     * Clear vocabulary cache
     */
    suspend fun clearVocabularyCache() {
        withContext(Dispatchers.IO) {
            try {
                vocabularyCache.clear()
                learningRepository.clearVocabularyCache(EngineType.VOSK)
                Log.i(TAG, "Cleared vocabulary cache")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear vocabulary cache: ${e.message}", e)
            }
        }
    }
    
    /**
     * Record command usage history
     */
    private fun recordCommandHistory(recognized: String, matched: String, type: String) {
        val history = CommandHistory(
            recognized = recognized,
            matched = matched,
            type = type,
            timestamp = System.currentTimeMillis()
        )
        
        commandHistory.add(history)
        
        // Keep only last 1000 history entries
        if (commandHistory.size > 1000) {
            commandHistory.removeAt(0)
        }
    }
    
    /**
     * Cleanup learned commands when cache is full
     */
    private suspend fun cleanupLearnedCommands() {
        try {
            Log.d(TAG, "Cleaning up learned commands cache...")
            
            // Keep most recently used commands (simple LRU simulation)
            // For now, we'll just remove oldest entries
            val entriesToRemove = learnedCommands.size - (MAX_LEARNED_COMMANDS * 0.8).toInt()
            
            if (entriesToRemove > 0) {
                val keysToRemove = learnedCommands.keys.take(entriesToRemove)
                keysToRemove.forEach { key ->
                    learnedCommands.remove(key)
                }
                
                // Update Room
                learningRepository.saveLearnedCommands(EngineType.VOSK, learnedCommands.toMap())
                
                Log.d(TAG, "Removed $entriesToRemove old learned commands")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup learned commands: ${e.message}", e)
        }
    }
    
    /**
     * Cleanup vocabulary cache when full
     */
    private suspend fun cleanupVocabularyCache() {
        try {
            Log.d(TAG, "Cleaning up vocabulary cache...")
            
            // Keep most recently used entries
            val entriesToRemove = vocabularyCache.size - (MAX_VOCABULARY_CACHE * 0.8).toInt()
            
            if (entriesToRemove > 0) {
                val keysToRemove = vocabularyCache.keys.take(entriesToRemove)
                keysToRemove.forEach { key ->
                    vocabularyCache.remove(key)
                }
                
                // Update Room
                learningRepository.saveVocabularyCache(EngineType.VOSK, vocabularyCache.toMap())
                
                Log.d(TAG, "Removed $entriesToRemove old vocabulary entries")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup vocabulary cache: ${e.message}", e)
        }
    }
    
    /**
     * Start periodic cleanup task
     */
    private fun startPeriodicCleanup() {
        cleanupJob = storageScope.launch {
            while (isActive) {
                delay(CLEANUP_INTERVAL_MS)
                
                try {
                    val currentTime = System.currentTimeMillis()
                    
                    // Cleanup old command history
                    val cutoffTime = currentTime - (24 * 60 * 60 * 1000L) // 24 hours
                    commandHistory.removeAll { it.timestamp < cutoffTime }
                    
                    // Perform cache cleanup if needed
                    if (learnedCommands.size > MAX_LEARNED_COMMANDS * 0.9) {
                        cleanupLearnedCommands()
                    }
                    
                    if (vocabularyCache.size > MAX_VOCABULARY_CACHE * 0.9) {
                        cleanupVocabularyCache()
                    }
                    
                    lastCleanupTime = currentTime
                    Log.d(TAG, "Periodic cleanup completed")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error during periodic cleanup: ${e.message}", e)
                }
            }
        }
    }
    
    /**
     * Estimate data size for metrics
     */
    private fun estimateDataSize(commands: Map<String, String>, vocabulary: Map<String, Boolean>): Long {
        var size = 0L
        commands.forEach { (key, value) -> size += key.length + value.length }
        vocabulary.forEach { (key, _) -> size += key.length + 1 }
        return size
    }
    
    /**
     * Estimate vocabulary size for metrics
     */
    private fun estimateVocabularySize(vocabulary: Map<String, Boolean>): Long {
        var size = 0L
        vocabulary.forEach { (key, _) -> size += key.length + 1 }
        return size
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up storage resources...")
            
            // Cancel cleanup job
            cleanupJob?.cancel()
            
            // Close Room store
            if (isInitialized) {
                // Room handles connection management automatically
            }
            
            // Clear in-memory caches
            learnedCommands.clear()
            vocabularyCache.clear()
            commandHistory.clear()
            
            // Cancel scope
            storageScope.coroutineContext.cancelChildren()
            
            isInitialized = false
            
            Log.d(TAG, "Storage cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during storage cleanup: ${e.message}", e)
        }
    }
    
    // Getters for state and metrics
    fun isInitialized(): Boolean = isInitialized
    fun getLearnedCommandsCount(): Int = learnedCommands.size
    fun getVocabularyCacheSize(): Int = vocabularyCache.size
    fun getCommandHistorySize(): Int = commandHistory.size
    fun getLoadOperations(): Long = loadOperations
    fun getSaveOperations(): Long = saveOperations
    fun getLastCleanupTime(): Long = lastCleanupTime
    
    /**
     * Get storage statistics
     */
    fun getStorageStats(): StorageStats {
        return StorageStats(
            isInitialized = isInitialized,
            learnedCommandsCount = learnedCommands.size,
            vocabularyCacheSize = vocabularyCache.size,
            commandHistorySize = commandHistory.size,
            loadOperations = loadOperations,
            saveOperations = saveOperations,
            totalBytesLoaded = totalBytesLoaded,
            totalBytesSaved = totalBytesSaved,
            lastCleanupTime = lastCleanupTime
        )
    }
    
    /**
     * Get diagnostic information
     */
    fun getDiagnostics(): Map<String, Any> {
        return mapOf(
            "initialized" to isInitialized,
            "learnedCommandsCount" to learnedCommands.size,
            "maxLearnedCommands" to MAX_LEARNED_COMMANDS,
            "vocabularyCacheSize" to vocabularyCache.size,
            "maxVocabularyCache" to MAX_VOCABULARY_CACHE,
            "commandHistorySize" to commandHistory.size,
            "loadOperations" to loadOperations,
            "saveOperations" to saveOperations,
            "totalBytesLoaded" to totalBytesLoaded,
            "totalBytesSaved" to totalBytesSaved,
            "lastCleanupTime" to lastCleanupTime,
            "timeSinceLastCleanup" to (System.currentTimeMillis() - lastCleanupTime)
        )
    }
    
    /**
     * Get command history
     */
    fun getCommandHistory(): List<CommandHistory> = commandHistory.toList()
    
    /**
     * Data class for storage statistics
     */
    data class StorageStats(
        val isInitialized: Boolean,
        val learnedCommandsCount: Int,
        val vocabularyCacheSize: Int,
        val commandHistorySize: Int,
        val loadOperations: Long,
        val saveOperations: Long,
        val totalBytesLoaded: Long,
        val totalBytesSaved: Long,
        val lastCleanupTime: Long
    )
    
    /**
     * Data class for command history tracking
     */
    data class CommandHistory(
        val recognized: String,
        val matched: String,
        val type: String,
        val timestamp: Long
    )
}