package com.augmentalis.avaelements.flutter.material.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Lightbox component - Flutter Material parity
 *
 * A Material Design 3 full-screen image viewer with zoom and pan gestures.
 * Displays images in an overlay with navigation between multiple images.
 *
 * **Web Equivalent:** `Dialog` with image viewer (MUI)
 * **Material Design 3:** https://m3.material.io/components/dialogs/overview
 *
 * ## Features
 * - Full-screen overlay
 * - Pinch to zoom (1x - 4x)
 * - Pan and drag gestures
 * - Navigation between images (swipe or arrows)
 * - Image counter (e.g., "3 of 10")
 * - Close button
 * - Optional captions
 * - Download/share actions
 * - Smooth zoom transitions
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * Lightbox(
 *     images = listOf(
 *         LightboxImage("https://example.com/1.jpg", "Photo 1", "Caption 1"),
 *         LightboxImage("https://example.com/2.jpg", "Photo 2", "Caption 2")
 *     ),
 *     initialIndex = 0,
 *     visible = true,
 *     showCaption = true,
 *     showCounter = true,
 *     enableZoom = true,
 *     onClose = { /* hide lightbox */ }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether lightbox is visible
 * @property images List of images to display
 * @property initialIndex Starting image index (default 0)
 * @property showCaption Whether to show image captions
 * @property showCounter Whether to show image counter
 * @property showNavigation Whether to show prev/next arrows
 * @property enableZoom Whether to enable pinch-to-zoom
 * @property minZoom Minimum zoom level (default 1)
 * @property maxZoom Maximum zoom level (default 4)
 * @property backgroundColor Background overlay color
 * @property showCloseButton Whether to show close button
 * @property showDownload Whether to show download action
 * @property showShare Whether to show share action
 * @property onClose Callback when lightbox closes
 * @property onIndexChanged Callback when image index changes
 * @property onDownload Callback when download is clicked
 * @property onShare Callback when share is clicked
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class Lightbox(
    override val type: String = "Lightbox",
    override val id: String? = null,
    val visible: Boolean = false,
    val images: List<LightboxImage>,
    val initialIndex: Int = 0,
    val showCaption: Boolean = true,
    val showCounter: Boolean = true,
    val showNavigation: Boolean = true,
    val enableZoom: Boolean = true,
    val minZoom: Float = 1f,
    val maxZoom: Float = 4f,
    val backgroundColor: String? = null,
    val showCloseButton: Boolean = true,
    val showDownload: Boolean = false,
    val showShare: Boolean = false,
    val onClose: (() -> Unit)? = null,
    val onIndexChanged: ((Int) -> Unit)? = null,
    val onDownload: ((Int) -> Unit)? = null,
    val onShare: ((Int) -> Unit)? = null,
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
    fun getAccessibilityDescription(currentIndex: Int): String {
        val imageDesc = images.getOrNull(currentIndex)?.description ?: "Image"
        val position = "Image ${currentIndex + 1} of ${images.size}"
        val zoomText = if (enableZoom) ". Pinch to zoom" else ""
        return contentDescription ?: "$imageDesc. $position$zoomText"
    }

    /**
     * Check if has images
     */
    fun hasImages(): Boolean = images.isNotEmpty()

    /**
     * Check if can navigate to previous
     */
    fun canNavigatePrevious(currentIndex: Int): Boolean = currentIndex > 0

    /**
     * Check if can navigate to next
     */
    fun canNavigateNext(currentIndex: Int): Boolean = currentIndex < images.lastIndex

    /**
     * Get counter text
     */
    fun getCounterText(currentIndex: Int): String {
        return "${currentIndex + 1} of ${images.size}"
    }

    /**
     * Validate initial index
     */
    fun isInitialIndexValid(): Boolean = initialIndex in images.indices

    /**
     * Check if has actions
     */
    fun hasActions(): Boolean = showDownload || showShare

    companion object {
        /**
         * Create a simple lightbox
         */
        fun simple(
            imageUrls: List<String>,
            descriptions: List<String>? = null,
            initialIndex: Int = 0,
            onClose: () -> Unit
        ) = Lightbox(
            images = imageUrls.mapIndexed { index, url ->
                LightboxImage(url, descriptions?.getOrNull(index) ?: "Image ${index + 1}")
            },
            initialIndex = initialIndex,
            visible = true,
            onClose = onClose
        )

        /**
         * Create a lightbox with captions
         */
        fun withCaptions(
            images: List<LightboxImage>,
            initialIndex: Int = 0,
            onClose: () -> Unit
        ) = Lightbox(
            images = images,
            initialIndex = initialIndex,
            visible = true,
            showCaption = true,
            showCounter = true,
            onClose = onClose
        )

        /**
         * Create a lightbox with actions
         */
        fun withActions(
            images: List<LightboxImage>,
            initialIndex: Int = 0,
            onClose: () -> Unit,
            onDownload: (Int) -> Unit,
            onShare: (Int) -> Unit
        ) = Lightbox(
            images = images,
            initialIndex = initialIndex,
            visible = true,
            showDownload = true,
            showShare = true,
            onClose = onClose,
            onDownload = onDownload,
            onShare = onShare
        )
    }
}

/**
 * Lightbox image with URL, description, and optional caption
 */
data class LightboxImage(
    val url: String,
    val description: String,
    val caption: String? = null,
    val thumbnail: String? = null
)
