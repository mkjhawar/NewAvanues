/**
 * MacroExecutor.kt - Executes command macros with error handling
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-09
 *
 * Purpose: Execute macro steps sequentially with robust error handling
 * Key Features:
 * - Sequential step execution
 * - Conditional and loop support
 * - Error handling and rollback
 * - Timeout protection
 * - Execution tracking
 */
package com.augmentalis.voiceoscore.managers.commandmanager.macros

import android.accessibilityservice.AccessibilityService
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * Macro Executor
 *
 * Executes CommandMacro sequences with error handling and timeout protection.
 *
 * ## Usage
 *
 * ```kotlin
 * val executor = MacroExecutor(accessibilityService, commandProcessor)
 *
 * val result = executor.execute(loginMacro)
 *
 * if (result.success) {
 *     println("Macro completed: ${result.message}")
 * } else {
 *     println("Macro failed: ${result.error}")
 * }
 * ```
 *
 * @property accessibilityService Accessibility service for context
 * @property commandProcessor Function to execute voice commands
 */
class MacroExecutor(
    private val accessibilityService: AccessibilityService,
    private val commandProcessor: suspend (VoiceCommand) -> CommandResult
) {

    companion object {
        private const val TAG = "MacroExecutor"
        private const val DEFAULT_TIMEOUT_MS = 60000L // 1 minute
        private const val MAX_LOOP_ITERATIONS = 100
    }

    /**
     * Execute a macro
     *
     * @param macro Macro to execute
     * @param timeout Timeout in milliseconds (default 60 seconds)
     * @return Execution result
     */
    suspend fun execute(
        macro: CommandMacro,
        timeout: Long = DEFAULT_TIMEOUT_MS
    ): MacroExecutionResult {
        Log.i(TAG, "=== Executing Macro: ${macro.name} ===")
        Log.d(TAG, "Macro ID: ${macro.id}")
        Log.d(TAG, "Steps: ${macro.steps.size}")
        Log.d(TAG, "Estimated duration: ${macro.getEstimatedDuration()}ms")

        // Validate macro
        val validation = macro.validate()
        if (!validation.isValid) {
            Log.e(TAG, "Macro validation failed: ${validation.errors}")
            return MacroExecutionResult(
                success = false,
                error = "Validation failed: ${validation.errors.joinToString()}"
            )
        }

        if (validation.hasWarnings()) {
            Log.w(TAG, "Macro warnings: ${validation.warnings}")
        }

        // Create execution context
        val context = MacroContext(accessibilityService)
        context.startExecution(macro.getTotalStepCount())

        val startTime = System.currentTimeMillis()

        return try {
            // Execute with timeout
            withTimeout(timeout) {
                executeSteps(macro.steps, context)

                val duration = System.currentTimeMillis() - startTime
                context.completeExecution()

                Log.i(TAG, "=== Macro Execution Complete ===")
                Log.i(TAG, "Duration: ${duration}ms")
                Log.i(TAG, "Steps executed: ${context.currentStepIndex}")

                MacroExecutionResult(
                    success = true,
                    message = "Macro '${macro.name}' completed successfully",
                    executionTimeMs = duration,
                    stepsExecuted = context.currentStepIndex
                )
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "Macro execution timeout after ${duration}ms", e)
            context.failExecution("Timeout after ${duration}ms")

            MacroExecutionResult(
                success = false,
                error = "Macro execution timeout after ${duration}ms",
                executionTimeMs = duration,
                stepsExecuted = context.currentStepIndex
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "Macro execution failed", e)
            context.failExecution(e.message ?: "Unknown error")

            MacroExecutionResult(
                success = false,
                error = e.message ?: "Unknown error",
                executionTimeMs = duration,
                stepsExecuted = context.currentStepIndex
            )
        }
    }

    /**
     * Execute a list of steps
     *
     * @param steps Steps to execute
     * @param context Execution context
     */
    private suspend fun executeSteps(steps: List<MacroStep>, context: MacroContext) {
        steps.forEachIndexed { index, step ->
            context.updateStepIndex(index)
            Log.d(TAG, "Executing step ${index + 1}/${steps.size}: ${step::class.simpleName}")

            when (step) {
                is MacroStep.Action -> executeAction(step, context)
                is MacroStep.Delay -> executeDelay(step)
                is MacroStep.Conditional -> executeConditional(step, context)
                is MacroStep.Loop -> executeLoop(step, context)
                is MacroStep.LoopWhile -> executeLoopWhile(step, context)
                is MacroStep.WaitFor -> executeWaitFor(step, context)
                is MacroStep.Variable -> executeVariable(step, context)
            }
        }
    }

    /**
     * Execute action step
     */
    private suspend fun executeAction(step: MacroStep.Action, @Suppress("UNUSED_PARAMETER") context: MacroContext) {
        Log.d(TAG, "Executing action: ${step.command.phrase}")

        val result = commandProcessor(step.command)

        if (!result.success) {
            throw MacroExecutionException("Action failed: ${step.command.phrase} - ${result.message}")
        }

        Log.d(TAG, "Action completed: ${result.message}")
    }

    /**
     * Execute delay step
     */
    private suspend fun executeDelay(step: MacroStep.Delay) {
        Log.d(TAG, "Delaying for ${step.millis}ms")
        delay(step.millis)
    }

    /**
     * Execute conditional step
     */
    private suspend fun executeConditional(step: MacroStep.Conditional, context: MacroContext) {
        Log.d(TAG, "Evaluating conditional")

        val conditionResult = try {
            step.condition(context)
        } catch (e: Exception) {
            Log.e(TAG, "Condition evaluation failed", e)
            false
        }

        Log.d(TAG, "Condition result: $conditionResult")

        if (conditionResult) {
            Log.d(TAG, "Executing 'then' branch (${step.thenSteps.size} steps)")
            executeSteps(step.thenSteps, context)
        } else if (step.elseSteps.isNotEmpty()) {
            Log.d(TAG, "Executing 'else' branch (${step.elseSteps.size} steps)")
            executeSteps(step.elseSteps, context)
        } else {
            Log.d(TAG, "No 'else' branch, skipping")
        }
    }

    /**
     * Execute loop step
     */
    private suspend fun executeLoop(step: MacroStep.Loop, context: MacroContext) {
        Log.d(TAG, "Executing loop: ${step.count} iterations")

        if (step.count > MAX_LOOP_ITERATIONS) {
            throw MacroExecutionException("Loop count exceeds maximum ($MAX_LOOP_ITERATIONS)")
        }

        repeat(step.count) { iteration ->
            Log.d(TAG, "Loop iteration ${iteration + 1}/${step.count}")
            executeSteps(step.steps, context)
        }
    }

    /**
     * Execute loop-while step
     */
    private suspend fun executeLoopWhile(step: MacroStep.LoopWhile, context: MacroContext) {
        Log.d(TAG, "Executing loop-while (max ${step.maxIterations} iterations)")

        var iteration = 0
        while (iteration < step.maxIterations) {
            val shouldContinue = try {
                step.condition(context)
            } catch (e: Exception) {
                Log.e(TAG, "Loop condition evaluation failed", e)
                false
            }

            if (!shouldContinue) {
                Log.d(TAG, "Loop condition false, exiting after $iteration iterations")
                break
            }

            Log.d(TAG, "Loop iteration ${iteration + 1}")
            executeSteps(step.steps, context)
            iteration++
        }

        if (iteration >= step.maxIterations) {
            Log.w(TAG, "Loop reached max iterations ($iteration)")
        }
    }

    /**
     * Execute wait-for step
     */
    private suspend fun executeWaitFor(step: MacroStep.WaitFor, context: MacroContext) {
        Log.d(TAG, "Waiting for condition (timeout: ${step.timeoutMillis}ms)")

        val startTime = System.currentTimeMillis()
        var conditionMet: Boolean

        while (System.currentTimeMillis() - startTime < step.timeoutMillis) {
            conditionMet = try {
                step.condition(context)
            } catch (e: Exception) {
                Log.e(TAG, "Wait condition evaluation failed", e)
                false
            }

            if (conditionMet) {
                val waitTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "Condition met after ${waitTime}ms")
                return
            }

            delay(step.checkIntervalMillis)
        }

        val waitTime = System.currentTimeMillis() - startTime
        throw MacroExecutionException("Wait condition timeout after ${waitTime}ms")
    }

    /**
     * Execute variable step
     */
    private suspend fun executeVariable(step: MacroStep.Variable, context: MacroContext) {
        Log.d(TAG, "Setting variable: ${step.name} = ${step.value}")
        context.setVariable(step.name, step.value)
    }
}

/**
 * Macro Execution Result
 *
 * @property success Whether macro executed successfully
 * @property message Success message (optional)
 * @property error Error message (optional)
 * @property executionTimeMs Total execution time in milliseconds
 * @property stepsExecuted Number of steps executed before completion/failure
 */
data class MacroExecutionResult(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val executionTimeMs: Long = 0,
    val stepsExecuted: Int = 0
)

/**
 * Command Result - Result of a single command execution
 *
 * @property success Whether command succeeded
 * @property message Result message
 */
data class CommandResult(
    val success: Boolean,
    val message: String? = null
)

/**
 * Macro Execution Exception
 */
class MacroExecutionException(message: String) : Exception(message)
