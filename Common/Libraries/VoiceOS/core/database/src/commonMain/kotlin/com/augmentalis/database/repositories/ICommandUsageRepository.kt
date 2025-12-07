// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.CommandUsageDTO

/**
 * Repository interface for command usage tracking.
 * Abstracts database implementation (SQLDelight).
 */
interface ICommandUsageRepository {

    /**
     * Insert a new usage record.
     * @return The ID of the inserted record.
     */
    suspend fun insert(usage: CommandUsageDTO): Long

    /**
     * Get all usage records.
     */
    suspend fun getAll(): List<CommandUsageDTO>

    /**
     * Get usage records for a specific command.
     */
    suspend fun getForCommand(commandId: String): List<CommandUsageDTO>

    /**
     * Get usage records for a specific context.
     */
    suspend fun getForContext(contextKey: String): List<CommandUsageDTO>

    /**
     * Get usage records for a specific command in a specific context.
     */
    suspend fun getForCommandInContext(commandId: String, contextKey: String): List<CommandUsageDTO>

    /**
     * Get recent usage records.
     */
    suspend fun getRecent(limit: Long): List<CommandUsageDTO>

    /**
     * Count usage records for a command.
     */
    suspend fun countForCommand(commandId: String): Long

    /**
     * Count usage records for a context.
     */
    suspend fun countForContext(contextKey: String): Long

    /**
     * Count total usage records.
     */
    suspend fun countTotal(): Long

    /**
     * Get success rate for a command (0.0 to 1.0).
     */
    suspend fun getSuccessRate(commandId: String): Double

    /**
     * Delete oldest records, keeping only the most recent N.
     */
    suspend fun deleteOldest(keepCount: Long)

    /**
     * Delete all usage records.
     */
    suspend fun deleteAll()

    /**
     * Record a command usage event.
     * Convenience method for PreferenceLearner.
     */
    suspend fun recordUsage(
        commandId: String,
        locale: String,
        timestamp: Long,
        userInput: String,
        matchType: String,
        success: Boolean,
        executionTimeMs: Long,
        contextApp: String?
    ): Long

    /**
     * Get aggregated statistics for a command.
     * Returns total executions, successful executions, and failed executions.
     */
    suspend fun getStatsForCommand(commandId: String): CommandStats

    /**
     * Apply time decay to usage counts.
     * Reduces the weight of older usage records.
     */
    suspend fun applyTimeDecay(currentTime: Long, decayFactor: Float)
}

/**
 * Command statistics data class.
 */
data class CommandStats(
    val totalExecutions: Int = 0,
    val successfulExecutions: Int = 0,
    val failedExecutions: Int = 0
) {
    val successRate: Float
        get() = if (totalExecutions > 0) {
            successfulExecutions.toFloat() / totalExecutions
        } else {
            0f
        }
}
