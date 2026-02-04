package com.augmentalis.avaelements.flutter.material.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ImageGallery component - Flutter Material parity
 *
 * A Material Design 3 photo grid gallery with lazy loading.
 * Displays images in a responsive grid layout with tap to expand.
 *
 * **Web Equivalent:** `ImageList` (MUI)
 * **Material Design 3:** https://m3.material.io/styles/imagery/overview
 *
 * ## Features
 * - Responsive grid layout (2-4 columns)
 * - Lazy loading of images
 * - Tap to expand in lightbox
 * - Configurable spacing and aspect ratio
 * - Selection mode support
 * - Placeholder while loading
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * ImageGallery(
 *     images = listOf(
 *         GalleryImage("https://example.com/1.jpg", "Photo 1"),
 *         GalleryImage("https://example.com/2.jpg", "Photo 2"),
 *         GalleryImage("https://example.com/3.jpg", "Photo 3")
 *     ),
 *     columns = 3,
 *     spacing = 8f,
 *     aspectRatio = 1f,
 *     onImageTap = { index -> /* open lightbox */ }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether gallery is visible
 * @property images List of gallery images
 * @property columns Number of columns (2-4, default 2)
 * @property spacing Spacing between images in dp (default 8)
 * @property aspectRatio Image aspect ratio (default 1 for square)
 * @property selectionMode Whether selection mode is enabled
 * @property selectedIndices Set of selected image indices
 * @property showOverlay Whether to show overlay on hover/press
 * @property placeholder Placeholder while loading
 * @property onImageTap Callback when image is tapped
 * @property onImageLongPress Callback when image is long-pressed
 * @property onSelectionChanged Callback when selection changes
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class ImageGallery(
    override val type: String = "ImageGallery",
    override val id: String? = null,
    val visible: Boolean = true,
    val images: List<GalleryImage>,
    val columns: Int = 2,
    val spacing: Float = 8f,
    val aspectRatio: Float = 1f,
    val selectionMode: Boolean = false,
    val selectedIndices: Set<Int> = emptySet(),
    val showOverlay: Boolean = true,
    val placeholder: String? = "image",
    val onImageTap: ((Int) -> Unit)? = null,
    val onImageLongPress: ((Int) -> Unit)? = null,
    val onSelectionChanged: ((Set<Int>) -> Unit)? = null,
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
    fun getAccessibilityDescription(): String {
        val imageCount = images.size
        val selectionText = if (selectionMode) {
            ". ${selectedIndices.size} of $imageCount selected"
        } else {
            ""
        }
        return contentDescription ?: "Image gallery with $imageCount photos$selectionText"
    }

    /**
     * Check if has images
     */
    fun hasImages(): Boolean = images.isNotEmpty()

    /**
     * Check if image is selected
     */
    fun isSelected(index: Int): Boolean = index in selectedIndices

    /**
     * Get total image count
     */
    fun getImageCount(): Int = images.size

    /**
     * Validate columns range
     */
    fun isColumnsValid(): Boolean = columns in 1..4

    companion object {
        /**
         * Create a simple photo gallery
         */
        fun photos(
            imageUrls: List<String>,
            descriptions: List<String>? = null,
            onTap: ((Int) -> Unit)? = null
        ) = ImageGallery(
            images = imageUrls.mapIndexed { index, url ->
                GalleryImage(url, descriptions?.getOrNull(index) ?: "Photo ${index + 1}")
            },
            columns = 3,
            onImageTap = onTap
        )

        /**
         * Create a product gallery
         */
        fun products(
            images: List<GalleryImage>,
            columns: Int = 2
        ) = ImageGallery(
            images = images,
            columns = columns,
            aspectRatio = 4f / 3f
        )

        /**
         * Create a selectable gallery
         */
        fun selectable(
            images: List<GalleryImage>,
            onSelectionChanged: (Set<Int>) -> Unit
        ) = ImageGallery(
            images = images,
            selectionMode = true,
            onSelectionChanged = onSelectionChanged
        )
    }
}

/**
 * Gallery image with URL and description
 */
data class GalleryImage(
    val url: String,
    val description: String,
    val thumbnail: String? = null,
    val width: Int? = null,
    val height: Int? = null
)
