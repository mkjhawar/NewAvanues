/**
 * PluginHandlerBridge.kt - Bridge between plugin and legacy handler systems
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 *
 * Provides a bridge layer that routes commands through the plugin system first,
 * then falls back to legacy handlers if needed. This enables gradual migration
 * from the old handler architecture to the Universal Plugin Architecture.
 *
 * ## Migration Strategy
 * 1. Commands are first routed to the plugin-based dispatcher
 * 2. If no plugin handles the command (confidence < threshold), fall back to legacy
 * 3. Legacy handlers can be deprecated one-by-one as plugin coverage increases
 * 4. Eventually, the legacy path can be removed entirely
 */
package com.augmentalis.magiccode.plugins.android

import android.util.Log
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.*
import com.augmentalis.voiceoscore.ActionResult
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridge between the plugin system and legacy handlers.
 *
 * Routes commands through plugins first, then falls back to legacy handlers
 * if needed. Provides metrics for monitoring migration progress.
 *
 * ## Usage
 * ```kotlin
 * val bridge = PluginHandlerBridge(
 *     pluginDispatcher = app.pluginCommandDispatcher,
 *     legacyDispatcher = { command, context -> legacyHandlerRegistry.dispatch(command, context) }
 * )
 *
 * val result = bridge.dispatch(command, context)
 * ```
 *
 * @param pluginDispatcher The plugin-based command dispatcher
 * @param legacyDispatcher Optional legacy handler dispatcher for fallback
 */
class PluginHandlerBridge(
    private val pluginDispatcher: PluginCommandDispatcher,
    private val legacyDispatcher: (suspend (QuantizedCommand, HandlerContext) -> ActionResult)? = null
) {
    private val tag = "PluginHandlerBridge"

    /**
     * Minimum confidence required to use plugin handler.
     * Commands below this threshold fall back to legacy.
     */
    var minPluginConfidence: Float = 0.7f

    /**
     * Enable legacy fallback when plugin handlers don't match.
     */
    var enableLegacyFallback: Boolean = true

    /**
     * Enable logging for routing decisions.
     */
    var debugLogging: Boolean = false

    /**
     * Bridge metrics for monitoring migration progress.
     */
    data class BridgeMetrics(
        val totalCommands: Long = 0,
        val pluginHandled: Long = 0,
        val legacyFallback: Long = 0,
        val unhandled: Long = 0,
        val pluginCoveragePercent: Float = 0f
    )

    private val _metrics = MutableStateFlow(BridgeMetrics())
    val metrics: StateFlow<BridgeMetrics> = _metrics.asStateFlow()

    /**
     * Dispatch a command through the bridge.
     *
     * 1. First tries plugin handlers
     * 2. If plugin confidence is below threshold or returns error, falls back to legacy
     * 3. If both fail, returns unhandled error
     *
     * @param command The command to execute
     * @param context Handler context with screen state
     * @return ActionResult from the handler
     */
    suspend fun dispatch(
        command: QuantizedCommand,
        context: HandlerContext
    ): ActionResult {
        if (debugLogging) {
            Log.d(tag, "Dispatching command: ${command.phrase}")
        }

        // Try plugin handlers first
        val pluginResult = tryPluginDispatch(command, context)

        return when {
            // Plugin handled successfully
            pluginResult is ActionResult.Success -> {
                recordPluginHandled()
                if (debugLogging) {
                    Log.d(tag, "Plugin handled: ${command.phrase} -> Success")
                }
                pluginResult
            }

            // Plugin returned ambiguous - let it handle disambiguation
            pluginResult is ActionResult.Ambiguous -> {
                recordPluginHandled()
                if (debugLogging) {
                    Log.d(tag, "Plugin handled: ${command.phrase} -> Ambiguous")
                }
                pluginResult
            }

            // Try legacy fallback
            enableLegacyFallback && legacyDispatcher != null -> {
                if (debugLogging) {
                    Log.d(tag, "Plugin didn't handle ${command.phrase}, trying legacy...")
                }
                tryLegacyFallback(command, context)
            }

            // No fallback available
            else -> {
                recordUnhandled()
                if (debugLogging) {
                    Log.w(tag, "Unhandled command: ${command.phrase}")
                }
                ActionResult.Error("No handler for command: ${command.phrase}")
            }
        }
    }

    /**
     * Try dispatching through plugin system.
     */
    private suspend fun tryPluginDispatch(
        command: QuantizedCommand,
        context: HandlerContext
    ): ActionResult {
        return try {
            pluginDispatcher.dispatch(command, context)
        } catch (e: Exception) {
            Log.e(tag, "Plugin dispatch error", e)
            ActionResult.Error("Plugin error: ${e.message}")
        }
    }

    /**
     * Try legacy handler fallback.
     */
    private suspend fun tryLegacyFallback(
        command: QuantizedCommand,
        context: HandlerContext
    ): ActionResult {
        return try {
            val result = legacyDispatcher?.invoke(command, context)
                ?: ActionResult.Error("Legacy dispatcher not available")

            if (result is ActionResult.Success) {
                recordLegacyFallback()
                if (debugLogging) {
                    Log.d(tag, "Legacy handled: ${command.phrase}")
                }
            } else {
                recordUnhandled()
                if (debugLogging) {
                    Log.w(tag, "Legacy also failed: ${command.phrase}")
                }
            }
            result
        } catch (e: Exception) {
            Log.e(tag, "Legacy dispatch error", e)
            recordUnhandled()
            ActionResult.Error("Legacy error: ${e.message}")
        }
    }

    /**
     * Convenience dispatch method with simplified context.
     */
    suspend fun dispatch(
        command: QuantizedCommand,
        packageName: String,
        elements: List<QuantizedElement> = emptyList()
    ): ActionResult {
        val context = HandlerContext(
            currentScreen = ScreenContext(
                packageName = packageName,
                activityName = "unknown",
                screenTitle = null,
                elementCount = elements.size,
                primaryAction = null
            ),
            elements = elements,
            previousCommand = null,
            userPreferences = emptyMap()
        )
        return dispatch(command, context)
    }

    /**
     * Check if a command can be handled by plugins.
     *
     * @param command Command to check
     * @param context Handler context
     * @return true if plugins can handle this command
     */
    fun canPluginHandle(command: QuantizedCommand, context: HandlerContext): Boolean {
        val handlers = pluginDispatcher.getHandlerPlugins()
        return handlers.any {
            it.canHandle(command, context) && it.getConfidence(command, context) >= minPluginConfidence
        }
    }

    /**
     * Get the plugin that would handle a command.
     *
     * @param command Command to check
     * @param context Handler context
     * @return Plugin ID of best matching handler, or null
     */
    fun getHandlingPlugin(command: QuantizedCommand, context: HandlerContext): String? {
        val handlers = pluginDispatcher.getHandlerPlugins()
        return handlers
            .filter { it.canHandle(command, context) }
            .maxByOrNull { it.getConfidence(command, context) }
            ?.pluginId
    }

    /**
     * Disable legacy fallback (use only plugins).
     *
     * Call this when plugin coverage is complete for a handler type.
     */
    fun disableLegacyFallback() {
        enableLegacyFallback = false
        Log.i(tag, "Legacy fallback disabled - using plugins only")
    }

    /**
     * Reset metrics.
     */
    fun resetMetrics() {
        _metrics.value = BridgeMetrics()
    }

    private fun recordPluginHandled() {
        val current = _metrics.value
        val newTotal = current.totalCommands + 1
        val newPluginHandled = current.pluginHandled + 1
        _metrics.value = current.copy(
            totalCommands = newTotal,
            pluginHandled = newPluginHandled,
            pluginCoveragePercent = (newPluginHandled.toFloat() / newTotal) * 100
        )
    }

    private fun recordLegacyFallback() {
        val current = _metrics.value
        val newTotal = current.totalCommands + 1
        _metrics.value = current.copy(
            totalCommands = newTotal,
            legacyFallback = current.legacyFallback + 1,
            pluginCoveragePercent = (current.pluginHandled.toFloat() / newTotal) * 100
        )
    }

    private fun recordUnhandled() {
        val current = _metrics.value
        val newTotal = current.totalCommands + 1
        _metrics.value = current.copy(
            totalCommands = newTotal,
            unhandled = current.unhandled + 1,
            pluginCoveragePercent = (current.pluginHandled.toFloat() / newTotal) * 100
        )
    }
}

/**
 * Handler type mapping for migration tracking.
 *
 * Maps legacy handler class names to plugin IDs for migration monitoring.
 */
object HandlerMigrationMap {
    val mappings = mapOf(
        "NavigationHandler" to "com.augmentalis.commandmanager.handler.navigation",
        "UIHandler" to "com.augmentalis.commandmanager.handler.uiinteraction",
        "InputHandler" to "com.augmentalis.commandmanager.handler.textinput",
        "SystemHandler" to "com.augmentalis.commandmanager.handler.system",
        "GestureHandler" to "com.augmentalis.commandmanager.handler.gesture",
        "SelectHandler" to "com.augmentalis.commandmanager.handler.selection",
        "AppHandler" to "com.augmentalis.commandmanager.handler.applauncher"
    )

    /**
     * Get the plugin ID for a legacy handler class.
     */
    fun getPluginId(legacyHandlerClass: String): String? {
        return mappings[legacyHandlerClass]
    }

    /**
     * Get the legacy handler class for a plugin ID.
     */
    fun getLegacyClass(pluginId: String): String? {
        return mappings.entries.find { it.value == pluginId }?.key
    }

    /**
     * Check if a legacy handler has been migrated to a plugin.
     */
    fun isMigrated(legacyHandlerClass: String): Boolean {
        return mappings.containsKey(legacyHandlerClass)
    }
}

/**
 * Factory to create a configured bridge for production use.
 */
object PluginHandlerBridgeFactory {

    /**
     * Create a bridge with default settings for production.
     *
     * @param pluginDispatcher The plugin command dispatcher
     * @param legacyDispatcher Legacy handler dispatcher (optional)
     * @return Configured bridge
     */
    fun createDefault(
        pluginDispatcher: PluginCommandDispatcher,
        legacyDispatcher: (suspend (QuantizedCommand, HandlerContext) -> ActionResult)? = null
    ): PluginHandlerBridge {
        return PluginHandlerBridge(
            pluginDispatcher = pluginDispatcher,
            legacyDispatcher = legacyDispatcher
        ).apply {
            minPluginConfidence = 0.7f
            enableLegacyFallback = legacyDispatcher != null
            debugLogging = false
        }
    }

    /**
     * Create a bridge for testing (plugins only, no fallback).
     *
     * @param pluginDispatcher The plugin command dispatcher
     * @return Configured bridge with debug logging
     */
    fun createForTesting(
        pluginDispatcher: PluginCommandDispatcher
    ): PluginHandlerBridge {
        return PluginHandlerBridge(
            pluginDispatcher = pluginDispatcher,
            legacyDispatcher = null
        ).apply {
            minPluginConfidence = 0.0f // Accept all plugin handlers
            enableLegacyFallback = false
            debugLogging = true
        }
    }

    /**
     * Create a bridge for migration mode (aggressive plugin use with legacy fallback).
     *
     * @param pluginDispatcher The plugin command dispatcher
     * @param legacyDispatcher Legacy handler dispatcher
     * @return Configured bridge
     */
    fun createForMigration(
        pluginDispatcher: PluginCommandDispatcher,
        legacyDispatcher: suspend (QuantizedCommand, HandlerContext) -> ActionResult
    ): PluginHandlerBridge {
        return PluginHandlerBridge(
            pluginDispatcher = pluginDispatcher,
            legacyDispatcher = legacyDispatcher
        ).apply {
            minPluginConfidence = 0.5f // Lower threshold for more plugin coverage
            enableLegacyFallback = true
            debugLogging = true // Track migration progress
        }
    }
}
