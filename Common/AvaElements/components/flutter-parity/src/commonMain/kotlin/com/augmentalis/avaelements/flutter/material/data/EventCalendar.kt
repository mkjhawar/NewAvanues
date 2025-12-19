package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * EventCalendar component for calendar with event markers and event list.
 *
 * A comprehensive calendar component that displays a month view with event markers
 * and a synchronized event list for the selected date. Perfect for event
 * management and scheduling applications.
 *
 * **Material Design 3 Implementation:**
 * - Uses Material 3 DatePicker with custom event markers
 * - Material 3 ListItem for event list
 * - Supports dynamic colors
 * - Follows Material Design guidelines
 *
 * **Accessibility:**
 * - Full TalkBack support with date and event announcements
 * - Event count announcements per date
 * - Keyboard navigation support
 * - WCAG 2.1 Level AA compliant
 *
 * @property selectedDate Currently selected date in ISO 8601 format (YYYY-MM-DD), or null if no date selected
 * @property events List of events to display with markers on the calendar
 * @property minDate Minimum selectable date in ISO 8601 format, or null for no minimum
 * @property maxDate Maximum selectable date in ISO 8601 format, or null for no maximum
 * @property showEventList Whether to show the event list below the calendar
 * @property maxVisibleEvents Maximum number of event markers to show per date (default 3)
 * @property onDateSelected Callback invoked when a date is selected, receives ISO 8601 date string
 * @property onEventClick Callback invoked when an event is clicked, receives event ID
 * @property onAddEvent Callback invoked when user requests to add an event, receives selected date
 * @property contentDescription Accessibility label for the calendar
 *
 * **Example:**
 * ```kotlin
 * EventCalendar(
 *     selectedDate = "2025-11-24",
 *     events = listOf(
 *         EventCalendar.CalendarEvent(
 *             id = "1",
 *             date = "2025-11-24",
 *             title = "Team Meeting",
 *             color = "#2196F3",
 *             allDay = false
 *         ),
 *         EventCalendar.CalendarEvent(
 *             id = "2",
 *             date = "2025-11-24",
 *             title = "Birthday Party",
 *             color = "#4CAF50",
 *             allDay = true
 *         )
 *     ),
 *     showEventList = true,
 *     maxVisibleEvents = 3,
 *     onEventClick = { eventId -> println("Event: $eventId") },
 *     onAddEvent = { date -> println("Add event on $date") }
 * )
 * ```
 */
data class EventCalendar(
    override val type: String = "EventCalendar",
    override val id: String? = null,
    val selectedDate: String? = null,
    val events: List<CalendarEvent> = emptyList(),
    val minDate: String? = null,
    val maxDate: String? = null,
    val showEventList: Boolean = true,
    val maxVisibleEvents: Int = 3,
    @Transient
    val onDateSelected: ((String) -> Unit)? = null,
    @Transient
    val onEventClick: ((String) -> Unit)? = null,
    @Transient
    val onAddEvent: ((String) -> Unit)? = null,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
    /**
     * Represents an event in the calendar.
     *
     * @property id Unique identifier for the event
     * @property date Date of the event in ISO 8601 format (YYYY-MM-DD)
     * @property title Event title
     * @property description Optional event description
     * @property color Optional color for the event marker (hex format #RRGGBB)
     * @property allDay Whether this is an all-day event
     * @property startTime Start time for timed events (HH:mm format, 24-hour)
     * @property endTime End time for timed events (HH:mm format, 24-hour)
     * @property location Optional event location
     * @property attendees Optional list of attendee names
     */
    data class CalendarEvent(
        val id: String,
        val date: String, // ISO 8601
        val title: String,
        val description: String? = null,
        val color: String? = null,
        val allDay: Boolean = true,
        val startTime: String? = null, // HH:mm (24-hour)
        val endTime: String? = null, // HH:mm (24-hour)
        val location: String? = null,
        val attendees: List<String> = emptyList()
    )
}
