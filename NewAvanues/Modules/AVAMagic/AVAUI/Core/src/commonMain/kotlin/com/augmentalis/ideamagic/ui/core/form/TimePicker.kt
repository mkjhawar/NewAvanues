package com.augmentalis.avamagic.ui.core.form

import com.augmentalis.avamagic.components.core.*

/**
 * Time data class for TimePicker
 */
data class Time(
    val hour: Int,
    val minute: Int
) {
    fun format(is24Hour: Boolean): String {
        return if (is24Hour) {
            "%02d:%02d".format(hour, minute)
        } else {
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val amPm = if (hour < 12) "AM" else "PM"
            "%d:%02d %s".format(displayHour, minute, amPm)
        }
    }
}

/**
 * TimePicker Component
 *
 * A time picker component for selecting hours and minutes.
 *
 * Features:
 * - 12-hour or 24-hour format
 * - Hours and minutes selection
 * - Optional step intervals
 * - AM/PM selection (12-hour mode)
 * - Keyboard input support
 * - Custom time formatting
 *
 * Platform mappings:
 * - Android: MaterialTimePicker
 * - iOS: UIDatePicker in time mode
 * - Web: Input type="time" or custom picker
 *
 * Usage:
 * ```kotlin
 * TimePicker(
 *     selectedTime = Time(14, 30),
 *     is24Hour = true,
 *     onTimeSelected = { time ->
 *         println("Selected: ${time.hour}:${time.minute}")
 *     }
 * )
 * ```
 */
data class TimePickerComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val selectedTime: Time? = null,
    val is24Hour: Boolean = true,
    val label: String? = null,
    val placeholder: String = "Select time",
    val enabled: Boolean = true,
    val onTimeSelected: ((Time) -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

typealias TimePicker = TimePickerComponent
