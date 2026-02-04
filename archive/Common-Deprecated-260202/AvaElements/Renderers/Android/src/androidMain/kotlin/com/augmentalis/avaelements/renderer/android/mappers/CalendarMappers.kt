package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.flutter.material.data.*
import com.augmentalis.avaelements.renderer.android.utils.DateUtils

/**
 * Maps Calendar component to Material 3 DatePicker.
 *
 * Implements full calendar view with date selection, date validation,
 * and accessibility support.
 *
 * @param component Calendar component to render
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarMapper(component: Calendar) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.parseIsoDate(component.selectedDate)
    )

    DatePicker(
        state = datePickerState,
        dateValidator = { timestamp ->
            val date = DateUtils.formatTimestamp(timestamp)
            !component.disabledDates.contains(date) &&
                    DateUtils.isDateInRange(date, component.minDate, component.maxDate)
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.contentDescription ?: "Calendar"
            }
    )

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { timestamp ->
            component.onDateSelected?.invoke(DateUtils.formatTimestamp(timestamp))
        }
    }
}

/**
 * Maps DateCalendar component to compact Material 3 DatePicker.
 *
 * Simplified date-only calendar without time selection.
 *
 * @param component DateCalendar component to render
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateCalendarMapper(component: DateCalendar) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.parseIsoDate(component.selectedDate)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.contentDescription ?: "Date Calendar"
            }
    ) {
        DatePicker(
            state = datePickerState,
            dateValidator = { timestamp ->
                val date = DateUtils.formatTimestamp(timestamp)
                !component.disabledDates.contains(date) &&
                        DateUtils.isDateInRange(date, component.minDate, component.maxDate)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { timestamp ->
            component.onDateSelected?.invoke(DateUtils.formatTimestamp(timestamp))
        }
    }
}

/**
 * Maps MonthCalendar component to month-constrained Material 3 DatePicker.
 *
 * Displays a single month view with highlighting and navigation support.
 *
 * @param component MonthCalendar component to render
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthCalendarMapper(component: MonthCalendar) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.parseIsoDate(component.selectedDate),
        initialDisplayedMonthMillis = DateUtils.parseIsoDate(
            String.format("%04d-%02d-01", component.year, component.month)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.contentDescription
                    ?: "Month Calendar for ${component.year}-${component.month}"
            }
    ) {
        DatePicker(
            state = datePickerState,
            dateValidator = { timestamp ->
                val date = DateUtils.formatTimestamp(timestamp)
                !component.disabledDates.contains(date)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { timestamp ->
            component.onDateSelected?.invoke(DateUtils.formatTimestamp(timestamp))
        }
    }
}

/**
 * Maps WeekCalendar component to custom week view with time slots.
 *
 * Displays a week view with events and time slot grid.
 *
 * @param component WeekCalendar component to render
 */
@Composable
fun WeekCalendarMapper(component: WeekCalendar) {
    val weekDates = remember(component.startDate) {
        DateUtils.getWeekDates(component.startDate)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.contentDescription
                    ?: "Week Calendar starting ${component.startDate}"
            }
    ) {
        // Week header with day names and dates
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekDates.forEach { date ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { component.onDateSelected?.invoke(date) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val dayOfWeek = DateUtils.getDayOfWeek(date)
                    val isSelected = date == component.selectedDate

                    Text(
                        text = DateUtils.formatDayOfWeek(dayOfWeek),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = date.split("-").last(), // Day number
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                CircleShape
                            )
                            .padding(4.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        HorizontalDivider()

        // Time slots and events
        if (component.showTimeSlots) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items((component.startHour..component.endHour).toList()) { hour ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(component.timeSlotHeight.dp)
                    ) {
                        // Hour label
                        Text(
                            text = DateUtils.formatTime(hour, 0),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .width(60.dp)
                                .padding(8.dp)
                        )

                        // Time slots for each day
                        weekDates.forEach { date ->
                            val dayEvents = component.events.filter { event ->
                                event.date == date &&
                                        DateUtils.parseTime(event.startTime)?.first == hour
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        component.onTimeSlotClick?.invoke(date, hour)
                                    }
                                    .padding(1.dp)
                            ) {
                                if (dayEvents.isNotEmpty()) {
                                    dayEvents.forEach { event ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    component.onEventClick?.invoke(event.id)
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        ) {
                                            Text(
                                                text = event.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(4.dp),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                } else {
                                    // Empty time slot
                                    Divider(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Simple event list without time slots
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(component.events) { event ->
                    ListItem(
                        headlineContent = { Text(event.title) },
                        supportingContent = {
                            Text("${event.startTime} - ${event.endTime}")
                        },
                        leadingContent = {
                            Text(
                                text = DateUtils.formatDate(event.date),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.clickable {
                            component.onEventClick?.invoke(event.id)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Maps EventCalendar component to Material 3 DatePicker with event list.
 *
 * Displays calendar with event markers and synchronized event list.
 *
 * @param component EventCalendar component to render
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCalendarMapper(component: EventCalendar) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.parseIsoDate(component.selectedDate)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.contentDescription
                    ?: "Event Calendar"
            }
    ) {
        // Calendar view
        DatePicker(
            state = datePickerState,
            dateValidator = { timestamp ->
                val date = DateUtils.formatTimestamp(timestamp)
                DateUtils.isDateInRange(date, component.minDate, component.maxDate)
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Event list for selected date
        if (component.showEventList) {
            datePickerState.selectedDateMillis?.let { timestamp ->
                val selectedDate = DateUtils.formatTimestamp(timestamp)
                val dayEvents = component.events.filter { it.date == selectedDate }

                if (dayEvents.isNotEmpty()) {
                    HorizontalDivider()

                    Text(
                        text = "Events on ${DateUtils.formatDate(selectedDate)}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    LazyColumn {
                        items(dayEvents.take(component.maxVisibleEvents)) { event ->
                            ListItem(
                                headlineContent = { Text(event.title) },
                                supportingContent = {
                                    Column {
                                        event.description?.let { desc ->
                                            Text(desc)
                                        }
                                        if (!event.allDay && event.startTime != null && event.endTime != null) {
                                            Text("${event.startTime} - ${event.endTime}")
                                        } else {
                                            Text("All day")
                                        }
                                        event.location?.let { loc ->
                                            Text("Location: $loc")
                                        }
                                    }
                                },
                                leadingContent = {
                                    event.color?.let { color ->
                                        DateUtils.parseColor(color)?.let { colorValue ->
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(
                                                        Color(colorValue),
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.clickable {
                                    component.onEventClick?.invoke(event.id)
                                }
                            )
                        }

                        if (dayEvents.size > component.maxVisibleEvents) {
                            item {
                                Text(
                                    text = "+${dayEvents.size - component.maxVisibleEvents} more events",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No events on ${DateUtils.formatDate(selectedDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    component.onAddEvent?.let { onAdd ->
                        TextButton(
                            onClick = { onAdd(selectedDate) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text("Add Event")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { timestamp ->
            component.onDateSelected?.invoke(DateUtils.formatTimestamp(timestamp))
        }
    }
}
