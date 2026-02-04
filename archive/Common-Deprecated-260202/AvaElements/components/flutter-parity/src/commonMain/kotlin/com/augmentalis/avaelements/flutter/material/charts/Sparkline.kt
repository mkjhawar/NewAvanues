package com.augmentalis.avaelements.flutter.material.charts

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Sparkline component - Flutter Material parity
 *
 * A miniature inline chart showing trends over time, rendered using Canvas.
 * Perfect for dashboards and KPI displays.
 *
 * **Flutter Equivalent:** `flutter_sparkline` package
 * **Material Design 3:** Custom data visualization component with Material theming
 *
 * ## Features
 * - Compact inline visualization
 * - Line or area rendering
 * - Trend indicator (up/down)
 * - Min/max highlighting
 * - Custom colors
 * - Smooth animations
 * - Accessibility support (TalkBack)
 * - WCAG 2.1 Level AA compliant
 * - Dark mode support
 *
 * ## Usage Example
 * ```kotlin
 * Sparkline(
 *     data = listOf(10f, 15f, 12f, 18f, 20f, 17f, 22f),
 *     color = "#4CAF50",
 *     width = 100f,
 *     height = 30f,
 *     showArea = true,
 *     showTrend = true
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property data List of values to display
 * @property color Hex color string for the line (e.g., "#2196F3")
 * @property width Chart width in dp
 * @property height Chart height in dp
 * @property lineWidth Line width in dp
 * @property showArea Whether to fill area under the line
 * @property areaOpacity Opacity of the filled area (0.0-1.0)
 * @property showPoints Whether to show data points
 * @property pointSize Size of data points in dp
 * @property highlightMin Whether to highlight minimum value
 * @property highlightMax Whether to highlight maximum value
 * @property showTrend Whether to show trend indicator
 * @property animated Whether to animate the sparkline
 * @property animationDuration Animation duration in milliseconds
 * @property contentDescription Accessibility description
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class Sparkline(
    override val type: String = "Sparkline",
    override val id: String? = null,
    val data: List<Float>,
    val color: String = "#2196F3",
    val width: Float = 100f,
    val height: Float = 30f,
    val lineWidth: Float = 2f,
    val showArea: Boolean = false,
    val areaOpacity: Float = 0.2f,
    val showPoints: Boolean = false,
    val pointSize: Float = 4f,
    val highlightMin: Boolean = false,
    val highlightMax: Boolean = false,
    val showTrend: Boolean = false,
    val animated: Boolean = true,
    val animationDuration: Int = 300,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Trend direction
     */
    enum class Trend {
        UP,
        DOWN,
        FLAT
    }

    /**
     * Get the trend based on first and last values
     */
    fun getTrend(): Trend {
        if (data.size < 2) return Trend.FLAT

        val first = data.first()
        val last = data.last()
        val threshold = 0.01f // Consider values within 1% as flat

        return when {
            last > first * (1 + threshold) -> Trend.UP
            last < first * (1 - threshold) -> Trend.DOWN
            else -> Trend.FLAT
        }
    }

    /**
     * Get percentage change from first to last value
     */
    fun getPercentageChange(): Float {
        if (data.size < 2) return 0f

        val first = data.first()
        if (first == 0f) return 0f

        val last = data.last()
        return ((last - first) / first) * 100f
    }

    /**
     * Get min value and its index
     */
    fun getMin(): Pair<Float, Int>? {
        if (data.isEmpty()) return null
        val min = data.minOrNull() ?: return null
        val index = data.indexOf(min)
        return min to index
    }

    /**
     * Get max value and its index
     */
    fun getMax(): Pair<Float, Int>? {
        if (data.isEmpty()) return null
        val max = data.maxOrNull() ?: return null
        val index = data.indexOf(max)
        return max to index
    }

    /**
     * Get value range
     */
    fun getRange(): Pair<Float, Float> {
        if (data.isEmpty()) return 0f to 0f
        return (data.minOrNull() ?: 0f) to (data.maxOrNull() ?: 0f)
    }

    /**
     * Get accessibility description
     */
    fun getAccessibilityDescription(): String {
        if (contentDescription != null) return contentDescription

        val trend = getTrend()
        val trendText = when (trend) {
            Trend.UP -> "trending up"
            Trend.DOWN -> "trending down"
            Trend.FLAT -> "stable"
        }

        val percentChange = getPercentageChange()
        val changeText = if (percentChange != 0f) {
            val absChange = kotlin.math.abs(percentChange)
            val rounded = (absChange * 10).toInt() / 10.0
            " by $rounded%"
        } else {
            ""
        }

        val (min, max) = getRange()
        return "Sparkline chart with ${data.size} data points, $trendText$changeText. Range: $min to $max"
    }

    /**
     * Validate sparkline data
     */
    fun isValid(): Boolean {
        return data.isNotEmpty()
    }

    companion object {
        /** Default width in dp */
        const val DEFAULT_WIDTH = 100f

        /** Default height in dp */
        const val DEFAULT_HEIGHT = 30f

        /** Minimum data points for meaningful display */
        const val MIN_DATA_POINTS = 2

        /** Maximum data points for optimal performance */
        const val MAX_DATA_POINTS = 100

        /**
         * Create a simple sparkline
         */
        fun simple(
            data: List<Float>,
            color: String = "#2196F3"
        ) = Sparkline(
            data = data,
            color = color
        )

        /**
         * Create an area sparkline
         */
        fun area(
            data: List<Float>,
            color: String = "#2196F3"
        ) = Sparkline(
            data = data,
            color = color,
            showArea = true
        )

        /**
         * Create a sparkline with trend indicator
         */
        fun withTrend(
            data: List<Float>,
            color: String = "#2196F3"
        ) = Sparkline(
            data = data,
            color = color,
            showTrend = true,
            highlightMin = true,
            highlightMax = true
        )
    }
}
