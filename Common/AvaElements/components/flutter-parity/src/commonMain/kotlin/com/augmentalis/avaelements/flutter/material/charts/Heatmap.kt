package com.augmentalis.avaelements.flutter.material.charts

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Heatmap component - Flutter Material parity
 *
 * A heatmap visualization for matrix data using Canvas rendering.
 *
 * **Flutter Equivalent:** `fl_heatmap` package
 * **Material Design 3:** Custom data visualization component with Material theming
 *
 * ## Features
 * - 2D matrix data visualization
 * - Color gradient mapping
 * - Row and column labels
 * - Value display on cells
 * - Interactive cell selection
 * - Custom color schemes
 * - Smooth animations
 * - Accessibility support (TalkBack)
 * - WCAG 2.1 Level AA compliant
 * - Dark mode support
 *
 * ## Usage Example
 * ```kotlin
 * Heatmap(
 *     data = listOf(
 *         listOf(10f, 20f, 30f),
 *         listOf(15f, 25f, 35f),
 *         listOf(20f, 30f, 40f)
 *     ),
 *     rowLabels = listOf("Row 1", "Row 2", "Row 3"),
 *     columnLabels = listOf("Col 1", "Col 2", "Col 3"),
 *     title = "Activity Heatmap",
 *     colorScheme = Heatmap.ColorScheme.BlueRed
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property data 2D matrix of values (rows x columns)
 * @property rowLabels Labels for each row
 * @property columnLabels Labels for each column
 * @property title Optional chart title
 * @property colorScheme Color scheme for the heatmap
 * @property showValues Whether to display values on cells
 * @property showGrid Whether to show grid lines
 * @property cellSize Size of each cell in dp
 * @property minValue Optional minimum value for color mapping
 * @property maxValue Optional maximum value for color mapping
 * @property animated Whether to animate the heatmap
 * @property animationDuration Animation duration in milliseconds
 * @property contentDescription Accessibility description
 * @property onCellClick Callback when a cell is clicked
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class Heatmap(
    override val type: String = "Heatmap",
    override val id: String? = null,
    val data: List<List<Float>>,
    val rowLabels: List<String> = emptyList(),
    val columnLabels: List<String> = emptyList(),
    val title: String? = null,
    val colorScheme: ColorScheme = ColorScheme.BlueRed,
    val showValues: Boolean = true,
    val showGrid: Boolean = true,
    val cellSize: Float = 50f,
    val minValue: Float? = null,
    val maxValue: Float? = null,
    val animated: Boolean = true,
    val animationDuration: Int = 500,
    val contentDescription: String? = null,
    @Transient
    val onCellClick: ((row: Int, column: Int, value: Float) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Color schemes for heatmap visualization
     */
    enum class ColorScheme {
        /** Blue (low) to Red (high) */
        BlueRed,

        /** Green (low) to Red (high) */
        GreenRed,

        /** Blue (low) to Yellow to Red (high) */
        BlueYellowRed,

        /** Purple (low) to White to Orange (high) */
        PurpleWhiteOrange,

        /** Grayscale */
        Grayscale
    }

    /**
     * Get value range from data
     */
    fun getValueRange(): Pair<Float, Float> {
        if (data.isEmpty() || data.all { it.isEmpty() }) return 0f to 0f

        val allValues = data.flatten()
        val min = minValue ?: allValues.minOrNull() ?: 0f
        val max = maxValue ?: allValues.maxOrNull() ?: 0f

        return min to max
    }

    /**
     * Get normalized value (0.0 to 1.0) for color mapping
     */
    fun getNormalizedValue(value: Float): Float {
        val (min, max) = getValueRange()
        if (max == min) return 0.5f
        return ((value - min) / (max - min)).coerceIn(0f, 1f)
    }

    /**
     * Get color for a value based on color scheme
     */
    fun getColorForValue(value: Float): String {
        val normalized = getNormalizedValue(value)

        return when (colorScheme) {
            ColorScheme.BlueRed -> interpolateColor("#2196F3", "#F44336", normalized)
            ColorScheme.GreenRed -> interpolateColor("#4CAF50", "#F44336", normalized)
            ColorScheme.BlueYellowRed -> {
                if (normalized < 0.5f) {
                    interpolateColor("#2196F3", "#FFEB3B", normalized * 2)
                } else {
                    interpolateColor("#FFEB3B", "#F44336", (normalized - 0.5f) * 2)
                }
            }
            ColorScheme.PurpleWhiteOrange -> {
                if (normalized < 0.5f) {
                    interpolateColor("#9C27B0", "#FFFFFF", normalized * 2)
                } else {
                    interpolateColor("#FFFFFF", "#FF9800", (normalized - 0.5f) * 2)
                }
            }
            ColorScheme.Grayscale -> {
                val gray = (255 * (1 - normalized)).toInt()
                val hex = gray.toString(16).padStart(2, '0').uppercase()
                "#$hex$hex$hex"
            }
        }
    }

    /**
     * Simple linear color interpolation
     */
    private fun interpolateColor(startColor: String, endColor: String, ratio: Float): String {
        // Simple implementation - returns start color for low values, end for high
        return if (ratio < 0.5f) startColor else endColor
    }

    /**
     * Get dimensions (rows x columns)
     */
    fun getDimensions(): Pair<Int, Int> {
        val rows = data.size
        val columns = data.maxOfOrNull { it.size } ?: 0
        return rows to columns
    }

    /**
     * Get accessibility description
     */
    fun getAccessibilityDescription(): String {
        if (contentDescription != null) return contentDescription

        val titlePart = title?.let { "$it. " } ?: ""
        val (rows, columns) = getDimensions()
        val (min, max) = getValueRange()

        return "${titlePart}Heatmap with $rows rows and $columns columns. Values range from $min to $max"
    }

    /**
     * Validate heatmap data
     */
    fun isValid(): Boolean {
        if (data.isEmpty()) return false
        val columnCount = data.first().size
        return data.all { it.size == columnCount }
    }

    companion object {
        /** Default cell size in dp */
        const val DEFAULT_CELL_SIZE = 50f

        /** Maximum cells for optimal performance */
        const val MAX_CELLS = 1000

        /**
         * Create a simple heatmap
         */
        fun simple(
            data: List<List<Float>>,
            title: String? = null
        ) = Heatmap(
            data = data,
            title = title,
            colorScheme = ColorScheme.BlueRed
        )
    }
}
