package com.augmentalis.avaelements.flutter.material.lists

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * SwitchListTile component - Flutter Material parity
 *
 * A list tile with an integrated switch toggle, following Material Design 3 specifications.
 * Combines a ListTile with a Switch control.
 *
 * **Flutter Equivalent:** `SwitchListTile`
 * **Material Design 3:** https://m3.material.io/components/lists/overview
 *
 * ## Features
 * - Toggle switch for boolean values
 * - Switch typically positioned as trailing element
 * - Support for title, subtitle, and secondary text
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 * - Minimum 48dp touch target
 *
 * ## Usage Example
 * ```kotlin
 * SwitchListTile(
 *     title = "Dark Mode",
 *     subtitle = "Use dark color scheme",
 *     value = false,
 *     onChanged = { enabled ->
 *         // Handle switch state change
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Main title text
 * @property subtitle Optional subtitle text
 * @property secondary Optional secondary widget description
 * @property value Switch state (true = on, false = off)
 * @property enabled Whether the switch is enabled for user interaction
 * @property controlAffinity Position of switch relative to title (typically trailing)
 * @property activeColor Color when switch is on
 * @property activeTrackColor Track color when switch is on
 * @property inactiveThumbColor Thumb color when switch is off
 * @property inactiveTrackColor Track color when switch is off
 * @property tileColor Background color of the tile
 * @property selectedTileColor Background color when selected
 * @property dense Whether to use dense vertical layout
 * @property isThreeLine Whether this is a three-line list tile
 * @property contentPadding Custom padding for the tile
 * @property selected Whether the tile is in selected state
 * @property autofocus Whether to autofocus this tile
 * @property shape Custom shape for the tile
 * @property contentDescription Accessibility description for TalkBack
 * @property onChanged Callback invoked when switch value changes (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class SwitchListTile(
    override val type: String = "SwitchListTile",
    override val id: String? = null,
    val title: String,
    val subtitle: String? = null,
    val secondary: String? = null,
    val value: Boolean = false,
    val enabled: Boolean = true,
    val controlAffinity: ListTileControlAffinity = ListTileControlAffinity.Trailing,
    val activeColor: String? = null,
    val activeTrackColor: String? = null,
    val inactiveThumbColor: String? = null,
    val inactiveTrackColor: String? = null,
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
    val onChanged: ((Boolean) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     * Combines title with switch state for TalkBack
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: title
        val state = if (value) "on" else "off"
        return "$base, $state"
    }

    /**
     * Control affinity - position of switch relative to title
     */
    enum class ListTileControlAffinity {
        /** Switch appears before the title (left in LTR) */
        Leading,

        /** Switch appears after the title (right in LTR) */
        Trailing,

        /** Switch follows platform conventions */
        Platform
    }

    companion object {
        /**
         * Create a switch list tile in ON state
         */
        fun on(
            title: String,
            onChanged: ((Boolean) -> Unit)? = null
        ) = SwitchListTile(
            title = title,
            value = true,
            onChanged = onChanged
        )

        /**
         * Create a switch list tile in OFF state
         */
        fun off(
            title: String,
            onChanged: ((Boolean) -> Unit)? = null
        ) = SwitchListTile(
            title = title,
            value = false,
            onChanged = onChanged
        )

        /**
         * Create a switch list tile with subtitle
         */
        fun withSubtitle(
            title: String,
            subtitle: String,
            value: Boolean = false,
            onChanged: ((Boolean) -> Unit)? = null
        ) = SwitchListTile(
            title = title,
            subtitle = subtitle,
            value = value,
            onChanged = onChanged
        )
    }
}
