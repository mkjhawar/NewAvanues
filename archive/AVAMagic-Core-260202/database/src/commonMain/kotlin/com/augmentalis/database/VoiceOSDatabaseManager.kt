/**
 * VoiceOSDatabaseManager.kt - Main database manager for VoiceOS
 *
 * Provides centralized access to all database operations.
 * Use this as the main entry point for database access.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.augmentalis.database.repositories.ICommandRepository
import com.augmentalis.database.repositories.ICommandHistoryRepository
import com.augmentalis.database.repositories.IUserPreferenceRepository
import com.augmentalis.database.repositories.IErrorReportRepository
import com.augmentalis.database.repositories.IAvidRepository
import com.augmentalis.database.repositories.IVoiceCommandRepository
import com.augmentalis.database.repositories.ICommandUsageRepository
import com.augmentalis.database.repositories.IContextPreferenceRepository
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.repositories.IScrapedElementRepository
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.database.repositories.IScreenContextRepository
import com.augmentalis.database.repositories.IScreenTransitionRepository
import com.augmentalis.database.repositories.IUserInteractionRepository
import com.augmentalis.database.repositories.IElementStateHistoryRepository
import com.augmentalis.database.repositories.IScrapedHierarchyRepository
import com.augmentalis.database.repositories.IElementRelationshipRepository
import com.augmentalis.database.repositories.IAppConsentHistoryRepository
import com.augmentalis.database.repositories.IElementCommandRepository
import com.augmentalis.database.repositories.IQualityMetricRepository
import com.augmentalis.database.repositories.IAppVersionRepository
import com.augmentalis.database.repositories.SQLDelightCommandRepository
import com.augmentalis.database.repositories.SQLDelightCommandHistoryRepository
import com.augmentalis.database.repositories.SQLDelightUserPreferenceRepository
import com.augmentalis.database.repositories.SQLDelightErrorReportRepository
import com.augmentalis.database.repositories.SQLDelightAvidRepository
import com.augmentalis.database.repositories.SQLDelightVoiceCommandRepository
import com.augmentalis.database.repositories.SQLDelightCommandUsageRepository
import com.augmentalis.database.repositories.SQLDelightContextPreferenceRepository
import com.augmentalis.database.repositories.SQLDelightScrapedAppRepository
import com.augmentalis.database.repositories.SQLDelightScrapedElementRepository
import com.augmentalis.database.repositories.SQLDelightGeneratedCommandRepository
import com.augmentalis.database.repositories.SQLDelightScreenContextRepository
import com.augmentalis.database.repositories.SQLDelightScreenTransitionRepository
import com.augmentalis.database.repositories.SQLDelightUserInteractionRepository
import com.augmentalis.database.repositories.SQLDelightElementStateHistoryRepository
import com.augmentalis.database.repositories.SQLDelightScrapedHierarchyRepository
import com.augmentalis.database.repositories.SQLDelightElementRelationshipRepository
import com.augmentalis.database.repositories.SQLDelightAppConsentHistoryRepository
import com.augmentalis.database.repositories.SQLDelightElementCommandRepository
import com.augmentalis.database.repositories.SQLDelightAppVersionRepository
import com.augmentalis.database.repositories.IPluginRepository
import com.augmentalis.database.repositories.SQLDelightPluginRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Main database manager providing access to all repositories.
 *
 * **SINGLETON PATTERN (FIX 2025-12-01):**
 * This class MUST be used as a singleton to prevent multiple SQLite connections
 * to the same database file, which causes SQLITE_BUSY errors.
 *
 * Use `VoiceOSDatabaseManager.getInstance(context)` to get the singleton instance.
 */
class VoiceOSDatabaseManager internal constructor(driverFactory: DatabaseDriverFactory) {

    companion object {
        // Thread-safe singleton instance
        @Volatile
        private var INSTANCE: VoiceOSDatabaseManager? = null

        // Lock object for double-checked locking pattern
        private val lock = Any()

        /**
         * Get the singleton instance of VoiceOSDatabaseManager.
         *
         * This ensures only ONE SQLite connection exists app-wide,
         * preventing database lock contention.
         *
         * Thread-safe using double-checked locking pattern.
         */
        fun getInstance(driverFactory: DatabaseDriverFactory): VoiceOSDatabaseManager {
            // First check (no locking)
            val instance = INSTANCE
            if (instance != null) {
                return instance
            }

            // Second check (with locking)
            return synchronized(lock) {
                val instance2 = INSTANCE
                if (instance2 != null) {
                    instance2
                } else {
                    VoiceOSDatabaseManager(driverFactory).also {
                        INSTANCE = it
                    }
                }
            }
        }
    }

    /**
     * Initialization state for database validation
     */
    sealed class InitializationState {
        object NotStarted : InitializationState()
        object InProgress : InitializationState()
        data class Completed(val timestamp: Long) : InitializationState()
        data class Failed(val error: String) : InitializationState()
    }

    // Initialization state tracking
    private val _initState = MutableStateFlow<InitializationState>(InitializationState.InProgress)
    val initState: StateFlow<InitializationState> = _initState

    private val driver: SqlDriver = driverFactory.createDriver()
    internal val database: VoiceOSDatabase = VoiceOSDatabase(driver)

    /**
     * Public accessor for database instance (needed for VoiceOSService foreign key verification)
     */
    fun getDatabase(): VoiceOSDatabase = database

    init {
        // Verify database initialization on creation
        try {
            // Verify foreign keys are enabled
            val fkEnabled = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA foreign_keys",
                mapper = { cursor ->
                    QueryResult.Value(if (cursor.next().value) {
                        cursor.getLong(0) == 1L
                    } else {
                        false
                    })
                },
                parameters = 0
            ).value

            if (!fkEnabled) {
                _initState.value = InitializationState.Failed("Foreign keys not enabled")
            } else {
                _initState.value = InitializationState.Completed(System.currentTimeMillis())
            }
        } catch (e: Exception) {
            _initState.value = InitializationState.Failed(e.message ?: "Unknown initialization error")
        }
    }

    // Repository interfaces (use these for abstraction)
    val commands: ICommandRepository = SQLDelightCommandRepository(database)
    val commandHistory: ICommandHistoryRepository = SQLDelightCommandHistoryRepository(database)
    val userPreferences: IUserPreferenceRepository = SQLDelightUserPreferenceRepository(database)
    val errorReports: IErrorReportRepository = SQLDelightErrorReportRepository(database)
    val avids: IAvidRepository = SQLDelightAvidRepository(database)
    val plugins: IPluginRepository = SQLDelightPluginRepository(database)

    // CommandManager repositories
    val voiceCommands: IVoiceCommandRepository = SQLDelightVoiceCommandRepository(database)
    val commandUsage: ICommandUsageRepository = SQLDelightCommandUsageRepository(database)
    val contextPreferences: IContextPreferenceRepository = SQLDelightContextPreferenceRepository(database)

    // VoiceOSCore scraping repositories (Phase 3 migration)
    val scrapedApps: IScrapedAppRepository = SQLDelightScrapedAppRepository(database)
    val scrapedElements: IScrapedElementRepository = SQLDelightScrapedElementRepository(database)
    val generatedCommands: IGeneratedCommandRepository = SQLDelightGeneratedCommandRepository(database)
    val screenContexts: IScreenContextRepository = SQLDelightScreenContextRepository(database)
    val screenTransitions: IScreenTransitionRepository = SQLDelightScreenTransitionRepository(database)
    val userInteractions: IUserInteractionRepository = SQLDelightUserInteractionRepository(database)
    val elementStateHistory: IElementStateHistoryRepository = SQLDelightElementStateHistoryRepository(database)
    val scrapedHierarchies: IScrapedHierarchyRepository = SQLDelightScrapedHierarchyRepository(database)
    val elementRelationships: IElementRelationshipRepository = SQLDelightElementRelationshipRepository(database)

    // LearnApp UX Phase 2 repositories
    val appConsentHistory: IAppConsentHistoryRepository = SQLDelightAppConsentHistoryRepository(database)

    // Metadata Quality Overlay & Manual Command Assignment (VOS-META-001)
    val elementCommands: IElementCommandRepository = SQLDelightElementCommandRepository(database)
    val qualityMetrics: IQualityMetricRepository = SQLDelightQualityMetricRepository(database)

    // Version-Aware Command Management (VOS4)
    val appVersions: IAppVersionRepository = SQLDelightAppVersionRepository(database)

    // Direct query access for advanced operations
    val commandHistoryQueries get() = database.commandHistoryQueries
    val customCommandQueries get() = database.customCommandQueries
    val recognitionLearningQueries get() = database.recognitionLearningQueries
    val generatedCommandQueries get() = database.generatedCommandQueries
    val scrapedAppQueries get() = database.scrapedAppQueries
    val userInteractionQueries get() = database.userInteractionQueries
    val elementStateHistoryQueries get() = database.elementStateHistoryQueries
    val settingsQueries get() = database.settingsQueries
    val deviceProfileQueries get() = database.deviceProfileQueries
    val touchGestureQueries get() = database.touchGestureQueries
    val userPreferenceQueries get() = database.userPreferenceQueries
    val errorReportQueries get() = database.errorReportQueries
    val languageModelQueries get() = database.languageModelQueries
    val gestureLearningQueries get() = database.gestureLearningQueries
    val usageStatisticQueries get() = database.usageStatisticQueries
    val scrappedCommandQueries get() = database.scrappedCommandQueries
    val scrapedElementQueries get() = database.scrapedElementQueries
    val screenContextQueries get() = database.screenContextQueries
    val screenTransitionQueries get() = database.screenTransitionQueries
    val userSequenceQueries get() = database.userSequenceQueries
    // AVID queries for AvidCreator module (backed by UUID tables)
    val avidElementQueries get() = database.uUIDElementQueries
    val avidHierarchyQueries get() = database.uUIDHierarchyQueries
    val avidAnalyticsQueries get() = database.uUIDAnalyticsQueries
    val avidAliasQueries get() = database.uUIDAliasQueries
    // Analytics and Retention settings are accessed via settingsQueries
    // Use: settingsQueries.getAnalyticsSettings() and settingsQueries.getRetentionSettings()

    // Plugin system queries
    val pluginQueries get() = database.pluginQueries
    val pluginDependencyQueries get() = database.pluginDependencyQueries
    val pluginPermissionQueries get() = database.pluginPermissionQueries
    val systemCheckpointQueries get() = database.systemCheckpointQueries

    // CommandManager queries
    val voiceCommandQueries get() = database.voiceCommandQueries
    val commandUsageQueries get() = database.commandUsageQueries
    val contextPreferenceQueries get() = database.contextPreferenceQueries
    val scrapedHierarchyQueries get() = database.scrapedHierarchyQueries
    val elementRelationshipQueries get() = database.elementRelationshipQueries

    // LearnApp queries (Phase 1 restoration)
    val learnedAppQueries get() = database.learnedAppQueries
    val explorationSessionQueries get() = database.explorationSessionQueries
    val navigationEdgeQueries get() = database.navigationEdgeQueries
    val screenStateQueries get() = database.screenStateQueries

    // LearnApp UX Phase 2 queries
    val appConsentHistoryQueries get() = database.appConsentHistoryQueries

    // Metadata Quality Overlay & Manual Command Assignment queries (VOS-META-001)
    val elementCommandQueries get() = database.elementCommandQueries

    // Version-Aware Command Management queries (VOS4)
    val appVersionQueries get() = database.appVersionQueries

    // Web scraping queries (LearnWeb)
    val scrapedWebsiteQueries get() = database.scrapedWebsiteQueries
    val scrapedWebElementQueries get() = database.scrapedWebElementQueries
    val generatedWebCommandQueries get() = database.generatedWebCommandQueries

    /**
     * Wait for database initialization to complete.
     *
     * This method suspends until the database is fully initialized and ready for use.
     * If initialization fails, throws IllegalStateException.
     *
     * @param timeoutMs Maximum time to wait in milliseconds (default: 10000ms)
     * @throws IllegalStateException if database initialization failed or timed out
     */
    suspend fun waitForInitialization(timeoutMs: Long = 10_000L) {
        val startTime = System.currentTimeMillis()

        // Wait for initialization to complete or fail
        val state = _initState.first {
            it is InitializationState.Completed ||
            it is InitializationState.Failed ||
            (System.currentTimeMillis() - startTime) > timeoutMs
        }

        when (state) {
            is InitializationState.Completed -> {
                // Success - database is ready
            }
            is InitializationState.Failed -> {
                throw IllegalStateException("Database initialization failed: ${state.error}")
            }
            else -> {
                if ((System.currentTimeMillis() - startTime) > timeoutMs) {
                    throw IllegalStateException("Database initialization timed out after ${timeoutMs}ms")
                }
            }
        }
    }

    /**
     * Check if database is ready for use (non-blocking).
     *
     * @return true if database is initialized and ready, false otherwise
     */
    fun isReady(): Boolean {
        return _initState.value is InitializationState.Completed
    }

    /**
     * Execute multiple operations in a transaction.
     */
    suspend fun <T> transaction(body: () -> T): T = withContext(Dispatchers.Default) {
        database.transactionWithResult {
            body()
        }
    }

    /**
     * Get database statistics.
     */
    suspend fun getStats(): DatabaseStats = withContext(Dispatchers.Default) {
        DatabaseStats(
            commandHistoryCount = commandHistoryQueries.count().executeAsOne(),
            customCommandCount = customCommandQueries.count().executeAsOne(),
            recognitionLearningCount = recognitionLearningQueries.count().executeAsOne(),
            generatedCommandCount = generatedCommandQueries.count().executeAsOne(),
            scrapedAppCount = scrapedAppQueries.count().executeAsOne(),
            errorReportCount = errorReportQueries.count().executeAsOne()
        )
    }

    /**
     * Clear all data from all tables.
     * Use with caution!
     */
    suspend fun clearAllData() = withContext(Dispatchers.Default) {
        database.transaction {
            commandHistoryQueries.deleteAll()
            customCommandQueries.deleteAll()
            recognitionLearningQueries.deleteAll()
            generatedCommandQueries.deleteAll()
            scrapedAppQueries.deleteAll()
            userInteractionQueries.deleteAll()
            elementStateHistoryQueries.deleteAll()
            deviceProfileQueries.deleteAll()
            touchGestureQueries.deleteAll()
            userPreferenceQueries.deleteAll()
            errorReportQueries.deleteAll()
            languageModelQueries.deleteAll()
            gestureLearningQueries.deleteAll()
            usageStatisticQueries.deleteAll()
            scrappedCommandQueries.deleteAll()
            scrapedElementQueries.deleteAll()
            screenContextQueries.deleteAll()
            screenTransitionQueries.deleteAll()
            userSequenceQueries.deleteAll()
            appConsentHistoryQueries.deleteAllConsentHistory()
            // Note: analyticsSettings and retentionSettings are single-record tables
            // They should be reset to defaults, not deleted
        }
    }

    /**
     * Optimize database by reclaiming unused space and defragmenting.
     * Should be run periodically (e.g., weekly) or after large deletions.
     *
     * Note: VACUUM can take several seconds on large databases.
     * Run on background thread only.
     */
    suspend fun vacuum() = withContext(Dispatchers.Default) {
        // Note: VACUUM cannot run inside a transaction, execute directly
        driver.execute(null, "VACUUM", 0)
    }

    /**
     * Check database integrity.
     * Returns true if database is healthy, false if corrupted.
     *
     * Checks performed:
     * - Table structure integrity
     * - Index consistency
     * - Foreign key constraints
     */
    suspend fun checkIntegrity(): Boolean = withContext(Dispatchers.Default) {
        database.transactionWithResult {
            val query = "PRAGMA integrity_check"
            driver.executeQuery(
                identifier = null,
                sql = query,
                mapper = { cursor ->
                    QueryResult.Value(if (cursor.next().value) {
                        cursor.getString(0) == "ok"
                    } else {
                        false
                    })
                },
                parameters = 0
            ).value
        }
    }

    /**
     * Get detailed integrity check results.
     */
    suspend fun getIntegrityReport(): List<String> = withContext(Dispatchers.Default) {
        database.transactionWithResult {
            val results = mutableListOf<String>()
            val query = "PRAGMA integrity_check"
            driver.executeQuery(
                identifier = null,
                sql = query,
                mapper = { cursor ->
                    while (cursor.next().value) {
                        results.add(cursor.getString(0) ?: "")
                    }
                    QueryResult.Value(Unit)
                },
                parameters = 0
            ).value
            results
        }
    }

    /**
     * Get database file size and page statistics.
     */
    suspend fun getDatabaseInfo(): DatabaseInfo = withContext(Dispatchers.Default) {
        database.transactionWithResult {
            val pageCount = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA page_count",
                mapper = { cursor ->
                    QueryResult.Value(if (cursor.next().value) {
                        cursor.getLong(0) ?: 0L
                    } else {
                        0L
                    })
                },
                parameters = 0
            ).value

            val pageSize = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA page_size",
                mapper = { cursor ->
                    QueryResult.Value(if (cursor.next().value) {
                        cursor.getLong(0) ?: 0L
                    } else {
                        0L
                    })
                },
                parameters = 0
            ).value

            val freelistCount = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA freelist_count",
                mapper = { cursor ->
                    QueryResult.Value(if (cursor.next().value) {
                        cursor.getLong(0) ?: 0L
                    } else {
                        0L
                    })
                },
                parameters = 0
            ).value

            DatabaseInfo(
                totalPages = pageCount,
                pageSize = pageSize,
                totalSize = pageCount * pageSize,
                unusedPages = freelistCount,
                unusedSize = freelistCount * pageSize
            )
        }
    }
}

/**
 * Database statistics.
 */
data class DatabaseStats(
    val commandHistoryCount: Long,
    val customCommandCount: Long,
    val recognitionLearningCount: Long,
    val generatedCommandCount: Long,
    val scrapedAppCount: Long,
    val errorReportCount: Long
)

/**
 * Database information including file size and page statistics.
 */
data class DatabaseInfo(
    val totalPages: Long,
    val pageSize: Long,
    val totalSize: Long,
    val unusedPages: Long,
    val unusedSize: Long
)
