package com.augmentalis.avaelements.components.form

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

/**
 * Dropdown Component
 *
 * A dropdown select component for choosing from a list of options.
 *
 * Features:
 * - Single selection from dropdown list
 * - Optional search/filter functionality
 * - Custom icons for options
 * - Disabled options support
 * - Placeholder text
 * - Keyboard navigation
 *
 * Platform mappings:
 * - Android: Spinner or ExposedDropdownMenu
 * - iOS: UIPickerView or custom menu
 * - Web: Select element or custom dropdown
 *
 * Usage:
 * ```kotlin
 * Dropdown(
 *     options = listOf(
 *         DropdownOption("us", "United States", "flag-us", false),
 *         DropdownOption("uk", "United Kingdom", "flag-uk", false),
 *         DropdownOption("ca", "Canada", "flag-ca", false)
 *     ),
 *     selectedValue = "us",
 *     placeholder = "Select a country",
 *     searchable = true,
 *     onValueChange = { value -> println("Selected: $value") }
 * )
 * ```
 */
data class DropdownComponent(
    override val type: String = "Dropdown",
    val options: List<DropdownOption>,
    val selectedValue: String? = null,
    val placeholder: String = "Select an option",
    val searchable: Boolean = false,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onValueChange: ((String) -> Unit)? = null
) : Component {
    init {
        require(options.isNotEmpty()) { "Dropdown must have at least one option" }
        if (selectedValue != null) {
            require(options.any { it.value == selectedValue }) {
                "Selected value must be one of the options"
            }
        }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Dropdown option data class
 */
data class DropdownOption(
    val value: String,
    val label: String,
    val icon: String? = null,
    val disabled: Boolean = false
) {
    init {
        require(value.isNotBlank()) { "Dropdown option value cannot be blank" }
        require(label.isNotBlank()) { "Dropdown option label cannot be blank" }
    }
}
