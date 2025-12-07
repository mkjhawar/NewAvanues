// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.data

import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.CustomCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CustomCommandRepo {
    
    private val dao get() = DatabaseManager.database.customCommandDao()
    
    // Direct CRUD implementation - no inheritance
    suspend fun insert(entity: CustomCommand): Long = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }

    suspend fun insertAll(entities: List<CustomCommand>): List<Long> = withContext(Dispatchers.IO) {
        dao.insertAll(entities)
    }

    suspend fun update(entity: CustomCommand) = withContext(Dispatchers.IO) {
        dao.update(entity)
    }

    suspend fun delete(entity: CustomCommand) = withContext(Dispatchers.IO) {
        dao.delete(entity)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.getById(id)?.let { dao.delete(it) }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    suspend fun getById(id: Long): CustomCommand? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun getAll(): List<CustomCommand> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun count(): Long = withContext(Dispatchers.IO) {
        dao.getAll().size.toLong()
    }
    
    suspend fun getActiveCommands(): List<CustomCommand> = withContext(Dispatchers.IO) {
        dao.getMostUsed(1000) // Get all active commands ordered by usage
    }
    
    suspend fun getCommandsByLanguage(language: String): List<CustomCommand> = withContext(Dispatchers.IO) {
        dao.getByLanguageAndActiveStatus(language, true)
    }
    
    suspend fun findCommandByPhrase(phrase: String): CustomCommand? = withContext(Dispatchers.IO) {
        dao.getAll().firstOrNull { command ->
            command.phrases.any { it.equals(phrase, ignoreCase = true) }
        }
    }
    
    suspend fun incrementUsageCount(commandId: Long) = withContext(Dispatchers.IO) {
        dao.incrementUsage(commandId, System.currentTimeMillis())
    }
    
    suspend fun getMostUsedCommands(limit: Int = 10): List<CustomCommand> = withContext(Dispatchers.IO) {
        dao.getMostUsed(limit)
    }
    
    suspend fun toggleCommandActive(commandId: Long) = withContext(Dispatchers.IO) {
        dao.getById(commandId)?.let { command ->
            dao.setActiveStatus(commandId, !command.isActive)
        }
    }
}