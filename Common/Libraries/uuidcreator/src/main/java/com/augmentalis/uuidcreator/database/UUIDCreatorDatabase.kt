/**
 * UUIDCreatorDatabase.kt - Room database for UUIDCreator
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/UUIDCreatorDatabase.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Room database definition for UUIDCreator persistent storage
 */

package com.augmentalis.uuidcreator.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.augmentalis.uuidcreator.database.converters.UUIDCreatorTypeConverters
import com.augmentalis.uuidcreator.database.dao.UUIDAliasDao
import com.augmentalis.uuidcreator.database.dao.UUIDAnalyticsDao
import com.augmentalis.uuidcreator.database.dao.UUIDElementDao
import com.augmentalis.uuidcreator.database.dao.UUIDHierarchyDao
import com.augmentalis.uuidcreator.database.entities.UUIDAliasEntity
import com.augmentalis.uuidcreator.database.entities.UUIDAnalyticsEntity
import com.augmentalis.uuidcreator.database.entities.UUIDElementEntity
import com.augmentalis.uuidcreator.database.entities.UUIDHierarchyEntity

/**
 * Room database for UUIDCreator
 *
 * Provides persistent storage for UUID elements, hierarchy relationships,
 * and usage analytics. Works in hybrid mode with in-memory caching.
 *
 * @property uuidElementDao DAO for UUID element operations
 * @property uuidHierarchyDao DAO for hierarchy operations
 * @property uuidAnalyticsDao DAO for analytics operations
 * @property uuidAliasDao DAO for alias operations
 */
@Database(
    entities = [
        UUIDElementEntity::class,
        UUIDHierarchyEntity::class,
        UUIDAnalyticsEntity::class,
        UUIDAliasEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(UUIDCreatorTypeConverters::class)
abstract class UUIDCreatorDatabase : RoomDatabase() {

    /**
     * DAO for UUID element operations
     */
    abstract fun uuidElementDao(): UUIDElementDao

    /**
     * DAO for hierarchy operations
     */
    abstract fun uuidHierarchyDao(): UUIDHierarchyDao

    /**
     * DAO for analytics operations
     */
    abstract fun uuidAnalyticsDao(): UUIDAnalyticsDao

    /**
     * DAO for alias operations
     */
    abstract fun uuidAliasDao(): UUIDAliasDao

    companion object {
        /**
         * Database name
         */
        private const val DATABASE_NAME = "uuid_creator_database"

        /**
         * Singleton instance
         */
        @Volatile
        private var INSTANCE: UUIDCreatorDatabase? = null

        /**
         * Get database instance (singleton)
         *
         * @param context Application context
         * @return Database instance
         */
        fun getInstance(context: Context): UUIDCreatorDatabase {
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
        private fun buildDatabase(context: Context): UUIDCreatorDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                UUIDCreatorDatabase::class.java,
                DATABASE_NAME
            )
            // MIGRATION REQUIRED: Currently version 2, implement migrations before v3
            .build()
        }

        /**
         * Clear database instance (for testing)
         */
        @androidx.annotation.VisibleForTesting
        fun clearInstance() {
            INSTANCE = null
        }
    }
}
