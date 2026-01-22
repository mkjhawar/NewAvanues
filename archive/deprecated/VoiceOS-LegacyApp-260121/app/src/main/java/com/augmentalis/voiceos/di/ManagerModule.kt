/**
 * ManagerModule.kt - Hilt Dependency Injection Module for Singleton Managers
 * Path: app/src/main/java/com/augmentalis/voiceos/di/ManagerModule.kt
 *
 * Author: Manoj Jhawar
 * Created: 2025-10-09
 *
 * Provides application-wide singleton manager dependencies for Hilt injection:
 * - CommandManager: Voice command processing and execution
 * - LocalizationModule: Multi-language support (42+ languages)
 * - LicensingModule: Subscription and license management
 * - HUDManager: Heads-up display management (future)
 *
 * All managers follow the getInstance() pattern for backward compatibility
 * but are now managed by Hilt for proper dependency injection.
 */

package com.augmentalis.voiceos.di

import android.content.Context
// import com.augmentalis.commandmanager.CommandManager  // DISABLED: Needs SQLDelight migration
import com.augmentalis.licensemanager.LicensingModule
import com.augmentalis.localizationmanager.LocalizationModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Manager Layer Hilt module
 *
 * This module provides application-wide singleton managers that handle:
 * - Voice command processing and execution (CommandManager)
 * - Multi-language support and translations (LocalizationModule)
 * - License validation and subscription management (LicensingModule)
 * - Future: HUD display management, device management, etc.
 *
 * Design Pattern:
 * - All managers use singleton pattern (getInstance)
 * - Hilt ensures single instance across entire app
 * - Managers are initialized on first access (lazy)
 * - Proper cleanup on app termination
 */
@Module
@InstallIn(SingletonComponent::class)
object ManagerModule {

    /**
     * TODO: Re-enable CommandManager when migrated to SQLDelight
     *
     * CommandManager needs migration for:
     * - LearningDatabase.kt (Room) → SQLDelight repositories
     * - PreferenceLearner.kt (already disabled)
     * - ContextSuggester.kt (references PreferenceLearner)
     * - DatabaseCommandResolver.kt (API mismatches with SQLDelight schema)
     *
     * CommandManager will handle:
     * - Voice command registration and execution
     * - Confidence-based command filtering
     * - Fuzzy matching for similar commands
     * - Action dispatching (navigation, volume, system)
     * - Command history and analytics
     * - Callback registration for user confirmation
     *
     * @Provides
     * @Singleton
     * fun provideCommandManager(
     *     @ApplicationContext context: Context
     * ): CommandManager {
     *     return CommandManager.getInstance(context)
     * }
     */

    /**
     * Provides LocalizationModule for multi-language support
     *
     * LocalizationModule handles:
     * - 42+ language support (Vivoka)
     * - 8 offline languages (Vosk)
     * - Dynamic language switching
     * - Translation management
     * - Language preference persistence
     * - HUD message localization
     * - Command localization
     *
     * Supported Languages:
     * - Western European: English, Spanish, French, German, Italian, Portuguese, Dutch
     * - Eastern European: Russian, Polish, Czech, Ukrainian, Bulgarian, Romanian, etc.
     * - Asian: Chinese, Japanese, Korean, Hindi, Thai
     * - Middle Eastern: Arabic, Hebrew, Turkish
     * - Nordic: Swedish, Norwegian, Danish, Finnish, Icelandic
     *
     * @param context Application context for preferences
     * @return LocalizationModule singleton instance
     */
    @Provides
    @Singleton
    fun provideLocalizationManager(
        @ApplicationContext context: Context
    ): LocalizationModule {
        return LocalizationModule.getInstance(context)
    }

    /**
     * Provides LicensingModule for subscription management
     *
     * LicensingModule handles:
     * - License validation (local and server)
     * - 30-day trial period management
     * - Premium subscription activation
     * - Enterprise license support
     * - License expiry monitoring
     * - Periodic validation (daily)
     * - Trial warning notifications (7 days before expiry)
     *
     * License Types:
     * - FREE: Basic features only
     * - TRIAL: 30-day full feature access
     * - PREMIUM: Annual subscription with all features
     * - ENTERPRISE: Perpetual license with priority support
     *
     * Features:
     * - Offline license caching
     * - Network requirement only for validation
     * - Graceful degradation on network failure
     * - User consent management
     *
     * @param context Application context for preferences
     * @return LicensingModule singleton instance
     */
    @Provides
    @Singleton
    fun provideLicenseManager(
        @ApplicationContext context: Context
    ): LicensingModule {
        return LicensingModule.getInstance(context)
    }

    // ========================================================================
    // Future Manager Providers
    // ========================================================================

    /**
     * TODO: Add HUDManager when implemented
     *
     * HUDManager will handle:
     * - Heads-up display overlay management
     * - Multi-mode support (Meeting, Driving, Workshop, Gaming, etc.)
     * - Notification display
     * - Status indicators
     * - Gaze target highlighting
     * - Context-aware information display
     *
     * @Provides
     * @Singleton
     * fun provideHUDManager(
     *     @ApplicationContext context: Context,
     *     localizationModule: LocalizationModule
     * ): HUDManager {
     *     return HUDManager.getInstance(context, localizationModule)
     * }
     */

    /**
     * TODO: Add DeviceManager when implemented
     *
     * DeviceManager will handle:
     * - Device capability detection
     * - Hardware feature queries
     * - RealWear device optimization
     * - Generic Android compatibility
     * - Sensor management
     * - Battery optimization
     *
     * @Provides
     * @Singleton
     * fun provideDeviceManager(
     *     @ApplicationContext context: Context
     * ): DeviceManager {
     *     return DeviceManager.getInstance(context)
     * }
     */

    /**
     * TODO: Add VoiceDataManager when refactored
     *
     * Note: VoiceDataManager currently uses DatabaseManager directly.
     * When it's refactored to be a proper singleton manager, add provider here.
     *
     * VoiceDataManager will handle:
     * - Voice data persistence coordination
     * - Command history management
     * - Analytics data aggregation
     * - Data export/import
     * - Privacy and retention policies
     *
     * @Provides
     * @Singleton
     * fun provideVoiceDataManager(
     *     @ApplicationContext context: Context,
     *     database: VoiceOSDatabase
     * ): VoiceDataManager {
     *     return VoiceDataManager.getInstance(context, database)
     * }
     */
}
