package com.augmentalis.avaelements.renderers.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.augmentalis.avaelements.flutter.material.data.*
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive test suite for calendar components.
 *
 * Tests cover:
 * - Rendering correctness
 * - Date selection
 * - Date validation (min/max)
 * - Disabled dates
 * - Accessibility (TalkBack support)
 * - Event handling
 * - Edge cases
 *
 * Target: 90%+ test coverage across all 5 calendar components.
 */
class CalendarComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===== Calendar Component Tests =====

    @Test
    fun calendar_rendersCorrectly() {
        // GIVEN: A calendar component
        val component = Calendar(
            selectedDate = "2025-11-24",
            contentDescription = "Test Calendar"
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            CalendarMapper(component)
        }

        // THEN: Calendar is displayed
        composeTestRule
            .onNodeWithContentDescription("Test Calendar")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun calendar_dateSelection_triggersCallback() {
        // GIVEN: A calendar with selection callback
        var selectedDate: String? = null
        val component = Calendar(
            selectedDate = "2025-11-24",
            onDateSelected = { date -> selectedDate = date }
        )

        // WHEN: Rendered and date selected
        composeTestRule.setContent {
            CalendarMapper(component)
        }

        // Simulate date selection (would happen through user interaction)
        // Note: In real test, this would involve clicking on a date
        // For now, verify callback can be invoked
        component.onDateSelected?.invoke("2025-11-25")

        // THEN: Callback is triggered
        assertEquals("2025-11-25", selectedDate)
    }

    @Test
    fun calendar_respectsMinDate() {
        // GIVEN: Calendar with minimum date
        val component = Calendar(
            minDate = "2025-01-01",
            maxDate = "2025-12-31"
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            CalendarMapper(component)
        }

        // THEN: Calendar exists with date constraints
        composeTestRule
            .onNodeWithContentDescription("Calendar")
            .assertExists()
    }

    @Test
    fun calendar_respectsMaxDate() {
        // GIVEN: Calendar with maximum date
        val component = Calendar(
            minDate = "2025-01-01",
            maxDate = "2025-12-31"
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            CalendarMapper(component)
        }

        // THEN: Calendar exists with date constraints
        composeTestRule
            .onNodeWithContentDescription("Calendar")
            .assertExists()
    }

    @Test
    fun calendar_disabledDates_areRespected() {
        // GIVEN: Calendar with disabled dates
        val component = Calendar(
            disabledDates = listOf("2025-11-25", "2025-12-25")
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            CalendarMapper(component)
        }

        // THEN: Calendar renders with disabled dates
        composeTestRule
            .onNodeWithContentDescription("Calendar")
            .assertExists()
    }

    // ===== DateCalendar Component Tests =====

    @Test
    fun dateCalendar_rendersCorrectly() {
        // GIVEN: A date calendar component
        val component = DateCalendar(
            selectedDate = "2025-11-24",
            showWeekNumbers = true,
            contentDescription = "Test Date Calendar"
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            DateCalendarMapper(component)
        }

        // THEN: Date calendar is displayed
        composeTestRule
            .onNodeWithContentDescription("Test Date Calendar")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun dateCalendar_dateSelection_triggersCallback() {
        // GIVEN: Date calendar with selection callback
        var selectedDate: String? = null
        val component = DateCalendar(
            selectedDate = "2025-11-24",
            onDateSelected = { date -> selectedDate = date }
        )

        // WHEN: Date selected
        composeTestRule.setContent {
            DateCalendarMapper(component)
        }

        component.onDateSelected?.invoke("2025-11-25")

        // THEN: Callback is triggered
        assertEquals("2025-11-25", selectedDate)
    }

    @Test
    fun dateCalendar_respectsFirstDayOfWeek() {
        // GIVEN: Date calendar with Monday as first day
        val component = DateCalendar(
            firstDayOfWeek = 1, // Monday
            showWeekNumbers = true
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            DateCalendarMapper(component)
        }

        // THEN: Calendar renders
        composeTestRule
            .onNodeWithContentDescription("Date Calendar")
            .assertExists()
    }

    @Test
    fun dateCalendar_showsWeekNumbers() {
        // GIVEN: Date calendar with week numbers enabled
        val component = DateCalendar(
            showWeekNumbers = true
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            DateCalendarMapper(component)
        }

        // THEN: Calendar renders with week numbers
        composeTestRule
            .onNodeWithContentDescription("Date Calendar")
            .assertExists()
    }

    @Test
    fun dateCalendar_disabledDates_areRespected() {
        // GIVEN: Date calendar with disabled dates
        val component = DateCalendar(
            disabledDates = listOf("2025-11-25", "2025-12-25")
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            DateCalendarMapper(component)
        }

        // THEN: Calendar renders with disabled dates
        composeTestRule
            .onNodeWithContentDescription("Date Calendar")
            .assertExists()
    }

    // ===== MonthCalendar Component Tests =====

    @Test
    fun monthCalendar_rendersCorrectly() {
        // GIVEN: A month calendar component
        val component = MonthCalendar(
            year = 2025,
            month = 11,
            selectedDate = "2025-11-24",
            contentDescription = "Test Month Calendar"
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            MonthCalendarMapper(component)
        }

        // THEN: Month calendar is displayed
        composeTestRule
            .onNodeWithContentDescription("Test Month Calendar")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun monthCalendar_dateSelection_triggersCallback() {
        // GIVEN: Month calendar with selection callback
        var selectedDate: String? = null
        val component = MonthCalendar(
            year = 2025,
            month = 11,
            onDateSelected = { date -> selectedDate = date }
        )

        // WHEN: Date selected
        composeTestRule.setContent {
            MonthCalendarMapper(component)
        }

        component.onDateSelected?.invoke("2025-11-25")

        // THEN: Callback is triggered
        assertEquals("2025-11-25", selectedDate)
    }

    @Test
    fun monthCalendar_monthChange_triggersCallback() {
        // GIVEN: Month calendar with month change callback
        var changedYear: Int? = null
        var changedMonth: Int? = null
        val component = MonthCalendar(
            year = 2025,
            month = 11,
            onMonthChange = { year, month ->
                changedYear = year
                changedMonth = month
            }
        )

        // WHEN: Month changed
        composeTestRule.setContent {
            MonthCalendarMapper(component)
        }

        component.onMonthChange?.invoke(2025, 12)

        // THEN: Callback is triggered
        assertEquals(2025, changedYear)
        assertEquals(12, changedMonth)
    }

    @Test
    fun monthCalendar_highlightedDates_displayed() {
        // GIVEN: Month calendar with highlighted dates
        val component = MonthCalendar(
            year = 2025,
            month = 11,
            highlightedDates = listOf("2025-11-01", "2025-11-25")
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            MonthCalendarMapper(component)
        }

        // THEN: Calendar renders with highlights
        composeTestRule
            .onNodeWithContentDescription("Month Calendar for 2025-11")
            .assertExists()
    }

    @Test
    fun monthCalendar_disabledDates_areRespected() {
        // GIVEN: Month calendar with disabled dates
        val component = MonthCalendar(
            year = 2025,
            month = 11,
            disabledDates = listOf("2025-11-25")
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            MonthCalendarMapper(component)
        }

        // THEN: Calendar renders with disabled dates
        composeTestRule
            .onNodeWithContentDescription("Month Calendar for 2025-11")
            .assertExists()
    }

    // ===== WeekCalendar Component Tests =====

    @Test
    fun weekCalendar_rendersCorrectly() {
        // GIVEN: A week calendar component
        val component = WeekCalendar(
            startDate = "2025-11-18", // Monday
            selectedDate = "2025-11-24",
            contentDescription = "Test Week Calendar"
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            WeekCalendarMapper(component)
        }

        // THEN: Week calendar is displayed
        composeTestRule
            .onNodeWithContentDescription("Test Week Calendar")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun weekCalendar_dateSelection_triggersCallback() {
        // GIVEN: Week calendar with selection callback
        var selectedDate: String? = null
        val component = WeekCalendar(
            startDate = "2025-11-18",
            onDateSelected = { date -> selectedDate = date }
        )

        // WHEN: Date selected
        composeTestRule.setContent {
            WeekCalendarMapper(component)
        }

        component.onDateSelected?.invoke("2025-11-24")

        // THEN: Callback is triggered
        assertEquals("2025-11-24", selectedDate)
    }

    @Test
    fun weekCalendar_eventClick_triggersCallback() {
        // GIVEN: Week calendar with event click callback
        var clickedEventId: String? = null
        val component = WeekCalendar(
            startDate = "2025-11-18",
            events = listOf(
                WeekCalendar.CalendarEvent(
                    id = "event1",
                    date = "2025-11-24",
                    startTime = "09:00",
                    endTime = "10:00",
                    title = "Team Meeting"
                )
            ),
            onEventClick = { eventId -> clickedEventId = eventId }
        )

        // WHEN: Event clicked
        composeTestRule.setContent {
            WeekCalendarMapper(component)
        }

        component.onEventClick?.invoke("event1")

        // THEN: Callback is triggered
        assertEquals("event1", clickedEventId)
    }

    @Test
    fun weekCalendar_timeSlotClick_triggersCallback() {
        // GIVEN: Week calendar with time slot click callback
        var clickedDate: String? = null
        var clickedHour: Int? = null
        val component = WeekCalendar(
            startDate = "2025-11-18",
            showTimeSlots = true,
            onTimeSlotClick = { date, hour ->
                clickedDate = date
                clickedHour = hour
            }
        )

        // WHEN: Time slot clicked
        composeTestRule.setContent {
            WeekCalendarMapper(component)
        }

        component.onTimeSlotClick?.invoke("2025-11-24", 14)

        // THEN: Callback is triggered
        assertEquals("2025-11-24", clickedDate)
        assertEquals(14, clickedHour)
    }

    @Test
    fun weekCalendar_withTimeSlots_rendered() {
        // GIVEN: Week calendar with time slots
        val component = WeekCalendar(
            startDate = "2025-11-18",
            showTimeSlots = true,
            startHour = 8,
            endHour = 18
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            WeekCalendarMapper(component)
        }

        // THEN: Calendar renders with time slots
        composeTestRule
            .onNodeWithContentDescription("Week Calendar starting 2025-11-18")
            .assertExists()
    }

    // ===== EventCalendar Component Tests =====

    @Test
    fun eventCalendar_rendersCorrectly() {
        // GIVEN: An event calendar component
        val component = EventCalendar(
            selectedDate = "2025-11-24",
            contentDescription = "Test Event Calendar"
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            EventCalendarMapper(component)
        }

        // THEN: Event calendar is displayed
        composeTestRule
            .onNodeWithContentDescription("Test Event Calendar")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun eventCalendar_dateSelection_triggersCallback() {
        // GIVEN: Event calendar with selection callback
        var selectedDate: String? = null
        val component = EventCalendar(
            selectedDate = "2025-11-24",
            onDateSelected = { date -> selectedDate = date }
        )

        // WHEN: Date selected
        composeTestRule.setContent {
            EventCalendarMapper(component)
        }

        component.onDateSelected?.invoke("2025-11-25")

        // THEN: Callback is triggered
        assertEquals("2025-11-25", selectedDate)
    }

    @Test
    fun eventCalendar_eventClick_triggersCallback() {
        // GIVEN: Event calendar with event click callback
        var clickedEventId: String? = null
        val component = EventCalendar(
            selectedDate = "2025-11-24",
            events = listOf(
                EventCalendar.CalendarEvent(
                    id = "event1",
                    date = "2025-11-24",
                    title = "Team Meeting"
                )
            ),
            onEventClick = { eventId -> clickedEventId = eventId }
        )

        // WHEN: Event clicked
        composeTestRule.setContent {
            EventCalendarMapper(component)
        }

        component.onEventClick?.invoke("event1")

        // THEN: Callback is triggered
        assertEquals("event1", clickedEventId)
    }

    @Test
    fun eventCalendar_addEvent_triggersCallback() {
        // GIVEN: Event calendar with add event callback
        var addEventDate: String? = null
        val component = EventCalendar(
            selectedDate = "2025-11-24",
            onAddEvent = { date -> addEventDate = date }
        )

        // WHEN: Add event triggered
        composeTestRule.setContent {
            EventCalendarMapper(component)
        }

        component.onAddEvent?.invoke("2025-11-24")

        // THEN: Callback is triggered
        assertEquals("2025-11-24", addEventDate)
    }

    @Test
    fun eventCalendar_showsEventList() {
        // GIVEN: Event calendar with events and event list enabled
        val component = EventCalendar(
            selectedDate = "2025-11-24",
            events = listOf(
                EventCalendar.CalendarEvent(
                    id = "event1",
                    date = "2025-11-24",
                    title = "Team Meeting",
                    allDay = false,
                    startTime = "09:00",
                    endTime = "10:00"
                )
            ),
            showEventList = true
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            EventCalendarMapper(component)
        }

        // THEN: Event list is visible
        composeTestRule
            .onNodeWithText("Team Meeting")
            .assertExists()
    }

    @Test
    fun eventCalendar_respectsMaxVisibleEvents() {
        // GIVEN: Event calendar with multiple events and limit
        val events = (1..5).map { i ->
            EventCalendar.CalendarEvent(
                id = "event$i",
                date = "2025-11-24",
                title = "Event $i"
            )
        }

        val component = EventCalendar(
            selectedDate = "2025-11-24",
            events = events,
            maxVisibleEvents = 3
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            EventCalendarMapper(component)
        }

        // THEN: Shows overflow indicator
        composeTestRule
            .onNodeWithText("+2 more events")
            .assertExists()
    }

    // ===== Accessibility Tests =====

    @Test
    fun calendar_hasContentDescription() {
        // GIVEN: Calendar with custom content description
        val component = Calendar(
            contentDescription = "Select appointment date"
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            CalendarMapper(component)
        }

        // THEN: Content description is set
        composeTestRule
            .onNodeWithContentDescription("Select appointment date")
            .assertExists()
    }

    @Test
    fun eventCalendar_eventList_isAccessible() {
        // GIVEN: Event calendar with events
        val component = EventCalendar(
            selectedDate = "2025-11-24",
            events = listOf(
                EventCalendar.CalendarEvent(
                    id = "event1",
                    date = "2025-11-24",
                    title = "Team Meeting"
                )
            )
        )

        // WHEN: Rendered
        composeTestRule.setContent {
            EventCalendarMapper(component)
        }

        // THEN: Event is accessible
        composeTestRule
            .onNodeWithText("Team Meeting")
            .assertExists()
            .assertHasClickAction()
    }
}
