/**
 * PreferencesRepository.kt - Repository for user preferences
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-06
 * Updated: 2025-12-01 - Migrated to use SQLDelight adapter
 */
package com.augmentalis.localizationmanager.repository

import com.augmentalis.localizationmanager.data.*
import com.augmentalis.localizationmanager.data.sqldelight.PreferencesDaoAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Direct implementation pattern - no dependency injection
 */
class PreferencesRepository(
    private val preferencesDao: PreferencesDaoAdapter
) {
    
    /**
     * Get message debounce duration in milliseconds
     */
    fun getDebounceDuration(): Flow<Long> {
        return preferencesDao.getDebounceDuration().map { value ->
            value?.toLongOrNull() ?: PreferenceDefaults.MESSAGE_DEBOUNCE_DURATION
        }
    }
    
    /**
     * Save message debounce duration
     */
    suspend fun saveDebounceDuration(duration: Long) {
        preferencesDao.savePreference(
            UserPreference(
                key = PreferenceKeys.MESSAGE_DEBOUNCE_DURATION,
                value = duration.toString()
            )
        )
    }
    
    /**
     * Get statistics auto-show preference
     */
    fun getStatisticsAutoShow(): Flow<Boolean> {
        return preferencesDao.getStatisticsAutoShow().map { value ->
            value?.toBooleanStrictOrNull() ?: PreferenceDefaults.STATISTICS_AUTO_SHOW
        }
    }
    
    /**
     * Save statistics auto-show preference
     */
    suspend fun saveStatisticsAutoShow(autoShow: Boolean) {
        preferencesDao.savePreference(
            UserPreference(
                key = PreferenceKeys.STATISTICS_AUTO_SHOW,
                value = autoShow.toString()
            )
        )
    }
    
    /**
     * Get language animation enabled preference
     */
    fun getLanguageAnimationEnabled(): Flow<Boolean> {
        return preferencesDao.getLanguageAnimationEnabled().map { value ->
            value?.toBooleanStrictOrNull() ?: PreferenceDefaults.LANGUAGE_ANIMATION_ENABLED
        }
    }
    
    /**
     * Save language animation enabled preference
     */
    suspend fun saveLanguageAnimationEnabled(enabled: Boolean) {
        preferencesDao.savePreference(
            UserPreference(
                key = PreferenceKeys.LANGUAGE_ANIMATION_ENABLED,
                value = enabled.toString()
            )
        )
    }
    
    /**
     * Get preferred detail level
     */
    fun getPreferredDetailLevel(): Flow<DetailLevel> {
        return kotlinx.coroutines.flow.flow {
            val value = preferencesDao.getPreference(PreferenceKeys.PREFERRED_DETAIL_LEVEL)
            val detailLevel = try {
                DetailLevel.valueOf(value ?: PreferenceDefaults.PREFERRED_DETAIL_LEVEL)
            } catch (e: IllegalArgumentException) {
                DetailLevel.valueOf(PreferenceDefaults.PREFERRED_DETAIL_LEVEL)
            }
            emit(detailLevel)
        }
    }
    
    /**
     * Save preferred detail level
     */
    suspend fun savePreferredDetailLevel(level: DetailLevel) {
        preferencesDao.savePreference(
            UserPreference(
                key = PreferenceKeys.PREFERRED_DETAIL_LEVEL,
                value = level.name
            )
        )
    }
    
    /**
     * Get all preferences as a flow
     */
    fun getAllPreferences(): Flow<List<UserPreference>> {
        return preferencesDao.getAllPreferences()
    }
    
    /**
     * Clear all preferences (for reset functionality)
     */
    suspend fun clearAllPreferences() {
        preferencesDao.clearAllPreferences()
    }
}