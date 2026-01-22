/**
 * BasePlugin.kt - Abstract base class for universal plugins
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides a base implementation of UniversalPlugin that reduces boilerplate
 * for plugin developers. Handles state management, lifecycle transitions,
 * and provides sensible defaults for optional methods.
 */
package com.augmentalis.magiccode.plugins.sdk

import com.augmentalis.magiccode.plugins.universal.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Abstract base class for implementing UniversalPlugin.
 *
 * BasePlugin handles the boilerplate of state management, lifecycle transitions,
 * and provides default implementations for optional methods. Plugin developers
 * only need to implement the core initialization logic.
 *
 * ## Benefits
 * - Automatic state management with proper transitions
 * - Exception handling in lifecycle methods
 * - Sensible defaults for pause/resume/shutdown
 * - Built-in health check with extensible diagnostics
 * - Configuration and event handling hooks
 *
 * ## Usage Example
 * ```kotlin
 * class MyLLMPlugin : BasePlugin() {
 *     override val pluginId = "com.example.llm.mymodel"
 *     override val pluginName = "My LLM Plugin"
 *     override val version = "1.0.0"
 *     override val capabilities = setOf(
 *         PluginCapability(
 *             id = PluginCapability.LLM_TEXT_GENERATION,
 *             name = "Text Generation",
 *             version = "1.0.0"
 *         )
 *     )
 *
 *     private lateinit var modelClient: ModelClient
 *
 *     override suspend fun onInitialize(): InitResult {
 *         val apiKey = config.getSecret("apiKey")
 *             ?: return InitResult.failure("API key required", recoverable = false)
 *
 *         modelClient = ModelClient(apiKey)
 *         return InitResult.success("Connected to model API")
 *     }
 *
 *     override suspend fun onShutdown() {
 *         modelClient.close()
 *     }
 *
 *     override fun getHealthDiagnostics(): Map<String, String> = mapOf(
 *         "modelConnected" to modelClient.isConnected.toString(),
 *         "requestCount" to modelClient.requestCount.toString()
 *     )
 * }
 * ```
 *
 * @since 1.0.0
 * @see UniversalPlugin
 */
abstract class BasePlugin : UniversalPlugin {

    // =========================================================================
    // Protected State Access
    // =========================================================================

    /**
     * The plugin configuration provided during initialization.
     * Available after [initialize] is called.
     */
    protected lateinit var config: PluginConfig
        private set

    /**
     * The plugin context provided during initialization.
     * Available after [initialize] is called.
     */
    protected lateinit var context: PluginContext
        private set

    /**
     * Check if the plugin has been initialized with config and context.
     */
    protected val isInitialized: Boolean
        get() = ::config.isInitialized && ::context.isInitialized

    // =========================================================================
    // State Management
    // =========================================================================

    private val _stateFlow = MutableStateFlow(PluginState.UNINITIALIZED)

    /**
     * Observable state flow for reactive state monitoring.
     */
    override val stateFlow: StateFlow<PluginState> = _stateFlow.asStateFlow()

    /**
     * Current plugin lifecycle state.
     */
    override val state: PluginState get() = _stateFlow.value

    /**
     * Update the plugin state.
     *
     * Protected to allow subclasses to manage state transitions for
     * custom lifecycle scenarios.
     *
     * @param newState The new state to transition to
     */
    protected fun updateState(newState: PluginState) {
        _stateFlow.value = newState
    }

    // =========================================================================
    // Lifecycle Implementation
    // =========================================================================

    /**
     * Initialize the plugin with configuration.
     *
     * Handles state transitions and exception catching. Delegates to
     * [onInitialize] for actual initialization logic.
     *
     * @param config Plugin configuration
     * @param context Plugin execution context
     * @return InitResult indicating success or failure
     */
    override suspend fun initialize(config: PluginConfig, context: PluginContext): InitResult {
        this.config = config
        this.context = context
        _stateFlow.value = PluginState.INITIALIZING

        return try {
            val result = onInitialize()
            _stateFlow.value = if (result.isSuccess()) PluginState.ACTIVE else PluginState.FAILED
            result
        } catch (e: Exception) {
            _stateFlow.value = PluginState.FAILED
            InitResult.failure(e)
        }
    }

    /**
     * Activate the plugin.
     *
     * Transitions from STOPPED or ERROR state to ACTIVE. Override [onActivate]
     * to provide custom activation logic.
     *
     * @return Result indicating success or failure
     */
    override suspend fun activate(): Result<Unit> {
        if (state !in setOf(PluginState.STOPPED, PluginState.ERROR, PluginState.INITIALIZING)) {
            return Result.failure(IllegalStateException("Cannot activate from state: $state"))
        }

        return try {
            onActivate()
            _stateFlow.value = PluginState.ACTIVE
            Result.success(Unit)
        } catch (e: Exception) {
            _stateFlow.value = PluginState.ERROR
            Result.failure(e)
        }
    }

    /**
     * Pause the plugin.
     *
     * Suspends non-essential operations to conserve resources. Override [onPause]
     * to release expensive resources.
     *
     * @return Result indicating success or failure
     */
    override suspend fun pause(): Result<Unit> {
        if (!state.canPause()) {
            return Result.failure(IllegalStateException("Cannot pause from state: $state"))
        }

        return try {
            onPause()
            _stateFlow.value = PluginState.PAUSED
            Result.success(Unit)
        } catch (e: Exception) {
            _stateFlow.value = PluginState.ERROR
            Result.failure(e)
        }
    }

    /**
     * Resume the plugin from paused state.
     *
     * Re-acquires resources and resumes normal operation. Override [onResume]
     * to restore expensive resources.
     *
     * @return Result indicating success or failure
     */
    override suspend fun resume(): Result<Unit> {
        if (!state.canResume()) {
            return Result.failure(IllegalStateException("Cannot resume from state: $state"))
        }

        _stateFlow.value = PluginState.RESUMING

        return try {
            onResume()
            _stateFlow.value = PluginState.ACTIVE
            Result.success(Unit)
        } catch (e: Exception) {
            _stateFlow.value = PluginState.ERROR
            Result.failure(e)
        }
    }

    /**
     * Shutdown the plugin gracefully.
     *
     * Releases all resources and stops the plugin. Override [onShutdown]
     * to perform cleanup.
     *
     * @return Result indicating success or failure
     */
    override suspend fun shutdown(): Result<Unit> {
        if (!state.canShutdown() && state != PluginState.UNINITIALIZED) {
            return Result.failure(IllegalStateException("Cannot shutdown from state: $state"))
        }

        _stateFlow.value = PluginState.STOPPING

        return try {
            onShutdown()
            _stateFlow.value = PluginState.STOPPED
            Result.success(Unit)
        } catch (e: Exception) {
            // Still mark as stopped even on error during shutdown
            _stateFlow.value = PluginState.STOPPED
            Result.failure(e)
        }
    }

    // =========================================================================
    // Configuration
    // =========================================================================

    /**
     * Handle configuration changes.
     *
     * Default implementation delegates to [onConfigChanged]. Override either
     * this method or [onConfigChanged] to handle dynamic configuration updates.
     *
     * @param config Map of changed configuration values
     */
    override suspend fun onConfigurationChanged(config: Map<String, Any>) {
        onConfigChanged(config)
    }

    // =========================================================================
    // Health Monitoring
    // =========================================================================

    /**
     * Perform a health check.
     *
     * Default implementation returns healthy if state is ACTIVE or PAUSED,
     * and includes diagnostics from [getHealthDiagnostics].
     *
     * @return HealthStatus with health state and diagnostics
     */
    override fun healthCheck(): HealthStatus {
        val diagnostics = mutableMapOf<String, String>()

        // Add state information
        diagnostics["state"] = state.name
        diagnostics["pluginId"] = pluginId
        diagnostics["version"] = version

        // Add custom diagnostics from subclass
        diagnostics.putAll(getHealthDiagnostics())

        val isHealthy = state in setOf(PluginState.ACTIVE, PluginState.PAUSED)

        return if (isHealthy) {
            HealthStatus.healthy(
                message = "Plugin is operational",
                diagnostics = diagnostics
            )
        } else {
            HealthStatus.unhealthy(
                message = "Plugin is in state: $state",
                diagnostics = diagnostics
            )
        }
    }

    // =========================================================================
    // Events
    // =========================================================================

    /**
     * Handle an incoming event.
     *
     * Default implementation delegates to [onEventReceived]. Override either
     * this method or [onEventReceived] to handle events.
     *
     * @param event The plugin event to handle
     */
    override suspend fun onEvent(event: PluginEvent) {
        onEventReceived(event)
    }

    // =========================================================================
    // Abstract Methods (Required Overrides)
    // =========================================================================

    /**
     * Perform plugin-specific initialization.
     *
     * Called during [initialize] after config and context are set. Implement
     * this method to:
     * - Validate configuration
     * - Initialize resources (connections, caches)
     * - Register with services
     *
     * @return InitResult indicating success or failure
     */
    protected abstract suspend fun onInitialize(): InitResult

    // =========================================================================
    // Optional Override Hooks
    // =========================================================================

    /**
     * Perform activation logic.
     *
     * Called during [activate]. Override to perform custom activation steps.
     * Default implementation does nothing.
     */
    protected open suspend fun onActivate() {}

    /**
     * Perform pause logic.
     *
     * Called during [pause]. Override to release expensive resources that
     * can be re-acquired on resume.
     */
    protected open suspend fun onPause() {}

    /**
     * Perform resume logic.
     *
     * Called during [resume]. Override to re-acquire resources that were
     * released during pause.
     */
    protected open suspend fun onResume() {}

    /**
     * Perform shutdown cleanup.
     *
     * Called during [shutdown]. Override to release all resources, close
     * connections, and save any necessary state.
     */
    protected open suspend fun onShutdown() {}

    /**
     * Handle configuration changes.
     *
     * Called when plugin configuration is updated at runtime. Override to
     * apply configuration changes without requiring a restart.
     *
     * @param config Map of changed configuration values
     */
    protected open suspend fun onConfigChanged(config: Map<String, Any>) {}

    /**
     * Handle incoming events.
     *
     * Called when an event is received that matches subscriptions. Override
     * to process events from other plugins or the system.
     *
     * @param event The received event
     */
    protected open suspend fun onEventReceived(event: PluginEvent) {}

    /**
     * Provide custom health diagnostics.
     *
     * Override to include plugin-specific diagnostic information in health
     * checks. Default returns an empty map.
     *
     * @return Map of diagnostic key-value pairs
     */
    protected open fun getHealthDiagnostics(): Map<String, String> = emptyMap()

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Get the plugin-specific data directory.
     *
     * Convenience method to get the data directory for this plugin.
     * Requires [context] to be initialized.
     *
     * @return Path to plugin data directory
     * @throws IllegalStateException if context is not initialized
     */
    protected fun getDataDir(): String {
        check(isInitialized) { "Plugin not initialized" }
        return context.getPluginDataDir(pluginId)
    }

    /**
     * Get the plugin-specific cache directory.
     *
     * Convenience method to get the cache directory for this plugin.
     * Requires [context] to be initialized.
     *
     * @return Path to plugin cache directory
     * @throws IllegalStateException if context is not initialized
     */
    protected fun getCacheDir(): String {
        check(isInitialized) { "Plugin not initialized" }
        return context.getPluginCacheDir(pluginId)
    }

    /**
     * Get a configuration setting with type-safe access.
     *
     * @param key Setting key
     * @param default Default value if not found
     * @return Setting value or default
     */
    protected fun getSetting(key: String, default: String = ""): String {
        check(isInitialized) { "Plugin not initialized" }
        return config.getSetting(key, default)
    }

    /**
     * Get an integer configuration setting.
     *
     * @param key Setting key
     * @param default Default value if not found or not parseable
     * @return Setting value as Int or default
     */
    protected fun getIntSetting(key: String, default: Int = 0): Int {
        check(isInitialized) { "Plugin not initialized" }
        return config.getInt(key, default)
    }

    /**
     * Get a boolean configuration setting.
     *
     * @param key Setting key
     * @param default Default value if not found
     * @return Setting value as Boolean or default
     */
    protected fun getBooleanSetting(key: String, default: Boolean = false): Boolean {
        check(isInitialized) { "Plugin not initialized" }
        return config.getBoolean(key, default)
    }

    /**
     * Check if a feature is enabled.
     *
     * @param feature Feature flag name
     * @return true if feature is enabled
     */
    protected fun hasFeature(feature: String): Boolean {
        check(isInitialized) { "Plugin not initialized" }
        return config.hasFeature(feature)
    }

    /**
     * Get a secret value.
     *
     * @param key Secret key
     * @return Secret value or null if not found
     */
    protected fun getSecret(key: String): String? {
        check(isInitialized) { "Plugin not initialized" }
        return config.getSecret(key)
    }
}
