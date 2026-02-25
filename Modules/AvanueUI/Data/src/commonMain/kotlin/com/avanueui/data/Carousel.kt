package com.avanueui.data

import com.augmentalis.avanueui.core.*
import com.augmentalis.avanueui.core.ComponentStyle
import com.augmentalis.avanueui.core.Size
import com.augmentalis.avanueui.core.Color
import com.augmentalis.avanueui.core.Spacing
import com.augmentalis.avanueui.core.Border
import com.augmentalis.avanueui.core.Shadow

/**
 * Carousel Component
 *
 * A carousel (slider) component for displaying multiple items in a
 * scrollable view, typically images or cards.
 *
 * Features:
 * - Multiple slides/items
 * - Auto-play with configurable interval
 * - Navigation controls
 * - Slide indicators
 * - Swipe/drag support
 *
 * Platform mappings:
 * - Android: ViewPager2
 * - iOS: UIPageViewController / UIScrollView
 * - Web: Slider/carousel
 *
 * Usage:
 * ```kotlin
 * Carousel(
 *     items = listOf(
 *         ImageComponent(source = "image1.jpg"),
 *         ImageComponent(source = "image2.jpg"),
 *         ImageComponent(source = "image3.jpg")
 *     ),
 *     currentIndex = 0,
 *     autoPlay = true,
 *     interval = 3000,
 *     showIndicators = true,
 *     onSlideChange = { index -> /* handle change */ }
 * )
 * ```
 */
data class CarouselComponent(
    val type: String = "Carousel",
    val items: List<Component>,
    val currentIndex: Int = 0,
    val autoPlay: Boolean = false,
    val interval: Long = 3000,
    val showIndicators: Boolean = true,
    val showControls: Boolean = true,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onSlideChange: ((Int) -> Unit)? = null
) : Component {
    init {
        require(items.isNotEmpty()) { "Carousel must have at least one item" }
        require(currentIndex in items.indices) { "currentIndex must be valid" }
        require(interval > 0) { "interval must be greater than 0" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}
