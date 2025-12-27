/**
 * AccessibilityModule.kt - Dependency Injection notes for VoiceOSService
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 * Updated: 2025-12-27 - Removed Hilt annotations (Hilt doesn't support AccessibilityService)
 *
 * NOTE: Hilt/Dagger cannot be used with AccessibilityService because:
 * 1. AccessibilityService is instantiated by the system, not by Hilt
 * 2. @AndroidEntryPoint doesn't work with AccessibilityService
 * 3. ServiceComponent requires @AndroidEntryPoint on the service
 *
 * SOLUTION: Manual DI via ServiceDependencies.kt
 * - IServiceDependencies interface defines all dependencies
 * - ProductionServiceDependencies provides lazy-initialized implementations
 * - ServiceDependenciesFactory allows test dependency injection
 *
 * See: com.augmentalis.voiceoscore.accessibility.managers.ServiceDependencies
 */

package com.augmentalis.voiceoscore.accessibility.di

/**
 * This file is kept for documentation purposes.
 *
 * All dependency injection for VoiceOSService is handled by:
 * - ServiceDependencies.kt (interface + production implementation)
 * - ServiceDependenciesFactory (factory with test injection support)
 *
 * Dependencies provided:
 * - DatabaseManager: SQLDelight database access
 * - SpeechEngineManager: Voice recognition engine management
 * - UIScrapingEngine: Accessibility tree scraping
 * - IPCManager: Inter-process communication
 * - LifecycleCoordinator: Service lifecycle management
 * - OverlayManager: UI overlay display
 * - InstalledAppsManager: App discovery and command generation
 * - AppVersionDetector: App version tracking
 * - AppVersionManager: Version-aware command invalidation
 */
object AccessibilityModule {
    // Placeholder - actual DI is in ServiceDependencies.kt
}
