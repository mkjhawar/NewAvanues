// Author: Manoj Jhawar

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.CommandHistoryDTO
import com.augmentalis.database.dto.toCommandHistoryDTO
import com.augmentalis.database.repositories.ICommandHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of ICommandHistoryRepository.
 */
class SQLDelightCommandHistoryRepository(
    private val database: VoiceOSDatabase
) : ICommandHistoryRepository {

    private val queries = database.commandHistoryQueries

    override suspend fun insert(entry: CommandHistoryDTO): Long = withContext(Dispatchers.Default) {
        queries.insert(
            originalText = entry.originalText,
            processedCommand = entry.processedCommand,
            confidence = entry.confidence,
            timestamp = entry.timestamp,
            language = entry.language,
            engineUsed = entry.engineUsed,
            success = if (entry.success) 1L else 0L,
            executionTimeMs = entry.executionTimeMs,
            usageCount = 1L,
            source = "VOICE"
        )
        queries.lastInsertRowId().executeAsOne()
    }

    override suspend fun getById(id: Long): CommandHistoryDTO? = withContext(Dispatchers.Default) {
        queries.getById(id).executeAsOneOrNull()?.let { entry: com.augmentalis.database.Command_history_entry ->
            entry.toCommandHistoryDTO()
        }
    }

    override suspend fun getAll(): List<CommandHistoryDTO> = withContext(Dispatchers.Default) {
        queries.getAll().executeAsList().map { entry: com.augmentalis.database.Command_history_entry ->
            entry.toCommandHistoryDTO()
        }
    }

    override suspend fun getByTimeRange(startTime: Long, endTime: Long): List<CommandHistoryDTO> =
        withContext(Dispatchers.Default) {
            queries.getByTimeRange(startTime, endTime).executeAsList().map { entry: com.augmentalis.database.Command_history_entry ->
                entry.toCommandHistoryDTO()
            }
        }

    override suspend fun getAfterTime(timestamp: Long): List<CommandHistoryDTO> =
        withContext(Dispatchers.Default) {
            queries.getAfterTime(timestamp).executeAsList().map { entry: com.augmentalis.database.Command_history_entry ->
                entry.toCommandHistoryDTO()
            }
        }

    override suspend fun getSuccessful(): List<CommandHistoryDTO> = withContext(Dispatchers.Default) {
        queries.getSuccessful().executeAsList().map { entry: com.augmentalis.database.Command_history_entry ->
            entry.toCommandHistoryDTO()
        }
    }

    override suspend fun getByEngine(engine: String): List<CommandHistoryDTO> =
        withContext(Dispatchers.Default) {
            queries.getByEngine(engine).executeAsList().map { entry: com.augmentalis.database.Command_history_entry ->
                entry.toCommandHistoryDTO()
            }
        }

    override suspend fun getByLanguage(language: String): List<CommandHistoryDTO> =
        withContext(Dispatchers.Default) {
            queries.getByLanguage(language).executeAsList().map { entry: com.augmentalis.database.Command_history_entry ->
                entry.toCommandHistoryDTO()
            }
        }

    override suspend fun getRecent(limit: Int): List<CommandHistoryDTO> =
        withContext(Dispatchers.Default) {
            queries.getRecent(limit.toLong()).executeAsList().map { entry: com.augmentalis.database.Command_history_entry ->
                entry.toCommandHistoryDTO()
            }
        }

    override suspend fun getSuccessRate(): Double = withContext(Dispatchers.Default) {
        queries.getSuccessRate().executeAsOne()
    }

    override suspend fun getAverageExecutionTime(): Double = withContext(Dispatchers.Default) {
        queries.getAverageExecutionTime().executeAsOne()
    }

    override suspend fun deleteOlderThan(timestamp: Long) = withContext(Dispatchers.Default) {
        queries.deleteOlderThan(timestamp)
    }

    override suspend fun cleanupOldEntries(cutoffTime: Long, retainCount: Long) =
        withContext(Dispatchers.Default) {
            queries.cleanupOldEntries(retainCount, cutoffTime)
        }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }

    override suspend fun countSuccessful(): Long = withContext(Dispatchers.Default) {
        queries.countSuccessful().executeAsOne()
    }
}
