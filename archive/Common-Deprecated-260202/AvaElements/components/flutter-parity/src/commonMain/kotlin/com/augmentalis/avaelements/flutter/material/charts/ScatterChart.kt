package com.augmentalis.avaelements.flutter.material.charts

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ScatterChart component - Flutter Material parity
 *
 * A scatter plot with support for multiple data series and bubble sizing.
 * Uses Vico library on Android for smooth rendering and Material3 theming.
 *
 * **Flutter Equivalent:** `fl_chart` ScatterChart
 * **Material Design 3:** Data visualization component with Material theming
 *
 * ## Features
 * - Multiple data series
 * - Variable point sizes (bubble chart mode)
 * - Grid lines and axis labels
 * - Interactive point selection
 * - Legend support
 * - Trend lines (optional)
 * - Animations (500ms default)
 * - Accessibility support (TalkBack)
 * - WCAG 2.1 Level AA compliant
 * - Dark mode support
 *
 * ## Usage Example
 * ```kotlin
 * ScatterChart(
 *     series = listOf(
 *         ScatterChart.ScatterSeries(
 *             label = "Group A",
 *             points = listOf(
 *                 ScatterChart.ScatterPoint(10f, 20f, size = 5f),
 *                 ScatterChart.ScatterPoint(15f, 25f, size = 8f)
 *             ),
 *             color = "#2196F3"
 *         )
 *     ),
 *     title = "Correlation Analysis"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property series List of scatter series to display
 * @property title Optional chart title
 * @property xAxisLabel Optional X-axis label
 * @property yAxisLabel Optional Y-axis label
 * @property showLegend Whether to show the legend
 * @property showGrid Whether to show grid lines
 * @property showTrendLine Whether to show trend line
 * @property pointSize Default point size in dp
 * @property animated Whether to animate the chart
 * @property animationDuration Animation duration in milliseconds
 * @property height Optional fixed height in dp
 * @property minX Optional minimum X-axis value
 * @property maxX Optional maximum X-axis value
 * @property minY Optional minimum Y-axis value
 * @property maxY Optional maximum Y-axis value
 * @property contentDescription Accessibility description
 * @property onPointClick Callback when a point is clicked
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class ScatterChart(
    override val type: String = "ScatterChart",
    override val id: String? = null,
    val series: List<ScatterSeries>,
    val title: String? = null,
    val xAxisLabel: String? = null,
    val yAxisLabel: String? = null,
    val showLegend: Boolean = true,
    val showGrid: Boolean = true,
    val showTrendLine: Boolean = false,
    val pointSize: Float = 6f,
    val animated: Boolean = true,
    val animationDuration: Int = 500,
    val height: Float? = null,
    val minX: Float? = null,
    val maxX: Float? = null,
    val minY: Float? = null,
    val maxY: Float? = null,
    val contentDescription: String? = null,
    @Transient
    val onPointClick: ((seriesIndex: Int, pointIndex: Int, point: ScatterPoint) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Data series for the scatter chart
     *
     * @property label Series label for legend
     * @property points List of scatter points
     * @property color Hex color string (e.g., "#2196F3")
     */
    data class ScatterSeries(
        val label: String,
        val points: List<ScatterPoint>,
        val color: String? = null
    ) {
        /**
         * Get effective color with fallback
         */
        fun getEffectiveColor(defaultColor: String = "#2196F3"): String {
            return color ?: defaultColor
        }

        /**
         * Get X range
         */
        fun getXRange(): Pair<Float, Float> {
            if (points.isEmpty()) return 0f to 0f
            return points.minOf { it.x } to points.maxOf { it.x }
        }

        /**
         * Get Y range
         */
        fun getYRange(): Pair<Float, Float> {
            if (points.isEmpty()) return 0f to 0f
            return points.minOf { it.y } to points.maxOf { it.y }
        }
    }

    /**
     * Individual point in a scatter chart
     *
     * @property x X-axis value
     * @property y Y-axis value
     * @property size Point size multiplier (default 1.0)
     * @property label Optional label for this point
     */
    data class ScatterPoint(
        val x: Float,
        val y: Float,
        val size: Float = 1f,
        val label: String? = null
    ) {
        /**
         * Get accessibility description for this point
         */
        fun getAccessibilityDescription(): String {
            val labelPart = label?.let { "$it: " } ?: ""
            return "${labelPart}X: $x, Y: $y"
        }
    }

    /**
     * Get combined X-axis range from all series
     */
    fun getXRange(): Pair<Float, Float> {
        if (series.isEmpty()) return 0f to 0f
        val ranges = series.map { it.getXRange() }
        val minX = minX ?: ranges.minOf { it.first }
        val maxX = maxX ?: ranges.maxOf { it.second }
        return minX to maxX
    }

    /**
     * Get combined Y-axis range from all series
     */
    fun getYRange(): Pair<Float, Float> {
        if (series.isEmpty()) return 0f to 0f
        val ranges = series.map { it.getYRange() }
        val minY = minY ?: ranges.minOf { it.first }
        val maxY = maxY ?: ranges.maxOf { it.second }
        return minY to maxY
    }

    /**
     * Get accessibility description
     */
    fun getAccessibilityDescription(): String {
        if (contentDescription != null) return contentDescription

        val seriesCount = series.size
        val pointCount = series.sumOf { it.points.size }
        val titlePart = title?.let { "$it. " } ?: ""

        return "${titlePart}Scatter chart with $seriesCount series and $pointCount total points"
    }

    /**
     * Validate chart data
     */
    fun isValid(): Boolean {
        return series.isNotEmpty() && series.all { it.points.isNotEmpty() }
    }

    companion object {
        /** Default chart height in dp */
        const val DEFAULT_HEIGHT = 300f

        /** Default point size in dp */
        const val DEFAULT_POINT_SIZE = 6f

        /** Maximum points for optimal performance */
        const val MAX_POINTS = 1000

        /**
         * Create a simple scatter chart with one series
         */
        fun simple(
            points: List<ScatterPoint>,
            label: String = "Data",
            color: String = "#2196F3",
            title: String? = null
        ) = ScatterChart(
            series = listOf(
                ScatterSeries(
                    label = label,
                    points = points,
                    color = color
                )
            ),
            title = title
        )

        /**
         * Create a bubble chart (scatter with variable sizes)
         */
        fun bubble(
            points: List<ScatterPoint>,
            label: String = "Data",
            color: String = "#2196F3",
            title: String? = null
        ) = simple(points, label, color, title)
    }
}
