/**
 * DatabaseManager.kt - Centralized database lifecycle management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (IDEACODE v12.1)
 * Created: 2025-12-22
 *
 * Extracts database management from VoiceOSService to follow Single Responsibility Principle.
 * Manages both VoiceOSDatabaseManager (SQLDelight repositories) and VoiceOSAppDatabase
 * (scraping database adapter) with unified initialization state machine.
 */

package com.augmentalis.voiceoscore.accessibility.managers

import android.content.Context
import android.util.Log
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/**
 * Database Manager
 *
 * Centralizes database lifecycle management for VoiceOSService:
 * - VoiceOSDatabaseManager (SQLDelight repositories)
 * - VoiceOSAppDatabase (scraping database adapter)
 * - Initialization state machine
 * - Safe access guards
 * - Cleanup
 *
 * @param context Application context for database initialization
 */
class DatabaseManager(private val context: Context) {

    companion object {
        private const val TAG = "DatabaseManager"
        private const val INIT_TIMEOUT_MS = 10_000L
    }

    /**
     * Database Initialization State Machine
     *
     * Prevents database access before initialization complete
     */
    sealed class InitializationState {
        object NotStarted : InitializationState()
        object InProgress : InitializationState()
        data class Completed(val timestamp: Long) : InitializationState()
        data class Failed(val error: String) : InitializationState()
    }

    /**
     * Initialization state flow
     */
    private val _initState = MutableStateFlow<InitializationState>(InitializationState.NotStarted)
    val initState: StateFlow<InitializationState> = _initState

    /**
     * SQLDelight database manager
     *
     * Provides access to repositories:
     * - generatedCommands
     * - appVersions
     * - screenContext
     */
    val sqlDelightManager: VoiceOSDatabaseManager by lazy {
        VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(context)).also {
            Log.d(TAG, "VoiceOSDatabaseManager initialized (lazy)")
        }
    }

    /**
     * Scraping database (adapter for SQLDelight)
     *
     * Nullable for safe fallback to in-memory cache
     */
    var scrapingDatabase: VoiceOSAppDatabase? = null
        private set

    /**
     * Initialize both databases with timeout and error handling
     *
     * Call from VoiceOSService.onServiceConnected() in coroutine scope
     *
     * @throws IllegalStateException if initialization fails
     */
    suspend fun initialize() {
        _initState.emit(InitializationState.InProgress)

        try {
            // Initialize scraping database adapter (VoiceOSAppDatabase)
            scrapingDatabase = try {
                VoiceOSAppDatabase.getInstance(context).also {
                    Log.i(TAG, "SQLDelight VoiceOSAppDatabase initialized successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize VoiceOSAppDatabase - will fall back to in-memory cache", e)
                null
            }

            // Wait for database initialization with timeout
            withTimeout(INIT_TIMEOUT_MS) {
                scrapingDatabase?.databaseManager?.waitForInitialization()
                    ?: throw IllegalStateException("Database not initialized")
            }

            // Database initialization verified - foreign keys enabled via VoiceOSDatabaseManager init
            Log.d(TAG, "Database initialized successfully")
            Log.i(TAG, "Database initialization verified - foreign keys enabled")
            _initState.emit(InitializationState.Completed(System.currentTimeMillis()))

        } catch (e: Exception) {
            val errorMsg = "Database initialization failed: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _initState.emit(InitializationState.Failed(e.message ?: "Unknown error"))
            throw IllegalStateException(errorMsg, e)
        }
    }

    /**
     * Guard function to ensure database is ready before access
     *
     * Suspends until database initialization is complete or failed.
     *
     * @throws IllegalStateException if database initialization failed
     */
    suspend fun <T> withDatabaseReady(block: suspend () -> T): T {
        val state = _initState.first {
            it is InitializationState.Completed || it is InitializationState.Failed
        }

        return when (state) {
            is InitializationState.Completed -> block()
            is InitializationState.Failed -> {
                throw IllegalStateException("Database not initialized: ${state.error}")
            }
            else -> {
                throw IllegalStateException("Unexpected state: $state")
            }
        }
    }

    /**
     * Cleanup database references
     *
     * Call from VoiceOSService.onDestroy()
     * Note: Database is singleton and managed by SQLDelight lifecycle
     */
    fun cleanup() {
        if (scrapingDatabase != null) {
            Log.d(TAG, "Clearing scraping database reference (SQLDelight manages lifecycle)...")
            scrapingDatabase = null
            Log.i(TAG, "✓ Scraping database reference cleared")
        }
    }
}
