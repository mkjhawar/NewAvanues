package com.avanues.avu.dsl.plugin

import com.avanues.avu.dsl.interpreter.AvuInterpreter
import com.avanues.avu.dsl.interpreter.ExecutionResult
import com.avanues.avu.dsl.interpreter.IAvuDispatcher
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Central registry for loaded AVU DSL plugins.
 *
 * Manages the full plugin lifecycle:
 * - **Register**: validate state, check trigger conflicts, index triggers
 * - **Activate/Deactivate**: control which plugins handle triggers
 * - **Handle triggers**: route incoming voice commands to the correct plugin
 * - **Unregister**: clean up plugin and trigger mappings
 *
 * Each plugin's triggers are exclusively owned — no two active plugins can
 * claim the same trigger pattern.
 */
class PluginRegistry(
    private val dispatcher: IAvuDispatcher
) : SynchronizedObject() {
    private val plugins = mutableMapOf<String, LoadedPlugin>()
    private val triggerIndex = mutableMapOf<String, String>() // trigger pattern → pluginId

    /**
     * Register a validated plugin.
     * Plugin must be in [PluginState.VALIDATED] state.
     */
    fun register(plugin: LoadedPlugin): PluginRegistrationResult = synchronized(this) {
        if (plugin.state != PluginState.VALIDATED) {
            return@synchronized PluginRegistrationResult.Error(
                "Plugin must be in VALIDATED state, got ${plugin.state}"
            )
        }

        // Check for ID conflicts
        val existing = plugins[plugin.pluginId]
        if (existing != null && existing.state.isRegistered) {
            return@synchronized PluginRegistrationResult.Error(
                "Plugin '${plugin.pluginId}' is already registered"
            )
        }

        // Check for trigger conflicts
        val conflicts = mutableListOf<String>()
        for (trigger in plugin.manifest.triggers) {
            val owner = triggerIndex[trigger]
            if (owner != null && owner != plugin.pluginId) {
                conflicts.add("Trigger '$trigger' already owned by plugin '$owner'")
            }
        }
        if (conflicts.isNotEmpty()) {
            return@synchronized PluginRegistrationResult.Conflict(conflicts)
        }

        // Register triggers
        for (trigger in plugin.manifest.triggers) {
            triggerIndex[trigger] = plugin.pluginId
        }

        val registered = plugin.withState(PluginState.REGISTERED)
        plugins[plugin.pluginId] = registered

        PluginRegistrationResult.Success(registered)
    }

    /**
     * Activate a registered plugin so it can handle triggers.
     */
    fun activate(pluginId: String): Boolean = synchronized(this) {
        val plugin = plugins[pluginId] ?: return@synchronized false
        if (plugin.state != PluginState.REGISTERED && plugin.state != PluginState.INACTIVE) return@synchronized false
        plugins[pluginId] = plugin.withState(PluginState.ACTIVE)
        true
    }

    /**
     * Deactivate a plugin (keeps registration but stops handling).
     */
    fun deactivate(pluginId: String): Boolean = synchronized(this) {
        val plugin = plugins[pluginId] ?: return@synchronized false
        if (plugin.state != PluginState.ACTIVE) return@synchronized false
        plugins[pluginId] = plugin.withState(PluginState.INACTIVE)
        true
    }

    /**
     * Unregister a plugin completely, removing all trigger mappings.
     */
    fun unregister(pluginId: String): Boolean = synchronized(this) {
        plugins.remove(pluginId) ?: return@synchronized false
        val toRemove = triggerIndex.entries.filter { it.value == pluginId }.map { it.key }
        toRemove.forEach { triggerIndex.remove(it) }
        true
    }

    /**
     * Handle an incoming trigger by finding the owning plugin and executing it.
     */
    suspend fun handleTrigger(
        pattern: String,
        captures: Map<String, String> = emptyMap()
    ): PluginTriggerResult {
        // Snapshot map state under lock before doing async interpreter work
        val plugin = synchronized(this) {
            val pluginId = triggerIndex[pattern]
                ?: return PluginTriggerResult.NoHandler(pattern)
            plugins[pluginId]
                ?: return PluginTriggerResult.Error("Plugin '$pluginId' not found")
        }

        if (!plugin.isActive) {
            return PluginTriggerResult.Error(
                "Plugin '${plugin.pluginId}' is not active (state: ${plugin.state})"
            )
        }

        val interpreter = AvuInterpreter(
            dispatcher = dispatcher,
            sandbox = plugin.sandboxConfig
        )

        return when (val result = interpreter.handleTrigger(plugin.ast, pattern, captures)) {
            is ExecutionResult.Success -> PluginTriggerResult.Success(
                pluginId = plugin.pluginId,
                returnValue = result.returnValue,
                executionTimeMs = result.executionTimeMs
            )
            is ExecutionResult.Failure -> {
                synchronized(this) {
                    plugins[plugin.pluginId] = plugin.withState(PluginState.ERROR, result.error.message)
                }
                PluginTriggerResult.Error(
                    "Plugin '${plugin.pluginId}' execution failed: ${result.error.message}"
                )
            }
            is ExecutionResult.NoHandler -> PluginTriggerResult.NoHandler(pattern)
        }
    }

    /** Find which plugin owns a trigger pattern. */
    fun findPluginForTrigger(pattern: String): LoadedPlugin? = synchronized(this) {
        val pluginId = triggerIndex[pattern] ?: return@synchronized null
        plugins[pluginId]
    }

    /** Get a plugin by ID. */
    fun getPlugin(pluginId: String): LoadedPlugin? = synchronized(this) { plugins[pluginId] }

    /** Get all registered plugins. */
    fun getAllPlugins(): List<LoadedPlugin> = synchronized(this) { plugins.values.toList() }

    /** Get active plugins only. */
    fun getActivePlugins(): List<LoadedPlugin> = synchronized(this) { plugins.values.filter { it.isActive } }

    /** Get all registered trigger patterns with their owning plugin IDs. */
    fun getRegisteredTriggers(): Map<String, String> = synchronized(this) { triggerIndex.toMap() }

    /** Get plugin count by state. */
    fun getStatistics(): Map<PluginState, Int> = synchronized(this) {
        plugins.values.groupBy { it.state }.mapValues { it.value.size }
    }

    /** Clear all plugins and triggers (for testing). */
    fun clear() = synchronized(this) {
        plugins.clear()
        triggerIndex.clear()
    }
}

sealed class PluginRegistrationResult {
    data class Success(val plugin: LoadedPlugin) : PluginRegistrationResult()
    data class Error(val message: String) : PluginRegistrationResult()
    data class Conflict(val conflicts: List<String>) : PluginRegistrationResult()

    val isSuccess: Boolean get() = this is Success
}

sealed class PluginTriggerResult {
    data class Success(
        val pluginId: String,
        val returnValue: Any?,
        val executionTimeMs: Long
    ) : PluginTriggerResult()
    data class Error(val message: String) : PluginTriggerResult()
    data class NoHandler(val pattern: String) : PluginTriggerResult()

    val isSuccess: Boolean get() = this is Success
}
