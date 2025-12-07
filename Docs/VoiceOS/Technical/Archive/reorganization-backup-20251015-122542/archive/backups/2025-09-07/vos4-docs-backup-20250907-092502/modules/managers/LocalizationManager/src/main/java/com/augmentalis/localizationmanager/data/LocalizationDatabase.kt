/**
 * LocalizationDatabase.kt - Room database for LocalizationManager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-06
 */
package com.augmentalis.localizationmanager.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [UserPreference::class],
    version = 1,
    exportSchema = false
)
abstract class LocalizationDatabase : RoomDatabase() {
    
    abstract fun preferencesDao(): PreferencesDao
    
    companion object {
        @Volatile
        private var INSTANCE: LocalizationDatabase? = null
        
        fun getDatabase(context: Context): LocalizationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalizationDatabase::class.java,
                    "localization_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}