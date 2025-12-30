// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.data

import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.CommandHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CommandHistory data class for test compatibility
 */
data class CommandHistory(
    val id: Long,
    val command: String,
    val success: Boolean,
    val timestamp: Long
)

class CommandHistoryRepo {
    
    private val dao get() = DatabaseManager.database.commandHistoryEntryDao()
    
    // Direct CRUD implementation - no inheritance
    suspend fun insert(entity: CommandHistoryEntry): Long = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }

    suspend fun insertAll(entities: List<CommandHistoryEntry>): List<Long> = withContext(Dispatchers.IO) {
        dao.insertAll(entities)
    }

    suspend fun update(entity: CommandHistoryEntry) = withContext(Dispatchers.IO) {
        dao.update(entity)
    }

    suspend fun delete(entity: CommandHistoryEntry) = withContext(Dispatchers.IO) {
        dao.delete(entity)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    suspend fun getById(id: Long): CommandHistoryEntry? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun getAll(): List<CommandHistoryEntry> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun count(): Long = withContext(Dispatchers.IO) {
        dao.count()
    }

    suspend fun query(queryBuilder: () -> List<CommandHistoryEntry>): List<CommandHistoryEntry> = withContext(Dispatchers.IO) {
        queryBuilder()
    }
    
    suspend fun getRecentCommands(limit: Int = 50): List<CommandHistoryEntry> = withContext(Dispatchers.IO) {
        dao.getRecentCommands(limit)
    }
    
    suspend fun getMostUsedCommands(limit: Int = 50): List<CommandHistoryEntry> = withContext(Dispatchers.IO) {
        dao.getMostUsedCommands(limit)
    }
    
    suspend fun cleanupOldEntries(retainCount: Int, maxDays: Int) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (maxDays * 24 * 60 * 60 * 1000L)
        dao.cleanupOldEntries(cutoffTime, retainCount)
    }
    
    suspend fun getCommandsByLanguage(language: String): List<CommandHistoryEntry> = withContext(Dispatchers.IO) {
        dao.getCommandsByLanguage(language)
    }
    
    suspend fun getSuccessRate(): Float = withContext(Dispatchers.IO) {
        dao.getSuccessRate()
    }
}