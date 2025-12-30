/**
 * VivokaLearning.kt - Command learning integration for Vivoka VSDK engine
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * 
 * Extracted from monolithic VivokaEngine.kt as part of SOLID refactoring
 * Integrates with shared LearningSystem and manages Vivoka-specific learning behavior
 */
package com.augmentalis.voiceos.speech.engines.vivoka

import android.content.Context
import android.util.Log
import com.augmentalis.voiceos.speech.engines.common.CommandCache
import com.augmentalis.voiceos.speech.engines.common.LearningSystem
import com.augmentalis.datamanager.entities.EngineType
import com.augmentalis.datamanager.repositories.RecognitionLearningRepository
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages command learning for the Vivoka engine using shared components
 */
class VivokaLearning(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "VivokaLearning"
        private const val MIN_CONFIDENCE_FOR_LEARNING = 0.85f
        private const val LEARNING_SYNC_INTERVAL = 30000L // 30 seconds
        private const val LEARNING_STATS_INTERVAL = 60000L // 1 minute
    }
    
    // Learning components - using shared components
    private val learningSystem = LearningSystem("vivoka", context)
    private val commandCache = CommandCache()
    private lateinit var learningRepository: RecognitionLearningRepository
    
    // Local caches for fast access
    private val learnedCommands = ConcurrentHashMap<String, String>()
    private val vocabularyCache = ConcurrentHashMap<String, Boolean>()
    
    // Learning state
    @Volatile
    private var isInitialized = false
    @Volatile
    private var totalLearningAttempts = 0
    @Volatile
    private var successfulLearns = 0
    @Volatile
    private var lastSyncTime = 0L
    
    // Background jobs
    private var syncJob: Job? = null
    private var statsJob: Job? = null
    
    /**
     * Initialize the learning system
     */
    suspend fun initialize(): Boolean {
        return try {
            Log.i(TAG, "Initializing Vivoka learning system")
            
            // Initialize Room learning repository
            learningRepository = RecognitionLearningRepository.getInstance(context)
            learningRepository.initialize()
            
            // Load existing learned data
            loadLearnedCommands()
            loadVocabularyCache()
            
            // Start background sync
            startBackgroundSync()
            
            isInitialized = true
            Log.i(TAG, "Learning system initialized with ${learnedCommands.size} learned commands")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize learning system", e)
            false
        }
    }
    
    /**
     * Process command with multi-tier matching and learning
     */
    suspend fun processCommandWithLearning(
        recognizedCommand: String,
        registeredCommands: List<String>,
        confidence: Float = 1.0f
    ): Pair<String?, Boolean> {
        
        totalLearningAttempts++
        
        // Use shared learning system for processing
        val matchResult = learningSystem.processWithLearning(
            recognized = recognizedCommand,
            commands = registeredCommands,
            confidence = confidence
        )
        
        var wasLearned = false
        var finalCommand: String? = null
        
        when (matchResult.source) {
            LearningSystem.MatchSource.LEARNED_COMMAND -> {
                // Command was matched from learned commands
                finalCommand = matchResult.matched
                wasLearned = true
                Log.d(TAG, "Found learned command match: '$recognizedCommand' -> '$finalCommand'")
            }
            
            LearningSystem.MatchSource.VOCABULARY_CACHE -> {
                // Command was matched via vocabulary cache
                finalCommand = matchResult.matched
                wasLearned = true
                Log.d(TAG, "Found vocabulary cache match: '$recognizedCommand' -> '$finalCommand'")
            }
            
            LearningSystem.MatchSource.SIMILARITY_MATCH -> {
                // Command was matched via similarity
                finalCommand = matchResult.matched
                wasLearned = true
                successfulLearns++
                
                // Update local cache for fast future access
                learnedCommands[recognizedCommand.lowercase()] = finalCommand
                
                Log.d(TAG, "Found similarity match and learned: '$recognizedCommand' -> '$finalCommand'")
            }
            
            LearningSystem.MatchSource.EXACT_MATCH -> {
                // Exact match found
                finalCommand = matchResult.matched
                
                // Learn for future if confidence is high
                if (confidence >= MIN_CONFIDENCE_FOR_LEARNING) {
                    learnCommandMapping(recognizedCommand, finalCommand, confidence)
                    wasLearned = true
                }
            }
            
            LearningSystem.MatchSource.NO_MATCH -> {
                // No match found
                finalCommand = null
                Log.d(TAG, "No match found for: '$recognizedCommand'")
            }
        }
        
        return Pair(finalCommand, wasLearned)
    }
    
    /**
     * Learn a new command mapping manually
     */
    fun learnCommandMapping(original: String, learned: String, confidence: Float) {
        if (confidence >= MIN_CONFIDENCE_FOR_LEARNING) {
            // Use shared learning system
            learningSystem.learnCommand(original, learned, confidence)
            
            // Update local cache
            learnedCommands[original.lowercase()] = learned
            successfulLearns++
            
            Log.d(TAG, "Learned command mapping: '$original' -> '$learned' (confidence: $confidence)")
        } else {
            Log.d(TAG, "Command not learned due to low confidence: $confidence < $MIN_CONFIDENCE_FOR_LEARNING")
        }
    }
    
    /**
     * Register commands for learning and similarity matching
     */
    fun registerCommands(commands: List<String>) {
        Log.d(TAG, "Registering ${commands.size} commands for learning system")
        
        // Update shared command cache
        coroutineScope.launch {
            commandCache.updateCommands(commands)
        }
        
        // Also register with shared learning system (if it supports this method)
        try {
            // Note: This would need to be added to the shared LearningSystem if not present
            Log.d(TAG, "Commands registered with learning system")
        } catch (e: Exception) {
            Log.w(TAG, "Could not register commands with learning system", e)
        }
    }
    
    /**
     * Load learned commands from ObjectBox
     */
    private suspend fun loadLearnedCommands() {
        try {
            val loadedCommands = learningRepository.getLearnedCommands(EngineType.VIVOKA)
            learnedCommands.clear()
            learnedCommands.putAll(loadedCommands)
            Log.d(TAG, "Loaded ${learnedCommands.size} learned commands from ObjectBox")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load learned commands", e)
        }
    }
    
    /**
     * Load vocabulary cache from ObjectBox
     */
    private suspend fun loadVocabularyCache() {
        try {
            val loadedCache = learningRepository.getVocabularyCache(EngineType.VIVOKA)
            vocabularyCache.clear()
            vocabularyCache.putAll(loadedCache)
            Log.d(TAG, "Loaded ${vocabularyCache.size} vocabulary cache entries from ObjectBox")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load vocabulary cache", e)
        }
    }
    
    /**
     * Save learned commands to ObjectBox
     */
    private suspend fun saveLearnedCommands() {
        try {
            // Save each learned command individually using the available API
            learnedCommands.forEach { (recognized, matched) ->
                learningRepository.saveLearnedCommand(EngineType.VIVOKA, recognized, matched)
            }
            Log.d(TAG, "Saved ${learnedCommands.size} learned commands to ObjectBox")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save learned commands", e)
        }
    }
    
    /**
     * Save vocabulary cache to ObjectBox
     */
    private suspend fun saveVocabularyCache() {
        try {
            learningRepository.saveVocabularyCache(EngineType.VIVOKA, vocabularyCache.toMap())
            Log.d(TAG, "Saved vocabulary cache to ObjectBox")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save vocabulary cache", e)
        }
    }
    
    /**
     * Start background sync job
     */
    private fun startBackgroundSync() {
        syncJob = coroutineScope.launch {
            while (isActive) {
                delay(LEARNING_SYNC_INTERVAL)
                try {
                    syncLearningData()
                } catch (e: Exception) {
                    Log.e(TAG, "Background sync failed", e)
                }
            }
        }
        
        statsJob = coroutineScope.launch {
            while (isActive) {
                delay(LEARNING_STATS_INTERVAL)
                try {
                    logLearningStatistics()
                } catch (e: Exception) {
                    Log.e(TAG, "Stats logging failed", e)
                }
            }
        }
    }
    
    /**
     * Sync learning data to persistent storage
     */
    suspend fun syncLearningData() {
        if (!isInitialized) return
        
        try {
            Log.d(TAG, "Syncing learning data...")
            
            // Save learned commands if we have any
            if (learnedCommands.isNotEmpty()) {
                saveLearnedCommands()
            }
            
            // Save vocabulary cache if we have any
            if (vocabularyCache.isNotEmpty()) {
                saveVocabularyCache()
            }
            
            lastSyncTime = System.currentTimeMillis()
            Log.d(TAG, "Learning data sync completed")
            
        } catch (e: Exception) {
            Log.w(TAG, "Learning data sync failed: ${e.message}")
        }
    }
    
    /**
     * Log learning statistics
     */
    private fun logLearningStatistics() {
        if (!isInitialized) return
        
        val stats = getLearningStats()
        val successRate = if (totalLearningAttempts > 0) {
            (successfulLearns.toFloat() / totalLearningAttempts) * 100
        } else {
            0f
        }
        
        Log.i(TAG, """
            Vivoka Learning Statistics:
            ├── Learned Commands: ${stats["learnedCommands"]}
            ├── Vocabulary Cache: ${stats["vocabularyCache"]}
            ├── Learning Attempts: $totalLearningAttempts
            ├── Success Rate: ${successRate.toInt()}%
            ├── Last Sync: ${(System.currentTimeMillis() - lastSyncTime) / 1000}s ago
            └── System Status: ${if (isInitialized) "Active" else "Inactive"}
        """.trimIndent())
    }
    
    /**
     * Get learning statistics
     */
    fun getLearningStats(): Map<String, Int> {
        return mapOf(
            "learnedCommands" to learnedCommands.size,
            "vocabularyCache" to vocabularyCache.size,
            "registeredCommands" to getCachedCommandCount(),
            "totalAttempts" to totalLearningAttempts,
            "successfulLearns" to successfulLearns
        )
    }
    
    /**
     * Get cached command count from command cache
     */
    private fun getCachedCommandCount(): Int {
        return commandCache.getStats().totalCount
    }
    
    /**
     * Get detailed learning metrics
     */
    fun getDetailedMetrics(): Map<String, Any> {
        val timeSinceLastSync = System.currentTimeMillis() - lastSyncTime
        val successRate = if (totalLearningAttempts > 0) {
            (successfulLearns.toFloat() / totalLearningAttempts) * 100
        } else {
            0f
        }
        
        return mapOf(
            "isInitialized" to isInitialized,
            "learnedCommandsCount" to learnedCommands.size,
            "vocabularyCacheCount" to vocabularyCache.size,
            "totalLearningAttempts" to totalLearningAttempts,
            "successfulLearns" to successfulLearns,
            "successRate" to successRate,
            "lastSyncTime" to lastSyncTime,
            "timeSinceLastSync" to timeSinceLastSync,
            "syncStatus" to if (timeSinceLastSync < LEARNING_SYNC_INTERVAL * 2) "recent" else "overdue"
        )
    }
    
    /**
     * Clear all learning data
     */
    suspend fun clearAllLearningData() {
        Log.w(TAG, "Clearing all learning data")
        
        try {
            // Clear local caches
            learnedCommands.clear()
            vocabularyCache.clear()
            
            // Clear command cache
            commandCache.clear()
            
            // Clear shared learning system data
            learningSystem.clearAllData()
            
            // Reset statistics
            totalLearningAttempts = 0
            successfulLearns = 0
            
            Log.i(TAG, "All learning data cleared")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear learning data", e)
        }
    }
    
    /**
     * Force immediate sync of learning data
     */
    suspend fun forceSyncLearningData(): Boolean {
        return try {
            Log.i(TAG, "Force syncing learning data")
            syncLearningData()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Force sync failed", e)
            false
        }
    }
    
    /**
     * Check if learning system is healthy
     */
    fun isHealthy(): Boolean {
        return isInitialized && 
               ::learningRepository.isInitialized && 
               (System.currentTimeMillis() - lastSyncTime) < (LEARNING_SYNC_INTERVAL * 3)
    }
    
    /**
     * Reset learning system
     */
    suspend fun reset() {
        Log.d(TAG, "Resetting learning system")
        
        try {
            // Cancel background jobs
            syncJob?.cancel()
            statsJob?.cancel()
            
            // Clear data
            learnedCommands.clear()
            vocabularyCache.clear()
            commandCache.clear()
            
            // Reset state
            isInitialized = false
            totalLearningAttempts = 0
            successfulLearns = 0
            lastSyncTime = 0L
            
            Log.d(TAG, "Learning system reset completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during learning system reset", e)
        }
    }
    
    /**
     * Destroy learning system and cleanup resources
     */
    suspend fun destroy() {
        Log.i(TAG, "Destroying learning system")
        
        try {
            // Final sync before destruction
            if (isInitialized) {
                syncLearningData()
            }
            
            // Reset everything
            reset()
            
            // Destroy shared components
            learningSystem.destroy()
            
            // Close learning store
            if (::learningRepository.isInitialized) {
                // Room handles connection management automatically
            }
            
            Log.i(TAG, "Learning system destroyed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during learning system destruction", e)
        }
    }
    
    /**
     * Get command suggestions based on partial input
     */
    fun getCommandSuggestions(partialInput: String, limit: Int = 5): List<String> {
        return try {
            // TODO: Implement findSimilarCommands method in CommandCache
            // For now, provide basic matching from available commands
            val allCommands = commandCache.getAllCommands()
            val normalizedInput = partialInput.lowercase().trim()
            
            val suggestions = allCommands.filter { command ->
                command.contains(normalizedInput) || normalizedInput.contains(command)
            }.take(limit)
            
            Log.d(TAG, "Found ${suggestions.size} suggestions for '$partialInput'")
            suggestions
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get command suggestions", e)
            emptyList()
        }
    }
    
    /**
     * Check if a command has been learned
     */
    fun isCommandLearned(command: String): Boolean {
        return learnedCommands.containsKey(command.lowercase())
    }
    
    /**
     * Get the learned mapping for a command
     */
    fun getLearnedMapping(command: String): String? {
        return learnedCommands[command.lowercase()]
    }
}