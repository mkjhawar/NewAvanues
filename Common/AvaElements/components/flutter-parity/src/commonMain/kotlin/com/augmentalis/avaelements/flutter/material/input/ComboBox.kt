package com.augmentalis.avaelements.flutter.material.input

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ComboBox component - Flutter Material parity
 *
 * A searchable dropdown combo box that combines text input with selection from a list.
 * Users can type to filter options or select from a dropdown list.
 *
 * **Flutter Equivalent:** `SearchableDropdown` or `Autocomplete`
 * **Material Design 3:** ExposedDropdownMenuBox with search
 *
 * ## Features
 * - Searchable dropdown functionality
 * - Text input with filtering
 * - Support for custom items
 * - Keyboard navigation
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * ComboBox(
 *     value = "Apple",
 *     options = listOf("Apple", "Banana", "Orange"),
 *     label = "Select Fruit",
 *     onValueChange = { selected ->
 *         // Handle selection
 *     },
 *     allowCustomValue = true
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property value Current selected or input value
 * @property options List of available options
 * @property label Label text displayed above the combo box
 * @property placeholder Placeholder text when empty
 * @property enabled Whether the combo box is enabled
 * @property required Whether the field is required
 * @property errorText Error message to display (null if valid)
 * @property allowCustomValue Allow user to enter custom values not in options
 * @property contentDescription Accessibility description
 * @property onValueChange Callback invoked when value changes
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class ComboBox(
    override val type: String = "ComboBox",
    override val id: String? = null,
    val value: String = "",
    val options: List<String> = emptyList(),
    val label: String? = null,
    val placeholder: String? = null,
    val enabled: Boolean = true,
    val required: Boolean = false,
    val errorText: String? = null,
    val allowCustomValue: Boolean = false,
    val contentDescription: String? = null,
    @Transient
    val onValueChange: ((String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: label ?: "Combo box"
        val requiredState = if (required) ", required" else ""
        val errorState = if (errorText != null) ", error: $errorText" else ""
        val optionsCount = ", ${options.size} options"
        return "$base$requiredState$errorState$optionsCount"
    }

    /**
     * Filter options based on input query
     */
    fun getFilteredOptions(query: String): List<String> {
        if (query.isBlank()) return options
        return options.filter { it.contains(query, ignoreCase = true) }
    }
}
