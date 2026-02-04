package com.augmentalis.avaelements.flutter.material.advanced

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * FadeInImage component - Flutter Material parity
 *
 * An image that displays a placeholder while loading and fades in when the target image loads.
 * Uses AsyncImage under the hood with fade-in animation.
 *
 * **Flutter Equivalent:** `FadeInImage`
 * **Material Design 3:** Part of image components with loading states
 *
 * ## Features
 * - Placeholder image during loading
 * - Smooth fade-in animation when loaded
 * - Error handling with fallback image
 * - Customizable fade duration
 * - Fit and alignment options
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility with image descriptions
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * FadeInImage(
 *     placeholder = "placeholder_image",
 *     image = "https://example.com/photo.jpg",
 *     fadeInDuration = 300,
 *     fit = BoxFit.Cover,
 *     contentDescription = "User profile photo"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property placeholder Placeholder image resource (shown while loading)
 * @property image Target image URL or resource to load
 * @property fadeInDuration Duration of fade-in animation in milliseconds
 * @property fadeOutDuration Duration of fade-out animation for placeholder in milliseconds
 * @property width Image width (in dp)
 * @property height Image height (in dp)
 * @property fit How to inscribe the image into the space allocated
 * @property alignment How to align the image within its bounds
 * @property repeat How to repeat the image if it doesn't fill the space
 * @property matchTextDirection Whether to flip image horizontally in RTL mode
 * @property excludeFromSemantics Whether to exclude from accessibility tree
 * @property semanticsLabel Accessibility label for the image
 * @property errorBuilder Component to show on error (optional)
 * @property contentDescription Accessibility description for TalkBack
 * @property onLoadComplete Callback invoked when image loads successfully (not serialized)
 * @property onError Callback invoked when image fails to load (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class FadeInImage(
    override val type: String = "FadeInImage",
    override val id: String? = null,
    val placeholder: String,
    val image: String,
    val fadeInDuration: Int = 300,
    val fadeOutDuration: Int = 300,
    val width: Float? = null,
    val height: Float? = null,
    val fit: BoxFit = BoxFit.Contain,
    val alignment: Alignment = Alignment.Center,
    val repeat: ImageRepeat = ImageRepeat.NoRepeat,
    val matchTextDirection: Boolean = false,
    val excludeFromSemantics: Boolean = false,
    val semanticsLabel: String? = null,
    val errorBuilder: Component? = null,
    val contentDescription: String? = null,
    @Transient
    val onLoadComplete: (() -> Unit)? = null,
    @Transient
    val onError: ((String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String? {
        if (excludeFromSemantics) return null
        return contentDescription ?: semanticsLabel
    }

    /**
     * Check if image is from network
     */
    fun isNetworkImage(): Boolean {
        return image.startsWith("http://") || image.startsWith("https://")
    }

    /**
     * How to inscribe the image into the space
     */
    enum class BoxFit {
        /** Fill the space, may distort aspect ratio */
        Fill,

        /** As large as possible while maintaining aspect ratio */
        Contain,

        /** As small as possible while filling space */
        Cover,

        /** Original size, may be clipped */
        None,

        /** Scale down to fit, but never scale up */
        ScaleDown,

        /** Fill width, may distort aspect ratio */
        FitWidth,

        /** Fill height, may distort aspect ratio */
        FitHeight
    }

    /**
     * How to align the image within its bounds
     */
    enum class Alignment {
        TopStart,
        TopCenter,
        TopEnd,
        CenterStart,
        Center,
        CenterEnd,
        BottomStart,
        BottomCenter,
        BottomEnd
    }

    /**
     * How to repeat the image if it doesn't fill the space
     */
    enum class ImageRepeat {
        /** No repeat */
        NoRepeat,

        /** Repeat horizontally and vertically */
        Repeat,

        /** Repeat horizontally only */
        RepeatX,

        /** Repeat vertically only */
        RepeatY
    }

    companion object {
        /**
         * Default fade-in duration (in milliseconds)
         */
        const val DEFAULT_FADE_DURATION = 300

        /**
         * Create a simple fade-in image
         */
        fun simple(
            placeholder: String,
            image: String,
            contentDescription: String? = null
        ) = FadeInImage(
            placeholder = placeholder,
            image = image,
            contentDescription = contentDescription
        )

        /**
         * Create a fade-in image from network URL
         */
        fun network(
            placeholder: String,
            imageUrl: String,
            contentDescription: String? = null
        ) = FadeInImage(
            placeholder = placeholder,
            image = imageUrl,
            contentDescription = contentDescription
        )

        /**
         * Create a fade-in image with custom duration
         */
        fun withDuration(
            placeholder: String,
            image: String,
            fadeInDuration: Int,
            contentDescription: String? = null
        ) = FadeInImage(
            placeholder = placeholder,
            image = image,
            fadeInDuration = fadeInDuration,
            contentDescription = contentDescription
        )

        /**
         * Create a fade-in image with cover fit (fills space)
         */
        fun cover(
            placeholder: String,
            image: String,
            contentDescription: String? = null
        ) = FadeInImage(
            placeholder = placeholder,
            image = image,
            fit = BoxFit.Cover,
            contentDescription = contentDescription
        )

        /**
         * Create a fade-in image with specified size
         */
        fun sized(
            placeholder: String,
            image: String,
            width: Float,
            height: Float,
            contentDescription: String? = null
        ) = FadeInImage(
            placeholder = placeholder,
            image = image,
            width = width,
            height = height,
            contentDescription = contentDescription
        )
    }
}
