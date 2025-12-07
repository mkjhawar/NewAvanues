// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.AnalyticsSettings

@Dao
interface AnalyticsSettingsDao : BaseDao<AnalyticsSettings> {
    
    @Query("SELECT * FROM analytics_settings LIMIT 1")
    suspend fun get(): AnalyticsSettings?
    
    @Query("SELECT * FROM analytics_settings")
    suspend fun getAll(): List<AnalyticsSettings>
    
    @Query("SELECT * FROM analytics_settings WHERE id = :id")
    suspend fun getById(id: Long): AnalyticsSettings?
    
    @Query("DELETE FROM analytics_settings WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM analytics_settings")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM analytics_settings")
    suspend fun count(): Long
    
    @Query("UPDATE analytics_settings SET trackPerformance = :enabled WHERE id = 1")
    suspend fun setTrackingEnabled(enabled: Boolean)
    
    @Query("UPDATE analytics_settings SET userConsent = :consent, consentDate = :date WHERE id = 1")
    suspend fun updateConsent(consent: Boolean, date: Long?)
}