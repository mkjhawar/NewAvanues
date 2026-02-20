/**
 * ActionComposer.kt - Enhancement 5: Action Composition Framework
 *
 * Compose multiple actions into workflows with:
 * - Sequential execution (chain)
 * - Parallel execution
 * - Conditional logic
 *
 * Part of Q12 Enhancement 5
 *
 * @since VOS4 Phase 4.1
 * @author VOS4 Development Team
 */

package com.augmentalis.voiceoscore.commandmanager.plugins

import android.util.Log
import com.augmentalis.voiceoscore.commandmanager.dynamic.VoiceCommand
import com.augmentalis.voiceoscore.commandmanager.dynamic.CommandResult
import com.augmentalis.voiceoscore.commandmanager.dynamic.ErrorCode
import kotlinx.coroutines.*

/**
 * Compose multiple actions into complex workflows
 *
 * Supports three composition patterns:
 * 1. Sequential (chain): Execute actions one after another
 * 2. Parallel: Execute actions simultaneously
 * 3. Conditional: Execute actions based on conditions
 *
 * Example usage:
 * ```kotlin
 * val composer = ActionComposer()
 *
 * // Sequential: "select all" → "copy" → "paste"
 * val workflow = composer.chain(
 *     selectAllAction,
 *     copyAction,
 *     pasteAction
 * )
 *
 * // Parallel: Execute multiple actions at once
 * val parallelWorkflow = composer.parallel(
 *     playMusicAction,
 *     showVisualizerAction
 * )
 *
 * // Conditional: Different actions based on condition
 * val conditionalWorkflow = composer.conditional(
 *     condition = { context -> context.isNetworkAvailable() },
 *     ifTrue = cloudSyncAction,
 *     ifFalse = localSaveAction
 * )
 * ```
 */
class ActionComposer {
    companion object {
        private const val TAG = "ActionComposer"

        /** Maximum composition depth (prevent infinite recursion) */
        private const val MAX_DEPTH = 10
    }

    /**
     * Create sequential composition (chain)
     *
     * Actions execute one after another. If any action fails, the chain stops.
     *
     * @param actions Actions to chain
     * @return Composite action that executes all actions sequentially
     */
    fun chain(vararg actions: ActionPlugin): CompositeAction {
        return ChainedAction(actions.toList())
    }

    /**
     * Create sequential composition from list
     */
    fun chain(actions: List<ActionPlugin>): CompositeAction {
        return ChainedAction(actions)
    }

    /**
     * Create parallel composition
     *
     * All actions execute simultaneously. Result is success only if all succeed.
     *
     * @param actions Actions to run in parallel
     * @return Composite action that executes all actions in parallel
     */
    fun parallel(vararg actions: ActionPlugin): CompositeAction {
        return ParallelAction(actions.toList())
    }

    /**
     * Create parallel composition from list
     */
    fun parallel(actions: List<ActionPlugin>): CompositeAction {
        return ParallelAction(actions)
    }

    /**
     * Create conditional composition
     *
     * Executes different actions based on a condition.
     *
     * @param condition Condition to evaluate
     * @param ifTrue Action to execute if condition is true
     * @param ifFalse Action to execute if condition is false
     * @return Composite action that conditionally executes actions
     */
    fun conditional(
        condition: suspend (VoiceCommand) -> Boolean,
        ifTrue: ActionPlugin,
        ifFalse: ActionPlugin? = null
    ): CompositeAction {
        return ConditionalAction(condition, ifTrue, ifFalse)
    }

    /**
     * Create retry composition
     *
     * Retries an action if it fails, up to maxAttempts times.
     *
     * @param action Action to retry
     * @param maxAttempts Maximum number of attempts
     * @param delayMs Delay between retries (milliseconds)
     * @return Composite action that retries on failure
     */
    fun retry(
        action: ActionPlugin,
        maxAttempts: Int = 3,
        delayMs: Long = 1000
    ): CompositeAction {
        return RetryAction(action, maxAttempts, delayMs)
    }

    /**
     * Create fallback composition
     *
     * If primary action fails, executes fallback action.
     *
     * @param primary Primary action to try first
     * @param fallback Fallback action if primary fails
     * @return Composite action with fallback
     */
    fun fallback(
        primary: ActionPlugin,
        fallback: ActionPlugin
    ): CompositeAction {
        return FallbackAction(primary, fallback)
    }

    /**
     * Create rate-limited composition
     *
     * Limits how often an action can execute (e.g., max once per second).
     *
     * @param action Action to rate-limit
     * @param intervalMs Minimum interval between executions (milliseconds)
     * @return Composite action with rate limiting
     */
    fun rateLimit(
        action: ActionPlugin,
        intervalMs: Long
    ): CompositeAction {
        return RateLimitedAction(action, intervalMs)
    }
}

/**
 * Base class for composite actions
 *
 * Composite actions wrap other actions and add composition logic.
 */
abstract class CompositeAction : ActionPlugin {
    override val pluginId: String
        get() = "composite.${javaClass.simpleName}"

    override val version: String
        get() = "1.0.0"

    override val name: String
        get() = javaClass.simpleName

    override val description: String
        get() = "Composite action"

    override val author: String
        get() = "VOS4"

    override val supportedCommands: List<String>
        get() = emptyList() // Composites don't register commands directly

    override fun initialize(
        context: android.content.Context,
        permissions: PluginPermissions
    ) {
        // Composites don't need initialization
    }

    override fun shutdown() {
        // Composites don't need shutdown
    }

    /**
     * Get all child actions
     */
    abstract fun getChildActions(): List<ActionPlugin>
}

/**
 * Sequential composition (chain)
 *
 * Executes actions one after another. Stops on first failure.
 */
private class ChainedAction(
    private val actions: List<ActionPlugin>
) : CompositeAction() {

    override val description: String
        get() = "Chain of ${actions.size} actions"

    override suspend fun execute(command: VoiceCommand): CommandResult {
        if (actions.isEmpty()) {
            return CommandResult.Success
        }

        Log.d("ChainedAction", "Executing chain of ${actions.size} actions")

        for ((index, action) in actions.withIndex()) {
            Log.d("ChainedAction", "Executing step ${index + 1}/${actions.size}: ${action.pluginId}")

            val result = action.execute(command)

            when (result) {
                is CommandResult.Success -> continue
                is CommandResult.Error -> {
                    Log.e("ChainedAction", "Chain failed at step ${index + 1}: ${result.message}")
                    return CommandResult.Error(
                        "Chain failed at step ${index + 1}/${actions.size}: ${result.message}",
                        result.code,
                        result.cause
                    )
                }
                else -> {
                    Log.w("ChainedAction", "Chain interrupted at step ${index + 1}")
                    return result
                }
            }
        }

        Log.d("ChainedAction", "Chain completed successfully")
        return CommandResult.Success
    }

    override fun getChildActions(): List<ActionPlugin> = actions
}

/**
 * Parallel composition
 *
 * Executes all actions simultaneously. Success only if all succeed.
 */
private class ParallelAction(
    private val actions: List<ActionPlugin>
) : CompositeAction() {

    override val description: String
        get() = "Parallel execution of ${actions.size} actions"

    override suspend fun execute(command: VoiceCommand): CommandResult {
        if (actions.isEmpty()) {
            return CommandResult.Success
        }

        Log.d("ParallelAction", "Executing ${actions.size} actions in parallel")

        return coroutineScope {
            val results = actions.map { action ->
                async {
                    try {
                        action.execute(command)
                    } catch (e: Exception) {
                        CommandResult.Error(
                            "Parallel action failed: ${action.pluginId}",
                            ErrorCode.EXECUTION_FAILED,
                            e
                        )
                    }
                }
            }.awaitAll()

            // Check if all succeeded
            val failures = results.filterIsInstance<CommandResult.Error>()
            if (failures.isEmpty()) {
                Log.d("ParallelAction", "All parallel actions succeeded")
                CommandResult.Success
            } else {
                val errorMessages = failures.joinToString("; ") { it.message }
                Log.e("ParallelAction", "Parallel execution had ${failures.size} failures: $errorMessages")
                CommandResult.Error(
                    "Parallel execution failed: ${failures.size}/${actions.size} actions failed",
                    ErrorCode.EXECUTION_FAILED
                )
            }
        }
    }

    override fun getChildActions(): List<ActionPlugin> = actions
}

/**
 * Conditional composition
 *
 * Executes different actions based on condition.
 */
private class ConditionalAction(
    private val condition: suspend (VoiceCommand) -> Boolean,
    private val ifTrue: ActionPlugin,
    private val ifFalse: ActionPlugin?
) : CompositeAction() {

    override val description: String
        get() = "Conditional: if(condition) then ${ifTrue.pluginId} else ${ifFalse?.pluginId ?: "nothing"}"

    override suspend fun execute(command: VoiceCommand): CommandResult {
        Log.d("ConditionalAction", "Evaluating condition")

        return try {
            val conditionResult = condition(command)
            Log.d("ConditionalAction", "Condition evaluated to: $conditionResult")

            if (conditionResult) {
                Log.d("ConditionalAction", "Executing ifTrue branch: ${ifTrue.pluginId}")
                ifTrue.execute(command)
            } else if (ifFalse != null) {
                Log.d("ConditionalAction", "Executing ifFalse branch: ${ifFalse.pluginId}")
                ifFalse.execute(command)
            } else {
                Log.d("ConditionalAction", "Condition false, no ifFalse branch")
                CommandResult.Success
            }
        } catch (e: Exception) {
            Log.e("ConditionalAction", "Condition evaluation failed", e)
            CommandResult.Error(
                "Conditional action failed: ${e.message}",
                ErrorCode.EXECUTION_FAILED,
                e
            )
        }
    }

    override fun getChildActions(): List<ActionPlugin> =
        listOfNotNull(ifTrue, ifFalse)
}

/**
 * Retry composition
 *
 * Retries action on failure.
 */
private class RetryAction(
    private val action: ActionPlugin,
    private val maxAttempts: Int,
    private val delayMs: Long
) : CompositeAction() {

    override val description: String
        get() = "Retry ${action.pluginId} up to $maxAttempts times"

    override suspend fun execute(command: VoiceCommand): CommandResult {
        Log.d("RetryAction", "Executing with retry (max $maxAttempts attempts)")

        repeat(maxAttempts) { attempt ->
            Log.d("RetryAction", "Attempt ${attempt + 1}/$maxAttempts")

            val result = action.execute(command)

            when (result) {
                is CommandResult.Success -> {
                    Log.d("RetryAction", "Succeeded on attempt ${attempt + 1}")
                    return result
                }
                is CommandResult.Error -> {
                    if (attempt < maxAttempts - 1) {
                        Log.w("RetryAction", "Attempt ${attempt + 1} failed, retrying after ${delayMs}ms: ${result.message}")
                        delay(delayMs)
                    } else {
                        Log.e("RetryAction", "All $maxAttempts attempts failed")
                        return CommandResult.Error(
                            "Action failed after $maxAttempts attempts: ${result.message}",
                            result.code,
                            result.cause
                        )
                    }
                }
                else -> {
                    Log.w("RetryAction", "Non-retryable result: $result")
                    return result
                }
            }
        }

        return CommandResult.Error(
            "Retry exhausted",
            ErrorCode.EXECUTION_FAILED
        )
    }

    override fun getChildActions(): List<ActionPlugin> = listOf(action)
}

/**
 * Fallback composition
 *
 * Tries primary action, falls back to fallback on failure.
 */
private class FallbackAction(
    private val primary: ActionPlugin,
    private val fallback: ActionPlugin
) : CompositeAction() {

    override val description: String
        get() = "Try ${primary.pluginId}, fallback to ${fallback.pluginId}"

    override suspend fun execute(command: VoiceCommand): CommandResult {
        Log.d("FallbackAction", "Trying primary: ${primary.pluginId}")

        val primaryResult = primary.execute(command)

        return when (primaryResult) {
            is CommandResult.Success -> {
                Log.d("FallbackAction", "Primary succeeded")
                primaryResult
            }
            is CommandResult.Error -> {
                Log.w("FallbackAction", "Primary failed, trying fallback: ${fallback.pluginId}")
                fallback.execute(command)
            }
            else -> primaryResult
        }
    }

    override fun getChildActions(): List<ActionPlugin> =
        listOf(primary, fallback)
}

/**
 * Rate-limited composition
 *
 * Prevents action from executing too frequently.
 */
private class RateLimitedAction(
    private val action: ActionPlugin,
    private val intervalMs: Long
) : CompositeAction() {

    private var lastExecutionTime = 0L

    override val description: String
        get() = "Rate-limited ${action.pluginId} (max every ${intervalMs}ms)"

    override suspend fun execute(command: VoiceCommand): CommandResult {
        val now = System.currentTimeMillis()
        val timeSinceLastExecution = now - lastExecutionTime

        if (timeSinceLastExecution < intervalMs) {
            val waitTime = intervalMs - timeSinceLastExecution
            Log.w("RateLimitedAction", "Rate limit: must wait ${waitTime}ms")
            return CommandResult.Error(
                "Rate limit: action can execute again in ${waitTime}ms",
                ErrorCode.RESOURCE_UNAVAILABLE
            )
        }

        Log.d("RateLimitedAction", "Executing (last execution: ${timeSinceLastExecution}ms ago)")
        lastExecutionTime = now
        return action.execute(command)
    }

    override fun getChildActions(): List<ActionPlugin> = listOf(action)
}
