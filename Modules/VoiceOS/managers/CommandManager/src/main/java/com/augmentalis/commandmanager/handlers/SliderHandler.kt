/**
 * SliderHandler.kt - Voice handler for Slider/SeekBar interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-27
 *
 * Purpose: Voice-driven slider control with absolute and relative value adjustments
 * Features:
 * - Set slider to specific numeric value or percentage
 * - Increment/decrement by amount or default step
 * - Quick presets (minimum, maximum, halfway)
 * - Named slider targeting (e.g., "set volume to 50")
 * - Focused slider targeting (e.g., "set to 75%")
 * - AVID-based targeting for precise element selection
 * - Voice feedback for value changes
 *
 * Location: CommandManager module handlers
 *
 * ## Supported Commands
 *
 * Absolute value:
 * - "set [name] to [value]" - Set named slider to value
 * - "set to [value]" - Set focused slider to value
 * - "[name] [value]" - Shorthand (e.g., "volume 50", "brightness 80%")
 *
 * Relative adjustment:
 * - "increase by [amount]" - Increment by amount
 * - "decrease by [amount]" - Decrement by amount
 * - "increase [name] by [amount]" - Increment named slider
 * - "decrease [name] by [amount]" - Decrement named slider
 * - "increase" / "decrease" - Increment/decrement by step
 *
 * Presets:
 * - "minimum" / "min" - Set to slider minimum
 * - "maximum" / "max" - Set to slider maximum
 * - "halfway" / "middle" - Set to 50%
 *
 * ## Value Parsing
 *
 * Supports:
 * - Integer values: "50", "100"
 * - Decimal values: "7.5", "2.5"
 * - Percentages: "50%", "75 percent"
 * - Word numbers: "fifty", "twenty five"
 */

package com.augmentalis.commandmanager.handlers

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for Slider/SeekBar interactions.
 *
 * Provides comprehensive voice control for slider components including:
 * - Absolute value setting (numeric or percentage)
 * - Relative value adjustment (increment/decrement)
 * - Quick presets (min, max, halfway)
 * - Named slider targeting with disambiguation
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for slider operations
 */
class SliderHandler(
    private val executor: SliderExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "SliderHandler"

        // Default step size when increment/decrement without amount
        private const val DEFAULT_STEP_PERCENT = 10.0

        // Patterns for parsing commands
        private val SET_TO_PATTERN = Regex(
            """^set\s+(?:(.+?)\s+)?to\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val INCREASE_BY_PATTERN = Regex(
            """^increase\s+(?:(.+?)\s+)?by\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        private val DECREASE_BY_PATTERN = Regex(
            """^decrease\s+(?:(.+?)\s+)?by\s+(.+)$""",
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
        // Increase
        "increase", "increase by", "increase [name] by",
        "raise", "raise by", "turn up", "up",
        // Decrease
        "decrease", "decrease by", "decrease [name] by",
        "lower", "lower by", "turn down", "down",
        // Presets
        "minimum", "min", "maximum", "max", "halfway", "middle"
    )

    /**
     * Callback for voice feedback when slider value changes.
     */
    var onValueChanged: ((sliderName: String, newValue: Double, percentage: Int) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing slider command: $normalizedAction")

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

                // Simple increase commands
                normalizedAction in listOf("increase", "raise", "turn up", "up") -> {
                    handleSimpleIncrease(command)
                }

                // Simple decrease commands
                normalizedAction in listOf("decrease", "lower", "turn down", "down") -> {
                    handleSimpleDecrease(command)
                }

                // Presets: minimum
                normalizedAction in listOf("minimum", "min") -> {
                    handlePreset(SliderPreset.MINIMUM, command)
                }

                // Presets: maximum
                normalizedAction in listOf("maximum", "max") -> {
                    handlePreset(SliderPreset.MAXIMUM, command)
                }

                // Presets: halfway/middle
                normalizedAction in listOf("halfway", "middle", "half") -> {
                    handlePreset(SliderPreset.HALFWAY, command)
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing slider command", e)
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

        val sliderName = matchResult.groupValues[1].takeIf { it.isNotBlank() }
        val valueString = matchResult.groupValues[2]

        // Parse the value
        val parsedValue = parseValue(valueString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse value: '$valueString'",
                recoverable = true,
                suggestedAction = "Try 'set to 50' or 'set to 75 percent'"
            )

        // Find the slider
        val sliderInfo = findSlider(
            name = sliderName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (sliderName != null) "Slider '$sliderName' not found" else "No slider focused",
            recoverable = true,
            suggestedAction = "Focus on a slider or say 'set volume to 50'"
        )

        // Calculate the actual value
        val targetValue = if (parsedValue.isPercentage) {
            sliderInfo.minValue + (sliderInfo.maxValue - sliderInfo.minValue) * (parsedValue.value / 100.0)
        } else {
            parsedValue.value
        }

        // Clamp to slider bounds
        val clampedValue = targetValue.coerceIn(sliderInfo.minValue, sliderInfo.maxValue)

        // Apply the value
        return applyValue(sliderInfo, clampedValue)
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

        val sliderName = matchResult.groupValues[1].takeIf { it.isNotBlank() }
        val amountString = matchResult.groupValues[2]

        // Parse the amount
        val parsedAmount = parseValue(amountString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse amount: '$amountString'",
                recoverable = true,
                suggestedAction = "Try 'increase by 10' or 'increase by 5 percent'"
            )

        // Find the slider
        val sliderInfo = findSlider(
            name = sliderName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (sliderName != null) "Slider '$sliderName' not found" else "No slider focused",
            recoverable = true,
            suggestedAction = "Focus on a slider first"
        )

        // Calculate the delta
        val delta = if (parsedAmount.isPercentage) {
            (sliderInfo.maxValue - sliderInfo.minValue) * (parsedAmount.value / 100.0)
        } else {
            parsedAmount.value
        }

        // Calculate new value
        val newValue = (sliderInfo.currentValue + delta).coerceIn(sliderInfo.minValue, sliderInfo.maxValue)

        return applyValue(sliderInfo, newValue)
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

        val sliderName = matchResult.groupValues[1].takeIf { it.isNotBlank() }
        val amountString = matchResult.groupValues[2]

        // Parse the amount
        val parsedAmount = parseValue(amountString)
            ?: return HandlerResult.Failure(
                reason = "Could not parse amount: '$amountString'",
                recoverable = true,
                suggestedAction = "Try 'decrease by 10' or 'decrease by 5 percent'"
            )

        // Find the slider
        val sliderInfo = findSlider(
            name = sliderName,
            avid = command.targetAvid
        ) ?: return HandlerResult.Failure(
            reason = if (sliderName != null) "Slider '$sliderName' not found" else "No slider focused",
            recoverable = true,
            suggestedAction = "Focus on a slider first"
        )

        // Calculate the delta
        val delta = if (parsedAmount.isPercentage) {
            (sliderInfo.maxValue - sliderInfo.minValue) * (parsedAmount.value / 100.0)
        } else {
            parsedAmount.value
        }

        // Calculate new value
        val newValue = (sliderInfo.currentValue - delta).coerceIn(sliderInfo.minValue, sliderInfo.maxValue)

        return applyValue(sliderInfo, newValue)
    }

    /**
     * Handle simple "increase" command (increment by default step).
     */
    private suspend fun handleSimpleIncrease(command: QuantizedCommand): HandlerResult {
        val sliderInfo = findSlider(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No slider focused",
                recoverable = true,
                suggestedAction = "Focus on a slider first"
            )

        // Use slider's step or default 10%
        val step = sliderInfo.stepValue ?: run {
            (sliderInfo.maxValue - sliderInfo.minValue) * (DEFAULT_STEP_PERCENT / 100.0)
        }

        val newValue = (sliderInfo.currentValue + step).coerceIn(sliderInfo.minValue, sliderInfo.maxValue)

        return applyValue(sliderInfo, newValue)
    }

    /**
     * Handle simple "decrease" command (decrement by default step).
     */
    private suspend fun handleSimpleDecrease(command: QuantizedCommand): HandlerResult {
        val sliderInfo = findSlider(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No slider focused",
                recoverable = true,
                suggestedAction = "Focus on a slider first"
            )

        // Use slider's step or default 10%
        val step = sliderInfo.stepValue ?: run {
            (sliderInfo.maxValue - sliderInfo.minValue) * (DEFAULT_STEP_PERCENT / 100.0)
        }

        val newValue = (sliderInfo.currentValue - step).coerceIn(sliderInfo.minValue, sliderInfo.maxValue)

        return applyValue(sliderInfo, newValue)
    }

    /**
     * Handle preset commands (min, max, halfway).
     */
    private suspend fun handlePreset(
        preset: SliderPreset,
        command: QuantizedCommand
    ): HandlerResult {
        val sliderInfo = findSlider(avid = command.targetAvid)
            ?: return HandlerResult.Failure(
                reason = "No slider focused",
                recoverable = true,
                suggestedAction = "Focus on a slider first"
            )

        val targetValue = when (preset) {
            SliderPreset.MINIMUM -> sliderInfo.minValue
            SliderPreset.MAXIMUM -> sliderInfo.maxValue
            SliderPreset.HALFWAY -> sliderInfo.minValue + (sliderInfo.maxValue - sliderInfo.minValue) / 2.0
        }

        return applyValue(sliderInfo, targetValue)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find slider by name, AVID, or focus state.
     */
    private suspend fun findSlider(
        name: String? = null,
        avid: String? = null
    ): SliderInfo? {
        // Priority 1: AVID lookup (fastest, most precise)
        if (avid != null) {
            val slider = executor.findSliderByAvid(avid)
            if (slider != null) return slider
        }

        // Priority 2: Name lookup
        if (name != null) {
            val slider = executor.findSliderByName(name)
            if (slider != null) return slider
        }

        // Priority 3: Focused slider
        return executor.findFocusedSlider()
    }

    /**
     * Apply a value to a slider and return result.
     */
    private suspend fun applyValue(sliderInfo: SliderInfo, targetValue: Double): HandlerResult {
        // Calculate percentage for feedback
        val range = sliderInfo.maxValue - sliderInfo.minValue
        val percentage = if (range > 0) {
            ((targetValue - sliderInfo.minValue) / range * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        // Apply via executor
        val result = executor.setValue(sliderInfo, targetValue)

        return if (result.success) {
            // Invoke callback for voice feedback
            onValueChanged?.invoke(
                sliderInfo.name.ifBlank { "Slider" },
                targetValue,
                percentage
            )

            // Build feedback message
            val feedback = buildString {
                if (sliderInfo.name.isNotBlank()) {
                    append(sliderInfo.name)
                    append(" set to ")
                } else {
                    append("Set to ")
                }
                // Show percentage if it's a standard 0-100 or similar range
                if (sliderInfo.showPercentage) {
                    append("$percentage%")
                } else {
                    append(formatValue(targetValue))
                }
            }

            Log.i(TAG, "Slider value set: ${sliderInfo.name} = $targetValue ($percentage%)")

            HandlerResult.Success(
                message = feedback,
                data = mapOf(
                    "sliderName" to sliderInfo.name,
                    "sliderAvid" to sliderInfo.avid,
                    "previousValue" to sliderInfo.currentValue,
                    "newValue" to targetValue,
                    "percentage" to percentage,
                    "minValue" to sliderInfo.minValue,
                    "maxValue" to sliderInfo.maxValue,
                    "accessibility_announcement" to feedback
                )
            )
        } else {
            HandlerResult.failure(
                reason = result.error ?: "Could not set slider value",
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
            "set to",
            "set volume to",
            "set brightness to",
            "increase", "increase by",
            "decrease", "decrease by",
            "raise", "lower",
            "turn up", "turn down",
            "minimum", "min",
            "maximum", "max",
            "halfway", "middle"
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Slider preset positions.
 */
private enum class SliderPreset {
    MINIMUM,
    MAXIMUM,
    HALFWAY
}

/**
 * Parsed numeric value with percentage flag.
 */
private data class ParsedValue(
    val value: Double,
    val isPercentage: Boolean
)

/**
 * Information about a slider component.
 *
 * @property avid AVID fingerprint for the slider (format: SLD:{hash8})
 * @property name Display name or associated label
 * @property currentValue Current slider value
 * @property minValue Minimum allowed value
 * @property maxValue Maximum allowed value
 * @property stepValue Discrete step value (null for continuous)
 * @property showPercentage Whether to display value as percentage in feedback
 * @property bounds Screen bounds for the slider
 * @property isFocused Whether this slider currently has focus
 * @property node Platform-specific node reference
 */
data class SliderInfo(
    val avid: String,
    val name: String = "",
    val currentValue: Double = 0.0,
    val minValue: Double = 0.0,
    val maxValue: Double = 100.0,
    val stepValue: Double? = null,
    val showPercentage: Boolean = true,
    val bounds: Bounds = Bounds.EMPTY,
    val isFocused: Boolean = false,
    val node: Any? = null
) {
    /**
     * Convert to ElementInfo for general element operations.
     */
    fun toElementInfo(): ElementInfo = ElementInfo(
        className = "Slider",
        text = name,
        bounds = bounds,
        isClickable = true,
        isEnabled = true,
        avid = avid,
        stateDescription = "${currentValue.toInt()}%"
    )
}

/**
 * Result of a slider operation.
 */
data class SliderOperationResult(
    val success: Boolean,
    val error: String? = null,
    val previousValue: Double = 0.0,
    val newValue: Double = 0.0
) {
    companion object {
        fun success(previousValue: Double, newValue: Double) = SliderOperationResult(
            success = true,
            previousValue = previousValue,
            newValue = newValue
        )

        fun error(message: String) = SliderOperationResult(
            success = false,
            error = message
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for slider operations.
 *
 * Implementations should:
 * 1. Find slider components by AVID, name, or focus state
 * 2. Read current slider values and bounds
 * 3. Set slider values via accessibility actions
 * 4. Handle both SeekBar and Slider (Material) components
 *
 * ## Slider Detection Algorithm
 *
 * ```kotlin
 * fun findSliderNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
 *     // Look for nodes with className:
 *     // - android.widget.SeekBar
 *     // - android.widget.ProgressBar (if interactive)
 *     // - androidx.appcompat.widget.AppCompatSeekBar
 *     // - com.google.android.material.slider.Slider
 *     // - Custom slider implementations with RangeInfo
 * }
 * ```
 *
 * ## Value Setting Algorithm
 *
 * ```kotlin
 * fun setValue(node: AccessibilityNodeInfo, value: Double): Boolean {
 *     // API 24+: Use ACTION_SET_PROGRESS with ARGUMENT_PROGRESS_VALUE
 *     // Pre-24: Use percentage-based gesture simulation
 * }
 * ```
 */
interface SliderExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Slider Discovery
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a slider by its AVID fingerprint.
     *
     * @param avid The AVID fingerprint (format: SLD:{hash8})
     * @return SliderInfo if found, null otherwise
     */
    suspend fun findSliderByAvid(avid: String): SliderInfo?

    /**
     * Find a slider by its name or associated label.
     *
     * Searches for:
     * 1. Slider with matching contentDescription
     * 2. Slider with label text matching name
     * 3. Slider with associated TextView label nearby
     *
     * @param name The name to search for (case-insensitive)
     * @return SliderInfo if found, null otherwise
     */
    suspend fun findSliderByName(name: String): SliderInfo?

    /**
     * Find the currently focused slider.
     *
     * @return SliderInfo if a slider has focus, null otherwise
     */
    suspend fun findFocusedSlider(): SliderInfo?

    /**
     * Get all sliders on the current screen.
     *
     * @return List of all visible slider components
     */
    suspend fun getAllSliders(): List<SliderInfo>

    // ═══════════════════════════════════════════════════════════════════════════
    // Value Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Set the slider to a specific value.
     *
     * The value should be within the slider's min/max range.
     * If stepValue is defined, the value will be snapped to the nearest step.
     *
     * @param slider The slider to modify
     * @param value The target value (will be clamped to valid range)
     * @return Operation result with previous and new values
     */
    suspend fun setValue(slider: SliderInfo, value: Double): SliderOperationResult

    /**
     * Get the current value of a slider.
     *
     * @param slider The slider to query
     * @return Current value, or null if unable to read
     */
    suspend fun getValue(slider: SliderInfo): Double?

    /**
     * Get the range information for a slider.
     *
     * @param slider The slider to query
     * @return Pair of (minValue, maxValue), or null if unable to read
     */
    suspend fun getRange(slider: SliderInfo): Pair<Double, Double>?

    // ═══════════════════════════════════════════════════════════════════════════
    // Convenience Operations
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Increment the slider by its step value or a default amount.
     *
     * @param slider The slider to modify
     * @return Operation result
     */
    suspend fun increment(slider: SliderInfo): SliderOperationResult

    /**
     * Decrement the slider by its step value or a default amount.
     *
     * @param slider The slider to modify
     * @return Operation result
     */
    suspend fun decrement(slider: SliderInfo): SliderOperationResult

    /**
     * Set slider to its minimum value.
     *
     * @param slider The slider to modify
     * @return Operation result
     */
    suspend fun setToMinimum(slider: SliderInfo): SliderOperationResult

    /**
     * Set slider to its maximum value.
     *
     * @param slider The slider to modify
     * @return Operation result
     */
    suspend fun setToMaximum(slider: SliderInfo): SliderOperationResult
}
