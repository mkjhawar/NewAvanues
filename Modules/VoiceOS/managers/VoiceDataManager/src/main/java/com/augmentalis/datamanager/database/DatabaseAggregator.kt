/**
 * DatabaseAggregator.kt - Central DAO Access Point
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (Anthropic)
 * Created: 2025-10-23 16:41:59 PDT
 * Part of: VOS4 Phase 3 - Conciseness Refactoring
 *
 * Purpose:
 * Provides single-point access to all database DAOs, replacing the unused
 * IDatabaseManager interface. This is a concrete implementation following
 * VOS4's direct implementation principle.
 *
 * Design Rationale:
 * - Aggregates all 14 DAOs from VoiceOSDatabase for convenient access
 * - NO interface - direct implementation per ADR-002
 * - Thread-safe singleton pattern
 * - Lazy initialization of database instance
 * - Comprehensive KDoc for all DAO accessors
 *
 * Usage:
 * ```kotlin
 * val aggregator = DatabaseAggregator.getInstance(context)
 * val preferences = aggregator.userPreferences().getAll()
 * aggregator.commandHistory().insert(entry)
 * ```
 *
 * Related:
 * - Replaces: IDatabaseManager interface (deleted, 514 lines)
 * - See: ADR-002 (No Interfaces by Default)
 * - Database: VoiceOSDatabase (14 entities, 14 DAOs)
 */
package com.augmentalis.datamanager.database

import android.content.Context
import com.augmentalis.datamanager.dao.*

/**
 * Database Aggregator - Central DAO Access Point
 *
 * Provides convenient access to all database DAOs through a single class.
 * This eliminates the need for clients to interact with the Room database
 * directly and provides a clean API surface.
 *
 * Thread Safety:
 * - Singleton instance is thread-safe (double-checked locking)
 * - All DAOs are thread-safe (provided by Room)
 * - Safe to call from any thread
 *
 * Lifecycle:
 * - Initialize once at app startup: DatabaseAggregator.getInstance(context)
 * - Use throughout app lifetime
 * - Clear instance in tests: DatabaseAggregator.clearInstance()
 *
 * @property context Application context for database initialization
 */
class DatabaseAggregator private constructor(context: Context) {

    /**
     * VoiceOS database instance
     * Lazy-initialized on first access
     */
    private val database: VoiceOSDatabase = VoiceOSDatabase.getInstance(context)

    // ========================================
    // Settings DAOs
    // ========================================

    /**
     * Analytics Settings DAO
     *
     * Manages analytics and telemetry configuration (single-record table).
     *
     * Common operations:
     * - get(): Get current analytics settings
     * - update(settings): Update analytics configuration
     * - insert(settings): Initialize settings (first run only)
     *
     * Use cases:
     * - Enable/disable performance tracking
     * - Configure error reporting
     * - Manage user consent for analytics
     *
     * @return AnalyticsSettingsDao for analytics configuration
     */
    fun analyticsSettings(): AnalyticsSettingsDao = database.analyticsSettingsDao()

    /**
     * Retention Settings DAO
     *
     * Manages data retention policies (single-record table).
     *
     * Common operations:
     * - get(): Get current retention settings
     * - update(settings): Update retention policies
     * - insert(settings): Initialize settings (first run only)
     *
     * Use cases:
     * - Configure command history retention
     * - Set automatic cleanup policies
     * - Manage database size limits
     *
     * @return RetentionSettingsDao for retention policies
     */
    fun retentionSettings(): RetentionSettingsDao = database.retentionSettingsDao()

    /**
     * User Preference DAO
     *
     * Manages user preferences and app settings.
     *
     * Common operations:
     * - getAll(): Get all user preferences
     * - getByKey(key): Get specific preference
     * - insert(pref): Add new preference
     * - update(pref): Update existing preference
     * - delete(pref): Remove preference
     *
     * Use cases:
     * - Store UI preferences
     * - Manage voice command settings
     * - Configure gesture sensitivity
     *
     * @return UserPreferenceDao for user preferences
     */
    fun userPreferences(): UserPreferenceDao = database.userPreferenceDao()

    // ========================================
    // Command & History DAOs
    // ========================================

    /**
     * Command History Entry DAO
     *
     * Tracks executed voice commands and their outcomes.
     *
     * Common operations:
     * - getRecent(limit): Get recent command history
     * - insert(entry): Log command execution
     * - deleteOlderThan(timestamp): Cleanup old history
     * - getByDateRange(start, end): Get history for period
     *
     * Use cases:
     * - Command usage analytics
     * - Debugging command failures
     * - User activity tracking
     * - Retention policy enforcement
     *
     * @return CommandHistoryEntryDao for command history
     */
    fun commandHistory(): CommandHistoryEntryDao = database.commandHistoryEntryDao()

    /**
     * Custom Command DAO
     *
     * Manages user-defined custom voice commands.
     *
     * Common operations:
     * - getAll(): Get all custom commands
     * - getByCategory(category): Get commands by category
     * - insert(command): Add new custom command
     * - update(command): Modify existing command
     * - delete(command): Remove custom command
     *
     * Use cases:
     * - Create app-specific commands
     * - Define command shortcuts
     * - Personalize voice control
     *
     * @return CustomCommandDao for custom commands
     */
    fun customCommands(): CustomCommandDao = database.customCommandDao()

    /**
     * Scrapped Command DAO
     *
     * Stores commands extracted from app UI elements via scraping.
     *
     * Common operations:
     * - getByPackage(packageName): Get scraped commands for app
     * - insert(command): Add scraped command
     * - deleteByPackage(packageName): Remove app's commands
     * - getAll(): Get all scraped commands
     *
     * Use cases:
     * - Auto-generate commands from UI
     * - Learn app-specific vocabulary
     * - Enable voice control for new apps
     *
     * @return ScrappedCommandDao for scraped commands
     */
    fun scrappedCommands(): ScrappedCommandDao = database.scrappedCommandDao()

    /**
     * User Sequence DAO
     *
     * Manages command sequences (macros) defined by users.
     *
     * Common operations:
     * - getAll(): Get all sequences
     * - getByName(name): Get specific sequence
     * - insert(sequence): Create new sequence
     * - update(sequence): Modify sequence
     * - delete(sequence): Remove sequence
     *
     * Use cases:
     * - Define multi-step command macros
     * - Automate common workflows
     * - Chain multiple voice commands
     *
     * @return UserSequenceDao for command sequences
     */
    fun userSequences(): UserSequenceDao = database.userSequenceDao()

    // ========================================
    // Learning & Recognition DAOs
    // ========================================

    /**
     * Gesture Learning Data DAO
     *
     * Stores data for learning and improving gesture recognition.
     *
     * Common operations:
     * - getAll(): Get all learning data
     * - insert(data): Add new training sample
     * - deleteOld(threshold): Remove outdated data
     * - getByGestureType(type): Get samples for gesture
     *
     * Use cases:
     * - Train gesture recognition models
     * - Adapt to user's gesture patterns
     * - Improve recognition accuracy
     *
     * @return GestureLearningDataDao for gesture learning
     */
    fun gestureLearning(): GestureLearningDataDao = database.gestureLearningDataDao()

    /**
     * Recognition Learning DAO
     *
     * Stores data for improving voice recognition accuracy.
     *
     * Common operations:
     * - getAll(): Get all recognition data
     * - insert(data): Add new recognition sample
     * - getByCommand(commandId): Get samples for command
     * - deleteOld(threshold): Remove outdated data
     *
     * Use cases:
     * - Adapt to user's voice patterns
     * - Learn pronunciation variations
     * - Improve recognition accuracy
     *
     * @return RecognitionLearningDao for voice recognition learning
     */
    fun recognitionLearning(): RecognitionLearningDao = database.recognitionLearningDao()

    /**
     * Language Model DAO
     *
     * Manages language models for voice recognition.
     *
     * Common operations:
     * - getAll(): Get all language models
     * - getByLocale(locale): Get models for locale
     * - insert(model): Add new model
     * - update(model): Update model metadata
     * - delete(model): Remove model
     *
     * Use cases:
     * - Support multiple languages
     * - Manage offline recognition models
     * - Configure speech recognition
     *
     * @return LanguageModelDao for language models
     */
    fun languageModels(): LanguageModelDao = database.languageModelDao()

    // ========================================
    // Gesture & Input DAOs
    // ========================================

    /**
     * Touch Gesture DAO
     *
     * Manages custom touch gesture definitions.
     *
     * Common operations:
     * - getAll(): Get all gestures
     * - getById(id): Get specific gesture
     * - insert(gesture): Define new gesture
     * - update(gesture): Modify gesture
     * - delete(gesture): Remove gesture
     *
     * Use cases:
     * - Define custom swipe patterns
     * - Configure gesture actions
     * - Personalize gesture controls
     *
     * @return TouchGestureDao for touch gestures
     */
    fun touchGestures(): TouchGestureDao = database.touchGestureDao()

    // ========================================
    // Analytics & Monitoring DAOs
    // ========================================

    /**
     * Usage Statistic DAO
     *
     * Tracks app and feature usage statistics.
     *
     * Common operations:
     * - getAll(): Get all statistics
     * - getByDateRange(start, end): Get stats for period
     * - insert(stat): Record new statistic
     * - deleteOld(threshold): Remove old stats
     * - getByFeature(feature): Get stats for feature
     *
     * Use cases:
     * - Feature usage analytics
     * - Performance monitoring
     * - User behavior tracking
     * - Retention reporting
     *
     * @return UsageStatisticDao for usage statistics
     */
    fun usageStatistics(): UsageStatisticDao = database.usageStatisticDao()

    /**
     * Error Report DAO
     *
     * Stores error reports for debugging and telemetry.
     *
     * Common operations:
     * - getRecent(limit): Get recent errors
     * - insert(report): Log new error
     * - deleteOld(threshold): Remove old reports
     * - getByType(type): Get errors by type
     * - markAsSent(id): Mark report as uploaded
     *
     * Use cases:
     * - Error logging and tracking
     * - Crash reporting
     * - Debugging production issues
     * - Telemetry and analytics
     *
     * @return ErrorReportDao for error reports
     */
    fun errorReports(): ErrorReportDao = database.errorReportDao()

    // ========================================
    // Device & Profile DAOs
    // ========================================

    /**
     * Device Profile DAO
     *
     * Manages device-specific configuration profiles.
     *
     * Common operations:
     * - getAll(): Get all device profiles
     * - getCurrent(): Get current device profile
     * - insert(profile): Create new profile
     * - update(profile): Modify profile
     * - delete(profile): Remove profile
     *
     * Use cases:
     * - Multi-device support
     * - Device-specific settings
     * - Configuration management
     * - Profile synchronization
     *
     * @return DeviceProfileDao for device profiles
     */
    fun deviceProfiles(): DeviceProfileDao = database.deviceProfileDao()

    // ========================================
    // Database Management
    // ========================================

    /**
     * Get the underlying VoiceOS database instance
     *
     * Provides direct access to the Room database for advanced operations
     * such as transactions, migrations, or custom queries.
     *
     * **Warning:** Direct database access bypasses aggregator abstractions.
     * Prefer using the DAO accessors above unless you need advanced features.
     *
     * @return VoiceOSDatabase instance
     */
    fun getDatabase(): VoiceOSDatabase = database

    companion object {
        @Volatile
        private var INSTANCE: DatabaseAggregator? = null

        /**
         * Get DatabaseAggregator singleton instance
         *
         * Thread-safe singleton implementation using double-checked locking.
         * The instance persists for the lifetime of the app process.
         *
         * @param context Application context (will be converted to applicationContext)
         * @return DatabaseAggregator singleton instance
         */
        fun getInstance(context: Context): DatabaseAggregator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseAggregator(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        /**
         * Clear singleton instance
         *
         * **Testing only** - Clears the aggregator instance to allow
         * fresh initialization in tests. DO NOT use in production code.
         *
         * Also clears the underlying VoiceOSDatabase instance.
         */
        @Suppress("unused")
        internal fun clearInstance() {
            synchronized(this) {
                VoiceOSDatabase.clearInstance()
                INSTANCE = null
            }
        }
    }
}
