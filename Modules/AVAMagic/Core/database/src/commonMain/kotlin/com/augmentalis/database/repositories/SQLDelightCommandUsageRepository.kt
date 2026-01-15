// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.database.repositories

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.CommandUsageDTO
import com.augmentalis.database.dto.toDTO
import com.augmentalis.database.repositories.CommandStats
import com.augmentalis.database.repositories.ICommandUsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

/**
 * SQLDelight implementation of ICommandUsageRepository.
 */
class SQLDelightCommandUsageRepository(
    private val database: VoiceOSDatabase
) : ICommandUsageRepository {

    private val queries = database.commandUsageQueries

    override suspend fun insert(usage: CommandUsageDTO): Long = withContext(Dispatchers.Default) {
        database.transactionWithResult {
            queries.insertUsage(
                command_id = usage.commandId,
                context_key = usage.contextKey,
                success = usage.success,
                timestamp = usage.timestamp
            )
            queries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun getAll(): List<CommandUsageDTO> = withContext(Dispatchers.Default) {
        queries.getAllUsage().executeAsList().map { usage: com.augmentalis.database.command.Command_usage ->
            usage.toDTO()
        }
    }

    override suspend fun getForCommand(commandId: String): List<CommandUsageDTO> =
        withContext(Dispatchers.Default) {
            queries.getUsageForCommand(commandId).executeAsList().map { usage: com.augmentalis.database.command.Command_usage ->
                usage.toDTO()
            }
        }

    override suspend fun getForContext(contextKey: String): List<CommandUsageDTO> =
        withContext(Dispatchers.Default) {
            queries.getUsageForContext(contextKey).executeAsList().map { usage: com.augmentalis.database.command.Command_usage ->
                usage.toDTO()
            }
        }

    override suspend fun getForCommandInContext(
        commandId: String,
        contextKey: String
    ): List<CommandUsageDTO> = withContext(Dispatchers.Default) {
        queries.getUsageForCommandInContext(commandId, contextKey).executeAsList().map { it.toDTO() }
    }

    override suspend fun getRecent(limit: Long): List<CommandUsageDTO> =
        withContext(Dispatchers.Default) {
            queries.getRecentUsage(limit).executeAsList().map { it.toDTO() }
        }

    override suspend fun countForCommand(commandId: String): Long =
        withContext(Dispatchers.Default) {
            queries.countUsageForCommand(commandId).executeAsOne()
        }

    override suspend fun countForContext(contextKey: String): Long =
        withContext(Dispatchers.Default) {
            queries.countUsageForContext(contextKey).executeAsOne()
        }

    override suspend fun countTotal(): Long = withContext(Dispatchers.Default) {
        queries.countTotalUsage().executeAsOne()
    }

    override suspend fun getSuccessRate(commandId: String): Double =
        withContext(Dispatchers.Default) {
            queries.getSuccessRateForCommand(commandId).executeAsOne() ?: 0.0
        }

    override suspend fun deleteOldest(keepCount: Long) = withContext(Dispatchers.Default) {
        val total = queries.countTotalUsage().executeAsOne()
        if (total > keepCount) {
            val deleteCount = total - keepCount
            queries.deleteOldestRecords(deleteCount)
        }
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAllUsage()
    }

    override suspend fun recordUsage(
        commandId: String,
        locale: String,
        timestamp: Long,
        userInput: String,
        matchType: String,
        success: Boolean,
        executionTimeMs: Long,
        contextApp: String?
    ): Long = withContext(Dispatchers.Default) {
        val dto = CommandUsageDTO(
            id = 0,
            commandId = commandId,
            contextKey = contextApp ?: "unknown",
            success = if (success) 1L else 0L,
            timestamp = timestamp
        )
        insert(dto)
    }

    override suspend fun getStatsForCommand(commandId: String): CommandStats =
        withContext(Dispatchers.Default) {
            val usages = queries.getUsageForCommand(commandId).executeAsList()
            val total = usages.size
            val successful = usages.count { it.success == 1L }
            val failed = total - successful

            CommandStats(
                totalExecutions = total,
                successfulExecutions = successful,
                failedExecutions = failed
            )
        }

    override suspend fun applyTimeDecay(currentTime: Long, decayFactor: Float) =
        withContext(Dispatchers.Default) {
            // CommandUsage table tracks individual events, not aggregated scores
            // Time decay is applied by deleting old records rather than reducing scores
            // Delete records older than the cutoff time
            database.transactionWithResult {
                queries.deleteOldRecords(currentTime)
            }
        }
}
