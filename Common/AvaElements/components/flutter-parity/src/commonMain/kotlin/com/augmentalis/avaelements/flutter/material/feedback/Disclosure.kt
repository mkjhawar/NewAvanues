package com.augmentalis.avaelements.flutter.material.feedback

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Disclosure component - Flutter Material parity
 *
 * A simple collapsible disclosure widget with expand/collapse animation.
 * Lighter weight alternative to ExpandableCard, focused on basic show/hide functionality.
 *
 * **Web Equivalent:** `Accordion` (MUI), `Collapse` (Ant Design), `<details>` (HTML)
 * **Material Design 3:** https://m3.material.io/components/lists/overview
 *
 * ## Features
 * - Simple expand/collapse toggle
 * - Title with expand icon
 * - Smooth animation
 * - Controlled or uncontrolled state
 * - Keyboard navigation support (Space/Enter)
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * Disclosure(
 *     title = "Show more details",
 *     content = "This is additional information that can be hidden or shown",
 *     initiallyExpanded = false,
 *     onExpansionChanged = { expanded ->
 *         println("Disclosure is now: ${if (expanded) "open" else "closed"}")
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Disclosure title/summary text
 * @property content Content to show when expanded
 * @property initiallyExpanded Initial expansion state (uncontrolled mode)
 * @property expanded Current expansion state (controlled mode, optional)
 * @property animationDuration Animation duration in milliseconds
 * @property showIcon Whether to show expand/collapse icon
 * @property contentDescription Accessibility description for TalkBack
 * @property onExpansionChanged Callback invoked when expansion state changes (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.2.0-feedback-components
 */
data class Disclosure(
    override val type: String = "Disclosure",
    override val id: String? = null,
    val title: String,
    val content: String,
    val initiallyExpanded: Boolean = false,
    val expanded: Boolean? = null,
    val animationDuration: Int = 200,
    val showIcon: Boolean = true,
    val contentDescription: String? = null,
    @Transient
    val onExpansionChanged: ((Boolean) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Check if component is in controlled mode (external state management)
     */
    fun isControlled(): Boolean {
        return expanded != null
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(currentlyExpanded: Boolean): String {
        val base = contentDescription ?: "Disclosure"
        val state = if (currentlyExpanded) "expanded" else "collapsed"
        return "$base: $title, $state. Double tap to ${if (currentlyExpanded) "collapse" else "expand"}"
    }

    /**
     * Validate animation duration
     */
    fun isAnimationDurationValid(): Boolean {
        return animationDuration in 0..5000
    }

    companion object {
        /** Default animation duration in milliseconds */
        const val DEFAULT_ANIMATION_DURATION = 200

        /**
         * Create a simple disclosure (initially collapsed)
         */
        fun simple(
            title: String,
            content: String
        ) = Disclosure(
            title = title,
            content = content,
            initiallyExpanded = false
        )

        /**
         * Create an initially expanded disclosure
         */
        fun expanded(
            title: String,
            content: String
        ) = Disclosure(
            title = title,
            content = content,
            initiallyExpanded = true
        )

        /**
         * Create a controlled disclosure
         */
        fun controlled(
            title: String,
            content: String,
            expanded: Boolean,
            onExpansionChanged: ((Boolean) -> Unit)? = null
        ) = Disclosure(
            title = title,
            content = content,
            expanded = expanded,
            onExpansionChanged = onExpansionChanged
        )
    }
}
