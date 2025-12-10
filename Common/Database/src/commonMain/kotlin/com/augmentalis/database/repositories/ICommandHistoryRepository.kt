// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.avanues.database.repositories

import com.avanues.database.dto.CommandHistoryDTO

/**
 * Repository interface for command execution history.
 * Provides analytics and history tracking.
 */
interface ICommandHistoryRepository {

    /**
     * Insert a new history entry.
     * @return The ID of the inserted entry.
     */
    suspend fun insert(entry: CommandHistoryDTO): Long

    /**
     * Get entry by ID.
     */
    suspend fun getById(id: Long): CommandHistoryDTO?

    /**
     * Get all history entries.
     */
    suspend fun getAll(): List<CommandHistoryDTO>

    /**
     * Get entries within a time range.
     */
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<CommandHistoryDTO>

    /**
     * Get entries after a certain time.
     */
    suspend fun getAfterTime(timestamp: Long): List<CommandHistoryDTO>

    /**
     * Get only successful entries.
     */
    suspend fun getSuccessful(): List<CommandHistoryDTO>

    /**
     * Get entries by engine used.
     */
    suspend fun getByEngine(engine: String): List<CommandHistoryDTO>

    /**
     * Get entries by language.
     */
    suspend fun getByLanguage(language: String): List<CommandHistoryDTO>

    /**
     * Get recent entries with limit.
     */
    suspend fun getRecent(limit: Int): List<CommandHistoryDTO>

    /**
     * Calculate success rate (0.0 to 1.0).
     */
    suspend fun getSuccessRate(): Double

    /**
     * Calculate average execution time in milliseconds.
     */
    suspend fun getAverageExecutionTime(): Double

    /**
     * Delete entries older than a timestamp.
     */
    suspend fun deleteOlderThan(timestamp: Long)

    /**
     * Cleanup old entries, retaining a maximum count.
     */
    suspend fun cleanupOldEntries(cutoffTime: Long, retainCount: Long)

    /**
     * Delete all entries.
     */
    suspend fun deleteAll()

    /**
     * Count all entries.
     */
    suspend fun count(): Long

    /**
     * Count successful entries.
     */
    suspend fun countSuccessful(): Long
}
