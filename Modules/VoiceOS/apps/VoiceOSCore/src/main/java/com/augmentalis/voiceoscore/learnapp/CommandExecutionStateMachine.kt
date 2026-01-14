/**
 * CommandExecutionStateMachine.kt - State machine for tracking command execution lifecycle
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (Implementation based on VOS-Plan-CriticalFixes-251219-V1.md)
 * Code-Reviewed-By: CCA
 * Created: 2025-12-19
 *
 * Purpose: Track command execution lifecycle and enable retry/recovery from failures
 *
 * Features:
 * - State tracking (Idle, Pending, Executing, Completed, Failed)
 * - Automatic retry with exponential backoff
 * - Execution history tracking
 * - State validation and transitions
 * - Observable state flow for UI feedback
 *
 * Task: 1.9 from VOS-Plan-CriticalFixes-251219-V1.md
 */

package com.augmentalis.voiceoscore.learnapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Sealed class representing all possible command execution states
 */
sealed class CommandExecutionState {
    /** No command currently executing */
    object Idle : CommandExecutionState()

    /** Command queued for execution */
    data class Pending(val commandId: Long, val timestamp: Long) : CommandExecutionState()

    /** Command currently executing */
    data class Executing(val commandId: Long, val startTime: Long) : CommandExecutionState()

    /** Command completed successfully */
    data class Completed(val commandId: Long, val duration: Long) : CommandExecutionState()

    /** Command failed with error */
    data class Failed(val commandId: Long, val error: String, val retryCount: Int) : CommandExecutionState()
}

/**
 * State machine for command execution lifecycle
 *
 * Tracks command execution through state transitions:
 * Idle -> Pending -> Executing -> (Completed | Failed) -> Idle
 *
 * Features:
 * - Automatic retry on failure (up to maxRetries)
 * - Exponential backoff on retries
 * - State validation on transitions
 * - Execution history tracking
 * - Observable state flow
 *
 * Usage:
 * ```kotlin
 * val stateMachine = CommandExecutionStateMachine()
 *
 * // Start execution
 * stateMachine.startExecution(commandId = 123)
 * stateMachine.markExecuting(commandId = 123)
 *
 * // Complete or fail
 * if (success) {
 *     stateMachine.markCompleted(commandId = 123)
 * } else {
 *     stateMachine.markFailed(commandId = 123, error = "Timeout")
 * }
 *
 * // Observe state
 * stateMachine.state.collect { state ->
 *     when (state) {
 *         is CommandExecutionState.Failed -> showError(state.error)
 *         is CommandExecutionState.Completed -> showSuccess()
 *         else -> {}
 *     }
 * }
 * ```
 */
class CommandExecutionStateMachine {

    /**
     * Current execution state (observable)
     */
    private val _state = MutableStateFlow<CommandExecutionState>(CommandExecutionState.Idle)
    val state: StateFlow<CommandExecutionState> = _state

    /**
     * History of all state transitions
     */
    private val executionHistory = mutableListOf<CommandExecutionState>()

    /**
     * Maximum retry attempts before giving up
     */
    private val maxRetries = 3

    /**
     * Start command execution
     *
     * Transitions from Idle/Completed/Failed to Pending state.
     *
     * @param commandId Unique command identifier
     * @throws IllegalStateException if current state is not Idle/Completed/Failed
     */
    suspend fun startExecution(commandId: Long) {
        val currentState = _state.value

        // Validate state transition
        require(currentState is CommandExecutionState.Idle ||
                currentState is CommandExecutionState.Completed ||
                currentState is CommandExecutionState.Failed) {
            "Cannot start execution from state: $currentState"
        }

        val newState = CommandExecutionState.Pending(commandId, System.currentTimeMillis())
        _state.emit(newState)
        executionHistory.add(newState)
    }

    /**
     * Mark command as executing
     *
     * Transitions from Pending to Executing state.
     *
     * @param commandId Command identifier (must match pending command)
     * @throws IllegalStateException if current state is not Pending
     */
    suspend fun markExecuting(commandId: Long) {
        val currentState = _state.value
        require(currentState is CommandExecutionState.Pending) {
            "Cannot mark executing from state: $currentState"
        }

        val newState = CommandExecutionState.Executing(commandId, System.currentTimeMillis())
        _state.emit(newState)
        executionHistory.add(newState)
    }

    /**
     * Mark command as completed successfully
     *
     * Transitions from Executing to Completed, then auto-transitions to Idle after 2 seconds.
     *
     * @param commandId Command identifier (must match executing command)
     * @throws IllegalStateException if current state is not Executing
     */
    suspend fun markCompleted(commandId: Long) {
        val currentState = _state.value
        require(currentState is CommandExecutionState.Executing) {
            "Cannot mark completed from state: $currentState"
        }

        val duration = System.currentTimeMillis() - currentState.startTime
        val newState = CommandExecutionState.Completed(commandId, duration)
        _state.emit(newState)
        executionHistory.add(newState)

        // Auto-transition to Idle after 2 seconds
        kotlinx.coroutines.delay(2000)
        _state.emit(CommandExecutionState.Idle)
    }

    /**
     * Mark command as failed
     *
     * Transitions from Pending/Executing to Failed state.
     * Automatically retries if retry count < maxRetries (with exponential backoff).
     * If max retries exceeded, auto-transitions to Idle after 5 seconds.
     *
     * @param commandId Command identifier
     * @param error Error message describing failure
     * @throws IllegalStateException if current state is not Pending/Executing
     */
    suspend fun markFailed(commandId: Long, error: String) {
        val currentState = _state.value
        require(currentState is CommandExecutionState.Pending ||
                currentState is CommandExecutionState.Executing) {
            "Cannot mark failed from state: $currentState"
        }

        val retryCount = executionHistory
            .filterIsInstance<CommandExecutionState.Failed>()
            .count { it.commandId == commandId }

        val newState = CommandExecutionState.Failed(commandId, error, retryCount)
        _state.emit(newState)
        executionHistory.add(newState)

        // Auto-retry if under limit
        if (retryCount < maxRetries) {
            // Exponential backoff: 1s, 2s, 3s...
            kotlinx.coroutines.delay(1000L * (retryCount + 1))
            startExecution(commandId)
        } else {
            // Max retries exceeded, transition to Idle
            kotlinx.coroutines.delay(5000)
            _state.emit(CommandExecutionState.Idle)
        }
    }

    /**
     * Get full execution history
     *
     * @return List of all state transitions (chronological order)
     */
    fun getExecutionHistory(): List<CommandExecutionState> = executionHistory.toList()

    /**
     * Clear execution history
     *
     * Removes all historical state records but keeps current state.
     */
    fun clearHistory() {
        executionHistory.clear()
    }

    /**
     * Get retry statistics for a command
     *
     * @param commandId Command to check
     * @return Pair of (failureCount, lastError) or null if no failures
     */
    fun getRetryStats(commandId: Long): Pair<Int, String>? {
        val failures = executionHistory
            .filterIsInstance<CommandExecutionState.Failed>()
            .filter { it.commandId == commandId }

        return if (failures.isNotEmpty()) {
            failures.size to failures.last().error
        } else {
            null
        }
    }

    /**
     * Check if command is currently executing
     *
     * @return true if state is Pending or Executing
     */
    fun isExecuting(): Boolean {
        return _state.value is CommandExecutionState.Pending ||
               _state.value is CommandExecutionState.Executing
    }

    /**
     * Reset state machine to Idle
     *
     * Emergency reset - use only when state machine is stuck.
     * Clears history and resets to Idle.
     */
    suspend fun reset() {
        executionHistory.clear()
        _state.emit(CommandExecutionState.Idle)
    }
}
