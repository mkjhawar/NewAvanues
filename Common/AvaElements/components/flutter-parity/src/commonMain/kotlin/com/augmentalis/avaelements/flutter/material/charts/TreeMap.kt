package com.augmentalis.avaelements.flutter.material.charts

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * TreeMap component - Flutter Material parity
 *
 * A hierarchical treemap visualization using Canvas rendering.
 *
 * **Flutter Equivalent:** Custom implementation (no direct Flutter equivalent)
 * **Material Design 3:** Custom data visualization component with Material theming
 *
 * ## Features
 * - Hierarchical data visualization
 * - Proportional rectangles based on values
 * - Color coding by category
 * - Labels on rectangles
 * - Interactive selection
 * - Smooth animations
 * - Accessibility support (TalkBack)
 * - WCAG 2.1 Level AA compliant
 * - Dark mode support
 *
 * ## Usage Example
 * ```kotlin
 * TreeMap(
 *     items = listOf(
 *         TreeMap.TreeMapItem("Product A", 120f, "#2196F3"),
 *         TreeMap.TreeMapItem("Product B", 80f, "#4CAF50"),
 *         TreeMap.TreeMapItem("Product C", 150f, "#FF9800")
 *     ),
 *     title = "Market Share"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property items List of treemap items
 * @property title Optional chart title
 * @property showLabels Whether to show labels on rectangles
 * @property showValues Whether to show values on rectangles
 * @property minArea Minimum area threshold for displaying labels
 * @property height Chart height in dp
 * @property animated Whether to animate the treemap
 * @property animationDuration Animation duration in milliseconds
 * @property contentDescription Accessibility description
 * @property onItemClick Callback when an item is clicked
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class TreeMap(
    override val type: String = "TreeMap",
    override val id: String? = null,
    val items: List<TreeMapItem>,
    val title: String? = null,
    val showLabels: Boolean = true,
    val showValues: Boolean = true,
    val minArea: Float = 1000f,
    val height: Float = 400f,
    val animated: Boolean = true,
    val animationDuration: Int = 500,
    val contentDescription: String? = null,
    @Transient
    val onItemClick: ((item: TreeMapItem) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Individual item in a treemap
     *
     * @property label Item label
     * @property value Item value (determines rectangle size)
     * @property color Hex color string (e.g., "#2196F3")
     * @property category Optional category for grouping
     */
    data class TreeMapItem(
        val label: String,
        val value: Float,
        val color: String? = null,
        val category: String? = null
    ) {
        /**
         * Get effective color with fallback
         */
        fun getEffectiveColor(defaultColor: String = "#2196F3"): String {
            return color ?: defaultColor
        }

        /**
         * Get percentage of total
         */
        fun getPercentage(total: Float): Float {
            if (total == 0f) return 0f
            return (value / total) * 100f
        }
    }

    /**
     * Get total value of all items
     */
    fun getTotalValue(): Float {
        return items.sumOf { it.value.toDouble() }.toFloat()
    }

    /**
     * Get items grouped by category
     */
    fun getItemsByCategory(): Map<String, List<TreeMapItem>> {
        return items.groupBy { it.category ?: "Other" }
    }

    /**
     * Get accessibility description
     */
    fun getAccessibilityDescription(): String {
        if (contentDescription != null) return contentDescription

        val titlePart = title?.let { "$it. " } ?: ""
        val itemCount = items.size
        val total = getTotalValue()

        val topItems = items.sortedByDescending { it.value }.take(3)
        val topItemsDesc = topItems.joinToString(", ") { item ->
            val pct = item.getPercentage(total)
            val rounded = (pct * 10).toInt() / 10.0
            "${item.label}: $rounded%"
        }

        return "${titlePart}Treemap with $itemCount items. Top items: $topItemsDesc"
    }

    /**
     * Validate treemap data
     */
    fun isValid(): Boolean {
        return items.isNotEmpty() && items.all { it.value > 0 }
    }

    companion object {
        /** Default chart height in dp */
        const val DEFAULT_HEIGHT = 400f

        /** Maximum items for optimal performance */
        const val MAX_ITEMS = 100

        /**
         * Create a simple treemap
         */
        fun simple(
            values: List<Pair<String, Float>>,
            colors: List<String>? = null,
            title: String? = null
        ): TreeMap {
            val defaultColors = listOf(
                "#2196F3", "#4CAF50", "#FF9800", "#F44336",
                "#9C27B0", "#00BCD4", "#FFEB3B", "#795548"
            )

            val items = values.mapIndexed { index, (label, value) ->
                TreeMapItem(
                    label = label,
                    value = value,
                    color = colors?.getOrNull(index) ?: defaultColors[index % defaultColors.size]
                )
            }

            return TreeMap(items = items, title = title)
        }
    }
}
