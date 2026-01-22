/**
 * AndroidPluginHost.kt - Main Android plugin host implementation
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * The AndroidPluginHost is the central component for managing plugins on Android.
 * It handles plugin loading, lifecycle management, Android lifecycle integration,
 * and provides access to the plugin registry and event bus.
 */
package com.augmentalis.magiccode.plugins.android

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.augmentalis.magiccode.plugins.core.PluginLog
import com.augmentalis.magiccode.plugins.core.PluginLogger
import com.augmentalis.magiccode.plugins.core.ConsolePluginLogger
import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.universalrpc.ServiceEndpoint
import com.augmentalis.universalrpc.ServiceRegistry as RpcServiceRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * State of the AndroidPluginHost.
 */
enum class HostState {
    /** Host is not initialized */
    UNINITIALIZED,
    /** Host is initializing */
    INITIALIZING,
    /** Host is ready to load plugins */
    READY,
    /** Host is shutting down */
    SHUTTING_DOWN,
    /** Host is shut down */
    SHUTDOWN,
    /** Host encountered an error */
    ERROR
}

/**
 * Main plugin host that loads and manages plugins on Android.
 *
 * The AndroidPluginHost serves as the primary entry point for the plugin system
 * on Android. It integrates with Android's Activity and Service lifecycles,
 * manages plugin loading/unloading, and coordinates plugin state transitions.
 *
 * ## Features
 * - Plugin loading and unloading with lifecycle management
 * - Android Activity/Service lifecycle integration
 * - Capability-based plugin discovery
 * - Built-in plugin registration for first-party plugins
 * - Event bus for inter-plugin communication
 * - Service connection management for plugins
 *
 * ## Initialization
 * ```kotlin
 * // In Application class
 * class MyApplication : Application() {
 *     lateinit var pluginHost: AndroidPluginHost
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *
 *         val rpcRegistry = ServiceRegistry()
 *         val eventBus = GrpcPluginEventBus()
 *         val serviceRegistry = ServiceRegistry()
 *
 *         pluginHost = AndroidPluginHost(
 *             context = this,
 *             serviceRegistry = serviceRegistry,
 *             eventBus = eventBus,
 *             rpcServiceRegistry = rpcRegistry
 *         )
 *
 *         lifecycleScope.launch {
 *             pluginHost.initialize()
 *             pluginHost.registerBuiltinPlugins()
 *         }
 *     }
 * }
 * ```
 *
 * ## AccessibilityService Integration
 * ```kotlin
 * class MyAccessibilityService : AccessibilityService() {
 *     override fun onServiceConnected() {
 *         super.onServiceConnected()
 *         (application as MyApplication).pluginHost.onServiceConnected(this)
 *     }
 *
 *     override fun onDestroy() {
 *         (application as MyApplication).pluginHost.onServiceDisconnected()
 *         super.onDestroy()
 *     }
 * }
 * ```
 *
 * @param context Android Context (preferably Application context)
 * @param serviceRegistry Platform service registry for dependency injection
 * @param eventBus Plugin event bus for inter-plugin communication
 * @param rpcServiceRegistry UniversalRPC service registry (optional)
 * @param logger Plugin logger (optional, defaults to console logger)
 * @param scope CoroutineScope for async operations
 *
 * @since 1.0.0
 * @see UniversalPluginRegistry
 * @see PluginLifecycleManager
 * @see PluginEventBus
 */
class AndroidPluginHost(
    private val context: Context,
    val serviceRegistry: ServiceRegistry,
    val eventBus: PluginEventBus,
    private val rpcServiceRegistry: RpcServiceRegistry = RpcServiceRegistry(),
    private val logger: PluginLogger = ConsolePluginLogger(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    /**
     * Plugin registry for all registered plugins.
     */
    val registry: UniversalPluginRegistry = UniversalPluginRegistry(rpcServiceRegistry)

    /**
     * Lifecycle manager for plugin state transitions.
     */
    val lifecycleManager: PluginLifecycleManager = PluginLifecycleManager(registry, eventBus, scope)

    /**
     * Service connection manager for plugins.
     */
    val serviceConnectionManager: PluginServiceConnection = PluginServiceConnection(context, this, scope)

    /**
     * Current host state.
     */
    private val _state = MutableStateFlow(HostState.UNINITIALIZED)
    val state: StateFlow<HostState> = _state.asStateFlow()

    /**
     * Map of plugin IDs to their instances.
     */
    private val _plugins = MutableStateFlow<Map<String, UniversalPlugin>>(emptyMap())
    val plugins: StateFlow<Map<String, UniversalPlugin>> = _plugins.asStateFlow()

    /**
     * Map of plugin IDs to their Android contexts.
     */
    private val pluginContexts = mutableMapOf<String, AndroidPluginContext>()

    /**
     * Current activity (if any).
     */
    private var currentActivity: Activity? = null

    /**
     * Current accessibility service (if connected).
     */
    private var accessibilityService: AccessibilityService? = null

    /**
     * Activity lifecycle callbacks for tracking activity state.
     */
    private var activityCallbacks: Application.ActivityLifecycleCallbacks? = null

    /**
     * Mutex for thread-safe plugin operations.
     */
    private val mutex = Mutex()

    /**
     * Built-in plugin factory functions.
     */
    private val builtinPluginFactories = mutableMapOf<String, suspend () -> UniversalPlugin>()

    /**
     * Initialize the plugin host.
     *
     * Sets up the host for plugin management. This should be called before
     * loading any plugins.
     *
     * @return Result indicating success or failure
     */
    suspend fun initialize(): Result<Unit> {
        if (_state.value != HostState.UNINITIALIZED) {
            PluginLog.w(TAG, "Plugin host already initialized (state: ${_state.value})")
            return if (_state.value == HostState.READY) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Plugin host in invalid state: ${_state.value}"))
            }
        }

        return try {
            _state.value = HostState.INITIALIZING
            PluginLog.i(TAG, "Initializing AndroidPluginHost...")

            // Register core services
            serviceRegistry.register(ServiceRegistry.PLUGIN_EVENT_BUS, eventBus)
            serviceRegistry.register(ServiceRegistry.PLUGIN_REGISTRY, registry)
            serviceRegistry.register(ServiceRegistry.RPC_SERVICE_REGISTRY, rpcServiceRegistry)

            // Register activity lifecycle callbacks if we have an Application context
            if (context.applicationContext is Application) {
                registerActivityLifecycleCallbacks()
            }

            // Start health checks
            lifecycleManager.startHealthChecks()

            _state.value = HostState.READY
            PluginLog.i(TAG, "AndroidPluginHost initialized successfully")

            // Publish host ready event
            eventBus.publish(
                PluginEvent(
                    eventId = "",
                    sourcePluginId = HOST_PLUGIN_ID,
                    eventType = EVENT_HOST_READY,
                    payload = mapOf("version" to VERSION)
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = HostState.ERROR
            PluginLog.e(TAG, "Failed to initialize AndroidPluginHost", e)
            Result.failure(e)
        }
    }

    /**
     * Load a plugin by ID.
     *
     * Loads and initializes a plugin. The plugin must have been previously
     * registered as a built-in plugin or be loadable through the plugin loader.
     *
     * @param pluginId Unique plugin identifier
     * @param config Optional plugin configuration
     * @return Result containing the loaded plugin or failure
     */
    suspend fun loadPlugin(
        pluginId: String,
        config: PluginConfig = PluginConfig.EMPTY
    ): Result<UniversalPlugin> {
        if (_state.value != HostState.READY) {
            return Result.failure(IllegalStateException("Plugin host not ready (state: ${_state.value})"))
        }

        return mutex.withLock {
            try {
                // Check if already loaded
                _plugins.value[pluginId]?.let { existing ->
                    PluginLog.d(TAG, "Plugin already loaded: $pluginId")
                    return@withLock Result.success(existing)
                }

                PluginLog.i(TAG, "Loading plugin: $pluginId")

                // Try to create from built-in factory
                val factory = builtinPluginFactories[pluginId]
                    ?: return@withLock Result.failure(
                        IllegalArgumentException("Plugin not found: $pluginId")
                    )

                val plugin = factory()

                // Create plugin context
                val pluginContext = AndroidPluginContext.create(
                    context = context,
                    pluginId = pluginId,
                    logger = logger,
                    serviceRegistry = serviceRegistry,
                    eventBus = eventBus as PluginEventBus
                )
                pluginContexts[pluginId] = pluginContext

                // Initialize the plugin
                val initResult = plugin.initialize(config, pluginContext.toPluginContext())

                when (initResult) {
                    is InitResult.Success -> {
                        PluginLog.i(TAG, "Plugin initialized: $pluginId - ${initResult.message}")

                        // Register with registry
                        val endpoint = createPluginEndpoint(pluginId)
                        registry.register(plugin, endpoint)

                        // Start lifecycle management
                        lifecycleManager.manage(plugin, pluginContext.toPluginContext())

                        // Add to loaded plugins
                        _plugins.value = _plugins.value + (pluginId to plugin)

                        // Publish plugin loaded event
                        eventBus.publish(
                            PluginEvent(
                                eventId = "",
                                sourcePluginId = HOST_PLUGIN_ID,
                                eventType = PluginEvent.TYPE_PLUGIN_REGISTERED,
                                payload = mapOf(
                                    "pluginId" to pluginId,
                                    "version" to plugin.version
                                )
                            )
                        )

                        Result.success(plugin)
                    }
                    is InitResult.Failure -> {
                        PluginLog.e(TAG, "Plugin initialization failed: $pluginId - ${initResult.message}")
                        pluginContexts.remove(pluginId)
                        Result.failure(initResult.error)
                    }
                }
            } catch (e: Exception) {
                PluginLog.e(TAG, "Exception loading plugin: $pluginId", e)
                pluginContexts.remove(pluginId)
                Result.failure(e)
            }
        }
    }

    /**
     * Unload a plugin by ID.
     *
     * Shuts down the plugin and removes it from the registry.
     *
     * @param pluginId Plugin identifier to unload
     * @return Result indicating success or failure
     */
    suspend fun unloadPlugin(pluginId: String): Result<Unit> {
        return mutex.withLock {
            try {
                val plugin = _plugins.value[pluginId]
                    ?: return@withLock Result.failure(
                        IllegalArgumentException("Plugin not loaded: $pluginId")
                    )

                PluginLog.i(TAG, "Unloading plugin: $pluginId")

                // Shutdown plugin via lifecycle manager
                lifecycleManager.shutdown(pluginId)

                // Unbind any service connections
                serviceConnectionManager.unbindService(pluginId)

                // Unregister from registry
                registry.unregister(pluginId)

                // Remove plugin and context
                _plugins.value = _plugins.value - pluginId
                pluginContexts.remove(pluginId)

                // Publish plugin unloaded event
                eventBus.publish(
                    PluginEvent(
                        eventId = "",
                        sourcePluginId = HOST_PLUGIN_ID,
                        eventType = PluginEvent.TYPE_PLUGIN_UNREGISTERED,
                        payload = mapOf("pluginId" to pluginId)
                    )
                )

                PluginLog.i(TAG, "Plugin unloaded: $pluginId")
                Result.success(Unit)
            } catch (e: Exception) {
                PluginLog.e(TAG, "Exception unloading plugin: $pluginId", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get a loaded plugin by ID.
     *
     * @param pluginId Plugin identifier
     * @return The plugin instance or null if not loaded
     */
    fun getPlugin(pluginId: String): UniversalPlugin? {
        return _plugins.value[pluginId]
    }

    /**
     * Get plugins that provide a specific capability.
     *
     * @param capabilityId Capability identifier to search for
     * @return List of plugins providing the capability
     */
    fun getPluginsByCapability(capabilityId: String): List<UniversalPlugin> {
        val registrations = registry.discoverByCapability(capabilityId)
        return registrations.mapNotNull { _plugins.value[it.pluginId] }
    }

    /**
     * Get all loaded plugins.
     *
     * @return List of all loaded plugin instances
     */
    fun getLoadedPlugins(): List<UniversalPlugin> {
        return _plugins.value.values.toList()
    }

    /**
     * Check if a plugin is loaded.
     *
     * @param pluginId Plugin identifier
     * @return true if the plugin is loaded
     */
    fun isPluginLoaded(pluginId: String): Boolean {
        return _plugins.value.containsKey(pluginId)
    }

    /**
     * Get the context for a loaded plugin.
     *
     * @param pluginId Plugin identifier
     * @return AndroidPluginContext or null if plugin not loaded
     */
    fun getPluginContext(pluginId: String): AndroidPluginContext? {
        return pluginContexts[pluginId]
    }

    // =========================================================================
    // Android Lifecycle Integration
    // =========================================================================

    /**
     * Called when an Activity is created.
     *
     * Notifies plugins of the activity lifecycle change and updates
     * the current activity reference.
     *
     * @param activity The created Activity
     */
    fun onActivityCreated(activity: Activity) {
        currentActivity = activity
        PluginLog.d(TAG, "Activity created: ${activity::class.simpleName}")

        scope.launch {
            eventBus.publish(
                PluginEvent(
                    eventId = "",
                    sourcePluginId = HOST_PLUGIN_ID,
                    eventType = EVENT_ACTIVITY_CREATED,
                    payload = mapOf(
                        "activityClass" to (activity::class.qualifiedName ?: "Unknown")
                    )
                )
            )
        }
    }

    /**
     * Called when an Activity is destroyed.
     *
     * Notifies plugins and clears the current activity reference if it matches.
     *
     * @param activity The destroyed Activity
     */
    fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
        PluginLog.d(TAG, "Activity destroyed: ${activity::class.simpleName}")

        scope.launch {
            eventBus.publish(
                PluginEvent(
                    eventId = "",
                    sourcePluginId = HOST_PLUGIN_ID,
                    eventType = EVENT_ACTIVITY_DESTROYED,
                    payload = mapOf(
                        "activityClass" to (activity::class.qualifiedName ?: "Unknown")
                    )
                )
            )
        }
    }

    /**
     * Called when an Activity is resumed.
     *
     * Triggers plugin resume for plugins that were paused.
     *
     * @param activity The resumed Activity
     */
    fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        PluginLog.d(TAG, "Activity resumed: ${activity::class.simpleName}")

        scope.launch {
            // Resume all paused plugins
            _plugins.value.forEach { (pluginId, plugin) ->
                if (plugin.state == PluginState.PAUSED) {
                    lifecycleManager.resume(pluginId)
                }
            }

            eventBus.publish(
                PluginEvent(
                    eventId = "",
                    sourcePluginId = HOST_PLUGIN_ID,
                    eventType = EVENT_ACTIVITY_RESUMED,
                    payload = mapOf(
                        "activityClass" to (activity::class.qualifiedName ?: "Unknown")
                    )
                )
            )
        }
    }

    /**
     * Called when an Activity is paused.
     *
     * Triggers plugin pause to conserve resources.
     *
     * @param activity The paused Activity
     */
    fun onActivityPaused(activity: Activity) {
        PluginLog.d(TAG, "Activity paused: ${activity::class.simpleName}")

        scope.launch {
            // Pause all active plugins
            _plugins.value.forEach { (pluginId, plugin) ->
                if (plugin.state == PluginState.ACTIVE) {
                    lifecycleManager.pause(pluginId)
                }
            }

            eventBus.publish(
                PluginEvent(
                    eventId = "",
                    sourcePluginId = HOST_PLUGIN_ID,
                    eventType = EVENT_ACTIVITY_PAUSED,
                    payload = mapOf(
                        "activityClass" to (activity::class.qualifiedName ?: "Unknown")
                    )
                )
            )
        }
    }

    /**
     * Called when AccessibilityService connects.
     *
     * Registers the accessibility service with the service registry and
     * notifies all plugins.
     *
     * @param service The connected AccessibilityService
     */
    fun onServiceConnected(service: AccessibilityService) {
        accessibilityService = service
        PluginLog.i(TAG, "AccessibilityService connected: ${service::class.simpleName}")

        scope.launch {
            // Register with service registry
            serviceRegistry.register(ServiceRegistry.ACCESSIBILITY_SERVICE, service)

            // Notify plugins
            eventBus.publish(
                PluginEvent(
                    eventId = "",
                    sourcePluginId = HOST_PLUGIN_ID,
                    eventType = EVENT_ACCESSIBILITY_CONNECTED,
                    payload = mapOf(
                        "serviceClass" to (service::class.qualifiedName ?: "Unknown")
                    )
                )
            )
        }
    }

    /**
     * Called when AccessibilityService disconnects.
     *
     * Unregisters the accessibility service and notifies all plugins.
     */
    fun onServiceDisconnected() {
        accessibilityService = null
        PluginLog.i(TAG, "AccessibilityService disconnected")

        scope.launch {
            // Unregister from service registry
            serviceRegistry.unregister(ServiceRegistry.ACCESSIBILITY_SERVICE)

            // Notify plugins
            eventBus.publish(
                PluginEvent(
                    eventId = "",
                    sourcePluginId = HOST_PLUGIN_ID,
                    eventType = EVENT_ACCESSIBILITY_DISCONNECTED
                )
            )
        }
    }

    /**
     * Get the current Activity if available.
     *
     * @return Current Activity or null
     */
    fun getCurrentActivity(): Activity? = currentActivity

    /**
     * Get the AccessibilityService if connected.
     *
     * @return Connected AccessibilityService or null
     */
    fun getAccessibilityService(): AccessibilityService? = accessibilityService

    // =========================================================================
    // Built-in Plugin Registration
    // =========================================================================

    /**
     * Register a factory for a built-in plugin.
     *
     * Built-in plugins are first-party plugins that are bundled with the app.
     * They are registered by factory functions that create plugin instances.
     *
     * @param pluginId Unique plugin identifier
     * @param factory Suspend function that creates the plugin instance
     */
    fun registerBuiltinPluginFactory(pluginId: String, factory: suspend () -> UniversalPlugin) {
        builtinPluginFactories[pluginId] = factory
        PluginLog.d(TAG, "Registered built-in plugin factory: $pluginId")
    }

    /**
     * Register built-in plugins.
     *
     * Loads all plugins that have been registered via registerBuiltinPluginFactory().
     * This is typically called during application startup.
     *
     * @param configs Optional map of plugin IDs to configurations
     * @return Map of plugin IDs to their load results
     */
    suspend fun registerBuiltinPlugins(
        configs: Map<String, PluginConfig> = emptyMap()
    ): Map<String, Result<UniversalPlugin>> {
        PluginLog.i(TAG, "Registering ${builtinPluginFactories.size} built-in plugins...")

        val results = mutableMapOf<String, Result<UniversalPlugin>>()

        builtinPluginFactories.keys.forEach { pluginId ->
            val config = configs[pluginId] ?: PluginConfig.EMPTY
            results[pluginId] = loadPlugin(pluginId, config)
        }

        val successes = results.count { it.value.isSuccess }
        val failures = results.count { it.value.isFailure }
        PluginLog.i(TAG, "Built-in plugin registration complete: $successes loaded, $failures failed")

        return results
    }

    // =========================================================================
    // Shutdown
    // =========================================================================

    /**
     * Shutdown the plugin host.
     *
     * Unloads all plugins, stops health checks, and cleans up resources.
     * The host cannot be reused after shutdown.
     */
    suspend fun shutdown() {
        if (_state.value == HostState.SHUTDOWN || _state.value == HostState.SHUTTING_DOWN) {
            PluginLog.w(TAG, "Plugin host already shut down or shutting down")
            return
        }

        _state.value = HostState.SHUTTING_DOWN
        PluginLog.i(TAG, "Shutting down AndroidPluginHost...")

        try {
            // Publish shutdown event
            eventBus.publish(
                PluginEvent(
                    eventId = "",
                    sourcePluginId = HOST_PLUGIN_ID,
                    eventType = EVENT_HOST_SHUTDOWN
                )
            )

            // Stop health checks
            lifecycleManager.stopHealthChecks()

            // Shutdown all plugins
            val failures = lifecycleManager.shutdownAll()
            if (failures.isNotEmpty()) {
                PluginLog.w(TAG, "Some plugins failed to shutdown: $failures")
            }

            // Unbind all service connections
            serviceConnectionManager.unbindAll()

            // Unregister activity callbacks
            unregisterActivityLifecycleCallbacks()

            // Clear state
            _plugins.value = emptyMap()
            pluginContexts.clear()
            registry.clear()

            // Clear service registry references
            serviceRegistry.clear()

            _state.value = HostState.SHUTDOWN
            PluginLog.i(TAG, "AndroidPluginHost shutdown complete")
        } catch (e: Exception) {
            PluginLog.e(TAG, "Error during shutdown", e)
            _state.value = HostState.ERROR
        }
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Register activity lifecycle callbacks.
     */
    private fun registerActivityLifecycleCallbacks() {
        val app = context.applicationContext as? Application ?: return

        activityCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                this@AndroidPluginHost.onActivityCreated(activity)
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                this@AndroidPluginHost.onActivityResumed(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                this@AndroidPluginHost.onActivityPaused(activity)
            }

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                this@AndroidPluginHost.onActivityDestroyed(activity)
            }
        }

        app.registerActivityLifecycleCallbacks(activityCallbacks)
        PluginLog.d(TAG, "Registered activity lifecycle callbacks")
    }

    /**
     * Unregister activity lifecycle callbacks.
     */
    private fun unregisterActivityLifecycleCallbacks() {
        val app = context.applicationContext as? Application ?: return
        activityCallbacks?.let {
            app.unregisterActivityLifecycleCallbacks(it)
            activityCallbacks = null
            PluginLog.d(TAG, "Unregistered activity lifecycle callbacks")
        }
    }

    /**
     * Create a service endpoint for a plugin.
     */
    private fun createPluginEndpoint(pluginId: String): ServiceEndpoint {
        return ServiceEndpoint(
            serviceName = pluginId,
            host = "localhost",
            port = 0, // In-process plugin
            protocol = "local",
            metadata = mapOf(
                "type" to "plugin",
                "host" to "android"
            )
        )
    }

    companion object {
        private const val TAG = "AndroidPluginHost"

        /** Current version of the plugin host */
        const val VERSION = "1.0.0"

        /** Plugin ID used for host events */
        const val HOST_PLUGIN_ID = "com.augmentalis.plugin.host"

        // =========================================================================
        // Host Event Types
        // =========================================================================

        /** Host is ready to load plugins */
        const val EVENT_HOST_READY = "host.ready"

        /** Host is shutting down */
        const val EVENT_HOST_SHUTDOWN = "host.shutdown"

        /** Activity was created */
        const val EVENT_ACTIVITY_CREATED = "android.activity.created"

        /** Activity was resumed */
        const val EVENT_ACTIVITY_RESUMED = "android.activity.resumed"

        /** Activity was paused */
        const val EVENT_ACTIVITY_PAUSED = "android.activity.paused"

        /** Activity was destroyed */
        const val EVENT_ACTIVITY_DESTROYED = "android.activity.destroyed"

        /** AccessibilityService connected */
        const val EVENT_ACCESSIBILITY_CONNECTED = "android.accessibility.connected"

        /** AccessibilityService disconnected */
        const val EVENT_ACCESSIBILITY_DISCONNECTED = "android.accessibility.disconnected"

        /**
         * Create and initialize a plugin host with default settings.
         *
         * Convenience factory method that creates a fully configured host.
         *
         * @param context Android Context (preferably Application)
         * @return Initialized AndroidPluginHost
         */
        suspend fun createAndInitialize(context: Context): AndroidPluginHost {
            val serviceRegistry = ServiceRegistry()
            val eventBus = GrpcPluginEventBus()
            val rpcRegistry = RpcServiceRegistry()

            val host = AndroidPluginHost(
                context = context,
                serviceRegistry = serviceRegistry,
                eventBus = eventBus,
                rpcServiceRegistry = rpcRegistry
            )

            host.initialize()
            return host
        }
    }
}

/**
 * Extension function to get typed plugins by capability.
 *
 * @param T The expected plugin interface type
 * @param capabilityId Capability to search for
 * @return List of plugins cast to the specified type
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T : UniversalPlugin> AndroidPluginHost.getPluginsAs(
    capabilityId: String
): List<T> {
    return getPluginsByCapability(capabilityId).filterIsInstance<T>()
}

/**
 * Extension function to load multiple plugins in parallel.
 *
 * @param pluginIds Plugin identifiers to load
 * @return Map of plugin IDs to load results
 */
suspend fun AndroidPluginHost.loadPlugins(
    vararg pluginIds: String
): Map<String, Result<UniversalPlugin>> {
    return coroutineScope {
        pluginIds.map { pluginId ->
            async { pluginId to loadPlugin(pluginId) }
        }.awaitAll().toMap()
    }
}

/**
 * Extension function to observe plugin state changes.
 *
 * @param pluginId Plugin identifier to observe
 * @return Flow of plugin states
 */
fun AndroidPluginHost.observePluginState(pluginId: String): Flow<PluginState?> {
    return plugins.map { it[pluginId]?.state }
        .distinctUntilChanged()
}
