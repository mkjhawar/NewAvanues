/**
 * DatePickerHandler.kt - Voice handler for DatePicker interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
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

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
        private val Log = LoggerFactory.getLogger(TAG)

        // Date pattern constants (month values are 0-indexed, matching Calendar.JANUARY=0 convention)
        private val MONTH_NAMES = mapOf(
            "january" to 0,
            "february" to 1,
            "march" to 2,
            "april" to 3,
            "may" to 4,
            "june" to 5,
            "july" to 6,
            "august" to 7,
            "september" to 8,
            "october" to 9,
            "november" to 10,
            "december" to 11
        )

        // Regex patterns for date parsing
        // Pattern: "January 15" or "January 15 2026"
        private val DATE_PATTERN = Regex(
            "^(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{1,2})(?:\\s+(\\d{4}))?$",
            RegexOption.IGNORE_CASE
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

    // Current date state (for relative operations) — stored as year/month(0-indexed)/day
    private var currentDateYear: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    private var currentDateMonth: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber - 1
    private var currentDateDay: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfMonth

    // ═══════════════════════════════════════════════════════════════════════════
    // Initialization
    // ═══════════════════════════════════════════════════════════════════════════

    override suspend fun initialize() {
        Log.d { "DatePickerHandler initialized" }
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
        Log.d { "Executing date command: $normalizedAction" }

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
            Log.e({ "Error executing date command" }, e)
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

        return applyDate(parsedDate.first, parsedDate.second, parsedDate.third)
    }

    /**
     * Handle relative date commands using kotlinx-datetime arithmetic
     */
    private suspend fun handleRelativeDate(relativeDate: RelativeDate): HandlerResult {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        var year = now.year
        var month = now.monthNumber - 1  // 0-indexed
        var day = now.dayOfMonth

        when (relativeDate) {
            RelativeDate.TODAY -> {
                // Already set to today
            }
            RelativeDate.TOMORROW -> {
                val result = addDays(year, month, day, 1)
                year = result.first; month = result.second; day = result.third
            }
            RelativeDate.YESTERDAY -> {
                val result = addDays(year, month, day, -1)
                year = result.first; month = result.second; day = result.third
            }
            RelativeDate.NEXT_WEEK -> {
                val result = addDays(year, month, day, 7)
                year = result.first; month = result.second; day = result.third
            }
            RelativeDate.NEXT_MONTH -> {
                val result = addMonths(year, month, 1)
                year = result.first; month = result.second; day = minOf(day, daysInMonth(result.first, result.second))
            }
            RelativeDate.PREVIOUS_MONTH -> {
                val result = addMonths(year, month, -1)
                year = result.first; month = result.second; day = minOf(day, daysInMonth(result.first, result.second))
            }
            RelativeDate.NEXT_YEAR -> {
                year += 1
                day = minOf(day, daysInMonth(year, month))
            }
            RelativeDate.PREVIOUS_YEAR -> {
                year -= 1
                day = minOf(day, daysInMonth(year, month))
            }
        }

        return applyDate(year, month, day)
    }

    /**
     * Parse natural language date string
     *
     * Supports:
     * - "January 15" (uses current year)
     * - "January 15 2026" (specific year)
     *
     * Returns Triple(year, month 0-indexed, day) or null
     */
    private fun parseNaturalDate(dateString: String): Triple<Int, Int, Int>? {
        val matchResult = DATE_PATTERN.find(dateString.lowercase().trim())

        if (matchResult == null) {
            Log.d { "Date string did not match pattern: $dateString" }
            return null
        }

        val monthName = matchResult.groupValues[1].lowercase()
        val dayString = matchResult.groupValues[2]
        val yearString = matchResult.groupValues[3].takeIf { it.isNotBlank() }

        val month = MONTH_NAMES[monthName] ?: return null
        val day = dayString.toIntOrNull() ?: return null
        val year = yearString?.toIntOrNull()
            ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year

        // Validate day for month
        if (!isValidDayForMonth(day, month, year)) {
            Log.w { "Invalid day $day for month $monthName" }
            return null
        }

        return Triple(year, month, day)
    }

    /**
     * Validate day is valid for the given month/year
     */
    private fun isValidDayForMonth(day: Int, month: Int, year: Int): Boolean {
        if (day < 1) return false
        return day <= daysInMonth(year, month)
    }

    /**
     * Returns the number of days in the given month (0-indexed month).
     */
    private fun daysInMonth(year: Int, month: Int): Int {
        return when (month) {
            0, 2, 4, 6, 7, 9, 11 -> 31  // Jan, Mar, May, Jul, Aug, Oct, Dec
            3, 5, 8, 10 -> 30            // Apr, Jun, Sep, Nov
            1 -> if (isLeapYear(year)) 29 else 28  // Feb
            else -> 30
        }
    }

    /**
     * Add days to a date, rolling over months and years as needed.
     */
    private fun addDays(year: Int, month: Int, day: Int, delta: Int): Triple<Int, Int, Int> {
        var y = year; var m = month; var d = day + delta
        while (d > daysInMonth(y, m)) {
            d -= daysInMonth(y, m)
            val next = addMonths(y, m, 1)
            y = next.first; m = next.second
        }
        while (d < 1) {
            val prev = addMonths(y, m, -1)
            y = prev.first; m = prev.second
            d += daysInMonth(y, m)
        }
        return Triple(y, m, d)
    }

    /**
     * Add months to a date, rolling over years as needed.
     */
    private fun addMonths(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val totalMonths = year * 12 + month + delta
        return Pair(totalMonths / 12, totalMonths % 12)
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
     * Apply date via executor.
     *
     * @param year The year
     * @param month The month (0-indexed, 0=January)
     * @param day The day of month
     */
    private suspend fun applyDate(year: Int, month: Int, day: Int): HandlerResult {
        val result = executor.setDate(year = year, month = month, day = day)

        return when (result) {
            is DatePickerResult.Success -> {
                // Update internal state
                currentDateYear = year
                currentDateMonth = month
                currentDateDay = day

                // Format date for feedback using manual formatting (KMP-safe)
                val monthNames = listOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )
                val monthName = if (month in 0..11) monthNames[month] else "Unknown"
                val formattedDate = "$monthName $day, $year"

                Log.i { "Date set to: $formattedDate" }
                HandlerResult.Success(
                    message = "Date set to $formattedDate",
                    data = mapOf(
                        "year" to year,
                        "month" to month,
                        "day" to day,
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
        Log.d { "DatePickerHandler disposed" }
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
