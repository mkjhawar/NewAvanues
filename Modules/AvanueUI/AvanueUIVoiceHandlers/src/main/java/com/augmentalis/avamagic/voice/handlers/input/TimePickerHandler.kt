/**
 * TimePickerHandler.kt
 *
 * Created: 2026-01-27 00:00 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Voice command handler for TimePicker widget interactions
 * Features: Time setting, hour/minute adjustment, AM/PM toggle, preset times
 * Location: CommandManager module - handlers package
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
 * - v1.0.0 (2026-01-27): Initial implementation with full time picker support
 */

package com.augmentalis.avamagic.voice.handlers.input

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.CommandHandler
import com.augmentalis.commandmanager.CommandRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

/**
 * Voice command handler for TimePicker widget interactions.
 *
 * Design:
 * - Implements CommandHandler for CommandRegistry integration
 * - Singleton pattern for consistent state management
 * - Thread-safe with atomic references
 * - Supports both 12-hour and 24-hour time formats
 * - Natural language time parsing
 *
 * Time Picker Detection:
 * - Searches for TimePicker/NumberPicker nodes via accessibility
 * - Supports Android standard TimePicker and MaterialTimePicker
 * - Falls back to text-based hour/minute input fields
 */
class TimePickerHandler private constructor(
    private val context: Context
) : CommandHandler {

    companion object {
        private const val TAG = "TimePickerHandler"
        private const val MODULE_ID = "timepicker"

        @Volatile
        private var instance: TimePickerHandler? = null

        /**
         * Get singleton instance
         */
        fun getInstance(context: Context): TimePickerHandler {
            return instance ?: synchronized(this) {
                instance ?: TimePickerHandler(context.applicationContext).also {
                    instance = it
                }
            }
        }

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

        // Command prefixes
        private val TIME_PREFIXES = setOf(
            "set time", "time", "set the time"
        )
        private val HOUR_PREFIXES = setOf(
            "set hour", "hour", "set the hour"
        )
        private val MINUTE_PREFIXES = setOf(
            "set minute", "set minutes", "minute", "minutes"
        )
    }

    // CommandHandler interface implementation
    override val moduleId: String = MODULE_ID

    override val supportedCommands: List<String> = listOf(
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

    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val accessibilityServiceRef = AtomicReference<AccessibilityService?>(null)

    // Track current time picker state
    private var currentHour: Int = 12
    private var currentMinute: Int = 0
    private var isAM: Boolean = true
    private var is24HourFormat: Boolean = false

    private var isInitialized = false

    init {
        initialize()
        // Register with CommandRegistry
        CommandRegistry.registerHandler(moduleId, this)
    }

    /**
     * Initialize the handler
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return true
        }

        return try {
            // Initialize with current time
            val calendar = Calendar.getInstance()
            currentHour = calendar.get(Calendar.HOUR)
            if (currentHour == 0) currentHour = 12
            currentMinute = calendar.get(Calendar.MINUTE)
            isAM = calendar.get(Calendar.AM_PM) == Calendar.AM

            isInitialized = true
            Log.d(TAG, "TimePickerHandler initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }

    /**
     * Set the accessibility service reference
     */
    fun setAccessibilityService(service: AccessibilityService?) {
        accessibilityServiceRef.set(service)
    }

    /**
     * CommandHandler interface: Check if this handler can process the command
     */
    override fun canHandle(command: String): Boolean {
        val normalized = command.lowercase().trim()

        return when {
            // Time setting commands
            normalized.startsWith("set time") -> true
            normalized.contains("time to") -> true

            // Hour commands
            normalized.startsWith("set hour") -> true
            normalized == "increase hour" || normalized == "hour up" || normalized == "next hour" -> true
            normalized == "decrease hour" || normalized == "hour down" || normalized == "previous hour" -> true
            HOUR_PATTERN.matcher(normalized).find() -> true

            // Minute commands
            normalized.startsWith("set minute") -> true
            normalized == "increase minute" || normalized == "minute up" || normalized == "next minute" -> true
            normalized == "decrease minute" || normalized == "minute down" || normalized == "previous minute" -> true
            MINUTE_PATTERN.matcher(normalized).find() -> true

            // AM/PM commands
            normalized == "am" || normalized == "a.m." -> true
            normalized == "pm" || normalized == "p.m." -> true
            normalized == "morning" -> true
            normalized == "afternoon" || normalized == "evening" -> true
            normalized.contains("switch to am") || normalized.contains("switch to pm") -> true
            normalized == "toggle am pm" || normalized == "toggle am/pm" -> true

            // Preset commands
            normalized == "now" || normalized == "current time" -> true
            normalized == "noon" || normalized == "midday" -> true
            normalized == "midnight" -> true

            else -> false
        }
    }

    /**
     * CommandHandler interface: Execute the command
     */
    override suspend fun handleCommand(command: String): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "Handler not initialized")
            return false
        }

        val normalized = command.lowercase().trim()
        Log.d(TAG, "Processing command: '$normalized'")

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

                else -> {
                    Log.w(TAG, "Unhandled command: $normalized")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling command: $command", e)
            false
        }
    }

    // ==================== Time Setting Handlers ====================

    /**
     * Handle "set time to [time]" command
     * Parses natural language time expressions
     */
    private suspend fun handleSetTime(command: String): Boolean {
        // Extract time part from command
        val timeStr = command
            .replace("set time to", "")
            .replace("set the time to", "")
            .replace("time to", "")
            .trim()

        val parsedTime = parseTimeExpression(timeStr)
        return if (parsedTime != null) {
            currentHour = parsedTime.hour
            currentMinute = parsedTime.minute
            isAM = parsedTime.isAM
            applyTimeToWidget()
        } else {
            Log.w(TAG, "Failed to parse time: $timeStr")
            false
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
                period == null -> this.isAM // Keep current AM/PM
                period.startsWith("a") -> true
                period.startsWith("p") -> false
                else -> this.isAM
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
                if (hour != null) ParsedTime(hour, 30, isAM) else null
            }
            phrase.contains("quarter past") -> {
                val hourStr = phrase.replace("quarter past", "").trim()
                val hour = parseHourWord(hourStr)
                if (hour != null) ParsedTime(hour, 15, isAM) else null
            }
            phrase.contains("quarter to") -> {
                val hourStr = phrase.replace("quarter to", "").trim()
                val hour = parseHourWord(hourStr)
                if (hour != null) {
                    val prevHour = if (hour == 1) 12 else hour - 1
                    ParsedTime(prevHour, 45, isAM)
                } else null
            }
            phrase == "o'clock" || phrase.endsWith(" o'clock") -> {
                val hourStr = phrase.replace("o'clock", "").trim()
                val hour = parseHourWord(hourStr)
                if (hour != null) ParsedTime(hour, 0, isAM) else null
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

    // ==================== Hour Handlers ====================

    /**
     * Handle "set hour to [N]" command
     */
    private suspend fun handleSetHour(command: String): Boolean {
        val matcher = HOUR_PATTERN.matcher(command)
        if (matcher.find()) {
            val hour = matcher.group(1)?.toIntOrNull()
            if (hour != null && hour in 1..12) {
                currentHour = hour
                return applyTimeToWidget()
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
            currentHour = hour
            return applyTimeToWidget()
        }

        Log.w(TAG, "Failed to parse hour from: $command")
        return false
    }

    /**
     * Increase hour by 1
     */
    private suspend fun handleIncreaseHour(): Boolean {
        currentHour = if (currentHour == 12) 1 else currentHour + 1
        // Toggle AM/PM when rolling over
        if (currentHour == 12) {
            isAM = !isAM
        }
        Log.d(TAG, "Increased hour to: $currentHour ${if (isAM) "AM" else "PM"}")
        return applyTimeToWidget()
    }

    /**
     * Decrease hour by 1
     */
    private suspend fun handleDecreaseHour(): Boolean {
        currentHour = if (currentHour == 1) 12 else currentHour - 1
        // Toggle AM/PM when rolling back
        if (currentHour == 11) {
            isAM = !isAM
        }
        Log.d(TAG, "Decreased hour to: $currentHour ${if (isAM) "AM" else "PM"}")
        return applyTimeToWidget()
    }

    // ==================== Minute Handlers ====================

    /**
     * Handle "set minute to [N]" command
     */
    private suspend fun handleSetMinute(command: String): Boolean {
        val matcher = MINUTE_PATTERN.matcher(command)
        if (matcher.find()) {
            val minute = matcher.group(1)?.toIntOrNull()
            if (minute != null && minute in 0..59) {
                currentMinute = minute
                return applyTimeToWidget()
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
            currentMinute = convertedMinute
            return applyTimeToWidget()
        }

        Log.w(TAG, "Failed to parse minute from: $command")
        return false
    }

    /**
     * Increase minute by 1
     */
    private suspend fun handleIncreaseMinute(): Boolean {
        currentMinute = (currentMinute + 1) % 60
        // Roll over hour when minute wraps
        if (currentMinute == 0) {
            handleIncreaseHour()
        }
        Log.d(TAG, "Increased minute to: $currentMinute")
        return applyTimeToWidget()
    }

    /**
     * Decrease minute by 1
     */
    private suspend fun handleDecreaseMinute(): Boolean {
        currentMinute = if (currentMinute == 0) 59 else currentMinute - 1
        // Roll back hour when minute wraps
        if (currentMinute == 59) {
            handleDecreaseHour()
        }
        Log.d(TAG, "Decreased minute to: $currentMinute")
        return applyTimeToWidget()
    }

    // ==================== AM/PM Handlers ====================

    /**
     * Set to AM
     */
    private suspend fun handleSetAM(): Boolean {
        isAM = true
        Log.d(TAG, "Set to AM")
        return applyTimeToWidget()
    }

    /**
     * Set to PM
     */
    private suspend fun handleSetPM(): Boolean {
        isAM = false
        Log.d(TAG, "Set to PM")
        return applyTimeToWidget()
    }

    /**
     * Toggle AM/PM
     */
    private suspend fun handleToggleAMPM(): Boolean {
        isAM = !isAM
        Log.d(TAG, "Toggled to: ${if (isAM) "AM" else "PM"}")
        return applyTimeToWidget()
    }

    // ==================== Preset Time Handlers ====================

    /**
     * Set to current system time
     */
    private suspend fun handleSetCurrentTime(): Boolean {
        val calendar = Calendar.getInstance()
        currentHour = calendar.get(Calendar.HOUR)
        if (currentHour == 0) currentHour = 12
        currentMinute = calendar.get(Calendar.MINUTE)
        isAM = calendar.get(Calendar.AM_PM) == Calendar.AM
        Log.d(TAG, "Set to current time: $currentHour:${String.format(Locale.US, "%02d", currentMinute)} ${if (isAM) "AM" else "PM"}")
        return applyTimeToWidget()
    }

    /**
     * Set to noon (12:00 PM)
     */
    private suspend fun handleSetNoon(): Boolean {
        currentHour = 12
        currentMinute = 0
        isAM = false
        Log.d(TAG, "Set to noon: 12:00 PM")
        return applyTimeToWidget()
    }

    /**
     * Set to midnight (12:00 AM)
     */
    private suspend fun handleSetMidnight(): Boolean {
        currentHour = 12
        currentMinute = 0
        isAM = true
        Log.d(TAG, "Set to midnight: 12:00 AM")
        return applyTimeToWidget()
    }

    // ==================== Widget Interaction ====================

    /**
     * Apply the current time state to the TimePicker widget via accessibility
     */
    private suspend fun applyTimeToWidget(): Boolean {
        return withContext(Dispatchers.Main) {
            val service = accessibilityServiceRef.get()
            if (service == null) {
                Log.w(TAG, "AccessibilityService not available")
                return@withContext false
            }

            try {
                val rootNode = service.rootInActiveWindow
                if (rootNode == null) {
                    Log.w(TAG, "Root node not available")
                    return@withContext false
                }

                // Try different strategies to find and interact with TimePicker
                val success = findAndUpdateTimePicker(rootNode) ||
                             findAndUpdateNumberPickers(rootNode) ||
                             findAndUpdateTimeInputFields(rootNode)

                rootNode.recycle()

                if (success) {
                    Log.i(TAG, "Time set to: $currentHour:${String.format(Locale.US, "%02d", currentMinute)} ${if (isAM) "AM" else "PM"}")
                } else {
                    Log.w(TAG, "Failed to find TimePicker widget")
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Error applying time to widget", e)
                false
            }
        }
    }

    /**
     * Find TimePicker widget and update it
     */
    private fun findAndUpdateTimePicker(rootNode: AccessibilityNodeInfo): Boolean {
        // Look for TimePicker class
        val timePickerNodes = findNodesByClassName(rootNode, "android.widget.TimePicker")
        if (timePickerNodes.isEmpty()) {
            // Also try MaterialTimePicker
            val materialPickers = findNodesByClassName(rootNode, "com.google.android.material.timepicker.TimePickerView")
            if (materialPickers.isNotEmpty()) {
                return updateMaterialTimePicker(materialPickers.first())
            }
            return false
        }

        val timePicker = timePickerNodes.first()
        return updateStandardTimePicker(timePicker)
    }

    /**
     * Update standard Android TimePicker
     */
    private fun updateStandardTimePicker(timePicker: AccessibilityNodeInfo): Boolean {
        // Find hour and minute NumberPickers within TimePicker
        val numberPickers = findNodesByClassName(timePicker, "android.widget.NumberPicker")

        if (numberPickers.size >= 2) {
            val hourPicker = numberPickers[0]
            val minutePicker = numberPickers[1]
            val amPmPicker = if (numberPickers.size >= 3) numberPickers[2] else null

            // Update hour
            updateNumberPickerValue(hourPicker, currentHour)

            // Update minute
            updateNumberPickerValue(minutePicker, currentMinute)

            // Update AM/PM if present (12-hour format)
            amPmPicker?.let {
                updateNumberPickerValue(it, if (isAM) 0 else 1)
            }

            return true
        }

        return false
    }

    /**
     * Update Material Design TimePicker
     */
    private fun updateMaterialTimePicker(timePicker: AccessibilityNodeInfo): Boolean {
        // Material TimePicker uses clickable chip views for hour/minute
        // Find hour and minute text views
        val textNodes = findNodesByClassName(timePicker, "android.widget.TextView")

        for (node in textNodes) {
            val text = node.text?.toString() ?: continue
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

            // Check if this is hour or minute field
            when {
                contentDesc.contains("hour") -> {
                    setNodeText(node, currentHour.toString())
                }
                contentDesc.contains("minute") -> {
                    setNodeText(node, String.format(Locale.US, "%02d", currentMinute))
                }
            }
        }

        // Handle AM/PM toggle
        val buttonNodes = findNodesByClassName(timePicker, "android.widget.Button")
        for (node in buttonNodes) {
            val text = node.text?.toString()?.lowercase() ?: continue
            if ((text == "am" && !isAM) || (text == "pm" && isAM)) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        return true
    }

    /**
     * Find and update individual NumberPicker widgets
     */
    private fun findAndUpdateNumberPickers(rootNode: AccessibilityNodeInfo): Boolean {
        val numberPickers = findNodesByClassName(rootNode, "android.widget.NumberPicker")

        // Need at least 2 NumberPickers for hour and minute
        if (numberPickers.size < 2) return false

        // Heuristic: first picker is hour, second is minute
        // This may need adjustment based on picker order in different apps
        val hourPicker = numberPickers[0]
        val minutePicker = numberPickers[1]

        updateNumberPickerValue(hourPicker, currentHour)
        updateNumberPickerValue(minutePicker, currentMinute)

        // Handle AM/PM picker if present
        if (numberPickers.size >= 3) {
            updateNumberPickerValue(numberPickers[2], if (isAM) 0 else 1)
        }

        return true
    }

    /**
     * Find and update EditText/input fields for time
     */
    private fun findAndUpdateTimeInputFields(rootNode: AccessibilityNodeInfo): Boolean {
        val editTextNodes = findNodesByClassName(rootNode, "android.widget.EditText")

        var hourUpdated = false
        var minuteUpdated = false

        for (node in editTextNodes) {
            val hint = node.hintText?.toString()?.lowercase() ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

            when {
                hint.contains("hour") || contentDesc.contains("hour") -> {
                    if (setNodeText(node, currentHour.toString())) {
                        hourUpdated = true
                    }
                }
                hint.contains("minute") || contentDesc.contains("minute") -> {
                    if (setNodeText(node, String.format(Locale.US, "%02d", currentMinute))) {
                        minuteUpdated = true
                    }
                }
            }
        }

        // Handle AM/PM buttons or spinners
        val buttonNodes = findNodesByClassName(rootNode, "android.widget.Button")
        for (node in buttonNodes) {
            val text = node.text?.toString()?.lowercase() ?: continue
            if (text == "am" || text == "pm") {
                if ((text == "am" && !isAM) || (text == "pm" && isAM)) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        }

        return hourUpdated || minuteUpdated
    }

    /**
     * Update NumberPicker value using scroll actions
     */
    private fun updateNumberPickerValue(picker: AccessibilityNodeInfo, targetValue: Int): Boolean {
        // Try to get current value from the picker
        val currentText = picker.text?.toString()?.toIntOrNull()

        if (currentText != null && currentText == targetValue) {
            return true // Already at target value
        }

        // Use ACTION_SCROLL_FORWARD/BACKWARD to change value
        // This is a simplified approach - full implementation would
        // calculate direction and number of scrolls needed

        // Try setting text directly first (works on some pickers)
        val bundle = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                targetValue.toString()
            )
        }

        if (picker.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)) {
            return true
        }

        // Fallback: try clicking to focus, then scroll
        picker.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        // Look for editable child
        for (i in 0 until picker.childCount) {
            val child = picker.getChild(i)
            if (child?.isEditable == true) {
                setNodeText(child, targetValue.toString())
                child.recycle()
                return true
            }
            child?.recycle()
        }

        return false
    }

    /**
     * Set text on an accessibility node
     */
    private fun setNodeText(node: AccessibilityNodeInfo, text: String): Boolean {
        val bundle = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
    }

    /**
     * Find nodes by class name recursively
     */
    private fun findNodesByClassName(
        rootNode: AccessibilityNodeInfo,
        className: String
    ): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        findNodesByClassNameRecursive(rootNode, className, results)
        return results
    }

    private fun findNodesByClassNameRecursive(
        node: AccessibilityNodeInfo,
        className: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.className?.toString() == className) {
            results.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findNodesByClassNameRecursive(child, className, results)
        }
    }

    /**
     * Get current time state
     */
    fun getCurrentTimeState(): TimeState {
        return TimeState(
            hour = currentHour,
            minute = currentMinute,
            isAM = isAM,
            formatted = "$currentHour:${String.format(Locale.US, "%02d", currentMinute)} ${if (isAM) "AM" else "PM"}"
        )
    }

    /**
     * Check if handler is ready
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Cleanup resources
     */
    fun dispose() {
        CommandRegistry.unregisterHandler(moduleId)
        handlerScope.cancel()
        accessibilityServiceRef.set(null)
        instance = null
        Log.d(TAG, "TimePickerHandler disposed")
    }
}

/**
 * Parsed time data class
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
