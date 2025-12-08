package com.augmentalis.avaelements.flutter.material.charts

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * RadarChart component - Flutter Material parity
 *
 * A radar/spider chart for multivariate data visualization using Canvas rendering.
 *
 * **Flutter Equivalent:** `fl_chart` RadarChart
 * **Material Design 3:** Custom data visualization component with Material theming
 *
 * ## Features
 * - Multiple data series overlay
 * - Configurable number of axes
 * - Gridlines and axis labels
 * - Filled areas with opacity
 * - Custom colors per series
 * - Smooth animations
 * - Accessibility support (TalkBack)
 * - WCAG 2.1 Level AA compliant
 * - Dark mode support
 *
 * ## Usage Example
 * ```kotlin
 * RadarChart(
 *     axes = listOf("Speed", "Power", "Defense", "Agility", "Intelligence"),
 *     series = listOf(
 *         RadarChart.RadarSeries(
 *             label = "Player 1",
 *             values = listOf(80f, 90f, 70f, 85f, 75f),
 *             color = "#2196F3"
 *         )
 *     ),
 *     title = "Character Stats"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property axes List of axis labels
 * @property series List of radar series to display
 * @property title Optional chart title
 * @property maxValue Maximum value for all axes
 * @property showLegend Whether to show the legend
 * @property showGrid Whether to show grid lines
 * @property gridLevels Number of grid levels
 * @property size Chart size in dp
 * @property fillOpacity Opacity of filled areas (0.0-1.0)
 * @property strokeWidth Width of the data lines
 * @property animated Whether to animate the chart
 * @property animationDuration Animation duration in milliseconds
 * @property contentDescription Accessibility description
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class RadarChart(
    override val type: String = "RadarChart",
    override val id: String? = null,
    val axes: List<String>,
    val series: List<RadarSeries>,
    val title: String? = null,
    val maxValue: Float = 100f,
    val showLegend: Boolean = true,
    val showGrid: Boolean = true,
    val gridLevels: Int = 5,
    val size: Float = 300f,
    val fillOpacity: Float = 0.3f,
    val strokeWidth: Float = 2f,
    val animated: Boolean = true,
    val animationDuration: Int = 500,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Data series for the radar chart
     *
     * @property label Series label for legend
     * @property values List of values (one per axis)
     * @property color Hex color string (e.g., "#2196F3")
     */
    data class RadarSeries(
        val label: String,
        val values: List<Float>,
        val color: String? = null
    ) {
        /**
         * Get effective color with fallback
         */
        fun getEffectiveColor(defaultColor: String = "#2196F3"): String {
            return color ?: defaultColor
        }

        /**
         * Get average value
         */
        fun getAverage(): Float {
            return values.average().toFloat()
        }
    }

    /**
     * Get angle for each axis (in degrees)
     */
    fun getAxisAngle(index: Int): Float {
        val axisCount = axes.size
        return (360f / axisCount) * index - 90f // Start from top
    }

    /**
     * Get accessibility description
     */
    fun getAccessibilityDescription(): String {
        if (contentDescription != null) return contentDescription

        val titlePart = title?.let { "$it. " } ?: ""
        val axisCount = axes.size
        val seriesCount = series.size

        val seriesDescriptions = series.joinToString(", ") { s ->
            val avg = s.getAverage()
            val rounded = (avg * 10).toInt() / 10.0
            "${s.label}: average $rounded"
        }

        return "${titlePart}Radar chart with $axisCount axes and $seriesCount series. $seriesDescriptions"
    }

    /**
     * Validate chart data
     */
    fun isValid(): Boolean {
        return axes.size >= 3 && series.isNotEmpty() && series.all { it.values.size == axes.size }
    }

    companion object {
        /** Default chart size in dp */
        const val DEFAULT_SIZE = 300f

        /** Minimum number of axes */
        const val MIN_AXES = 3

        /** Maximum number of axes for optimal display */
        const val MAX_AXES = 12

        /**
         * Create a simple radar chart with one series
         */
        fun simple(
            axes: List<String>,
            values: List<Float>,
            label: String = "Data",
            title: String? = null
        ) = RadarChart(
            axes = axes,
            series = listOf(
                RadarSeries(
                    label = label,
                    values = values,
                    color = "#2196F3"
                )
            ),
            title = title
        )
    }
}
