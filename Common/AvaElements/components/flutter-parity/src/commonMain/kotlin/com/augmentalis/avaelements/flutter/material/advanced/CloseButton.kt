package com.augmentalis.avaelements.flutter.material.advanced

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * CloseButton component - Flutter Material parity
 *
 * A standardized close/dismiss button for dialogs, drawers, alerts, and other dismissible components.
 * Provides consistent styling and behavior across the application.
 *
 * **Web Equivalent:** `CloseButton` (MUI IconButton with Close icon)
 * **Material Design 3:** https://m3.material.io/components/icon-buttons/overview
 *
 * ## Features
 * - Standardized close icon
 * - Three sizes (small, medium, large)
 * - Edge positioning support for app bars
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 * - 48dp minimum touch target
 *
 * ## Usage Example
 * ```kotlin
 * CloseButton(
 *     onPressed = {
 *         // Handle close action
 *     },
 *     size = CloseButton.Size.Medium
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property enabled Whether the button is enabled for user interaction
 * @property size Size of the close button (small/medium/large)
 * @property edge Whether button is positioned at edge of parent (for app bars)
 * @property contentDescription Accessibility description for TalkBack
 * @property onPressed Callback invoked when button is pressed (not serialized)
 * @property style Optional button style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class CloseButton(
    override val type: String = "CloseButton",
    override val id: String? = null,
    val enabled: Boolean = true,
    val size: Size = Size.Medium,
    val edge: EdgePosition? = null,
    val contentDescription: String? = null,
    @Transient
    val onPressed: (() -> Unit)? = null,
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
        val base = contentDescription ?: "Close"
        val state = if (enabled) "" else ", disabled"
        return "$base$state"
    }

    /**
     * Get icon size in pixels based on size variant
     */
    fun getIconSizeInPixels(): Int {
        return when (size) {
            Size.Small -> 18
            Size.Medium -> 24
            Size.Large -> 32
        }
    }

    /**
     * Button size variants
     */
    enum class Size {
        /** Small close button (18dp icon) */
        Small,

        /** Medium close button (24dp icon) - default */
        Medium,

        /** Large close button (32dp icon) */
        Large
    }

    /**
     * Edge positioning for app bars and toolbars
     */
    enum class EdgePosition {
        /** No edge positioning */
        None,

        /** Position at start edge */
        Start,

        /** Position at end edge */
        End,

        /** Position at top edge */
        Top,

        /** Position at bottom edge */
        Bottom
    }

    companion object {
        /**
         * Create a standard close button
         */
        fun standard(
            enabled: Boolean = true,
            onPressed: (() -> Unit)? = null
        ) = CloseButton(
            enabled = enabled,
            onPressed = onPressed
        )

        /**
         * Create a small close button
         */
        fun small(
            enabled: Boolean = true,
            onPressed: (() -> Unit)? = null
        ) = CloseButton(
            size = Size.Small,
            enabled = enabled,
            onPressed = onPressed
        )

        /**
         * Create a large close button
         */
        fun large(
            enabled: Boolean = true,
            onPressed: (() -> Unit)? = null
        ) = CloseButton(
            size = Size.Large,
            enabled = enabled,
            onPressed = onPressed
        )

        /**
         * Create a close button positioned at edge
         */
        fun atEdge(
            edge: EdgePosition,
            enabled: Boolean = true,
            onPressed: (() -> Unit)? = null
        ) = CloseButton(
            edge = edge,
            enabled = enabled,
            onPressed = onPressed
        )
    }
}
