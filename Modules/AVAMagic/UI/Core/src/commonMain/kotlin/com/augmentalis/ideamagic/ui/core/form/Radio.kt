package com.augmentalis.avanues.avamagic.ui.core.form

import com.augmentalis.avanues.avamagic.components.core.*

/**
 * Radio button for mutually exclusive selections.
 *
 * Radio buttons allow users to select one option from a set.
 * Use RadioGroup to manage multiple radio buttons.
 *
 * ## Usage Examples
 * ```kotlin
 * // Single radio button
 * val radio = RadioComponent(
 *     label = "Option 1",
 *     selected = true
 * )
 *
 * // Radio group
 * val options = listOf("Small", "Medium", "Large")
 * val selectedIndex = 1
 * val radioGroup = options.mapIndexed { index, option ->
 *     RadioComponent(
 *         label = option,
 *         selected = index == selectedIndex,
 *         value = option
 *     )
 * }
 *
 * // With custom size
 * val largeRadio = RadioComponent(
 *     label = "Large Option",
 *     size = ComponentSize.LG
 * )
 * ```
 *
 * @property label Display text next to radio button
 * @property selected Whether this radio is selected
 * @property value Optional value (for form submission)
 * @property size Radio button size (default MD)
 * @property enabled Whether user can interact (default true)
 * @since 1.0.0
 */
data class RadioComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val label: String,
    val selected: Boolean = false,
    val value: String? = null,
    val size: ComponentSize = ComponentSize.MD,
    val enabled: Boolean = true,
    val onSelected: (() -> Unit)? = null
) : Component {
    init {
        require(label.isNotBlank()) { "Label cannot be blank" }
    }

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    /**
     * Returns a copy with selected state toggled.
     */
    fun toggle(): RadioComponent = copy(selected = !selected)

    /**
     * Returns a copy with selected = true.
     */
    fun select(): RadioComponent = copy(selected = true)

    /**
     * Returns a copy with selected = false.
     */
    fun deselect(): RadioComponent = copy(selected = false)
}

/**
 * Radio button group for managing multiple mutually exclusive options.
 *
 * ## Usage Example
 * ```kotlin
 * val group = RadioGroupComponent(
 *     options = listOf("Option 1", "Option 2", "Option 3"),
 *     selectedIndex = 0
 * )
 *
 * // With labels different from values
 * val group = RadioGroupComponent(
 *     options = listOf(
 *         "S" to "Small",
 *         "M" to "Medium",
 *         "L" to "Large"
 *     ),
 *     selectedValue = "M"
 * )
 * ```
 *
 * @property options List of options (value to label pairs)
 * @property selectedValue Currently selected option value
 * @property orientation Layout orientation (default VERTICAL)
 * @since 1.0.0
 */
data class RadioGroupComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val options: List<Pair<String, String>>,
    val selectedValue: String? = null,
    val orientation: Orientation =
        Orientation.Vertical,
    val size: ComponentSize = ComponentSize.MD,
    val enabled: Boolean = true
) : Component {
    init {
        require(options.isNotEmpty()) { "Options cannot be empty" }
        if (selectedValue != null) {
            require(options.any { it.first == selectedValue }) {
                "selectedValue must be in options"
            }
        }
    }

    /**
     * Convenience constructor for simple string options.
     */
    constructor(
        options: List<String>,
        selectedIndex: Int? = null,
        id: String? = null,
        style: ComponentStyle? = null,
        modifiers: List<Modifier> = emptyList()
    ) : this(
        id = id,
        style = style,
        modifiers = modifiers,
        options = options.map { it to it },
        selectedValue = selectedIndex?.let { options.getOrNull(it) }
    )

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    /**
     * Returns a copy with the specified value selected.
     */
    fun select(value: String): RadioGroupComponent = copy(selectedValue = value)

    /**
     * Returns the currently selected option label, or null if none selected.
     */
    val selectedLabel: String?
        get() = options.firstOrNull { it.first == selectedValue }?.second
}

typealias RadioButton = RadioComponent
typealias RadioGroup = RadioGroupComponent
