// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.data

import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.RetentionSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RetentionSettingsRepo {
    
    private val dao get() = DatabaseManager.database.retentionSettingsDao()
    
    // Direct CRUD implementation - no inheritance
    suspend fun insert(entity: RetentionSettings): Long = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }

    suspend fun insertAll(entities: List<RetentionSettings>): List<Long> = withContext(Dispatchers.IO) {
        dao.insertAll(entities)
    }

    suspend fun update(entity: RetentionSettings) = withContext(Dispatchers.IO) {
        dao.update(entity)
    }

    suspend fun delete(entity: RetentionSettings) = withContext(Dispatchers.IO) {
        dao.delete(entity)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    suspend fun getById(id: Long): RetentionSettings? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun getAll(): List<RetentionSettings> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun count(): Long = withContext(Dispatchers.IO) {
        dao.count()
    }

    suspend fun query(queryBuilder: () -> List<RetentionSettings>): List<RetentionSettings> = withContext(Dispatchers.IO) {
        queryBuilder()
    }
    
    suspend fun getSettings(): RetentionSettings? = withContext(Dispatchers.IO) {
        dao.getById(1) // Single record with ID 1
    }
    
    suspend fun updateSettings(settings: RetentionSettings) = withContext(Dispatchers.IO) {
        dao.update(settings.copy(id = 1))
    }
    
    suspend fun initializeDefaults() = withContext(Dispatchers.IO) {
        if (dao.getById(1) == null) {
            dao.insert(RetentionSettings(id = 1))
        }
    }
    
    suspend fun updateRetainCount(count: Int) = withContext(Dispatchers.IO) {
        val current = getSettings() ?: RetentionSettings(id = 1)
        val updated = current.copy(
            commandHistoryRetainCount = count.coerceIn(25, 200)
        )
        dao.update(updated)
    }
    
    suspend fun toggleAutoCleanup() = withContext(Dispatchers.IO) {
        val current = getSettings() ?: RetentionSettings(id = 1)
        val updated = current.copy(enableAutoCleanup = !current.enableAutoCleanup)
        dao.update(updated)
    }
    
    suspend fun setMaxDatabaseSize(sizeMB: Int) = withContext(Dispatchers.IO) {
        val current = getSettings() ?: RetentionSettings(id = 1)
        val updated = current.copy(maxDatabaseSizeMB = sizeMB.coerceIn(50, 500))
        dao.update(updated)
    }
}