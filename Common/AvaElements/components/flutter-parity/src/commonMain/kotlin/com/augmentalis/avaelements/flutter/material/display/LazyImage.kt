package com.augmentalis.avaelements.flutter.material.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * LazyImage component - Flutter Material parity
 *
 * A Material Design 3 lazy-loaded image with placeholder and error handling.
 * Uses Coil for efficient image loading with memory and disk caching.
 *
 * **Web Equivalent:** `CardMedia` with lazy loading (MUI)
 * **Material Design 3:** https://m3.material.io/styles/imagery/overview
 *
 * ## Features
 * - Lazy loading with placeholder
 * - Automatic memory and disk caching
 * - Error state with fallback image
 * - Crossfade transition
 * - Content scale options (fit, fill, crop)
 * - Circular or rounded corners
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * LazyImage(
 *     url = "https://example.com/image.jpg",
 *     contentDescription = "Product photo",
 *     placeholder = "image_placeholder",
 *     errorPlaceholder = "broken_image",
 *     contentScale = ImageContentScale.CROP,
 *     aspectRatio = 1f
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether image is visible
 * @property url Image URL to load
 * @property contentDescription Accessibility description for TalkBack
 * @property placeholder Placeholder icon/image while loading
 * @property errorPlaceholder Icon/image to show on error
 * @property contentScale How to scale the image (fit, fill, crop, etc.)
 * @property aspectRatio Optional aspect ratio to maintain (width/height)
 * @property shape Image shape (default, circular, rounded)
 * @property cornerRadius Corner radius in dp (for rounded shape, default 8)
 * @property crossfadeDuration Crossfade animation duration in ms (default 300)
 * @property enableCache Whether to enable caching (default true)
 * @property onLoading Callback when loading starts
 * @property onSuccess Callback when image loads successfully
 * @property onError Callback when loading fails
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class LazyImage(
    override val type: String = "LazyImage",
    override val id: String? = null,
    val visible: Boolean = true,
    val url: String,
    val contentDescription: String,
    val placeholder: String? = null,
    val errorPlaceholder: String? = "broken_image",
    val contentScale: ImageContentScale = ImageContentScale.FIT,
    val aspectRatio: Float? = null,
    val shape: ImageShape = ImageShape.DEFAULT,
    val cornerRadius: Float = 8f,
    val crossfadeDuration: Int = 300,
    val enableCache: Boolean = true,
    val onLoading: (() -> Unit)? = null,
    val onSuccess: (() -> Unit)? = null,
    val onError: ((String) -> Unit)? = null,
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
        return contentDescription.ifBlank { "Image" }
    }

    /**
     * Check if has placeholder
     */
    fun hasPlaceholder(): Boolean = placeholder != null

    /**
     * Check if has error placeholder
     */
    fun hasErrorPlaceholder(): Boolean = errorPlaceholder != null

    /**
     * Check if has aspect ratio constraint
     */
    fun hasAspectRatio(): Boolean = aspectRatio != null && aspectRatio > 0f

    companion object {
        /**
         * Create a simple lazy image
         */
        fun simple(
            url: String,
            description: String
        ) = LazyImage(
            url = url,
            contentDescription = description
        )

        /**
         * Create a circular avatar image
         */
        fun avatar(
            url: String,
            description: String = "Profile picture"
        ) = LazyImage(
            url = url,
            contentDescription = description,
            shape = ImageShape.CIRCULAR,
            contentScale = ImageContentScale.CROP,
            aspectRatio = 1f,
            placeholder = "account_circle"
        )

        /**
         * Create a product image with placeholder
         */
        fun product(
            url: String,
            description: String,
            aspectRatio: Float = 1f
        ) = LazyImage(
            url = url,
            contentDescription = description,
            placeholder = "image",
            aspectRatio = aspectRatio,
            contentScale = ImageContentScale.CROP,
            shape = ImageShape.ROUNDED
        )

        /**
         * Create a thumbnail image
         */
        fun thumbnail(
            url: String,
            description: String
        ) = LazyImage(
            url = url,
            contentDescription = description,
            aspectRatio = 1f,
            contentScale = ImageContentScale.CROP,
            shape = ImageShape.ROUNDED,
            cornerRadius = 4f
        )
    }
}

/**
 * Image content scale options
 */
enum class ImageContentScale {
    FIT,        // Fit inside bounds maintaining aspect ratio
    FILL,       // Fill bounds (may distort)
    CROP,       // Fill bounds and crop (maintain aspect ratio)
    INSIDE,     // Fit inside with no upscaling
    NONE        // Original size
}

/**
 * Image shape options
 */
enum class ImageShape {
    DEFAULT,    // Rectangular with no rounding
    ROUNDED,    // Rounded corners
    CIRCULAR    // Circular shape
}
