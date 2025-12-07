/**
 * AccessibilityModule.kt - Hilt Dependency Injection Module for VoiceAccessibility Service
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */

package com.augmentalis.voiceoscore.accessibility.di

import android.content.Context
import com.augmentalis.voiceoscore.accessibility.managers.InstalledAppsManager
import com.augmentalis.voiceoscore.accessibility.speech.SpeechEngineManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

/**
 * Accessibility Service Hilt module
 *
 * This module provides dependencies specific to the VoiceOSService accessibility service.
 * All dependencies are scoped to ServiceScoped to ensure they live as long as the service.
 *
 * Key differences from application-level modules:
 * - Uses ServiceComponent instead of SingletonComponent
 * - Uses ServiceScoped instead of Singleton
 * - Provides service-specific components (UI scraping, speech engine, etc.)
 */
@Module
@InstallIn(ServiceComponent::class)
object AccessibilityModule {

    // Note: UIScrapingEngine is NOT provided by Hilt because it requires the AccessibilityService
    // instance (not just Context). It remains lazy-initialized in VoiceOSService.
    // UIScrapingEngine(service: AccessibilityService) - initialized with service reference

    /**
     * Provides SpeechEngineManager for managing speech recognition engines
     *
     * TEMPORARY: Commented out for standalone testing.
     * SpeechEngineManager requires Vivoka/Vosk/Whisper SDK dependencies
     * that are only available in host applications, not standalone VoiceOSCore.
     *
     * SpeechEngineManager handles:
     * - Initialization of speech engines (Vivoka, Vosk, etc.)
     * - Engine lifecycle management
     * - Speech recognition state management
     * - Command registration and updates
     * - Speech event dispatching
     *
     * @param context Application context for engine initialization
     * @return SpeechEngineManager instance scoped to service lifecycle
     */
     @Provides
     @ServiceScoped
     fun provideSpeechEngineManager(
         @ApplicationContext context: Context
     ): SpeechEngineManager {
         return SpeechEngineManager(context)
     }

    /**
     * Provides InstalledAppsManager for app command registration
     *
     * InstalledAppsManager is responsible for:
     * - Detecting installed applications
     * - Generating voice commands for apps ("open [app name]")
     * - Monitoring app installation/uninstallation
     * - Caching app information for performance
     *
     * @param context Application context for package manager access
     * @return InstalledAppsManager instance scoped to service lifecycle
     */
    @Provides
    @ServiceScoped
    fun provideInstalledAppsManager(
        @ApplicationContext context: Context
    ): InstalledAppsManager {
        return InstalledAppsManager(context)
    }

    // Note: ActionCoordinator is NOT provided by Hilt because it requires the VoiceOSService
    // instance for gesture dispatch. It remains lazy-initialized in VoiceOSService.
    // ActionCoordinator(service: VoiceOSService) - initialized with service reference

    // TODO: Add future accessibility-related providers
    // - GestureHandler for touch gesture recognition
    // - VoiceCommandProcessor for command parsing
    // - AccessibilityEventFilter for event optimization
    // - CommandCacheManager for performance optimization
    // - ActionCoordinator factory if refactored to separate gesture dispatch
}
