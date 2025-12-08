package com.augmentalis.avaelements.flutter.material.layout

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * MasonryGrid component - Flutter Material parity
 *
 * A Pinterest-style staggered grid layout where items flow naturally based on height.
 * Items are arranged in columns with variable heights, creating an organic masonry pattern.
 *
 * **Web Equivalent:** `Masonry` (MUI Lab), CSS Grid masonry
 * **Material Design 3:** https://m3.material.io/foundations/layout/understanding-layout/spacing
 *
 * ## Features
 * - Staggered grid layout (Pinterest-style)
 * - Variable height items
 * - Fixed or adaptive column count
 * - Configurable horizontal and vertical spacing
 * - Smooth scrolling
 * - Item click handling
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * MasonryGrid(
 *     items = listOf(item1, item2, item3),
 *     columns = MasonryGrid.Columns.Adaptive(minItemWidth = 200f),
 *     horizontalSpacing = 16f,
 *     verticalSpacing = 16f,
 *     onItemClick = { index ->
 *         println("Clicked item $index")
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property items List of items to display (as Components or data)
 * @property columns Column configuration (fixed count or adaptive)
 * @property horizontalSpacing Spacing between columns in dp
 * @property verticalSpacing Spacing between items vertically in dp
 * @property minItemWidth Minimum item width for adaptive mode (dp)
 * @property contentDescription Accessibility description for TalkBack
 * @property onItemClick Callback invoked when item is clicked (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.2.0-layout-components
 */
data class MasonryGrid(
    override val type: String = "MasonryGrid",
    override val id: String? = null,
    val items: List<Component>,
    val columns: Columns = Columns.Fixed(2),
    val horizontalSpacing: Float = 16f,
    val verticalSpacing: Float = 16f,
    val minItemWidth: Float = 180f,
    val contentDescription: String? = null,
    @Transient
    val onItemClick: ((Int) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Column configuration for masonry grid
     */
    sealed class Columns {
        /**
         * Fixed number of columns
         */
        data class Fixed(val count: Int) : Columns() {
            init {
                require(count > 0) { "Column count must be positive" }
            }
        }

        /**
         * Adaptive columns based on available width
         */
        data class Adaptive(val minItemWidth: Float) : Columns() {
            init {
                require(minItemWidth > 0f) { "Min item width must be positive" }
            }
        }
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Masonry grid"
        val itemCount = items.size
        val colInfo = when (columns) {
            is Columns.Fixed -> "${columns.count} columns"
            is Columns.Adaptive -> "adaptive columns"
        }
        return "$base with $itemCount items in $colInfo"
    }

    /**
     * Get column count for fixed mode
     */
    fun getFixedColumnCount(): Int? {
        return (columns as? Columns.Fixed)?.count
    }

    /**
     * Validate spacing parameters
     */
    fun areSpacingsValid(): Boolean {
        return horizontalSpacing >= 0f &&
               verticalSpacing >= 0f &&
               minItemWidth > 0f
    }

    companion object {
        /**
         * Create a 2-column masonry grid
         */
        fun twoColumn(
            items: List<Component>,
            spacing: Float = 16f
        ) = MasonryGrid(
            items = items,
            columns = Columns.Fixed(2),
            horizontalSpacing = spacing,
            verticalSpacing = spacing
        )

        /**
         * Create a 3-column masonry grid
         */
        fun threeColumn(
            items: List<Component>,
            spacing: Float = 16f
        ) = MasonryGrid(
            items = items,
            columns = Columns.Fixed(3),
            horizontalSpacing = spacing,
            verticalSpacing = spacing
        )

        /**
         * Create an adaptive masonry grid
         */
        fun adaptive(
            items: List<Component>,
            minItemWidth: Float = 180f,
            spacing: Float = 16f
        ) = MasonryGrid(
            items = items,
            columns = Columns.Adaptive(minItemWidth),
            minItemWidth = minItemWidth,
            horizontalSpacing = spacing,
            verticalSpacing = spacing
        )

        /**
         * Create a Pinterest-style grid
         */
        fun pinterest(
            items: List<Component>,
            minItemWidth: Float = 200f
        ) = MasonryGrid(
            items = items,
            columns = Columns.Adaptive(minItemWidth),
            minItemWidth = minItemWidth,
            horizontalSpacing = 12f,
            verticalSpacing = 12f
        )
    }
}
