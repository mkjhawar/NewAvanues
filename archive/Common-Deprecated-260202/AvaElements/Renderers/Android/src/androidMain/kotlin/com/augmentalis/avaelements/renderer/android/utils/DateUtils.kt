package com.augmentalis.avaelements.renderer.android.utils

import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale

/**
 * Date utility functions for calendar components.
 *
 * Provides ISO 8601 date parsing, formatting, and conversion utilities
 * for use with Material 3 DatePicker and custom calendar components.
 */
object DateUtils {

    // ISO 8601 date format (YYYY-MM-DD)
    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    // Time format (HH:mm)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    // Full date-time format (for display)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.US)

    // Date format for display
    private val displayDateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)

    /**
     * Parses an ISO 8601 date string to milliseconds since epoch.
     *
     * @param isoDate Date string in YYYY-MM-DD format
     * @return Milliseconds since epoch, or null if parsing fails
     */
    fun parseIsoDate(isoDate: String?): Long? {
        if (isoDate.isNullOrBlank()) return null

        return try {
            val localDate = LocalDate.parse(isoDate, isoDateFormatter)
            localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats a timestamp (milliseconds since epoch) to ISO 8601 date string.
     *
     * @param timestamp Milliseconds since epoch
     * @return Date string in YYYY-MM-DD format
     */
    fun formatTimestamp(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        return localDate.format(isoDateFormatter)
    }

    /**
     * Formats an ISO 8601 date string for display.
     *
     * @param isoDate Date string in YYYY-MM-DD format
     * @return Formatted date string (e.g., "Nov 24, 2025")
     */
    fun formatDate(isoDate: String): String {
        return try {
            val localDate = LocalDate.parse(isoDate, isoDateFormatter)
            localDate.format(displayDateFormatter)
        } catch (e: Exception) {
            isoDate
        }
    }

    /**
     * Formats an ISO 8601 date string with time for display.
     *
     * @param isoDate Date string in YYYY-MM-DD format
     * @param time Time string in HH:mm format
     * @return Formatted date-time string (e.g., "Nov 24, 2025 14:30")
     */
    fun formatDateTime(isoDate: String, time: String): String {
        return try {
            val localDate = LocalDate.parse(isoDate, isoDateFormatter)
            val localTime = LocalTime.parse(time, timeFormatter)
            val localDateTime = LocalDateTime.of(localDate, localTime)
            localDateTime.format(dateTimeFormatter)
        } catch (e: Exception) {
            "$isoDate $time"
        }
    }

    /**
     * Parses a color string (hex format) to Android Color.
     *
     * @param colorString Color in hex format (#RRGGBB or #AARRGGBB)
     * @return Color value, or null if parsing fails
     */
    fun parseColor(colorString: String?): Long? {
        if (colorString.isNullOrBlank()) return null

        return try {
            val hex = colorString.removePrefix("#")
            val color = when (hex.length) {
                6 -> "FF$hex" // Add alpha channel
                8 -> hex
                else -> return null
            }
            color.toLong(16)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets the current date in ISO 8601 format.
     *
     * @return Current date string in YYYY-MM-DD format
     */
    fun getCurrentDate(): String {
        return LocalDate.now().format(isoDateFormatter)
    }

    /**
     * Gets the start of the week (Monday) for a given date.
     *
     * @param isoDate Date string in YYYY-MM-DD format
     * @return Start of week date string in YYYY-MM-DD format
     */
    fun getWeekStart(isoDate: String): String {
        return try {
            val localDate = LocalDate.parse(isoDate, isoDateFormatter)
            val monday = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            monday.format(isoDateFormatter)
        } catch (e: Exception) {
            isoDate
        }
    }

    /**
     * Gets the end of the week (Sunday) for a given date.
     *
     * @param isoDate Date string in YYYY-MM-DD format
     * @return End of week date string in YYYY-MM-DD format
     */
    fun getWeekEnd(isoDate: String): String {
        return try {
            val localDate = LocalDate.parse(isoDate, isoDateFormatter)
            val sunday = localDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            sunday.format(isoDateFormatter)
        } catch (e: Exception) {
            isoDate
        }
    }

    /**
     * Gets all dates in a week starting from the given date.
     *
     * @param startDate Start of week (Monday) in YYYY-MM-DD format
     * @return List of 7 date strings in YYYY-MM-DD format (Monday to Sunday)
     */
    fun getWeekDates(startDate: String): List<String> {
        return try {
            val localDate = LocalDate.parse(startDate, isoDateFormatter)
            (0..6).map { offset ->
                localDate.plusDays(offset.toLong()).format(isoDateFormatter)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Checks if a date is within a range.
     *
     * @param date Date to check in YYYY-MM-DD format
     * @param minDate Minimum date in YYYY-MM-DD format, or null for no minimum
     * @param maxDate Maximum date in YYYY-MM-DD format, or null for no maximum
     * @return True if date is within range, false otherwise
     */
    fun isDateInRange(date: String, minDate: String?, maxDate: String?): Boolean {
        return try {
            val localDate = LocalDate.parse(date, isoDateFormatter)

            val afterMin = minDate?.let { min ->
                val minLocalDate = LocalDate.parse(min, isoDateFormatter)
                !localDate.isBefore(minLocalDate)
            } ?: true

            val beforeMax = maxDate?.let { max ->
                val maxLocalDate = LocalDate.parse(max, isoDateFormatter)
                !localDate.isAfter(maxLocalDate)
            } ?: true

            afterMin && beforeMax
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Parses a time string to hour and minute components.
     *
     * @param timeString Time in HH:mm format
     * @return Pair of (hour, minute), or null if parsing fails
     */
    fun parseTime(timeString: String?): Pair<Int, Int>? {
        if (timeString.isNullOrBlank()) return null

        return try {
            val parts = timeString.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                if (hour in 0..23 && minute in 0..59) {
                    Pair(hour, minute)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats hour and minute to time string.
     *
     * @param hour Hour (0-23)
     * @param minute Minute (0-59)
     * @return Time string in HH:mm format
     */
    fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    /**
     * Calculates the duration in minutes between two times.
     *
     * @param startTime Start time in HH:mm format
     * @param endTime End time in HH:mm format
     * @return Duration in minutes, or null if parsing fails
     */
    fun calculateDuration(startTime: String, endTime: String): Int? {
        val start = parseTime(startTime) ?: return null
        val end = parseTime(endTime) ?: return null

        val startMinutes = start.first * 60 + start.second
        val endMinutes = end.first * 60 + end.second

        return if (endMinutes >= startMinutes) {
            endMinutes - startMinutes
        } else {
            // Handle overnight events
            (24 * 60 - startMinutes) + endMinutes
        }
    }

    /**
     * Gets the day of week for a given date.
     *
     * @param isoDate Date string in YYYY-MM-DD format
     * @return Day of week (0 = Sunday, 1 = Monday, ..., 6 = Saturday)
     */
    fun getDayOfWeek(isoDate: String): Int {
        return try {
            val localDate = LocalDate.parse(isoDate, isoDateFormatter)
            val dayOfWeek = localDate.dayOfWeek.value
            // Convert from Monday=1 to Sunday=0 format
            if (dayOfWeek == 7) 0 else dayOfWeek
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Formats day of week to short name.
     *
     * @param dayOfWeek Day of week (0 = Sunday, 1 = Monday, ..., 6 = Saturday)
     * @return Short day name (e.g., "Mon", "Tue")
     */
    fun formatDayOfWeek(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            0 -> "Sun"
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            else -> ""
        }
    }
}
