/**
 * PluginSystemSetup.kt - KMP interface for plugin system integration
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Common interface for setting up the plugin system across platforms.
 * Platform-specific implementations provide actual integration logic.
 */
package com.augmentalis.magiccode.plugins.integration

import com.augmentalis.magiccode.plugins.universal.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Plugin system setup result.
 */
data class PluginSystemSetupResult(
    val success: Boolean,
    val message: String,
    val pluginsLoaded: Int = 0,
    val errors: List<String> = emptyList()
)

/**
 * Plugin system configuration.
 */
data class PluginSystemConfig(
    /** Enable debug logging */
    val debugMode: Boolean = false,
    /** Enable performance monitoring */
    val enablePerformanceMonitoring: Boolean = true,
    /** Enable hot-reload support */
    val enableHotReload: Boolean = false,
    /** Minimum confidence for plugin handler selection */
    val minHandlerConfidence: Float = 0.7f,
    /** Register built-in plugins automatically */
    val registerBuiltinPlugins: Boolean = true,
    /** Custom plugin factories to register */
    val customPluginFactories: Map<String, () -> Plugin> = emptyMap()
)

/**
 * Platform-agnostic interface for plugin system setup.
 *
 * Each platform provides its own implementation that handles
 * platform-specific initialization and service integration.
 *
 * ## Usage
 * ```kotlin
 * // Get platform-specific setup instance
 * val setup = PluginSystemSetup.create(context)
 *
 * // Initialize with config
 * val result = setup.initialize(PluginSystemConfig(debugMode = true))
 *
 * // Access components
 * val dispatcher = setup.commandDispatcher
 * ```
 */
interface IPluginSystemSetup {

    /**
     * Current initialization state.
     */
    val isInitialized: StateFlow<Boolean>

    /**
     * Plugin host instance (available after initialization).
     */
    val pluginHost: IPluginHost<*>?

    /**
     * Command dispatcher (available after initialization).
     */
    val commandDispatcher: ICommandDispatcher?

    /**
     * Performance monitor (if enabled).
     */
    val performanceMonitor: PluginPerformanceMonitor?

    /**
     * Initialize the plugin system.
     *
     * @param config Configuration options
     * @return Setup result indicating success/failure
     */
    suspend fun initialize(config: PluginSystemConfig = PluginSystemConfig()): PluginSystemSetupResult

    /**
     * Shutdown the plugin system and release resources.
     */
    suspend fun shutdown()

    /**
     * Register a custom plugin factory.
     *
     * @param pluginId Unique plugin identifier
     * @param factory Factory function to create the plugin
     */
    fun registerPluginFactory(pluginId: String, factory: () -> Plugin)

    /**
     * Get a loaded plugin by ID.
     *
     * @param pluginId The plugin identifier
     * @return Plugin instance or null if not found
     */
    fun getPlugin(pluginId: String): Plugin?

    /**
     * Get all loaded plugins.
     *
     * @return List of loaded plugins
     */
    fun getLoadedPlugins(): List<Plugin>
}

/**
 * Command dispatcher interface for routing commands through plugins.
 */
interface ICommandDispatcher {
    /**
     * Dispatch a command to the appropriate handler plugin.
     *
     * @param command Command phrase
     * @param context Additional context (platform-specific)
     * @return Result of the command execution
     */
    suspend fun dispatch(command: String, context: Any? = null): DispatchResult
}

/**
 * Result of a command dispatch.
 */
sealed class DispatchResult {
    data class Success(val message: String) : DispatchResult()
    data class Error(val message: String) : DispatchResult()
    data class Ambiguous(val options: List<String>) : DispatchResult()
    data object NotHandled : DispatchResult()
}

/**
 * Factory for creating platform-specific setup instances.
 */
expect object PluginSystemSetup {
    /**
     * Create a platform-specific plugin system setup.
     *
     * @param platformContext Platform-specific context (e.g., Android Context)
     * @return Platform-specific IPluginSystemSetup implementation
     */
    fun create(platformContext: Any): IPluginSystemSetup
}
