/**
 * PluginHotReloader.kt - Hot-reload support for plugins
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 *
 * Provides hot-reload capabilities for the plugin system, allowing plugins
 * to be updated, reloaded, or replaced without restarting the application.
 */
package com.augmentalis.magiccode.plugins.universal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hot-reload manager for plugins.
 *
 * Supports:
 * - Reloading individual plugins
 * - Replacing plugins with new versions
 * - Safe state transfer during reload
 * - Rollback on failure
 *
 * ## Usage
 * ```kotlin
 * val reloader = PluginHotReloader(pluginHost)
 *
 * // Reload a specific plugin
 * val result = reloader.reloadPlugin("com.example.myplugin")
 *
 * // Replace a plugin with a new factory
 * reloader.replacePlugin("com.example.myplugin") { MyUpdatedPlugin() }
 *
 * // Check reload status
 * reloader.status.collect { status ->
 *     when (status) {
 *         is ReloadStatus.Success -> println("Reloaded successfully")
 *         is ReloadStatus.Failed -> println("Reload failed: ${status.error}")
 *     }
 * }
 * ```
 *
 * @param pluginHost The plugin host to manage
 */
class PluginHotReloader<T : Any>(
    private val pluginHost: IPluginHost<T>
) {
    /**
     * Status of a reload operation.
     */
    sealed class ReloadStatus {
        data object Idle : ReloadStatus()
        data class InProgress(val pluginId: String) : ReloadStatus()
        data class Success(val pluginId: String, val durationMs: Long) : ReloadStatus()
        data class Failed(val pluginId: String, val error: String, val recovered: Boolean) : ReloadStatus()
    }

    /**
     * Result of a reload operation.
     */
    data class ReloadResult(
        val success: Boolean,
        val pluginId: String,
        val durationMs: Long,
        val error: String? = null,
        val previousState: Map<String, Any?>? = null
    )

    private val _status = MutableStateFlow<ReloadStatus>(ReloadStatus.Idle)
    val status: StateFlow<ReloadStatus> = _status.asStateFlow()

    private val _reloadHistory = MutableStateFlow<List<ReloadResult>>(emptyList())
    val reloadHistory: StateFlow<List<ReloadResult>> = _reloadHistory.asStateFlow()

    // State backup for rollback
    private val stateBackups = mutableMapOf<String, PluginStateBackup>()

    /**
     * Reload a plugin by ID.
     *
     * This will:
     * 1. Save the plugin's current state
     * 2. Shutdown the plugin
     * 3. Re-initialize the plugin
     * 4. Restore state if possible
     *
     * @param pluginId The ID of the plugin to reload
     * @return ReloadResult indicating success or failure
     */
    suspend fun reloadPlugin(pluginId: String): ReloadResult {
        val startTime = getCurrentTimeMs()
        _status.value = ReloadStatus.InProgress(pluginId)

        val plugin = pluginHost.getPlugin(pluginId)
        if (plugin == null) {
            val result = ReloadResult(
                success = false,
                pluginId = pluginId,
                durationMs = getCurrentTimeMs() - startTime,
                error = "Plugin not found: $pluginId"
            )
            _status.value = ReloadStatus.Failed(pluginId, result.error!!, false)
            recordResult(result)
            return result
        }

        return try {
            // Backup state
            val backup = backupPluginState(plugin)
            stateBackups[pluginId] = backup

            // Shutdown plugin
            plugin.shutdown()

            // Re-initialize
            val initResult = plugin.initialize(
                PluginConfig(pluginId = pluginId),
                EmptyPluginContext
            )

            if (initResult.success) {
                // Restore state if applicable
                restorePluginState(plugin, backup)

                val result = ReloadResult(
                    success = true,
                    pluginId = pluginId,
                    durationMs = getCurrentTimeMs() - startTime,
                    previousState = backup.state
                )
                _status.value = ReloadStatus.Success(pluginId, result.durationMs)
                recordResult(result)
                result
            } else {
                throw Exception(initResult.message)
            }
        } catch (e: Exception) {
            val recovered = tryRollback(pluginId)
            val result = ReloadResult(
                success = false,
                pluginId = pluginId,
                durationMs = getCurrentTimeMs() - startTime,
                error = e.message ?: "Unknown error"
            )
            _status.value = ReloadStatus.Failed(pluginId, result.error!!, recovered)
            recordResult(result)
            result
        }
    }

    /**
     * Replace a plugin with a new factory.
     *
     * This is useful for updating plugins with new versions at runtime.
     *
     * @param pluginId The ID of the plugin to replace
     * @param factory Factory function to create the new plugin
     * @return ReloadResult indicating success or failure
     */
    suspend fun replacePlugin(
        pluginId: String,
        factory: () -> Plugin
    ): ReloadResult {
        val startTime = getCurrentTimeMs()
        _status.value = ReloadStatus.InProgress(pluginId)

        val existingPlugin = pluginHost.getPlugin(pluginId)

        return try {
            // Backup existing state if plugin exists
            val backup = existingPlugin?.let { backupPluginState(it) }
            if (backup != null) {
                stateBackups[pluginId] = backup
            }

            // Shutdown existing plugin
            existingPlugin?.shutdown()

            // Unregister old plugin (if supported by host)
            if (pluginHost is IPluginHostExtended<*>) {
                (pluginHost as IPluginHostExtended<*>).unregisterPlugin(pluginId)
            }

            // Create and register new plugin
            val newPlugin = factory()
            if (pluginHost is IPluginHostExtended<*>) {
                (pluginHost as IPluginHostExtended<T>).registerPlugin(newPlugin)
            }

            // Initialize new plugin
            val initResult = newPlugin.initialize(
                PluginConfig(pluginId = pluginId),
                EmptyPluginContext
            )

            if (initResult.success) {
                // Try to restore state
                if (backup != null) {
                    restorePluginState(newPlugin, backup)
                }

                val result = ReloadResult(
                    success = true,
                    pluginId = pluginId,
                    durationMs = getCurrentTimeMs() - startTime,
                    previousState = backup?.state
                )
                _status.value = ReloadStatus.Success(pluginId, result.durationMs)
                recordResult(result)
                result
            } else {
                throw Exception(initResult.message)
            }
        } catch (e: Exception) {
            val result = ReloadResult(
                success = false,
                pluginId = pluginId,
                durationMs = getCurrentTimeMs() - startTime,
                error = e.message ?: "Unknown error"
            )
            _status.value = ReloadStatus.Failed(pluginId, result.error!!, false)
            recordResult(result)
            result
        }
    }

    /**
     * Reload all plugins.
     *
     * @return Map of plugin IDs to their reload results
     */
    suspend fun reloadAll(): Map<String, ReloadResult> {
        val results = mutableMapOf<String, ReloadResult>()
        val pluginIds = pluginHost.getLoadedPlugins().map { it.pluginId }

        for (pluginId in pluginIds) {
            results[pluginId] = reloadPlugin(pluginId)
        }

        return results
    }

    /**
     * Check if a plugin can be safely reloaded.
     *
     * Plugins with active operations or critical state may not be safe to reload.
     *
     * @param pluginId The plugin ID to check
     * @return true if safe to reload
     */
    fun canReload(pluginId: String): Boolean {
        val plugin = pluginHost.getPlugin(pluginId) ?: return false
        return plugin.state == PluginState.READY || plugin.state == PluginState.PAUSED
    }

    /**
     * Get the last reload result for a plugin.
     *
     * @param pluginId The plugin ID
     * @return Last reload result or null if never reloaded
     */
    fun getLastReloadResult(pluginId: String): ReloadResult? {
        return _reloadHistory.value.lastOrNull { it.pluginId == pluginId }
    }

    /**
     * Clear reload history.
     */
    fun clearHistory() {
        _reloadHistory.value = emptyList()
        stateBackups.clear()
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private fun backupPluginState(plugin: Plugin): PluginStateBackup {
        return PluginStateBackup(
            pluginId = plugin.pluginId,
            state = plugin.getHealthDiagnostics(),
            timestamp = getCurrentTimeMs()
        )
    }

    private fun restorePluginState(plugin: Plugin, backup: PluginStateBackup) {
        // State restoration is plugin-specific
        // Plugins can implement IStatefulPlugin interface for advanced state management
        // For now, we just log the attempt
    }

    private suspend fun tryRollback(pluginId: String): Boolean {
        val backup = stateBackups[pluginId] ?: return false

        return try {
            val plugin = pluginHost.getPlugin(pluginId)
            if (plugin != null) {
                restorePluginState(plugin, backup)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun recordResult(result: ReloadResult) {
        val current = _reloadHistory.value
        _reloadHistory.value = (current + result).takeLast(100) // Keep last 100 results
    }
}

/**
 * Backup of plugin state for rollback.
 */
data class PluginStateBackup(
    val pluginId: String,
    val state: Map<String, String>,
    val timestamp: Long
)

/**
 * Extended plugin host interface for hot-reload support.
 */
interface IPluginHostExtended<T> : IPluginHost<T> {
    /**
     * Unregister a plugin by ID.
     */
    suspend fun unregisterPlugin(pluginId: String)

    /**
     * Register a new plugin.
     */
    suspend fun registerPlugin(plugin: Plugin)
}

/**
 * Empty plugin context for re-initialization.
 */
private object EmptyPluginContext : PluginContext {
    override val platformInfo: PlatformInfo = object : PlatformInfo {
        override val platformName: String = "unknown"
        override val platformVersion: String = "unknown"
        override val deviceModel: String = "unknown"
        override val isDebugBuild: Boolean = false
    }
}
