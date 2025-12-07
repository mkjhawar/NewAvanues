// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.data

import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.ErrorReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ErrorReportRepo {
    
    private val dao get() = DatabaseManager.database.errorReportDao()
    
    // Direct CRUD implementation - no inheritance
    suspend fun insert(entity: ErrorReport): Long = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }

    suspend fun insertAll(entities: List<ErrorReport>): List<Long> = withContext(Dispatchers.IO) {
        dao.insertAll(entities)
    }

    suspend fun update(entity: ErrorReport) = withContext(Dispatchers.IO) {
        dao.update(entity)
    }

    suspend fun delete(entity: ErrorReport) = withContext(Dispatchers.IO) {
        dao.delete(entity)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    suspend fun getById(id: Long): ErrorReport? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun getAll(): List<ErrorReport> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun count(): Long = withContext(Dispatchers.IO) {
        dao.count()
    }

    suspend fun query(queryBuilder: () -> List<ErrorReport>): List<ErrorReport> = withContext(Dispatchers.IO) {
        queryBuilder()
    }
    
    suspend fun getUnsentReports(): List<ErrorReport> = withContext(Dispatchers.IO) {
        dao.getPendingReports()
    }
    
    suspend fun markAsSent(reportId: Long) = withContext(Dispatchers.IO) {
        dao.markAsSent(reportId, System.currentTimeMillis())
    }
    
    suspend fun getReportsByModule(module: String): List<ErrorReport> = withContext(Dispatchers.IO) {
        dao.getByModule(module)
    }
    
    suspend fun getRecentErrors(days: Int = 7): List<ErrorReport> = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        val endTime = System.currentTimeMillis()
        dao.getByTimeRange(cutoffTime, endTime)
    }
    
    suspend fun cleanupSentReports(olderThanDays: Int = 30) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        dao.deleteOlderThan(cutoffTime)
    }
    
    suspend fun getErrorRate(): Float = withContext(Dispatchers.IO) {
        val recentErrors = getRecentErrors(1)
        val totalCommands = 100 // This would come from command history
        
        if (totalCommands == 0) return@withContext 0f
        (recentErrors.size * 100f / totalCommands)
    }
    
    suspend fun logError(
        type: String,
        message: String,
        module: String,
        context: String? = null,
        commandText: String? = null
    ) = withContext(Dispatchers.IO) {
        val report = ErrorReport(
            errorType = type,
            errorMessage = message,
            context = sanitizeContext(context ?: ""),
            timestamp = System.currentTimeMillis(),
            commandText = anonymizeCommand(commandText),
            moduleAffected = module
        )
        dao.insert(report)
    }
    
    private fun sanitizeContext(context: String): String {
        // Remove any personal information from context
        return context
            .replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), "[email]")
            .replace(Regex("\\b\\d{3}-\\d{3}-\\d{4}\\b"), "[phone]")
            .replace(Regex("/Users/[^/]+"), "/Users/[user]")
    }
    
    private fun anonymizeCommand(command: String?): String? {
        // Remove any personal information from commands
        return command
            ?.replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), "[email]")
            ?.replace(Regex("\\b\\d{3}-\\d{3}-\\d{4}\\b"), "[phone]")
            ?.replace(Regex("\"[^\"]+\""), "\"[text]\"")
    }
}