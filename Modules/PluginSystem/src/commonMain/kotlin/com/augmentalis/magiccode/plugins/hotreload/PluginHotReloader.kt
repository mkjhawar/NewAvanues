/**
 * PluginHotReloader.kt - Hot reload orchestrator for plugins
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Orchestrates the hot reload process when plugin files change on disk.
 * Watches for file changes and notifies listeners when plugins need reloading.
 * Supports full reload cycle with state preservation across reloads.
 *
 * ## Implementation Status
 * - [x] File watching via FileSystemPluginDiscovery
 * - [x] Change event detection (add/remove/update)
 * - [x] Debouncing for rapid changes
 * - [x] Statistics tracking
 * - [x] Event callback system
 * - [x] Full plugin reload with service endpoint management
 * - [x] State preservation across reloads
 * - [x] Rollback on failure
 *
 * ## Reload Process
 * 1. Save plugin state via plugin.saveState()
 * 2. Shutdown the current plugin
 * 3. Unregister from registry
 * 4. Refresh discovery to find updated plugin
 * 5. Load and register new plugin
 * 6. Initialize new plugin
 * 7. Restore state via plugin.restoreState()
 * 8. On failure: rollback to previous state if possible
 */
package com.augmentalis.magiccode.plugins.hotreload

import com.augmentalis.magiccode.plugins.discovery.FileSystemPluginDiscovery
import com.augmentalis.magiccode.plugins.discovery.PluginChangeEvent
import com.augmentalis.magiccode.plugins.discovery.PluginDescriptor
import com.augmentalis.magiccode.plugins.universal.InitResult
import com.augmentalis.magiccode.plugins.universal.PluginConfig
import com.augmentalis.magiccode.plugins.universal.PluginContext
import com.augmentalis.magiccode.plugins.universal.PluginEvent
import com.augmentalis.magiccode.plugins.universal.PluginEventBus
import com.augmentalis.magiccode.plugins.universal.PluginLifecycleManager
import com.augmentalis.magiccode.plugins.universal.PluginState
import com.augmentalis.magiccode.plugins.universal.PluginStateSerializer
import com.augmentalis.magiccode.plugins.universal.JsonPluginStateSerializer
import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import com.augmentalis.magiccode.plugins.universal.UniversalPluginRegistry
import com.augmentalis.magiccode.plugins.universal.currentTimeMillis
import com.augmentalis.universalrpc.ServiceEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestrates hot reload of plugins when files change.
 *
 * ## Overview
 * The PluginHotReloader monitors plugin directories via FileSystemPluginDiscovery
 * and handles the full reload cycle when plugins are added, removed, or updated.
 * It preserves plugin state across reloads and provides rollback on failure.
 *
 * ## Usage
 * ```kotlin
 * val hotReloader = PluginHotReloader(
 *     discovery = fileSystemDiscovery,
 *     eventBus = eventBus,
 *     registry = pluginRegistry,
 *     lifecycleManager = lifecycleManager
 * )
 *
 * // Listen for reload events
 * hotReloader.onReload { event ->
 *     when (event) {
 *         is HotReloadEvent.PluginChanged -> {
 *             println("Plugin ${event.pluginId} changed: ${event.changeType}")
 *         }
 *         is HotReloadEvent.ReloadCompleted -> {
 *             println("Reload completed for ${event.pluginId}")
 *         }
 *         is HotReloadEvent.ReloadFailed -> {
 *             println("Reload failed for ${event.pluginId}: ${event.error}")
 *         }
 *         is HotReloadEvent.Enabled -> println("Hot reload enabled")
 *         is HotReloadEvent.Disabled -> println("Hot reload disabled")
 *     }
 * }
 *
 * // Enable hot reload
 * hotReloader.enable()
 *
 * // Manual reload of a specific plugin
 * val result = hotReloader.reloadPlugin("com.example.myplugin")
 *
 * // Disable when done
 * hotReloader.disable()
 * ```
 *
 * @param discovery FileSystemPluginDiscovery for watching changes
 * @param eventBus Event bus for publishing change events
 * @param registry Plugin registry for managing registrations
 * @param lifecycleManager Lifecycle manager for plugin state management
 * @param stateSerializer Serializer for plugin state (optional)
 * @param scope CoroutineScope for async operations
 * @since 1.0.0
 */
class PluginHotReloader(
    private val discovery: FileSystemPluginDiscovery,
    private val eventBus: PluginEventBus,
    private val registry: UniversalPluginRegistry? = null,
    private val lifecycleManager: PluginLifecycleManager? = null,
    private val stateSerializer: PluginStateSerializer = JsonPluginStateSerializer.Default,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    /**
     * Current enabled state.
     */
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /**
     * Statistics about hot reloads.
     */
    private val _stats = MutableStateFlow(HotReloadStats())
    val stats: StateFlow<HotReloadStats> = _stats.asStateFlow()

    /**
     * Last change times for rate limiting.
     */
    private val lastChangeTimes = mutableMapOf<String, Long>()

    /**
     * Minimum time between handling changes for the same plugin (debounce).
     */
    var debounceMs: Long = DEFAULT_DEBOUNCE_MS

    /**
     * Callback for reload events.
     */
    private var onReloadCallback: ((HotReloadEvent) -> Unit)? = null

    /**
     * Mutex for preventing concurrent reloads of the same plugin.
     */
    private val reloadMutex = Mutex()

    /**
     * Map of plugins currently being reloaded to prevent concurrent reloads.
     */
    private val reloadingPlugins = mutableSetOf<String>()

    /**
     * Cache of plugin instances for rollback purposes.
     */
    private val pluginCache = mutableMapOf<String, CachedPluginData>()

    /**
     * Plugin configuration cache for reload.
     */
    private val configCache = mutableMapOf<String, PluginConfig>()

    /**
     * Plugin context cache for reload.
     */
    private val contextCache = mutableMapOf<String, PluginContext>()

    /**
     * Enable hot reload monitoring.
     *
     * Starts watching for file changes and notifying listeners.
     */
    fun enable() {
        if (_enabled.value) return
        _enabled.value = true

        discovery.watchForChanges(scope) { event ->
            scope.launch {
                handleChangeEvent(event)
            }
        }

        emitEvent(HotReloadEvent.Enabled)
    }

    /**
     * Disable hot reload monitoring.
     *
     * Stops watching for file changes.
     */
    fun disable() {
        if (!_enabled.value) return
        _enabled.value = false

        discovery.stopWatching()
        emitEvent(HotReloadEvent.Disabled)
    }

    /**
     * Set callback for reload events.
     *
     * @param callback Function to call on reload events
     */
    fun onReload(callback: (HotReloadEvent) -> Unit) {
        onReloadCallback = callback
    }

    /**
     * Cache plugin configuration for reload.
     *
     * @param pluginId Plugin ID
     * @param config Plugin configuration
     */
    fun cacheConfig(pluginId: String, config: PluginConfig) {
        configCache[pluginId] = config
    }

    /**
     * Cache plugin context for reload.
     *
     * @param pluginId Plugin ID
     * @param context Plugin context
     */
    fun cacheContext(pluginId: String, context: PluginContext) {
        contextCache[pluginId] = context
    }

    /**
     * Reload a specific plugin with state preservation.
     *
     * Performs a full reload cycle:
     * 1. Get current plugin from registry
     * 2. Save state via plugin.saveState()
     * 3. Shutdown plugin
     * 4. Unregister from registry
     * 5. Refresh discovery
     * 6. Find updated plugin descriptor
     * 7. Load and register new plugin
     * 8. Initialize new plugin
     * 9. Restore state via plugin.restoreState()
     * 10. On failure: rollback to previous state
     *
     * @param pluginId The ID of the plugin to reload
     * @return Result indicating success or failure
     */
    suspend fun reloadPlugin(pluginId: String): Result<Unit> = reloadMutex.withLock {
        // Check if plugin is already being reloaded
        if (pluginId in reloadingPlugins) {
            return@withLock Result.failure(
                ReloadException("Plugin $pluginId is already being reloaded", pluginId)
            )
        }

        reloadingPlugins.add(pluginId)

        try {
            performReload(pluginId)
        } finally {
            reloadingPlugins.remove(pluginId)
        }
    }

    /**
     * Internal reload implementation.
     */
    private suspend fun performReload(pluginId: String): Result<Unit> {
        val startTime = currentTimeMillis()

        // Validate dependencies
        if (registry == null || lifecycleManager == null) {
            val error = ReloadException(
                "Registry and LifecycleManager are required for reload",
                pluginId
            )
            emitEvent(HotReloadEvent.ReloadFailed(pluginId, error.message ?: "Unknown error"))
            return Result.failure(error)
        }

        // Step 1: Get current plugin registration
        val currentRegistration = registry.getPlugin(pluginId)
        if (currentRegistration == null) {
            val error = ReloadException("Plugin $pluginId is not registered", pluginId)
            emitEvent(HotReloadEvent.ReloadFailed(pluginId, error.message ?: "Unknown error"))
            return Result.failure(error)
        }

        // Get the current plugin instance from lifecycle manager
        val managedPlugins = lifecycleManager.managedPlugins.value
        val managedPlugin = managedPlugins[pluginId]
        val currentPlugin = managedPlugin?.plugin

        if (currentPlugin == null) {
            val error = ReloadException("Plugin $pluginId is not managed", pluginId)
            emitEvent(HotReloadEvent.ReloadFailed(pluginId, error.message ?: "Unknown error"))
            return Result.failure(error)
        }

        // Step 2: Save plugin state
        val savedState: Map<String, Any> = try {
            currentPlugin.saveState()
        } catch (e: Exception) {
            emptyMap() // Continue with empty state if save fails
        }

        // Cache current data for potential rollback
        val cachedData = CachedPluginData(
            plugin = currentPlugin,
            endpoint = currentRegistration.endpoint,
            state = savedState,
            config = configCache[pluginId] ?: PluginConfig.EMPTY,
            context = contextCache[pluginId] ?: managedPlugin.context
        )
        pluginCache[pluginId] = cachedData

        try {
            // Step 3: Shutdown current plugin
            val shutdownResult = lifecycleManager.shutdown(pluginId)
            if (shutdownResult.isFailure) {
                // Log but continue - we want to try reloading anyway
            }

            // Step 4: Unregister from registry
            registry.unregister(pluginId)

            // Step 5: Refresh discovery
            discovery.refresh()

            // Step 6: Find updated plugin descriptor
            val descriptors = discovery.discoverPlugins()
            val newDescriptor = descriptors.find { it.pluginId == pluginId }

            if (newDescriptor == null) {
                // Rollback - plugin no longer available
                return rollback(pluginId, "Plugin descriptor not found after refresh", cachedData)
            }

            // Step 7: Load new plugin
            val loadResult = discovery.loadPlugin(newDescriptor)
            if (loadResult.isFailure) {
                return rollback(
                    pluginId,
                    "Failed to load plugin: ${loadResult.exceptionOrNull()?.message}",
                    cachedData
                )
            }

            val newPlugin = loadResult.getOrThrow()

            // Create endpoint for new plugin (reuse port from cached if available)
            val newEndpoint = cachedData.endpoint.copy(
                serviceName = pluginId
            )

            // Step 8: Register new plugin
            val registerResult = registry.register(newPlugin, newEndpoint)
            if (registerResult.isFailure) {
                return rollback(
                    pluginId,
                    "Failed to register plugin: ${registerResult.exceptionOrNull()?.message}",
                    cachedData
                )
            }

            // Manage with lifecycle manager
            lifecycleManager.manage(newPlugin, cachedData.context)

            // Initialize new plugin
            val initResult = lifecycleManager.initialize(pluginId, cachedData.config)
            if (initResult is InitResult.Failure) {
                return rollback(
                    pluginId,
                    "Failed to initialize plugin: ${initResult.message}",
                    cachedData
                )
            }

            // Step 9: Restore state
            if (savedState.isNotEmpty()) {
                val restoreResult = newPlugin.restoreState(savedState)
                if (restoreResult.isFailure) {
                    // Log but don't fail - plugin is running, just without restored state
                }
            }

            // Success
            val duration = currentTimeMillis() - startTime
            updateStats {
                copy(
                    successfulReloads = successfulReloads + 1,
                    lastReloadDurationMs = duration
                )
            }

            // Publish success event
            eventBus.publish(
                PluginEvent(
                    eventId = "hotreload_complete_${currentTimeMillis()}",
                    sourcePluginId = SYSTEM_PLUGIN_ID,
                    eventType = TYPE_PLUGIN_RELOADED,
                    payload = mapOf(
                        "pluginId" to pluginId,
                        "durationMs" to duration.toString(),
                        "stateRestored" to savedState.isNotEmpty().toString()
                    )
                )
            )

            emitEvent(HotReloadEvent.ReloadCompleted(pluginId, duration))

            // Clear cache on success
            pluginCache.remove(pluginId)

            return Result.success(Unit)
        } catch (e: Exception) {
            return rollback(pluginId, e.message ?: "Unknown error", cachedData)
        }
    }

    /**
     * Attempt to rollback to previous plugin state.
     */
    private suspend fun rollback(
        pluginId: String,
        errorMessage: String,
        cachedData: CachedPluginData
    ): Result<Unit> {
        updateStats { copy(failedReloads = failedReloads + 1) }

        // Try to restore the old plugin
        try {
            if (registry != null && lifecycleManager != null) {
                val registerResult = registry.register(cachedData.plugin, cachedData.endpoint)
                if (registerResult.isSuccess) {
                    lifecycleManager.manage(cachedData.plugin, cachedData.context)
                    lifecycleManager.initialize(pluginId, cachedData.config)

                    if (cachedData.state.isNotEmpty()) {
                        cachedData.plugin.restoreState(cachedData.state)
                    }

                    updateStats { copy(rollbacks = rollbacks + 1) }
                }
            }
        } catch (e: Exception) {
            // Rollback failed - plugin is in unknown state
        }

        val error = ReloadException(errorMessage, pluginId)
        emitEvent(HotReloadEvent.ReloadFailed(pluginId, errorMessage))

        return Result.failure(error)
    }

    /**
     * Handle a change event from discovery.
     */
    private suspend fun handleChangeEvent(event: PluginChangeEvent) {
        when (event) {
            is PluginChangeEvent.Added -> handlePluginAdded(event.descriptor)
            is PluginChangeEvent.Removed -> handlePluginRemoved(event.pluginId)
            is PluginChangeEvent.Updated -> handlePluginUpdated(event.descriptor)
        }
    }

    /**
     * Handle a new plugin being added.
     */
    private suspend fun handlePluginAdded(descriptor: PluginDescriptor) {
        updateStats { copy(pluginsAdded = pluginsAdded + 1) }

        // Publish event to event bus
        eventBus.publish(
            PluginEvent(
                eventId = "hotreload_add_${currentTimeMillis()}",
                sourcePluginId = SYSTEM_PLUGIN_ID,
                eventType = TYPE_PLUGIN_DISCOVERED,
                payload = mapOf(
                    "pluginId" to descriptor.pluginId,
                    "version" to descriptor.version,
                    "changeType" to "added"
                )
            )
        )

        emitEvent(HotReloadEvent.PluginChanged(
            pluginId = descriptor.pluginId,
            changeType = ChangeType.ADDED,
            descriptor = descriptor
        ))
    }

    /**
     * Handle a plugin being removed.
     */
    private suspend fun handlePluginRemoved(pluginId: String) {
        updateStats { copy(pluginsRemoved = pluginsRemoved + 1) }

        // Publish event to event bus
        eventBus.publish(
            PluginEvent(
                eventId = "hotreload_remove_${currentTimeMillis()}",
                sourcePluginId = SYSTEM_PLUGIN_ID,
                eventType = TYPE_PLUGIN_REMOVED,
                payload = mapOf(
                    "pluginId" to pluginId,
                    "changeType" to "removed"
                )
            )
        )

        emitEvent(HotReloadEvent.PluginChanged(
            pluginId = pluginId,
            changeType = ChangeType.REMOVED,
            descriptor = null
        ))
    }

    /**
     * Handle a plugin being updated.
     */
    private suspend fun handlePluginUpdated(descriptor: PluginDescriptor) {
        // Debounce rapid updates
        val now = currentTimeMillis()
        val lastChange = lastChangeTimes[descriptor.pluginId] ?: 0L
        if (now - lastChange < debounceMs) {
            return // Skip this update, too soon
        }
        lastChangeTimes[descriptor.pluginId] = now

        updateStats { copy(updatesDetected = updatesDetected + 1) }

        // Publish event to event bus
        eventBus.publish(
            PluginEvent(
                eventId = "hotreload_update_${currentTimeMillis()}",
                sourcePluginId = SYSTEM_PLUGIN_ID,
                eventType = TYPE_PLUGIN_UPDATED,
                payload = mapOf(
                    "pluginId" to descriptor.pluginId,
                    "version" to descriptor.version,
                    "changeType" to "updated"
                )
            )
        )

        emitEvent(HotReloadEvent.PluginChanged(
            pluginId = descriptor.pluginId,
            changeType = ChangeType.UPDATED,
            descriptor = descriptor
        ))
    }

    /**
     * Update statistics atomically.
     */
    private fun updateStats(update: HotReloadStats.() -> HotReloadStats) {
        _stats.value = _stats.value.update()
    }

    /**
     * Emit an event to callback.
     */
    private fun emitEvent(event: HotReloadEvent) {
        onReloadCallback?.invoke(event)
    }

    /**
     * Get current statistics.
     */
    fun getStats(): HotReloadStats = _stats.value

    /**
     * Reset statistics.
     */
    fun resetStats() {
        _stats.value = HotReloadStats()
    }

    /**
     * Check if hot reload is currently enabled.
     */
    fun isEnabled(): Boolean = _enabled.value

    /**
     * Check if a plugin is currently being reloaded.
     *
     * @param pluginId Plugin ID to check
     * @return true if the plugin is being reloaded
     */
    fun isReloading(pluginId: String): Boolean = pluginId in reloadingPlugins

    companion object {
        /** Default debounce time in milliseconds. */
        const val DEFAULT_DEBOUNCE_MS = 1000L

        /** System plugin ID for hot reload events. */
        const val SYSTEM_PLUGIN_ID = "system.hotreload"

        /** Event type for plugin discovered. */
        const val TYPE_PLUGIN_DISCOVERED = "hotreload.plugin.discovered"

        /** Event type for plugin updated. */
        const val TYPE_PLUGIN_UPDATED = "hotreload.plugin.updated"

        /** Event type for plugin removed. */
        const val TYPE_PLUGIN_REMOVED = "hotreload.plugin.removed"

        /** Event type for plugin reloaded successfully. */
        const val TYPE_PLUGIN_RELOADED = "hotreload.plugin.reloaded"
    }
}

/**
 * Statistics about hot reload operations.
 *
 * @property pluginsAdded Number of plugins added
 * @property pluginsRemoved Number of plugins removed
 * @property updatesDetected Number of updates detected
 * @property successfulReloads Number of successful full reloads
 * @property failedReloads Number of failed reload attempts
 * @property rollbacks Number of rollback operations performed
 * @property lastReloadDurationMs Duration of the last reload in milliseconds
 */
data class HotReloadStats(
    val pluginsAdded: Int = 0,
    val pluginsRemoved: Int = 0,
    val updatesDetected: Int = 0,
    val successfulReloads: Int = 0,
    val failedReloads: Int = 0,
    val rollbacks: Int = 0,
    val lastReloadDurationMs: Long = 0
) {
    /** Total number of changes detected. */
    val totalChanges: Int get() = pluginsAdded + pluginsRemoved + updatesDetected

    /** Total reload attempts. */
    val totalReloadAttempts: Int get() = successfulReloads + failedReloads

    /** Reload success rate (0.0 to 1.0). */
    val successRate: Float
        get() = if (totalReloadAttempts > 0) {
            successfulReloads.toFloat() / totalReloadAttempts
        } else {
            1.0f
        }
}

/**
 * Type of change detected.
 */
enum class ChangeType {
    ADDED,
    REMOVED,
    UPDATED
}

/**
 * Events emitted during hot reload operations.
 */
sealed class HotReloadEvent {
    /** Hot reload was enabled. */
    data object Enabled : HotReloadEvent()

    /** Hot reload was disabled. */
    data object Disabled : HotReloadEvent()

    /**
     * A plugin file changed.
     *
     * The listener is responsible for deciding whether to perform
     * a full reload using the provided descriptor.
     *
     * @property pluginId The plugin that changed
     * @property changeType Type of change (ADDED, REMOVED, UPDATED)
     * @property descriptor Updated descriptor (null for REMOVED)
     */
    data class PluginChanged(
        val pluginId: String,
        val changeType: ChangeType,
        val descriptor: PluginDescriptor?
    ) : HotReloadEvent()

    /**
     * A plugin was successfully reloaded.
     *
     * @property pluginId The plugin that was reloaded
     * @property durationMs Time taken to reload in milliseconds
     */
    data class ReloadCompleted(
        val pluginId: String,
        val durationMs: Long
    ) : HotReloadEvent()

    /**
     * A plugin reload failed.
     *
     * @property pluginId The plugin that failed to reload
     * @property error Error message describing the failure
     */
    data class ReloadFailed(
        val pluginId: String,
        val error: String
    ) : HotReloadEvent()
}

/**
 * Exception thrown during hot reload operations.
 *
 * @param message Error message
 * @param pluginId Plugin ID that failed to reload
 * @param cause Underlying cause
 */
class ReloadException(
    message: String,
    val pluginId: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Cached plugin data for rollback purposes.
 *
 * @property plugin The plugin instance
 * @property endpoint The service endpoint
 * @property state The saved plugin state
 * @property config The plugin configuration
 * @property context The plugin context
 */
private data class CachedPluginData(
    val plugin: UniversalPlugin,
    val endpoint: ServiceEndpoint,
    val state: Map<String, Any>,
    val config: PluginConfig,
    val context: PluginContext
)
