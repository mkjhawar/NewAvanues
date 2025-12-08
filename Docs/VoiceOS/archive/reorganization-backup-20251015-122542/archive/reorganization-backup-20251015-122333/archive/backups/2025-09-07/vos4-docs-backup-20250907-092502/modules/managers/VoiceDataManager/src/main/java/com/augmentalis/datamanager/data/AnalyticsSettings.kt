// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.data

import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.AnalyticsSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pure data access for AnalyticsSettings
 * Single Responsibility: CRUD operations only, no business logic
 */
class AnalyticsSettingsRepo {
    
    private val dao get() = DatabaseManager.database.analyticsSettingsDao()
    
    // Direct CRUD implementation - no inheritance
    suspend fun insert(entity: AnalyticsSettings): Long = withContext(Dispatchers.IO) {
        dao.insert(entity.copy(id = 1)) // Force single record
    }
    
    suspend fun insertAll(entities: List<AnalyticsSettings>): List<Long> = withContext(Dispatchers.IO) {
        // For settings, only keep the first one as single record
        if (entities.isNotEmpty()) {
            listOf(dao.insert(entities.first().copy(id = 1)))
        } else {
            emptyList()
        }
    }
    
    suspend fun update(entity: AnalyticsSettings) = withContext(Dispatchers.IO) {
        dao.update(entity.copy(id = 1)) // Force single record
    }
    
    suspend fun delete(entity: AnalyticsSettings) = withContext(Dispatchers.IO) {
        dao.delete(entity)
    }
    
    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }
    
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }
    
    suspend fun getById(id: Long): AnalyticsSettings? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }
    
    suspend fun getAll(): List<AnalyticsSettings> = withContext(Dispatchers.IO) {
        dao.getAll()
    }
    
    suspend fun count(): Long = withContext(Dispatchers.IO) {
        dao.count()
    }
    
    suspend fun query(queryBuilder: () -> List<AnalyticsSettings>): List<AnalyticsSettings> = withContext(Dispatchers.IO) {
        queryBuilder()
    }
    
    // Single specialized method for settings (always ID = 1)
    suspend fun getSettings(): AnalyticsSettings? = withContext(Dispatchers.IO) {
        dao.getById(1)
    }
    
    suspend fun updateSettings(settings: AnalyticsSettings) = withContext(Dispatchers.IO) {
        dao.update(settings.copy(id = 1))
    }
    
    suspend fun initializeDefaults() = withContext(Dispatchers.IO) {
        if (getSettings() == null) {
            dao.insert(AnalyticsSettings(
                id = 1,
                sendAnonymousReports = false,
                trackPerformance = false,
                autoEnableOnErrors = false,
                userConsent = false
            ))
        }
    }
}