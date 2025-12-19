package com.augmentalis.avanues.avamagic.ui.core.display

import com.augmentalis.avanues.avamagic.components.core.*
/**
 * StatCard Component
 *
 * A display component for presenting statistics with label, value, and optional trend indicators.
 * Ideal for dashboards, analytics pages, and KPI displays.
 *
 * Features:
 * - Statistic label and value display
 * - Trend indicator (up/down/neutral)
 * - Trend percentage value
 * - Optional icon display
 * - Color coding by trend direction
 * - Multiple size variants
 * - Clickable support
 * - Customizable styling
 * - Responsive layout
 *
 * Platform mappings:
 * - Android: Compose Card + Column layout
 * - iOS: ZStack with VStack for nested layout
 * - Web: Div with flexbox layout
 *
 * Usage:
 * ```kotlin
 * // Basic stat card with upward trend
 * StatCardComponent(
 *     label = "Revenue",
 *     value = "$25,000",
 *     trend = TrendDirection.Up,
 *     trendValue = 12.5f
 * )
 *
 * // Stat card with icon and downward trend
 * StatCardComponent(
 *     label = "Churn Rate",
 *     value = "3.2%",
 *     trend = TrendDirection.Down,
 *     trendValue = 2.1f,
 *     icon = "chart-down"
 * )
 *
 * // Stat card with neutral trend
 * StatCardComponent(
 *     label = "Monthly Target",
 *     value = "80/100",
 *     trend = TrendDirection.Neutral,
 *     trendValue = 80.0f,
 *     size = StatCardSize.Large
 * )
 * ```
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 * @since 1.0.0
 */
data class StatCardComponent(
    /**
     * The label or title for the statistic (e.g., "Revenue", "Users", "Conversion Rate").
     *
     * Typically displayed above or beside the value.
     * Should be concise (1-3 words recommended).
     */
    val label: String,

    /**
     * The main statistic value to display (e.g., "$25,000", "15.2%", "1.2M").
     *
     * Can include units, currency symbols, or formatted values.
     * Should be prominent and easily readable.
     */
    val value: String,

    /**
     * The trend direction indicator.
     *
     * Typically color-coded:
     * - Up: green (positive)
     * - Down: red (negative)
     * - Neutral: gray or default color
     */
    val trend: TrendDirection = TrendDirection.Neutral,

    /**
     * The trend percentage or change value.
     *
     * Displayed alongside the trend direction (e.g., "+12.5%" or "-2.1%").
     * Can be null if trend should not show a numeric value.
     */
    val trendValue: Float? = null,

    /**
     * Optional icon name for visual representation.
     *
     * Icons from standard icon libraries (Material Icons, Feather, etc.).
     * Examples: "trending-up", "chart-bar", "users", "dollar-sign"
     *
     * If null, no icon is displayed.
     */
    val icon: String? = null,

    /**
     * Size variant of the stat card.
     *
     * Controls overall dimensions and text sizes.
     */
    val size: StatCardSize = StatCardSize.Medium,

    /**
     * Whether the card should respond to click events.
     *
     * If true, visual feedback (hover, ripple) should be shown.
     */
    val clickable: Boolean = false,

    /**
     * Comparison or context label (optional).
     *
     * Displayed below the value for additional context
     * (e.g., "vs last month", "target: 100").
     *
     * If null, not displayed.
     */
    val subtitle: String? = null,

    /**
     * Unique identifier for this component instance.
     *
     * Used for tracking, state management, and testing.
     */
    val id: String? = null,

    /**
     * Styling configuration for this component.
     *
     * Allows custom colors, fonts, spacing, and effects.
     */
    val style: Any? = null,

    /**
     * List of modifiers for behavior decoration.
     *
     * Can include click handlers, animations, accessibility features.
     */
    val modifiers: List<Any> = emptyList(),

    /**
     * Optional click handler for the card.
     *
     * Invoked when the card is clicked and [clickable] is true.
     */
    val onClick: (() -> Unit)? = null
) {
    init {
        // Validation: label must not be empty
        require(label.isNotBlank()) {
            "StatCard label must not be empty"
        }

        // Validation: value must not be empty
        require(value.isNotBlank()) {
            "StatCard value must not be empty"
        }

        // Validation: trendValue must be non-negative if provided
        if (trendValue != null) {
            require(trendValue >= 0f) {
                "StatCard trendValue must be non-negative (got: $trendValue)"
            }
        }

        // Validation: if trend is Neutral, trendValue should be null or ignored
        if (trend == TrendDirection.Neutral) {
            require(trendValue == null || trendValue == 0f) {
                "StatCard with Neutral trend should have trendValue null or 0"
            }
        }

        // Validation: if clickable is true and no onClick handler, warn (but don't fail)
        // This is handled at runtime by the renderer
    }

    /**
     * Get formatted trend display string.
     *
     * @return Formatted trend string like "+12.5%" or "-2.1%", or empty if no trend value
     */
    val formattedTrend: String
        get() {
            if (trendValue == null || trendValue == 0f) return ""

            val sign = when (trend) {
                TrendDirection.Up -> "+"
                TrendDirection.Down -> "-"
                TrendDirection.Neutral -> ""
            }

            return "$sign$trendValue%"
        }

    /**
     * Get trend color code based on direction.
     *
     * Allows renderers to apply semantic coloring without explicit color parameters.
     */
    val trendColor: String
        get() = when (trend) {
            TrendDirection.Up -> "#4CAF50"        // Green
            TrendDirection.Down -> "#F44336"       // Red
            TrendDirection.Neutral -> "#9E9E9E"    // Gray
        }

    /**
     * Check if this stat card should display trend information.
     */
    val hasTrend: Boolean
        get() = trend != TrendDirection.Neutral && trendValue != null && trendValue > 0f
}

/**
 * Trend direction indicator for stat cards.
 *
 * Used to show whether a statistic is trending up, down, or remaining neutral.
 * Typically color-coded for semantic meaning.
 */
enum class TrendDirection {
    /**
     * Positive trend - value is increasing or improving.
     * Usually displayed in green.
     */
    Up,

    /**
     * Negative trend - value is decreasing or declining.
     * Usually displayed in red.
     */
    Down,

    /**
     * Neutral trend - value is stable or trend is not applicable.
     * Usually displayed in gray or default color.
     */
    Neutral
}

/**
 * Size variants for stat cards.
 *
 * Controls overall dimensions, padding, text sizes, and spacing.
 */
enum class StatCardSize {
    /**
     * Small stat card (compact, minimal spacing).
     * Suitable for dense dashboards or small screens.
     * Typical dimensions: 120dp x 80dp
     */
    Small,

    /**
     * Medium stat card (default, balanced spacing).
     * Suitable for standard dashboards and most use cases.
     * Typical dimensions: 160dp x 100dp
     */
    Medium,

    /**
     * Large stat card (spacious, prominent).
     * Suitable for hero sections or high-importance metrics.
     * Typical dimensions: 200dp x 140dp
     */
    Large,

    /**
     * Extra large stat card (maximum prominence).
     * Suitable for featured metrics or full-width displays.
     * Typical dimensions: 240dp x 160dp
     */
    XLarge
}

/**
 * Common stat card presets and factory methods.
 *
 * Provides convenient builders for common stat card patterns
 * and use cases.
 */
object StatCardPresets {
    /**
     * Revenue stat card with upward trend.
     *
     * @param value Revenue value (e.g., "$25,000")
     * @param trend Trend percentage (e.g., 12.5f)
     * @return Configured StatCardComponent
     */
    fun revenue(value: String, trend: Float) = StatCardComponent(
        label = "Revenue",
        value = value,
        trend = TrendDirection.Up,
        trendValue = trend,
        icon = "dollar-sign"
    )

    /**
     * User count stat card.
     *
     * @param count User count (e.g., "15,234")
     * @param trend Growth percentage (e.g., 5.2f)
     * @return Configured StatCardComponent
     */
    fun users(count: String, trend: Float) = StatCardComponent(
        label = "Users",
        value = count,
        trend = TrendDirection.Up,
        trendValue = trend,
        icon = "users"
    )

    /**
     * Conversion rate stat card.
     *
     * @param rate Conversion rate (e.g., "3.45%")
     * @param trend Rate change (e.g., 0.5f)
     * @return Configured StatCardComponent
     */
    fun conversionRate(rate: String, trend: Float) = StatCardComponent(
        label = "Conversion Rate",
        value = rate,
        trend = if (trend > 0) TrendDirection.Up else TrendDirection.Down,
        trendValue = kotlin.math.abs(trend),
        icon = "trending-up"
    )

    /**
     * Churn rate stat card with downward trend (lower is better).
     *
     * @param rate Churn rate (e.g., "2.1%")
     * @param change Rate change (negative is good)
     * @return Configured StatCardComponent
     */
    fun churnRate(rate: String, change: Float) = StatCardComponent(
        label = "Churn Rate",
        value = rate,
        trend = if (change < 0) TrendDirection.Down else TrendDirection.Up,
        trendValue = kotlin.math.abs(change),
        icon = "trending-down"
    )

    /**
     * Average order value stat card.
     *
     * @param value AOV value (e.g., "$85.50")
     * @param trend Change percentage (e.g., 3.2f)
     * @return Configured StatCardComponent
     */
    fun averageOrderValue(value: String, trend: Float) = StatCardComponent(
        label = "Avg Order Value",
        value = value,
        trend = TrendDirection.Up,
        trendValue = trend,
        icon = "shopping-cart"
    )

    /**
     * Engagement rate stat card.
     *
     * @param rate Engagement rate (e.g., "42.5%")
     * @param trend Change percentage (e.g., 8.1f)
     * @return Configured StatCardComponent
     */
    fun engagementRate(rate: String, trend: Float) = StatCardComponent(
        label = "Engagement Rate",
        value = rate,
        trend = TrendDirection.Up,
        trendValue = trend,
        icon = "activity"
    )

    /**
     * Performance score stat card (0-100).
     *
     * @param score Performance score (e.g., "85")
     * @param target Target score (e.g., "100")
     * @return Configured StatCardComponent
     */
    fun performanceScore(score: String, target: String = "100") = StatCardComponent(
        label = "Performance",
        value = score,
        trend = TrendDirection.Neutral,
        subtitle = "Target: $target",
        icon = "gauge"
    )

    /**
     * Task completion stat card.
     *
     * @param completed Tasks completed (e.g., "28")
     * @param total Total tasks (e.g., "35")
     * @return Configured StatCardComponent
     */
    fun taskCompletion(completed: String, total: String) = StatCardComponent(
        label = "Tasks Completed",
        value = completed,
        trend = TrendDirection.Neutral,
        subtitle = "of $total",
        icon = "check-circle"
    )

    /**
     * System uptime stat card.
     *
     * @param uptime Uptime percentage (e.g., "99.95%")
     * @return Configured StatCardComponent
     */
    fun uptime(uptime: String) = StatCardComponent(
        label = "System Uptime",
        value = uptime,
        trend = TrendDirection.Neutral,
        icon = "server"
    )

    /**
     * Active sessions stat card.
     *
     * @param sessions Active session count (e.g., "1,234")
     * @param change Change from previous period (e.g., 15.5f)
     * @return Configured StatCardComponent
     */
    fun activeSessions(sessions: String, change: Float) = StatCardComponent(
        label = "Active Sessions",
        value = sessions,
        trend = TrendDirection.Up,
        trendValue = change,
        icon = "log-in"
    )

    /**
     * Data storage stat card.
     *
     * @param used Used storage (e.g., "45.2 GB")
     * @param total Total storage (e.g., "100 GB")
     * @return Configured StatCardComponent
     */
    fun storage(used: String, total: String) = StatCardComponent(
        label = "Storage",
        value = used,
        trend = TrendDirection.Neutral,
        subtitle = "of $total",
        icon = "hard-drive"
    )

    /**
     * Error rate stat card (lower is better).
     *
     * @param rate Error rate (e.g., "0.5%")
     * @param change Change from previous period (negative is good)
     * @return Configured StatCardComponent
     */
    fun errorRate(rate: String, change: Float) = StatCardComponent(
        label = "Error Rate",
        value = rate,
        trend = if (change < 0) TrendDirection.Down else TrendDirection.Up,
        trendValue = kotlin.math.abs(change),
        icon = "alert-triangle"
    )
}
