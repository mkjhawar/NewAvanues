package com.augmentalis.avaelements.flutter.material.advanced

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * FilledButton component - Flutter Material parity
 *
 * A Material Design 3 filled button with all visual states (enabled, disabled, pressed, hovered, focused).
 * This is the primary button style in Material 3.
 *
 * **Flutter Equivalent:** `FilledButton`
 * **Material Design 3:** https://m3.material.io/components/buttons/overview
 *
 * ## Features
 * - Material3 filled button style
 * - All interaction states (enabled, disabled, pressed, hovered, focused)
 * - Optional leading/trailing icons
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 * - Minimum 48dp touch target
 *
 * ## Usage Example
 * ```kotlin
 * FilledButton(
 *     text = "Continue",
 *     onPressed = {
 *         // Handle button press
 *     },
 *     icon = "arrow_forward",
 *     iconPosition = IconPosition.Trailing
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property text Button text label
 * @property enabled Whether the button is enabled for user interaction
 * @property icon Optional icon name/resource to display
 * @property iconPosition Position of icon relative to text
 * @property autofocus Whether to autofocus this button
 * @property clipBehavior How to clip the button content
 * @property focusNode Focus node for managing focus
 * @property style Optional button style overrides
 * @property contentDescription Accessibility description for TalkBack
 * @property onPressed Callback invoked when button is pressed (not serialized)
 * @property onLongPress Callback invoked on long press (not serialized)
 * @property onHover Callback invoked on hover state change (not serialized)
 * @property onFocusChange Callback invoked on focus state change (not serialized)
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class FilledButton(
    override val type: String = "FilledButton",
    override val id: String? = null,
    val text: String,
    val enabled: Boolean = true,
    val icon: String? = null,
    val iconPosition: IconPosition = IconPosition.Leading,
    val autofocus: Boolean = false,
    val clipBehavior: ClipBehavior = ClipBehavior.None,
    val focusNode: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onPressed: (() -> Unit)? = null,
    @Transient
    val onLongPress: (() -> Unit)? = null,
    @Transient
    val onHover: ((Boolean) -> Unit)? = null,
    @Transient
    val onFocusChange: ((Boolean) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: text
        val state = if (enabled) "button" else "button, disabled"
        return "$base, $state"
    }

    /**
     * Icon position relative to text
     */
    enum class IconPosition {
        /** Icon before text */
        Leading,

        /** Icon after text */
        Trailing
    }

    /**
     * Clip behavior for button content
     */
    enum class ClipBehavior {
        /** No clipping */
        None,

        /** Clip to bounds */
        HardEdge,

        /** Clip with anti-aliasing */
        AntiAlias,

        /** Clip with anti-aliasing and save layer */
        AntiAliasWithSaveLayer
    }

    companion object {
        /**
         * Create a simple filled button with text only
         */
        fun simple(
            text: String,
            enabled: Boolean = true,
            onPressed: (() -> Unit)? = null
        ) = FilledButton(
            text = text,
            enabled = enabled,
            onPressed = onPressed
        )

        /**
         * Create a filled button with leading icon
         */
        fun withLeadingIcon(
            text: String,
            icon: String,
            enabled: Boolean = true,
            onPressed: (() -> Unit)? = null
        ) = FilledButton(
            text = text,
            icon = icon,
            iconPosition = IconPosition.Leading,
            enabled = enabled,
            onPressed = onPressed
        )

        /**
         * Create a filled button with trailing icon
         */
        fun withTrailingIcon(
            text: String,
            icon: String,
            enabled: Boolean = true,
            onPressed: (() -> Unit)? = null
        ) = FilledButton(
            text = text,
            icon = icon,
            iconPosition = IconPosition.Trailing,
            enabled = enabled,
            onPressed = onPressed
        )

        /**
         * Create a disabled filled button
         */
        fun disabled(
            text: String
        ) = FilledButton(
            text = text,
            enabled = false,
            onPressed = null
        )
    }
}
