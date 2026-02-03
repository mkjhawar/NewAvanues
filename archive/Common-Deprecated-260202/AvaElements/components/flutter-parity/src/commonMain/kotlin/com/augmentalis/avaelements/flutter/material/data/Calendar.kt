package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Calendar component for full calendar view with date selection.
 *
 * Provides a standard calendar interface with date selection capabilities,
 * date range constraints, and disabled dates support.
 *
 * **Material Design 3 Implementation:**
 * - Uses Material 3 DatePicker
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
 * @property onDateSelected Callback invoked when a date is selected, receives ISO 8601 date string
 * @property contentDescription Accessibility label for the calendar
 *
 * **Example:**
 * ```kotlin
 * Calendar(
 *     selectedDate = "2025-11-24",
 *     minDate = "2025-01-01",
 *     maxDate = "2025-12-31",
 *     disabledDates = listOf("2025-11-25", "2025-12-25"),
 *     onDateSelected = { date -> println("Selected: $date") },
 *     contentDescription = "Select appointment date"
 * )
 * ```
 */
data class Calendar(
    override val type: String = "Calendar",
    override val id: String? = null,
    val selectedDate: String? = null,
    val minDate: String? = null,
    val maxDate: String? = null,
    val disabledDates: List<String> = emptyList(),
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
