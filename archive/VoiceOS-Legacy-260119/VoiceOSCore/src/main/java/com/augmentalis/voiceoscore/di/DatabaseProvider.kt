/**
 * DatabaseProvider.kt - Manual dependency provider for database access
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Provides singleton database and repository instances without Hilt.
 * Note: Hilt is disabled in VoiceOSCore (doesn't support AccessibilityService).
 */

package com.augmentalis.voiceoscore.di

import android.content.Context
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.repositories.IGeneratedCommandRepository

/**
 * Manual dependency provider for database instances.
 *
 * This singleton object provides database access without Hilt DI,
 * ensuring single SQLite connection per application.
 */
object DatabaseProvider {

    /**
     * Get VoiceOSDatabase singleton instance.
     *
     * @param context Application or service context
     * @return Singleton VoiceOSDatabase instance
     */
    fun provideVoiceOSDatabase(context: Context): VoiceOSDatabase {
        val driverFactory = DatabaseDriverFactory(context.applicationContext)
        return VoiceOSDatabaseManager.getInstance(driverFactory).getDatabase()
    }

    /**
     * Get IGeneratedCommandRepository instance.
     *
     * @param context Application or service context
     * @return GeneratedCommand repository implementation
     */
    fun provideGeneratedCommandRepository(context: Context): IGeneratedCommandRepository {
        val driverFactory = DatabaseDriverFactory(context.applicationContext)
        return VoiceOSDatabaseManager.getInstance(driverFactory).generatedCommands
    }
}
