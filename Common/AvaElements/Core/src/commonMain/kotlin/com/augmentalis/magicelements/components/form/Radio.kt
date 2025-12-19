package com.augmentalis.avaelements.components.form

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

/**
 * Radio Component
 *
 * A radio button group component for single selection from multiple options.
 *
 * Features:
 * - Single selection from multiple options
 * - Horizontal or vertical layout
 * - Individual option enable/disable state
 * - Custom styling and modifiers
 * - Change event handling
 *
 * Platform mappings:
 * - Android: RadioGroup with RadioButtons
 * - iOS: Custom radio button group
 * - Web: Radio input group
 *
 * Usage:
 * ```kotlin
 * Radio(
 *     options = listOf(
 *         RadioOption("male", "Male"),
 *         RadioOption("female", "Female"),
 *         RadioOption("other", "Other")
 *     ),
 *     selectedValue = "male",
 *     groupName = "gender",
 *     orientation = Orientation.Horizontal,
 *     onValueChange = { value -> println("Selected: $value") }
 * )
 * ```
 */
data class RadioComponent(
    override val type: String = "Radio",
    val options: List<RadioOption>,
    val selectedValue: String?,
    val groupName: String,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val orientation: Orientation = Orientation.Vertical,
    val onValueChange: ((String) -> Unit)? = null
) : Component {
    init {
        require(options.isNotEmpty()) { "Radio group must have at least one option" }
        require(groupName.isNotBlank()) { "Radio group name cannot be blank" }
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
 * Radio option data class
 */
data class RadioOption(
    val value: String,
    val label: String,
    val enabled: Boolean = true
) {
    init {
        require(value.isNotBlank()) { "Radio option value cannot be blank" }
        require(label.isNotBlank()) { "Radio option label cannot be blank" }
    }
}
