/**
 * PluginSystemSetup.android.kt - Android implementation of plugin system setup
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Android-specific plugin system integration. Handles:
 * - AndroidPluginHost initialization
 * - AccessibilityService registration
 * - Built-in handler plugin registration with Android executors
 */
package com.augmentalis.magiccode.plugins.integration

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import com.augmentalis.magiccode.plugins.android.*
import com.augmentalis.magiccode.plugins.android.executors.*
import com.augmentalis.magiccode.plugins.builtin.*
import com.augmentalis.magiccode.plugins.security.DefaultPluginSandbox
import com.augmentalis.magiccode.plugins.security.PermissionStorage
import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.HandlerContext
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.ScreenContext
import com.augmentalis.voiceoscore.ActionResult
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.CommandActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "PluginSystemSetup"

/**
 * Android implementation of IPluginSystemSetup.
 *
 * Provides complete plugin system integration for Android apps.
 *
 * ## Usage in Application.onCreate()
 * ```kotlin
 * class MyApplication : Application() {
 *     lateinit var pluginSetup: AndroidPluginSystemSetup
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *
 *         pluginSetup = PluginSystemSetup.create(this) as AndroidPluginSystemSetup
 *
 *         lifecycleScope.launch {
 *             val result = pluginSetup.initialize(PluginSystemConfig(
 *                 debugMode = BuildConfig.DEBUG
 *             ))
 *             Log.i("App", "Plugin system: ${result.message}")
 *         }
 *     }
 * }
 * ```
 *
 * ## Usage in AccessibilityService
 * ```kotlin
 * override fun onServiceConnected() {
 *     super.onServiceConnected()
 *     (application as MyApplication).pluginSetup.onServiceConnected(this)
 * }
 *
 * override fun onDestroy() {
 *     (application as MyApplication).pluginSetup.onServiceDisconnected()
 *     super.onDestroy()
 * }
 * ```
 */
class AndroidPluginSystemSetup(
    private val context: Context
) : IPluginSystemSetup {

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var _pluginHost: AndroidPluginHost? = null
    override val pluginHost: IPluginHost<*>?
        get() = _pluginHost

    private var _commandDispatcher: AndroidCommandDispatcher? = null
    override val commandDispatcher: ICommandDispatcher?
        get() = _commandDispatcher

    private var _performanceMonitor: PluginPerformanceMonitor? = null
    override val performanceMonitor: PluginPerformanceMonitor?
        get() = _performanceMonitor

    // Direct access to typed components
    val androidPluginHost: AndroidPluginHost?
        get() = _pluginHost

    val pluginCommandDispatcher: PluginCommandDispatcher?
        get() = _commandDispatcher?.pluginDispatcher

    val handlerBridge: PluginHandlerBridge?
        get() = _commandDispatcher?.handlerBridge

    private var config: PluginSystemConfig? = null
    private val customFactories = mutableMapOf<String, () -> Plugin>()

    /**
     * Plugin sandbox backed by encrypted [PermissionStorage].
     *
     * Created lazily on first access so callers that never use the sandbox
     * don't pay the EncryptedSharedPreferences initialization cost.
     * Permissions granted via this sandbox survive app restarts.
     */
    val pluginSandbox: DefaultPluginSandbox by lazy {
        val storage = try {
            PermissionStorage.create(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create PermissionStorage — sandbox will run without persistence", e)
            null
        }
        DefaultPluginSandbox(storage = storage)
    }

    override suspend fun initialize(config: PluginSystemConfig): PluginSystemSetupResult {
        if (_isInitialized.value) {
            return PluginSystemSetupResult(
                success = true,
                message = "Already initialized",
                pluginsLoaded = _pluginHost?.getLoadedPlugins()?.size ?: 0
            )
        }

        this.config = config
        val errors = mutableListOf<String>()

        try {
            Log.i(TAG, "Initializing plugin system...")

            // Create plugin host
            _pluginHost = AndroidPluginHost(context)
            val host = _pluginHost!!

            // Register built-in plugins if enabled
            if (config.registerBuiltinPlugins) {
                registerBuiltinHandlerPlugins(host)
            }

            // Register custom plugins
            customFactories.forEach { (id, factory) ->
                host.registerBuiltinPluginFactory(id) { factory() }
            }
            config.customPluginFactories.forEach { (id, factory) ->
                host.registerBuiltinPluginFactory(id) { factory() }
            }

            // Initialize plugins
            host.registerBuiltinPlugins()

            // Create command dispatcher
            val pluginDispatcher = PluginCommandDispatcher(host)
            val handlerBridge = PluginHandlerBridgeFactory.createDefault(
                pluginDispatcher = pluginDispatcher,
                legacyDispatcher = null // No legacy fallback by default
            ).apply {
                minPluginConfidence = config.minHandlerConfidence
                debugLogging = config.debugMode
            }

            _commandDispatcher = AndroidCommandDispatcher(pluginDispatcher, handlerBridge)

            // Setup performance monitoring
            if (config.enablePerformanceMonitoring) {
                _performanceMonitor = PluginPerformanceMonitor.initialize()
                _performanceMonitor?.updatePluginCounts(
                    total = host.getLoadedPlugins().size,
                    active = host.getLoadedPlugins().count { it.state == PluginState.ACTIVE }
                )
            }

            _isInitialized.value = true
            val pluginCount = host.getLoadedPlugins().size

            Log.i(TAG, "Plugin system initialized: $pluginCount plugins loaded")

            return PluginSystemSetupResult(
                success = true,
                message = "Plugin system initialized with $pluginCount plugins",
                pluginsLoaded = pluginCount,
                errors = errors
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize plugin system", e)
            errors.add(e.message ?: "Unknown error")
            return PluginSystemSetupResult(
                success = false,
                message = "Initialization failed: ${e.message}",
                errors = errors
            )
        }
    }

    override suspend fun shutdown() {
        Log.i(TAG, "Shutting down plugin system...")

        _pluginHost?.let { host ->
            host.getLoadedPlugins().forEach { plugin ->
                try {
                    plugin.shutdown()
                } catch (e: Exception) {
                    Log.w(TAG, "Error shutting down plugin ${plugin.pluginId}", e)
                }
            }
        }

        _pluginHost = null
        _commandDispatcher = null
        _performanceMonitor = null
        _isInitialized.value = false

        Log.i(TAG, "Plugin system shutdown complete")
    }

    override fun registerPluginFactory(pluginId: String, factory: () -> Plugin) {
        if (_isInitialized.value) {
            _pluginHost?.registerBuiltinPluginFactory(pluginId) { factory() }
        } else {
            customFactories[pluginId] = factory
        }
    }

    override fun getPlugin(pluginId: String): Plugin? {
        return _pluginHost?.getPlugin(pluginId)
    }

    override fun getLoadedPlugins(): List<Plugin> {
        return _pluginHost?.getLoadedPlugins() ?: emptyList()
    }

    /**
     * Notify plugin system that AccessibilityService is connected.
     *
     * Call this from AccessibilityService.onServiceConnected().
     *
     * @param service The connected AccessibilityService
     */
    suspend fun onServiceConnected(service: AccessibilityService) {
        Log.i(TAG, "AccessibilityService connected")
        _pluginHost?.onServiceConnected(service)
    }

    /**
     * Notify plugin system that AccessibilityService is disconnected.
     *
     * Call this from AccessibilityService.onDestroy().
     */
    suspend fun onServiceDisconnected() {
        Log.i(TAG, "AccessibilityService disconnected")
        _pluginHost?.onServiceDisconnected()
    }

    /**
     * Register all built-in handler plugins with Android executors.
     */
    private fun registerBuiltinHandlerPlugins(host: AndroidPluginHost) {
        val serviceRegistry = host.serviceRegistry

        // Navigation Handler Plugin
        host.registerBuiltinPluginFactory(NavigationHandlerPlugin.PLUGIN_ID) {
            NavigationHandlerPlugin {
                AndroidNavigationExecutor(serviceRegistry)
            }
        }

        // UI Interaction Handler Plugin
        host.registerBuiltinPluginFactory(UIInteractionPlugin.PLUGIN_ID) {
            UIInteractionPlugin {
                AndroidUIInteractionExecutor(serviceRegistry)
            }
        }

        // Text Input Handler Plugin
        host.registerBuiltinPluginFactory(TextInputPlugin.PLUGIN_ID) {
            TextInputPlugin {
                AndroidTextInputExecutor(serviceRegistry)
            }
        }

        // System Command Handler Plugin
        host.registerBuiltinPluginFactory(SystemCommandPlugin.PLUGIN_ID) {
            SystemCommandPlugin {
                AndroidSystemCommandExecutor(serviceRegistry)
            }
        }

        // Gesture Handler Plugin
        host.registerBuiltinPluginFactory(GesturePlugin.PLUGIN_ID) {
            GesturePlugin {
                AndroidGestureExecutor(serviceRegistry)
            }
        }

        // Selection Handler Plugin
        host.registerBuiltinPluginFactory(SelectionPlugin.PLUGIN_ID) {
            SelectionPlugin(
                clipboardProvider = { AndroidClipboardProvider(serviceRegistry) },
                selectionExecutor = { AndroidSelectionExecutor(serviceRegistry) }
            )
        }

        // App Launcher Handler Plugin
        host.registerBuiltinPluginFactory(AppLauncherPlugin.PLUGIN_ID) {
            AppLauncherPlugin {
                AndroidAppLauncherExecutor(serviceRegistry)
            }
        }

        Log.i(TAG, "Registered ${BuiltinPluginRegistration.PLUGIN_IDS.size} built-in handler plugins")
    }
}

/**
 * Android command dispatcher wrapping plugin dispatcher and handler bridge.
 */
class AndroidCommandDispatcher(
    val pluginDispatcher: PluginCommandDispatcher,
    val handlerBridge: PluginHandlerBridge
) : ICommandDispatcher {

    override suspend fun dispatch(command: String, context: Any?): DispatchResult {
        val quantizedCommand = QuantizedCommand(
            avid = "",
            phrase = command,
            actionType = CommandActionType.CLICK,
            targetAvid = null,
            confidence = 1.0f
        )

        val handlerContext = when (context) {
            is HandlerContext -> context
            is String -> HandlerContext(
                currentScreen = ScreenContext(
                    packageName = context,
                    activityName = "unknown",
                    screenTitle = null,
                    elementCount = 0,
                    primaryAction = null
                ),
                elements = emptyList(),
                previousCommand = null,
                userPreferences = emptyMap()
            )
            else -> HandlerContext(
                currentScreen = ScreenContext(
                    packageName = "unknown",
                    activityName = "unknown",
                    screenTitle = null,
                    elementCount = 0,
                    primaryAction = null
                ),
                elements = emptyList(),
                previousCommand = null,
                userPreferences = emptyMap()
            )
        }

        return when (val result = handlerBridge.dispatch(quantizedCommand, handlerContext)) {
            is ActionResult.Success -> DispatchResult.Success(result.message)
            is ActionResult.Error -> DispatchResult.Error(result.message)
            is ActionResult.Ambiguous -> DispatchResult.Ambiguous(result.candidates)
            else -> DispatchResult.Error(result.message)
        }
    }
}

/**
 * Factory implementation for Android.
 */
actual object PluginSystemSetup {
    actual fun create(platformContext: Any): IPluginSystemSetup {
        require(platformContext is Context) {
            "Android requires Context for PluginSystemSetup"
        }
        return AndroidPluginSystemSetup(platformContext)
    }
}
