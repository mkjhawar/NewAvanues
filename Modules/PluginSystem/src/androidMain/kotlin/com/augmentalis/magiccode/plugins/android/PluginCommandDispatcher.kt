/**
 * PluginCommandDispatcher.kt - Routes voice commands through the plugin system
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 *
 * Replaces legacy handler dispatch with plugin-based routing.
 * Routes commands to the appropriate HandlerPlugin based on confidence scores.
 */
package com.augmentalis.magiccode.plugins.android

import android.util.Log
import com.augmentalis.magiccode.plugins.builtin.*
import com.augmentalis.magiccode.plugins.universal.*
import com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore.*
import com.augmentalis.voiceoscore.ActionResult
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plugin-based command dispatcher.
 *
 * Routes voice commands through registered HandlerPlugins, selecting the handler
 * with the highest confidence score for each command.
 *
 * ## Features
 * - Confidence-based handler selection
 * - Fallback to legacy handlers (optional)
 * - Performance metrics tracking
 * - Disambiguation support
 *
 * ## Usage
 * ```kotlin
 * val dispatcher = PluginCommandDispatcher(pluginHost)
 *
 * val result = dispatcher.dispatch(
 *     command = QuantizedCommand(phrase = "click submit", ...),
 *     context = currentHandlerContext
 * )
 * ```
 *
 * @param pluginHost The AndroidPluginHost to retrieve handler plugins from
 */
class PluginCommandDispatcher(
    private val pluginHost: AndroidPluginHost
) {
    private val tag = "PluginCmdDispatcher"

    /**
     * Dispatch metrics for performance monitoring.
     */
    data class DispatchMetrics(
        val totalDispatches: Long = 0,
        val successfulDispatches: Long = 0,
        val failedDispatches: Long = 0,
        val averageRoutingTimeMs: Double = 0.0,
        val averageExecutionTimeMs: Double = 0.0
    )

    private val _metrics = MutableStateFlow(DispatchMetrics())
    val metrics: StateFlow<DispatchMetrics> = _metrics.asStateFlow()

    /**
     * Last dispatch result for debugging.
     */
    private val _lastResult = MutableStateFlow<DispatchResult?>(null)
    val lastResult: StateFlow<DispatchResult?> = _lastResult.asStateFlow()

    /**
     * Dispatch a command to the appropriate handler plugin.
     *
     * @param command The command to execute
     * @param context Handler context with screen state
     * @return ActionResult from the handler
     */
    suspend fun dispatch(
        command: QuantizedCommand,
        context: HandlerContext
    ): ActionResult {
        val startTime = System.currentTimeMillis()

        // Find all handler plugins
        val handlerPlugins = pluginHost.getPluginsByCapability(PluginCapability.ACCESSIBILITY_HANDLER)
            .filterIsInstance<HandlerPlugin>()

        if (handlerPlugins.isEmpty()) {
            Log.w(tag, "No handler plugins registered")
            return ActionResult.Error("No handlers available")
        }

        // Calculate confidence scores
        val routingStartTime = System.currentTimeMillis()
        val candidates = handlerPlugins
            .filter { it.canHandle(command, context) }
            .map { plugin ->
                CandidateHandler(
                    plugin = plugin,
                    confidence = plugin.getConfidence(command, context)
                )
            }
            .sortedByDescending { it.confidence }

        val routingTimeMs = System.currentTimeMillis() - routingStartTime
        Log.d(tag, "Routing took ${routingTimeMs}ms, found ${candidates.size} candidates")

        if (candidates.isEmpty()) {
            Log.w(tag, "No handler can process command: ${command.phrase}")
            updateMetrics(success = false, routingTimeMs = routingTimeMs, executionTimeMs = 0)
            return ActionResult.Error("No handler for command: ${command.phrase}")
        }

        // Log candidates for debugging
        candidates.forEach { candidate ->
            Log.d(tag, "  Candidate: ${candidate.plugin.pluginId} (confidence: ${candidate.confidence})")
        }

        // Execute with highest confidence handler
        val selected = candidates.first()
        Log.d(tag, "Selected handler: ${selected.plugin.pluginId} (confidence: ${selected.confidence})")

        val executionStartTime = System.currentTimeMillis()
        val result = try {
            selected.plugin.handle(command, context)
        } catch (e: Exception) {
            Log.e(tag, "Handler execution failed: ${e.message}", e)
            ActionResult.Error("Handler error: ${e.message}")
        }
        val executionTimeMs = System.currentTimeMillis() - executionStartTime

        // Update metrics
        val success = result is ActionResult.Success
        updateMetrics(success = success, routingTimeMs = routingTimeMs, executionTimeMs = executionTimeMs)

        // Store last result
        _lastResult.value = DispatchResult(
            command = command,
            selectedHandler = selected.plugin.pluginId,
            confidence = selected.confidence,
            result = result,
            routingTimeMs = routingTimeMs,
            executionTimeMs = executionTimeMs,
            candidateCount = candidates.size
        )

        val totalTime = System.currentTimeMillis() - startTime
        Log.d(tag, "Dispatch complete in ${totalTime}ms (routing: ${routingTimeMs}ms, execution: ${executionTimeMs}ms)")

        return result
    }

    /**
     * Dispatch a command with automatic context building.
     *
     * Convenience method that builds HandlerContext from current screen state.
     *
     * @param command The command to execute
     * @param packageName Current package name
     * @param elements Current screen elements
     * @param previousCommand Previous command (optional)
     * @return ActionResult from the handler
     */
    suspend fun dispatch(
        command: QuantizedCommand,
        packageName: String,
        elements: List<QuantizedElement> = emptyList(),
        previousCommand: QuantizedCommand? = null
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
            previousCommand = previousCommand,
            userPreferences = emptyMap()
        )
        return dispatch(command, context)
    }

    /**
     * Handle number selection for disambiguation.
     *
     * When a command results in Ambiguous, call this with the user's number selection.
     *
     * @param number The number spoken (1-based)
     * @return ActionResult from the disambiguation resolution
     */
    suspend fun handleNumberSelection(number: Int): ActionResult {
        // Find the UIInteractionPlugin which handles disambiguation
        val uiPlugin = pluginHost.getPlugin(UIInteractionPlugin.PLUGIN_ID)
        if (uiPlugin is UIInteractionPlugin) {
            return uiPlugin.handleNumberSelection(number)
        }
        return ActionResult.Error("No disambiguation handler available")
    }

    /**
     * Check if disambiguation is currently active.
     */
    fun isDisambiguationActive(): Boolean {
        val uiPlugin = pluginHost.getPlugin(UIInteractionPlugin.PLUGIN_ID)
        return (uiPlugin as? UIInteractionPlugin)?.isDisambiguationActive() ?: false
    }

    /**
     * Cancel active disambiguation.
     */
    fun cancelDisambiguation() {
        val uiPlugin = pluginHost.getPlugin(UIInteractionPlugin.PLUGIN_ID)
        (uiPlugin as? UIInteractionPlugin)?.cancelDisambiguation()
    }

    /**
     * Get all registered handler plugins.
     */
    fun getHandlerPlugins(): List<HandlerPlugin> {
        return pluginHost.getPluginsByCapability(PluginCapability.ACCESSIBILITY_HANDLER)
            .filterIsInstance<HandlerPlugin>()
    }

    /**
     * Get handler plugin by ID.
     */
    fun getHandler(pluginId: String): HandlerPlugin? {
        return pluginHost.getPlugin(pluginId) as? HandlerPlugin
    }

    /**
     * Reset metrics.
     */
    fun resetMetrics() {
        _metrics.value = DispatchMetrics()
    }

    private fun updateMetrics(success: Boolean, routingTimeMs: Long, executionTimeMs: Long) {
        val current = _metrics.value
        val newTotal = current.totalDispatches + 1
        val newSuccessful = current.successfulDispatches + if (success) 1 else 0
        val newFailed = current.failedDispatches + if (!success) 1 else 0

        // Calculate running averages
        val newAvgRouting = (current.averageRoutingTimeMs * current.totalDispatches + routingTimeMs) / newTotal
        val newAvgExecution = (current.averageExecutionTimeMs * current.totalDispatches + executionTimeMs) / newTotal

        _metrics.value = DispatchMetrics(
            totalDispatches = newTotal,
            successfulDispatches = newSuccessful,
            failedDispatches = newFailed,
            averageRoutingTimeMs = newAvgRouting,
            averageExecutionTimeMs = newAvgExecution
        )
    }

    /**
     * Candidate handler with confidence score.
     */
    private data class CandidateHandler(
        val plugin: HandlerPlugin,
        val confidence: Float
    )
}

/**
 * Result of a command dispatch for debugging/logging.
 */
data class DispatchResult(
    val command: QuantizedCommand,
    val selectedHandler: String,
    val confidence: Float,
    val result: ActionResult,
    val routingTimeMs: Long,
    val executionTimeMs: Long,
    val candidateCount: Int
)
