/**
 * RangeSliderHandler.kt - Voice handler for Range Slider (dual thumb) interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * Purpose: Voice-driven range slider control with dual thumb (low/high) value adjustments
 * Features:
 * - Set complete range with min and max values
 * - Set individual low (minimum) or high (maximum) thumb values
 * - Increment/decrement low or high thumbs independently
 * - Quick presets (reset range, full range)
 * - Named range slider targeting (e.g., "set price range 50 to 200")
 * - Focused range slider targeting (e.g., "set range 10 to 90")
 * - AVID-based targeting for precise element selection
 * - Voice feedback for value changes
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Set complete range:
 * - "set range [min] to [max]" - Set both thumbs
 * - "set [name] range [min] to [max]" - Set named range slider
 *
 * Set low (minimum) thumb:
 * - "set minimum to [value]" - Set lower bound
 * - "set low to [value]" - Set lower bound (alias)
 * - "set [name] minimum to [value]" - Set named slider's lower bound
 *
 * Set high (maximum) thumb:
 * - "set maximum to [value]" - Set upper bound
 * - "set high to [value]" - Set upper bound (alias)
 * - "set [name] maximum to [value]" - Set named slider's upper bound
 *
 * Adjust low thumb:
 * - "increase minimum" / "increase low" - Increment lower bound
 * - "decrease minimum" / "decrease low" - Decrement lower bound
 * - "increase minimum by [amount]" - Increment by specific amount
 * - "decrease minimum by [amount]" - Decrement by specific amount
 *
 * Adjust high thumb:
 * - "increase maximum" / "increase high" - Increment upper bound
 * - "decrease maximum" / "decrease high" - Decrement upper bound
 * - "increase maximum by [amount]" - Increment by specific amount
 * - "decrease maximum by [amount]" - Decrement by specific amount
 *
 * Presets:
 * - "reset range" - Reset to default range values
 * - "full range" - Set to minimum-maximum extent
 *
 * ## Value Parsing
 *
 * Supports:
 * - Integer values: "50", "100"
 * - Decimal values: "7.5", "2.5"
 * - Percentages: "50%", "75 percent"
 * - Word numbers: "fifty", "twenty five"
 */

package com.augmentalis.avamagic.voice.handlers.input

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Range Slider (dual thumb) interactions.
 *
 * Provides comprehensive voice control for range slider components including:
 * - Complete range setting (both low and high values)
 * - Individual thumb value setting (low or high)
 * - Relative value adjustment (increment/decrement per thumb)
 * - Quick presets (reset, full range)
 * - Named range slider targeting with disambiguation
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for range slider operations
 */
class RangeSliderHandler(
    private val executor: RangeSliderExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "RangeSliderHandler"

        // Default step size when increment/decrement without amount
        private const val DEFAULT_STEP_PERCENT = 10.0

        // Patterns for parsing commands

        // "set range [min] to [max]" or "set [name] range [min] to [max]"
        private val SET_RANGE_PATTERN = Regex(
            """^set\s+(?:(.+?)\s+)?range\s+(.+?)\s+to\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // "set minimum to [value]" or "set low to [value]" or "set [name] minimum to [value]"
        private val SET_MINIMUM_PATTERN = Regex(
            """^set\s+(?:(.+?)\s+)?(?:minimum|low)\s+to\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // "set maximum to [value]" or "set high to [value]" or "set [name] maximum to [value]"
        private val SET_MAXIMUM_PATTERN = Regex(
            """^set\s+(?:(.+?)\s+)?(?:maximum|high)\s+to\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // "increase minimum by [amount]" or "increase low by [amount]"
        private val INCREASE_MINIMUM_BY_PATTERN = Regex(
            """^increase\s+(?:minimum|low)\s+by\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // "decrease minimum by [amount]" or "decrease low by [amount]"
        private val DECREASE_MINIMUM_BY_PATTERN = Regex(
            """^decrease\s+(?:minimum|low)\s+by\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // "increase maximum by [amount]" or "increase high by [amount]"
        private val INCREASE_MAXIMUM_BY_PATTERN = Regex(
            """^increase\s+(?:maximum|high)\s+by\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // "decrease maximum by [amount]" or "decrease high by [amount]"
        private val DECREASE_MAXIMUM_BY_PATTERN = Regex(
            """^decrease\s+(?:maximum|high)\s+by\s+(.+)$""",
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
        // Set range
        "set range", "set [name] range",
        // Set minimum/low
        "set minimum to", "set low to",
        "set [name] minimum to", "set [name] low to",
        // Set maximum/high
        "set maximum to", "set high to",
        "set [name] maximum to", "set [name] high to",
        // Increase minimum/low
        "increase minimum", "increase low",
        "increase minimum by", "increase low by",
        // Decrease minimum/low
        "decrease minimum", "decrease low",
        "decrease minimum by", "decrease low by",
        // Increase maximum/high
        "increase maximum", "increase high",
        "increase maximum by", "increase high by",
        // Decrease maximum/high
        "decrease maximum", "decrease high",
        "decrease maximum by", "decrease high by",
        // Presets
        "reset range", "full range"
    )

    /**
     * Callback for voice feedback when range slider values change.
     */
    var onRangeChanged: ((sliderName: String, lowValue: Double, highValue: Double) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing range slider command: $normalizedAction")

        return try {
            when {
                // Set complete range: "set range [min] to [max]"
                SET_RANGE_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSetRange(normalizedAction, command)
                }

                // Set minimum/low value: "set minimum to [value]" or "set low to [value]"
                SET_MINIMUM_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSetMinimum(normalizedAction, command)
                }

                // Set maximum/high value: "set maximum to [value]" or "set high to [value]"
                SET_MAXIMUM_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleSetMaximum(normalizedAction, command)
                }

                // Increase minimum by amount
                INCREASE_MINIMUM_BY_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleIncreaseMinimumBy(normalizedAction, command)
                }

                // Decrease minimum by amount
                DECREASE_MINIMUM_BY_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleDecreaseMinimumBy(normalizedAction, command)
                }

                // Increase maximum by amount
                INCREASE_MAXIMUM_BY_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleIncreaseMaximumBy(normalizedAction, command)
                }

                // Decrease maximum by amount
                DECREASE_MAXIMUM_BY_PATTERN.containsMatchIn(normalizedAction) -> {
                    handleDecreaseMaximumBy(normalizedAction, command)
                }

                // Simple increase minimum/low
                normalizedAction in listOf("increase minimum", "increase low") -> {
                    handleSimpleIncreaseMinimum(command)
                }

                // Simple decrease minimum/low
                normalizedAction in listOf("decrease minimum", "decrease low") -> {
                    handleSimpleDecreaseMinimum(command)
                }

                // Simple increase maximum/high
                normalizedAction in listOf("increase maximum", "increase high") -> {
                    handleSimpleIncreaseMaximum(command)
                }

                // Simple decrease maximum/high
                normalizedAction in listOf("decrease maximum", "decrease high") -> {
                    handleSimpleDecreaseMaximum(command)
                }

                // Preset: reset range
                normalizedAction == "reset range" -> {
                    handleResetRange(command)
                }

                // Preset: full range
                normalizedAction == "full range" -> {
                    handleFullRange(command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing range slider command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "set range [min] to [max]" command.
     */
    private suspend fun handleSetRange(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SET_RANGE_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse set range command")

        val sliderName = matchResult.groupValues[1].takeIf { it.isNotBlank() }
        val minValueString = matchResult.groupValues[2]
        val maxValueString = matchResult.groupValues[3]

        // Parse the values
        val parsedMin = parseValue(minValueString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse minimum value: '$minValueString'",
                recoverable = true,
                suggestedAction = "Try 'set range 10 to 90' or 'set range 20% to 80%'"
            )

        val parsedMax = parseValue(maxValueString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse maximum value: '$maxValueString'",
                recoverable = true,
                suggestedAction = "Try 'set range 10 to 90' or 'set range 20% to 80%'"
            )

        // Find the range slider
        val sliderInfo = findRangeSlider(
            name = sliderName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (sliderName != null) "Range slider '$sliderName' not found" else "No range slider focused",
            recoverable = true,
            suggestedAction = "Focus on a range slider or say 'set price range 50 to 200'"
        )

        // Calculate the actual values
        val targetLow = if (parsedMin.isPercentage) {
            sliderInfo.minValue + (sliderInfo.maxValue - sliderInfo.minValue) * (parsedMin.value / 100.0)
        } else {
            parsedMin.value
        }

        val targetHigh = if (parsedMax.isPercentage) {
            sliderInfo.minValue + (sliderInfo.maxValue - sliderInfo.minValue) * (parsedMax.value / 100.0)
        } else {
            parsedMax.value
        }

        // Validate that low <= high
        if (targetLow > targetHigh) {
            return HandlerResult.Failure(
                reason = "Minimum value ($targetLow) cannot be greater than maximum value ($targetHigh)",
                recoverable = true,
                suggestedAction = "Try swapping the values: 'set range ${formatValue(targetHigh)} to ${formatValue(targetLow)}'"
            )
        }

        // Clamp to slider bounds
        val clampedLow = targetLow.coerceIn(sliderInfo.minValue, sliderInfo.maxValue)
        val clampedHigh = targetHigh.coerceIn(sliderInfo.minValue, sliderInfo.maxValue)

        // Apply the range
        return applyRange(sliderInfo, clampedLow, clampedHigh)
    }

    /**
     * Handle "set minimum to [value]" or "set low to [value]" command.
     */
    private suspend fun handleSetMinimum(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SET_MINIMUM_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse set minimum command")

        val sliderName = matchResult.groupValues[1].takeIf { it.isNotBlank() }
        val valueString = matchResult.groupValues[2]

        // Parse the value
        val parsedValue = parseValue(valueString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse value: '$valueString'",
                recoverable = true,
                suggestedAction = "Try 'set minimum to 20' or 'set low to 25%'"
            )

        // Find the range slider
        val sliderInfo = findRangeSlider(
            name = sliderName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (sliderName != null) "Range slider '$sliderName' not found" else "No range slider focused",
            recoverable = true,
            suggestedAction = "Focus on a range slider or say 'set price minimum to 50'"
        )

        // Calculate the actual value
        val targetValue = if (parsedValue.isPercentage) {
            sliderInfo.minValue + (sliderInfo.maxValue - sliderInfo.minValue) * (parsedValue.value / 100.0)
        } else {
            parsedValue.value
        }

        // Clamp to valid range (cannot exceed current high value)
        val clampedValue = targetValue.coerceIn(sliderInfo.minValue, sliderInfo.highValue)

        // Apply the low value
        return applyLowValue(sliderInfo, clampedValue)
    }

    /**
     * Handle "set maximum to [value]" or "set high to [value]" command.
     */
    private suspend fun handleSetMaximum(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = SET_MAXIMUM_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse set maximum command")

        val sliderName = matchResult.groupValues[1].takeIf { it.isNotBlank() }
        val valueString = matchResult.groupValues[2]

        // Parse the value
        val parsedValue = parseValue(valueString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse value: '$valueString'",
                recoverable = true,
                suggestedAction = "Try 'set maximum to 80' or 'set high to 90%'"
            )

        // Find the range slider
        val sliderInfo = findRangeSlider(
            name = sliderName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (sliderName != null) "Range slider '$sliderName' not found" else "No range slider focused",
            recoverable = true,
            suggestedAction = "Focus on a range slider or say 'set price maximum to 200'"
        )

        // Calculate the actual value
        val targetValue = if (parsedValue.isPercentage) {
            sliderInfo.minValue + (sliderInfo.maxValue - sliderInfo.minValue) * (parsedValue.value / 100.0)
        } else {
            parsedValue.value
        }

        // Clamp to valid range (cannot be below current low value)
        val clampedValue = targetValue.coerceIn(sliderInfo.lowValue, sliderInfo.maxValue)

        // Apply the high value
        return applyHighValue(sliderInfo, clampedValue)
    }

    /**
     * Handle "increase minimum by [amount]" command.
     */
    private suspend fun handleIncreaseMinimumBy(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = INCREASE_MINIMUM_BY_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse increase minimum command")

        val amountString = matchResult.groupValues[1]

        // Parse the amount
        val parsedAmount = parseValue(amountString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse amount: '$amountString'",
                recoverable = true,
                suggestedAction = "Try 'increase minimum by 10' or 'increase low by 5%'"
            )

        // Find the range slider
        val sliderInfo = findRangeSlider(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No range slider focused",
                recoverable = true,
                suggestedAction = "Focus on a range slider first"
            )

        // Calculate the delta
        val delta = if (parsedAmount.isPercentage) {
            (sliderInfo.maxValue - sliderInfo.minValue) * (parsedAmount.value / 100.0)
        } else {
            parsedAmount.value
        }

        // Calculate new low value (cannot exceed current high value)
        val newLow = (sliderInfo.lowValue + delta).coerceIn(sliderInfo.minValue, sliderInfo.highValue)

        return applyLowValue(sliderInfo, newLow)
    }

    /**
     * Handle "decrease minimum by [amount]" command.
     */
    private suspend fun handleDecreaseMinimumBy(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = DECREASE_MINIMUM_BY_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse decrease minimum command")

        val amountString = matchResult.groupValues[1]

        // Parse the amount
        val parsedAmount = parseValue(amountString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse amount: '$amountString'",
                recoverable = true,
                suggestedAction = "Try 'decrease minimum by 10' or 'decrease low by 5%'"
            )

        // Find the range slider
        val sliderInfo = findRangeSlider(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No range slider focused",
                recoverable = true,
                suggestedAction = "Focus on a range slider first"
            )

        // Calculate the delta
        val delta = if (parsedAmount.isPercentage) {
            (sliderInfo.maxValue - sliderInfo.minValue) * (parsedAmount.value / 100.0)
        } else {
            parsedAmount.value
        }

        // Calculate new low value
        val newLow = (sliderInfo.lowValue - delta).coerceIn(sliderInfo.minValue, sliderInfo.highValue)

        return applyLowValue(sliderInfo, newLow)
    }

    /**
     * Handle "increase maximum by [amount]" command.
     */
    private suspend fun handleIncreaseMaximumBy(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = INCREASE_MAXIMUM_BY_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse increase maximum command")

        val amountString = matchResult.groupValues[1]

        // Parse the amount
        val parsedAmount = parseValue(amountString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse amount: '$amountString'",
                recoverable = true,
                suggestedAction = "Try 'increase maximum by 10' or 'increase high by 5%'"
            )

        // Find the range slider
        val sliderInfo = findRangeSlider(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No range slider focused",
                recoverable = true,
                suggestedAction = "Focus on a range slider first"
            )

        // Calculate the delta
        val delta = if (parsedAmount.isPercentage) {
            (sliderInfo.maxValue - sliderInfo.minValue) * (parsedAmount.value / 100.0)
        } else {
            parsedAmount.value
        }

        // Calculate new high value
        val newHigh = (sliderInfo.highValue + delta).coerceIn(sliderInfo.lowValue, sliderInfo.maxValue)

        return applyHighValue(sliderInfo, newHigh)
    }

    /**
     * Handle "decrease maximum by [amount]" command.
     */
    private suspend fun handleDecreaseMaximumBy(
        normalizedAction: String,
        command: QuantizedCommand
    ): HandlerResult {
        val matchResult = DECREASE_MAXIMUM_BY_PATTERN.find(normalizedAction)
            ?: return HandlerResult.failure("Could not parse decrease maximum command")

        val amountString = matchResult.groupValues[1]

        // Parse the amount
        val parsedAmount = parseValue(amountString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse amount: '$amountString'",
                recoverable = true,
                suggestedAction = "Try 'decrease maximum by 10' or 'decrease high by 5%'"
            )

        // Find the range slider
        val sliderInfo = findRangeSlider(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No range slider focused",
                recoverable = true,
                suggestedAction = "Focus on a range slider first"
            )

        // Calculate the delta
        val delta = if (parsedAmount.isPercentage) {
            (sliderInfo.maxValue - sliderInfo.minValue) * (parsedAmount.value / 100.0)
        } else {
            parsedAmount.value
        }

        // Calculate new high value (cannot go below current low value)
        val newHigh = (sliderInfo.highValue - delta).coerceIn(sliderInfo.lowValue, sliderInfo.maxValue)

        return applyHighValue(sliderInfo, newHigh)
    }

    /**
     * Handle simple "increase minimum" or "increase low" command (increment by default step).
     */
    private suspend fun handleSimpleIncreaseMinimum(command: QuantizedCommand): HandlerResult {
        val sliderInfo = findRangeSlider(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No range slider focused",
                recoverable = true,
                suggestedAction = "Focus on a range slider first"
            )

        // Use slider's step or default 10%
        val step = sliderInfo.stepValue ?: run {
            (sliderInfo.maxValue - sliderInfo.minValue) * (DEFAULT_STEP_PERCENT / 100.0)
        }

        val newLow = (sliderInfo.lowValue + step).coerceIn(sliderInfo.minValue, sliderInfo.highValue)

        return applyLowValue(sliderInfo, newLow)
    }

    /**
     * Handle simple "decrease minimum" or "decrease low" command (decrement by default step).
     */
    private suspend fun handleSimpleDecreaseMinimum(command: QuantizedCommand): HandlerResult {
        val sliderInfo = findRangeSlider(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No range slider focused",
                recoverable = true,
                suggestedAction = "Focus on a range slider first"
            )

        // Use slider's step or default 10%
        val step = sliderInfo.stepValue ?: run {
            (sliderInfo.maxValue - sliderInfo.minValue) * (DEFAULT_STEP_PERCENT / 100.0)
        }

        val newLow = (sliderInfo.lowValue - step).coerceIn(sliderInfo.minValue, sliderInfo.highValue)

        return applyLowValue(sliderInfo, newLow)
    }

    /**
     * Handle simple "increase maximum" or "increase high" command (increment by default step).
     */
    private suspend fun handleSimpleIncreaseMaximum(command: QuantizedCommand): HandlerResult {
        val sliderInfo = findRangeSlider(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No range slider focused",
                recoverable = true,
                suggestedAction = "Focus on a range slider first"
            )

        // Use slider's step or default 10%
        val step = sliderInfo.stepValue ?: run {
            (sliderInfo.maxValue - sliderInfo.minValue) * (DEFAULT_STEP_PERCENT / 100.0)
        }

        val newHigh = (sliderInfo.highValue + step).coerceIn(sliderInfo.lowValue, sliderInfo.maxValue)

        return applyHighValue(sliderInfo, newHigh)
    }

    /**
     * Handle simple "decrease maximum" or "decrease high" command (decrement by default step).
     */
    private suspend fun handleSimpleDecreaseMaximum(command: QuantizedCommand): HandlerResult {
        val sliderInfo = findRangeSlider(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No range slider focused",
                recoverable = true,
                suggestedAction = "Focus on a range slider first"
            )

        // Use slider's step or default 10%
        val step = sliderInfo.stepValue ?: run {
            (sliderInfo.maxValue - sliderInfo.minValue) * (DEFAULT_STEP_PERCENT / 100.0)
        }

        val newHigh = (sliderInfo.highValue - step).coerceIn(sliderInfo.lowValue, sliderInfo.maxValue)

        return applyHighValue(sliderInfo, newHigh)
    }

    /**
     * Handle "reset range" command - reset to default values.
     */
    private suspend fun handleResetRange(command: QuantizedCommand): HandlerResult {
        val sliderInfo = findRangeSlider(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No range slider focused",
                recoverable = true,
                suggestedAction = "Focus on a range slider first"
            )

        // Apply via executor
        val result = executor.resetRange(sliderInfo)

        return if (result.success) {
            onRangeChanged?.invoke(
                sliderInfo.name.ifBlank { "Range slider" },
                result.newLowValue,
                result.newHighValue
            )

            val feedback = buildString {
                if (sliderInfo.name.isNotBlank()) {
                    append(sliderInfo.name)
                    append(" reset to ")
                } else {
                    append("Range reset to ")
                }
                append("${formatValue(result.newLowValue)} - ${formatValue(result.newHighValue)}")
            }

            Log.i(TAG, "Range slider reset: ${sliderInfo.name}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "sliderName" to sliderInfo.name,
                    "sliderAvid" to sliderInfo.avid,
                    "previousLowValue" to sliderInfo.lowValue,
                    "previousHighValue" to sliderInfo.highValue,
                    "newLowValue" to result.newLowValue,
                    "newHighValue" to result.newHighValue,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not reset range",
                recoverable = true
            )
        }
    }

    /**
     * Handle "full range" command - set to minimum and maximum extent.
     */
    private suspend fun handleFullRange(command: QuantizedCommand): HandlerResult {
        val sliderInfo = findRangeSlider(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No range slider focused",
                recoverable = true,
                suggestedAction = "Focus on a range slider first"
            )

        // Apply via executor
        val result = executor.setFullRange(sliderInfo)

        return if (result.success) {
            onRangeChanged?.invoke(
                sliderInfo.name.ifBlank { "Range slider" },
                result.newLowValue,
                result.newHighValue
            )

            val feedback = buildString {
                if (sliderInfo.name.isNotBlank()) {
                    append(sliderInfo.name)
                    append(" set to full range: ")
                } else {
                    append("Set to full range: ")
                }
                append("${formatValue(result.newLowValue)} - ${formatValue(result.newHighValue)}")
            }

            Log.i(TAG, "Range slider set to full range: ${sliderInfo.name}")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "sliderName" to sliderInfo.name,
                    "sliderAvid" to sliderInfo.avid,
                    "previousLowValue" to sliderInfo.lowValue,
                    "previousHighValue" to sliderInfo.highValue,
                    "newLowValue" to result.newLowValue,
                    "newHighValue" to result.newHighValue,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set full range",
                recoverable = true
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find range slider by name, AVID, or focus state.
     */
    private suspend fun findRangeSlider(
        name: String? = null,
        avid: String? = null
    ): RangeSliderInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val slider = executor.findByAvid(avid)
            if (slider != null) return slider
        }

        // Priority 2: Name lookup
        if (name != null) {
            val slider = executor.findByName(name)
            if (slider != null) return slider
        }

        // Priority 3: Focused range slider
        return executor.findFocused()
    }

    /**
     * Apply both low and high values to a range slider and return result.
     */
    private suspend fun applyRange(
        sliderInfo: RangeSliderInfo,
        lowValue: Double,
        highValue: Double
    ): HandlerResult {
        // Apply via executor
        val result = executor.setRange(sliderInfo, lowValue, highValue)

        return if (result.success) {
            // Invoke callback for voice feedback
            onRangeChanged?.invoke(
                sliderInfo.name.ifBlank { "Range slider" },
                lowValue,
                highValue
            )

            // Build feedback message
            val feedback = buildString {
                if (sliderInfo.name.isNotBlank()) {
                    append(sliderInfo.name)
                    append(" set to ")
                } else {
                    append("Range set to ")
                }
                append("${formatValue(lowValue)} - ${formatValue(highValue)}")
            }

            Log.i(TAG, "Range slider values set: ${sliderInfo.name} = $lowValue - $highValue")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "sliderName" to sliderInfo.name,
                    "sliderAvid" to sliderInfo.avid,
                    "previousLowValue" to sliderInfo.lowValue,
                    "previousHighValue" to sliderInfo.highValue,
                    "newLowValue" to lowValue,
                    "newHighValue" to highValue,
                    "minValue" to sliderInfo.minValue,
                    "maxValue" to sliderInfo.maxValue,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set range values",
                recoverable = true
            )
        }
    }

    /**
     * Apply a low value to a range slider and return result.
     */
    private suspend fun applyLowValue(sliderInfo: RangeSliderInfo, lowValue: Double): HandlerResult {
        // Apply via executor
        val result = executor.setLow(sliderInfo, lowValue)

        return if (result.success) {
            // Invoke callback for voice feedback
            onRangeChanged?.invoke(
                sliderInfo.name.ifBlank { "Range slider" },
                lowValue,
                sliderInfo.highValue
            )

            // Build feedback message
            val feedback = buildString {
                if (sliderInfo.name.isNotBlank()) {
                    append(sliderInfo.name)
                    append(" minimum set to ")
                } else {
                    append("Minimum set to ")
                }
                append(formatValue(lowValue))
            }

            Log.i(TAG, "Range slider low value set: ${sliderInfo.name} low = $lowValue")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "sliderName" to sliderInfo.name,
                    "sliderAvid" to sliderInfo.avid,
                    "previousLowValue" to sliderInfo.lowValue,
                    "newLowValue" to lowValue,
                    "highValue" to sliderInfo.highValue,
                    "minValue" to sliderInfo.minValue,
                    "maxValue" to sliderInfo.maxValue,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set minimum value",
                recoverable = true
            )
        }
    }

    /**
     * Apply a high value to a range slider and return result.
     */
    private suspend fun applyHighValue(sliderInfo: RangeSliderInfo, highValue: Double): HandlerResult {
        // Apply via executor
        val result = executor.setHigh(sliderInfo, highValue)

        return if (result.success) {
            // Invoke callback for voice feedback
            onRangeChanged?.invoke(
                sliderInfo.name.ifBlank { "Range slider" },
                sliderInfo.lowValue,
                highValue
            )

            // Build feedback message
            val feedback = buildString {
                if (sliderInfo.name.isNotBlank()) {
                    append(sliderInfo.name)
                    append(" maximum set to ")
                } else {
                    append("Maximum set to ")
                }
                append(formatValue(highValue))
            }

            Log.i(TAG, "Range slider high value set: ${sliderInfo.name} high = $highValue")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "sliderName" to sliderInfo.name,
                    "sliderAvid" to sliderInfo.avid,
                    "lowValue" to sliderInfo.lowValue,
                    "previousHighValue" to sliderInfo.highValue,
                    "newHighValue" to highValue,
                    "minValue" to sliderInfo.minValue,
                    "maxValue" to sliderInfo.maxValue,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set maximum value",
                recoverable = true
            )
        }
    }

    /**
     * Parse a value string into a numeric value.
     *
     * Supports:
     * - "50" -> 50.0 (absolute)
     * - "50%" -> 50.0 (percentage)
     * - "50 percent" -> 50.0 (percentage)
     * - "fifty" -> 50.0 (absolute)
     * - "fifty percent" -> 50.0 (percentage)
     * - "7.5" -> 7.5 (absolute)
     * - "twenty five" -> 25.0 (absolute)
     */
    private fun parseValue(input: String): ParsedValue? {
        val trimmed = input.trim().lowercase()

        // Check for percentage suffix
        val isPercentage = trimmed.endsWith("%") ||
                           trimmed.endsWith("percent") ||
                           trimmed.endsWith(" percent")

        // Remove percentage suffix
        val valueStr = trimmed
            .removeSuffix("%")
            .removeSuffix("percent")
            .removeSuffix(" percent")
            .trim()

        // Try direct numeric parsing
        valueStr.toDoubleOrNull()?.let {
            return ParsedValue(it, isPercentage)
        }

        // Try word number parsing
        parseWordNumber(valueStr)?.let {
            return ParsedValue(it.toDouble(), isPercentage)
        }

        return null
    }

    /**
     * Parse word numbers like "fifty", "twenty five", etc.
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
            "set range",
            "set price range",
            "set minimum to",
            "set low to",
            "set maximum to",
            "set high to",
            "increase minimum",
            "increase low",
            "decrease minimum",
            "decrease low",
            "increase maximum",
            "increase high",
            "decrease maximum",
            "decrease high",
            "reset range",
            "full range"
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Parsed numeric value with percentage flag.
 */
private data class ParsedValue(
    val value: Double,
    val isPercentage: Boolean
)

/**
 * Information about a range slider component.
 *
 * @property avid AVID fingerprint for the range slider (format: RSL:{hash8})
 * @property name Display name or associated label
 * @property lowValue Current low (minimum) thumb value
 * @property highValue Current high (maximum) thumb value
 * @property minValue Slider's absolute minimum allowed value
 * @property maxValue Slider's absolute maximum allowed value
 * @property stepValue Discrete step value (null for continuous)
 * @property bounds Screen bounds for the slider
 * @property isFocused Whether this range slider currently has focus
 * @property node Platform-specific node reference
 */
data class RangeSliderInfo(
    val avid: String,
    val name: String = "",
    val lowValue: Double = 0.0,
    val highValue: Double = 100.0,
    val minValue: Double = 0.0,
    val maxValue: Double = 100.0,
    val stepValue: Double? = null,
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "RangeSlider",
        text = name,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = "${lowValue.toInt()} - ${highValue.toInt()}"
    )
}

/**
 * Result of a range slider operation.
 *
 * @property success Whether the operation succeeded
 * @property error Error message if operation failed
 * @property previousLowValue Previous low thumb value before operation
 * @property previousHighValue Previous high thumb value before operation
 * @property newLowValue New low thumb value after operation
 * @property newHighValue New high thumb value after operation
 */
data class RangeSliderOperationResult(
    val success: Boolean,
    val error: String? = null,
    val previousLowValue: Double = 0.0,
    val previousHighValue: Double = 0.0,
    val newLowValue: Double = 0.0,
    val newHighValue: Double = 0.0
) {
    companion object {
        /**
         * Create a success result with previous and new values.
         */
        fun success(
            previousLowValue: Double,
            previousHighValue: Double,
            newLowValue: Double,
            newHighValue: Double
        ) = RangeSliderOperationResult(
            success = true,
            previousLowValue = previousLowValue,
            previousHighValue = previousHighValue,
            newLowValue = newLowValue,
            newHighValue = newHighValue
        )

        /**
         * Create an error result with message.
         */
        fun error(message: String) = RangeSliderOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for range slider operations.
 *
 * Implementations should:
 * 1. Find range slider components by AVID, name, or focus state
 * 2. Read current range slider values (low and high thumbs)
 * 3. Set range slider values via accessibility actions
 * 4. Handle Material RangeSlider components
 *
 * ## Range Slider Detection Algorithm
 *
 * ```kotlin
 * fun findRangeSliderNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className:
 *     // - com.google.android.material.slider.RangeSlider
 *     // - Custom range slider implementations with dual RangeInfo
 *     // - Nodes with two child thumb elements
 * }
 * ```
 *
 * ## Value Setting Algorithm
 *
 * ```kotlin
 * fun setRange(node: AccessibilityNodeInfo, low: Double, high: Double): Boolean {
 *     // API 24+: Use ACTION_SET_PROGRESS for each thumb
 *     // Alternative: Use accessibility gesture simulation
 *     // Ensure low <= high constraint is maintained
 * }
 * ```
 */
interface RangeSliderExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Range Slider Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a range slider by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: RSL:{hash8})
     * @return RangeSliderInfo if found, null otherwise
     */
    suspend fun findByAvid(avid: String): RangeSliderInfo?

    /**
     * Find a range slider by its name or associated label.
     *
     * Searches for:
     * 1. Range slider with matching contentDescription
     * 2. Range slider with label text matching name
     * 3. Range slider with associated TextView label nearby
     *
     * @param name The name to search for (case-insensitive)
     * @return RangeSliderInfo if found, null otherwise
     */
    suspend fun findByName(name: String): RangeSliderInfo?

    /**
     * Find the currently focused range slider.
     *
     * @return RangeSliderInfo if a range slider has focus, null otherwise
     */
    suspend fun findFocused(): RangeSliderInfo?

    // ═══════════════════════════════════════════════════════════════════════════
    // Value Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Set both low and high values on a range slider.
     *
     * The values should be within the slider's min/max range.
     * Low value must be less than or equal to high value.
     * If stepValue is defined, values will be snapped to the nearest step.
     *
     * @param slider The range slider to modify
     * @param lowValue The target low (minimum) value
     * @param highValue The target high (maximum) value
     * @return Operation result with previous and new values
     */
    suspend fun setRange(slider: RangeSliderInfo, lowValue: Double, highValue: Double): RangeSliderOperationResult

    /**
     * Set only the low (minimum) thumb value.
     *
     * The value will be clamped to not exceed the current high value.
     *
     * @param slider The range slider to modify
     * @param value The target low value
     * @return Operation result with previous and new values
     */
    suspend fun setLow(slider: RangeSliderInfo, value: Double): RangeSliderOperationResult

    /**
     * Set only the high (maximum) thumb value.
     *
     * The value will be clamped to not go below the current low value.
     *
     * @param slider The range slider to modify
     * @param value The target high value
     * @return Operation result with previous and new values
     */
    suspend fun setHigh(slider: RangeSliderInfo, value: Double): RangeSliderOperationResult

    /**
     * Increment the low thumb by its step value or a default amount.
     *
     * @param slider The range slider to modify
     * @return Operation result
     */
    suspend fun incrementLow(slider: RangeSliderInfo): RangeSliderOperationResult

    /**
     * Decrement the low thumb by its step value or a default amount.
     *
     * @param slider The range slider to modify
     * @return Operation result
     */
    suspend fun decrementLow(slider: RangeSliderInfo): RangeSliderOperationResult

    /**
     * Increment the high thumb by its step value or a default amount.
     *
     * @param slider The range slider to modify
     * @return Operation result
     */
    suspend fun incrementHigh(slider: RangeSliderInfo): RangeSliderOperationResult

    /**
     * Decrement the high thumb by its step value or a default amount.
     *
     * @param slider The range slider to modify
     * @return Operation result
     */
    suspend fun decrementHigh(slider: RangeSliderInfo): RangeSliderOperationResult

    /**
     * Reset the range slider to its default values.
     *
     * @param slider The range slider to reset
     * @return Operation result with the new (default) values
     */
    suspend fun resetRange(slider: RangeSliderInfo): RangeSliderOperationResult

    /**
     * Set the range slider to its full extent (min to max).
     *
     * @param slider The range slider to modify
     * @return Operation result
     */
    suspend fun setFullRange(slider: RangeSliderInfo): RangeSliderOperationResult
}
