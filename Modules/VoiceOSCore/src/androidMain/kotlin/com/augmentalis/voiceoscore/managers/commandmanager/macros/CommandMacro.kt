/**
 * CommandMacro.kt - Data class for voice command macros
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-09
 *
 * Purpose: Define macro structure and metadata
 * Key Features:
 * - Multi-step sequences (up to 20 steps)
 * - Metadata (name, description, author)
 * - Validation and constraints
 */
package com.augmentalis.voiceoscore.managers.commandmanager.macros

/**
 * Command Macro
 *
 * Represents a multi-step voice command sequence.
 * Macros enable users to automate complex workflows with a single voice command.
 *
 * ## Example
 *
 * ```kotlin
 * val loginMacro = CommandMacro(
 *     id = "login_workflow",
 *     name = "Login Workflow",
 *     description = "Complete login process",
 *     steps = listOf(
 *         MacroStep.Action(VoiceCommand(id = "open_app", phrase = "open app")),
 *         MacroStep.Delay(1000),
 *         MacroStep.Action(VoiceCommand(id = "click_login", phrase = "click login")),
 *         MacroStep.Delay(500),
 *         MacroStep.Action(VoiceCommand(id = "enter_username", phrase = "type username")),
 *         MacroStep.Action(VoiceCommand(id = "enter_password", phrase = "type password")),
 *         MacroStep.Action(VoiceCommand(id = "submit", phrase = "click submit"))
 *     ),
 *     triggerPhrase = "login"
 * )
 * ```
 *
 * @property id Unique macro identifier
 * @property name Human-readable macro name
 * @property description What the macro does
 * @property steps List of macro steps (max 20)
 * @property triggerPhrase Voice phrase to trigger macro
 * @property author Macro creator (optional)
 * @property tags Categorization tags (optional)
 * @property isEnabled Whether macro is active
 * @property createdAt Creation timestamp
 * @property modifiedAt Last modification timestamp
 */
data class CommandMacro(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<MacroStep>,
    val triggerPhrase: String,
    val author: String? = null,
    val tags: List<String> = emptyList(),
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {

    companion object {
        const val MAX_STEPS = 20
        const val MAX_NAME_LENGTH = 50
        const val MAX_DESCRIPTION_LENGTH = 200
    }

    init {
        // Validation
        require(id.isNotBlank()) { "Macro ID cannot be blank" }
        require(name.isNotBlank()) { "Macro name cannot be blank" }
        require(name.length <= MAX_NAME_LENGTH) { "Name exceeds max length ($MAX_NAME_LENGTH)" }
        require(description.length <= MAX_DESCRIPTION_LENGTH) { "Description exceeds max length ($MAX_DESCRIPTION_LENGTH)" }
        require(steps.isNotEmpty()) { "Macro must have at least one step" }
        require(steps.size <= MAX_STEPS) { "Macro exceeds max steps ($MAX_STEPS)" }
        require(triggerPhrase.isNotBlank()) { "Trigger phrase cannot be blank" }
    }

    /**
     * Get total step count (including nested steps in conditionals and loops)
     *
     * @return Total number of steps
     */
    fun getTotalStepCount(): Int {
        return countSteps(steps)
    }

    /**
     * Recursively count all steps
     */
    private fun countSteps(stepList: List<MacroStep>): Int {
        var count = 0
        stepList.forEach { step ->
            count++ // Count this step
            when (step) {
                is MacroStep.Conditional -> {
                    count += countSteps(step.thenSteps)
                    count += countSteps(step.elseSteps)
                }
                is MacroStep.Loop -> {
                    count += countSteps(step.steps) * step.count
                }
                is MacroStep.LoopWhile -> {
                    count += countSteps(step.steps) * step.maxIterations // Worst case
                }
                else -> {} // Other steps don't have nested steps
            }
        }
        return count
    }

    /**
     * Get estimated execution time in milliseconds
     *
     * This is a rough estimate based on:
     * - Action steps: 500ms each
     * - Delay steps: actual delay time
     * - Conditional/Loop steps: estimate based on nested steps
     *
     * @return Estimated duration in ms
     */
    fun getEstimatedDuration(): Long {
        return estimateDuration(steps)
    }

    /**
     * Recursively estimate duration
     */
    private fun estimateDuration(stepList: List<MacroStep>): Long {
        var duration = 0L
        stepList.forEach { step ->
            when (step) {
                is MacroStep.Action -> duration += 500 // Assume 500ms per action
                is MacroStep.Delay -> duration += step.millis
                is MacroStep.Conditional -> {
                    // Estimate worst case (longer branch)
                    val thenDuration = estimateDuration(step.thenSteps)
                    val elseDuration = estimateDuration(step.elseSteps)
                    duration += maxOf(thenDuration, elseDuration)
                }
                is MacroStep.Loop -> {
                    duration += estimateDuration(step.steps) * step.count
                }
                is MacroStep.LoopWhile -> {
                    // Estimate average case (half of max iterations)
                    duration += estimateDuration(step.steps) * (step.maxIterations / 2)
                }
                is MacroStep.WaitFor -> {
                    duration += step.timeoutMillis // Worst case
                }
                is MacroStep.Variable -> duration += 10 // Negligible
            }
        }
        return duration
    }

    /**
     * Validate macro structure
     *
     * Checks for common issues like:
     * - Empty conditional branches
     * - Excessive delays
     * - Deep nesting
     *
     * @return Validation result
     */
    fun validate(): MacroValidation {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        // Check for excessive delays
        val totalDelay = calculateTotalDelay(steps)
        if (totalDelay > 30000) {
            warnings.add("Total delay exceeds 30 seconds ($totalDelay ms)")
        }

        // Check nesting depth
        val maxDepth = calculateMaxDepth(steps)
        if (maxDepth > 3) {
            warnings.add("Deep nesting detected (depth: $maxDepth). Consider simplifying.")
        }

        // Check for empty steps
        if (steps.any { it is MacroStep.Conditional && it.thenSteps.isEmpty() && it.elseSteps.isEmpty() }) {
            errors.add("Conditional step has no branches")
        }

        return MacroValidation(
            isValid = errors.isEmpty(),
            warnings = warnings,
            errors = errors
        )
    }

    /**
     * Calculate total delay time
     */
    private fun calculateTotalDelay(stepList: List<MacroStep>): Long {
        var totalDelay = 0L
        stepList.forEach { step ->
            when (step) {
                is MacroStep.Delay -> totalDelay += step.millis
                is MacroStep.Conditional -> {
                    totalDelay += calculateTotalDelay(step.thenSteps)
                    totalDelay += calculateTotalDelay(step.elseSteps)
                }
                is MacroStep.Loop -> {
                    totalDelay += calculateTotalDelay(step.steps) * step.count
                }
                is MacroStep.LoopWhile -> {
                    totalDelay += calculateTotalDelay(step.steps) * step.maxIterations
                }
                else -> {}
            }
        }
        return totalDelay
    }

    /**
     * Calculate maximum nesting depth
     */
    private fun calculateMaxDepth(stepList: List<MacroStep>, currentDepth: Int = 0): Int {
        var maxDepth = currentDepth
        stepList.forEach { step ->
            when (step) {
                is MacroStep.Conditional -> {
                    maxDepth = maxOf(
                        maxDepth,
                        calculateMaxDepth(step.thenSteps, currentDepth + 1),
                        calculateMaxDepth(step.elseSteps, currentDepth + 1)
                    )
                }
                is MacroStep.Loop -> {
                    maxDepth = maxOf(maxDepth, calculateMaxDepth(step.steps, currentDepth + 1))
                }
                is MacroStep.LoopWhile -> {
                    maxDepth = maxOf(maxDepth, calculateMaxDepth(step.steps, currentDepth + 1))
                }
                else -> {}
            }
        }
        return maxDepth
    }

    /**
     * Create a copy with updated modification timestamp
     *
     * @return Updated macro
     */
    fun touch(): CommandMacro {
        return copy(modifiedAt = System.currentTimeMillis())
    }

    /**
     * Create a disabled copy
     *
     * @return Disabled macro
     */
    fun disable(): CommandMacro {
        return copy(isEnabled = false, modifiedAt = System.currentTimeMillis())
    }

    /**
     * Create an enabled copy
     *
     * @return Enabled macro
     */
    fun enable(): CommandMacro {
        return copy(isEnabled = true, modifiedAt = System.currentTimeMillis())
    }
}

/**
 * Macro Validation Result
 *
 * @property isValid Whether macro passed validation
 * @property warnings Non-critical issues
 * @property errors Critical issues that prevent execution
 */
data class MacroValidation(
    val isValid: Boolean,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
) {
    fun hasWarnings(): Boolean = warnings.isNotEmpty()
    fun hasErrors(): Boolean = errors.isNotEmpty()
}
