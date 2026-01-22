/**
 * PluginHotReloader.kt - Hot reload orchestrator for plugins
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Orchestrates the hot reload process when plugin files change on disk.
 * Watches for file changes and notifies listeners when plugins need reloading.
 *
 * Phase 5: Advanced features for developer experience.
 *
 * ## Implementation Status
 * - [x] File watching via FileSystemPluginDiscovery
 * - [x] Change event detection (add/remove/update)
 * - [x] Debouncing for rapid changes
 * - [x] Statistics tracking
 * - [x] Event callback system
 * - [ ] Full plugin reload (requires service endpoint management)
 * - [ ] State preservation across reloads
 *
 * ## Future Work
 * To enable full automatic reload:
 * 1. Store ServiceEndpoint for each plugin at registration
 * 2. Implement plugin unload → load cycle with endpoint preservation
 * 3. Add state serialization/deserialization to UniversalPlugin
 */
package com.augmentalis.magiccode.plugins.hotreload

import com.augmentalis.magiccode.plugins.discovery.FileSystemPluginDiscovery
import com.augmentalis.magiccode.plugins.discovery.PluginChangeEvent
import com.augmentalis.magiccode.plugins.discovery.PluginDescriptor
import com.augmentalis.magiccode.plugins.universal.PluginEvent
import com.augmentalis.magiccode.plugins.universal.PluginEventBus
import com.augmentalis.magiccode.plugins.universal.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Orchestrates hot reload of plugins when files change.
 *
 * ## Overview
 * The PluginHotReloader monitors plugin directories via FileSystemPluginDiscovery
 * and notifies listeners when plugins are added, removed, or updated. This provides
 * the foundation for hot reload functionality in development mode.
 *
 * ## Usage
 * ```kotlin
 * val hotReloader = PluginHotReloader(
 *     discovery = fileSystemDiscovery,
 *     eventBus = eventBus
 * )
 *
 * // Listen for reload events
 * hotReloader.onReload { event ->
 *     when (event) {
 *         is HotReloadEvent.PluginChanged -> {
 *             // Handle the change - reload logic goes here
 *             println("Plugin ${event.pluginId} changed: ${event.changeType}")
 *         }
 *         is HotReloadEvent.Enabled -> println("Hot reload enabled")
 *         is HotReloadEvent.Disabled -> println("Hot reload disabled")
 *     }
 * }
 *
 * // Enable hot reload
 * hotReloader.enable()
 *
 * // Disable when done
 * hotReloader.disable()
 * ```
 *
 * @param discovery FileSystemPluginDiscovery for watching changes
 * @param eventBus Event bus for publishing change events
 * @param scope CoroutineScope for async operations
 * @since 1.0.0
 */
class PluginHotReloader(
    private val discovery: FileSystemPluginDiscovery,
    private val eventBus: PluginEventBus,
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
    }
}

/**
 * Statistics about hot reload operations.
 */
data class HotReloadStats(
    val pluginsAdded: Int = 0,
    val pluginsRemoved: Int = 0,
    val updatesDetected: Int = 0
) {
    val totalChanges: Int get() = pluginsAdded + pluginsRemoved + updatesDetected
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
    object Enabled : HotReloadEvent()

    /** Hot reload was disabled. */
    object Disabled : HotReloadEvent()

    /**
     * A plugin file changed.
     *
     * The listener is responsible for performing the actual reload
     * using the provided descriptor.
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
}
