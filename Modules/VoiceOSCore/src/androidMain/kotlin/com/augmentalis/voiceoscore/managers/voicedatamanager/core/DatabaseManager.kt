/**
 * SQLDelight Database Manager - VoiceOS Data Persistence Layer
 *
 * Features:
 * - Thread-safe singleton database access
 * - Coroutine-based async operations via repository interfaces
 * - Database size monitoring
 * - Complete data clearing for factory reset
 * - KMP-compatible SQLDelight implementation
 *
 * Usage:
 * - Initialize once in Application.onCreate(): DatabaseManager.init(context)
 * - Access repositories: DatabaseManager.commands, DatabaseManager.commandHistory, etc.
 * - Check size: DatabaseManager.getDatabaseSizeMB()
 * - Clear data: DatabaseManager.clearAllData()
 *
 * Author: Manoj Jhawar
 * Updated: 2025-01-28 - Migrated from Room to SQLDelight KMP
 */
package com.augmentalis.voiceoscore.managers.voicedatamanager.core

import android.content.Context
import android.util.Log
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.repositories.ICommandRepository
import com.augmentalis.database.repositories.ICommandHistoryRepository
import com.augmentalis.database.repositories.IUserPreferenceRepository
import com.augmentalis.database.repositories.IErrorReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DatabaseManager {
    private const val TAG = "VosDatabaseManager"

    private lateinit var databaseManager: VoiceOSDatabaseManager
    private lateinit var applicationContext: Context
    private var isInitialized = false

    // Direct database manager access (for getStats, etc.)
    val dbManager: VoiceOSDatabaseManager get() = databaseManager

    // Repository access (high-level abstraction)
    val commands: ICommandRepository get() = databaseManager.commands
    val commandHistory: ICommandHistoryRepository get() = databaseManager.commandHistory
    val userPreferences: IUserPreferenceRepository get() = databaseManager.userPreferences
    val errorReports: IErrorReportRepository get() = databaseManager.errorReports

    // Direct query access (for tables without repository abstraction)
    val deviceProfileQueries get() = databaseManager.deviceProfileQueries
    val touchGestureQueries get() = databaseManager.touchGestureQueries
    val gestureLearningQueries get() = databaseManager.gestureLearningQueries
    val languageModelQueries get() = databaseManager.languageModelQueries
    val usageStatisticQueries get() = databaseManager.usageStatisticQueries
    val recognitionLearningQueries get() = databaseManager.recognitionLearningQueries
    val settingsQueries get() = databaseManager.settingsQueries
    val scrapedAppQueries get() = databaseManager.scrapedAppQueries
    val generatedCommandQueries get() = databaseManager.generatedCommandQueries
    val userInteractionQueries get() = databaseManager.userInteractionQueries
    val elementStateHistoryQueries get() = databaseManager.elementStateHistoryQueries
    val scrappedCommandQueries get() = databaseManager.scrappedCommandQueries
    val scrapedElementQueries get() = databaseManager.scrapedElementQueries
    val screenContextQueries get() = databaseManager.screenContextQueries
    val screenTransitionQueries get() = databaseManager.screenTransitionQueries
    val userSequenceQueries get() = databaseManager.userSequenceQueries
    // Analytics and Retention settings are accessed via settingsQueries
    // Use: settingsQueries.getAnalyticsSettings() and settingsQueries.getRetentionSettings()

    /**
     * Check if database is initialized
     */
    fun isInitialized(): Boolean = isInitialized && ::databaseManager.isInitialized

    /**
     * Initialize SQLDelight database
     * @param context Application context for database initialization
     * @return true if initialization successful, false otherwise
     */
    fun init(context: Context): Boolean {
        if (isInitialized) {
            Log.d(TAG, "Database already initialized")
            return true
        }

        return try {
            applicationContext = context.applicationContext
            val driverFactory = DatabaseDriverFactory(context)
            databaseManager = VoiceOSDatabaseManager.getInstance(driverFactory)

            isInitialized = true
            Log.i(TAG, "SQLDelight database initialized successfully")
            Log.d(TAG, "Initial database size: ${getDatabaseSizeMB()} MB")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SQLDelight database", e)
            false
        }
    }

    /**
     * Close the database
     * Should be called in Application.onTerminate() or during cleanup
     */
    fun close() {
        if (isInitialized && ::databaseManager.isInitialized) {
            // SQLDelight database is managed by the driver; no explicit close needed
            isInitialized = false
            Log.i(TAG, "Database closed successfully")
        }
    }

    /**
     * Get the current database size in megabytes
     * @return Database size in MB, or 0 if not initialized
     */
    fun getDatabaseSizeMB(): Float {
        return if (isInitialized && ::applicationContext.isInitialized) {
            try {
                val dbPath = getDatabasePath()
                if (dbPath != null) {
                    val dbFile = File(dbPath)
                    if (dbFile.exists()) {
                        val sizeInBytes = dbFile.length()
                        // Also include journal and wal files
                        val walFile = File("$dbPath-wal")
                        val shmFile = File("$dbPath-shm")
                        val totalSize = sizeInBytes +
                            (if (walFile.exists()) walFile.length() else 0) +
                            (if (shmFile.exists()) shmFile.length() else 0)

                        val sizeInMB = totalSize / (1024f * 1024f)
                        Log.v(TAG, "Database size: $sizeInMB MB ($totalSize bytes)")
                        sizeInMB
                    } else {
                        Log.w(TAG, "Database file does not exist")
                        0f
                    }
                } else {
                    Log.w(TAG, "Database path not available")
                    0f
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating database size", e)
                0f
            }
        } else {
            Log.w(TAG, "Cannot get database size - Database not initialized")
            0f
        }
    }

    /**
     * Get the database file path
     * @return Absolute path to database file, or null if not initialized
     */
    fun getDatabasePath(): String? {
        return if (::applicationContext.isInitialized) {
            applicationContext.getDatabasePath("voiceos.db").absolutePath
        } else {
            null
        }
    }

    /**
     * Clear all data from all tables
     * WARNING: This is irreversible and will delete all stored data
     * Use for factory reset or complete data wipe scenarios
     */
    suspend fun clearAllData() {
        if (isInitialized && ::databaseManager.isInitialized) {
            withContext(Dispatchers.IO) {
                try {
                    val sizeBefore = getDatabaseSizeMB()

                    // Clear all tables via repositories
                    commands.deleteAll()
                    commandHistory.deleteAll()
                    userPreferences.deleteAll()
                    errorReports.deleteAll()

                    val sizeAfter = getDatabaseSizeMB()
                    Log.w(TAG, "All database data cleared. Size reduced from $sizeBefore MB to $sizeAfter MB")
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing all data", e)
                }
            }
        } else {
            Log.w(TAG, "Cannot clear data - Database not initialized")
        }
    }

    /**
     * Get database statistics for monitoring
     * @return Map of statistics or empty map if not initialized
     */
    fun getStatistics(): Map<String, Any> {
        return if (isInitialized && ::databaseManager.isInitialized) {
            mapOf(
                "initialized" to true,
                "database_path" to (getDatabasePath() ?: "unknown"),
                "size_mb" to getDatabaseSizeMB(),
                "backend" to "SQLDelight"
            )
        } else {
            mapOf("initialized" to false)
        }
    }
}