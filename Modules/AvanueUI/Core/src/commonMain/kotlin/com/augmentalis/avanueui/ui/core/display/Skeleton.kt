package com.augmentalis.avanueui.ui.core.display

import com.augmentalis.avanueui.core.*


/**
 * Skeleton Component
 *
 * Displays a loading placeholder that shows the structure of content while data is being loaded.
 * Skeleton loaders improve perceived performance by showing users what to expect before content
 * is fully loaded. Can be used for text, avatars, cards, and other content shapes.
 *
 * Features:
 * - Multiple variants (TEXT, CIRCULAR, RECTANGULAR)
 * - Configurable dimensions (width, height)
 * - Animated pulse effect (optional shimmer animation)
 * - Accessibility support with loading indicators
 * - Lazy loading optimization
 * - Theme-aware coloring
 * - Compound skeleton groups for complex layouts
 *
 * Variants:
 * - **TEXT**: Rectangle with reduced height, ideal for text lines
 * - **CIRCULAR**: Circle shape, perfect for avatars and profile images
 * - **RECTANGULAR**: Default rectangle, versatile for cards, images, buttons
 *
 * Platform mappings:
 * - Android: Compose Surface with animation modifier
 * - iOS: SwiftUI Shape with animation
 * - Web: CSS div with gradient animation
 *
 * Usage:
 * ```kotlin
 * // Simple text skeleton
 * SkeletonComponent(
 *     variant = SkeletonVariant.TEXT,
 *     width = 200f,
 *     height = 16f,
 *     animated = true
 * )
 *
 * // Avatar skeleton (circular)
 * SkeletonComponent(
 *     variant = SkeletonVariant.CIRCULAR,
 *     width = 48f,
 *     height = 48f,
 *     animated = true
 * )
 *
 * // Card skeleton
 * SkeletonComponent(
 *     variant = SkeletonVariant.RECTANGULAR,
 *     width = 300f,
 *     height = 200f,
 *     animated = true,
 *     id = "card-skeleton"
 * )
 *
 * // Multiple skeletons for a user profile
 * val profileSkeleton = listOf(
 *     SkeletonComponent.avatar(),        // Circular avatar
 *     SkeletonComponent.textLine(),      // Name text
 *     SkeletonComponent.textLine(height = 14f),  // Subtitle text
 *     SkeletonComponent.rectangular()    // Bio section
 * )
 * ```
 *
 * Accessibility:
 * - Components are marked with semantic role "status" or "progressbar"
 * - Screen readers announce "Loading content"
 * - aria-busy attribute set to true during loading
 * - aria-label provides context about what's loading
 *
 * Performance Considerations:
 * - Animations are GPU-accelerated where possible
 * - Skeletons should be removed when content loads
 * - Consider using a lazy skeleton list for long lists
 * - Reduce animation frame rate on low-end devices
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 * @since 1.0.0
 */
data class SkeletonComponent(
    val width: Float = 200f,
    val height: Float = 16f,
    val variant: SkeletonVariant = SkeletonVariant.RECTANGULAR,
    val animated: Boolean = true,
    val backgroundColor: String? = null,
    val shimmerColor: String? = null,
    val contentDescription: String = "Loading",
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    init {
        // Validate dimensions
        require(width > 0) {
            "Skeleton width must be positive, got $width"
        }
        require(height > 0) {
            "Skeleton height must be positive, got $height"
        }

        // For circular variants, width and height should be equal
        if (variant == SkeletonVariant.CIRCULAR) {
            require(width == height) {
                "Skeleton with CIRCULAR variant must have equal width and height (width=$width, height=$height)"
            }
        }

        // For text variants, height should be reasonable for text
        if (variant == SkeletonVariant.TEXT) {
            require(height in 8f..48f) {
                "Skeleton with TEXT variant should have height between 8dp and 48dp for readability, got $height"
            }
        }

        // Content description should not be empty
        require(contentDescription.isNotBlank()) {
            "Skeleton contentDescription must not be empty"
        }
    }

    /**
     * Renders this skeleton component using the provided renderer.
     *
     * @param renderer The platform-specific renderer
     * @return Platform-specific rendered output (Composable, SwiftUI View, HTML element, etc.)
     */
    override fun render(renderer: Renderer): Any =
        renderer.renderComponent(this)

    companion object {
        /**
         * Creates a text line skeleton.
         */
        fun textLine(
            width: Float = 200f,
            height: Float = 16f,
            animated: Boolean = true,
            id: String? = null
        ): SkeletonComponent = SkeletonComponent(
            width = width,
            height = height,
            variant = SkeletonVariant.TEXT,
            animated = animated,
            contentDescription = "Text loading",
            id = id
        )

        /**
         * Creates an avatar skeleton.
         */
        fun avatar(
            size: Float = 48f,
            animated: Boolean = true,
            id: String? = null
        ): SkeletonComponent = SkeletonComponent(
            width = size,
            height = size,
            variant = SkeletonVariant.CIRCULAR,
            animated = animated,
            contentDescription = "Avatar loading",
            id = id
        )

        /**
         * Creates a card skeleton.
         */
        fun card(
            width: Float = 300f,
            height: Float = 200f,
            animated: Boolean = true,
            id: String? = null
        ): SkeletonComponent = SkeletonComponent(
            width = width,
            height = height,
            variant = SkeletonVariant.RECTANGULAR,
            animated = animated,
            contentDescription = "Card content loading",
            id = id
        )

        /**
         * Creates a rectangular skeleton.
         */
        fun rectangular(
            width: Float = 200f,
            height: Float = 100f,
            animated: Boolean = true,
            id: String? = null
        ): SkeletonComponent = SkeletonComponent(
            width = width,
            height = height,
            variant = SkeletonVariant.RECTANGULAR,
            animated = animated,
            contentDescription = "Content loading",
            id = id
        )
    }
}

/**
 * Skeleton variant enumeration.
 *
 * Defines the shape and intended use case for the skeleton placeholder.
 */
enum class SkeletonVariant {
    /**
     * Text variant: Rectangle optimized for text lines.
     * Typically used for paragraph text, titles, and labels.
     * Automatically sets appropriate height for text rendering.
     */
    TEXT,

    /**
     * Circular variant: Perfect circle shape for avatars and profile images.
     * Width and height must be equal.
     * Includes border-radius of 50%.
     */
    CIRCULAR,

    /**
     * Rectangular variant: Standard rectangle shape.
     * Default variant, versatile for cards, images, buttons, and other content.
     * Can be customized with corner radius via style.
     */
    RECTANGULAR
}
