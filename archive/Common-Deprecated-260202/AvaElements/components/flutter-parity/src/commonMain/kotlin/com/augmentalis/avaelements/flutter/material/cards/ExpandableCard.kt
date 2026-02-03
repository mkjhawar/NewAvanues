package com.augmentalis.avaelements.flutter.material.cards

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ExpandableCard component - Flutter Material parity
 *
 * A Material Design 3 card that can expand/collapse to show more content.
 * Commonly used in FAQs, settings panels, and collapsible content sections.
 *
 * **Web Equivalent:** `ExpandableCard` / `Accordion` (MUI)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Smooth expand/collapse animation
 * - Summary content always visible
 * - Detailed content shown when expanded
 * - Rotating expand/collapse icon
 * - Control expanded state programmatically
 * - Optional action buttons in header
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * ExpandableCard(
 *     title = "Advanced Settings",
 *     subtitle = "Configure advanced options",
 *     icon = "settings",
 *     summaryContent = "Click to view advanced configuration options",
 *     expandedContent = "Detailed settings content goes here...",
 *     initiallyExpanded = false,
 *     showDivider = true,
 *     onExpansionChanged = { expanded ->
 *         // Handle expansion state change
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Card title
 * @property subtitle Optional subtitle
 * @property icon Optional leading icon
 * @property summaryContent Content shown when collapsed (can be text or component ID)
 * @property expandedContent Content shown when expanded (can be text or component ID)
 * @property initiallyExpanded Whether card starts expanded
 * @property expanded Optional controlled expanded state
 * @property showDivider Whether to show divider between summary and expanded content
 * @property expandIcon Optional custom expand icon
 * @property collapseIcon Optional custom collapse icon
 * @property animationDuration Animation duration in milliseconds
 * @property headerActions Optional action buttons in header
 * @property contentDescription Accessibility description for TalkBack
 * @property onExpansionChanged Callback invoked when expansion state changes (not serialized)
 * @property onHeaderActionPressed Callback invoked when header action is pressed (not serialized)
 * @property style Optional card style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class ExpandableCard(
    override val type: String = "ExpandableCard",
    override val id: String? = null,
    val title: String,
    val subtitle: String? = null,
    val icon: String? = null,
    val summaryContent: String? = null,
    val expandedContent: String,
    val initiallyExpanded: Boolean = false,
    val expanded: Boolean? = null,
    val showDivider: Boolean = true,
    val expandIcon: String? = null,
    val collapseIcon: String? = null,
    val animationDuration: Int = 300,
    val headerActions: List<HeaderAction> = emptyList(),
    val contentDescription: String? = null,
    @Transient
    val onExpansionChanged: ((Boolean) -> Unit)? = null,
    @Transient
    val onHeaderActionPressed: ((String) -> Unit)? = null,
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
    fun getAccessibilityDescription(isExpanded: Boolean): String {
        val base = contentDescription ?: title
        val state = if (isExpanded) "expanded" else "collapsed"
        val subtitle = if (this.subtitle != null) ", ${this.subtitle}" else ""
        return "$base$subtitle, $state"
    }

    /**
     * Get effective expanded state (controlled or uncontrolled)
     */
    fun isControlled(): Boolean {
        return expanded != null
    }

    /**
     * Header action button
     */
    data class HeaderAction(
        val id: String,
        val icon: String,
        val label: String? = null,
        val enabled: Boolean = true
    )

    companion object {
        /**
         * Default animation duration in milliseconds
         */
        const val DEFAULT_ANIMATION_DURATION = 300

        /**
         * Create a simple expandable card
         */
        fun simple(
            title: String,
            expandedContent: String,
            initiallyExpanded: Boolean = false,
            onExpansionChanged: ((Boolean) -> Unit)? = null
        ) = ExpandableCard(
            title = title,
            expandedContent = expandedContent,
            initiallyExpanded = initiallyExpanded,
            onExpansionChanged = onExpansionChanged
        )

        /**
         * Create an expandable card with summary content
         */
        fun withSummary(
            title: String,
            summaryContent: String,
            expandedContent: String,
            initiallyExpanded: Boolean = false,
            onExpansionChanged: ((Boolean) -> Unit)? = null
        ) = ExpandableCard(
            title = title,
            summaryContent = summaryContent,
            expandedContent = expandedContent,
            initiallyExpanded = initiallyExpanded,
            onExpansionChanged = onExpansionChanged
        )

        /**
         * Create a controlled expandable card
         */
        fun controlled(
            title: String,
            expandedContent: String,
            expanded: Boolean,
            onExpansionChanged: ((Boolean) -> Unit)? = null
        ) = ExpandableCard(
            title = title,
            expandedContent = expandedContent,
            expanded = expanded,
            onExpansionChanged = onExpansionChanged
        )
    }
}
