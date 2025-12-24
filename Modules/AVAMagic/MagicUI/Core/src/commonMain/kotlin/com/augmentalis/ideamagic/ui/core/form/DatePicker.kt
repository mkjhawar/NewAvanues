package com.augmentalis.magicui.ui.core.form

import com.augmentalis.magicui.components.core.*

/**
 * DatePicker Component
 *
 * A date picker component for selecting dates from a calendar interface.
 *
 * Features:
 * - Calendar view for date selection
 * - Min/max date constraints
 * - Custom date formatting
 * - Date range selection support
 * - Locale-aware display
 * - Keyboard input support
 *
 * Platform mappings:
 * - Android: MaterialDatePicker
 * - iOS: UIDatePicker
 * - Web: Input type="date" or custom calendar
 *
 * Usage:
 * ```kotlin
 * DatePicker(
 *     selectedDate = System.currentTimeMillis(),
 *     minDate = startOfYear,
 *     maxDate = endOfYear,
 *     dateFormat = "yyyy-MM-dd",
 *     onDateChange = { timestamp -> println("Selected: $timestamp") }
 * )
 * ```
 */
data class DatePickerComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val selectedDate: Long? = null,
    val minDate: Long? = null,
    val maxDate: Long? = null,
    val dateFormat: String = "yyyy-MM-dd",
    val label: String? = null,
    val enabled: Boolean = true,
    val onDateChange: ((Long) -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    init {
        if (minDate != null && maxDate != null) {
            require(minDate <= maxDate) {
                "DatePicker minDate must be before or equal to maxDate"
            }
        }
        if (selectedDate != null) {
            if (minDate != null) {
                require(selectedDate >= minDate) {
                    "Selected date must be after or equal to minDate"
                }
            }
            if (maxDate != null) {
                require(selectedDate <= maxDate) {
                    "Selected date must be before or equal to maxDate"
                }
            }
        }
        require(dateFormat.isNotBlank()) { "Date format cannot be blank" }
    }
}

typealias DatePicker = DatePickerComponent
