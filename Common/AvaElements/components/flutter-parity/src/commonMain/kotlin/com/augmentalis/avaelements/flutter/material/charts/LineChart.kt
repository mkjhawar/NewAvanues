package com.augmentalis.avaelements.flutter.material.charts

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * LineChart component - Flutter Material parity
 *
 * A line chart with support for multiple series, animations, and interactivity.
 * Uses Vico library on Android for smooth rendering and Material3 theming.
 *
 * **Flutter Equivalent:** `fl_chart` LineChart
 * **Material Design 3:** Data visualization component with Material theming
 *
 * ## Features
 * - Multiple data series with different colors
 * - Smooth line interpolation
 * - Grid lines and axis labels
 * - Interactive point selection
 * - Legend support
 * - Animations (500ms default)
 * - Touch/hover interactions
 * - Accessibility support (TalkBack)
 * - WCAG 2.1 Level AA compliant
 * - Dark mode support
 *
 * ## Usage Example
 * ```kotlin
 * LineChart(
 *     series = listOf(
 *         LineChart.ChartSeries(
 *             label = "Revenue",
 *             data = listOf(
 *                 ChartPoint(0f, 100f),
 *                 ChartPoint(1f, 150f),
 *                 ChartPoint(2f, 125f)
 *             ),
 *             color = "#2196F3"
 *         )
 *     ),
 *     title = "Revenue Over Time",
 *     xAxisLabel = "Month",
 *     yAxisLabel = "Amount ($)",
 *     showLegend = true,
 *     animated = true
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property series List of data series to display
 * @property title Optional chart title
 * @property xAxisLabel Optional X-axis label
 * @property yAxisLabel Optional Y-axis label
 * @property showLegend Whether to show the legend
 * @property showGrid Whether to show grid lines
 * @property showPoints Whether to show data points
 * @property lineWidth Width of the line in dp (default 2)
 * @property pointSize Size of data points in dp (default 6)
 * @property animated Whether to animate the chart
 * @property animationDuration Animation duration in milliseconds
 * @property height Optional fixed height in dp
 * @property minY Optional minimum Y-axis value
 * @property maxY Optional maximum Y-axis value
 * @property enableZoom Whether to enable pinch-to-zoom
 * @property enablePan Whether to enable panning
 * @property contentDescription Accessibility description
 * @property onPointClick Callback when a point is clicked
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class LineChart(
    override val type: String = "LineChart",
    override val id: String? = null,
    val series: List<ChartSeries> = emptyList(),
    val title: String? = null,
    val xAxisLabel: String? = null,
    val yAxisLabel: String? = null,
    val showLegend: Boolean = true,
    val showGrid: Boolean = true,
    val showPoints: Boolean = true,
    val lineWidth: Float = 2f,
    val pointSize: Float = 6f,
    val animated: Boolean = true,
    val animationDuration: Int = 500,
    val height: Float? = null,
    val minY: Float? = null,
    val maxY: Float? = null,
    val enableZoom: Boolean = false,
    val enablePan: Boolean = false,
    val contentDescription: String? = null,
    @Transient
    val onPointClick: ((seriesIndex: Int, pointIndex: Int, point: ChartPoint) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Data series for the line chart
     *
     * @property label Series label for legend
     * @property data List of data points
     * @property color Hex color string (e.g., "#2196F3")
     * @property strokeWidth Custom line width for this series
     * @property fillArea Whether to fill area under the line
     * @property dashed Whether to use dashed line
     */
    data class ChartSeries(
        val label: String,
        val data: List<ChartPoint>,
        val color: String? = null,
        val strokeWidth: Float? = null,
        val fillArea: Boolean = false,
        val dashed: Boolean = false
    ) {
        /**
         * Get effective color with fallback
         */
        fun getEffectiveColor(defaultColor: String = "#2196F3"): String {
            return color ?: defaultColor
        }

        /**
         * Get data range
         */
        fun getYRange(): Pair<Float, Float> {
            if (data.isEmpty()) return 0f to 0f
            val minY = data.minOf { it.y }
            val maxY = data.maxOf { it.y }
            return minY to maxY
        }
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
        val dataPointsCount = series.sumOf { it.data.size }
        val titlePart = title?.let { "$it. " } ?: ""

        return "${titlePart}Line chart with $seriesCount series and $dataPointsCount total data points"
    }

    /**
     * Validate chart data
     */
    fun isValid(): Boolean {
        return series.isNotEmpty() && series.all { it.data.isNotEmpty() }
    }

    companion object {
        /** Default chart height in dp */
        const val DEFAULT_HEIGHT = 300f

        /** Default animation duration in milliseconds */
        const val DEFAULT_ANIMATION_DURATION = 500

        /** Maximum number of series for optimal performance */
        const val MAX_SERIES_COUNT = 10

        /** Maximum points per series for optimal performance */
        const val MAX_POINTS_PER_SERIES = 1000

        /**
         * Create a simple single-series line chart
         */
        fun simple(
            data: List<ChartPoint>,
            label: String = "Data",
            color: String = "#2196F3",
            title: String? = null
        ) = LineChart(
            series = listOf(
                ChartSeries(
                    label = label,
                    data = data,
                    color = color
                )
            ),
            title = title
        )

        /**
         * Create a line chart with area fill
         */
        fun area(
            data: List<ChartPoint>,
            label: String = "Data",
            color: String = "#2196F3",
            title: String? = null
        ) = LineChart(
            series = listOf(
                ChartSeries(
                    label = label,
                    data = data,
                    color = color,
                    fillArea = true
                )
            ),
            title = title
        )
    }
}

/**
 * Shared data point class for all chart types
 *
 * @property x X-axis value
 * @property y Y-axis value
 * @property label Optional label for this point
 * @property metadata Optional metadata map for custom data
 */
data class ChartPoint(
    val x: Float,
    val y: Float,
    val label: String? = null,
    val metadata: Map<String, Any>? = null
) {
    /**
     * Get accessibility description for this point
     */
    fun getAccessibilityDescription(): String {
        val labelPart = label?.let { "$it: " } ?: ""
        return "${labelPart}X: $x, Y: $y"
    }
}
