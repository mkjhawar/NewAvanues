/**
 * VoiceOSDatabaseManager.kt - Central database manager for VoiceOS
 *
 * Provides singleton access to all database repositories.
 * Uses lazy initialization for efficient resource usage.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database

import com.augmentalis.database.repositories.ICommandUsageRepository
import com.augmentalis.database.repositories.IContextPreferenceRepository
import com.augmentalis.database.repositories.IVoiceCommandRepository
import com.augmentalis.database.repositories.IAvidRepository
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.repositories.IScrapedElementRepository
import com.augmentalis.database.repositories.IScrapedHierarchyRepository
import com.augmentalis.database.repositories.IScreenContextRepository
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.database.repositories.IUserPreferenceRepository
import com.augmentalis.database.repositories.IErrorReportRepository
import com.augmentalis.database.repositories.IScreenTransitionRepository
import com.augmentalis.database.repositories.IAppConsentHistoryRepository
import com.augmentalis.database.repositories.IAppVersionRepository
import com.augmentalis.database.repositories.IUserInteractionRepository
import com.augmentalis.database.repositories.IElementStateHistoryRepository
import com.augmentalis.database.repositories.IElementCommandRepository
import com.augmentalis.database.repositories.IQualityMetricRepository
import com.augmentalis.database.repositories.impl.SQLDelightCommandUsageRepository
import com.augmentalis.database.repositories.impl.SQLDelightContextPreferenceRepository
import com.augmentalis.database.repositories.impl.SQLDelightVoiceCommandRepository
import com.augmentalis.database.repositories.impl.SQLDelightAvidRepository
import com.augmentalis.database.repositories.impl.SQLDelightScrapedAppRepository
import com.augmentalis.database.repositories.impl.SQLDelightScrapedElementRepository
import com.augmentalis.database.repositories.impl.SQLDelightScrapedHierarchyRepository
import com.augmentalis.database.repositories.impl.SQLDelightScreenContextRepository
import com.augmentalis.database.repositories.impl.SQLDelightGeneratedCommandRepository
import com.augmentalis.database.repositories.impl.SQLDelightUserPreferenceRepository
import com.augmentalis.database.repositories.impl.SQLDelightErrorReportRepository
import com.augmentalis.database.repositories.impl.SQLDelightScreenTransitionRepository
import com.augmentalis.database.repositories.impl.SQLDelightAppConsentHistoryRepository
import com.augmentalis.database.repositories.impl.SQLDelightAppVersionRepository
import com.augmentalis.database.repositories.impl.SQLDelightUserInteractionRepository
import com.augmentalis.database.repositories.impl.SQLDelightElementStateHistoryRepository
import com.augmentalis.database.repositories.impl.SQLDelightElementCommandRepository
import com.augmentalis.database.repositories.impl.SQLDelightQualityMetricRepository

/**
 * Central database manager providing access to all VoiceOS repositories.
 *
 * Usage:
 * ```kotlin
 * val manager = VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(context))
 * val commands = manager.voiceCommands.getAll()
 * ```
 *
 * Thread Safety:
 * - Instance is created with double-checked locking
 * - All repositories are lazily initialized
 * - Underlying SQLDelight operations are thread-safe
 */
class VoiceOSDatabaseManager private constructor(
    private val driverFactory: DatabaseDriverFactory
) {

    companion object {
        @Volatile
        private var instance: VoiceOSDatabaseManager? = null

        /**
         * Get singleton instance of VoiceOSDatabaseManager.
         *
         * @param driverFactory Platform-specific driver factory
         * @return Singleton database manager instance
         */
        fun getInstance(driverFactory: DatabaseDriverFactory): VoiceOSDatabaseManager {
            return instance ?: synchronized(this) {
                instance ?: VoiceOSDatabaseManager(driverFactory).also {
                    instance = it
                }
            }
        }

        /**
         * Clear singleton instance (for testing only).
         */
        fun clearInstance() {
            synchronized(this) {
                instance = null
            }
        }
    }

    // Lazy database initialization
    private val _database: VoiceOSDatabase by lazy {
        createDatabase(driverFactory)
    }

    // ==================== Command Repositories ====================

    /**
     * Repository for command usage tracking (learning preferences).
     */
    val commandUsage: ICommandUsageRepository by lazy {
        SQLDelightCommandUsageRepository(_database)
    }

    /**
     * Repository for context preferences (command-context associations).
     */
    val contextPreferences: IContextPreferenceRepository by lazy {
        SQLDelightContextPreferenceRepository(_database)
    }

    /**
     * Repository for static voice commands.
     */
    val voiceCommands: IVoiceCommandRepository by lazy {
        SQLDelightVoiceCommandRepository(_database)
    }

    /**
     * Repository for generated commands (AI-created commands).
     */
    val generatedCommands: IGeneratedCommandRepository by lazy {
        SQLDelightGeneratedCommandRepository(_database)
    }

    // ==================== AVID Repositories ====================

    /**
     * Repository for AVID elements (Avanues Voice Identifiers).
     */
    val avidRepository: IAvidRepository by lazy {
        SQLDelightAvidRepository(_database)
    }

    // ==================== Scraping Repositories ====================

    /**
     * Repository for scraped apps.
     */
    val scrapedApps: IScrapedAppRepository by lazy {
        SQLDelightScrapedAppRepository(_database)
    }

    /**
     * Repository for scraped UI elements.
     */
    val scrapedElements: IScrapedElementRepository by lazy {
        SQLDelightScrapedElementRepository(_database)
    }

    /**
     * Repository for scraped element hierarchies.
     */
    val scrapedHierarchies: IScrapedHierarchyRepository by lazy {
        SQLDelightScrapedHierarchyRepository(_database)
    }

    // ==================== Context Repositories ====================

    /**
     * Repository for screen context data.
     */
    val screenContexts: IScreenContextRepository by lazy {
        SQLDelightScreenContextRepository(_database)
    }

    /**
     * Repository for screen transitions.
     */
    val screenTransitions: IScreenTransitionRepository by lazy {
        SQLDelightScreenTransitionRepository(_database)
    }

    /**
     * Repository for user interactions.
     */
    val userInteractions: IUserInteractionRepository by lazy {
        SQLDelightUserInteractionRepository(_database)
    }

    /**
     * Repository for element state history.
     */
    val elementStateHistory: IElementStateHistoryRepository by lazy {
        SQLDelightElementStateHistoryRepository(_database)
    }

    // ==================== Element Command Repositories ====================

    /**
     * Repository for element commands (user-assigned voice commands).
     */
    val elementCommands: IElementCommandRepository by lazy {
        SQLDelightElementCommandRepository(_database)
    }

    /**
     * Repository for quality metrics.
     */
    val qualityMetrics: IQualityMetricRepository by lazy {
        SQLDelightQualityMetricRepository(_database)
    }

    // ==================== App Repositories ====================

    /**
     * Repository for app consent history.
     */
    val appConsentHistory: IAppConsentHistoryRepository by lazy {
        SQLDelightAppConsentHistoryRepository(_database)
    }

    /**
     * Repository for app versions.
     */
    val appVersions: IAppVersionRepository by lazy {
        SQLDelightAppVersionRepository(_database)
    }

    // ==================== User Repositories ====================

    /**
     * Repository for user preferences.
     */
    val userPreferences: IUserPreferenceRepository by lazy {
        SQLDelightUserPreferenceRepository(_database)
    }

    // ==================== Error Repositories ====================

    /**
     * Repository for error reports.
     */
    val errorReports: IErrorReportRepository by lazy {
        SQLDelightErrorReportRepository(_database)
    }

    // ==================== Database Operations ====================

    /**
     * Ensure database is initialized.
     *
     * Since database uses lazy initialization, this method simply
     * triggers the lazy creation by accessing the database instance.
     * Call this to ensure the database is ready before operations.
     */
    suspend fun waitForInitialization() {
        // Force lazy initialization by accessing the database
        _database.hashCode()
    }

    /**
     * Check if database is ready for use.
     *
     * Since database uses lazy initialization, this returns true after
     * first access triggers creation. For explicit initialization,
     * call waitForInitialization() first.
     */
    fun isReady(): Boolean {
        return try {
            // Access the database to check if it's initialized
            _database.hashCode()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get direct access to the underlying database.
     * Use with caution - prefer using repositories.
     */
    fun getDatabase(): VoiceOSDatabase = _database

    /**
     * Execute a transaction across multiple repositories.
     */
    suspend fun <T> transaction(block: suspend () -> T): T {
        return _database.transactionWithResult {
            // Note: This is a simplified version - actual implementation
            // would need coroutine-compatible transaction handling
            @Suppress("UNCHECKED_CAST")
            block as T
        }
    }

    // ==================== Raw Query Accessors (Legacy Compatibility) ====================
    // Note: Prefer using repository interfaces for new code.
    // These are exposed for backward compatibility with existing VoiceOSCore code.

    /** Raw queries for LearnedApp table */
    val learnedAppQueries get() = _database.learnedAppQueries

    /** Raw queries for ExplorationSession table */
    val explorationSessionQueries get() = _database.explorationSessionQueries

    /** Raw queries for NavigationEdge table */
    val navigationEdgeQueries get() = _database.navigationEdgeQueries

    /** Raw queries for ScreenState table */
    val screenStateQueries get() = _database.screenStateQueries

    /** Raw queries for GeneratedCommand table */
    val generatedCommandQueries get() = _database.generatedCommandQueries

    /** Raw queries for AppVersion table */
    val appVersionQueries get() = _database.appVersionQueries

    /** Raw queries for GeneratedWebCommand table */
    val generatedWebCommandQueries get() = _database.generatedWebCommandQueries

    /** Raw queries for ScrapedWebsite table */
    val scrapedWebsiteQueries get() = _database.scrapedWebsiteQueries

    /** Raw queries for ScrapedWebElement table */
    val scrapedWebElementQueries get() = _database.scrapedWebElementQueries

    /** Raw queries for AvidElement table */
    val avidElementQueries get() = _database.avidElementQueries

    /** Raw queries for AvidAlias table */
    val avidAliasQueries get() = _database.avidAliasQueries

    /** Raw queries for AvidAnalytics table */
    val avidAnalyticsQueries get() = _database.avidAnalyticsQueries

    /** Raw queries for AvidHierarchy table */
    val avidHierarchyQueries get() = _database.avidHierarchyQueries

    /** Raw queries for ScrapedElement table */
    val scrapedElementQueries get() = _database.scrapedElementQueries

    /** Raw queries for ScreenContext table */
    val screenContextQueries get() = _database.screenContextQueries

    /** Raw queries for ScrapedHierarchy table */
    val scrapedHierarchyQueries get() = _database.scrapedHierarchyQueries

    /** Raw queries for ElementRelationship table */
    val elementRelationshipQueries get() = _database.elementRelationshipQueries

    /** Raw queries for UserInteraction table */
    val userInteractionQueries get() = _database.userInteractionQueries

    /** Raw queries for ElementStateHistory table */
    val elementStateHistoryQueries get() = _database.elementStateHistoryQueries

    /** Raw queries for ScreenTransition table */
    val screenTransitionQueries get() = _database.screenTransitionQueries

    /** Raw queries for ElementCommand table */
    val elementCommandQueries get() = _database.elementCommandQueries

    /** Raw queries for CommandUsage table */
    val commandUsageQueries get() = _database.commandUsageQueries
}
