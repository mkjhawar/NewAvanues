// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.RetentionSettings

@Dao
interface RetentionSettingsDao : BaseDao<RetentionSettings> {
    
    @Query("SELECT * FROM retention_settings LIMIT 1")
    suspend fun get(): RetentionSettings?
    
    @Query("SELECT * FROM retention_settings")
    suspend fun getAll(): List<RetentionSettings>
    
    @Query("SELECT * FROM retention_settings WHERE id = :id")
    suspend fun getById(id: Long): RetentionSettings?
    
    @Query("DELETE FROM retention_settings WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM retention_settings")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM retention_settings")
    suspend fun count(): Long
    
    @Query("UPDATE retention_settings SET commandHistoryRetainCount = :count WHERE id = 1")
    suspend fun updateCommandHistoryRetainCount(count: Int)
    
    @Query("UPDATE retention_settings SET commandHistoryMaxDays = :days WHERE id = 1")
    suspend fun updateCommandHistoryMaxDays(days: Int)
    
    @Query("UPDATE retention_settings SET statisticsRetentionDays = :days WHERE id = 1")
    suspend fun updateStatisticsRetentionDays(days: Int)
    
    @Query("UPDATE retention_settings SET enableAutoCleanup = :enabled WHERE id = 1")
    suspend fun setAutoCleanupEnabled(enabled: Boolean)
    
    @Query("UPDATE retention_settings SET notifyBeforeCleanup = :notify WHERE id = 1")
    suspend fun setNotifyBeforeCleanup(notify: Boolean)
    
    @Query("UPDATE retention_settings SET maxDatabaseSizeMB = :sizeMB WHERE id = 1")
    suspend fun updateMaxDatabaseSize(sizeMB: Int)
}