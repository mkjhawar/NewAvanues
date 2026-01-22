/**
 * UniversalPlugin.kt - Universal plugin interface
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the contract for all universal plugins in the system.
 * Extends existing AIPluginInterface pattern with lifecycle and event support.
 */
package com.augmentalis.magiccode.plugins.universal

import kotlinx.coroutines.flow.StateFlow

/**
 * Universal Plugin contract for all plugin types.
 *
 * This interface defines the standard contract that all plugins must implement
 * to participate in the Universal Plugin system. It provides:
 * - Identity (ID, name, version)
 * - Capability advertisement for discovery
 * - Lifecycle management (initialize, activate, pause, resume, shutdown)
 * - Configuration change handling
 * - Health monitoring
 * - Event handling for inter-plugin communication
 *
 * ## Design Goals
 * - **Accessibility-First**: Built for voice/gaze control applications
 * - **Resource-Aware**: Pause/resume support for mobile environments
 * - **Discoverable**: Capability-based plugin discovery
 * - **Observable**: Reactive state observation via Flow
 * - **Resilient**: Health monitoring and recovery support
 *
 * ## Implementation Example
 * ```kotlin
 * class MyLLMPlugin : UniversalPlugin {
 *     override val pluginId = "com.augmentalis.llm.mymodel"
 *     override val pluginName = "My LLM Plugin"
 *     override val version = "1.0.0"
 *
 *     override val capabilities = setOf(
 *         PluginCapability(
 *             id = PluginCapability.LLM_TEXT_GENERATION,
 *             name = "Text Generation",
 *             version = "1.0.0"
 *         )
 *     )
 *
 *     private val _state = MutableStateFlow(PluginState.UNINITIALIZED)
 *     override val state: PluginState get() = _state.value
 *     override val stateFlow: StateFlow<PluginState> = _state
 *
 *     override suspend fun initialize(config: PluginConfig, context: PluginContext): InitResult {
 *         _state.value = PluginState.INITIALIZING
 *         // Initialize resources...
 *         _state.value = PluginState.ACTIVE
 *         return InitResult.Success()
 *     }
 *
 *     // ... implement other methods
 * }
 * ```
 *
 * @since 1.0.0
 * @see PluginCapability
 * @see PluginState
 * @see PluginConfig
 * @see PluginContext
 */
interface UniversalPlugin {

    // =========================================================================
    // Identity Properties
    // =========================================================================

    /**
     * Unique plugin identifier.
     *
     * Must be in reverse-domain notation (e.g., "com.augmentalis.llm.openai").
     * This ID is used for registration, discovery, and event routing.
     */
    val pluginId: String

    /**
     * Human-readable plugin name.
     *
     * Used for display purposes in UI and logs.
     */
    val pluginName: String

    /**
     * Plugin version in semantic versioning format.
     *
     * Format: MAJOR.MINOR.PATCH (e.g., "1.2.3")
     */
    val version: String

    // =========================================================================
    // Capability Advertisement
    // =========================================================================

    /**
     * Set of capabilities this plugin advertises.
     *
     * Capabilities are used for plugin discovery. Other components can
     * find plugins by the capabilities they need.
     *
     * @see PluginCapability
     */
    val capabilities: Set<PluginCapability>

    // =========================================================================
    // State Management
    // =========================================================================

    /**
     * Current plugin lifecycle state.
     *
     * Returns the current state synchronously. For reactive observation,
     * use [stateFlow].
     *
     * @see PluginState
     */
    val state: PluginState

    /**
     * Observable state flow for reactive state monitoring.
     *
     * Emits the current state and all subsequent state changes.
     * Useful for UI components and lifecycle management.
     *
     * @see PluginState
     */
    val stateFlow: StateFlow<PluginState>

    // =========================================================================
    // Lifecycle Methods
    // =========================================================================

    /**
     * Initialize the plugin with configuration.
     *
     * Called once during plugin startup. The plugin should:
     * - Validate configuration
     * - Initialize resources (connections, caches, etc.)
     * - Register with any required services
     * - Transition to ACTIVE state on success
     *
     * @param config Plugin configuration containing settings and secrets
     * @param context Plugin execution context with directories and services
     * @return InitResult indicating success or failure
     * @see PluginConfig
     * @see PluginContext
     * @see InitResult
     */
    suspend fun initialize(config: PluginConfig, context: PluginContext): InitResult

    /**
     * Activate the plugin.
     *
     * Called to transition from INITIALIZING or STOPPED state to ACTIVE.
     * The plugin should make itself available for use.
     *
     * @return Result indicating success or failure
     */
    suspend fun activate(): Result<Unit>

    /**
     * Pause the plugin.
     *
     * Called when the plugin should temporarily suspend operations
     * (e.g., when app goes to background). The plugin should:
     * - Suspend non-essential operations
     * - Release expensive resources
     * - Maintain minimal state for quick resume
     *
     * ## Accessibility Context
     * For accessibility plugins, pause may be called when:
     * - Device screen is off
     * - Application loses focus
     * - System requests memory reduction
     *
     * @return Result indicating success or failure
     * @see resume
     */
    suspend fun pause(): Result<Unit>

    /**
     * Resume the plugin from paused state.
     *
     * Called when the plugin should resume normal operations
     * (e.g., when app returns to foreground). The plugin should:
     * - Re-acquire released resources
     * - Resume suspended operations
     * - Transition back to ACTIVE state
     *
     * @return Result indicating success or failure
     * @see pause
     */
    suspend fun resume(): Result<Unit>

    /**
     * Shutdown the plugin gracefully.
     *
     * Called when the plugin should stop completely. The plugin should:
     * - Complete any pending operations
     * - Save necessary state
     * - Release all resources
     * - Unregister from services
     * - Transition to STOPPED state
     *
     * @return Result indicating success or failure
     */
    suspend fun shutdown(): Result<Unit>

    // =========================================================================
    // Configuration
    // =========================================================================

    /**
     * Handle configuration changes.
     *
     * Called when plugin configuration is updated at runtime.
     * The plugin should apply the new configuration without requiring
     * a full restart when possible.
     *
     * ## Hot-Reload Support
     * Plugins should support hot-reload for:
     * - UI preferences (themes, fonts)
     * - Feature flags
     * - Timeout values
     *
     * Full restart may be required for:
     * - API endpoint changes
     * - Authentication credentials
     * - Model loading
     *
     * @param config Map of changed configuration values
     */
    suspend fun onConfigurationChanged(config: Map<String, Any>)

    // =========================================================================
    // Health Monitoring
    // =========================================================================

    /**
     * Perform a health check.
     *
     * Called periodically by the PluginLifecycleManager to verify
     * the plugin is functioning correctly. Should complete quickly (< 1 second).
     *
     * ## Diagnostic Information
     * Include useful diagnostics in [HealthStatus.diagnostics]:
     * - `lastRequestTime` - Time of last processed request
     * - `errorCount` - Number of errors since startup
     * - `memoryUsage` - Current memory usage
     * - `queueDepth` - Pending request count
     *
     * @return HealthStatus with health state and diagnostics
     * @see HealthStatus
     */
    fun healthCheck(): HealthStatus

    // =========================================================================
    // Events
    // =========================================================================

    /**
     * Handle an incoming event.
     *
     * Called when an event is published that matches this plugin's
     * subscriptions. Events enable loose coupling between plugins.
     *
     * ## Event Sources
     * Events may come from:
     * - Other plugins (capability announcements, data sharing)
     * - System (lifecycle events, configuration changes)
     * - Accessibility services (UI changes, voice commands)
     *
     * ## Implementation Note
     * Event handling should be non-blocking. For long-running processing,
     * dispatch to a worker and return immediately.
     *
     * @param event The plugin event to handle
     * @see PluginEvent
     */
    suspend fun onEvent(event: PluginEvent)

    // =========================================================================
    // State Persistence (for Hot Reload)
    // =========================================================================

    /**
     * Save the plugin's current state for hot reload.
     *
     * Called before the plugin is unloaded during a hot reload cycle.
     * Plugins should return a map containing all state that needs to be
     * preserved across the reload.
     *
     * ## State Map Guidelines
     * The returned map should contain only serializable types:
     * - Primitives: String, Int, Long, Float, Double, Boolean
     * - Collections: List<Any>, Map<String, Any>
     * - Null values
     *
     * Complex objects should be serialized to JSON strings or broken
     * down into primitive components.
     *
     * ## Example Implementation
     * ```kotlin
     * override suspend fun saveState(): Map<String, Any> {
     *     return mapOf(
     *         "counter" to requestCounter,
     *         "lastProcessedId" to lastProcessedId,
     *         "cacheEntries" to cacheEntries.toList(),
     *         "userPreferences" to userPreferences.toMap()
     *     )
     * }
     * ```
     *
     * ## Default Behavior
     * The default implementation returns an empty map, indicating no state
     * needs to be preserved. Override this method to enable state persistence.
     *
     * @return Map containing state to preserve, or empty map if no state
     * @see restoreState
     */
    suspend fun saveState(): Map<String, Any> = emptyMap()

    /**
     * Restore the plugin's state after hot reload.
     *
     * Called after the plugin is reloaded and initialized, with the state
     * that was saved before the reload. Plugins should restore their
     * internal state from the provided map.
     *
     * ## Implementation Notes
     * - This method is called after [initialize] completes successfully
     * - The state map will contain the same data returned by [saveState]
     * - Handle missing keys gracefully (state format may have changed)
     * - Validate restored data before using it
     *
     * ## Example Implementation
     * ```kotlin
     * override suspend fun restoreState(state: Map<String, Any>): Result<Unit> {
     *     return try {
     *         requestCounter = (state["counter"] as? Int) ?: 0
     *         lastProcessedId = (state["lastProcessedId"] as? String) ?: ""
     *
     *         @Suppress("UNCHECKED_CAST")
     *         val entries = state["cacheEntries"] as? List<String> ?: emptyList()
     *         cacheEntries.addAll(entries)
     *
     *         Result.success(Unit)
     *     } catch (e: Exception) {
     *         Result.failure(e)
     *     }
     * }
     * ```
     *
     * ## Error Handling
     * If state restoration fails, return a failure Result. The hot reload
     * system will log the error but the plugin will continue with its
     * default initialized state.
     *
     * ## Default Behavior
     * The default implementation returns success without restoring any state.
     * Override this method if you override [saveState].
     *
     * @param state The state map saved before reload
     * @return Result indicating success or failure of state restoration
     * @see saveState
     */
    suspend fun restoreState(state: Map<String, Any>): Result<Unit> = Result.success(Unit)
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Check if a plugin has a specific capability.
 *
 * @param capabilityId Capability ID to check
 * @return true if the plugin has the capability
 */
fun UniversalPlugin.hasCapability(capabilityId: String): Boolean {
    return capabilities.any { it.id == capabilityId }
}

/**
 * Get a specific capability by ID.
 *
 * @param capabilityId Capability ID to find
 * @return The capability or null if not found
 */
fun UniversalPlugin.getCapability(capabilityId: String): PluginCapability? {
    return capabilities.find { it.id == capabilityId }
}

/**
 * Check if plugin is in operational state.
 *
 * @return true if plugin can process requests
 */
fun UniversalPlugin.isOperational(): Boolean {
    return state.isOperational()
}

/**
 * Check if plugin is currently healthy.
 *
 * @return true if latest health check passed
 */
fun UniversalPlugin.isHealthy(): Boolean {
    return healthCheck().healthy
}

/**
 * Get all capability IDs as a set.
 *
 * @return Set of capability ID strings
 */
fun UniversalPlugin.getCapabilityIds(): Set<String> {
    return capabilities.map { it.id }.toSet()
}
