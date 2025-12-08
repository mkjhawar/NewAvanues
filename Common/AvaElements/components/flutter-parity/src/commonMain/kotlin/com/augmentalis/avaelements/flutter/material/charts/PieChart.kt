package com.augmentalis.avaelements.flutter.material.charts

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * PieChart component - Flutter Material parity
 *
 * A pie/donut chart with support for slices, labels, and interactivity.
 * Renders using Canvas on Android with Material3 theming.
 *
 * **Flutter Equivalent:** `fl_chart` PieChart
 * **Material Design 3:** Data visualization component with Material theming
 *
 * ## Features
 * - Pie chart or donut chart modes
 * - Interactive slice selection
 * - Percentage labels
 * - Custom slice colors
 * - Legend support
 * - Animations (500ms default)
 * - Start angle customization
 * - Accessibility support (TalkBack)
 * - WCAG 2.1 Level AA compliant
 * - Dark mode support
 *
 * ## Usage Example
 * ```kotlin
 * PieChart(
 *     slices = listOf(
 *         PieChart.Slice(value = 30f, label = "Category A", color = "#2196F3"),
 *         PieChart.Slice(value = 45f, label = "Category B", color = "#4CAF50"),
 *         PieChart.Slice(value = 25f, label = "Category C", color = "#FF9800")
 *     ),
 *     title = "Market Share",
 *     donutMode = true,
 *     donutRatio = 0.6f
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property slices List of pie slices
 * @property title Optional chart title
 * @property donutMode Whether to display as donut chart
 * @property donutRatio Inner radius ratio (0.0-1.0) for donut mode
 * @property showLabels Whether to show slice labels
 * @property showPercentages Whether to show percentages on slices
 * @property showLegend Whether to show the legend
 * @property startAngle Start angle in degrees (0 = top)
 * @property size Chart size in dp
 * @property strokeWidth Stroke width for slice borders
 * @property animated Whether to animate the chart
 * @property animationDuration Animation duration in milliseconds
 * @property contentDescription Accessibility description
 * @property onSliceClick Callback when a slice is clicked
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class PieChart(
    override val type: String = "PieChart",
    override val id: String? = null,
    val slices: List<Slice> = emptyList(),
    val title: String? = null,
    val donutMode: Boolean = false,
    val donutRatio: Float = 0.6f,
    val showLabels: Boolean = true,
    val showPercentages: Boolean = true,
    val showLegend: Boolean = true,
    val startAngle: Float = -90f, // Start from top
    val size: Float = 250f,
    val strokeWidth: Float = 2f,
    val animated: Boolean = true,
    val animationDuration: Int = 500,
    val contentDescription: String? = null,
    @Transient
    val onSliceClick: ((sliceIndex: Int, slice: Slice) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Individual slice in a pie chart
     *
     * @property value Slice value (will be converted to percentage)
     * @property label Slice label
     * @property color Hex color string (e.g., "#2196F3")
     */
    data class Slice(
        val value: Float,
        val label: String,
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
     * Get total value of all slices
     */
    fun getTotalValue(): Float {
        return slices.sumOf { it.value.toDouble() }.toFloat()
    }

    /**
     * Get percentage for each slice
     */
    fun getPercentages(): List<Float> {
        val total = getTotalValue()
        if (total == 0f) return slices.map { 0f }
        return slices.map { (it.value / total) * 100f }
    }

    /**
     * Get sweep angle for each slice (in degrees)
     */
    fun getSweepAngles(): List<Float> {
        val total = getTotalValue()
        if (total == 0f) return slices.map { 0f }
        return slices.map { (it.value / total) * 360f }
    }

    /**
     * Get accessibility description
     */
    fun getAccessibilityDescription(): String {
        if (contentDescription != null) return contentDescription

        val titlePart = title?.let { "$it. " } ?: ""
        val chartType = if (donutMode) "Donut chart" else "Pie chart"
        val sliceCount = slices.size

        val percentages = getPercentages()
        val sliceDescriptions = slices.zip(percentages).joinToString(", ") { (slice, pct) ->
            val rounded = (pct * 10).toInt() / 10.0
            "${slice.label}: $rounded%"
        }

        return "$titlePart$chartType with $sliceCount slices. $sliceDescriptions"
    }

    /**
     * Validate chart data
     */
    fun isValid(): Boolean {
        return slices.isNotEmpty() && slices.all { it.value > 0 }
    }

    companion object {
        /** Default chart size in dp */
        const val DEFAULT_SIZE = 250f

        /** Default animation duration in milliseconds */
        const val DEFAULT_ANIMATION_DURATION = 500

        /** Maximum number of slices for optimal display */
        const val MAX_SLICES = 12

        /** Default donut ratio */
        const val DEFAULT_DONUT_RATIO = 0.6f

        /**
         * Create a simple pie chart
         */
        fun simple(
            values: List<Pair<String, Float>>,
            colors: List<String>? = null,
            title: String? = null
        ): PieChart {
            val defaultColors = listOf(
                "#2196F3", "#4CAF50", "#FF9800", "#F44336",
                "#9C27B0", "#00BCD4", "#FFEB3B", "#795548"
            )

            val slices = values.mapIndexed { index, (label, value) ->
                Slice(
                    value = value,
                    label = label,
                    color = colors?.getOrNull(index) ?: defaultColors[index % defaultColors.size]
                )
            }

            return PieChart(slices = slices, title = title)
        }

        /**
         * Create a donut chart
         */
        fun donut(
            values: List<Pair<String, Float>>,
            colors: List<String>? = null,
            title: String? = null,
            donutRatio: Float = DEFAULT_DONUT_RATIO
        ) = simple(values, colors, title).copy(
            donutMode = true,
            donutRatio = donutRatio
        )
    }
}
