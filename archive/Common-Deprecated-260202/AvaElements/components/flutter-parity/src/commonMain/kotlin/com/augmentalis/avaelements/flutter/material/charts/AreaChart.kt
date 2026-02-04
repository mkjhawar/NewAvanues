package com.augmentalis.avaelements.flutter.material.charts

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * AreaChart component - Flutter Material parity
 *
 * An area chart with filled regions under lines, supporting multiple series and stacking.
 * Uses Vico library on Android for smooth rendering and Material3 theming.
 *
 * **Flutter Equivalent:** `fl_chart` LineChart with filled area
 * **Material Design 3:** Data visualization component with Material theming
 *
 * ## Features
 * - Multiple data series with different colors
 * - Filled areas under lines
 * - Stacked area mode
 * - Gradient fills
 * - Grid lines and axis labels
 * - Interactive point selection
 * - Legend support
 * - Animations (500ms default)
 * - Accessibility support (TalkBack)
 * - WCAG 2.1 Level AA compliant
 * - Dark mode support
 *
 * ## Usage Example
 * ```kotlin
 * AreaChart(
 *     series = listOf(
 *         AreaChart.AreaSeries(
 *             label = "Revenue",
 *             data = listOf(
 *                 ChartPoint(0f, 100f),
 *                 ChartPoint(1f, 150f),
 *                 ChartPoint(2f, 125f)
 *             ),
 *             color = "#2196F3",
 *             fillOpacity = 0.3f
 *         )
 *     ),
 *     title = "Revenue Over Time",
 *     stacked = false
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property series List of area series to display
 * @property title Optional chart title
 * @property xAxisLabel Optional X-axis label
 * @property yAxisLabel Optional Y-axis label
 * @property stacked Whether to stack areas on top of each other
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
 * @property contentDescription Accessibility description
 * @property onPointClick Callback when a point is clicked
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class AreaChart(
    override val type: String = "AreaChart",
    override val id: String? = null,
    val series: List<AreaSeries> = emptyList(),
    val title: String? = null,
    val xAxisLabel: String? = null,
    val yAxisLabel: String? = null,
    val stacked: Boolean = false,
    val showLegend: Boolean = true,
    val showGrid: Boolean = true,
    val showPoints: Boolean = false,
    val lineWidth: Float = 2f,
    val pointSize: Float = 6f,
    val animated: Boolean = true,
    val animationDuration: Int = 500,
    val height: Float? = null,
    val minY: Float? = null,
    val maxY: Float? = null,
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
     * Data series for the area chart
     *
     * @property label Series label for legend
     * @property data List of data points
     * @property color Hex color string (e.g., "#2196F3")
     * @property fillOpacity Opacity of the filled area (0.0-1.0)
     * @property strokeWidth Custom line width for this series
     * @property gradient Whether to use gradient fill (top to bottom)
     */
    data class AreaSeries(
        val label: String,
        val data: List<ChartPoint>,
        val color: String? = null,
        val fillOpacity: Float = 0.3f,
        val strokeWidth: Float? = null,
        val gradient: Boolean = true
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

        val ranges = if (stacked) {
            // For stacked, calculate cumulative values
            val xValues = series.flatMap { it.data.map { point -> point.x } }.distinct().sorted()
            val maxStacked = xValues.maxOfOrNull { x ->
                series.sumOf { areaSeries ->
                    areaSeries.data.find { it.x == x }?.y?.toDouble() ?: 0.0
                }.toFloat()
            } ?: 0f
            listOf(0f to maxStacked)
        } else {
            series.map { it.getYRange() }
        }

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
        val stackedPart = if (stacked) "Stacked " else ""

        return "${titlePart}${stackedPart}Area chart with $seriesCount series and $dataPointsCount total data points"
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

        /** Default fill opacity */
        const val DEFAULT_FILL_OPACITY = 0.3f

        /**
         * Create a simple single-series area chart
         */
        fun simple(
            data: List<ChartPoint>,
            label: String = "Data",
            color: String = "#2196F3",
            title: String? = null
        ) = AreaChart(
            series = listOf(
                AreaSeries(
                    label = label,
                    data = data,
                    color = color
                )
            ),
            title = title
        )

        /**
         * Create a stacked area chart
         */
        fun stacked(
            series: List<AreaSeries>,
            title: String? = null
        ) = AreaChart(
            series = series,
            title = title,
            stacked = true
        )
    }
}
