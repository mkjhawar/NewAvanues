/**
 * CommandDispatcher.kt - Command routing and execution
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * Routes matched commands to appropriate executors.
 * Handles both static and dynamic commands.
 */
package com.augmentalis.voiceoscoreng.execution

import com.augmentalis.voiceoscoreng.avu.CommandActionType
import com.augmentalis.voiceoscoreng.avu.QuantizedCommand
import com.augmentalis.voiceoscoreng.command.CommandMatcher
import com.augmentalis.voiceoscoreng.command.CommandRegistry
import com.augmentalis.voiceoscoreng.command.StaticCommand
import com.augmentalis.voiceoscoreng.command.StaticCommandRegistry
import com.augmentalis.voiceoscoreng.speech.SpeechMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dispatches voice commands to appropriate action executors.
 *
 * Flow:
 * 1. Voice input → CommandMatcher → MatchResult
 * 2. MatchResult → CommandDispatcher → ActionExecutor
 * 3. ActionExecutor → ActionResult → Feedback
 *
 * Supports:
 * - Static commands (system-wide)
 * - Dynamic commands (screen-specific)
 * - Combined mode (both)
 */
class CommandDispatcher(
    private val executor: IActionExecutor,
    private val dynamicRegistry: CommandRegistry = CommandRegistry()
) {
    // ═══════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════

    private val _state = MutableStateFlow(DispatcherState())
    val state: StateFlow<DispatcherState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<DispatchEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<DispatchEvent> = _events.asSharedFlow()

    private var currentMode: SpeechMode = SpeechMode.COMBINED_COMMAND

    // ═══════════════════════════════════════════════════════════════════
    // Configuration
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Set the speech mode for command matching.
     */
    fun setMode(mode: SpeechMode) {
        currentMode = mode
        _state.value = _state.value.copy(mode = mode)
    }

    /**
     * Update dynamic commands from screen scraping.
     */
    fun updateDynamicCommands(commands: List<QuantizedCommand>) {
        dynamicRegistry.update(commands)
        _state.value = _state.value.copy(
            dynamicCommandCount = commands.size
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // Command Processing
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Process voice input and execute matching command.
     *
     * @param voiceInput Raw voice input string
     * @param confidence Recognition confidence (0.0 - 1.0)
     * @return DispatchResult with execution outcome
     */
    suspend fun dispatch(voiceInput: String, confidence: Float = 1.0f): DispatchResult {
        val startTime = currentTimeMillis()

        _state.value = _state.value.copy(isProcessing = true)
        emitEvent(DispatchEvent.Processing(voiceInput))

        try {
            // Step 1: Try static commands first (if mode allows)
            if (currentMode.requiresStaticCommands()) {
                val staticResult = matchStaticCommand(voiceInput)
                if (staticResult != null) {
                    val actionResult = executeStaticCommand(staticResult)
                    return completeDispatch(
                        voiceInput = voiceInput,
                        commandType = CommandType.STATIC,
                        actionResult = actionResult,
                        duration = currentTimeMillis() - startTime
                    )
                }
            }

            // Step 2: Try dynamic commands (if mode allows)
            if (currentMode.requiresDynamicCommands()) {
                val matchResult = CommandMatcher.match(
                    voiceInput = voiceInput,
                    registry = dynamicRegistry,
                    threshold = currentMode.getRecommendedConfidenceThreshold()
                )

                when (matchResult) {
                    is CommandMatcher.MatchResult.Exact -> {
                        val actionResult = executor.executeCommand(matchResult.command)
                        return completeDispatch(
                            voiceInput = voiceInput,
                            commandType = CommandType.DYNAMIC,
                            actionResult = actionResult,
                            matchedCommand = matchResult.command.phrase,
                            duration = currentTimeMillis() - startTime
                        )
                    }

                    is CommandMatcher.MatchResult.Fuzzy -> {
                        val actionResult = executor.executeCommand(matchResult.command)
                        return completeDispatch(
                            voiceInput = voiceInput,
                            commandType = CommandType.DYNAMIC,
                            actionResult = actionResult,
                            matchedCommand = matchResult.command.phrase,
                            matchConfidence = matchResult.confidence,
                            duration = currentTimeMillis() - startTime
                        )
                    }

                    is CommandMatcher.MatchResult.Ambiguous -> {
                        emitEvent(DispatchEvent.Ambiguous(
                            voiceInput,
                            matchResult.candidates.map { it.phrase }
                        ))
                        return DispatchResult.Ambiguous(
                            candidates = matchResult.candidates.map { it.phrase },
                            duration = currentTimeMillis() - startTime
                        )
                    }

                    is CommandMatcher.MatchResult.NoMatch -> {
                        // Fall through to no match
                    }
                }
            }

            // No match found
            emitEvent(DispatchEvent.NoMatch(voiceInput))
            return DispatchResult.NoMatch(
                voiceInput = voiceInput,
                duration = currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            emitEvent(DispatchEvent.Error(voiceInput, e.message ?: "Unknown error"))
            return DispatchResult.Error(
                error = e.message ?: "Unknown error",
                duration = currentTimeMillis() - startTime
            )
        } finally {
            _state.value = _state.value.copy(isProcessing = false)
        }
    }

    /**
     * Execute a command directly (bypass matching).
     */
    suspend fun executeDirectly(command: QuantizedCommand): ActionResult {
        return executor.executeCommand(command)
    }

    /**
     * Execute an action type directly.
     */
    suspend fun executeAction(
        actionType: CommandActionType,
        params: Map<String, Any> = emptyMap()
    ): ActionResult {
        return executor.executeAction(actionType, params)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Static Command Handling
    // ═══════════════════════════════════════════════════════════════════

    private fun matchStaticCommand(voiceInput: String): StaticCommand? {
        return StaticCommandRegistry.findByPhrase(voiceInput)
    }

    private suspend fun executeStaticCommand(command: StaticCommand): ActionResult {
        return executor.executeAction(command.actionType, command.metadata)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private fun completeDispatch(
        voiceInput: String,
        commandType: CommandType,
        actionResult: ActionResult,
        matchedCommand: String? = null,
        matchConfidence: Float = 1.0f,
        duration: Long
    ): DispatchResult {
        _state.value = _state.value.copy(
            lastCommand = voiceInput,
            lastResult = actionResult.isSuccess,
            commandsProcessed = _state.value.commandsProcessed + 1
        )

        emitEvent(DispatchEvent.Executed(
            voiceInput = voiceInput,
            commandType = commandType,
            success = actionResult.isSuccess,
            message = actionResult.message
        ))

        return if (actionResult.isSuccess) {
            DispatchResult.Success(
                voiceInput = voiceInput,
                matchedCommand = matchedCommand ?: voiceInput,
                commandType = commandType,
                confidence = matchConfidence,
                duration = duration
            )
        } else {
            DispatchResult.Failed(
                voiceInput = voiceInput,
                reason = actionResult.message,
                actionResult = actionResult,
                duration = duration
            )
        }
    }

    private fun emitEvent(event: DispatchEvent) {
        _events.tryEmit(event)
    }

    private fun currentTimeMillis(): Long = com.augmentalis.voiceoscoreng.speech.currentTimeMillis()
}

/**
 * Dispatcher state
 */
data class DispatcherState(
    val mode: SpeechMode = SpeechMode.COMBINED_COMMAND,
    val isProcessing: Boolean = false,
    val dynamicCommandCount: Int = 0,
    val lastCommand: String? = null,
    val lastResult: Boolean? = null,
    val commandsProcessed: Long = 0
)

/**
 * Dispatch result
 */
sealed class DispatchResult {
    abstract val duration: Long

    data class Success(
        val voiceInput: String,
        val matchedCommand: String,
        val commandType: CommandType,
        val confidence: Float,
        override val duration: Long
    ) : DispatchResult()

    data class Failed(
        val voiceInput: String,
        val reason: String,
        val actionResult: ActionResult,
        override val duration: Long
    ) : DispatchResult()

    data class NoMatch(
        val voiceInput: String,
        override val duration: Long
    ) : DispatchResult()

    data class Ambiguous(
        val candidates: List<String>,
        override val duration: Long
    ) : DispatchResult()

    data class Error(
        val error: String,
        override val duration: Long
    ) : DispatchResult()
}

/**
 * Command type classification
 */
enum class CommandType {
    STATIC,
    DYNAMIC
}

/**
 * Dispatch events for observation
 */
sealed class DispatchEvent {
    data class Processing(val voiceInput: String) : DispatchEvent()
    data class Executed(
        val voiceInput: String,
        val commandType: CommandType,
        val success: Boolean,
        val message: String
    ) : DispatchEvent()
    data class NoMatch(val voiceInput: String) : DispatchEvent()
    data class Ambiguous(val voiceInput: String, val candidates: List<String>) : DispatchEvent()
    data class Error(val voiceInput: String, val error: String) : DispatchEvent()
}
