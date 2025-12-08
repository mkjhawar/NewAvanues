package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * KPI component - Flutter Material parity
 *
 * A Key Performance Indicator card for displaying important metrics with targets and trends.
 * Commonly used in executive dashboards and business intelligence applications.
 *
 * **Flutter Equivalent:** Custom implementation
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Large value display with title
 * - Target comparison
 * - Progress indicator
 * - Trend indicators (up, down, neutral)
 * - Icon support
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * KPI(
 *     title = "Monthly Sales",
 *     value = "$125,000",
 *     target = "$150,000",
 *     progress = 0.833f,
 *     trend = TrendType.Up,
 *     icon = "trending_up",
 *     subtitle = "83% of target"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Title of the KPI
 * @property value Current value
 * @property target Optional target value
 * @property progress Optional progress value (0.0 to 1.0)
 * @property trend Trend indicator type
 * @property icon Optional icon identifier
 * @property subtitle Optional subtitle text
 * @property showProgressBar Whether to show progress bar
 * @property contentDescription Accessibility description for TalkBack
 * @property onClick Optional click handler
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class KPI(
    override val type: String = "KPI",
    override val id: String? = null,
    val title: String,
    val value: String,
    val target: String? = null,
    val progress: Float? = null,
    val trend: TrendType = TrendType.Neutral,
    val icon: String? = null,
    val subtitle: String? = null,
    val showProgressBar: Boolean = true,
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
     * Trend type indicator
     */
    enum class TrendType {
        /** Upward trend (positive) */
        Up,

        /** Downward trend (negative) */
        Down,

        /** No significant trend */
        Neutral
    }

    /**
     * Get progress percentage as string
     */
    fun getProgressPercentage(): String? {
        return progress?.let { "${(it * 100).toInt()}%" }
    }

    /**
     * Check if KPI is meeting target
     */
    fun isMeetingTarget(): Boolean {
        return progress?.let { it >= 1.0f } ?: false
    }

    companion object {
        /**
         * Create a KPI with upward trend
         */
        fun trending(
            title: String,
            value: String,
            trend: TrendType = TrendType.Up
        ) = KPI(
            title = title,
            value = value,
            trend = trend
        )

        /**
         * Create a KPI with target and progress
         */
        fun withTarget(
            title: String,
            value: String,
            target: String,
            progress: Float
        ) = KPI(
            title = title,
            value = value,
            target = target,
            progress = progress,
            trend = if (progress >= 1.0f) TrendType.Up else TrendType.Neutral
        )
    }
}
