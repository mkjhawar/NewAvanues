/**
 * ServiceDependencies.kt - Dependency injection interface for VoiceOSService
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (IDEACODE v18)
 * Created: 2025-12-27
 * Updated: 2026-01-15 - Added EventRouter, IntegrationCoordinator, CommandDispatcher (P2-8e)
 *
 * Provides dependency abstraction for testability. Since AccessibilityService
 * cannot use @AndroidEntryPoint, this interface enables manual DI with the
 * ability to swap implementations for testing.
 */

package com.augmentalis.voiceoscore.accessibility.managers

import android.content.Context
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine.UIElement
import com.augmentalis.voiceoscore.accessibility.overlays.OverlayManager
import com.augmentalis.voiceoscore.accessibility.speech.SpeechEngineManager
import com.augmentalis.voiceoscore.accessibility.utils.EventPriorityManager
import com.augmentalis.voiceoscore.accessibility.VoiceOSService
import com.augmentalis.voiceoscore.version.AppVersionDetector
import com.augmentalis.voiceoscore.version.AppVersionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock

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
 * - Event routing (P2-8e)
 * - Integration coordination (P2-8e)
 * - Command dispatch (P2-8e)
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

    /** Event routing and prioritization (P2-8e) */
    val eventRouter: EventRouter

    /** Integration lifecycle management (P2-8e) */
    val integrationCoordinator: IntegrationCoordinator

    /** Voice command dispatch (P2-8e) */
    val commandDispatcher: CommandDispatcher

    /** Action coordinator for legacy command handling */
    val actionCoordinator: ActionCoordinator

    /** Event priority manager */
    val eventPriorityManager: EventPriorityManager

    /** Cache lock for thread-safe cache access */
    val cacheLock: ReentrantReadWriteLock

    /** Node cache for UI elements */
    val nodeCache: MutableList<UIElement>

    /** Command cache for normalized command text */
    val commandCache: MutableList<String>

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

    // Coroutine scopes for managers
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val commandScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Service configuration suppliers
    private var isLowResourceMode: () -> Boolean = { false }
    private var verboseLogging: () -> Boolean = { false }

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

    override val eventPriorityManager: EventPriorityManager by lazy {
        EventPriorityManager().also {
            android.util.Log.d(TAG, "EventPriorityManager initialized (lazy)")
        }
    }

    override val cacheLock: ReentrantReadWriteLock by lazy {
        ReentrantReadWriteLock().also {
            android.util.Log.d(TAG, "CacheLock initialized (lazy)")
        }
    }

    override val nodeCache: MutableList<UIElement> by lazy {
        CopyOnWriteArrayList<UIElement>().also {
            android.util.Log.d(TAG, "NodeCache initialized (lazy)")
        }
    }

    override val commandCache: MutableList<String> by lazy {
        CopyOnWriteArrayList<String>().also {
            android.util.Log.d(TAG, "CommandCache initialized (lazy)")
        }
    }

    override val actionCoordinator: ActionCoordinator by lazy {
        ActionCoordinator(service).also {
            android.util.Log.d(TAG, "ActionCoordinator initialized (lazy)")
        }
    }

    override val integrationCoordinator: IntegrationCoordinator by lazy {
        IntegrationCoordinator(
            context = context,
            accessibilityService = service,
            serviceScope = serviceScope,
            databaseManager = dbManager
        ).also {
            android.util.Log.d(TAG, "IntegrationCoordinator initialized (lazy)")
        }
    }

    override val commandDispatcher: CommandDispatcher by lazy {
        CommandDispatcher(
            context = context,
            accessibilityService = service,
            serviceScope = serviceScope,
            actionCoordinator = actionCoordinator,
            integrationCoordinator = integrationCoordinator,
            rootNodeProvider = { service.rootInActiveWindow },
            commandCacheProvider = { commandCache },
            nodeCacheProvider = { nodeCache }
        ).also {
            android.util.Log.d(TAG, "CommandDispatcher initialized (lazy)")
        }
    }

    override val eventRouter: EventRouter by lazy {
        EventRouter(
            serviceScope = serviceScope,
            commandScope = commandScope,
            uiScrapingEngine = uiScrapingEngine,
            eventPriorityManager = eventPriorityManager,
            cacheLock = cacheLock,
            nodeCache = nodeCache,
            commandCache = commandCache,
            isServiceReady = isServiceReady,
            isLowResourceMode = isLowResourceMode,
            verboseLogging = verboseLogging,
            rootNodeProvider = { service.rootInActiveWindow },
            contextProvider = { context }
        ).also {
            android.util.Log.d(TAG, "EventRouter initialized (lazy)")
        }
    }

    /**
     * Configure service state suppliers
     * Called during service initialization to wire up dynamic state
     */
    fun configureStateSuppliers(
        lowResourceMode: () -> Boolean,
        verbose: () -> Boolean
    ) {
        isLowResourceMode = lowResourceMode
        verboseLogging = verbose
    }

    override fun initialize() {
        android.util.Log.d(TAG, "Initializing production dependencies...")
        // Dependencies are lazy-initialized on first access
        // This method can be used for eager initialization if needed
    }

    override fun cleanup() {
        android.util.Log.d(TAG, "Cleaning up production dependencies...")

        // Cleanup new managers (P2-8e)
        eventRouter.cleanup()
        commandDispatcher.cleanup()
        integrationCoordinator.cleanup()

        // Cleanup existing managers
        lifecycleCoordinator.unregister()
        speechEngineManager.cleanup()
        dbManager.cleanup()

        android.util.Log.i(TAG, "All dependencies cleaned up")
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
