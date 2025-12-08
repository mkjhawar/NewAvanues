package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.flutter.material.data.*
import com.augmentalis.avaelements.renderer.ios.bridge.*

/**
 * Flutter Material Calendar Component Mappers for iOS SwiftUI
 *
 * Maps AvaElements Flutter-parity calendar components to SwiftUI equivalents.
 *
 * Components:
 * - Calendar → DatePicker with full calendar view
 * - DateCalendar → DatePicker with date selection
 * - MonthCalendar → Custom month grid view
 * - WeekCalendar → Custom week grid view
 * - EventCalendar → Calendar with event indicators
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================
// CALENDAR
// ============================================

object CalendarMapper {
    fun map(component: Calendar, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Date picker with calendar style
        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf("datePickerStyle" to "graphical")))

        // Padding
        modifiers.add(SwiftUIModifier.padding(16f))

        return SwiftUIView(
            type = ViewType.Custom("DatePicker"),
            properties = mapOf(
                "selection" to (component.selectedDate ?: ""),
                "displayedComponents" to "date"
            ),
            modifiers = modifiers,
        )
    }
}

// ============================================
// DATE CALENDAR
// ============================================

object DateCalendarMapper {
    fun map(component: DateCalendar, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Date picker with calendar style
        modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf("datePickerStyle" to "graphical")))

        // Min/max date constraints
        component.minDate?.let { minDate ->
            modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf("datePickerRange" to mapOf("start" to minDate))))
        }

        component.maxDate?.let { maxDate ->
            modifiers.add(SwiftUIModifier(ModifierType.Custom, mapOf("datePickerRange" to mapOf("end" to maxDate))))
        }

        // Padding
        modifiers.add(SwiftUIModifier.padding(16f))

        return SwiftUIView(
            type = ViewType.Custom("DatePicker"),
            properties = mapOf(
                "selection" to (component.selectedDate ?: ""),
                "displayedComponents" to "date"
            ),
            modifiers = modifiers,
        )
    }
}

// ============================================
// MONTH CALENDAR
// ============================================

object MonthCalendarMapper {
    fun map(component: MonthCalendar, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Month header
        val monthNames = listOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        val monthName = monthNames.getOrNull(component.month - 1) ?: ""

        val header = SwiftUIView(
            type = ViewType.HStack,
            properties = emptyMap(),
            children = listOf(
                SwiftUIView(
                    type = ViewType.Custom("Button"),
                    properties = mapOf("systemImage" to "chevron.left"),
                    modifiers = listOf(SwiftUIModifier(ModifierType.Custom, mapOf("buttonStyle" to "borderless")))
                ),
                SwiftUIView(
                    type = ViewType.Custom("Spacer"),
                    properties = emptyMap()
                ),
                SwiftUIView.text(
                    content = "$monthName ${component.year}",
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Headline),
                        SwiftUIModifier.fontWeight(FontWeight.Semibold)
                    )
                ),
                SwiftUIView(
                    type = ViewType.Custom("Spacer"),
                    properties = emptyMap()
                ),
                SwiftUIView(
                    type = ViewType.Custom("Button"),
                    properties = mapOf("systemImage" to "chevron.right"),
                    modifiers = listOf(SwiftUIModifier(ModifierType.Custom, mapOf("buttonStyle" to "borderless")))
                )
            ),
            modifiers = listOf(SwiftUIModifier.padding(8f))
        )
        children.add(header)

        // Weekday headers
        val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
        val weekdayHeader = SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 0f),
            children = weekdays.map { day ->
                SwiftUIView.text(
                    content = day,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Caption),
                        SwiftUIModifier.fontWeight(FontWeight.Semibold),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel")),
                        SwiftUIModifier(ModifierType.Custom, mapOf("frame" to mapOf("maxWidth" to Float.MAX_VALUE)))
                    )
                )
            }
        )
        children.add(weekdayHeader)

        // Calendar grid (LazyVGrid with 7 columns)
        val calendarGrid = SwiftUIView(
            type = ViewType.Custom("LazyVGrid"),
            properties = mapOf(
                "columns" to 7,
                "spacing" to 8f
            ),
            children = generateCalendarDays(component),
            modifiers = listOf(SwiftUIModifier.padding(8f))
        )
        children.add(calendarGrid)

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("spacing" to 8f),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                SwiftUIModifier.cornerRadius(12f)
            ),
        )
    }

    private fun generateCalendarDays(component: MonthCalendar): List<SwiftUIView> {
        // Simplified: generate day cells for the month
        val days = mutableListOf<SwiftUIView>()

        // Calculate days in month (simplified)
        val daysInMonth = when (component.month) {
            2 -> if (component.year % 4 == 0 && (component.year % 100 != 0 || component.year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        for (day in 1..daysInMonth) {
            val dateStr = "${component.year.toString().padStart(4, '0')}-${component.month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
            val isSelected = dateStr == component.selectedDate
            val isDisabled = component.disabledDates.contains(dateStr)
            val isHighlighted = component.highlightedDates.contains(dateStr)

            days.add(SwiftUIView(
                type = ViewType.ZStack,
                properties = emptyMap(),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Custom("Circle"),
                        properties = emptyMap(),
                        modifiers = if (isSelected) {
                            listOf(SwiftUIModifier.foregroundColor(SwiftUIColor.primary))
                        } else if (isHighlighted) {
                            listOf(SwiftUIModifier.foregroundColor(SwiftUIColor.system("systemYellow")))
                        } else {
                            listOf(SwiftUIModifier(ModifierType.Custom, mapOf("opacity" to 0f)))
                        }
                    ),
                    SwiftUIView.text(
                        content = day.toString(),
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Body),
                            SwiftUIModifier.foregroundColor(
                                when {
                                    isDisabled -> SwiftUIColor.system("tertiaryLabel")
                                    isSelected -> SwiftUIColor.system("white")
                                    else -> SwiftUIColor.system("label")
                                }
                            )
                        )
                    )
                ),
                modifiers = listOf(
                    SwiftUIModifier(ModifierType.Custom, mapOf("frame" to mapOf("width" to 36f, "height" to 36f)))
                )
            ))
        }

        return days
    }
}

// ============================================
// WEEK CALENDAR
// ============================================

object WeekCalendarMapper {
    fun map(component: WeekCalendar, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Week dates header
        val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val weekdayRow = SwiftUIView(
            type = ViewType.HStack,
            properties = mapOf("spacing" to 4f),
            children = weekdays.mapIndexed { index, day ->
                SwiftUIView(
                    type = ViewType.VStack,
                    properties = mapOf("spacing" to 4f),
                    children = listOf(
                        SwiftUIView.text(
                            content = day,
                            modifiers = listOf(
                                SwiftUIModifier.font(FontStyle.Caption),
                                SwiftUIModifier.fontWeight(FontWeight.Semibold),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                            )
                        ),
                        SwiftUIView(
                            type = ViewType.ZStack,
                            properties = emptyMap(),
                            children = listOf(
                                SwiftUIView.text(
                                    content = (index + 1).toString(),
                                    modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
                                )
                            ),
                            modifiers = listOf(
                                SwiftUIModifier(ModifierType.Custom, mapOf("frame" to mapOf("width" to 36f, "height" to 36f))),
                                SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                                SwiftUIModifier.cornerRadius(18f)
                            )
                        )
                    ),
                    modifiers = listOf(
                        SwiftUIModifier(ModifierType.Custom, mapOf("frame" to mapOf("maxWidth" to Float.MAX_VALUE)))
                    )
                )
            }
        )
        children.add(weekdayRow)

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("spacing" to 8f),
            children = children,
            modifiers = listOf(
                SwiftUIModifier.padding(16f),
                SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                SwiftUIModifier.cornerRadius(12f)
            ),
        )
    }
}

// ============================================
// EVENT CALENDAR
// ============================================

object EventCalendarMapper {
    fun map(component: EventCalendar, theme: Theme?): SwiftUIView {
        val children = mutableListOf<SwiftUIView>()

        // Date picker with calendar style
        children.add(SwiftUIView(
            type = ViewType.Custom("DatePicker"),
            properties = mapOf(
                "selection" to (component.selectedDate ?: ""),
                "displayedComponents" to "date"
            ),
            modifiers = listOf(
                SwiftUIModifier(ModifierType.Custom, mapOf("datePickerStyle" to "graphical")),
                SwiftUIModifier.padding(16f)
            )
        ))

        // Event list
        if (component.showEventList && component.events.isNotEmpty()) {
            children.add(SwiftUIView(
                type = ViewType.Custom("Divider"),
                properties = emptyMap(),
                modifiers = listOf(SwiftUIModifier.padding(8f))
            ))

            children.add(SwiftUIView.text(
                content = "Events",
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Subheadline),
                    SwiftUIModifier.fontWeight(FontWeight.Semibold)
                )
            ))

            component.events.forEach { event ->
                children.add(SwiftUIView(
                    type = ViewType.HStack,
                    properties = mapOf("spacing" to 8f),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Custom("Circle"),
                            properties = emptyMap(),
                            modifiers = listOf(
                                SwiftUIModifier(ModifierType.Custom, mapOf("frame" to mapOf("width" to 8f, "height" to 8f))),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                            )
                        ),
                        SwiftUIView(
                            type = ViewType.VStack,
                            properties = mapOf("alignment" to "leading", "spacing" to 2f),
                            children = listOf(
                                SwiftUIView.text(
                                    content = event.title,
                                    modifiers = listOf(SwiftUIModifier.font(FontStyle.Body))
                                ),
                                SwiftUIView.text(
                                    content = event.date,
                                    modifiers = listOf(
                                        SwiftUIModifier.font(FontStyle.Caption),
                                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                                    )
                                )
                            )
                        )
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.padding(8f),
                        SwiftUIModifier.background(SwiftUIColor.system("secondarySystemGroupedBackground")),
                        SwiftUIModifier.cornerRadius(8f)
                    )
                ))
            }
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf("alignment" to "leading", "spacing" to 12f),
            children = children,
            modifiers = listOf(SwiftUIModifier.padding(16f)),
        )
    }
}
