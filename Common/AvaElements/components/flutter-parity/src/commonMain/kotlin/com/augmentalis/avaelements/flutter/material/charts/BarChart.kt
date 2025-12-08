package com.augmentalis.avaelements.flutter.material.charts

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * BarChart component - Flutter Material parity
 *
 * A bar chart with support for grouped and stacked bars, animations, and interactivity.
 * Uses Vico library on Android for smooth rendering and Material3 theming.
 *
 * **Flutter Equivalent:** `fl_chart` BarChart
 * **Material Design 3:** Data visualization component with Material theming
 *
 * ## Features
 * - Single or grouped bars
 * - Stacked bar mode
 * - Horizontal or vertical orientation
 * - Custom bar colors and widths
 * - Grid lines and axis labels
 * - Interactive bar selection
 * - Legend support
 * - Animations (500ms default)
 * - Accessibility support (TalkBack)
 * - WCAG 2.1 Level AA compliant
 * - Dark mode support
 *
 * ## Usage Example
 * ```kotlin
 * BarChart(
 *     data = listOf(
 *         BarChart.BarGroup(
 *             label = "Q1",
 *             bars = listOf(
 *                 BarChart.Bar(value = 100f, label = "Revenue", color = "#2196F3"),
 *                 BarChart.Bar(value = 80f, label = "Costs", color = "#F44336")
 *             )
 *         )
 *     ),
 *     title = "Quarterly Performance",
 *     mode = BarChart.BarMode.Grouped,
 *     orientation = BarChart.Orientation.Vertical
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property data List of bar groups to display
 * @property title Optional chart title
 * @property xAxisLabel Optional X-axis label
 * @property yAxisLabel Optional Y-axis label
 * @property mode Bar display mode (grouped or stacked)
 * @property orientation Chart orientation (vertical or horizontal)
 * @property showLegend Whether to show the legend
 * @property showGrid Whether to show grid lines
 * @property showValues Whether to show values on bars
 * @property barWidth Width of bars in dp
 * @property groupSpacing Spacing between bar groups
 * @property barSpacing Spacing between bars in a group
 * @property animated Whether to animate the chart
 * @property animationDuration Animation duration in milliseconds
 * @property height Optional fixed height in dp
 * @property minValue Optional minimum value
 * @property maxValue Optional maximum value
 * @property contentDescription Accessibility description
 * @property onBarClick Callback when a bar is clicked
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class BarChart(
    override val type: String = "BarChart",
    override val id: String? = null,
    val data: List<BarGroup> = emptyList(),
    val title: String? = null,
    val xAxisLabel: String? = null,
    val yAxisLabel: String? = null,
    val mode: BarMode = BarMode.Grouped,
    val orientation: Orientation = Orientation.Vertical,
    val showLegend: Boolean = true,
    val showGrid: Boolean = true,
    val showValues: Boolean = false,
    val barWidth: Float = 40f,
    val groupSpacing: Float = 20f,
    val barSpacing: Float = 4f,
    val animated: Boolean = true,
    val animationDuration: Int = 500,
    val height: Float? = null,
    val minValue: Float? = null,
    val maxValue: Float? = null,
    val contentDescription: String? = null,
    @Transient
    val onBarClick: ((groupIndex: Int, barIndex: Int, bar: Bar) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Bar display mode
     */
    enum class BarMode {
        /** Bars in a group are displayed side by side */
        Grouped,

        /** Bars in a group are stacked on top of each other */
        Stacked
    }

    /**
     * Chart orientation
     */
    enum class Orientation {
        /** Vertical bars (default) */
        Vertical,

        /** Horizontal bars */
        Horizontal
    }

    /**
     * A group of bars sharing the same X-axis position
     *
     * @property label X-axis label for this group
     * @property bars List of bars in this group
     */
    data class BarGroup(
        val label: String,
        val bars: List<Bar>
    ) {
        /**
         * Get total value (for stacked mode)
         */
        fun getTotalValue(): Float {
            return bars.sumOf { it.value.toDouble() }.toFloat()
        }

        /**
         * Get max value (for grouped mode)
         */
        fun getMaxValue(): Float {
            return bars.maxOfOrNull { it.value } ?: 0f
        }
    }

    /**
     * Individual bar in a bar chart
     *
     * @property value Bar value
     * @property label Bar label for legend
     * @property color Hex color string (e.g., "#2196F3")
     */
    data class Bar(
        val value: Float,
        val label: String? = null,
        val color: String? = null
    ) {
        /**
         * Get effective color with fallback
         */
        fun getEffectiveColor(defaultColor: String = "#2196F3"): String {
            return color ?: defaultColor
        }
    }

    /**
     * Get value range
     */
    fun getValueRange(): Pair<Float, Float> {
        if (data.isEmpty()) return 0f to 0f

        val values = when (mode) {
            BarMode.Grouped -> data.flatMap { group -> group.bars.map { it.value } }
            BarMode.Stacked -> data.map { it.getTotalValue() }
        }

        val min = minValue ?: 0f
        val max = maxValue ?: (values.maxOrNull() ?: 0f)

        return min to max
    }

    /**
     * Get all unique bar labels for legend
     */
    fun getBarLabels(): List<String> {
        return data.flatMap { group ->
            group.bars.mapNotNull { it.label }
        }.distinct()
    }

    /**
     * Get accessibility description
     */
    fun getAccessibilityDescription(): String {
        if (contentDescription != null) return contentDescription

        val groupCount = data.size
        val titlePart = title?.let { "$it. " } ?: ""
        val modePart = when (mode) {
            BarMode.Grouped -> "grouped"
            BarMode.Stacked -> "stacked"
        }
        val orientationPart = when (orientation) {
            Orientation.Vertical -> "vertical"
            Orientation.Horizontal -> "horizontal"
        }

        return "${titlePart}$modePart $orientationPart bar chart with $groupCount groups"
    }

    /**
     * Validate chart data
     */
    fun isValid(): Boolean {
        return data.isNotEmpty() && data.all { it.bars.isNotEmpty() }
    }

    companion object {
        /** Default chart height in dp */
        const val DEFAULT_HEIGHT = 300f

        /** Default animation duration in milliseconds */
        const val DEFAULT_ANIMATION_DURATION = 500

        /** Maximum number of groups for optimal performance */
        const val MAX_GROUPS = 50

        /** Maximum bars per group for optimal performance */
        const val MAX_BARS_PER_GROUP = 10

        /**
         * Create a simple single-bar chart
         */
        fun simple(
            values: List<Pair<String, Float>>,
            color: String = "#2196F3",
            title: String? = null
        ) = BarChart(
            data = values.map { (label, value) ->
                BarGroup(
                    label = label,
                    bars = listOf(Bar(value = value, color = color))
                )
            },
            title = title,
            mode = BarMode.Grouped
        )

        /**
         * Create a horizontal bar chart
         */
        fun horizontal(
            values: List<Pair<String, Float>>,
            color: String = "#2196F3",
            title: String? = null
        ) = simple(values, color, title).copy(
            orientation = Orientation.Horizontal
        )
    }
}
