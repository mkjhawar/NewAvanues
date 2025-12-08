package com.augmentalis.avanues.avamagic.ui.core.form

import com.augmentalis.avanues.avamagic.components.core.*
import com.augmentalis.avanues.avamagic.components.core.Orientation

/**
 * Group of toggle buttons for selecting one or multiple options.
 *
 * Toggle button groups present a set of buttons where one or more can be
 * selected. Similar to radio buttons but with button styling.
 *
 * ## Usage Examples
 * ```kotlin
 * // Single selection
 * val group = ToggleButtonGroupComponent(
 *     options = listOf("Left", "Center", "Right"),
 *     selectedIndices = setOf(1)
 * )
 *
 * // Multiple selection
 * val group = ToggleButtonGroupComponent(
 *     options = listOf("Bold", "Italic", "Underline"),
 *     selectedIndices = setOf(0, 2),
 *     multiSelect = true
 * )
 *
 * // With icons
 * val group = ToggleButtonGroupComponent(
 *     options = listOf("align_left" to "Left", "align_center" to "Center"),
 *     selectedIndices = setOf(0)
 * )
 * ```
 *
 * @property options List of options (icon to label pairs)
 * @property selectedIndices Set of selected option indices
 * @property multiSelect Allow multiple selections (default false)
 * @property orientation Button layout (default HORIZONTAL)
 * @property size Button size (default MD)
 * @property enabled Whether user can interact (default true)
 * @since 1.0.0
 */
data class ToggleButtonGroupComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val options: List<Pair<String, String>>,
    val selectedIndices: Set<Int> = emptySet(),
    val multiSelect: Boolean = false,
    val orientation: Orientation = Orientation.Horizontal,
    val size: ComponentSize = ComponentSize.MD,
    val enabled: Boolean = true
) : Component {
    init {
        require(options.isNotEmpty()) { "Options cannot be empty" }
        require(selectedIndices.all { it in options.indices }) {
            "All selectedIndices must be valid option indices"
        }
        if (!multiSelect) {
            require(selectedIndices.size <= 1) {
                "Single-select mode allows only one selection"
            }
        }
    }

    /**
     * Convenience constructor for simple text options.
     */
    constructor(
        options: List<String>,
        selectedIndices: Set<Int> = emptySet(),
        multiSelect: Boolean = false,
        id: String? = null,
        style: ComponentStyle? = null,
        modifiers: List<Modifier> = emptyList()
    ) : this(
        id = id,
        style = style,
        modifiers = modifiers,
        options = options.map { it to it },
        selectedIndices = selectedIndices,
        multiSelect = multiSelect
    )

    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    /**
     * Toggles selection at the given index.
     * In single-select mode, deselects previous and selects new.
     * In multi-select mode, toggles the index.
     */
    fun toggleSelection(index: Int): ToggleButtonGroupComponent {
        require(index in options.indices) { "Index $index out of bounds" }

        val newSelection = if (multiSelect) {
            if (index in selectedIndices) {
                selectedIndices - index
            } else {
                selectedIndices + index
            }
        } else {
            if (index in selectedIndices) emptySet() else setOf(index)
        }

        return copy(selectedIndices = newSelection)
    }

    /**
     * Selects the given index (adds to selection in multi-select mode).
     */
    fun select(index: Int): ToggleButtonGroupComponent {
        require(index in options.indices) { "Index $index out of bounds" }

        val newSelection = if (multiSelect) {
            selectedIndices + index
        } else {
            setOf(index)
        }

        return copy(selectedIndices = newSelection)
    }

    /**
     * Deselects the given index.
     */
    fun deselect(index: Int): ToggleButtonGroupComponent {
        return copy(selectedIndices = selectedIndices - index)
    }

    /**
     * Clears all selections.
     */
    fun clearSelection(): ToggleButtonGroupComponent {
        return copy(selectedIndices = emptySet())
    }

    companion object {
        /**
         * Creates text alignment toggle group.
         */
        fun textAlign(selectedIndex: Int = 0) = ToggleButtonGroupComponent(
            options = listOf("Left", "Center", "Right", "Justify"),
            selectedIndices = setOf(selectedIndex)
        )

        /**
         * Creates text formatting toggle group (multi-select).
         */
        fun textFormat(bold: Boolean = false, italic: Boolean = false, underline: Boolean = false) =
            ToggleButtonGroupComponent(
                options = listOf("Bold", "Italic", "Underline"),
                selectedIndices = buildSet {
                    if (bold) add(0)
                    if (italic) add(1)
                    if (underline) add(2)
                },
                multiSelect = true
            )
    }
}
