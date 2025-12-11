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
import com.augmentalis.database.repositories.IUUIDRepository
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
import com.augmentalis.database.repositories.impl.SQLDelightCommandRepository
import com.augmentalis.database.repositories.impl.SQLDelightCommandHistoryRepository
import com.augmentalis.database.repositories.impl.SQLDelightUserPreferenceRepository
import com.augmentalis.database.repositories.impl.SQLDelightErrorReportRepository
import com.augmentalis.database.repositories.impl.SQLDelightUUIDRepository
import com.augmentalis.database.repositories.impl.SQLDelightVoiceCommandRepository
import com.augmentalis.database.repositories.impl.SQLDelightCommandUsageRepository
import com.augmentalis.database.repositories.impl.SQLDelightContextPreferenceRepository
import com.augmentalis.database.repositories.impl.SQLDelightScrapedAppRepository
import com.augmentalis.database.repositories.impl.SQLDelightScrapedElementRepository
import com.augmentalis.database.repositories.impl.SQLDelightGeneratedCommandRepository
import com.augmentalis.database.repositories.impl.SQLDelightScreenContextRepository
import com.augmentalis.database.repositories.impl.SQLDelightScreenTransitionRepository
import com.augmentalis.database.repositories.impl.SQLDelightUserInteractionRepository
import com.augmentalis.database.repositories.impl.SQLDelightElementStateHistoryRepository
import com.augmentalis.database.repositories.impl.SQLDelightScrapedHierarchyRepository
import com.augmentalis.database.repositories.impl.SQLDelightElementRelationshipRepository
import com.augmentalis.database.repositories.impl.SQLDelightAppConsentHistoryRepository
import com.augmentalis.database.repositories.impl.SQLDelightElementCommandRepository
import com.augmentalis.database.repositories.impl.SQLDelightQualityMetricRepository
import com.augmentalis.database.repositories.plugin.IPluginRepository
import com.augmentalis.database.repositories.plugin.SQLDelightPluginRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        @Volatile
        private var INSTANCE: VoiceOSDatabaseManager? = null

        /**
         * Get the singleton instance of VoiceOSDatabaseManager.
         *
         * This ensures only ONE SQLite connection exists app-wide,
         * preventing database lock contention.
         */
        fun getInstance(driverFactory: DatabaseDriverFactory): VoiceOSDatabaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VoiceOSDatabaseManager(driverFactory).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val driver: SqlDriver = driverFactory.createDriver()
    private val database: VoiceOSDatabase = VoiceOSDatabase(driver)

    // Repository interfaces (use these for abstraction)
    val commands: ICommandRepository = SQLDelightCommandRepository(database)
    val commandHistory: ICommandHistoryRepository = SQLDelightCommandHistoryRepository(database)
    val userPreferences: IUserPreferenceRepository = SQLDelightUserPreferenceRepository(database)
    val errorReports: IErrorReportRepository = SQLDelightErrorReportRepository(database)
    val uuids: IUUIDRepository = SQLDelightUUIDRepository(database)
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
    // UUID queries for UUIDCreator module
    val uuidElementQueries get() = database.uUIDElementQueries
    val uuidHierarchyQueries get() = database.uUIDHierarchyQueries
    val uuidAnalyticsQueries get() = database.uUIDAnalyticsQueries
    val uuidAliasQueries get() = database.uUIDAliasQueries
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
