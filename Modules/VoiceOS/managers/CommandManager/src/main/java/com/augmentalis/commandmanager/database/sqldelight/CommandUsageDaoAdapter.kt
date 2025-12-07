/**
 * CommandUsageDaoAdapter.kt - SQLDelight adapter for CommandUsageDao
 *
 * Purpose: Bridge between Room-style DAO interface and SQLDelight queries
 * Provides the same API as Room CommandUsageDao for backward compatibility
 */

package com.augmentalis.commandmanager.database.sqldelight

import com.augmentalis.database.VoiceOSDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data class matching Room CommandUsageEntity structure
 */
data class CommandUsageEntity(
    val id: Long = 0,
    val commandId: String,
    val locale: String,
    val timestamp: Long,
    val userInput: String,
    val matchType: String,
    val success: Boolean,
    val executionTimeMs: Long,
    val contextApp: String? = null
) {
    companion object {
        fun success(
            commandId: String,
            locale: String,
            userInput: String,
            matchType: String,
            executionTimeMs: Long,
            contextApp: String? = null
        ): CommandUsageEntity {
            return CommandUsageEntity(
                commandId = commandId,
                locale = locale,
                timestamp = System.currentTimeMillis(),
                userInput = userInput,
                matchType = matchType,
                success = true,
                executionTimeMs = executionTimeMs,
                contextApp = contextApp
            )
        }

        fun failure(
            userInput: String,
            locale: String,
            executionTimeMs: Long,
            contextApp: String? = null
        ): CommandUsageEntity {
            return CommandUsageEntity(
                commandId = "UNKNOWN",
                locale = locale,
                timestamp = System.currentTimeMillis(),
                userInput = userInput,
                matchType = "NONE",
                success = false,
                executionTimeMs = executionTimeMs,
                contextApp = contextApp
            )
        }
    }
}

/**
 * Result class for usage statistics query
 */
data class CommandUsageStats(
    val commandId: String,
    val usageCount: Int
)

/**
 * Result class for success rate query
 */
data class CommandSuccessRate(
    val commandId: String,
    val totalAttempts: Int,
    val successfulAttempts: Int
) {
    fun getSuccessPercentage(): Float {
        return if (totalAttempts > 0) {
            (successfulAttempts.toFloat() / totalAttempts) * 100f
        } else {
            0f
        }
    }
}

/**
 * SQLDelight adapter implementing CommandUsageDao-like interface
 */
class CommandUsageDaoAdapter(private val database: VoiceOSDatabase) {

    private val queries = database.commandUsageQueries

    // ==================== INSERT ====================

    suspend fun recordUsage(usage: CommandUsageEntity): Long = withContext(Dispatchers.IO) {
        queries.recordUsage(
            command_id = usage.commandId,
            locale = usage.locale,
            timestamp = usage.timestamp,
            user_input = usage.userInput,
            match_type = usage.matchType,
            success = if (usage.success) 1L else 0L,
            execution_time_ms = usage.executionTimeMs,
            context_app = usage.contextApp,
            context_key = usage.contextApp ?: ""
        )
        queries.transactionWithResult {
            queries.lastInsertRowId().executeAsOne()
        }
    }

    // ==================== QUERY ====================

    suspend fun getUsageForCommand(commandId: String): List<CommandUsageEntity> = withContext(Dispatchers.IO) {
        queries.getUsageForCommand(commandId).executeAsList().map { it.toEntity() }
    }

    suspend fun getMostUsedCommands(limit: Int = 10): List<CommandUsageStats> = withContext(Dispatchers.IO) {
        queries.getMostUsedCommands(limit.toLong()).executeAsList().map {
            CommandUsageStats(
                commandId = it.command_id,
                usageCount = it.usage_count.toInt()
            )
        }
    }

    suspend fun getUsageInPeriod(startTime: Long, endTime: Long): List<CommandUsageEntity> = withContext(Dispatchers.IO) {
        queries.getUsageInPeriod(startTime, endTime).executeAsList().map { it.toEntity() }
    }

    suspend fun getSuccessRates(): List<CommandSuccessRate> = withContext(Dispatchers.IO) {
        queries.getSuccessRates().executeAsList().map {
            CommandSuccessRate(
                commandId = it.command_id,
                totalAttempts = (it.total_attempts ?: 0L).toInt(),
                successfulAttempts = (it.successful_attempts ?: 0L).toInt()
            )
        }
    }

    suspend fun getTotalUsageCount(): Int = withContext(Dispatchers.IO) {
        queries.getTotalUsageCount().executeAsOne().toInt()
    }

    suspend fun getFailedAttempts(limit: Int = 50): List<CommandUsageEntity> = withContext(Dispatchers.IO) {
        queries.getFailedAttempts(limit.toLong()).executeAsList().map { it.toEntity() }
    }

    suspend fun getAverageExecutionTime(commandId: String): Float? = withContext(Dispatchers.IO) {
        queries.getAverageExecutionTime(commandId).executeAsOneOrNull()?.avg_time?.toFloat()
    }

    suspend fun getUsageByApp(appPackage: String): List<CommandUsageEntity> = withContext(Dispatchers.IO) {
        queries.getUsageByApp(appPackage).executeAsList().map { it.toEntity() }
    }

    // ==================== DELETE (Privacy) ====================

    suspend fun deleteOldRecords(cutoffTime: Long): Int = withContext(Dispatchers.IO) {
        // Count before delete
        val countBefore = queries.countTotalUsage().executeAsOne()
        queries.deleteOldRecords(cutoffTime)
        val countAfter = queries.countTotalUsage().executeAsOne()
        (countBefore - countAfter).toInt()
    }

    suspend fun deleteAllRecords(): Int = withContext(Dispatchers.IO) {
        val count = queries.countTotalUsage().executeAsOne().toInt()
        queries.deleteAllRecords()
        count
    }

    suspend fun deleteCommandRecords(commandId: String): Int = withContext(Dispatchers.IO) {
        val count = queries.countUsageForCommand(commandId).executeAsOne().toInt()
        queries.deleteCommandRecords(commandId)
        count
    }

    // ==================== EXTENSION ====================

    private fun com.augmentalis.database.command.Command_usage.toEntity(): CommandUsageEntity = CommandUsageEntity(
        id = id,
        commandId = command_id,
        locale = locale,
        timestamp = timestamp,
        userInput = user_input,
        matchType = match_type,
        success = success == 1L,
        executionTimeMs = execution_time_ms,
        contextApp = context_app
    )
}
