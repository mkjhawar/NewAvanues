/**
 * DatePickerHandler.kt - Voice handler for DatePicker interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-27
 *
 * Purpose: Voice-driven date picker control with natural language date parsing
 * Features:
 * - Natural date parsing ("January 15", "January 15 2026")
 * - Relative dates (today, tomorrow, yesterday)
 * - Navigation (next week, next month, previous month, next year, previous year)
 * - Accessibility service integration for DatePicker updates
 * - Voice feedback for selected dates
 *
 * Location: MagicVoiceHandlers module
 */

package com.augmentalis.avamagic.voice.handlers.input

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/**
 * Voice command handler for DatePicker interactions.
 *
 * Supports natural language date commands:
 * - "set date to January 15"
 * - "set date to January 15 2026"
 * - "today" - sets to current date
 * - "tomorrow" - sets to next day
 * - "yesterday" - sets to previous day
 * - "next week" - adds 7 days
 * - "next month" - advances one month
 * - "previous month" - goes back one month
 * - "next year" - advances one year
 * - "previous year" - goes back one year
 *
 * Thread Safety:
 * - All operations execute on Main dispatcher
 * - AccessibilityService operations are synchronized via executor
 */
class DatePickerHandler(
    private val executor: DatePickerExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "DatePickerHandler"

        // Date pattern constants
        private val MONTH_NAMES = mapOf(
            "january" to Calendar.JANUARY,
            "february" to Calendar.FEBRUARY,
            "march" to Calendar.MARCH,
            "april" to Calendar.APRIL,
            "may" to Calendar.MAY,
            "june" to Calendar.JUNE,
            "july" to Calendar.JULY,
            "august" to Calendar.AUGUST,
            "september" to Calendar.SEPTEMBER,
            "october" to Calendar.OCTOBER,
            "november" to Calendar.NOVEMBER,
            "december" to Calendar.DECEMBER
        )

        // Regex patterns for date parsing
        // Pattern: "January 15" or "January 15 2026"
        private val DATE_PATTERN = Pattern.compile(
            "^(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{1,2})(?:\\s+(\\d{4}))?$",
            Pattern.CASE_INSENSITIVE
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BaseHandler Implementation
    // ═══════════════════════════════════════════════════════════════════════════

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        "set date to",
        "today",
        "tomorrow",
        "yesterday",
        "next week",
        "next month",
        "previous month",
        "last month",
        "next year",
        "previous year",
        "last year"
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════════

    // Current date state (for relative operations)
    private var currentDate: Calendar = Calendar.getInstance()

    // ═══════════════════════════════════════════════════════════════════════════
    // Initialization
    // ═══════════════════════════════════════════════════════════════════════════

    override suspend fun initialize() {
        Log.d(TAG, "DatePickerHandler initialized")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handling
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BaseHandler execute implementation
     */
    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()
        Log.d(TAG, "Executing date command: $normalizedAction")

        return try {
            when {
                // Set date to specific date
                normalizedAction.startsWith("set date to ") -> {
                    val dateString = normalizedAction.removePrefix("set date to ").trim()
                    handleSetDate(dateString)
                }

                // Relative dates
                normalizedAction == "today" -> handleRelativeDate(RelativeDate.TODAY)
                normalizedAction == "tomorrow" -> handleRelativeDate(RelativeDate.TOMORROW)
                normalizedAction == "yesterday" -> handleRelativeDate(RelativeDate.YESTERDAY)
                normalizedAction == "next week" -> handleRelativeDate(RelativeDate.NEXT_WEEK)
                normalizedAction == "next month" -> handleRelativeDate(RelativeDate.NEXT_MONTH)
                normalizedAction == "previous month" ||
                normalizedAction == "last month" -> handleRelativeDate(RelativeDate.PREVIOUS_MONTH)
                normalizedAction == "next year" -> handleRelativeDate(RelativeDate.NEXT_YEAR)
                normalizedAction == "previous year" ||
                normalizedAction == "last year" -> handleRelativeDate(RelativeDate.PREVIOUS_YEAR)

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing date command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Date Parsing
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "set date to [date]" command
     */
    private suspend fun handleSetDate(dateString: String): HandlerResult {
        val parsedDate = parseNaturalDate(dateString)
            ?: return HandlerResult.failure(
                reason = "Could not parse date: $dateString. Try 'January 15' or 'January 15 2026'",
                recoverable = true,
                suggestedAction = "Say 'set date to' followed by month and day"
            )

        return applyDate(parsedDate)
    }

    /**
     * Handle relative date commands
     */
    private suspend fun handleRelativeDate(relativeDate: RelativeDate): HandlerResult {
        val calendar = Calendar.getInstance()

        when (relativeDate) {
            RelativeDate.TODAY -> {
                // Already set to today
            }
            RelativeDate.TOMORROW -> {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            RelativeDate.YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
            }
            RelativeDate.NEXT_WEEK -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            RelativeDate.NEXT_MONTH -> {
                calendar.add(Calendar.MONTH, 1)
            }
            RelativeDate.PREVIOUS_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
            }
            RelativeDate.NEXT_YEAR -> {
                calendar.add(Calendar.YEAR, 1)
            }
            RelativeDate.PREVIOUS_YEAR -> {
                calendar.add(Calendar.YEAR, -1)
            }
        }

        return applyDate(calendar)
    }

    /**
     * Parse natural language date string
     *
     * Supports:
     * - "January 15" (uses current year)
     * - "January 15 2026" (specific year)
     */
    private fun parseNaturalDate(dateString: String): Calendar? {
        val matcher = DATE_PATTERN.matcher(dateString.lowercase().trim())

        if (!matcher.matches()) {
            Log.d(TAG, "Date string did not match pattern: $dateString")
            return null
        }

        val monthName = matcher.group(1)?.lowercase() ?: return null
        val dayString = matcher.group(2) ?: return null
        val yearString = matcher.group(3)

        val month = MONTH_NAMES[monthName] ?: return null
        val day = dayString.toIntOrNull() ?: return null
        val year = yearString?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)

        // Validate day for month
        if (!isValidDayForMonth(day, month, year)) {
            Log.w(TAG, "Invalid day $day for month $monthName")
            return null
        }

        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
    }

    /**
     * Validate day is valid for the given month/year
     */
    private fun isValidDayForMonth(day: Int, month: Int, year: Int): Boolean {
        if (day < 1) return false

        val maxDays = when (month) {
            Calendar.JANUARY, Calendar.MARCH, Calendar.MAY, Calendar.JULY,
            Calendar.AUGUST, Calendar.OCTOBER, Calendar.DECEMBER -> 31
            Calendar.APRIL, Calendar.JUNE, Calendar.SEPTEMBER, Calendar.NOVEMBER -> 30
            Calendar.FEBRUARY -> if (isLeapYear(year)) 29 else 28
            else -> return false
        }

        return day <= maxDays
    }

    /**
     * Check if year is a leap year
     */
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Executor Integration
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Apply date via executor
     */
    private suspend fun applyDate(calendar: Calendar): HandlerResult {
        val result = executor.setDate(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH),
            day = calendar.get(Calendar.DAY_OF_MONTH)
        )

        return when (result) {
            is DatePickerResult.Success -> {
                // Update internal state
                currentDate = calendar

                // Format date for feedback
                val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                val formattedDate = dateFormat.format(calendar.time)

                Log.i(TAG, "Date set to: $formattedDate")
                HandlerResult.Success(
                    message = "Date set to $formattedDate",
                    data = mapOf(
                        "year" to calendar.get(Calendar.YEAR),
                        "month" to calendar.get(Calendar.MONTH),
                        "day" to calendar.get(Calendar.DAY_OF_MONTH),
                        "formatted" to formattedDate
                    )
                )
            }
            is DatePickerResult.NoDatePickerFound -> {
                HandlerResult.failure(
                    reason = "No date picker found. Please focus a date picker first.",
                    recoverable = true,
                    suggestedAction = "Tap on a date picker to focus it"
                )
            }
            is DatePickerResult.ServiceUnavailable -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            is DatePickerResult.Failure -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = result.recoverable
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Cleanup resources
     */
    override suspend fun dispose() {
        Log.d(TAG, "DatePickerHandler disposed")
    }
}

/**
 * Relative date types for navigation commands
 */
private enum class RelativeDate {
    TODAY,
    TOMORROW,
    YESTERDAY,
    NEXT_WEEK,
    NEXT_MONTH,
    PREVIOUS_MONTH,
    NEXT_YEAR,
    PREVIOUS_YEAR
}

/**
 * Executor interface for DatePicker operations.
 * Implementations handle platform-specific accessibility interactions.
 */
interface DatePickerExecutor {
    /**
     * Set date on the focused DatePicker widget
     * @param year The year to set
     * @param month The month to set (0-indexed, Calendar.JANUARY = 0)
     * @param day The day of month to set
     * @return DatePickerResult indicating success or failure
     */
    suspend fun setDate(year: Int, month: Int, day: Int): DatePickerResult
}

/**
 * Result type for DatePicker operations
 */
sealed class DatePickerResult {
    /** Date was successfully set */
    data object Success : DatePickerResult()

    /** No DatePicker widget found in focus */
    data object NoDatePickerFound : DatePickerResult()

    /** Accessibility service is not available */
    data object ServiceUnavailable : DatePickerResult()

    /** Operation failed with message */
    data class Failure(
        val message: String,
        val recoverable: Boolean = true
    ) : DatePickerResult()
}
