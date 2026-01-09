/**
 * ActionCoordinator.kt - Coordinates action execution across handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 * Updated: 2026-01-08 - Consolidated dynamic command support (CommandRegistry + fuzzy matching)
 *
 * KMP coordinator for managing handler registration and command execution.
 * Now supports both static handlers AND dynamic screen-specific commands.
 */
package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.CommandActionType
import com.augmentalis.voiceoscoreng.common.CommandMatcher
import com.augmentalis.voiceoscoreng.common.CommandRegistry
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
 * - Dynamic command support (screen-specific commands with VUIDs)
 * - Fuzzy matching for voice input variations
 * - Performance metrics collection
 * - Voice command interpretation
 *
 * ## Execution Priority:
 * 1. Dynamic command lookup by VUID (fastest, most accurate)
 * 2. Dynamic command fuzzy match (handles voice variations)
 * 3. Static handler lookup (system commands)
 * 4. Voice interpreter fallback (natural language)
 */
class ActionCoordinator(
    private val voiceInterpreter: IVoiceCommandInterpreter = DefaultVoiceCommandInterpreter,
    private val handlerRegistry: IHandlerRegistry = HandlerRegistry(),
    private val commandRegistry: CommandRegistry = CommandRegistry(),
    private val metrics: IMetricsCollector = MetricsCollector()
) {

    companion object {
        private const val HANDLER_TIMEOUT_MS = 5000L
        private const val DEFAULT_FUZZY_THRESHOLD = 0.7f
    }

    /**
     * Current number of dynamic commands registered.
     */
    val dynamicCommandCount: Int get() = commandRegistry.size

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
                handlerRegistry.register(handler)
            }

            // Initialize all handlers
            val initCount = handlerRegistry.initializeAll()

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
        handlerRegistry.register(handler)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Dynamic Command Management
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Update dynamic commands from screen scraping.
     *
     * Call this after each screen scan to register the current screen's
     * actionable elements as voice commands.
     *
     * @param commands List of quantized commands from UI elements
     */
    suspend fun updateDynamicCommands(commands: List<QuantizedCommand>) {
        commandRegistry.update(commands)
    }

    /**
     * Clear all dynamic commands.
     * Call when leaving an app or screen context is invalid.
     */
    fun clearDynamicCommands() {
        commandRegistry.clear()
    }

    /**
     * Get all current dynamic commands.
     */
    fun getDynamicCommands(): List<QuantizedCommand> {
        return commandRegistry.all()
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
        val handler = handlerRegistry.findHandler(command)
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
     * Process a voice command string with full dynamic command support.
     *
     * Execution priority:
     * 1. Dynamic command by exact phrase match (with VUID)
     * 2. Dynamic command by fuzzy match (handles voice variations)
     * 3. Static handler match (system commands)
     * 4. Voice interpreter fallback (natural language)
     *
     * @param text The voice command text
     * @param confidence Confidence level (0-1)
     * @return HandlerResult from execution
     */
    suspend fun processVoiceCommand(text: String, confidence: Float = 1.0f): HandlerResult {
        val normalizedText = text.lowercase().trim()

        // ═══════════════════════════════════════════════════════════════════
        // Step 1: Try dynamic command lookup (has VUID for direct execution)
        // ═══════════════════════════════════════════════════════════════════
        if (commandRegistry.size > 0) {
            // First try exact phrase match in dynamic registry
            val exactMatch = commandRegistry.findByPhrase(normalizedText)
            if (exactMatch != null) {
                return processCommand(exactMatch)
            }

            // Then try fuzzy matching for voice variations
            val matchResult = CommandMatcher.match(
                voiceInput = normalizedText,
                registry = commandRegistry,
                threshold = DEFAULT_FUZZY_THRESHOLD
            )

            when (matchResult) {
                is CommandMatcher.MatchResult.Exact -> {
                    return processCommand(matchResult.command)
                }
                is CommandMatcher.MatchResult.Fuzzy -> {
                    return processCommand(matchResult.command)
                }
                is CommandMatcher.MatchResult.Ambiguous -> {
                    // Return ambiguous result - caller can show disambiguation UI
                    return HandlerResult.awaitingSelection(
                        message = "${matchResult.candidates.size} matches found. Please be more specific.",
                        matchCount = matchResult.candidates.size,
                        accessibilityAnnouncement = "Multiple matches. Say a number to select."
                    )
                }
                is CommandMatcher.MatchResult.NoMatch -> {
                    // Fall through to static handlers
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // Step 2: Try static handler lookup
        // ═══════════════════════════════════════════════════════════════════
        val directCommand = QuantizedCommand(
            phrase = normalizedText,
            actionType = CommandActionType.EXECUTE,
            targetVuid = null,
            confidence = confidence
        )

        if (handlerRegistry.canHandle(normalizedText)) {
            return processCommand(directCommand)
        }

        // ═══════════════════════════════════════════════════════════════════
        // Step 3: Try voice interpreter (natural language fallback)
        // ═══════════════════════════════════════════════════════════════════
        val interpretedAction = interpretVoiceCommand(normalizedText)
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
     * Checks both dynamic commands and static handlers.
     */
    suspend fun canHandle(command: String): Boolean {
        val normalized = command.lowercase().trim()

        // Check dynamic commands first
        if (commandRegistry.findByPhrase(normalized) != null) {
            return true
        }

        // Check static handlers
        return handlerRegistry.canHandle(normalized)
    }

    /**
     * Get all supported actions.
     * Returns both dynamic commands and static handler actions.
     */
    suspend fun getAllSupportedActions(): List<String> {
        val staticActions = handlerRegistry.getAllSupportedActions()
        val dynamicActions = commandRegistry.all().map { it.phrase }
        return staticActions + dynamicActions
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
            handlerRegistry.disposeAll()
            handlerRegistry.clear()
            commandRegistry.clear()
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
            appendLine("Handlers: ${handlerRegistry.getHandlerCount()}")
            appendLine("Categories: ${handlerRegistry.getCategoryCount()}")
            appendLine("Dynamic Commands: ${commandRegistry.size}")
            appendLine()
            append(handlerRegistry.getDebugInfo())
            appendLine()
            appendLine("Dynamic Commands:")
            commandRegistry.all().take(10).forEach { cmd ->
                appendLine("  - ${cmd.phrase} (VUID: ${cmd.targetVuid})")
            }
            if (commandRegistry.size > 10) {
                appendLine("  ... and ${commandRegistry.size - 10} more")
            }
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
