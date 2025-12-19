package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * WeekCalendar component for week view calendar with time slots.
 *
 * Displays a week view calendar with support for events and time slots.
 * Ideal for scheduling applications and weekly planners.
 *
 * **Material Design 3 Implementation:**
 * - Custom week view using Material 3 components
 * - Supports dynamic colors
 * - Follows Material Design guidelines
 *
 * **Accessibility:**
 * - Full TalkBack support with day and event announcements
 * - Keyboard navigation support
 * - WCAG 2.1 Level AA compliant
 *
 * @property startDate The start date of the week (Monday) in ISO 8601 format (YYYY-MM-DD)
 * @property selectedDate Currently selected date in ISO 8601 format, or null if no date selected
 * @property events List of events to display in the week view
 * @property showTimeSlots Whether to display time slots (hourly grid)
 * @property timeSlotHeight Height of each time slot in dp (default 60dp = 1 hour)
 * @property startHour Starting hour for time slots (0-23, default 0)
 * @property endHour Ending hour for time slots (0-23, default 23)
 * @property onDateSelected Callback invoked when a date is selected, receives ISO 8601 date string
 * @property onEventClick Callback invoked when an event is clicked, receives event ID
 * @property onTimeSlotClick Callback invoked when a time slot is clicked, receives date and hour
 * @property contentDescription Accessibility label for the calendar
 *
 * **Example:**
 * ```kotlin
 * WeekCalendar(
 *     startDate = "2025-11-18", // Monday
 *     selectedDate = "2025-11-24",
 *     events = listOf(
 *         WeekCalendar.CalendarEvent(
 *             id = "1",
 *             date = "2025-11-24",
 *             startTime = "09:00",
 *             endTime = "10:30",
 *             title = "Team Meeting"
 *         )
 *     ),
 *     showTimeSlots = true,
 *     startHour = 8,
 *     endHour = 18,
 *     onEventClick = { eventId -> println("Event clicked: $eventId") }
 * )
 * ```
 */
data class WeekCalendar(
    override val type: String = "WeekCalendar",
    override val id: String? = null,
    val startDate: String, // ISO 8601 (Monday of week)
    val selectedDate: String? = null,
    val events: List<CalendarEvent> = emptyList(),
    val showTimeSlots: Boolean = true,
    val timeSlotHeight: Float = 60f, // dp per hour
    val startHour: Int = 0, // 0-23
    val endHour: Int = 23, // 0-23
    @Transient
    val onDateSelected: ((String) -> Unit)? = null,
    @Transient
    val onEventClick: ((String) -> Unit)? = null,
    @Transient
    val onTimeSlotClick: ((date: String, hour: Int) -> Unit)? = null,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
    /**
     * Represents an event in the week calendar.
     *
     * @property id Unique identifier for the event
     * @property date Date of the event in ISO 8601 format (YYYY-MM-DD)
     * @property startTime Start time in HH:mm format (24-hour)
     * @property endTime End time in HH:mm format (24-hour)
     * @property title Event title
     * @property description Optional event description
     * @property color Optional color for the event (hex format #RRGGBB)
     * @property location Optional event location
     */
    data class CalendarEvent(
        val id: String,
        val date: String, // ISO 8601
        val startTime: String, // HH:mm (24-hour)
        val endTime: String, // HH:mm (24-hour)
        val title: String,
        val description: String? = null,
        val color: String? = null,
        val location: String? = null
    )
}
