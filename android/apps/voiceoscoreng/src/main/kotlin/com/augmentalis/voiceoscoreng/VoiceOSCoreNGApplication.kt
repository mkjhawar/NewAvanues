package com.augmentalis.voiceoscoreng

import android.app.Application
import com.augmentalis.voiceoscoreng.app.BuildConfig
import com.augmentalis.voiceoscore.LearnAppDevToggle
import com.augmentalis.voiceoscore.LearnAppConfig
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.repositories.IScrapedElementRepository
import com.augmentalis.voiceoscore.ICommandPersistence
import com.augmentalis.voiceoscore.VivokaEngineFactory
import com.augmentalis.magiccode.plugins.integration.AndroidPluginSystemSetup
import com.augmentalis.magiccode.plugins.integration.PluginSystemConfig
import com.augmentalis.magiccode.plugins.integration.PluginSystemSetup
import com.augmentalis.voiceoscore.AppCategoryLoader
import com.augmentalis.voiceoscore.IAppCategoryProvider
import com.augmentalis.voiceoscore.IAppCategoryRepository
import com.augmentalis.voiceoscore.IAppPatternGroupRepository
import com.augmentalis.voiceoscore.PersistenceDecisionEngine
import com.augmentalis.voiceoscoreng.service.AndroidAppCategoryProvider
import com.augmentalis.voiceoscoreng.service.AndroidAssetReader
import com.augmentalis.voiceoscoreng.service.SQLDelightAppCategoryOverrideRepository
import com.augmentalis.voiceoscoreng.service.SQLDelightAppPatternGroupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class for VoiceOSCoreNG Test App.
 *
 * Initializes the VoiceOSCoreNG library with appropriate settings
 * and provides database access for command persistence.
 *
 * Phase 4: Uses reusable PluginSystemSetup from PluginSystem module.
 */
class VoiceOSCoreNGApplication : Application() {

    // =========================================================================
    // Application Scope
    // =========================================================================

    /**
     * Application-wide coroutine scope for background operations.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // =========================================================================
    // Database Layer
    // =========================================================================

    /**
     * Database manager singleton - provides access to all repositories.
     */
    lateinit var databaseManager: VoiceOSDatabaseManager
        private set

    /**
     * Command persistence layer - bridges VoiceOSCoreNG to SQLDelight.
     */
    lateinit var commandPersistence: ICommandPersistence
        private set

    /**
     * Generated command repository - direct database access.
     */
    val generatedCommandRepository: IGeneratedCommandRepository
        get() = databaseManager.generatedCommands

    /**
     * Scraped app repository - for FK integrity (must insert before elements/commands).
     */
    val scrapedAppRepository: IScrapedAppRepository
        get() = databaseManager.scrapedApps

    /**
     * Scraped element repository - for FK integrity (must insert before commands).
     */
    val scrapedElementRepository: IScrapedElementRepository
        get() = databaseManager.scrapedElements

    // =========================================================================
    // App Category System (Hybrid Persistence)
    // =========================================================================

    /**
     * Repository for app category overrides loaded from ACD files.
     */
    val appCategoryRepository: IAppCategoryRepository by lazy {
        SQLDelightAppCategoryOverrideRepository(databaseManager.getDatabase())
    }

    /**
     * Repository for app pattern groups (fallback matching).
     */
    val appPatternGroupRepository: IAppPatternGroupRepository by lazy {
        SQLDelightAppPatternGroupRepository(databaseManager.getDatabase())
    }

    /**
     * App category provider with full hybrid 4-layer support.
     * Use this instead of creating AndroidAppCategoryProvider directly.
     */
    val appCategoryProvider: IAppCategoryProvider by lazy {
        AndroidAppCategoryProvider.withDatabase(
            this,
            appCategoryRepository,
            appPatternGroupRepository
        )
    }

    /**
     * Flag indicating if ACD file has been loaded.
     */
    @Volatile
    var isAcdLoaded: Boolean = false
        private set

    // =========================================================================
    // Universal Plugin Architecture (Phase 4)
    // =========================================================================

    /**
     * Plugin system setup - provides access to plugin host, dispatcher, etc.
     * Uses the reusable PluginSystemSetup from the PluginSystem module.
     */
    lateinit var pluginSetup: AndroidPluginSystemSetup
        private set

    /**
     * Flag indicating if plugin system is fully initialized.
     */
    val isPluginSystemReady: Boolean
        get() = pluginSetup.isInitialized.value

    /**
     * Convenience access to plugin host.
     */
    val pluginHost get() = pluginSetup.androidPluginHost

    /**
     * Convenience access to command dispatcher.
     */
    val pluginCommandDispatcher get() = pluginSetup.pluginCommandDispatcher

    /**
     * Convenience access to handler bridge.
     */
    val pluginHandlerBridge get() = pluginSetup.handlerBridge

    override fun onCreate() {
        super.onCreate()

        // Initialize database
        val driverFactory = DatabaseDriverFactory(this)
        databaseManager = VoiceOSDatabaseManager.getInstance(driverFactory)

        // Create command persistence bridge
        commandPersistence = AndroidCommandPersistence(databaseManager.generatedCommands)

        // Initialize Vivoka engine factory with application context
        VivokaEngineFactory.initialize(this)

        // Initialize LearnApp feature toggles
        LearnAppDevToggle.initialize(
            tier = LearnAppDevToggle.Tier.LITE,
            isDebug = BuildConfig.DEBUG
        )

        // Enable test mode if configured
        if (BuildConfig.ENABLE_TEST_MODE) {
            LearnAppConfig.enableTestMode()
        }

        android.util.Log.d(TAG, "Database initialized: voiceos.db")

        // =====================================================================
        // Initialize App Category System (Hybrid Persistence)
        // Load known-apps.acd from assets into SQLite
        // =====================================================================
        initializeAppCategorySystem()

        // =====================================================================
        // Phase 4: Initialize Universal Plugin Architecture
        // Uses reusable PluginSystemSetup from PluginSystem module
        // =====================================================================
        initializePluginSystem()
    }

    /**
     * Initialize the App Category System.
     *
     * Loads the known-apps.acd file from assets into SQLite for
     * hybrid 4-layer app category classification.
     */
    private fun initializeAppCategorySystem() {
        android.util.Log.i(TAG, "Initializing App Category System...")

        applicationScope.launch {
            try {
                val assetReader = AndroidAssetReader(this@VoiceOSCoreNGApplication)
                val loader = AppCategoryLoader(
                    assetReader,
                    appCategoryRepository,
                    appPatternGroupRepository
                )

                val result = loader.loadFromAssets()

                if (result.success) {
                    isAcdLoaded = true
                    // Wire provider into PersistenceDecisionEngine for hybrid classification
                    PersistenceDecisionEngine.appCategoryProvider = appCategoryProvider
                    if (result.entriesLoaded > 0) {
                        android.util.Log.i(TAG, "ACD loaded: ${result.entriesLoaded} apps, ${result.patternsLoaded} patterns (v${result.version})")
                    } else {
                        android.util.Log.d(TAG, "ACD skipped: ${result.error}")
                    }
                } else {
                    android.util.Log.e(TAG, "ACD load failed: ${result.error}")
                    // Still wire provider even without ACD - will use L2-L4 layers
                    PersistenceDecisionEngine.appCategoryProvider = appCategoryProvider
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "ACD initialization error", e)
            }
        }
    }

    /**
     * Initialize the Universal Plugin Architecture using PluginSystemSetup.
     */
    private fun initializePluginSystem() {
        android.util.Log.i(TAG, "Initializing Universal Plugin Architecture...")

        // Create platform-specific setup
        pluginSetup = PluginSystemSetup.create(this) as AndroidPluginSystemSetup

        // Initialize asynchronously
        applicationScope.launch {
            val result = pluginSetup.initialize(
                PluginSystemConfig(
                    debugMode = BuildConfig.DEBUG,
                    enablePerformanceMonitoring = true,
                    enableHotReload = false,
                    minHandlerConfidence = 0.7f,
                    registerBuiltinPlugins = true
                )
            )

            if (result.success) {
                android.util.Log.i(TAG, "Plugin system: ${result.message}")
            } else {
                android.util.Log.e(TAG, "Plugin system failed: ${result.message}")
                result.errors.forEach { error ->
                    android.util.Log.e(TAG, "  Error: $error")
                }
            }
        }
    }

    /**
     * Notify plugin system that AccessibilityService is connected.
     *
     * Called by VoiceOSAccessibilityService when it connects.
     * This allows plugins to access the service for UI operations.
     *
     * @param service The connected AccessibilityService
     */
    fun onAccessibilityServiceConnected(service: android.accessibilityservice.AccessibilityService) {
        android.util.Log.i(TAG, "AccessibilityService connected, notifying plugin system")
        applicationScope.launch {
            pluginSetup.onServiceConnected(service)
        }
    }

    /**
     * Notify plugin system that AccessibilityService is disconnected.
     *
     * Called by VoiceOSAccessibilityService when it disconnects.
     */
    fun onAccessibilityServiceDisconnected() {
        android.util.Log.i(TAG, "AccessibilityService disconnected, notifying plugin system")
        applicationScope.launch {
            pluginSetup.onServiceDisconnected()
        }
    }

    companion object {
        private const val TAG = "VoiceOSCoreNGApp"

        /**
         * Get the Application instance from any context.
         */
        fun getInstance(context: android.content.Context): VoiceOSCoreNGApplication {
            return context.applicationContext as VoiceOSCoreNGApplication
        }
    }
}
