/**
 * RecognitionLearningRepository.kt - Room repository for speech recognition learning data
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * 
 * Provides high-performance Room operations for speech recognition learning
 * Migrated from ObjectBox to Room for better compatibility
 */
package com.augmentalis.datamanager.repositories

import android.content.Context
import android.util.Log
import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.dao.RecognitionLearningDao
import com.augmentalis.datamanager.entities.RecognitionLearning
import com.augmentalis.datamanager.entities.LearningType
import com.augmentalis.datamanager.entities.EngineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing RecognitionLearning data using Room
 * Provides thread-safe, high-performance operations for all speech engines
 */
class RecognitionLearningRepository(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "RecognitionLearningRepo"
        
        @Volatile
        private var INSTANCE: RecognitionLearningRepository? = null
        
        fun getInstance(context: Context): RecognitionLearningRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecognitionLearningRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Room DAO
    private val recognitionLearningDao: RecognitionLearningDao by lazy {
        DatabaseManager.database.recognitionLearningDao()
    }
    
    /**
     * Initialize the repository
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "🗄️ Initializing RecognitionLearningRepository...")
            
            val totalEntries = recognitionLearningDao.getAll().size
            Log.i(TAG, "🗄️ Repository initialized with $totalEntries learning entries")
            
        } catch (e: Exception) {
            Log.e(TAG, "🗄️ Failed to initialize repository: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Load all learned commands for a specific engine
     */
    suspend fun getLearnedCommands(engine: String): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val results = recognitionLearningDao.getByEngineAndType(engine, LearningType.LEARNED_COMMAND)
            
            val commands = results.associate { it.keyValue to it.mappedValue }
            Log.d(TAG, "🧠 $engine: Loaded ${commands.size} learned commands from Room")
            
            return@withContext commands
        } catch (e: Exception) {
            Log.e(TAG, "🧠 $engine: Failed to load learned commands: ${e.message}", e)
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
            val existing = recognitionLearningDao.getByEngineTypeAndKey(engine, LearningType.LEARNED_COMMAND, recognized)
            
            if (existing != null) {
                // Update existing
                existing.mappedValue = matched
                existing.confidence = confidence
                existing.lastUsed = System.currentTimeMillis()
                existing.usageCount++
                recognitionLearningDao.update(existing)
                Log.d(TAG, "🧠 $engine: Updated learned command: '$recognized' → '$matched' (usage: ${existing.usageCount})")
            } else {
                // Create new
                val newLearning = RecognitionLearning(
                    engine = engine,
                    type = LearningType.LEARNED_COMMAND,
                    keyValue = recognized,
                    mappedValue = matched,
                    confidence = confidence,
                    timestamp = System.currentTimeMillis(),
                    lastUsed = System.currentTimeMillis(),
                    usageCount = 1
                )
                recognitionLearningDao.insert(newLearning)
                Log.d(TAG, "🧠 $engine: Saved new learned command: '$recognized' → '$matched'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "🧠 $engine: Failed to save learned command: ${e.message}", e)
        }
    }
    
    /**
     * Load vocabulary cache for a specific engine
     */
    suspend fun getVocabularyCache(engine: String): Map<String, Boolean> = withContext(Dispatchers.IO) {
        try {
            val results = recognitionLearningDao.getByEngineAndType(engine, LearningType.VOCABULARY_CACHE)
            
            val cache = results.associate { 
                it.keyValue to (it.mappedValue == "true")
            }
            Log.d(TAG, "🧠 $engine: Loaded ${cache.size} vocabulary cache entries from Room")
            
            return@withContext cache
        } catch (e: Exception) {
            Log.e(TAG, "🧠 $engine: Failed to load vocabulary cache: ${e.message}", e)
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
                val existing = recognitionLearningDao.getByEngineTypeAndKey(
                    engine, 
                    LearningType.VOCABULARY_CACHE, 
                    word
                )
                
                if (existing != null) {
                    // Update existing
                    existing.mappedValue = isValid.toString()
                    existing.lastUsed = System.currentTimeMillis()
                    existing.usageCount++
                    recognitionLearningDao.update(existing)
                } else {
                    // Create new
                    val newCache = RecognitionLearning(
                        engine = engine,
                        type = LearningType.VOCABULARY_CACHE,
                        keyValue = word,
                        mappedValue = isValid.toString(),
                        timestamp = System.currentTimeMillis(),
                        lastUsed = System.currentTimeMillis(),
                        usageCount = 1
                    )
                    recognitionLearningDao.insert(newCache)
                }
            }
            Log.d(TAG, "🧠 $engine: Saved ${vocabulary.size} vocabulary cache entries")
        } catch (e: Exception) {
            Log.e(TAG, "🧠 $engine: Failed to save vocabulary cache: ${e.message}", e)
        }
    }
    
    /**
     * Check if a learned command exists for a specific engine
     */
    suspend fun hasLearnedCommand(engine: String, recognized: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = recognitionLearningDao.getByEngineTypeAndKey(
                engine, 
                LearningType.LEARNED_COMMAND, 
                recognized
            )
            return@withContext existing != null
        } catch (e: Exception) {
            Log.e(TAG, "🧠 $engine: Failed to check learned command: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * Get a specific learned command for an engine
     */
    suspend fun getLearnedCommand(engine: String, recognized: String): String? = withContext(Dispatchers.IO) {
        try {
            val existing = recognitionLearningDao.getByEngineTypeAndKey(
                engine, 
                LearningType.LEARNED_COMMAND, 
                recognized
            )
            
            existing?.let {
                // Update usage statistics
                it.lastUsed = System.currentTimeMillis()
                it.usageCount++
                recognitionLearningDao.update(it)
                return@withContext it.mappedValue
            }
            
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "🧠 $engine: Failed to get learned command: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Get learning statistics for monitoring
     */
    suspend fun getLearningStats(engine: String): Map<String, Int> = withContext(Dispatchers.IO) {
        try {
            val learnedCount = recognitionLearningDao.getByEngineAndType(
                engine, 
                LearningType.LEARNED_COMMAND
            ).size
            
            val vocabularyCount = recognitionLearningDao.getByEngineAndType(
                engine, 
                LearningType.VOCABULARY_CACHE
            ).size
            
            return@withContext mapOf(
                "learnedCommands" to learnedCount,
                "vocabularyCache" to vocabularyCount,
                "totalEntries" to (learnedCount + vocabularyCount)
            )
        } catch (e: Exception) {
            Log.e(TAG, "🧠 $engine: Failed to get learning stats: ${e.message}", e)
            return@withContext emptyMap()
        }
    }
    
    /**
     * Clear all learning data for a specific engine
     */
    suspend fun clearLearningData(engine: String) = withContext(Dispatchers.IO) {
        try {
            val count = recognitionLearningDao.deleteByEngine(engine)
            Log.i(TAG, "🧠 $engine: Cleared $count learning entries")
        } catch (e: Exception) {
            Log.e(TAG, "🧠 $engine: Failed to clear learning data: ${e.message}", e)
        }
    }

    /**
     * Clear only learned commands for a specific engine
     */
    suspend fun clearLearnedCommands(engine: String) = withContext(Dispatchers.IO) {
        try {
            val count = recognitionLearningDao.deleteByEngineAndType(engine, LearningType.LEARNED_COMMAND)
            Log.i(TAG, "🧠 $engine: Cleared $count learned command entries")
        } catch (e: Exception) {
            Log.e(TAG, "🧠 $engine: Failed to clear learned commands: ${e.message}", e)
        }
    }

    /**
     * Clear only vocabulary cache for a specific engine
     */
    suspend fun clearVocabularyCache(engine: String) = withContext(Dispatchers.IO) {
        try {
            val count = recognitionLearningDao.deleteByEngineAndType(engine, LearningType.VOCABULARY_CACHE)
            Log.i(TAG, "🧠 $engine: Cleared $count vocabulary cache entries")
        } catch (e: Exception) {
            Log.e(TAG, "🧠 $engine: Failed to clear vocabulary cache: ${e.message}", e)
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
            Log.d(TAG, "🧠 $engine: Saved ${commands.size} learned commands in batch")
        } catch (e: Exception) {
            Log.e(TAG, "🧠 $engine: Failed to save learned commands batch: ${e.message}", e)
        }
    }
    
    /**
     * Cleanup old learning data (older than specified days)
     */
    suspend fun cleanupOldData(maxAgeDays: Int = 90) = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
            val count = recognitionLearningDao.deleteOlderThan(cutoffTime)
            Log.i(TAG, "🧠 Cleaned up $count old learning entries (older than $maxAgeDays days)")
        } catch (e: Exception) {
            Log.e(TAG, "🧠 Failed to cleanup old data: ${e.message}", e)
        }
    }
}