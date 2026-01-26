// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.ErrorReportDTO
import com.augmentalis.database.dto.toErrorReportDTO
import com.augmentalis.database.repositories.IErrorReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IErrorReportRepository.
 */
class SQLDelightErrorReportRepository(
    private val database: VoiceOSDatabase
) : IErrorReportRepository {

    private val queries = database.errorReportQueries

    override suspend fun insert(report: ErrorReportDTO): Long = withContext(Dispatchers.Default) {
        queries.insert(
            errorType = report.errorType,
            errorMessage = report.message,
            stackTrace = report.stackTrace,
            context = report.context,
            commandText = null,
            deviceId = null,
            timestamp = report.timestamp,
            isSent = if (report.isSent) 1L else 0L
        )
        // Get last insert ID
        database.transactionWithResult {
            queries.count().executeAsOne()
        }
    }

    override suspend fun getById(id: Long): ErrorReportDTO? = withContext(Dispatchers.Default) {
        queries.getById(id).executeAsOneOrNull()?.toErrorReportDTO()
    }

    override suspend fun getAll(): List<ErrorReportDTO> = withContext(Dispatchers.Default) {
        queries.getRecent(Long.MAX_VALUE).executeAsList().map { it.toErrorReportDTO() }
    }

    override suspend fun getUnsent(): List<ErrorReportDTO> = withContext(Dispatchers.Default) {
        queries.getUnsent().executeAsList().map { it.toErrorReportDTO() }
    }

    override suspend fun getByType(errorType: String): List<ErrorReportDTO> = withContext(Dispatchers.Default) {
        queries.getByType(errorType).executeAsList().map { it.toErrorReportDTO() }
    }

    override suspend fun getRecent(limit: Int): List<ErrorReportDTO> = withContext(Dispatchers.Default) {
        queries.getRecent(limit.toLong()).executeAsList().map { it.toErrorReportDTO() }
    }

    override suspend fun markSent(id: Long) = withContext(Dispatchers.Default) {
        queries.markSent(id)
    }

    override suspend fun markSentBatch(ids: List<Long>) = withContext(Dispatchers.Default) {
        database.transaction {
            ids.forEach { queries.markSent(it) }
        }
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteById(id)
    }

    override suspend fun deleteOlderThan(timestamp: Long) = withContext(Dispatchers.Default) {
        queries.deleteOlderThan(timestamp)
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }

    override suspend fun countUnsent(): Long = withContext(Dispatchers.Default) {
        queries.countUnsent().executeAsOne()
    }
}
