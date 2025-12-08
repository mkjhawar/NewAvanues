package com.augmentalis.avaelements.flutter.material.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ImageCarousel component - Flutter Material parity
 *
 * A Material Design 3 swipeable image carousel with indicator dots.
 * Supports automatic cycling, infinite scroll, and touch gestures.
 *
 * **Web Equivalent:** `Carousel` (MUI)
 * **Material Design 3:** https://m3.material.io/components/carousel/overview
 *
 * ## Features
 * - Swipeable image carousel
 * - Dot indicators showing current position
 * - Auto-play with configurable interval
 * - Infinite scroll loop
 * - Fade or slide transitions
 * - Configurable aspect ratio
 * - Lazy loading support
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility with image descriptions
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * ImageCarousel(
 *     images = listOf(
 *         CarouselImage("https://example.com/1.jpg", "Product view 1"),
 *         CarouselImage("https://example.com/2.jpg", "Product view 2"),
 *         CarouselImage("https://example.com/3.jpg", "Product view 3")
 *     ),
 *     autoPlay = true,
 *     interval = 3000,
 *     showIndicators = true,
 *     aspectRatio = 16f / 9f
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether carousel is visible
 * @property images List of carousel images with URLs and descriptions
 * @property initialPage Starting page index (default 0)
 * @property autoPlay Whether to auto-advance slides
 * @property interval Auto-play interval in milliseconds (default 3000)
 * @property infinite Whether to loop infinitely
 * @property showIndicators Whether to show dot indicators
 * @property showArrows Whether to show navigation arrows
 * @property aspectRatio Image aspect ratio (width/height, default 16/9)
 * @property transitionType Transition animation (fade or slide)
 * @property onPageChanged Callback when page changes
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class ImageCarousel(
    override val type: String = "ImageCarousel",
    override val id: String? = null,
    val visible: Boolean = true,
    val images: List<CarouselImage>,
    val initialPage: Int = 0,
    val autoPlay: Boolean = false,
    val interval: Long = 3000,
    val infinite: Boolean = true,
    val showIndicators: Boolean = true,
    val showArrows: Boolean = false,
    val aspectRatio: Float = 16f / 9f,
    val transitionType: CarouselTransition = CarouselTransition.SLIDE,
    val onPageChanged: ((Int) -> Unit)? = null,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(currentPage: Int): String {
        val imageDesc = images.getOrNull(currentPage)?.description ?: "Image"
        val position = "Image ${currentPage + 1} of ${images.size}"
        return contentDescription ?: "$imageDesc. $position"
    }

    /**
     * Check if has images
     */
    fun hasImages(): Boolean = images.isNotEmpty()

    /**
     * Get total page count
     */
    fun getPageCount(): Int = images.size

    /**
     * Validate initial page
     */
    fun isInitialPageValid(): Boolean = initialPage in images.indices

    companion object {
        /**
         * Create a simple image carousel
         */
        fun simple(
            imageUrls: List<String>,
            descriptions: List<String>? = null
        ) = ImageCarousel(
            images = imageUrls.mapIndexed { index, url ->
                CarouselImage(url, descriptions?.getOrNull(index) ?: "Image ${index + 1}")
            }
        )

        /**
         * Create an auto-playing carousel
         */
        fun autoPlay(
            images: List<CarouselImage>,
            interval: Long = 3000
        ) = ImageCarousel(
            images = images,
            autoPlay = true,
            interval = interval,
            infinite = true
        )
    }
}

/**
 * Carousel image with URL and description
 */
data class CarouselImage(
    val url: String,
    val description: String,
    val thumbnail: String? = null
)

/**
 * Carousel transition types
 */
enum class CarouselTransition {
    SLIDE,
    FADE
}
