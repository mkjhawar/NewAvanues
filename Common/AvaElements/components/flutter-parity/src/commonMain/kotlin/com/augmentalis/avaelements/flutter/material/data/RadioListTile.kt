package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * RadioListTile component - Flutter Material parity
 *
 * A list tile with an integrated radio button, following Material Design 3 specifications.
 * Combines a ListTile with a Radio control for grouped radio behavior.
 *
 * **Flutter Equivalent:** `RadioListTile`
 * **Material Design 3:** https://m3.material.io/components/radio-button/overview
 *
 * ## Features
 * - Radio button for exclusive selection within a group
 * - Radio typically positioned as trailing or leading element
 * - Support for title, subtitle, and secondary text
 * - Grouped radio behavior via value/groupValue pattern
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 * - Minimum 48dp touch target
 *
 * ## Usage Example
 * ```kotlin
 * // Group of radio buttons
 * var selectedValue by remember { mutableStateOf("option1") }
 *
 * Column {
 *     RadioListTile(
 *         title = "Option 1",
 *         subtitle = "First choice",
 *         value = "option1",
 *         groupValue = selectedValue,
 *         onChanged = { selectedValue = it }
 *     )
 *     RadioListTile(
 *         title = "Option 2",
 *         subtitle = "Second choice",
 *         value = "option2",
 *         groupValue = selectedValue,
 *         onChanged = { selectedValue = it }
 *     )
 * }
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Main title text
 * @property subtitle Optional subtitle text
 * @property secondary Optional secondary widget description
 * @property value This radio button's value
 * @property groupValue Currently selected value in the radio group
 * @property enabled Whether the radio button is enabled for user interaction
 * @property controlAffinity Position of radio relative to title (typically trailing)
 * @property activeColor Color when radio is selected
 * @property tileColor Background color of the tile
 * @property selectedTileColor Background color when selected
 * @property dense Whether to use dense vertical layout
 * @property isThreeLine Whether this is a three-line list tile
 * @property contentPadding Custom padding for the tile
 * @property selected Whether the tile is in selected state
 * @property autofocus Whether to autofocus this tile
 * @property shape Custom shape for the tile
 * @property contentDescription Accessibility description for TalkBack
 * @property onChanged Callback invoked when radio value changes (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class RadioListTile(
    override val type: String = "RadioListTile",
    override val id: String? = null,
    val title: String,
    val subtitle: String? = null,
    val secondary: String? = null,
    val value: String,
    val groupValue: String?,
    val enabled: Boolean = true,
    val controlAffinity: ListTileControlAffinity = ListTileControlAffinity.Trailing,
    val activeColor: String? = null,
    val tileColor: String? = null,
    val selectedTileColor: String? = null,
    val dense: Boolean = false,
    val isThreeLine: Boolean = false,
    val contentPadding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val selected: Boolean = false,
    val autofocus: Boolean = false,
    val shape: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onChanged: ((String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Check if this radio button is selected
     */
    val isSelected: Boolean
        get() = value == groupValue

    /**
     * Get effective accessibility description
     * Combines title with radio state for TalkBack
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: title
        val state = if (isSelected) "selected" else "not selected"
        return "$base, $state"
    }

    /**
     * Control affinity - position of radio relative to title
     */
    enum class ListTileControlAffinity {
        /** Radio appears before the title (left in LTR) */
        Leading,

        /** Radio appears after the title (right in LTR) */
        Trailing,

        /** Radio follows platform conventions */
        Platform
    }

    companion object {
        /**
         * Create a selected radio list tile
         */
        fun selected(
            title: String,
            value: String,
            onChanged: ((String) -> Unit)? = null
        ) = RadioListTile(
            title = title,
            value = value,
            groupValue = value,
            onChanged = onChanged
        )

        /**
         * Create an unselected radio list tile
         */
        fun unselected(
            title: String,
            value: String,
            groupValue: String?,
            onChanged: ((String) -> Unit)? = null
        ) = RadioListTile(
            title = title,
            value = value,
            groupValue = groupValue,
            onChanged = onChanged
        )

        /**
         * Create a radio list tile with subtitle
         */
        fun withSubtitle(
            title: String,
            subtitle: String,
            value: String,
            groupValue: String?,
            onChanged: ((String) -> Unit)? = null
        ) = RadioListTile(
            title = title,
            subtitle = subtitle,
            value = value,
            groupValue = groupValue,
            onChanged = onChanged
        )
    }
}
