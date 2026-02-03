package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.flutter.material.data.*

/**
 * iOS SwiftUI Mappers for Flutter Calendar Parity Components
 *
 * This file maps cross-platform Flutter Material calendar components to iOS SwiftUI
 * bridge representations. The SwiftUI bridge models are consumed by Swift
 * code to render native iOS calendar UI.
 *
 * Architecture:
 * Flutter Calendar Component → iOS Mapper → SwiftUIView Bridge → Swift → Native SwiftUI DatePicker/Custom Calendar
 *
 * Components Implemented:
 * - Calendar: Base calendar component with date selection
 * - DateCalendar: Date-only calendar with month navigation
 * - MonthCalendar: Month picker with year/month display
 * - WeekCalendar: Week view with time slots and events
 * - EventCalendar: Calendar with event markers and event list
 *
 * iOS-specific features:
 * - Uses native iOS DatePicker for Calendar component
 * - Custom SwiftUI views for advanced calendar features
 * - Event markers as colored dots on calendar dates
 * - Week view with horizontal scrolling
 * - Accessibility support with VoiceOver announcements
 *
 * @since 3.0.0-flutter-parity-calendar
 */

/**
 * Maps Calendar component to SwiftUI DatePicker
 *
 * Uses iOS native DatePicker in graphical mode with:
 * - Date selection with min/max date constraints
 * - Disabled dates support (custom implementation)
 * - Material Design 3 visual styling where possible
 * - Full accessibility support
 *
 * SwiftUI DatePicker provides:
 * - Native iOS calendar appearance
 * - Automatic localization
 * - System integration (dark mode, dynamic type)
 * - VoiceOver support
 *
 * @param component Calendar component to render
 * @param theme Optional theme for styling
 * @param renderChild Callback to render child components (not used for Calendar)
 * @return SwiftUIView representing the calendar
 */
object CalendarMapper {
    fun map(
        component: Calendar,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val properties = buildMap<String, Any> {
            put("selectedDate", component.selectedDate ?: "")
            component.minDate?.let { put("minDate", it) }
            component.maxDate?.let { put("maxDate", it) }
            if (component.disabledDates.isNotEmpty()) {
                put("disabledDates", component.disabledDates)
            }
            component.contentDescription?.let { put("accessibilityLabel", it) }
        }

        return SwiftUIView(
            type = ViewType.Custom("CustomView"),
            id = component.id,
            properties = properties,
            modifiers = listOf(
                SwiftUIModifier.padding(16f)
            )
        )
    }
}

/**
 * Maps DateCalendar component to SwiftUI custom calendar view
 *
 * Creates a custom calendar with:
 * - Month grid with date cells
 * - Week number column (optional)
 * - Configurable first day of week
 * - Date selection highlighting
 * - Disabled dates support
 *
 * SwiftUI implementation uses:
 * - Custom Layout protocol for month grid
 * - LazyVGrid for efficient rendering
 * - Date formatting with Foundation DateFormatter
 *
 * @param component DateCalendar component to render
 * @param theme Optional theme for styling
 * @param renderChild Callback to render child components
 * @return SwiftUIView representing the date calendar
 */
object DateCalendarMapper {
    fun map(
        component: DateCalendar,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val properties = buildMap<String, Any> {
            put("selectedDate", component.selectedDate ?: "")
            component.minDate?.let { put("minDate", it) }
            component.maxDate?.let { put("maxDate", it) }
            if (component.disabledDates.isNotEmpty()) {
                put("disabledDates", component.disabledDates)
            }
            put("showWeekNumbers", component.showWeekNumbers)
            put("firstDayOfWeek", component.firstDayOfWeek)
            component.contentDescription?.let { put("accessibilityLabel", it) }
        }

        // Create month header with navigation
        val headerView = SwiftUIView.hStack(
            spacing = 8f,
            children = listOf(
                // Previous month button
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf("label" to ""),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "chevron.left"),
                            modifiers = listOf(SwiftUIModifier.fontSize(16f))
                        )
                    )
                ),
                // Month/Year label
                SwiftUIView.text(
                    content = "Month Year", // Will be set dynamically by Swift
                    modifiers = listOf(
                        SwiftUIModifier.fontWeight(FontWeight.Bold),
                        SwiftUIModifier.fillMaxWidth()
                    )
                ),
                // Next month button
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf("label" to ""),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "chevron.right"),
                            modifiers = listOf(SwiftUIModifier.fontSize(16f))
                        )
                    )
                )
            ),
            modifiers = listOf(SwiftUIModifier.padding(16f))
        )

        // Calendar grid (implemented in Swift)
        val calendarGrid = SwiftUIView(
            type = ViewType.Custom("CustomView"),
            properties = properties + mapOf("customType" to "DateCalendarGrid")
        )

        return SwiftUIView.vStack(
            spacing = 0f,
            children = listOf(headerView, calendarGrid),
            modifiers = listOf(
                SwiftUIModifier.background(SwiftUIColor.system("systemBackground")),
                SwiftUIModifier.cornerRadius(12f)
            )
        )
    }
}

/**
 * Maps MonthCalendar component to SwiftUI custom month picker
 *
 * Creates a month-focused calendar with:
 * - Single month display (year/month specified)
 * - Date grid for the specified month
 * - Highlighted dates (e.g., holidays)
 * - Month navigation callbacks
 *
 * SwiftUI implementation uses:
 * - Custom month grid layout
 * - Highlighted dates with special styling
 * - Month change navigation
 *
 * @param component MonthCalendar component to render
 * @param theme Optional theme for styling
 * @param renderChild Callback to render child components
 * @return SwiftUIView representing the month calendar
 */
object MonthCalendarMapper {
    fun map(
        component: MonthCalendar,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val properties = buildMap<String, Any> {
            put("year", component.year)
            put("month", component.month)
            put("selectedDate", component.selectedDate ?: "")
            if (component.disabledDates.isNotEmpty()) {
                put("disabledDates", component.disabledDates)
            }
            if (component.highlightedDates.isNotEmpty()) {
                put("highlightedDates", component.highlightedDates)
            }
            put("firstDayOfWeek", component.firstDayOfWeek)
            component.contentDescription?.let { put("accessibilityLabel", it) }
        }

        // Month header with year/month display
        val monthYearText = SwiftUIView.text(
            content = "${getMonthName(component.month)} ${component.year}",
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Title2),
                SwiftUIModifier.fontWeight(FontWeight.Bold)
            )
        )

        val headerView = SwiftUIView.hStack(
            spacing = 8f,
            children = listOf(
                // Previous month button
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf("label" to ""),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "chevron.left"),
                            modifiers = listOf(SwiftUIModifier.fontSize(16f))
                        )
                    )
                ),
                monthYearText,
                SwiftUIView(
                    type = ViewType.Spacer,
                    properties = emptyMap()
                ),
                // Next month button
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf("label" to ""),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "chevron.right"),
                            modifiers = listOf(SwiftUIModifier.fontSize(16f))
                        )
                    )
                )
            ),
            modifiers = listOf(SwiftUIModifier.padding(16f))
        )

        // Month calendar grid
        val calendarGrid = SwiftUIView(
            type = ViewType.Custom("CustomView"),
            properties = properties + mapOf("customType" to "MonthCalendarGrid")
        )

        return SwiftUIView.vStack(
            spacing = 0f,
            children = listOf(headerView, calendarGrid),
            modifiers = listOf(
                SwiftUIModifier.background(SwiftUIColor.system("systemBackground")),
                SwiftUIModifier.cornerRadius(12f)
            )
        )
    }

    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> "Unknown"
        }
    }
}

/**
 * Maps WeekCalendar component to SwiftUI custom week view
 *
 * Creates a week view calendar with:
 * - Horizontal week header (Mon-Sun)
 * - Time slots grid (hourly)
 * - Event blocks positioned by time
 * - Event click and time slot click handlers
 *
 * SwiftUI implementation uses:
 * - ScrollView with time slot grid
 * - Custom event positioning based on start/end times
 * - Drag gesture for event creation
 * - Color-coded events
 *
 * @param component WeekCalendar component to render
 * @param theme Optional theme for styling
 * @param renderChild Callback to render child components
 * @return SwiftUIView representing the week calendar
 */
object WeekCalendarMapper {
    fun map(
        component: WeekCalendar,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val properties = buildMap<String, Any> {
            put("startDate", component.startDate)
            put("selectedDate", component.selectedDate ?: "")
            put("showTimeSlots", component.showTimeSlots)
            put("timeSlotHeight", component.timeSlotHeight)
            put("startHour", component.startHour)
            put("endHour", component.endHour)

            // Convert events to serializable format
            val eventsData = component.events.map { weekEvent ->
                mapOf(
                    "id" to weekEvent.id,
                    "date" to weekEvent.date,
                    "startTime" to weekEvent.startTime,
                    "endTime" to weekEvent.endTime,
                    "title" to weekEvent.title,
                    "description" to (weekEvent.description ?: ""),
                    "color" to (weekEvent.color ?: "#2196F3"),
                    "location" to (weekEvent.location ?: "")
                )
            }
            put("events", eventsData)

            component.contentDescription?.let { put("accessibilityLabel", it) }
        }

        // Week header with day names
        val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayHeaders = weekDays.map { dayName ->
            SwiftUIView.text(
                content = dayName,
                modifiers = listOf(
                    SwiftUIModifier.fontSize(14f),
                    SwiftUIModifier.fontWeight(FontWeight.Semibold),
                    SwiftUIModifier.fillMaxWidth()
                )
            )
        }

        val weekHeader = SwiftUIView.hStack(
            spacing = 4f,
            children = dayHeaders,
            modifiers = listOf(
                SwiftUIModifier.padding(8f),
                SwiftUIModifier.background(SwiftUIColor.system("systemGray6"))
            )
        )

        // Week calendar grid (time slots + events)
        val weekGrid = SwiftUIView(
            type = ViewType.Custom("CustomView"),
            properties = properties + mapOf("customType" to "WeekCalendarGrid")
        )

        return SwiftUIView.vStack(
            spacing = 0f,
            children = listOf(weekHeader, weekGrid),
            modifiers = listOf(
                SwiftUIModifier.background(SwiftUIColor.system("systemBackground")),
                SwiftUIModifier.cornerRadius(12f)
            )
        )
    }
}

/**
 * Maps EventCalendar component to SwiftUI calendar with event list
 *
 * Creates a comprehensive calendar with:
 * - Month calendar view with event dots
 * - Event list for selected date
 * - Event markers (up to maxVisibleEvents per date)
 * - Add event button
 *
 * SwiftUI implementation uses:
 * - DatePicker or custom month grid
 * - Event dots overlay on calendar dates
 * - List view for events on selected date
 * - Material Design 3 styling for event cards
 *
 * @param component EventCalendar component to render
 * @param theme Optional theme for styling
 * @param renderChild Callback to render child components
 * @return SwiftUIView representing the event calendar
 */
object EventCalendarMapper {
    fun map(
        component: EventCalendar,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val properties = buildMap<String, Any> {
            put("selectedDate", component.selectedDate ?: "")
            component.minDate?.let { put("minDate", it) }
            component.maxDate?.let { put("maxDate", it) }
            put("showEventList", component.showEventList)
            put("maxVisibleEvents", component.maxVisibleEvents)

            // Convert events to serializable format
            val eventsData = component.events.map { calendarEvent ->
                mapOf(
                    "id" to calendarEvent.id,
                    "date" to calendarEvent.date,
                    "title" to calendarEvent.title,
                    "description" to (calendarEvent.description ?: ""),
                    "color" to (calendarEvent.color ?: "#2196F3"),
                    "allDay" to calendarEvent.allDay,
                    "startTime" to (calendarEvent.startTime ?: ""),
                    "endTime" to (calendarEvent.endTime ?: ""),
                    "location" to (calendarEvent.location ?: ""),
                    "attendees" to calendarEvent.attendees
                )
            }
            put("events", eventsData)

            component.contentDescription?.let { put("accessibilityLabel", it) }
        }

        // Calendar with event markers
        val calendarView = SwiftUIView(
            type = ViewType.Custom("CustomView"),
            properties = properties + mapOf("customType" to "EventCalendarGrid")
        )

        // Event list for selected date (if enabled)
        val eventListView = if (component.showEventList) {
            val selectedEvents = component.events.filter { calEvent -> calEvent.date == component.selectedDate }

            if (selectedEvents.isEmpty()) {
                // Empty state
                SwiftUIView.vStack(
                    spacing = 8f,
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "calendar.badge.clock"),
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(48f),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemGray3"))
                            )
                        ),
                        SwiftUIView.text(
                            content = "No events for this date",
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(16f),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemGray"))
                            )
                        )
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.padding(32f),
                        SwiftUIModifier.fillMaxWidth()
                    )
                )
            } else {
                // Event cards
                val eventCards = selectedEvents.map { selectedEvent ->
                    createEventCard(selectedEvent, theme)
                }

                SwiftUIView(
                    type = ViewType.ScrollView,
                    properties = mapOf("axis" to "vertical"),
                    children = listOf(
                        SwiftUIView.vStack(
                            spacing = 12f,
                            children = eventCards,
                            modifiers = listOf(SwiftUIModifier.padding(16f))
                        )
                    )
                )
            }
        } else {
            null
        }

        // Add event button (floating action button style)
        val addEventButton = if (component.onAddEvent != null) {
            SwiftUIView(
                type = ViewType.Button,
                properties = mapOf(
                    "label" to "",
                    "action" to "addEvent"
                ),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to "plus"),
                        modifiers = listOf(
                            SwiftUIModifier.fontSize(20f),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.white)
                        )
                    )
                ),
                modifiers = listOf(
                    SwiftUIModifier.padding(16f),
                    SwiftUIModifier.background(SwiftUIColor.primary),
                    SwiftUIModifier.cornerRadius(28f),
                    SwiftUIModifier.shadow(radius = 4f, x = 0f, y = 2f)
                )
            )
        } else {
            null
        }

        // Combine calendar and event list
        val children = mutableListOf<SwiftUIView>()
        children.add(calendarView)

        if (eventListView != null) {
            children.add(
                SwiftUIView(
                    type = ViewType.Divider,
                    properties = emptyMap()
                )
            )
            children.add(eventListView)
        }

        val mainContent = SwiftUIView.vStack(
            spacing = 0f,
            children = children,
            modifiers = listOf(
                SwiftUIModifier.background(SwiftUIColor.system("systemBackground"))
            )
        )

        // Wrap in ZStack if add button exists
        return if (addEventButton != null) {
            SwiftUIView.zStack(
                alignment = ZStackAlignment.BottomTrailing,
                children = listOf(
                    mainContent,
                    addEventButton
                ),
                modifiers = listOf(SwiftUIModifier.cornerRadius(12f))
            )
        } else {
            SwiftUIView(
                type = ViewType.Group,
                properties = emptyMap(),
                children = listOf(mainContent),
                modifiers = listOf(SwiftUIModifier.cornerRadius(12f))
            )
        }
    }

    /**
     * Creates an event card view for the event list
     */
    private fun createEventCard(event: EventCalendar.CalendarEvent, theme: Theme?): SwiftUIView {
        val timeText = if (event.allDay) {
            "All Day"
        } else {
            "${event.startTime ?: ""} - ${event.endTime ?: ""}"
        }

        // Event color indicator
        val colorIndicator = SwiftUIView(
            type = ViewType.RoundedRectangle,
            properties = mapOf("cornerRadius" to 2f),
            modifiers = listOf(
                SwiftUIModifier.fillMaxHeight(),
                SwiftUIModifier.frame(width = SizeValue.Fixed(4f)),
                SwiftUIModifier.foregroundColor(
                    parseHexColor(event.color ?: "#2196F3")
                )
            )
        )

        // Event details
        val titleText = SwiftUIView.text(
            content = event.title,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Headline),
                SwiftUIModifier.fontWeight(FontWeight.Semibold)
            )
        )

        val timeView = SwiftUIView.hStack(
            spacing = 4f,
            children = listOf(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to "clock"),
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(12f),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemGray"))
                    )
                ),
                SwiftUIView.text(
                    content = timeText,
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(14f),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemGray"))
                    )
                )
            )
        )

        val detailsColumn = mutableListOf(titleText, timeView)

        // Add location if present
        val eventLocation = event.location
        if (!eventLocation.isNullOrEmpty()) {
            detailsColumn.add(
                SwiftUIView.hStack(
                    spacing = 4f,
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "location"),
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(12f),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemGray"))
                            )
                        ),
                        SwiftUIView.text(
                            content = eventLocation,
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(14f),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemGray"))
                            )
                        )
                    )
                )
            )
        }

        // Add description if present
        val eventDescription = event.description
        if (!eventDescription.isNullOrEmpty()) {
            detailsColumn.add(
                SwiftUIView.text(
                    content = eventDescription,
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(14f),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel")),
                        SwiftUIModifier(ModifierType.LineLimit, 2)
                    )
                )
            )
        }

        val eventDetails = SwiftUIView.vStack(
            spacing = 4f,
            alignment = HorizontalAlignment.Leading,
            children = detailsColumn,
            modifiers = listOf(SwiftUIModifier.fillMaxWidth())
        )

        // Card content
        val cardContent = SwiftUIView.hStack(
            spacing = 12f,
            alignment = VerticalAlignment.Top,
            children = listOf(colorIndicator, eventDetails)
        )

        return SwiftUIView(
            type = ViewType.Custom("CustomView"),
            id = event.id,
            properties = mapOf(
                "customType" to "EventCard",
                "eventId" to event.id,
                "title" to event.title
            ),
            children = listOf(cardContent),
            modifiers = listOf(
                SwiftUIModifier.padding(12f),
                SwiftUIModifier.background(SwiftUIColor.system("secondarySystemBackground")),
                SwiftUIModifier.cornerRadius(8f)
            )
        )
    }

    /**
     * Parses hex color string to SwiftUIColor
     */
    private fun parseHexColor(hex: String): SwiftUIColor {
        val cleanHex = hex.removePrefix("#")
        if (cleanHex.length != 6) {
            return SwiftUIColor.primary
        }

        return try {
            val r = cleanHex.substring(0, 2).toInt(16) / 255f
            val g = cleanHex.substring(2, 4).toInt(16) / 255f
            val b = cleanHex.substring(4, 6).toInt(16) / 255f
            SwiftUIColor.rgb(r, g, b)
        } catch (e: Exception) {
            SwiftUIColor.primary
        }
    }
}
