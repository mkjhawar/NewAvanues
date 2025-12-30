package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * DateCalendar component for date-only calendar (no time selection).
 *
 * A simplified calendar component that focuses on date selection without
 * time picker functionality. Ideal for forms and date input fields.
 *
 * **Material Design 3 Implementation:**
 * - Uses Material 3 DatePicker in compact mode
 * - Supports dynamic colors
 * - Follows Material Design guidelines
 *
 * **Accessibility:**
 * - Full TalkBack support with date announcements
 * - Keyboard navigation support
 * - WCAG 2.1 Level AA compliant
 *
 * @property selectedDate Currently selected date in ISO 8601 format (YYYY-MM-DD), or null if no date selected
 * @property minDate Minimum selectable date in ISO 8601 format, or null for no minimum
 * @property maxDate Maximum selectable date in ISO 8601 format, or null for no maximum
 * @property disabledDates List of dates that cannot be selected (ISO 8601 format)
 * @property showWeekNumbers Whether to display week numbers in the calendar
 * @property firstDayOfWeek First day of the week (0 = Sunday, 1 = Monday, etc.)
 * @property onDateSelected Callback invoked when a date is selected, receives ISO 8601 date string
 * @property contentDescription Accessibility label for the calendar
 *
 * **Example:**
 * ```kotlin
 * DateCalendar(
 *     selectedDate = "2025-11-24",
 *     minDate = "2025-01-01",
 *     showWeekNumbers = true,
 *     firstDayOfWeek = 1, // Monday
 *     onDateSelected = { date -> println("Date: $date") }
 * )
 * ```
 */
data class DateCalendar(
    override val type: String = "DateCalendar",
    override val id: String? = null,
    val selectedDate: String? = null,
    val minDate: String? = null,
    val maxDate: String? = null,
    val disabledDates: List<String> = emptyList(),
    val showWeekNumbers: Boolean = false,
    val firstDayOfWeek: Int = 0, // 0 = Sunday, 1 = Monday
    @Transient
    val onDateSelected: ((String) -> Unit)? = null,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}
