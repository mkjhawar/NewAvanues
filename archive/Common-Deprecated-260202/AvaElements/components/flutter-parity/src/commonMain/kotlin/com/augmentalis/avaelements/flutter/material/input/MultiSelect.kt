package com.augmentalis.avaelements.flutter.material.input

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * MultiSelect component - Flutter Material parity
 *
 * A multi-selection input allowing users to select multiple items from a list.
 * Displays selected items as chips with a dropdown or modal for selection.
 *
 * **Flutter Equivalent:** `MultiSelectDialogField` (from multi_select_flutter)
 * **Material Design 3:** ExposedDropdownMenuBox with multiple selection
 *
 * ## Features
 * - Multiple item selection
 * - Selected items shown as chips
 * - Dropdown or modal selection UI
 * - Search/filter capability
 * - Select all / Clear all
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * MultiSelect(
 *     selectedValues = listOf("Apple", "Banana"),
 *     options = listOf("Apple", "Banana", "Orange", "Grape"),
 *     label = "Select Fruits",
 *     placeholder = "Choose one or more",
 *     onSelectionChange = { selected ->
 *         // Handle selection change
 *     },
 *     maxSelections = 5
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property selectedValues List of currently selected values
 * @property options List of available options
 * @property label Label text displayed above the input
 * @property placeholder Placeholder text when no selections
 * @property enabled Whether the input is enabled
 * @property required Whether at least one selection is required
 * @property maxSelections Maximum number of selections allowed (null = unlimited)
 * @property searchable Whether to show search field in dropdown
 * @property showChips Whether to show selected items as chips
 * @property errorText Error message to display (null if valid)
 * @property contentDescription Accessibility description
 * @property onSelectionChange Callback invoked when selection changes
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class MultiSelect(
    override val type: String = "MultiSelect",
    override val id: String? = null,
    val selectedValues: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val label: String? = null,
    val placeholder: String? = null,
    val enabled: Boolean = true,
    val required: Boolean = false,
    val maxSelections: Int? = null,
    val searchable: Boolean = true,
    val showChips: Boolean = true,
    val errorText: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onSelectionChange: ((List<String>) -> Unit)? = null,
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
        val base = contentDescription ?: label ?: "Multi-select"
        val selectionCount = ", ${selectedValues.size} of ${options.size} selected"
        val requiredState = if (required) ", required" else ""
        val errorState = if (errorText != null) ", error: $errorText" else ""
        return "$base$selectionCount$requiredState$errorState"
    }

    /**
     * Check if max selections reached
     */
    fun isMaxSelectionsReached(): Boolean {
        return maxSelections != null && selectedValues.size >= maxSelections
    }

    /**
     * Check if a value is selected
     */
    fun isSelected(value: String): Boolean {
        return selectedValues.contains(value)
    }

    /**
     * Toggle selection of a value
     */
    fun toggleSelection(value: String): List<String> {
        return if (isSelected(value)) {
            selectedValues - value
        } else if (!isMaxSelectionsReached()) {
            selectedValues + value
        } else {
            selectedValues
        }
    }
}
