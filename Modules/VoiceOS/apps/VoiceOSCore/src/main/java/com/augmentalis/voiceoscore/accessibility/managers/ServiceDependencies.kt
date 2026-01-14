/**
 * ServiceDependencies.kt - Dependency injection interface for VoiceOSService
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (IDEACODE v12.1)
 * Created: 2025-12-27
 *
 * Provides dependency abstraction for testability. Since AccessibilityService
 * cannot use @AndroidEntryPoint, this interface enables manual DI with the
 * ability to swap implementations for testing.
 */

package com.augmentalis.voiceoscore.accessibility.managers

import android.content.Context
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine
import com.augmentalis.voiceoscore.accessibility.overlays.OverlayManager
import com.augmentalis.voiceoscore.accessibility.speech.SpeechEngineManager
import com.augmentalis.voiceoscore.accessibility.VoiceOSService
import com.augmentalis.voiceoscore.version.AppVersionDetector
import com.augmentalis.voiceoscore.version.AppVersionManager

/**
 * Service Dependencies Interface
 *
 * Abstracts core dependencies required by VoiceOSService:
 * - Database management
 * - Inter-process communication
 * - Lifecycle coordination
 * - UI scraping
 * - Speech recognition
 * - Overlay management
 *
 * Implementations:
 * - [ProductionServiceDependencies]: Real dependencies for production
 * - Test implementations can mock individual components
 */
interface IServiceDependencies {

    /** Database lifecycle manager (SQLDelight + scraping adapter) */
    val dbManager: DatabaseManager

    /** Speech recognition engine manager */
    val speechEngineManager: SpeechEngineManager

    /** UI element extraction engine */
    val uiScrapingEngine: UIScrapingEngine

    /** Inter-process communication handler */
    val ipcManager: IPCManager

    /** Service lifecycle and foreground service coordinator */
    val lifecycleCoordinator: LifecycleCoordinator

    /** Overlay display manager */
    val overlayManager: OverlayManager

    /** Installed applications manager */
    val installedAppsManager: InstalledAppsManager

    /** App version detection */
    val appVersionDetector: AppVersionDetector

    /** App version management with command invalidation */
    val appVersionManager: AppVersionManager

    /**
     * Initialize all dependencies
     * Called during service startup
     */
    fun initialize()

    /**
     * Cleanup all dependencies
     * Called during service shutdown
     */
    fun cleanup()
}

/**
 * Production Service Dependencies
 *
 * Real implementation of [IServiceDependencies] for production use.
 * Uses lazy initialization for efficient resource usage.
 *
 * @param service The VoiceOSService instance
 * @param isServiceReady Supplier function to check service readiness
 */
class ProductionServiceDependencies(
    private val service: VoiceOSService,
    private val isServiceReady: () -> Boolean
) : IServiceDependencies {

    companion object {
        private const val TAG = "ServiceDependencies"
    }

    private val context: Context
        get() = service.applicationContext

    override val dbManager: DatabaseManager by lazy {
        DatabaseManager(context).also {
            android.util.Log.d(TAG, "DatabaseManager initialized (lazy)")
        }
    }

    override val speechEngineManager: SpeechEngineManager by lazy {
        SpeechEngineManager(context).also {
            android.util.Log.d(TAG, "SpeechEngineManager initialized (lazy)")
        }
    }

    override val uiScrapingEngine: UIScrapingEngine by lazy {
        UIScrapingEngine(service).also {
            android.util.Log.d(TAG, "UIScrapingEngine initialized (lazy)")
        }
    }

    override val ipcManager: IPCManager by lazy {
        IPCManager(
            accessibilityService = service,
            speechEngineManager = speechEngineManager,
            uiScrapingEngine = uiScrapingEngine,
            databaseManager = dbManager,
            isServiceReady = isServiceReady
        ).also {
            android.util.Log.d(TAG, "IPCManager initialized (lazy)")
        }
    }

    override val lifecycleCoordinator: LifecycleCoordinator by lazy {
        LifecycleCoordinator(service).also {
            android.util.Log.d(TAG, "LifecycleCoordinator initialized (lazy)")
        }
    }

    override val overlayManager: OverlayManager by lazy {
        OverlayManager.getInstance(service).also {
            android.util.Log.d(TAG, "OverlayManager initialized (lazy)")
        }
    }

    override val installedAppsManager: InstalledAppsManager by lazy {
        InstalledAppsManager(context).also {
            android.util.Log.d(TAG, "InstalledAppsManager initialized (lazy)")
        }
    }

    override val appVersionDetector: AppVersionDetector by lazy {
        AppVersionDetector(context, dbManager.sqlDelightManager.appVersions).also {
            android.util.Log.d(TAG, "AppVersionDetector initialized (lazy)")
        }
    }

    override val appVersionManager: AppVersionManager by lazy {
        AppVersionManager(
            context = context,
            detector = appVersionDetector,
            versionRepo = dbManager.sqlDelightManager.appVersions,
            commandRepo = dbManager.sqlDelightManager.generatedCommands
        ).also {
            android.util.Log.d(TAG, "AppVersionManager initialized (lazy)")
        }
    }

    override fun initialize() {
        android.util.Log.d(TAG, "Initializing production dependencies...")
        // Dependencies are lazy-initialized on first access
        // This method can be used for eager initialization if needed
    }

    override fun cleanup() {
        android.util.Log.d(TAG, "Cleaning up production dependencies...")
        lifecycleCoordinator.unregister()
        speechEngineManager.cleanup()
        dbManager.cleanup()
    }
}

/**
 * Factory for creating service dependencies
 *
 * Allows injection of custom dependencies for testing
 */
object ServiceDependenciesFactory {

    @Volatile
    private var testDependencies: IServiceDependencies? = null

    /**
     * Create dependencies for a service
     *
     * @param service The VoiceOSService instance
     * @param isServiceReady Supplier function to check service readiness
     * @return Dependencies instance (test or production)
     */
    fun create(
        service: VoiceOSService,
        isServiceReady: () -> Boolean
    ): IServiceDependencies {
        return testDependencies ?: ProductionServiceDependencies(service, isServiceReady)
    }

    /**
     * Set test dependencies (for unit tests only)
     *
     * @param dependencies Mock dependencies or null to use production
     */
    @JvmStatic
    fun setTestDependencies(dependencies: IServiceDependencies?) {
        testDependencies = dependencies
    }

    /**
     * Reset to production dependencies (call in test teardown)
     */
    @JvmStatic
    fun reset() {
        testDependencies = null
    }
}
