/**
 * ActionCoordinator.kt - Coordinates action execution across handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP coordinator for managing handler registration and command execution.
 */
package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.CommandActionType
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import com.augmentalis.voiceoscoreng.features.currentTimeMillis
import com.augmentalis.voiceoscoreng.handlers.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates action execution across multiple handlers.
 *
 * Provides:
 * - Handler registration and lifecycle management
 * - Priority-based command routing
 * - Performance metrics collection
 * - Voice command interpretation
 */
class ActionCoordinator(
    private val voiceInterpreter: IVoiceCommandInterpreter = DefaultVoiceCommandInterpreter,
    private val registry: IHandlerRegistry = HandlerRegistry(),
    private val metrics: IMetricsCollector = MetricsCollector()
) {

    companion object {
        private const val HANDLER_TIMEOUT_MS = 5000L
    }

    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // State flow for coordinator status
    private val _state = MutableStateFlow(CoordinatorState.UNINITIALIZED)
    val state: StateFlow<CoordinatorState> = _state.asStateFlow()

    // Event flow for execution results
    private val _results = MutableSharedFlow<CommandResult>(extraBufferCapacity = 64)
    val results: SharedFlow<CommandResult> = _results.asSharedFlow()

    /**
     * Initialize the coordinator with handlers.
     *
     * @param handlers List of handlers to register
     */
    suspend fun initialize(handlers: List<IHandler>) {
        _state.value = CoordinatorState.INITIALIZING

        try {
            // Register all handlers
            handlers.forEach { handler ->
                registry.register(handler)
            }

            // Initialize all handlers
            val initCount = registry.initializeAll()

            _state.value = CoordinatorState.READY
        } catch (e: Exception) {
            _state.value = CoordinatorState.ERROR
            throw e
        }
    }

    /**
     * Register a handler.
     */
    suspend fun registerHandler(handler: IHandler) {
        registry.register(handler)
    }

    /**
     * Process a quantized command.
     *
     * @param command The command to process
     * @return HandlerResult from execution
     */
    suspend fun processCommand(command: QuantizedCommand): HandlerResult {
        val startTime = currentTimeMillis()

        // Find handler
        val handler = registry.findHandler(command)
        if (handler == null) {
            val result = HandlerResult.failure("No handler found for: ${command.phrase}")
            recordResult(command, result, currentTimeMillis() - startTime)
            return result
        }

        // Execute with timeout
        return try {
            val result = withTimeoutOrNull(HANDLER_TIMEOUT_MS) {
                handler.execute(command)
            } ?: HandlerResult.failure("Handler timed out", recoverable = true)

            recordResult(command, result, currentTimeMillis() - startTime)
            result
        } catch (e: Exception) {
            val result = HandlerResult.failure("Execution error: ${e.message}", recoverable = true)
            recordResult(command, result, currentTimeMillis() - startTime)
            result
        }
    }

    /**
     * Process a voice command string.
     *
     * @param text The voice command text
     * @param confidence Confidence level (0-1)
     * @return HandlerResult from execution
     */
    suspend fun processVoiceCommand(text: String, confidence: Float = 1.0f): HandlerResult {
        val normalizedCommand = text.lowercase().trim()

        // Try direct command first
        val directCommand = QuantizedCommand(
            phrase = normalizedCommand,
            actionType = CommandActionType.EXECUTE,
            targetVuid = null,
            confidence = confidence
        )

        if (registry.canHandle(normalizedCommand)) {
            return processCommand(directCommand)
        }

        // Try interpreted command
        val interpretedAction = interpretVoiceCommand(normalizedCommand)
        if (interpretedAction != null) {
            val interpretedCommand = directCommand.copy(phrase = interpretedAction)
            return processCommand(interpretedCommand)
        }

        return HandlerResult.failure("Unknown command: $text")
    }

    /**
     * Interpret natural language voice commands into action strings.
     */
    private fun interpretVoiceCommand(command: String): String? {
        return voiceInterpreter.interpret(command)
    }

    /**
     * Check if any handler can handle the command.
     */
    suspend fun canHandle(command: String): Boolean {
        return registry.canHandle(command)
    }

    /**
     * Get all supported actions.
     */
    suspend fun getAllSupportedActions(): List<String> {
        return registry.getAllSupportedActions()
    }

    /**
     * Record execution result for metrics.
     */
    private suspend fun recordResult(command: QuantizedCommand, result: HandlerResult, durationMs: Long) {
        val commandResult = CommandResult(
            command = command,
            result = result,
            durationMs = durationMs,
            timestamp = currentTimeMillis()
        )

        _results.emit(commandResult)
        metrics.record(commandResult)
    }

    /**
     * Get metrics summary.
     */
    fun getMetricsSummary(): MetricsSummary {
        return metrics.getSummary()
    }

    /**
     * Reset all metrics.
     */
    fun resetMetrics() {
        metrics.reset()
    }

    /**
     * Dispose the coordinator.
     */
    suspend fun dispose() {
        _state.value = CoordinatorState.DISPOSING

        try {
            registry.disposeAll()
            registry.clear()
            scope.cancel()
            _state.value = CoordinatorState.DISPOSED
        } catch (e: Exception) {
            _state.value = CoordinatorState.ERROR
        }
    }

    /**
     * Get debug information.
     */
    suspend fun getDebugInfo(): String {
        return buildString {
            appendLine("ActionCoordinator Debug Info")
            appendLine("State: ${_state.value}")
            appendLine("Handlers: ${registry.getHandlerCount()}")
            appendLine("Categories: ${registry.getCategoryCount()}")
            appendLine()
            append(registry.getDebugInfo())
            appendLine()
            append(metrics.getDebugInfo())
        }
    }

    /**
     * Platform-specific currentTimeMillis.
     */
    private fun currentTimeMillis(): Long = com.augmentalis.voiceoscoreng.features.currentTimeMillis()
}

/**
 * Coordinator state.
 */
enum class CoordinatorState {
    UNINITIALIZED,
    INITIALIZING,
    READY,
    DISPOSING,
    DISPOSED,
    ERROR
}

/**
 * Result of command execution.
 */
data class CommandResult(
    val command: QuantizedCommand,
    val result: HandlerResult,
    val durationMs: Long,
    val timestamp: Long
)
