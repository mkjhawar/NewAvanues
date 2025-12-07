/**
 * PreferencesDao.kt - Room DAO for user preferences
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-06
 */
package com.augmentalis.localizationmanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferencesDao {
    
    @Query("SELECT value FROM user_preferences WHERE key = :key")
    suspend fun getPreference(key: String): String?
    
    @Query("SELECT * FROM user_preferences WHERE key = :key")
    suspend fun getPreferenceWithTimestamp(key: String): UserPreference?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreference(preference: UserPreference)
    
    @Query("SELECT * FROM user_preferences")
    fun getAllPreferences(): Flow<List<UserPreference>>
    
    @Query("SELECT * FROM user_preferences WHERE key LIKE :keyPattern")
    fun getPreferencesByPattern(keyPattern: String): Flow<List<UserPreference>>
    
    @Query("DELETE FROM user_preferences WHERE key = :key")
    suspend fun deletePreference(key: String)
    
    @Query("DELETE FROM user_preferences")
    suspend fun clearAllPreferences()
    
    // Specific methods for common preferences
    @Query("SELECT value FROM user_preferences WHERE key = 'message_debounce_duration'")
    fun getDebounceDuration(): Flow<String?>
    
    @Query("SELECT value FROM user_preferences WHERE key = 'statistics_auto_show'")
    fun getStatisticsAutoShow(): Flow<String?>
    
    @Query("SELECT value FROM user_preferences WHERE key = 'language_animation_enabled'")
    fun getLanguageAnimationEnabled(): Flow<String?>
}