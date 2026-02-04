package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * MonthCalendar component for month view calendar display.
 *
 * Displays a single month view with date selection capabilities.
 * Optimized for compact displays and focused date selection within a month.
 *
 * **Material Design 3 Implementation:**
 * - Uses Material 3 DatePicker with month constraint
 * - Supports dynamic colors
 * - Follows Material Design guidelines
 *
 * **Accessibility:**
 * - Full TalkBack support with month and date announcements
 * - Keyboard navigation support
 * - WCAG 2.1 Level AA compliant
 *
 * @property year The year to display (e.g., 2025)
 * @property month The month to display (1-12)
 * @property selectedDate Currently selected date in ISO 8601 format (YYYY-MM-DD), or null if no date selected
 * @property disabledDates List of dates that cannot be selected (ISO 8601 format)
 * @property highlightedDates List of dates to highlight (e.g., holidays) in ISO 8601 format
 * @property firstDayOfWeek First day of the week (0 = Sunday, 1 = Monday, etc.)
 * @property onDateSelected Callback invoked when a date is selected, receives ISO 8601 date string
 * @property onMonthChange Callback invoked when user navigates to a different month
 * @property contentDescription Accessibility label for the calendar
 *
 * **Example:**
 * ```kotlin
 * MonthCalendar(
 *     year = 2025,
 *     month = 11,
 *     selectedDate = "2025-11-24",
 *     highlightedDates = listOf("2025-11-01", "2025-11-25"),
 *     firstDayOfWeek = 1, // Monday
 *     onDateSelected = { date -> println("Selected: $date") },
 *     onMonthChange = { year, month -> println("Navigated to $year-$month") }
 * )
 * ```
 */
data class MonthCalendar(
    override val type: String = "MonthCalendar",
    override val id: String? = null,
    val year: Int,
    val month: Int, // 1-12
    val selectedDate: String? = null,
    val disabledDates: List<String> = emptyList(),
    val highlightedDates: List<String> = emptyList(),
    val firstDayOfWeek: Int = 0, // 0 = Sunday, 1 = Monday
    @Transient
    val onDateSelected: ((String) -> Unit)? = null,
    @Transient
    val onMonthChange: ((year: Int, month: Int) -> Unit)? = null,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}
