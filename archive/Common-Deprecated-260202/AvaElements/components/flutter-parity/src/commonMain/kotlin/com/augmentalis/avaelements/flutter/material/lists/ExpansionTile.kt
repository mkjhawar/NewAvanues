package com.augmentalis.avaelements.flutter.material.lists

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ExpansionTile component - Flutter Material parity
 *
 * An expandable list tile with children that can be shown or hidden.
 * Features smooth expand/collapse animation and rotating trailing icon.
 *
 * **Flutter Equivalent:** `ExpansionTile`
 * **Material Design 3:** https://m3.material.io/components/lists/overview
 *
 * ## Features
 * - Smooth expand/collapse animation (200ms)
 * - Rotating trailing icon (180° rotation)
 * - Support for leading widget (icon/avatar)
 * - Optional subtitle when collapsed
 * - Nested expansion tiles supported
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * ExpansionTile(
 *     title = "Settings",
 *     subtitle = "Configure app preferences",
 *     leading = "settings_icon",
 *     initiallyExpanded = false,
 *     children = listOf(
 *         // Child components
 *     ),
 *     onExpansionChanged = { expanded ->
 *         // Handle expansion state change
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Main title text
 * @property subtitle Optional subtitle text shown when collapsed
 * @property leading Optional leading icon/avatar resource name
 * @property trailing Optional custom trailing widget (overrides default expand icon)
 * @property initiallyExpanded Whether tile starts in expanded state
 * @property maintainState Whether to maintain child state when collapsed (default: true)
 * @property tilePadding Custom padding for the tile (uses Material default if null)
 * @property expandedCrossAxisAlignment Alignment of children when expanded
 * @property expandedAlignment Horizontal alignment of expanded content
 * @property childrenPadding Padding applied to children container
 * @property backgroundColor Background color override
 * @property collapsedBackgroundColor Background color when collapsed
 * @property textColor Text color override
 * @property collapsedTextColor Text color when collapsed
 * @property iconColor Icon color override
 * @property collapsedIconColor Icon color when collapsed
 * @property contentDescription Accessibility description for TalkBack
 * @property children List of child components shown when expanded
 * @property onExpansionChanged Callback invoked when expansion state changes (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class ExpansionTile(
    override val type: String = "ExpansionTile",
    override val id: String? = null,
    val title: String,
    val subtitle: String? = null,
    val leading: String? = null,
    val trailing: String? = null,
    val initiallyExpanded: Boolean = false,
    val maintainState: Boolean = true,
    val tilePadding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val expandedCrossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Center,
    val expandedAlignment: Alignment = Alignment.Start,
    val childrenPadding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val backgroundColor: String? = null,
    val collapsedBackgroundColor: String? = null,
    val textColor: String? = null,
    val collapsedTextColor: String? = null,
    val iconColor: String? = null,
    val collapsedIconColor: String? = null,
    val contentDescription: String? = null,
    val children: List<Component> = emptyList(),
    @Transient
    val onExpansionChanged: ((Boolean) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     * Combines title with expansion state for TalkBack
     */
    fun getAccessibilityDescription(expanded: Boolean): String {
        val base = contentDescription ?: title
        val state = if (expanded) "expanded" else "collapsed"
        return "$base, $state"
    }

    /**
     * Cross-axis alignment for children
     */
    enum class CrossAxisAlignment {
        /** Align children to start */
        Start,

        /** Center children */
        Center,

        /** Align children to end */
        End,

        /** Stretch children to fill width */
        Stretch
    }

    /**
     * Horizontal alignment for expanded content
     */
    enum class Alignment {
        /** Align to start (left in LTR) */
        Start,

        /** Center horizontally */
        Center,

        /** Align to end (right in LTR) */
        End
    }

    companion object {
        /**
         * Default animation duration for expand/collapse (milliseconds)
         */
        const val DEFAULT_ANIMATION_DURATION = 200

        /**
         * Create an expansion tile with simple configuration
         */
        fun simple(
            title: String,
            children: List<Component>,
            initiallyExpanded: Boolean = false,
            onExpansionChanged: ((Boolean) -> Unit)? = null
        ) = ExpansionTile(
            title = title,
            children = children,
            initiallyExpanded = initiallyExpanded,
            onExpansionChanged = onExpansionChanged
        )

        /**
         * Create an expansion tile with subtitle
         */
        fun withSubtitle(
            title: String,
            subtitle: String,
            children: List<Component>,
            initiallyExpanded: Boolean = false,
            onExpansionChanged: ((Boolean) -> Unit)? = null
        ) = ExpansionTile(
            title = title,
            subtitle = subtitle,
            children = children,
            initiallyExpanded = initiallyExpanded,
            onExpansionChanged = onExpansionChanged
        )

        /**
         * Create an expansion tile with leading icon
         */
        fun withIcon(
            title: String,
            leading: String,
            children: List<Component>,
            initiallyExpanded: Boolean = false,
            onExpansionChanged: ((Boolean) -> Unit)? = null
        ) = ExpansionTile(
            title = title,
            leading = leading,
            children = children,
            initiallyExpanded = initiallyExpanded,
            onExpansionChanged = onExpansionChanged
        )
    }
}
