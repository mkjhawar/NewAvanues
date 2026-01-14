package com.augmentalis.browseravanue.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.augmentalis.browseravanue.data.local.dao.BrowserSettingsDao
import com.augmentalis.browseravanue.data.local.dao.BrowserFavoriteDao
import com.augmentalis.browseravanue.data.local.dao.BrowserTabDao
import com.augmentalis.browseravanue.data.local.entity.BrowserSettingsEntity
import com.augmentalis.browseravanue.data.local.entity.FavoriteEntity
import com.augmentalis.browseravanue.data.local.entity.TabEntity

/**
 * Shared Room database for BrowserAvanue
 *
 * Architecture Modes:
 * 1. **Shared Mode (Production):** Part of Avanues ecosystem, all apps share data
 * 2. **Standalone Mode (Testing/Export):** Independent browser database
 *
 * This database is designed to work in BOTH modes:
 * - Production: Integrated with Avanues shared database
 * - Testing/Standalone: Can be exported and used independently
 *
 * Benefits of Shared Approach:
 * - Cross-app features (Browser favorites linked to Notepad notes)
 * - Voice commands can query across all app data
 * - File Manager can access browser downloads metadata
 * - Single source of truth for Avanues ecosystem
 *
 * Export Capability:
 * - Database can be exported for standalone browser app
 * - Schema is self-contained (no external dependencies)
 * - Easy to package separately for testing or distribution
 *
 * @property tabDao DAO for browser tab operations
 * @property favoriteDao DAO for browser favorite operations
 * @property browserSettingsDao DAO for browser settings operations
 */
@Database(
    entities = [
        TabEntity::class,
        FavoriteEntity::class,
        BrowserSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class BrowserAvanueDatabase : RoomDatabase() {

    /**
     * DAO for browser tab operations
     */
    abstract fun browserTabDao(): BrowserTabDao

    /**
     * DAO for browser favorite/bookmark operations
     */
    abstract fun browserFavoriteDao(): BrowserFavoriteDao

    /**
     * DAO for browser settings operations
     */
    abstract fun browserSettingsDao(): BrowserSettingsDao

    companion object {
        /**
         * Database name
         */
        private const val DATABASE_NAME = "browser_avanue_database"

        /**
         * Singleton instance
         */
        @Volatile
        private var INSTANCE: BrowserAvanueDatabase? = null

        /**
         * Get database instance (singleton)
         *
         * @param context Application context
         * @return Database instance
         */
        fun getInstance(context: Context): BrowserAvanueDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Build database instance
         *
         * @param context Application context
         * @return Database instance
         */
        private fun buildDatabase(context: Context): BrowserAvanueDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                BrowserAvanueDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // TODO: Replace with proper migrations before v2
            .build()
        }

        /**
         * Clear database instance (for testing)
         */
        @androidx.annotation.VisibleForTesting
        fun clearInstance() {
            INSTANCE = null
        }

        /**
         * Export database for standalone use
         *
         * This method can be used to create a standalone version
         * of the browser database for testing or distribution.
         */
        @androidx.annotation.VisibleForTesting
        fun getStandaloneName(): String = DATABASE_NAME
    }
}
