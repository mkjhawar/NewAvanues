package com.augmentalis.magicui.ui.core.data

import com.augmentalis.magicui.components.core.*

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
    val items: List<Any>,
    val currentIndex: Int = 0,
    val autoPlay: Boolean = false,
    val interval: Long = 3000,
    val showIndicators: Boolean = true,
    val showControls: Boolean = true,
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList(),
    val onSlideChange: ((Int) -> Unit)? = null
) {
    init {
        require(items.isNotEmpty()) { "Carousel must have at least one item" }
        require(currentIndex in items.indices) { "currentIndex must be valid" }
        require(interval > 0) { "interval must be greater than 0" }
    }

}
