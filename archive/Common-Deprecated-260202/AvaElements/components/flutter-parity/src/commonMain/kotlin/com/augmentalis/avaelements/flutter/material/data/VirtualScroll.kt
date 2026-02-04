package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * VirtualScroll component - Flutter Material parity
 *
 * A virtualized scrolling container that only renders visible items for optimal performance
 * with large datasets. Wraps LazyColumn/LazyRow for efficient scrolling.
 *
 * **Flutter Equivalent:** `ListView.builder`, custom virtual scrolling
 * **Material Design 3:** https://m3.material.io/foundations/layout/applying-layout/compact
 *
 * ## Features
 * - Only renders visible items (+ buffer)
 * - Optimal performance for large datasets (10,000+ items)
 * - Fixed or dynamic item heights
 * - Index-based item rendering
 * - Automatic recycling of off-screen items
 * - Configurable cache size
 * - Scroll position restoration
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * VirtualScroll(
 *     itemCount = 10000,
 *     itemHeight = 56f, // Optional, helps with layout calculation
 *     cacheSize = 50,
 *     orientation = VirtualScroll.Orientation.Vertical,
 *     onItemRender = { index ->
 *         // Return component for this index
 *         ListTile(
 *             title = "Item $index",
 *             subtitle = "Description for item $index"
 *         )
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property itemCount Total number of items in the list
 * @property itemHeight Optional fixed height for each item (helps with layout)
 * @property itemWidth Optional fixed width for each item (horizontal scrolling)
 * @property cacheSize Number of off-screen items to keep cached
 * @property orientation Scroll orientation (Vertical or Horizontal)
 * @property reverseLayout Whether to reverse the scroll direction
 * @property backgroundColor Background color of the scroll container
 * @property contentPadding Custom padding for the scroll container
 * @property scrollbarVisible Whether to show scrollbar
 * @property initialScrollIndex Initial scroll position (item index)
 * @property contentDescription Accessibility description for TalkBack
 * @property onItemRender Callback to render item at given index (not serialized)
 * @property onScrolledToEnd Callback invoked when scrolled to end (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class VirtualScroll(
    override val type: String = "VirtualScroll",
    override val id: String? = null,
    val itemCount: Int,
    val itemHeight: Float? = null,
    val itemWidth: Float? = null,
    val cacheSize: Int = 50,
    val orientation: Orientation = Orientation.Vertical,
    val reverseLayout: Boolean = false,
    val backgroundColor: String? = null,
    val contentPadding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val scrollbarVisible: Boolean = true,
    val initialScrollIndex: Int = 0,
    val contentDescription: String? = null,
    @Transient
    val onItemRender: ((Int) -> Component)? = null,
    @Transient
    val onScrolledToEnd: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Scroll orientation
     */
    enum class Orientation {
        /** Vertical scrolling (default) */
        Vertical,

        /** Horizontal scrolling */
        Horizontal
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Virtual scroll list"
        val orientationDesc = when (orientation) {
            Orientation.Vertical -> "vertical"
            Orientation.Horizontal -> "horizontal"
        }
        return "$base with $itemCount items, $orientationDesc scrolling"
    }

    /**
     * Check if item height is fixed
     */
    fun hasFixedItemHeight(): Boolean = itemHeight != null

    /**
     * Get estimated total height (if fixed item height)
     */
    fun getEstimatedTotalHeight(): Float? {
        return itemHeight?.let { it * itemCount }
    }

    /**
     * Calculate visible item range for a given viewport
     */
    fun calculateVisibleRange(scrollOffset: Float, viewportSize: Float): IntRange {
        if (itemHeight == null) {
            // Dynamic heights - render more conservatively
            return 0 until minOf(itemCount, cacheSize)
        }

        val firstVisibleIndex = (scrollOffset / itemHeight).toInt().coerceAtLeast(0)
        val visibleCount = (viewportSize / itemHeight).toInt() + 1
        val lastVisibleIndex = (firstVisibleIndex + visibleCount + cacheSize)
            .coerceAtMost(itemCount - 1)

        return firstVisibleIndex..lastVisibleIndex
    }

    companion object {
        /**
         * Create a vertical virtual scroll list
         */
        fun vertical(
            itemCount: Int,
            itemHeight: Float? = null,
            onItemRender: ((Int) -> Component)? = null
        ) = VirtualScroll(
            itemCount = itemCount,
            itemHeight = itemHeight,
            orientation = Orientation.Vertical,
            onItemRender = onItemRender
        )

        /**
         * Create a horizontal virtual scroll list
         */
        fun horizontal(
            itemCount: Int,
            itemWidth: Float? = null,
            onItemRender: ((Int) -> Component)? = null
        ) = VirtualScroll(
            itemCount = itemCount,
            itemWidth = itemWidth,
            orientation = Orientation.Horizontal,
            onItemRender = onItemRender
        )

        /**
         * Create a virtual scroll with fixed item size
         */
        fun fixedSize(
            itemCount: Int,
            itemHeight: Float,
            onItemRender: ((Int) -> Component)? = null
        ) = VirtualScroll(
            itemCount = itemCount,
            itemHeight = itemHeight,
            orientation = Orientation.Vertical,
            onItemRender = onItemRender
        )

        /**
         * Create a virtual scroll with dynamic item sizes
         */
        fun dynamicSize(
            itemCount: Int,
            cacheSize: Int = 50,
            onItemRender: ((Int) -> Component)? = null
        ) = VirtualScroll(
            itemCount = itemCount,
            itemHeight = null,
            cacheSize = cacheSize,
            orientation = Orientation.Vertical,
            onItemRender = onItemRender
        )
    }
}
