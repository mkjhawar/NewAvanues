/**
 * CommandDatabase.kt - SQLDelight database for voice commands with locale fallback
 *
 * Purpose: Store voice commands with English fallback support
 * Features:
 * - Multiple locales (en-US, es-ES, fr-FR, de-DE)
 * - Automatic English fallback
 * - Fast command resolution
 *
 * Migrated from Room to SQLDelight for KMP compatibility.
 * Uses the core:database VoiceOSDatabase for persistence.
 */

package com.augmentalis.voiceoscore.managers.commandmanager.database

import android.content.Context
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.CommandUsageDaoAdapter
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.DatabaseVersionDaoAdapter
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.VoiceCommandDaoAdapter
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabase

/**
 * SQLDelight database wrapper for voice commands.
 * Provides adapter DAOs that match the previous Room DAO interfaces.
 *
 * Migration history:
 * - v1-v3: Room implementation
 * - v4: Migrated to SQLDelight for KMP compatibility
 */
class CommandDatabase private constructor(
    private val database: VoiceOSDatabase
) {

    private val _voiceCommandDao: VoiceCommandDaoAdapter by lazy {
        VoiceCommandDaoAdapter(database)
    }

    private val _databaseVersionDao: DatabaseVersionDaoAdapter by lazy {
        DatabaseVersionDaoAdapter(database)
    }

    private val _commandUsageDao: CommandUsageDaoAdapter by lazy {
        CommandUsageDaoAdapter(database)
    }

    /**
     * Data access object for voice commands.
     */
    fun voiceCommandDao(): VoiceCommandDaoAdapter = _voiceCommandDao

    /**
     * Data access object for database version tracking.
     */
    fun databaseVersionDao(): DatabaseVersionDaoAdapter = _databaseVersionDao

    /**
     * Data access object for usage analytics.
     */
    fun commandUsageDao(): CommandUsageDaoAdapter = _commandUsageDao

    companion object {
        @Volatile
        private var INSTANCE: CommandDatabase? = null

        @Volatile
        private var sqlDelightDatabase: VoiceOSDatabase? = null

        /**
         * Get singleton instance of CommandDatabase.
         * Thread-safe using double-checked locking.
         */
        fun getInstance(context: Context): CommandDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Build database instance using SQLDelight.
         */
        private fun buildDatabase(context: Context): CommandDatabase {
            val db = sqlDelightDatabase ?: run {
                val driver = DatabaseDriverFactory(context.applicationContext).createDriver()
                VoiceOSDatabase(driver).also { sqlDelightDatabase = it }
            }
            return CommandDatabase(db)
        }

        /**
         * Clear singleton instance (for testing).
         */
        @androidx.annotation.VisibleForTesting
        fun clearInstance() {
            sqlDelightDatabase = null
            INSTANCE = null
        }
    }
}
