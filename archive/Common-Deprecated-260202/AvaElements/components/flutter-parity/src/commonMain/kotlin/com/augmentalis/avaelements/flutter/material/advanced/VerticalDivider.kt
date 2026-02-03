package com.augmentalis.avaelements.flutter.material.advanced

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer

/**
 * VerticalDivider component - Flutter Material parity
 *
 * A vertical line that separates content, following Material Design 3 specifications.
 * Typically used in horizontal layouts like Row or between toolbar items.
 *
 * **Flutter Equivalent:** `VerticalDivider`
 * **Material Design 3:** https://m3.material.io/components/divider/overview
 *
 * ## Features
 * - Vertical divider line
 * - Customizable width, thickness, and color
 * - Indent support (top and bottom)
 * - Material3 theming with proper contrast ratios
 * - Dark mode support
 * - TalkBack accessibility (marked as decorative)
 * - WCAG 2.1 Level AA compliant (3:1 contrast ratio minimum)
 *
 * ## Usage Example
 * ```kotlin
 * Row {
 *     IconButton("home")
 *     VerticalDivider(
 *         thickness = 1f,
 *         indent = 8f,
 *         endIndent = 8f,
 *         color = "outline"
 *     )
 *     IconButton("search")
 * }
 * ```
 *
 * @property id Unique identifier for the component
 * @property width Total width of the divider (including padding)
 * @property thickness Thickness (horizontal width) of the line
 * @property indent Empty space above the divider
 * @property endIndent Empty space below the divider
 * @property color Color of the divider line (uses outline color from theme if null)
 * @property semanticsLabel Accessibility label (usually decorative, so often null)
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class VerticalDivider(
    override val type: String = "VerticalDivider",
    override val id: String? = null,
    val width: Float? = null,
    val thickness: Float = 1f,
    val indent: Float = 0f,
    val endIndent: Float = 0f,
    val color: String? = null,
    val semanticsLabel: String? = null,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective width (total space occupied)
     */
    fun getEffectiveWidth(): Float {
        return width ?: (thickness + 16f) // Default width includes padding
    }

    /**
     * Get effective accessibility description
     * Dividers are typically decorative, so this may be null
     */
    fun getAccessibilityDescription(): String? {
        return contentDescription ?: semanticsLabel
    }

    /**
     * Check if divider is visible (has positive thickness)
     */
    fun isVisible(): Boolean {
        return thickness > 0f
    }

    companion object {
        /**
         * Default divider thickness (in dp)
         */
        const val DEFAULT_THICKNESS = 1f

        /**
         * Default divider width including padding (in dp)
         */
        const val DEFAULT_WIDTH = 16f

        /**
         * Create a simple vertical divider with default styling
         */
        fun simple() = VerticalDivider()

        /**
         * Create a vertical divider with custom thickness
         */
        fun withThickness(
            thickness: Float
        ) = VerticalDivider(
            thickness = thickness
        )

        /**
         * Create a vertical divider with custom color
         */
        fun withColor(
            color: String
        ) = VerticalDivider(
            color = color
        )

        /**
         * Create a vertical divider with indents
         */
        fun withIndents(
            indent: Float,
            endIndent: Float = indent
        ) = VerticalDivider(
            indent = indent,
            endIndent = endIndent
        )

        /**
         * Create a vertical divider with custom width and thickness
         */
        fun custom(
            width: Float,
            thickness: Float,
            color: String? = null
        ) = VerticalDivider(
            width = width,
            thickness = thickness,
            color = color
        )

        /**
         * Create a thick vertical divider (2dp)
         */
        fun thick() = VerticalDivider(
            thickness = 2f
        )

        /**
         * Create a thin vertical divider (0.5dp)
         */
        fun thin() = VerticalDivider(
            thickness = 0.5f
        )
    }
}
