/**
 * LocalizationDatabase.kt - SQLDelight database for LocalizationManager
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-06
 * Updated: 2025-12-01 - Migrated from Room to SQLDelight for KMP compatibility
 */
package com.augmentalis.localization.data

import android.content.Context
import com.augmentalis.localization.data.sqldelight.PreferencesDaoAdapter

/**
 * SQLDelight database wrapper for LocalizationManager
 *
 * Uses the centralized VoiceOSDatabase from libraries:core:database
 * with adapter classes for backward compatibility.
 */
object LocalizationDatabase {
    @Volatile
    private var preferencesDao: PreferencesDaoAdapter? = null

    /**
     * Get the PreferencesDao adapter instance
     */
    fun getPreferencesDao(context: Context): PreferencesDaoAdapter {
        return preferencesDao ?: synchronized(this) {
            preferencesDao ?: PreferencesDaoAdapter.create(context).also {
                preferencesDao = it
            }
        }
    }

    /**
     * Clear the cached instance (for testing)
     */
    fun clearInstance() {
        preferencesDao = null
    }
}