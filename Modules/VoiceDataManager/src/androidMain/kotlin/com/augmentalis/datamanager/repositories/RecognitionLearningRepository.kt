/**
 * RecognitionLearningRepository.kt - SQLDelight repository for speech recognition learning data
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * Updated: 2026-01-28 - Migrated to KMP structure, uses common data models
 *
 * Provides high-performance SQLDelight operations for speech recognition learning
 * Migrated from Room to SQLDelight for KMP compatibility
 */
package com.augmentalis.datamanager.repositories

import android.content.Context
import android.util.Log
import com.augmentalis.datamanager.LearningTypes
import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.database.RecognitionLearningQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing RecognitionLearning data using SQLDelight
 * Provides thread-safe, high-performance operations for all speech engines
 */
class RecognitionLearningRepository(
    private val context: Context
) {

    companion object {
        private const val TAG = "RecognitionLearningRepo"

        // Learning type constants - using common definitions
        const val TYPE_LEARNED_COMMAND = LearningTypes.TYPE_LEARNED_COMMAND
        const val TYPE_VOCABULARY_CACHE = LearningTypes.TYPE_VOCABULARY_CACHE

        @Volatile
        private var INSTANCE: RecognitionLearningRepository? = null

        fun getInstance(context: Context): RecognitionLearningRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecognitionLearningRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // SQLDelight queries
    private val recognitionLearningQueries: RecognitionLearningQueries by lazy {
        if (!DatabaseManager.isInitialized()){
            DatabaseManager.init(context)
        }
        DatabaseManager.recognitionLearningQueries
    }
    
    /**
     * Initialize the repository
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Initializing RecognitionLearningRepository...")

            val totalEntries = recognitionLearningQueries.getAll().executeAsList().size
            Log.i(TAG, "Repository initialized with $totalEntries learning entries")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize repository: ${e.message}", e)
            throw e
        }
    }

    /**
     * Load all learned commands for a specific engine
     */
    suspend fun getLearnedCommands(engine: String): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val results = recognitionLearningQueries.getByEngineAndType(engine, TYPE_LEARNED_COMMAND).executeAsList()

            val commands = results.associate { it.keyValue to it.learnedValue }
            Log.d(TAG, "$engine: Loaded ${commands.size} learned commands from SQLDelight")

            return@withContext commands
        } catch (e: Exception) {
            Log.e(TAG, "$engine: Failed to load learned commands: ${e.message}", e)
            return@withContext emptyMap()
        }
    }

    /**
     * Save a learned command for a specific engine
     */
    suspend fun saveLearnedCommand(
        engine: String,
        recognized: String,
        matched: String,
        confidence: Float = 1.0f
    ) = withContext(Dispatchers.IO) {
        try {
            // Check if already exists
            val existing = recognitionLearningQueries.getByEngineAndKey(engine, recognized).executeAsOneOrNull()

            if (existing != null && existing.type == TYPE_LEARNED_COMMAND) {
                // Update existing
                val currentTime = System.currentTimeMillis()
                recognitionLearningQueries.update(
                    learnedValue = matched,
                    confidence = confidence.toDouble(),
                    metadata = existing.metadata,
                    lastUsed = currentTime,
                    id = existing.id
                )
                recognitionLearningQueries.incrementUsage(currentTime, existing.id)
                Log.d(TAG, "$engine: Updated learned command: '$recognized' -> '$matched'")
            } else {
                // Create new
                val currentTime = System.currentTimeMillis()
                recognitionLearningQueries.insert(
                    engine = engine,
                    type = TYPE_LEARNED_COMMAND,
                    keyValue = recognized,
                    learnedValue = matched,
                    confidence = confidence.toDouble(),
                    usageCount = 1,
                    lastUsed = currentTime,
                    createdAt = currentTime,
                    metadata = null
                )
                Log.d(TAG, "$engine: Saved new learned command: '$recognized' -> '$matched'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "$engine: Failed to save learned command: ${e.message}", e)
        }
    }

    /**
     * Load vocabulary cache for a specific engine
     */
    suspend fun getVocabularyCache(engine: String): Map<String, Boolean> = withContext(Dispatchers.IO) {
        try {
            val results = recognitionLearningQueries.getByEngineAndType(engine, TYPE_VOCABULARY_CACHE).executeAsList()

            val cache = results.associate {
                it.keyValue to (it.learnedValue == "true")
            }
            Log.d(TAG, "$engine: Loaded ${cache.size} vocabulary cache entries from SQLDelight")

            return@withContext cache
        } catch (e: Exception) {
            Log.e(TAG, "$engine: Failed to load vocabulary cache: ${e.message}", e)
            return@withContext emptyMap()
        }
    }

    /**
     * Save vocabulary cache entry for a specific engine
     */
    suspend fun saveVocabularyCache(
        engine: String,
        vocabulary: Map<String, Boolean>
    ) = withContext(Dispatchers.IO) {
        try {
            vocabulary.forEach { (word, isValid) ->
                // Check if already exists
                val existing = recognitionLearningQueries.getByEngineAndKey(engine, word).executeAsOneOrNull()

                if (existing != null && existing.type == TYPE_VOCABULARY_CACHE) {
                    // Update existing
                    val currentTime = System.currentTimeMillis()
                    recognitionLearningQueries.update(
                        learnedValue = isValid.toString(),
                        confidence = existing.confidence,
                        metadata = existing.metadata,
                        lastUsed = currentTime,
                        id = existing.id
                    )
                    recognitionLearningQueries.incrementUsage(currentTime, existing.id)
                } else {
                    // Create new
                    val currentTime = System.currentTimeMillis()
                    recognitionLearningQueries.insert(
                        engine = engine,
                        type = TYPE_VOCABULARY_CACHE,
                        keyValue = word,
                        learnedValue = isValid.toString(),
                        confidence = 1.0,
                        usageCount = 1,
                        lastUsed = currentTime,
                        createdAt = currentTime,
                        metadata = null
                    )
                }
            }
            Log.d(TAG, "$engine: Saved ${vocabulary.size} vocabulary cache entries")
        } catch (e: Exception) {
            Log.e(TAG, "$engine: Failed to save vocabulary cache: ${e.message}", e)
        }
    }

    /**
     * Check if a learned command exists for a specific engine
     */
    suspend fun hasLearnedCommand(engine: String, recognized: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = recognitionLearningQueries.getByEngineAndKey(engine, recognized).executeAsOneOrNull()
            return@withContext existing != null && existing.type == TYPE_LEARNED_COMMAND
        } catch (e: Exception) {
            Log.e(TAG, "$engine: Failed to check learned command: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Get a specific learned command for an engine
     */
    suspend fun getLearnedCommand(engine: String, recognized: String): String? = withContext(Dispatchers.IO) {
        try {
            val existing = recognitionLearningQueries.getByEngineAndKey(engine, recognized).executeAsOneOrNull()

            if (existing != null && existing.type == TYPE_LEARNED_COMMAND) {
                // Update usage statistics
                val currentTime = System.currentTimeMillis()
                recognitionLearningQueries.incrementUsage(currentTime, existing.id)
                return@withContext existing.learnedValue
            }

            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "$engine: Failed to get learned command: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Get learning statistics for monitoring
     */
    suspend fun getLearningStats(engine: String): Map<String, Int> = withContext(Dispatchers.IO) {
        try {
            val learnedCount = recognitionLearningQueries.getByEngineAndType(
                engine,
                TYPE_LEARNED_COMMAND
            ).executeAsList().size

            val vocabularyCount = recognitionLearningQueries.getByEngineAndType(
                engine,
                TYPE_VOCABULARY_CACHE
            ).executeAsList().size

            return@withContext mapOf(
                "learnedCommands" to learnedCount,
                "vocabularyCache" to vocabularyCount,
                "totalEntries" to (learnedCount + vocabularyCount)
            )
        } catch (e: Exception) {
            Log.e(TAG, "$engine: Failed to get learning stats: ${e.message}", e)
            return@withContext emptyMap()
        }
    }

    /**
     * Clear all learning data for a specific engine
     */
    suspend fun clearLearningData(engine: String) = withContext(Dispatchers.IO) {
        try {
            recognitionLearningQueries.deleteByEngine(engine)
            Log.i(TAG, "$engine: Cleared learning entries")
        } catch (e: Exception) {
            Log.e(TAG, "$engine: Failed to clear learning data: ${e.message}", e)
        }
    }

    /**
     * Clear only learned commands for a specific engine
     */
    suspend fun clearLearnedCommands(engine: String) = withContext(Dispatchers.IO) {
        try {
            // SQLDelight doesn't have deleteByEngineAndType, so we need to get IDs first
            val entries = recognitionLearningQueries.getByEngineAndType(engine, TYPE_LEARNED_COMMAND).executeAsList()
            entries.forEach { entry ->
                recognitionLearningQueries.deleteById(entry.id)
            }
            Log.i(TAG, "$engine: Cleared ${entries.size} learned command entries")
        } catch (e: Exception) {
            Log.e(TAG, "$engine: Failed to clear learned commands: ${e.message}", e)
        }
    }

    /**
     * Clear only vocabulary cache for a specific engine
     */
    suspend fun clearVocabularyCache(engine: String) = withContext(Dispatchers.IO) {
        try {
            // SQLDelight doesn't have deleteByEngineAndType, so we need to get IDs first
            val entries = recognitionLearningQueries.getByEngineAndType(engine, TYPE_VOCABULARY_CACHE).executeAsList()
            entries.forEach { entry ->
                recognitionLearningQueries.deleteById(entry.id)
            }
            Log.i(TAG, "$engine: Cleared ${entries.size} vocabulary cache entries")
        } catch (e: Exception) {
            Log.e(TAG, "$engine: Failed to clear vocabulary cache: ${e.message}", e)
        }
    }

    /**
     * Save multiple learned commands in batch for a specific engine
     */
    suspend fun saveLearnedCommands(
        engine: String,
        commands: Map<String, String>
    ) = withContext(Dispatchers.IO) {
        try {
            commands.forEach { (recognized, matched) ->
                saveLearnedCommand(engine, recognized, matched)
            }
            Log.d(TAG, "$engine: Saved ${commands.size} learned commands in batch")
        } catch (e: Exception) {
            Log.e(TAG, "$engine: Failed to save learned commands batch: ${e.message}", e)
        }
    }

    /**
     * Cleanup old learning data (older than specified days)
     */
    suspend fun cleanupOldData(maxAgeDays: Int = 90) = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
            recognitionLearningQueries.deleteOlderThan(cutoffTime)
            Log.i(TAG, "Cleaned up old learning entries (older than $maxAgeDays days)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old data: ${e.message}", e)
        }
    }
}