package com.augmentalis.avaelements.renderers.android

import com.augmentalis.avaelements.renderer.android.utils.DateUtils
import org.junit.Test
import org.junit.Assert.*

/**
 * Test suite for DateUtils utility functions.
 *
 * Tests cover:
 * - ISO 8601 date parsing and formatting
 * - Time parsing and formatting
 * - Date range validation
 * - Week calculations
 * - Color parsing
 * - Edge cases and error handling
 */
class DateUtilsTest {

    @Test
    fun parseIsoDate_validDate_returnsMillis() {
        // GIVEN: A valid ISO 8601 date
        val isoDate = "2025-11-24"

        // WHEN: Parsed
        val millis = DateUtils.parseIsoDate(isoDate)

        // THEN: Returns non-null milliseconds
        assertNotNull(millis)
        assertTrue(millis!! > 0)
    }

    @Test
    fun parseIsoDate_nullDate_returnsNull() {
        // GIVEN: A null date
        val isoDate: String? = null

        // WHEN: Parsed
        val millis = DateUtils.parseIsoDate(isoDate)

        // THEN: Returns null
        assertNull(millis)
    }

    @Test
    fun parseIsoDate_emptyDate_returnsNull() {
        // GIVEN: An empty date
        val isoDate = ""

        // WHEN: Parsed
        val millis = DateUtils.parseIsoDate(isoDate)

        // THEN: Returns null
        assertNull(millis)
    }

    @Test
    fun parseIsoDate_invalidDate_returnsNull() {
        // GIVEN: An invalid date format
        val isoDate = "invalid-date"

        // WHEN: Parsed
        val millis = DateUtils.parseIsoDate(isoDate)

        // THEN: Returns null
        assertNull(millis)
    }

    @Test
    fun formatTimestamp_validTimestamp_returnsIsoDate() {
        // GIVEN: A valid timestamp
        val timestamp = 1732406400000L // 2025-11-24

        // WHEN: Formatted
        val isoDate = DateUtils.formatTimestamp(timestamp)

        // THEN: Returns ISO 8601 date
        assertTrue(isoDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun formatDate_validIsoDate_returnsDisplayDate() {
        // GIVEN: A valid ISO 8601 date
        val isoDate = "2025-11-24"

        // WHEN: Formatted for display
        val displayDate = DateUtils.formatDate(isoDate)

        // THEN: Returns formatted date
        assertTrue(displayDate.contains("Nov"))
        assertTrue(displayDate.contains("24"))
        assertTrue(displayDate.contains("2025"))
    }

    @Test
    fun formatDate_invalidDate_returnsOriginal() {
        // GIVEN: An invalid date
        val isoDate = "invalid-date"

        // WHEN: Formatted
        val displayDate = DateUtils.formatDate(isoDate)

        // THEN: Returns original string
        assertEquals(isoDate, displayDate)
    }

    @Test
    fun formatDateTime_validDateTime_returnsFormatted() {
        // GIVEN: Valid date and time
        val isoDate = "2025-11-24"
        val time = "14:30"

        // WHEN: Formatted
        val formatted = DateUtils.formatDateTime(isoDate, time)

        // THEN: Returns formatted date-time
        assertTrue(formatted.contains("Nov"))
        assertTrue(formatted.contains("14:30"))
    }

    @Test
    fun parseColor_validHex6_returnsColor() {
        // GIVEN: Valid 6-digit hex color
        val colorString = "#2196F3"

        // WHEN: Parsed
        val color = DateUtils.parseColor(colorString)

        // THEN: Returns color value
        assertNotNull(color)
    }

    @Test
    fun parseColor_validHex8_returnsColor() {
        // GIVEN: Valid 8-digit hex color with alpha
        val colorString = "#FF2196F3"

        // WHEN: Parsed
        val color = DateUtils.parseColor(colorString)

        // THEN: Returns color value
        assertNotNull(color)
    }

    @Test
    fun parseColor_nullColor_returnsNull() {
        // GIVEN: Null color
        val colorString: String? = null

        // WHEN: Parsed
        val color = DateUtils.parseColor(colorString)

        // THEN: Returns null
        assertNull(color)
    }

    @Test
    fun parseColor_invalidColor_returnsNull() {
        // GIVEN: Invalid color format
        val colorString = "invalid"

        // WHEN: Parsed
        val color = DateUtils.parseColor(colorString)

        // THEN: Returns null
        assertNull(color)
    }

    @Test
    fun getCurrentDate_returnsValidIsoDate() {
        // WHEN: Getting current date
        val currentDate = DateUtils.getCurrentDate()

        // THEN: Returns valid ISO 8601 format
        assertTrue(currentDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun getWeekStart_validDate_returnsMonday() {
        // GIVEN: A date (Wednesday)
        val isoDate = "2025-11-24" // Wednesday

        // WHEN: Getting week start
        val weekStart = DateUtils.getWeekStart(isoDate)

        // THEN: Returns Monday of that week
        assertEquals("2025-11-18", weekStart) // Monday
    }

    @Test
    fun getWeekEnd_validDate_returnsSunday() {
        // GIVEN: A date (Wednesday)
        val isoDate = "2025-11-24" // Wednesday

        // WHEN: Getting week end
        val weekEnd = DateUtils.getWeekEnd(isoDate)

        // THEN: Returns Sunday of that week
        assertEquals("2025-11-30", weekEnd) // Sunday
    }

    @Test
    fun getWeekDates_validStartDate_returns7Dates() {
        // GIVEN: A Monday date
        val startDate = "2025-11-18" // Monday

        // WHEN: Getting week dates
        val weekDates = DateUtils.getWeekDates(startDate)

        // THEN: Returns 7 dates
        assertEquals(7, weekDates.size)
        assertEquals("2025-11-18", weekDates[0]) // Monday
        assertEquals("2025-11-24", weekDates[6]) // Sunday
    }

    @Test
    fun getWeekDates_invalidDate_returnsEmptyList() {
        // GIVEN: Invalid date
        val startDate = "invalid"

        // WHEN: Getting week dates
        val weekDates = DateUtils.getWeekDates(startDate)

        // THEN: Returns empty list
        assertTrue(weekDates.isEmpty())
    }

    @Test
    fun isDateInRange_withinRange_returnsTrue() {
        // GIVEN: Date within range
        val date = "2025-11-24"
        val minDate = "2025-01-01"
        val maxDate = "2025-12-31"

        // WHEN: Checking range
        val isInRange = DateUtils.isDateInRange(date, minDate, maxDate)

        // THEN: Returns true
        assertTrue(isInRange)
    }

    @Test
    fun isDateInRange_beforeMin_returnsFalse() {
        // GIVEN: Date before minimum
        val date = "2024-12-31"
        val minDate = "2025-01-01"
        val maxDate = "2025-12-31"

        // WHEN: Checking range
        val isInRange = DateUtils.isDateInRange(date, minDate, maxDate)

        // THEN: Returns false
        assertFalse(isInRange)
    }

    @Test
    fun isDateInRange_afterMax_returnsFalse() {
        // GIVEN: Date after maximum
        val date = "2026-01-01"
        val minDate = "2025-01-01"
        val maxDate = "2025-12-31"

        // WHEN: Checking range
        val isInRange = DateUtils.isDateInRange(date, minDate, maxDate)

        // THEN: Returns false
        assertFalse(isInRange)
    }

    @Test
    fun isDateInRange_noConstraints_returnsTrue() {
        // GIVEN: Date with no constraints
        val date = "2025-11-24"

        // WHEN: Checking range with null constraints
        val isInRange = DateUtils.isDateInRange(date, null, null)

        // THEN: Returns true
        assertTrue(isInRange)
    }

    @Test
    fun parseTime_validTime_returnsHourMinute() {
        // GIVEN: Valid time string
        val timeString = "14:30"

        // WHEN: Parsed
        val time = DateUtils.parseTime(timeString)

        // THEN: Returns hour and minute
        assertNotNull(time)
        assertEquals(14, time!!.first)
        assertEquals(30, time.second)
    }

    @Test
    fun parseTime_nullTime_returnsNull() {
        // GIVEN: Null time
        val timeString: String? = null

        // WHEN: Parsed
        val time = DateUtils.parseTime(timeString)

        // THEN: Returns null
        assertNull(time)
    }

    @Test
    fun parseTime_invalidTime_returnsNull() {
        // GIVEN: Invalid time format
        val timeString = "invalid"

        // WHEN: Parsed
        val time = DateUtils.parseTime(timeString)

        // THEN: Returns null
        assertNull(time)
    }

    @Test
    fun parseTime_invalidHour_returnsNull() {
        // GIVEN: Invalid hour
        val timeString = "25:30"

        // WHEN: Parsed
        val time = DateUtils.parseTime(timeString)

        // THEN: Returns null
        assertNull(time)
    }

    @Test
    fun parseTime_invalidMinute_returnsNull() {
        // GIVEN: Invalid minute
        val timeString = "14:65"

        // WHEN: Parsed
        val time = DateUtils.parseTime(timeString)

        // THEN: Returns null
        assertNull(time)
    }

    @Test
    fun formatTime_validHourMinute_returnsFormatted() {
        // GIVEN: Valid hour and minute
        val hour = 14
        val minute = 30

        // WHEN: Formatted
        val formatted = DateUtils.formatTime(hour, minute)

        // THEN: Returns formatted time
        assertEquals("14:30", formatted)
    }

    @Test
    fun formatTime_singleDigits_addsPadding() {
        // GIVEN: Single digit hour and minute
        val hour = 9
        val minute = 5

        // WHEN: Formatted
        val formatted = DateUtils.formatTime(hour, minute)

        // THEN: Returns padded time
        assertEquals("09:05", formatted)
    }

    @Test
    fun calculateDuration_validTimes_returnsMinutes() {
        // GIVEN: Valid start and end times
        val startTime = "09:00"
        val endTime = "10:30"

        // WHEN: Calculating duration
        val duration = DateUtils.calculateDuration(startTime, endTime)

        // THEN: Returns duration in minutes
        assertEquals(90, duration)
    }

    @Test
    fun calculateDuration_invalidTimes_returnsNull() {
        // GIVEN: Invalid times
        val startTime = "invalid"
        val endTime = "10:30"

        // WHEN: Calculating duration
        val duration = DateUtils.calculateDuration(startTime, endTime)

        // THEN: Returns null
        assertNull(duration)
    }

    @Test
    fun calculateDuration_endBeforeStart_handlesOvernight() {
        // GIVEN: End time before start (overnight)
        val startTime = "23:00"
        val endTime = "01:00"

        // WHEN: Calculating duration
        val duration = DateUtils.calculateDuration(startTime, endTime)

        // THEN: Returns duration accounting for overnight
        assertNotNull(duration)
        assertTrue(duration!! > 0)
    }

    @Test
    fun getDayOfWeek_monday_returns1() {
        // GIVEN: A Monday date
        val isoDate = "2025-11-18" // Monday

        // WHEN: Getting day of week
        val dayOfWeek = DateUtils.getDayOfWeek(isoDate)

        // THEN: Returns 1 (Monday)
        assertEquals(1, dayOfWeek)
    }

    @Test
    fun getDayOfWeek_sunday_returns0() {
        // GIVEN: A Sunday date
        val isoDate = "2025-11-30" // Sunday

        // WHEN: Getting day of week
        val dayOfWeek = DateUtils.getDayOfWeek(isoDate)

        // THEN: Returns 0 (Sunday)
        assertEquals(0, dayOfWeek)
    }

    @Test
    fun formatDayOfWeek_allDays_returnShortNames() {
        // GIVEN: All days of week (0-6)
        val days = listOf(0, 1, 2, 3, 4, 5, 6)

        // WHEN: Formatting
        val names = days.map { DateUtils.formatDayOfWeek(it) }

        // THEN: Returns short day names
        assertEquals(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"), names)
    }

    @Test
    fun formatDayOfWeek_invalidDay_returnsEmpty() {
        // GIVEN: Invalid day
        val day = 7

        // WHEN: Formatting
        val name = DateUtils.formatDayOfWeek(day)

        // THEN: Returns empty string
        assertEquals("", name)
    }
}
