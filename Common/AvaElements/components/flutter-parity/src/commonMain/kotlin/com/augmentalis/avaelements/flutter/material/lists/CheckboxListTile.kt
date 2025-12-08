package com.augmentalis.avaelements.flutter.material.lists

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * CheckboxListTile component - Flutter Material parity
 *
 * A list tile with an integrated checkbox, following Material Design 3 specifications.
 * Combines a ListTile with a Checkbox control.
 *
 * **Flutter Equivalent:** `CheckboxListTile`
 * **Material Design 3:** https://m3.material.io/components/lists/overview
 *
 * ## Features
 * - Three checkbox states: checked, unchecked, indeterminate (tri-state)
 * - Checkbox can be positioned as leading or trailing
 * - Support for title, subtitle, and secondary text
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 * - Minimum 48dp touch target
 *
 * ## Usage Example
 * ```kotlin
 * CheckboxListTile(
 *     title = "Enable notifications",
 *     subtitle = "Receive updates and alerts",
 *     value = true,
 *     onChanged = { checked ->
 *         // Handle checkbox state change
 *     },
 *     controlAffinity = ListTileControlAffinity.Leading
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Main title text
 * @property subtitle Optional subtitle text
 * @property secondary Optional secondary widget description
 * @property value Checkbox state (true = checked, false = unchecked, null = indeterminate)
 * @property tristate Whether to allow indeterminate state (default: false)
 * @property enabled Whether the checkbox is enabled for user interaction
 * @property controlAffinity Position of checkbox relative to title (leading or trailing)
 * @property activeColor Color when checkbox is checked
 * @property checkColor Color of checkmark
 * @property tileColor Background color of the tile
 * @property selectedTileColor Background color when selected
 * @property dense Whether to use dense vertical layout
 * @property isThreeLine Whether this is a three-line list tile
 * @property contentPadding Custom padding for the tile
 * @property selected Whether the tile is in selected state
 * @property autofocus Whether to autofocus this tile
 * @property shape Custom shape for the tile
 * @property contentDescription Accessibility description for TalkBack
 * @property onChanged Callback invoked when checkbox value changes (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class CheckboxListTile(
    override val type: String = "CheckboxListTile",
    override val id: String? = null,
    val title: String,
    val subtitle: String? = null,
    val secondary: String? = null,
    val value: Boolean? = false,
    val tristate: Boolean = false,
    val enabled: Boolean = true,
    val controlAffinity: ListTileControlAffinity = ListTileControlAffinity.Leading,
    val activeColor: String? = null,
    val checkColor: String? = null,
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
    val onChanged: ((Boolean?) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     * Combines title with checkbox state for TalkBack
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: title
        val state = when (value) {
            true -> "checked"
            false -> "unchecked"
            null -> "indeterminate"
        }
        return "$base, $state"
    }

    /**
     * Control affinity - position of checkbox relative to title
     */
    enum class ListTileControlAffinity {
        /** Checkbox appears before the title (left in LTR) */
        Leading,

        /** Checkbox appears after the title (right in LTR) */
        Trailing,

        /** Checkbox follows platform conventions */
        Platform
    }

    companion object {
        /**
         * Create a checked checkbox list tile
         */
        fun checked(
            title: String,
            onChanged: ((Boolean?) -> Unit)? = null
        ) = CheckboxListTile(
            title = title,
            value = true,
            onChanged = onChanged
        )

        /**
         * Create an unchecked checkbox list tile
         */
        fun unchecked(
            title: String,
            onChanged: ((Boolean?) -> Unit)? = null
        ) = CheckboxListTile(
            title = title,
            value = false,
            onChanged = onChanged
        )

        /**
         * Create an indeterminate checkbox list tile
         */
        fun indeterminate(
            title: String,
            onChanged: ((Boolean?) -> Unit)? = null
        ) = CheckboxListTile(
            title = title,
            value = null,
            tristate = true,
            onChanged = onChanged
        )

        /**
         * Create a checkbox list tile with subtitle
         */
        fun withSubtitle(
            title: String,
            subtitle: String,
            value: Boolean = false,
            onChanged: ((Boolean?) -> Unit)? = null
        ) = CheckboxListTile(
            title = title,
            subtitle = subtitle,
            value = value,
            onChanged = onChanged
        )
    }
}
