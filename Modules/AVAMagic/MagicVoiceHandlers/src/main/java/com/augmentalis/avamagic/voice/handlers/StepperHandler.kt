/**
 * StepperHandler.kt - Voice handler for Stepper/Spinner/NumberPicker interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven numeric stepper control with absolute and relative value adjustments
 * Features:
 * - Set stepper to specific numeric value
 * - Increment/decrement by step amount or custom amount
 * - Quick presets (minimum, maximum, reset to default)
 * - Named stepper targeting (e.g., "set quantity to 5")
 * - Focused stepper targeting (e.g., "set to 10")
 * - AVID-based targeting for precise element selection
 * - Voice feedback for value changes
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Absolute value:
 * - "set [name] to [value]" - Set named stepper to value
 * - "set to [value]" - Set focused stepper to value
 *
 * Relative adjustment (single step):
 * - "increase" / "plus" / "add one" - Increment by one step
 * - "decrease" / "minus" / "subtract one" - Decrement by one step
 *
 * Relative adjustment (multiple steps):
 * - "increase by [N]" / "add [N]" - Increment by N
 * - "decrease by [N]" / "subtract [N]" - Decrement by N
 *
 * Presets:
 * - "minimum" / "min" - Set to stepper minimum
 * - "maximum" / "max" - Set to stepper maximum
 * - "reset" - Reset to default value
 *
 * ## Value Parsing
 *
 * Supports:
 * - Integer values: "5", "10"
 * - Decimal values: "2.5", "7.5"
 * - Word numbers: "five", "twenty five"
 */

package com.augmentalis.avamagic.voice.handlers

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Stepper/Spinner/NumberPicker interactions.
 *
 * Provides comprehensive voice control for stepper components including:
 * - Absolute value setting (numeric)
 * - Relative value adjustment (increment/decrement by step or custom amount)
 * - Quick presets (min, max, reset)
 * - Named stepper targeting with disambiguation
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for stepper operations
 */
class StepperHandler(
    private val executor: StepperExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "StepperHandler"

        // Patterns for parsing commands
        private val SET_TO_PATTERN = Regex(
            """^set\s+(?:(.+?)\s+)?to\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val INCREASE_BY_PATTERN = Regex(
            """^(?:increase|add)\s+(?:(.+?)\s+)?by\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val DECREASE_BY_PATTERN = Regex(
            """^(?:decrease|subtract)\s+(?:(.+?)\s+)?by\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val ADD_N_PATTERN = Regex(
            """^add\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val SUBTRACT_N_PATTERN = Regex(
            """^subtract\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // Word to number mapping for common spoken numbers
        private val WORD_NUMBERS = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
            "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
            "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
            "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
            "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "thirty" to 30,
            "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
            "eighty" to 80, "ninety" to 90, "hundred" to 100
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Set absolute value
        "set to", "set [name] to",
        // Increase (single step)
        "increase", "plus", "add one",
        // Decrease (single step)
        "decrease", "minus", "subtract one",
        // Increase by N
        "increase by", "add", "add [N]",
        // Decrease by N
        "decrease by", "subtract", "subtract [N]",
        // Presets
        "minimum", "min", "maximum", "max", "reset"
    )

    /**
     * Callback for voice feedback when stepper value changes.
     */
    var onValueChanged: ((stepperName: String, newValue: Double) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing stepper command: $normalizedAction")

        return try {
            when {
                // Set to specific value: "set [name] to [value]" or "set to [value]"
                SET_TO_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSetTo(normalizedAction, command)
                }

                // Increase by amount: "increase [name] by [amount]" or "increase by [amount]"
                INCREASE_BY_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleIncreaseBy(normalizedAction, command)
                }

                // Decrease by amount: "decrease [name] by [amount]" or "decrease by [amount]"
                DECREASE_BY_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleDecreaseBy(normalizedAction, command)
                }

                // Add N: "add 5" (without "by")
                ADD_N_PATTERN.containsMatchIn(normalizedAction) &&
                        normalizedAction != "add one" -> {
                    handleAddN(normalizedAction, command)
                }

                // Subtract N: "subtract 5" (without "by")
                SUBTRACT_N_PATTERN.containsMatchIn(normalizedAction) &&
                        normalizedAction != "subtract one" -> {
                    handleSubtractN(normalizedAction, command)
                }

                // Simple increase commands (single step)
                normalizedAction in listOf("increase", "plus", "add one") -> {
                    handleSimpleIncrement(command)
                }

                // Simple decrease commands (single step)
                normalizedAction in listOf("decrease", "minus", "subtract one") -> {
                    handleSimpleDecrement(command)
                }

                // Presets: minimum
                normalizedAction in listOf("minimum", "min") -> {
                    handlePreset(StepperPreset.MINIMUM, command)
                }

                // Presets: maximum
                normalizedAction in listOf("maximum", "max") -> {
                    handlePreset(StepperPreset.MAXIMUM, command)
                }

                // Presets: reset
                normalizedAction == "reset" -> {
                    handlePreset(StepperPreset.RESET, command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing stepper command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "set [name] to [value]" command.
     */
    private suspend fun handleSetTo(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SET_TO_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse set command")

        val stepperName = matchResult.groupValues[1].takeIf { it.isNotBlank() }
        val valueString = matchResult.groupValues[2]

        // Parse the value
        val parsedValue = parseValue(valueString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse value: '$valueString'",
                recoverable = true,
                suggestedAction = "Try 'set to 5' or 'set quantity to 10'"
            )

        // Find the stepper
        val stepperInfo = findStepper(
            name = stepperName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (stepperName != null) "Stepper '$stepperName' not found" else "No stepper focused",
            recoverable = true,
            suggestedAction = "Focus on a stepper or say 'set quantity to 5'"
        )

        // Clamp to stepper bounds
        val clampedValue = parsedValue.coerceIn(stepperInfo.minValue, stepperInfo.maxValue)

        // Apply the value
        return applySetValue(stepperInfo, clampedValue)
    }

    /**
     * Handle "increase [name] by [amount]" command.
     */
    private suspend fun handleIncreaseBy(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = INCREASE_BY_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse increase command")

        val stepperName = matchResult.groupValues[1].takeIf { it.isNotBlank() }
        val amountString = matchResult.groupValues[2]

        // Parse the amount
        val amount = parseValue(amountString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse amount: '$amountString'",
                recoverable = true,
                suggestedAction = "Try 'increase by 5' or 'add 10'"
            )

        // Find the stepper
        val stepperInfo = findStepper(
            name = stepperName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (stepperName != null) "Stepper '$stepperName' not found" else "No stepper focused",
            recoverable = true,
            suggestedAction = "Focus on a stepper first"
        )

        return applyIncrementBy(stepperInfo, amount)
    }

    /**
     * Handle "decrease [name] by [amount]" command.
     */
    private suspend fun handleDecreaseBy(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = DECREASE_BY_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse decrease command")

        val stepperName = matchResult.groupValues[1].takeIf { it.isNotBlank() }
        val amountString = matchResult.groupValues[2]

        // Parse the amount
        val amount = parseValue(amountString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse amount: '$amountString'",
                recoverable = true,
                suggestedAction = "Try 'decrease by 5' or 'subtract 10'"
            )

        // Find the stepper
        val stepperInfo = findStepper(
            name = stepperName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (stepperName != null) "Stepper '$stepperName' not found" else "No stepper focused",
            recoverable = true,
            suggestedAction = "Focus on a stepper first"
        )

        return applyDecrementBy(stepperInfo, amount)
    }

    /**
     * Handle "add [N]" command (without "by").
     */
    private suspend fun handleAddN(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = ADD_N_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse add command")

        val amountString = matchResult.groupValues[1]

        // Parse the amount
        val amount = parseValue(amountString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse amount: '$amountString'",
                recoverable = true,
                suggestedAction = "Try 'add 5' or 'add ten'"
            )

        // Find the stepper
        val stepperInfo = findStepper(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No stepper focused",
                recoverable = true,
                suggestedAction = "Focus on a stepper first"
            )

        return applyIncrementBy(stepperInfo, amount)
    }

    /**
     * Handle "subtract [N]" command (without "by").
     */
    private suspend fun handleSubtractN(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SUBTRACT_N_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse subtract command")

        val amountString = matchResult.groupValues[1]

        // Parse the amount
        val amount = parseValue(amountString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse amount: '$amountString'",
                recoverable = true,
                suggestedAction = "Try 'subtract 5' or 'subtract ten'"
            )

        // Find the stepper
        val stepperInfo = findStepper(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No stepper focused",
                recoverable = true,
                suggestedAction = "Focus on a stepper first"
            )

        return applyDecrementBy(stepperInfo, amount)
    }

    /**
     * Handle simple "increase" / "plus" / "add one" command (increment by single step).
     */
    private suspend fun handleSimpleIncrement(command: QuantizedCommand): HandlerResult {
        val stepperInfo = findStepper(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No stepper focused",
                recoverable = true,
                suggestedAction = "Focus on a stepper first"
            )

        return applyIncrement(stepperInfo)
    }

    /**
     * Handle simple "decrease" / "minus" / "subtract one" command (decrement by single step).
     */
    private suspend fun handleSimpleDecrement(command: QuantizedCommand): HandlerResult {
        val stepperInfo = findStepper(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No stepper focused",
                recoverable = true,
                suggestedAction = "Focus on a stepper first"
            )

        return applyDecrement(stepperInfo)
    }

    /**
     * Handle preset commands (min, max, reset).
     */
    private suspend fun handlePreset(
        preset: StepperPreset,
        command: QuantizedCommand
    ): HandlerResult {
        val stepperInfo = findStepper(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No stepper focused",
                recoverable = true,
                suggestedAction = "Focus on a stepper first"
            )

        return when (preset) {
            StepperPreset.MINIMUM -> applySetToMin(stepperInfo)
            StepperPreset.MAXIMUM -> applySetToMax(stepperInfo)
            StepperPreset.RESET -> applyReset(stepperInfo)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find stepper by name, AVID, or focus state.
     */
    private suspend fun findStepper(
        name: String? = null,
        avid: String? = null
    ): StepperInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val stepper = executor.findByAvid(avid)
            if (stepper != null) return stepper
        }

        // Priority 2: Name lookup
        if (name != null) {
            val stepper = executor.findByName(name)
            if (stepper != null) return stepper
        }

        // Priority 3: Focused stepper
        return executor.findFocused()
    }

    /**
     * Apply setValue operation and return result.
     */
    private suspend fun applySetValue(stepperInfo: StepperInfo, targetValue: Double): HandlerResult {
        val result = executor.setValue(stepperInfo, targetValue)

        return if (result.success) {
            // Invoke callback for voice feedback
            onValueChanged?.invoke(
                stepperInfo.name.ifBlank { "Stepper" },
                result.newValue
            )

            // Build feedback message
            val feedback = buildFeedbackMessage(stepperInfo, "set to", result.newValue)

            Log.i(TAG, "Stepper value set: ${stepperInfo.name} = ${result.newValue}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "stepperName" to stepperInfo.name,
                    "stepperAvid" to stepperInfo.avid,
                    "previousValue" to result.previousValue,
                    "newValue" to result.newValue,
                    "minValue" to stepperInfo.minValue,
                    "maxValue" to stepperInfo.maxValue,
                    "stepValue" to stepperInfo.stepValue,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set stepper value",
                recoverable = true
            )
        }
    }

    /**
     * Apply increment operation and return result.
     */
    private suspend fun applyIncrement(stepperInfo: StepperInfo): HandlerResult {
        val result = executor.increment(stepperInfo)

        return if (result.success) {
            onValueChanged?.invoke(
                stepperInfo.name.ifBlank { "Stepper" },
                result.newValue
            )

            val feedback = buildFeedbackMessage(stepperInfo, "increased to", result.newValue)

            Log.i(TAG, "Stepper incremented: ${stepperInfo.name} = ${result.newValue}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "stepperName" to stepperInfo.name,
                    "stepperAvid" to stepperInfo.avid,
                    "previousValue" to result.previousValue,
                    "newValue" to result.newValue,
                    "operation" to "increment",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not increment stepper",
                recoverable = true
            )
        }
    }

    /**
     * Apply decrement operation and return result.
     */
    private suspend fun applyDecrement(stepperInfo: StepperInfo): HandlerResult {
        val result = executor.decrement(stepperInfo)

        return if (result.success) {
            onValueChanged?.invoke(
                stepperInfo.name.ifBlank { "Stepper" },
                result.newValue
            )

            val feedback = buildFeedbackMessage(stepperInfo, "decreased to", result.newValue)

            Log.i(TAG, "Stepper decremented: ${stepperInfo.name} = ${result.newValue}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "stepperName" to stepperInfo.name,
                    "stepperAvid" to stepperInfo.avid,
                    "previousValue" to result.previousValue,
                    "newValue" to result.newValue,
                    "operation" to "decrement",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not decrement stepper",
                recoverable = true
            )
        }
    }

    /**
     * Apply incrementBy operation and return result.
     */
    private suspend fun applyIncrementBy(stepperInfo: StepperInfo, amount: Double): HandlerResult {
        val result = executor.incrementBy(stepperInfo, amount)

        return if (result.success) {
            onValueChanged?.invoke(
                stepperInfo.name.ifBlank { "Stepper" },
                result.newValue
            )

            val feedback = buildFeedbackMessage(stepperInfo, "increased to", result.newValue)

            Log.i(TAG, "Stepper incremented by $amount: ${stepperInfo.name} = ${result.newValue}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "stepperName" to stepperInfo.name,
                    "stepperAvid" to stepperInfo.avid,
                    "previousValue" to result.previousValue,
                    "newValue" to result.newValue,
                    "operation" to "incrementBy",
                    "amount" to amount,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not increment stepper",
                recoverable = true
            )
        }
    }

    /**
     * Apply decrementBy operation and return result.
     */
    private suspend fun applyDecrementBy(stepperInfo: StepperInfo, amount: Double): HandlerResult {
        val result = executor.decrementBy(stepperInfo, amount)

        return if (result.success) {
            onValueChanged?.invoke(
                stepperInfo.name.ifBlank { "Stepper" },
                result.newValue
            )

            val feedback = buildFeedbackMessage(stepperInfo, "decreased to", result.newValue)

            Log.i(TAG, "Stepper decremented by $amount: ${stepperInfo.name} = ${result.newValue}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "stepperName" to stepperInfo.name,
                    "stepperAvid" to stepperInfo.avid,
                    "previousValue" to result.previousValue,
                    "newValue" to result.newValue,
                    "operation" to "decrementBy",
                    "amount" to amount,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not decrement stepper",
                recoverable = true
            )
        }
    }

    /**
     * Apply setToMin operation and return result.
     */
    private suspend fun applySetToMin(stepperInfo: StepperInfo): HandlerResult {
        val result = executor.setToMin(stepperInfo)

        return if (result.success) {
            onValueChanged?.invoke(
                stepperInfo.name.ifBlank { "Stepper" },
                result.newValue
            )

            val feedback = buildFeedbackMessage(stepperInfo, "set to minimum", result.newValue)

            Log.i(TAG, "Stepper set to minimum: ${stepperInfo.name} = ${result.newValue}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "stepperName" to stepperInfo.name,
                    "stepperAvid" to stepperInfo.avid,
                    "previousValue" to result.previousValue,
                    "newValue" to result.newValue,
                    "operation" to "setToMin",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set stepper to minimum",
                recoverable = true
            )
        }
    }

    /**
     * Apply setToMax operation and return result.
     */
    private suspend fun applySetToMax(stepperInfo: StepperInfo): HandlerResult {
        val result = executor.setToMax(stepperInfo)

        return if (result.success) {
            onValueChanged?.invoke(
                stepperInfo.name.ifBlank { "Stepper" },
                result.newValue
            )

            val feedback = buildFeedbackMessage(stepperInfo, "set to maximum", result.newValue)

            Log.i(TAG, "Stepper set to maximum: ${stepperInfo.name} = ${result.newValue}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "stepperName" to stepperInfo.name,
                    "stepperAvid" to stepperInfo.avid,
                    "previousValue" to result.previousValue,
                    "newValue" to result.newValue,
                    "operation" to "setToMax",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set stepper to maximum",
                recoverable = true
            )
        }
    }

    /**
     * Apply reset operation and return result.
     */
    private suspend fun applyReset(stepperInfo: StepperInfo): HandlerResult {
        val result = executor.reset(stepperInfo)

        return if (result.success) {
            onValueChanged?.invoke(
                stepperInfo.name.ifBlank { "Stepper" },
                result.newValue
            )

            val feedback = buildFeedbackMessage(stepperInfo, "reset to", result.newValue)

            Log.i(TAG, "Stepper reset: ${stepperInfo.name} = ${result.newValue}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "stepperName" to stepperInfo.name,
                    "stepperAvid" to stepperInfo.avid,
                    "previousValue" to result.previousValue,
                    "newValue" to result.newValue,
                    "operation" to "reset",
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not reset stepper",
                recoverable = true
            )
        }
    }

    /**
     * Build feedback message for voice announcement.
     */
    private fun buildFeedbackMessage(
        stepperInfo: StepperInfo,
        action: String,
        value: Double
    ): String {
        return buildString {
            if (stepperInfo.name.isNotBlank()) {
                append(stepperInfo.name)
                append(" ")
                append(action)
                append(" ")
            } else {
                append(action.replaceFirstChar { it.uppercase() })
                append(" ")
            }
            append(formatValue(value))
        }
    }

    /**
     * Parse a value string into a numeric value.
     *
     * Supports:
     * - "5" -> 5.0
     * - "10" -> 10.0
     * - "2.5" -> 2.5
     * - "five" -> 5.0
     * - "twenty five" -> 25.0
     */
    private fun parseValue(input: String): Double? {
        val trimmed = input.trim().lowercase()

        // Try direct numeric parsing
        trimmed.toDoubleOrNull()?.let {
            return it
        }

        // Try word number parsing
        parseWordNumber(trimmed)?.let {
            return it.toDouble()
        }

        return null
    }

    /**
     * Parse word numbers like "five", "twenty five", etc.
     */
    private fun parseWordNumber(input: String): Int? {
        // Direct match
        WORD_NUMBERS[input]?.let { return it }

        // Compound numbers (e.g., "twenty five", "thirty two")
        val words = input.split(" ", "-")
        if (words.size == 2) {
            val tens = WORD_NUMBERS[words[0]]
            val ones = WORD_NUMBERS[words[1]]
            if (tens != null && ones != null && tens >= 20 && ones < 10) {
                return tens + ones
            }
        }

        return null
    }

    /**
     * Format a numeric value for display.
     */
    private fun formatValue(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Voice Phrases for Speech Engine Registration
    // ═══════════════════════════════════════════════════════════════════════════

    override fun getVoicePhrases(): List<String> {
        return listOf(
            "set to",
            "set quantity to",
            "set amount to",
            "increase", "plus", "add one",
            "decrease", "minus", "subtract one",
            "increase by", "add",
            "decrease by", "subtract",
            "minimum", "min",
            "maximum", "max",
            "reset"
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Stepper preset positions.
 */
private enum class StepperPreset {
    MINIMUM,
    MAXIMUM,
    RESET
}

/**
 * Information about a stepper component.
 *
 * @property avid AVID fingerprint for the stepper (format: STP:{hash8})
 * @property name Display name or associated label
 * @property currentValue Current stepper value
 * @property minValue Minimum allowed value
 * @property maxValue Maximum allowed value
 * @property stepValue Step increment value (e.g., 1 for integer steppers)
 * @property defaultValue Default/initial value for reset operation
 * @property bounds Screen bounds for the stepper
 * @property isFocused Whether this stepper currently has focus
 * @property node Platform-specific node reference
 */
data class StepperInfo(
    val avid: String,
    val name: String = "",
    val currentValue: Double = 0.0,
    val minValue: Double = 0.0,
    val maxValue: Double = 100.0,
    val stepValue: Double = 1.0,
    val defaultValue: Double = 0.0,
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "Stepper",
        text = name,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = formatValue(currentValue)
    )

    private fun formatValue(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}

/**
 * Result of a stepper operation.
 *
 * @property success Whether the operation completed successfully
 * @property error Error message if operation failed
 * @property previousValue The value before the operation
 * @property newValue The value after the operation
 */
data class StepperOperationResult(
    val success: Boolean,
    val error: String? = null,
    val previousValue: Double = 0.0,
    val newValue: Double = 0.0
) {
    companion object {
        /**
         * Create a successful operation result.
         */
        fun success(previousValue: Double, newValue: Double) = StepperOperationResult(
            success = true,
            previousValue = previousValue,
            newValue = newValue
        )

        /**
         * Create a failed operation result.
         */
        fun error(message: String) = StepperOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for stepper operations.
 *
 * Implementations should:
 * 1. Find stepper components by AVID, name, or focus state
 * 2. Read current stepper values and bounds
 * 3. Set stepper values via accessibility actions
 * 4. Handle NumberPicker, Spinner, and custom stepper components
 *
 * ## Stepper Detection Algorithm
 *
 * ```kotlin
 * fun findStepperNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className:
 *     // - android.widget.NumberPicker
 *     // - android.widget.Spinner (numeric mode)
 *     // - Custom stepper implementations with increment/decrement buttons
 *     // - Nodes with ACTION_SCROLL_FORWARD/BACKWARD and numeric content
 * }
 * ```
 *
 * ## Value Setting Algorithm
 *
 * ```kotlin
 * fun setValue(node: AccessibilityNodeInfo, value: Double): Boolean {
 *     // Option 1: Direct text input if editable
 *     // Option 2: Repeated increment/decrement to reach target
 *     // Option 3: Accessibility action with value argument
 * }
 * ```
 */
interface StepperExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Stepper Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a stepper by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: STP:{hash8})
     * @return StepperInfo if found, null otherwise
     */
    suspend fun findByAvid(avid: String): StepperInfo?

    /**
     * Find a stepper by its name or associated label.
     *
     * Searches for:
     * 1. Stepper with matching contentDescription
     * 2. Stepper with label text matching name
     * 3. Stepper with associated TextView label nearby
     *
     * @param name The name to search for (case-insensitive)
     * @return StepperInfo if found, null otherwise
     */
    suspend fun findByName(name: String): StepperInfo?

    /**
     * Find the currently focused stepper.
     *
     * @return StepperInfo if a stepper has focus, null otherwise
     */
    suspend fun findFocused(): StepperInfo?

    /**
     * Get all steppers on the current screen.
     *
     * @return List of all visible stepper components
     */
    suspend fun getAllSteppers(): List<StepperInfo>

    // ═══════════════════════════════════════════════════════════════════════════
    // Value Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Set the stepper to a specific value.
     *
     * The value will be clamped to the stepper's min/max range and
     * snapped to the nearest step if stepValue is defined.
     *
     * @param stepper The stepper to modify
     * @param value The target value (will be clamped and snapped)
     * @return Operation result with previous and new values
     */
    suspend fun setValue(stepper: StepperInfo, value: Double): StepperOperationResult

    /**
     * Increment the stepper by its step value.
     *
     * @param stepper The stepper to modify
     * @return Operation result with previous and new values
     */
    suspend fun increment(stepper: StepperInfo): StepperOperationResult

    /**
     * Decrement the stepper by its step value.
     *
     * @param stepper The stepper to modify
     * @return Operation result with previous and new values
     */
    suspend fun decrement(stepper: StepperInfo): StepperOperationResult

    /**
     * Increment the stepper by a specific amount.
     *
     * The result will be clamped to the stepper's max value.
     *
     * @param stepper The stepper to modify
     * @param amount The amount to increment by
     * @return Operation result with previous and new values
     */
    suspend fun incrementBy(stepper: StepperInfo, amount: Double): StepperOperationResult

    /**
     * Decrement the stepper by a specific amount.
     *
     * The result will be clamped to the stepper's min value.
     *
     * @param stepper The stepper to modify
     * @param amount The amount to decrement by
     * @return Operation result with previous and new values
     */
    suspend fun decrementBy(stepper: StepperInfo, amount: Double): StepperOperationResult

    /**
     * Set stepper to its minimum value.
     *
     * @param stepper The stepper to modify
     * @return Operation result with previous and new values
     */
    suspend fun setToMin(stepper: StepperInfo): StepperOperationResult

    /**
     * Set stepper to its maximum value.
     *
     * @param stepper The stepper to modify
     * @return Operation result with previous and new values
     */
    suspend fun setToMax(stepper: StepperInfo): StepperOperationResult

    /**
     * Reset stepper to its default value.
     *
     * @param stepper The stepper to modify
     * @return Operation result with previous and new values
     */
    suspend fun reset(stepper: StepperInfo): StepperOperationResult
}
