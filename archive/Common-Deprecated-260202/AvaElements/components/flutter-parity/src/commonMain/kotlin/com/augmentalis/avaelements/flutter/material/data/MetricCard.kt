package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * MetricCard component - Flutter Material parity
 *
 * A metric display card for showing a single measurement with optional comparison data.
 * Used in analytics dashboards and monitoring interfaces.
 *
 * **Flutter Equivalent:** Custom implementation
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Large metric value display
 * - Comparison with previous period
 * - Trend visualization
 * - Icon and color theming
 * - Sparkline support (visual trend)
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * MetricCard(
 *     title = "Active Users",
 *     value = "12,450",
 *     unit = "users",
 *     comparison = "vs last week",
 *     change = "+8.5%",
 *     changeType = ChangeType.Positive,
 *     icon = "people"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Title of the metric
 * @property value The metric value
 * @property unit Optional unit of measurement
 * @property comparison Optional comparison text (e.g., "vs last week")
 * @property change Optional change value (e.g., "+8.5%")
 * @property changeType Type of change (positive, negative, neutral)
 * @property icon Optional icon identifier
 * @property color Optional color theme
 * @property showSparkline Whether to show mini trend line
 * @property sparklineData Optional data points for sparkline
 * @property contentDescription Accessibility description for TalkBack
 * @property onClick Optional click handler
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class MetricCard(
    override val type: String = "MetricCard",
    override val id: String? = null,
    val title: String,
    val value: String,
    val unit: String? = null,
    val comparison: String? = null,
    val change: String? = null,
    val changeType: ChangeType = ChangeType.Neutral,
    val icon: String? = null,
    val color: String? = null,
    val showSparkline: Boolean = false,
    val sparklineData: List<Float>? = null,
    val contentDescription: String? = null,
    @Transient
    val onClick: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Change type indicator
     */
    enum class ChangeType {
        /** Positive change (green) */
        Positive,

        /** Negative change (red) */
        Negative,

        /** Neutral change (default color) */
        Neutral
    }

    /**
     * Get formatted value with unit
     */
    fun getFormattedValue(): String {
        return if (unit != null) "$value $unit" else value
    }

    companion object {
        /**
         * Create a metric card with positive trend
         */
        fun positive(
            title: String,
            value: String,
            change: String,
            icon: String? = null
        ) = MetricCard(
            title = title,
            value = value,
            change = change,
            changeType = ChangeType.Positive,
            icon = icon
        )

        /**
         * Create a metric card with sparkline
         */
        fun withSparkline(
            title: String,
            value: String,
            sparklineData: List<Float>
        ) = MetricCard(
            title = title,
            value = value,
            showSparkline = true,
            sparklineData = sparklineData
        )
    }
}
