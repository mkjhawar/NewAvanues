/**
 * PreferencesDaoAdapter.kt - SQLDelight adapter for user preferences
 *
 * Provides Room-like DAO interface backed by SQLDelight VoiceOSDatabase.
 * Maps between LocalizationManager's UserPreference and SQLDelight's user_preference table.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-01
 * Migration: Room -> SQLDelight for KMP compatibility
 */
package com.augmentalis.localizationmanager.data.sqldelight

import android.content.Context
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.settings.UserPreferenceQueries
import com.augmentalis.database.settings.User_preference
import com.augmentalis.localizationmanager.data.UserPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * SQLDelight adapter that implements PreferencesDao-like interface
 * for LocalizationManager preferences storage
 */
class PreferencesDaoAdapter(
    private val queries: UserPreferenceQueries
) {
    companion object {
        private const val TYPE_LOCALIZATION = "LOCALIZATION"

        @Volatile
        private var database: VoiceOSDatabase? = null

        fun create(context: Context): PreferencesDaoAdapter {
            val db = database ?: synchronized(this) {
                database ?: run {
                    val driver = DatabaseDriverFactory(context.applicationContext).createDriver()
                    VoiceOSDatabase(driver).also { database = it }
                }
            }
            return PreferencesDaoAdapter(db.userPreferenceQueries)
        }
    }

    /**
     * Map SQLDelight row to domain model
     */
    private fun User_preference.toUserPreference(): UserPreference = UserPreference(
        key = this.key,
        value = this.value_,
        lastModified = this.updatedAt
    )

    /**
     * Get preference value by key
     */
    suspend fun getPreference(key: String): String? = withContext(Dispatchers.IO) {
        queries.getValue(key).executeAsOneOrNull()
    }

    /**
     * Get preference with full data including timestamp
     */
    suspend fun getPreferenceWithTimestamp(key: String): UserPreference? = withContext(Dispatchers.IO) {
        queries.getByKey(key).executeAsOneOrNull()?.toUserPreference()
    }

    /**
     * Save or update preference
     */
    suspend fun savePreference(preference: UserPreference) = withContext(Dispatchers.IO) {
        queries.insert(
            key = preference.key,
            value_ = preference.value,
            type = TYPE_LOCALIZATION,
            updatedAt = preference.lastModified
        )
    }

    /**
     * Get all preferences as Flow
     */
    fun getAllPreferences(): Flow<List<UserPreference>> = flow<List<UserPreference>> {
        val prefs = queries.getByType(TYPE_LOCALIZATION).executeAsList()
            .map { row -> row.toUserPreference() }
        emit(prefs)
    }.flowOn(Dispatchers.IO)

    /**
     * Get preferences matching pattern (e.g., "locale_%")
     */
    fun getPreferencesByPattern(keyPattern: String): Flow<List<UserPreference>> = flow<List<UserPreference>> {
        val prefs = queries.getByKeyPattern(keyPattern).executeAsList()
            .map { row -> row.toUserPreference() }
        emit(prefs)
    }.flowOn(Dispatchers.IO)

    /**
     * Delete preference by key
     */
    suspend fun deletePreference(key: String) = withContext(Dispatchers.IO) {
        queries.deleteByKey(key)
    }

    /**
     * Clear all localization preferences
     */
    suspend fun clearAllPreferences() = withContext(Dispatchers.IO) {
        queries.deleteByType(TYPE_LOCALIZATION)
    }

    /**
     * Get debounce duration setting
     */
    fun getDebounceDuration(): Flow<String?> = flow<String?> {
        emit(queries.getDebounceDuration().executeAsOneOrNull())
    }.flowOn(Dispatchers.IO)

    /**
     * Get statistics auto-show setting
     */
    fun getStatisticsAutoShow(): Flow<String?> = flow<String?> {
        emit(queries.getStatisticsAutoShow().executeAsOneOrNull())
    }.flowOn(Dispatchers.IO)

    /**
     * Get language animation enabled setting
     */
    fun getLanguageAnimationEnabled(): Flow<String?> = flow<String?> {
        emit(queries.getLanguageAnimationEnabled().executeAsOneOrNull())
    }.flowOn(Dispatchers.IO)
}
