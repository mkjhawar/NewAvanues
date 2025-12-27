/**
 * DataModule.kt - Hilt Dependency Injection Module for Data Layer (SQLDelight)
 * Path: app/src/main/java/com/augmentalis/voiceos/di/DataModule.kt
 *
 * Author: Manoj Jhawar
 * Created: 2025-10-09
 * Migrated to SQLDelight: 2025-11-26
 *
 * Provides data layer dependencies for Hilt injection:
 * - VoiceOSDatabaseManager (SQLDelight) for data persistence
 * - Repository interfaces for data operations
 * - Direct query access for DAOs not yet migrated to repositories
 *
 * Migration Status:
 * - ✅ CommandHistory → ICommandHistoryRepository
 * - ✅ CustomCommand → ICommandRepository
 * - ✅ VoiceCommand → IVoiceCommandRepository
 * - ✅ CommandUsage → ICommandUsageRepository
 * - ✅ ErrorReport → IErrorReportRepository
 * - ✅ UserPreference → IUserPreferenceRepository
 * - ✅ ScrapedApp → IScrapedAppRepository
 * - ✅ GeneratedCommand → IGeneratedCommandRepository
 * - ⏳ Analytics, DeviceProfile, Gestures, Language, etc. → Direct queries (temporary)
 */

package com.augmentalis.voiceos.di

import android.content.Context
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.repositories.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Data Layer Hilt module (SQLDelight)
 *
 * This module provides all data persistence related dependencies including:
 * - VoiceOSDatabaseManager for centralized database access
 * - Repository interfaces for business logic operations
 * - Direct query access for advanced/legacy operations
 *
 * All dependencies are Singleton scoped to ensure single database instance.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // ========================================================================
    // Core Database Provider
    // ========================================================================

    /**
     * Provides VoiceOSDatabaseManager instance (SQLDelight)
     *
     * VoiceOSDatabaseManager is the central database manager providing:
     * - Repository interfaces for all major operations
     * - Direct query access for advanced operations
     * - Transaction support
     * - Database lifecycle management
     *
     * Database covers:
     * - Voice commands and command history
     * - User preferences and settings
     * - Analytics and usage statistics
     * - Error reporting and diagnostics
     * - Gesture learning and touch patterns
     * - Speech recognition models
     * - App scraping and element metadata
     * - Screen contexts and navigation
     *
     * @param context Application context for database initialization
     * @return VoiceOSDatabaseManager singleton instance
     */
    @Provides
    @Singleton
    fun provideDatabaseManager(
        @ApplicationContext context: Context
    ): VoiceOSDatabaseManager {
        val driverFactory = DatabaseDriverFactory(context)
        return VoiceOSDatabaseManager.getInstance(driverFactory)
    }

    // ========================================================================
    // Repository Providers (Migrated from Room DAOs)
    // ========================================================================

    /**
     * Provides CommandHistoryRepository for command history tracking
     * Replaces: CommandHistoryEntryDao
     */
    @Provides
    @Singleton
    fun provideCommandHistoryRepository(
        dbManager: VoiceOSDatabaseManager
    ): ICommandHistoryRepository {
        return dbManager.commandHistory
    }

    /**
     * Provides CommandRepository for custom/user-defined commands
     * Replaces: CustomCommandDao
     */
    @Provides
    @Singleton
    fun provideCommandRepository(
        dbManager: VoiceOSDatabaseManager
    ): ICommandRepository {
        return dbManager.commands
    }

    /**
     * Provides VoiceCommandRepository for voice command definitions
     * Replaces: Part of CustomCommandDao functionality
     */
    @Provides
    @Singleton
    fun provideVoiceCommandRepository(
        dbManager: VoiceOSDatabaseManager
    ): IVoiceCommandRepository {
        return dbManager.voiceCommands
    }

    /**
     * Provides CommandUsageRepository for usage statistics
     * Replaces: UsageStatisticDao
     */
    @Provides
    @Singleton
    fun provideCommandUsageRepository(
        dbManager: VoiceOSDatabaseManager
    ): ICommandUsageRepository {
        return dbManager.commandUsage
    }

    /**
     * Provides ErrorReportRepository for error tracking
     * Replaces: ErrorReportDao
     */
    @Provides
    @Singleton
    fun provideErrorReportRepository(
        dbManager: VoiceOSDatabaseManager
    ): IErrorReportRepository {
        return dbManager.errorReports
    }

    /**
     * Provides UserPreferenceRepository for user preferences
     * Replaces: UserPreferenceDao
     */
    @Provides
    @Singleton
    fun provideUserPreferenceRepository(
        dbManager: VoiceOSDatabaseManager
    ): IUserPreferenceRepository {
        return dbManager.userPreferences
    }

    /**
     * Provides ContextPreferenceRepository for context-based preferences
     * New functionality for AI-powered command suggestions
     */
    @Provides
    @Singleton
    fun provideContextPreferenceRepository(
        dbManager: VoiceOSDatabaseManager
    ): IContextPreferenceRepository {
        return dbManager.contextPreferences
    }

    /**
     * Provides ScrapedAppRepository for app metadata
     * Replaces: AppDao (from VoiceOSCore)
     */
    @Provides
    @Singleton
    fun provideScrapedAppRepository(
        dbManager: VoiceOSDatabaseManager
    ): IScrapedAppRepository {
        return dbManager.scrapedApps
    }

    /**
     * Provides GeneratedCommandRepository for auto-generated commands
     * Replaces: ScrappedCommandDao
     */
    @Provides
    @Singleton
    fun provideGeneratedCommandRepository(
        dbManager: VoiceOSDatabaseManager
    ): IGeneratedCommandRepository {
        return dbManager.generatedCommands
    }

    /**
     * Provides ScrapedElementRepository for UI element metadata
     * New functionality for LearnApp features
     */
    @Provides
    @Singleton
    fun provideScrapedElementRepository(
        dbManager: VoiceOSDatabaseManager
    ): IScrapedElementRepository {
        return dbManager.scrapedElements
    }

    // ========================================================================
    // Legacy Query Providers (Temporary - until repositories created)
    // ========================================================================

    /**
     * Provides direct access to analytics settings queries
     * TODO: Create IAnalyticsSettingsRepository
     * Replaces: AnalyticsSettingsDao
     */
    @Provides
    @Singleton
    fun provideAnalyticsSettingsQueries(
        dbManager: VoiceOSDatabaseManager
    ) = dbManager.settingsQueries

    /**
     * Provides direct access to device profile queries
     * TODO: Create IDeviceProfileRepository
     * Replaces: DeviceProfileDao
     */
    @Provides
    @Singleton
    fun provideDeviceProfileQueries(
        dbManager: VoiceOSDatabaseManager
    ) = dbManager.deviceProfileQueries

    /**
     * Provides direct access to gesture learning queries
     * TODO: Create IGestureLearningRepository
     * Replaces: GestureLearningDataDao
     */
    @Provides
    @Singleton
    fun provideGestureLearningQueries(
        dbManager: VoiceOSDatabaseManager
    ) = dbManager.gestureLearningQueries

    /**
     * Provides direct access to language model queries
     * TODO: Create ILanguageModelRepository
     * Replaces: LanguageModelDao
     */
    @Provides
    @Singleton
    fun provideLanguageModelQueries(
        dbManager: VoiceOSDatabaseManager
    ) = dbManager.languageModelQueries

    /**
     * Provides direct access to recognition learning queries
     * TODO: Create IRecognitionLearningRepository
     * Replaces: RecognitionLearningDao
     */
    @Provides
    @Singleton
    fun provideRecognitionLearningQueries(
        dbManager: VoiceOSDatabaseManager
    ) = dbManager.recognitionLearningQueries

    /**
     * Provides direct access to touch gesture queries
     * TODO: Create ITouchGestureRepository
     * Replaces: TouchGestureDao
     */
    @Provides
    @Singleton
    fun provideTouchGestureQueries(
        dbManager: VoiceOSDatabaseManager
    ) = dbManager.touchGestureQueries

    /**
     * Provides direct access to usage statistic queries (legacy)
     * Use ICommandUsageRepository for new code
     * Replaces: UsageStatisticDao (legacy queries)
     */
    @Provides
    @Singleton
    fun provideUsageStatisticQueries(
        dbManager: VoiceOSDatabaseManager
    ) = dbManager.usageStatisticQueries

    /**
     * Provides direct access to custom command queries (legacy)
     * Use ICommandRepository for new code
     * Replaces: CustomCommandDao (legacy queries)
     */
    @Provides
    @Singleton
    fun provideCustomCommandQueries(
        dbManager: VoiceOSDatabaseManager
    ) = dbManager.customCommandQueries

    // ========================================================================
    // Additional Advanced Repositories
    // ========================================================================

    /**
     * Provides ScreenContextRepository for screen state tracking
     * New functionality for navigation and context awareness
     */
    @Provides
    @Singleton
    fun provideScreenContextRepository(
        dbManager: VoiceOSDatabaseManager
    ): IScreenContextRepository {
        return dbManager.screenContexts
    }

    /**
     * Provides UserInteractionRepository for user interaction tracking
     * New functionality for learning user patterns
     */
    @Provides
    @Singleton
    fun provideUserInteractionRepository(
        dbManager: VoiceOSDatabaseManager
    ): IUserInteractionRepository {
        return dbManager.userInteractions
    }
}
