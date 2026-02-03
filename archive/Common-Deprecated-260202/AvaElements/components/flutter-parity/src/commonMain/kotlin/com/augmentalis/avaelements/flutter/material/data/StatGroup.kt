package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * StatGroup component - Flutter Material parity
 *
 * A grouped statistics display for showing multiple related metrics at once.
 * Commonly used in dashboards, analytics, and reporting interfaces.
 *
 * **Flutter Equivalent:** Custom implementation
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Multiple layout modes (horizontal, vertical, grid)
 * - Change indicators (positive, negative, neutral)
 * - Optional icons for stats
 * - Trend visualization
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * StatGroup(
 *     title = "Weekly Overview",
 *     stats = listOf(
 *         StatItem("Revenue", "$12,450", change = "+12.5%", changeType = ChangeType.Positive),
 *         StatItem("Orders", "234", change = "+8", changeType = ChangeType.Positive),
 *         StatItem("Returns", "12", change = "-3", changeType = ChangeType.Negative)
 *     ),
 *     layout = Layout.Horizontal
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Optional title for the stat group
 * @property stats List of statistics to display
 * @property layout Layout mode for the statistics
 * @property showDividers Whether to show dividers between stats
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class StatGroup(
    override val type: String = "StatGroup",
    override val id: String? = null,
    val title: String? = null,
    val stats: List<StatItem> = emptyList(),
    val layout: Layout = Layout.Horizontal,
    val showDividers: Boolean = false,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Single statistic item
     */
    data class StatItem(
        val label: String,
        val value: String,
        val change: String? = null,
        val changeType: ChangeType = ChangeType.Neutral,
        val icon: String? = null,
        val description: String? = null
    )

    /**
     * Change type indicator
     */
    enum class ChangeType {
        /** Positive change (green indicator) */
        Positive,

        /** Negative change (red indicator) */
        Negative,

        /** Neutral change (no color indicator) */
        Neutral
    }

    /**
     * Layout mode for statistics
     */
    enum class Layout {
        /** Horizontal row layout */
        Horizontal,

        /** Vertical column layout */
        Vertical,

        /** Grid layout (2 columns) */
        Grid
    }

    companion object {
        /**
         * Create a vertical stat group
         */
        fun vertical(
            title: String? = null,
            stats: List<StatItem>
        ) = StatGroup(
            title = title,
            stats = stats,
            layout = Layout.Vertical
        )

        /**
         * Create a grid stat group
         */
        fun grid(
            title: String? = null,
            stats: List<StatItem>
        ) = StatGroup(
            title = title,
            stats = stats,
            layout = Layout.Grid
        )
    }
}
