/**
 * TimePickerHandler.kt
 *
 * Created: 2026-01-27 00:00 PST
 * Last Modified: 2026-01-28 00:00 PST
 * Author: VOS4 Development Team
 * Version: 2.0.0
 *
 * Purpose: Voice command handler for TimePicker widget interactions
 * Features: Time setting, hour/minute adjustment, AM/PM toggle, preset times
 * Location: MagicVoiceHandlers module
 *
 * Supported Commands:
 * - "set time to [time]" - e.g., "set time to 3:30 PM"
 * - "set hour to [N]" - set specific hour (1-12)
 * - "set minute to [N]" - set specific minute (0-59)
 * - "AM" / "PM" - switch AM/PM period
 * - "now" / "current time" - set to current time
 * - "noon" / "midnight" - preset times
 * - "increase hour" / "decrease hour" - increment/decrement hour
 * - "increase minute" / "decrease minute" - increment/decrement minute
 *
 * Changelog:
 * - v2.0.0 (2026-01-28): Migrated to BaseHandler pattern with executor
 * - v1.0.0 (2026-01-27): Initial implementation with full time picker support
 */

package com.augmentalis.avamagic.voice.handlers.input

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/**
 * Voice command handler for TimePicker widget interactions.
 *
 * Routes commands to set time, adjust hour/minute, toggle AM/PM,
 * or set preset times via executor pattern.
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for time picker operations
 */
class TimePickerHandler(
    private val executor: TimePickerExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "TimePickerHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Time parsing patterns
        private val TIME_PATTERN_12H = Pattern.compile(
            "(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)?",
            Pattern.CASE_INSENSITIVE
        )
        private val TIME_PATTERN_24H = Pattern.compile(
            "(\\d{1,2}):(\\d{2})(?::(\\d{2}))?",
            Pattern.CASE_INSENSITIVE
        )
        private val HOUR_PATTERN = Pattern.compile(
            "(?:set\\s+)?hour\\s+(?:to\\s+)?(\\d{1,2})",
            Pattern.CASE_INSENSITIVE
        )
        private val MINUTE_PATTERN = Pattern.compile(
            "(?:set\\s+)?minute[s]?\\s+(?:to\\s+)?(\\d{1,2})",
            Pattern.CASE_INSENSITIVE
        )

        // Word to number mapping for natural speech
        private val WORD_TO_NUMBER = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
            "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
            "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
            "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
            "eighteen" to 18, "nineteen" to 19, "twenty" to 20,
            "twenty-one" to 21, "twenty one" to 21,
            "twenty-two" to 22, "twenty two" to 22,
            "twenty-three" to 23, "twenty three" to 23,
            "twenty-four" to 24, "twenty four" to 24,
            "thirty" to 30, "forty" to 40, "forty-five" to 45, "forty five" to 45,
            "fifty" to 50, "fifty-nine" to 59, "fifty nine" to 59
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Time setting commands
        "set time to [time]",
        "set time to [hour]:[minute] [AM/PM]",

        // Hour commands
        "set hour to [N]",
        "hour [N]",
        "increase hour",
        "decrease hour",
        "hour up",
        "hour down",
        "next hour",
        "previous hour",

        // Minute commands
        "set minute to [N]",
        "minute [N]",
        "increase minute",
        "decrease minute",
        "minute up",
        "minute down",
        "next minute",
        "previous minute",

        // AM/PM commands
        "AM",
        "PM",
        "morning",
        "afternoon",
        "evening",
        "switch to AM",
        "switch to PM",
        "toggle AM PM",

        // Preset time commands
        "now",
        "current time",
        "noon",
        "midnight",
        "midday"
    )

    /**
     * Callback for voice feedback when time changes
     */
    var onTimeChanged: ((hour: Int, minute: Int, isAM: Boolean) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalized = command.phrase.lowercase().trim()

        Log.d { "Processing time picker command: '$normalized'" }

        return try {
            when {
                // Time setting commands
                normalized.startsWith("set time") || normalized.contains("time to") -> {
                    handleSetTime(normalized)
                }

                // Hour increase/decrease
                normalized == "increase hour" || normalized == "hour up" || normalized == "next hour" -> {
                    handleIncreaseHour()
                }
                normalized == "decrease hour" || normalized == "hour down" || normalized == "previous hour" -> {
                    handleDecreaseHour()
                }

                // Hour setting
                normalized.startsWith("set hour") || HOUR_PATTERN.matcher(normalized).find() -> {
                    handleSetHour(normalized)
                }

                // Minute increase/decrease
                normalized == "increase minute" || normalized == "minute up" || normalized == "next minute" -> {
                    handleIncreaseMinute()
                }
                normalized == "decrease minute" || normalized == "minute down" || normalized == "previous minute" -> {
                    handleDecreaseMinute()
                }

                // Minute setting
                normalized.startsWith("set minute") || MINUTE_PATTERN.matcher(normalized).find() -> {
                    handleSetMinute(normalized)
                }

                // AM/PM toggle
                normalized == "am" || normalized == "a.m." || normalized == "morning" -> {
                    handleSetAM()
                }
                normalized == "pm" || normalized == "p.m." || normalized == "afternoon" || normalized == "evening" -> {
                    handleSetPM()
                }
                normalized.contains("switch to am") -> handleSetAM()
                normalized.contains("switch to pm") -> handleSetPM()
                normalized == "toggle am pm" || normalized == "toggle am/pm" -> handleToggleAMPM()

                // Preset times
                normalized == "now" || normalized == "current time" -> handleSetCurrentTime()
                normalized == "noon" || normalized == "midday" -> handleSetNoon()
                normalized == "midnight" -> handleSetMidnight()

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e({ "Error processing time picker command" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Time Setting Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "set time to [time]" command
     * Parses natural language time expressions
     */
    private suspend fun handleSetTime(command: String): HandlerResult {
        // Extract time part from command
        val timeStr = command
            .replace("set time to", "")
            .replace("set the time to", "")
            .replace("time to", "")
            .trim()

        val parsedTime = parseTimeExpression(timeStr)
        return if (parsedTime != null) {
            val result = executor.setTime(parsedTime.hour, parsedTime.minute, parsedTime.isAM)
            handleTimeResult(result, "Time set")
        } else {
            HandlerResult.failure(
                reason = "Could not parse time: $timeStr",
                recoverable = true
            )
        }
    }

    /**
     * Parse time expression from natural language
     * Supports: "3:30 PM", "15:30", "three thirty", "half past three"
     */
    private fun parseTimeExpression(input: String): ParsedTime? {
        val trimmed = input.lowercase().trim()

        // Handle word-based times
        val convertedInput = convertWordsToNumbers(trimmed)

        // Try 12-hour format with AM/PM
        val matcher12h = TIME_PATTERN_12H.matcher(convertedInput)
        if (matcher12h.find()) {
            val hour = matcher12h.group(1)?.toIntOrNull() ?: return null
            val minute = matcher12h.group(2)?.toIntOrNull() ?: 0
            val period = matcher12h.group(3)?.lowercase()

            if (hour !in 1..12 || minute !in 0..59) return null

            val isAM = when {
                period == null -> true // Default to AM if not specified
                period.startsWith("a") -> true
                period.startsWith("p") -> false
                else -> true
            }

            return ParsedTime(hour, minute, isAM)
        }

        // Try 24-hour format
        val matcher24h = TIME_PATTERN_24H.matcher(convertedInput)
        if (matcher24h.find()) {
            val hour24 = matcher24h.group(1)?.toIntOrNull() ?: return null
            val minute = matcher24h.group(2)?.toIntOrNull() ?: 0

            if (hour24 !in 0..23 || minute !in 0..59) return null

            val (hour12, isAM) = convert24To12(hour24)
            return ParsedTime(hour12, minute, isAM)
        }

        // Handle special phrases
        return parseSpecialTimePhrase(trimmed)
    }

    /**
     * Parse special time phrases like "half past", "quarter to"
     */
    private fun parseSpecialTimePhrase(phrase: String): ParsedTime? {
        return when {
            phrase.contains("half past") -> {
                val hourStr = phrase.replace("half past", "").trim()
                val hour = parseHourWord(hourStr)
                if (hour != null) ParsedTime(hour, 30, true) else null
            }
            phrase.contains("quarter past") -> {
                val hourStr = phrase.replace("quarter past", "").trim()
                val hour = parseHourWord(hourStr)
                if (hour != null) ParsedTime(hour, 15, true) else null
            }
            phrase.contains("quarter to") -> {
                val hourStr = phrase.replace("quarter to", "").trim()
                val hour = parseHourWord(hourStr)
                if (hour != null) {
                    val prevHour = if (hour == 1) 12 else hour - 1
                    ParsedTime(prevHour, 45, true)
                } else null
            }
            phrase == "o'clock" || phrase.endsWith(" o'clock") -> {
                val hourStr = phrase.replace("o'clock", "").trim()
                val hour = parseHourWord(hourStr)
                if (hour != null) ParsedTime(hour, 0, true) else null
            }
            else -> null
        }
    }

    /**
     * Convert word numbers to digits
     */
    private fun convertWordsToNumbers(input: String): String {
        var result = input
        for ((word, number) in WORD_TO_NUMBER) {
            result = result.replace(word, number.toString())
        }
        return result
    }

    /**
     * Parse hour from word or number
     */
    private fun parseHourWord(input: String): Int? {
        val trimmed = input.trim()

        // Try direct number
        trimmed.toIntOrNull()?.let {
            if (it in 1..12) return it
        }

        // Try word mapping
        WORD_TO_NUMBER[trimmed]?.let {
            if (it in 1..12) return it
        }

        return null
    }

    /**
     * Convert 24-hour to 12-hour format
     */
    private fun convert24To12(hour24: Int): Pair<Int, Boolean> {
        return when {
            hour24 == 0 -> 12 to true      // 00:00 = 12:00 AM
            hour24 == 12 -> 12 to false    // 12:00 = 12:00 PM
            hour24 < 12 -> hour24 to true  // AM
            else -> (hour24 - 12) to false // PM
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Hour Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "set hour to [N]" command
     */
    private suspend fun handleSetHour(command: String): HandlerResult {
        val matcher = HOUR_PATTERN.matcher(command)
        if (matcher.find()) {
            val hour = matcher.group(1)?.toIntOrNull()
            if (hour != null && hour in 1..12) {
                val result = executor.setHour(hour)
                return handleTimeResult(result, "Hour set to $hour")
            }
        }

        // Try word-based parsing
        val hourStr = command
            .replace("set hour to", "")
            .replace("set the hour to", "")
            .replace("hour to", "")
            .replace("hour", "")
            .trim()

        val hour = parseHourWord(hourStr)
        if (hour != null) {
            val result = executor.setHour(hour)
            return handleTimeResult(result, "Hour set to $hour")
        }

        return HandlerResult.failure(
            reason = "Could not parse hour from: $command",
            recoverable = true
        )
    }

    /**
     * Increase hour by 1
     */
    private suspend fun handleIncreaseHour(): HandlerResult {
        val result = executor.increaseHour()
        return handleTimeResult(result, "Hour increased")
    }

    /**
     * Decrease hour by 1
     */
    private suspend fun handleDecreaseHour(): HandlerResult {
        val result = executor.decreaseHour()
        return handleTimeResult(result, "Hour decreased")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Minute Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "set minute to [N]" command
     */
    private suspend fun handleSetMinute(command: String): HandlerResult {
        val matcher = MINUTE_PATTERN.matcher(command)
        if (matcher.find()) {
            val minute = matcher.group(1)?.toIntOrNull()
            if (minute != null && minute in 0..59) {
                val result = executor.setMinute(minute)
                return handleTimeResult(result, "Minute set to $minute")
            }
        }

        // Try word-based parsing
        val minuteStr = command
            .replace("set minute to", "")
            .replace("set minutes to", "")
            .replace("set the minute to", "")
            .replace("minute to", "")
            .replace("minutes to", "")
            .replace("minute", "")
            .replace("minutes", "")
            .trim()

        val convertedMinute = convertWordsToNumbers(minuteStr).trim().toIntOrNull()
        if (convertedMinute != null && convertedMinute in 0..59) {
            val result = executor.setMinute(convertedMinute)
            return handleTimeResult(result, "Minute set to $convertedMinute")
        }

        return HandlerResult.failure(
            reason = "Could not parse minute from: $command",
            recoverable = true
        )
    }

    /**
     * Increase minute by 1
     */
    private suspend fun handleIncreaseMinute(): HandlerResult {
        val result = executor.increaseMinute()
        return handleTimeResult(result, "Minute increased")
    }

    /**
     * Decrease minute by 1
     */
    private suspend fun handleDecreaseMinute(): HandlerResult {
        val result = executor.decreaseMinute()
        return handleTimeResult(result, "Minute decreased")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AM/PM Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Set to AM
     */
    private suspend fun handleSetAM(): HandlerResult {
        val result = executor.setAM()
        return handleTimeResult(result, "Set to AM")
    }

    /**
     * Set to PM
     */
    private suspend fun handleSetPM(): HandlerResult {
        val result = executor.setPM()
        return handleTimeResult(result, "Set to PM")
    }

    /**
     * Toggle AM/PM
     */
    private suspend fun handleToggleAMPM(): HandlerResult {
        val result = executor.toggleAMPM()
        return handleTimeResult(result, "AM/PM toggled")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Preset Time Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Set to current system time
     */
    private suspend fun handleSetCurrentTime(): HandlerResult {
        val calendar = Calendar.getInstance()
        var hour = calendar.get(Calendar.HOUR)
        if (hour == 0) hour = 12
        val minute = calendar.get(Calendar.MINUTE)
        val isAM = calendar.get(Calendar.AM_PM) == Calendar.AM

        val result = executor.setTime(hour, minute, isAM)
        return handleTimeResult(result, "Set to current time")
    }

    /**
     * Set to noon (12:00 PM)
     */
    private suspend fun handleSetNoon(): HandlerResult {
        val result = executor.setTime(12, 0, false)
        return handleTimeResult(result, "Set to noon")
    }

    /**
     * Set to midnight (12:00 AM)
     */
    private suspend fun handleSetMidnight(): HandlerResult {
        val result = executor.setTime(12, 0, true)
        return handleTimeResult(result, "Set to midnight")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Result Handling
    // ═══════════════════════════════════════════════════════════════════════════

    private fun handleTimeResult(result: TimePickerResult, successMessage: String): HandlerResult {
        return when (result) {
            is TimePickerResult.Success -> {
                onTimeChanged?.invoke(result.hour, result.minute, result.isAM)
                val formattedTime = "${result.hour}:${String.format(Locale.US, "%02d", result.minute)} ${if (result.isAM) "AM" else "PM"}"
                HandlerResult.Success(
                    message = "$successMessage: $formattedTime",
                    data = mapOf(
                        "hour" to result.hour,
                        "minute" to result.minute,
                        "isAM" to result.isAM,
                        "formatted" to formattedTime
                    )
                )
            }
            is TimePickerResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            TimePickerResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            TimePickerResult.NoTimePickerWidget -> {
                HandlerResult.Failure(
                    reason = "No time picker widget found",
                    recoverable = true,
                    suggestedAction = "Focus on a time picker first"
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Parsed time data class (internal)
 */
private data class ParsedTime(
    val hour: Int,      // 1-12
    val minute: Int,    // 0-59
    val isAM: Boolean
)

/**
 * Time state data class for external access
 */
data class TimeState(
    val hour: Int,
    val minute: Int,
    val isAM: Boolean,
    val formatted: String
)

/**
 * Time picker handler status
 */
data class TimePickerHandlerStatus(
    val hasAccessibilityService: Boolean,
    val currentTime: TimeState?
)

/**
 * Time picker operation result
 */
sealed class TimePickerResult {
    data class Success(val hour: Int, val minute: Int, val isAM: Boolean) : TimePickerResult()
    data class Error(val message: String) : TimePickerResult()
    object NoAccessibility : TimePickerResult()
    object NoTimePickerWidget : TimePickerResult()
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for time picker operations.
 *
 * Implementations should:
 * 1. Find TimePicker/NumberPicker widgets in accessibility tree
 * 2. Update hour, minute, and AM/PM values
 * 3. Support both standard Android and Material TimePicker
 */
interface TimePickerExecutor {

    /**
     * Set the complete time.
     *
     * @param hour Hour (1-12)
     * @param minute Minute (0-59)
     * @param isAM True for AM, false for PM
     * @return The result of the operation
     */
    suspend fun setTime(hour: Int, minute: Int, isAM: Boolean): TimePickerResult

    /**
     * Set the hour value.
     *
     * @param hour Hour (1-12)
     * @return The result of the operation
     */
    suspend fun setHour(hour: Int): TimePickerResult

    /**
     * Set the minute value.
     *
     * @param minute Minute (0-59)
     * @return The result of the operation
     */
    suspend fun setMinute(minute: Int): TimePickerResult

    /**
     * Increase the hour by 1.
     *
     * @return The result of the operation
     */
    suspend fun increaseHour(): TimePickerResult

    /**
     * Decrease the hour by 1.
     *
     * @return The result of the operation
     */
    suspend fun decreaseHour(): TimePickerResult

    /**
     * Increase the minute by 1.
     *
     * @return The result of the operation
     */
    suspend fun increaseMinute(): TimePickerResult

    /**
     * Decrease the minute by 1.
     *
     * @return The result of the operation
     */
    suspend fun decreaseMinute(): TimePickerResult

    /**
     * Set to AM.
     *
     * @return The result of the operation
     */
    suspend fun setAM(): TimePickerResult

    /**
     * Set to PM.
     *
     * @return The result of the operation
     */
    suspend fun setPM(): TimePickerResult

    /**
     * Toggle AM/PM.
     *
     * @return The result of the operation
     */
    suspend fun toggleAMPM(): TimePickerResult

    /**
     * Get the current time state.
     *
     * @return Current time state or null if not available
     */
    suspend fun getCurrentTimeState(): TimeState?

    /**
     * Get handler status.
     */
    suspend fun getStatus(): TimePickerHandlerStatus
}
