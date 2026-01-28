/**
 * MacroStep.kt - Sealed class hierarchy for macro execution steps
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-09
 *
 * Purpose: Define different types of macro steps for voice command sequences
 * Key Features:
 * - Action steps (execute commands)
 * - Delay steps (timing control)
 * - Conditional steps (if/else branching)
 * - Loop steps (repeat sequences)
 * - Variable steps (store/retrieve data)
 */
package com.augmentalis.voiceoscore.macros

/**
 * MacroStep - Base sealed class for all macro step types
 *
 * A macro is composed of multiple steps that execute sequentially.
 * Each step type serves a specific purpose in the macro workflow.
 *
 * ## Step Types
 *
 * 1. **Action** - Execute a voice command
 *    ```kotlin
 *    MacroStep.Action(VoiceCommand(id = "click_submit", phrase = "click submit"))
 *    ```
 *
 * 2. **Delay** - Wait for specified milliseconds
 *    ```kotlin
 *    MacroStep.Delay(millis = 1000) // Wait 1 second
 *    ```
 *
 * 3. **Conditional** - Execute different steps based on condition
 *    ```kotlin
 *    MacroStep.Conditional(
 *        condition = { screenContains("error") },
 *        thenSteps = listOf(MacroStep.Action(retry)),
 *        elseSteps = listOf(MacroStep.Action(proceed))
 *    )
 *    ```
 *
 * 4. **Loop** - Repeat steps N times
 *    ```kotlin
 *    MacroStep.Loop(
 *        count = 3,
 *        steps = listOf(MacroStep.Action(swipeDown))
 *    )
 *    ```
 *
 * 5. **Variable** - Store/retrieve variable data
 *    ```kotlin
 *    MacroStep.Variable(name = "username", value = "john_doe")
 *    ```
 */
sealed class MacroStep {

    /**
     * Action Step - Execute a voice command
     *
     * This step executes a single voice command action.
     * The command is processed through the standard voice command system.
     *
     * @property command Voice command to execute
     *
     * Example:
     * ```kotlin
     * val clickButton = MacroStep.Action(
     *     VoiceCommand(
     *         id = "click_login",
     *         phrase = "click login button",
     *         actionType = "click"
     *     )
     * )
     * ```
     */
    data class Action(
        val command: VoiceCommand
    ) : MacroStep()

    /**
     * Delay Step - Wait for specified milliseconds
     *
     * This step pauses macro execution for the specified duration.
     * Useful for waiting for UI animations, network requests, or screen transitions.
     *
     * @property millis Milliseconds to wait (max 30 seconds)
     *
     * Example:
     * ```kotlin
     * val waitForAnimation = MacroStep.Delay(millis = 500)
     * ```
     */
    data class Delay(
        val millis: Long
    ) : MacroStep() {
        init {
            require(millis >= 0) { "Delay must be non-negative" }
            require(millis <= 30000) { "Delay cannot exceed 30 seconds" }
        }
    }

    /**
     * Conditional Step - Execute steps based on condition
     *
     * This step evaluates a condition and executes either the "then" or "else" branch.
     * Conditions have access to MacroContext for screen state checking.
     *
     * @property condition Lambda that returns true/false
     * @property thenSteps Steps to execute if condition is true
     * @property elseSteps Steps to execute if condition is false (optional)
     *
     * Example:
     * ```kotlin
     * MacroStep.Conditional(
     *     condition = { context.screenContains("Login successful") },
     *     thenSteps = listOf(
     *         MacroStep.Action(proceedToHome)
     *     ),
     *     elseSteps = listOf(
     *         MacroStep.Action(showError)
     *     )
     * )
     * ```
     */
    data class Conditional(
        val condition: (MacroContext) -> Boolean,
        val thenSteps: List<MacroStep>,
        val elseSteps: List<MacroStep> = emptyList()
    ) : MacroStep()

    /**
     * Loop Step - Repeat steps N times
     *
     * This step executes a list of steps multiple times.
     * Use for repetitive actions like scrolling or swiping.
     *
     * @property count Number of iterations (1-100)
     * @property steps Steps to repeat
     *
     * Example:
     * ```kotlin
     * MacroStep.Loop(
     *     count = 5,
     *     steps = listOf(
     *         MacroStep.Action(swipeDown),
     *         MacroStep.Delay(300)
     *     )
     * )
     * ```
     */
    data class Loop(
        val count: Int,
        val steps: List<MacroStep>
    ) : MacroStep() {
        init {
            require(count > 0) { "Loop count must be positive" }
            require(count <= 100) { "Loop count cannot exceed 100" }
        }
    }

    /**
     * Variable Step - Store or retrieve variable data
     *
     * This step sets a variable in the macro context.
     * Variables can be used in conditional steps or command parameters.
     *
     * @property name Variable name
     * @property value Variable value (Any type)
     *
     * Example:
     * ```kotlin
     * MacroStep.Variable(name = "attemptCount", value = 3)
     * MacroStep.Variable(name = "username", value = "john_doe")
     * MacroStep.Variable(name = "isFirstRun", value = true)
     * ```
     */
    data class Variable(
        val name: String,
        val value: Any
    ) : MacroStep() {
        init {
            require(name.isNotBlank()) { "Variable name cannot be blank" }
        }
    }

    /**
     * LoopWhile Step - Repeat steps while condition is true
     *
     * This step executes steps repeatedly while a condition remains true.
     * Includes a maximum iteration limit to prevent infinite loops.
     *
     * @property condition Lambda that returns true to continue looping
     * @property steps Steps to repeat
     * @property maxIterations Maximum number of iterations (default 50)
     *
     * Example:
     * ```kotlin
     * MacroStep.LoopWhile(
     *     condition = { !context.screenContains("End of list") },
     *     steps = listOf(
     *         MacroStep.Action(scrollDown),
     *         MacroStep.Delay(200)
     *     ),
     *     maxIterations = 20
     * )
     * ```
     */
    data class LoopWhile(
        val condition: (MacroContext) -> Boolean,
        val steps: List<MacroStep>,
        val maxIterations: Int = 50
    ) : MacroStep() {
        init {
            require(maxIterations > 0) { "Max iterations must be positive" }
            require(maxIterations <= 100) { "Max iterations cannot exceed 100" }
        }
    }

    /**
     * WaitFor Step - Wait until condition is true
     *
     * This step waits until a condition becomes true or timeout is reached.
     * Useful for waiting for specific UI elements or screen states.
     *
     * @property condition Lambda that returns true when ready to proceed
     * @property timeoutMillis Maximum time to wait (default 5000ms)
     * @property checkIntervalMillis How often to check condition (default 100ms)
     *
     * Example:
     * ```kotlin
     * MacroStep.WaitFor(
     *     condition = { context.screenContains("Submit button") },
     *     timeoutMillis = 3000,
     *     checkIntervalMillis = 200
     * )
     * ```
     */
    data class WaitFor(
        val condition: (MacroContext) -> Boolean,
        val timeoutMillis: Long = 5000,
        val checkIntervalMillis: Long = 100
    ) : MacroStep() {
        init {
            require(timeoutMillis > 0) { "Timeout must be positive" }
            require(timeoutMillis <= 30000) { "Timeout cannot exceed 30 seconds" }
            require(checkIntervalMillis > 0) { "Check interval must be positive" }
            require(checkIntervalMillis < timeoutMillis) { "Check interval must be less than timeout" }
        }
    }
}

/**
 * Voice Command - Data class representing a voice command action
 *
 * This is used within MacroStep.Action to define the command to execute.
 *
 * @property id Unique command identifier
 * @property phrase Command phrase (e.g., "click submit")
 * @property actionType Action type (click, type, scroll, etc.)
 * @property parameters Additional parameters (e.g., text for type action)
 */
data class VoiceCommand(
    val id: String,
    val phrase: String,
    val actionType: String = "click",
    val parameters: Map<String, Any> = emptyMap()
)
