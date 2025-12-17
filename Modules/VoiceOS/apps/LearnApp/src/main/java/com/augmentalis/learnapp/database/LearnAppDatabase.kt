/**
 * LearnAppDatabase.kt - Room database for LearnApp
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/database/LearnAppDatabase.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Room database definition for LearnApp persistent storage
 */

package com.augmentalis.learnapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.augmentalis.learnapp.database.dao.LearnAppDao
import com.augmentalis.learnapp.database.entities.*

/**
 * LearnApp Database
 *
 * Room database for persistent storage of learned apps data.
 *
 * @property learnAppDao DAO for all LearnApp operations
 *
 * @since 1.0.0
 */
@Database(
    entities = [
        LearnedAppEntity::class,
        ExplorationSessionEntity::class,
        NavigationEdgeEntity::class,
        ScreenStateEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class LearnAppDatabase : RoomDatabase() {

    /**
     * DAO for LearnApp operations
     */
    abstract fun learnAppDao(): LearnAppDao

    companion object {
        /**
         * Database name
         */
        private const val DATABASE_NAME = "learnapp_database"

        /**
         * Singleton instance
         */
        @Volatile
        private var INSTANCE: LearnAppDatabase? = null

        /**
         * Get database instance (singleton)
         *
         * @param context Application context
         * @return Database instance
         */
        fun getInstance(context: Context): LearnAppDatabase {
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
        private fun buildDatabase(context: Context): LearnAppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                LearnAppDatabase::class.java,
                DATABASE_NAME
            )
            // MIGRATION REQUIRED: Before incrementing version, implement Migration objects
            // Example: .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            // See AppScrapingDatabase.kt for migration implementation examples
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
