package com.augmentalis.avanueui.ui.core.form

import com.augmentalis.avanueui.core.*

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
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val options: List<DropdownOption>,
    val selectedIndex: Int? = null,
    val selectedValue: String? = null,
    val label: String? = null,
    val placeholder: String = "Select an option",
    val searchable: Boolean = false,
    val enabled: Boolean = true,
    val onValueChange: ((String) -> Unit)? = null,
    val onSelectionChanged: ((Int) -> Unit)? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
    init {
        require(options.isNotEmpty()) { "Dropdown must have at least one option" }
        if (selectedValue != null) {
            require(options.any { it.value == selectedValue }) {
                "Selected value must be one of the options"
            }
        }
    }

}

/**
 * Dropdown option data class
 */
data class DropdownOption(
    val value: String,
    val label: String,
    val icon: String? = null,
    val enabled: Boolean = true
) {
    init {
        require(value.isNotBlank()) { "Dropdown option value cannot be blank" }
        require(label.isNotBlank()) { "Dropdown option label cannot be blank" }
    }
}

typealias Dropdown = DropdownComponent
