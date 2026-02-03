/**
 * CommandHistoryRepository.kt - Repository for command history operations
 *
 * Provides a clean API for CommandHistory database operations.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.queries

import com.augmentalis.database.Command_history_entry
import com.augmentalis.database.VoiceOSDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommandHistoryRepository(private val database: VoiceOSDatabase) {

    private val queries = database.commandHistoryQueries

    suspend fun insert(
        originalText: String,
        processedCommand: String?,
        confidence: Double,
        timestamp: Long,
        language: String,
        engineUsed: String,
        success: Boolean,
        executionTimeMs: Long,
        usageCount: Long = 1,
        source: String = "VOICE"
    ) = withContext(Dispatchers.Default) {
        queries.insert(
            originalText = originalText,
            processedCommand = processedCommand,
            confidence = confidence,
            timestamp = timestamp,
            language = language,
            engineUsed = engineUsed,
            success = if (success) 1 else 0,
            executionTimeMs = executionTimeMs,
            usageCount = usageCount,
            source = source
        )
    }

    suspend fun getAll(): List<Command_history_entry> = withContext(Dispatchers.Default) {
        queries.getAll().executeAsList()
    }

    suspend fun getById(id: Long): Command_history_entry? = withContext(Dispatchers.Default) {
        queries.getById(id).executeAsOneOrNull()
    }

    suspend fun getByTimeRange(start: Long, end: Long): List<Command_history_entry> =
        withContext(Dispatchers.Default) {
            queries.getByTimeRange(start, end).executeAsList()
        }

    suspend fun getByLanguage(language: String): List<Command_history_entry> =
        withContext(Dispatchers.Default) {
            queries.getByLanguage(language).executeAsList()
        }

    suspend fun getByEngine(engine: String): List<Command_history_entry> =
        withContext(Dispatchers.Default) {
            queries.getByEngine(engine).executeAsList()
        }

    suspend fun getSuccessful(): List<Command_history_entry> = withContext(Dispatchers.Default) {
        queries.getSuccessful().executeAsList()
    }

    suspend fun getMostUsed(limit: Long): List<GetMostUsed> = withContext(Dispatchers.Default) {
        queries.getMostUsed(limit).executeAsList()
    }

    suspend fun getSuccessRate(): Double = withContext(Dispatchers.Default) {
        queries.getSuccessRate().executeAsOne()
    }

    suspend fun incrementUsage(id: Long) = withContext(Dispatchers.Default) {
        queries.incrementUsage(id)
    }

    suspend fun deleteOlderThan(timestamp: Long) = withContext(Dispatchers.Default) {
        queries.deleteOlderThan(timestamp)
    }

    suspend fun cleanupOldEntries(cutoffTime: Long, retainCount: Long) =
        withContext(Dispatchers.Default) {
            queries.cleanupOldEntries(retainCount, cutoffTime)
        }

    suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }
}

// Type alias for getMostUsed result
typealias GetMostUsed = com.augmentalis.database.GetMostUsed
