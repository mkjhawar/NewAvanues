// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.ErrorReportDTO

/**
 * Repository interface for error reports.
 * Stores errors for debugging and remote reporting.
 */
interface IErrorReportRepository {

    /**
     * Insert a new error report.
     * @return The ID of the inserted report.
     */
    suspend fun insert(report: ErrorReportDTO): Long

    /**
     * Get report by ID.
     */
    suspend fun getById(id: Long): ErrorReportDTO?

    /**
     * Get all reports.
     */
    suspend fun getAll(): List<ErrorReportDTO>

    /**
     * Get unsent reports (for batch sending).
     */
    suspend fun getUnsent(): List<ErrorReportDTO>

    /**
     * Get reports by error type.
     */
    suspend fun getByType(errorType: String): List<ErrorReportDTO>

    /**
     * Get recent reports with limit.
     */
    suspend fun getRecent(limit: Int): List<ErrorReportDTO>

    /**
     * Mark report as sent.
     */
    suspend fun markSent(id: Long)

    /**
     * Mark multiple reports as sent.
     */
    suspend fun markSentBatch(ids: List<Long>)

    /**
     * Delete report by ID.
     */
    suspend fun delete(id: Long)

    /**
     * Delete old reports.
     */
    suspend fun deleteOlderThan(timestamp: Long)

    /**
     * Delete all reports.
     */
    suspend fun deleteAll()

    /**
     * Count all reports.
     */
    suspend fun count(): Long

    /**
     * Count unsent reports.
     */
    suspend fun countUnsent(): Long
}
