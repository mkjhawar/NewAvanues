/**
 * Room Database Manager - VoiceOS Data Persistence Layer
 * 
 * Features:
 * - Thread-safe singleton database access
 * - Coroutine-based async operations
 * - Database size monitoring
 * - Complete data clearing for factory reset
 * - Transaction support
 * - Migration handling
 * 
 * Usage:
 * - Initialize once in Application.onCreate(): DatabaseManager.init(context)
 * - Access database: DatabaseManager.database
 * - Access DAOs: DatabaseManager.database.customCommandDao()
 * - Check size: DatabaseManager.getDatabaseSizeMB()
 * - Clear data: DatabaseManager.clearAllData()
 * 
 * Author: Manoj Jhawar
 * Updated: 2025-01-28 - Migrated from ObjectBox to Room
 */
package com.augmentalis.datamanager.core

import android.content.Context
import android.util.Log
import com.augmentalis.datamanager.database.VoiceOSDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DatabaseManager {
    private const val TAG = "VosDatabaseManager"
    
    lateinit var database: VoiceOSDatabase
        private set
    
    private lateinit var applicationContext: Context
    private var isInitialized = false
    
    /**
     * Check if database is initialized
     */
    fun isInitialized(): Boolean = isInitialized && ::database.isInitialized
    
    /**
     * Initialize Room database
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
            database = VoiceOSDatabase.getInstance(context)
            
            isInitialized = true
            Log.i(TAG, "Room database initialized successfully")
            Log.d(TAG, "Initial database size: ${getDatabaseSizeMB()} MB")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Room database", e)
            false
        }
    }
    
    /**
     * Close the database
     * Should be called in Application.onTerminate() or during cleanup
     */
    fun close() {
        if (isInitialized && ::database.isInitialized) {
            database.close()
            VoiceOSDatabase.clearInstance()
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
            applicationContext.getDatabasePath("voiceos_database.db").absolutePath
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
        if (isInitialized && ::database.isInitialized) {
            withContext(Dispatchers.IO) {
                try {
                    val sizeBefore = getDatabaseSizeMB()
                    
                    // Clear all tables
                    database.clearAllTables()
                    
                    // Re-initialize default settings
                    database.analyticsSettingsDao().insert(
                        com.augmentalis.datamanager.entities.AnalyticsSettings(id = 1)
                    )
                    database.retentionSettingsDao().insert(
                        com.augmentalis.datamanager.entities.RetentionSettings(id = 1)
                    )
                    
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
     * Run database operations in a transaction
     */
    suspend fun <R> withTransaction(block: suspend () -> R): R {
        return withContext(Dispatchers.IO) {
            var result: R? = null
            database.runInTransaction {
                result = kotlinx.coroutines.runBlocking {
                    block()
                }
            }
            @Suppress("UNCHECKED_CAST")
            result as R
        }
    }
    
    /**
     * Get database statistics for monitoring
     * @return Map of statistics or empty map if not initialized
     */
    fun getStatistics(): Map<String, Any> {
        return if (isInitialized && ::database.isInitialized) {
            mapOf(
                "initialized" to true,
                "database_path" to (getDatabasePath() ?: "unknown"),
                "size_mb" to getDatabaseSizeMB(),
                "version" to database.openHelper.readableDatabase.version,
                "is_open" to database.isOpen
            )
        } else {
            mapOf("initialized" to false)
        }
    }
}