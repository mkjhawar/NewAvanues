package com.augmentalis.magiccode.plugins.universal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages plugin lifecycle transitions and health monitoring.
 *
 * This class is responsible for:
 * - Managing plugin lifecycle states (initialize, pause, resume, shutdown)
 * - Monitoring plugin health through periodic health checks
 * - Publishing state and health change events via the event bus
 * - Coordinating with the registry for state updates
 *
 * @param registry The plugin registry for state management
 * @param eventBus The event bus for publishing lifecycle events
 * @param scope The coroutine scope for async operations (defaults to SupervisorJob)
 */
class PluginLifecycleManager(
    private val registry: UniversalPluginRegistry,
    private val eventBus: PluginEventBus,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _managedPlugins = MutableStateFlow<Map<String, ManagedPlugin>>(emptyMap())

    /** Tracks the state-observer Job created per plugin in [manage], so it can be cancelled in [shutdown]. */
    private val observerJobs = mutableMapOf<String, Job>()

    /**
     * Observable state of all managed plugins.
     */
    val managedPlugins: StateFlow<Map<String, ManagedPlugin>> = _managedPlugins.asStateFlow()

    /**
     * Health check interval in milliseconds (30 seconds).
     */
    private val healthCheckIntervalMs = 30_000L

    /**
     * Job for periodic health checks.
     */
    private var healthCheckJob: Job? = null

    /**
     * Start managing a plugin's lifecycle.
     *
     * This registers the plugin for lifecycle management and begins
     * observing its state changes to publish events.
     *
     * @param plugin The plugin to manage
     * @param context The plugin context with runtime information
     * @return Result indicating success or failure
     */
    suspend fun manage(plugin: UniversalPlugin, context: PluginContext): Result<Unit> {
        val managed = ManagedPlugin(
            plugin = plugin,
            context = context,
            managedSince = currentTimeMillis()
        )

        _managedPlugins.value = _managedPlugins.value + (plugin.pluginId to managed)

        // Observe state changes and publish events; store the Job so it can be cancelled on shutdown
        val observerJob = scope.launch {
            plugin.stateFlow.collect { newState ->
                // Update registry with new state
                registry.updateState(plugin.pluginId, newState)

                // Publish state change event
                eventBus.publish(
                    PluginEvent(
                        eventId = "",
                        sourcePluginId = plugin.pluginId,
                        eventType = PluginEvent.TYPE_STATE_CHANGED,
                        payload = mapOf("state" to newState.name)
                    )
                )
            }
        }
        observerJobs[plugin.pluginId] = observerJob

        return Result.success(Unit)
    }

    /**
     * Initialize a plugin with the given configuration.
     *
     * @param pluginId The ID of the plugin to initialize
     * @param config The plugin configuration
     * @return InitResult indicating success or failure
     */
    suspend fun initialize(pluginId: String, config: PluginConfig): InitResult {
        val managed = _managedPlugins.value[pluginId]
            ?: return InitResult.Failure(
                error = IllegalArgumentException("Plugin not managed: $pluginId"),
                recoverable = false
            )

        return managed.plugin.initialize(config, managed.context)
    }

    /**
     * Pause a plugin to conserve resources.
     *
     * This is typically called when the app is backgrounded or when
     * the plugin is not actively needed.
     *
     * @param pluginId The ID of the plugin to pause
     * @return Result indicating success or failure
     */
    suspend fun pause(pluginId: String): Result<Unit> {
        val managed = _managedPlugins.value[pluginId]
            ?: return Result.failure(IllegalArgumentException("Plugin not managed: $pluginId"))

        return managed.plugin.pause()
    }

    /**
     * Resume a paused plugin.
     *
     * @param pluginId The ID of the plugin to resume
     * @return Result indicating success or failure
     */
    suspend fun resume(pluginId: String): Result<Unit> {
        val managed = _managedPlugins.value[pluginId]
            ?: return Result.failure(IllegalArgumentException("Plugin not managed: $pluginId"))

        return managed.plugin.resume()
    }

    /**
     * Update plugin configuration.
     *
     * Notifies the plugin of configuration changes and publishes
     * a config change event.
     *
     * @param pluginId The ID of the plugin to update
     * @param config The new configuration values
     */
    suspend fun updateConfig(pluginId: String, config: Map<String, Any>) {
        val managed = _managedPlugins.value[pluginId] ?: return

        managed.plugin.onConfigurationChanged(config)

        eventBus.publish(
            PluginEvent(
                eventId = "",
                sourcePluginId = pluginId,
                eventType = PluginEvent.TYPE_CONFIG_CHANGED,
                payloadJson = config.toString()
            )
        )
    }

    /**
     * Shutdown a plugin gracefully.
     *
     * This stops the plugin and removes it from lifecycle management.
     *
     * @param pluginId The ID of the plugin to shutdown
     * @return Result indicating success or failure
     */
    suspend fun shutdown(pluginId: String): Result<Unit> {
        val managed = _managedPlugins.value[pluginId]
            ?: return Result.failure(IllegalArgumentException("Plugin not managed: $pluginId"))

        val result = managed.plugin.shutdown()

        // Cancel the state-observer job and remove from managed plugins after shutdown
        observerJobs.remove(pluginId)?.cancel()
        _managedPlugins.value = _managedPlugins.value - pluginId

        return result
    }

    /**
     * Start periodic health checks for all managed plugins.
     *
     * Health checks run every 30 seconds and publish health change
     * events when a plugin's health status changes.
     */
    fun startHealthChecks() {
        // Cancel existing health check job if running
        healthCheckJob?.cancel()

        healthCheckJob = scope.launch {
            while (isActive) {
                checkAllPluginsHealth()
                delay(healthCheckIntervalMs)
            }
        }
    }

    /**
     * Stop periodic health checks.
     */
    fun stopHealthChecks() {
        healthCheckJob?.cancel()
        healthCheckJob = null
    }

    /**
     * Perform health check on all managed plugins.
     *
     * This is called periodically by startHealthChecks() and can also
     * be called manually for immediate health checks.
     */
    private suspend fun checkAllPluginsHealth() {
        _managedPlugins.value.forEach { (pluginId, managed) ->
            val health = managed.plugin.healthCheck()
            val previousHealth = managed.lastHealth

            // Check if health status changed
            if (previousHealth?.healthy != health.healthy) {
                eventBus.publish(
                    PluginEvent(
                        eventId = "",
                        sourcePluginId = pluginId,
                        eventType = PluginEvent.TYPE_HEALTH_CHANGED,
                        payload = mapOf(
                            "healthy" to health.healthy.toString(),
                            "message" to health.message
                        )
                    )
                )
            }

            // Update managed plugin with latest health status
            _managedPlugins.value = _managedPlugins.value +
                (pluginId to managed.copy(lastHealth = health))
        }
    }

    /**
     * Get the current health status of a managed plugin.
     *
     * @param pluginId The ID of the plugin
     * @return The last known health status, or null if not managed
     */
    fun getPluginHealth(pluginId: String): HealthStatus? {
        return _managedPlugins.value[pluginId]?.lastHealth
    }

    /**
     * Check if a plugin is currently being managed.
     *
     * @param pluginId The ID of the plugin to check
     * @return true if the plugin is managed
     */
    fun isManaged(pluginId: String): Boolean {
        return pluginId in _managedPlugins.value
    }

    /**
     * Get the count of currently managed plugins.
     */
    val managedCount: Int
        get() = _managedPlugins.value.size

    /**
     * Shutdown all managed plugins.
     *
     * This is typically called during application shutdown.
     *
     * @return List of plugin IDs that failed to shutdown
     */
    suspend fun shutdownAll(): List<String> {
        stopHealthChecks()

        val failures = mutableListOf<String>()

        _managedPlugins.value.keys.toList().forEach { pluginId ->
            val result = shutdown(pluginId)
            if (result.isFailure) {
                failures.add(pluginId)
            }
        }

        return failures
    }
}

/**
 * Represents a plugin that is being managed by the PluginLifecycleManager.
 *
 * @param plugin The managed plugin instance
 * @param context The plugin's runtime context
 * @param managedSince Timestamp when management began
 * @param lastHealth The most recent health check result
 */
data class ManagedPlugin(
    val plugin: UniversalPlugin,
    val context: PluginContext,
    val managedSince: Long,
    val lastHealth: HealthStatus? = null
)
