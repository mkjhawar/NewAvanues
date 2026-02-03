package com.augmentalis.avaelements.flutter.material.cards

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ImageCard component - Flutter Material parity
 *
 * A Material Design 3 card with prominent image and optional overlay text/actions.
 * Commonly used in galleries, portfolios, and visual content grids.
 *
 * **Web Equivalent:** `ImageCard` (MUI)
 * **Material Design 3:** https://m3.material.io/components/cards/overview
 *
 * ## Features
 * - Prominent full-bleed image
 * - Optional text overlay with gradient
 * - Title and subtitle support
 * - Overlay position control (bottom, top, center)
 * - Optional action buttons on overlay
 * - Image aspect ratio control
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * ImageCard(
 *     imageUrl = "https://example.com/photo.jpg",
 *     title = "Sunset Over Mountains",
 *     subtitle = "Photo by Jane Doe",
 *     overlayPosition = ImageCard.OverlayPosition.Bottom,
 *     showGradient = true,
 *     aspectRatio = 16f / 9f,
 *     onPressed = {
 *         // View full image
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property imageUrl Image URL
 * @property title Optional overlay title
 * @property subtitle Optional overlay subtitle
 * @property overlayPosition Position of text overlay
 * @property showGradient Whether to show gradient behind overlay text
 * @property gradientColor Optional gradient color
 * @property aspectRatio Optional aspect ratio (width/height)
 * @property fit How image should fit within bounds
 * @property actionIcon Optional action icon on overlay
 * @property actionText Optional action button text
 * @property contentDescription Accessibility description for TalkBack
 * @property onPressed Callback invoked when card is pressed (not serialized)
 * @property onActionPressed Callback invoked when action is pressed (not serialized)
 * @property style Optional card style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class ImageCard(
    override val type: String = "ImageCard",
    override val id: String? = null,
    val imageUrl: String,
    val title: String? = null,
    val subtitle: String? = null,
    val overlayPosition: OverlayPosition = OverlayPosition.Bottom,
    val showGradient: Boolean = true,
    val gradientColor: String? = null,
    val aspectRatio: Float? = null,
    val fit: ImageFit = ImageFit.Cover,
    val actionIcon: String? = null,
    val actionText: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onPressed: (() -> Unit)? = null,
    @Transient
    val onActionPressed: (() -> Unit)? = null,
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
        val base = contentDescription ?: "Image card"
        val titleInfo = if (title != null) ": $title" else ""
        val subtitleInfo = if (subtitle != null) ", $subtitle" else ""
        return "$base$titleInfo$subtitleInfo"
    }

    /**
     * Check if overlay content exists
     */
    fun hasOverlay(): Boolean {
        return title != null || subtitle != null || actionText != null
    }

    /**
     * Overlay position
     */
    enum class OverlayPosition {
        /** Overlay at top */
        Top,

        /** Overlay at center */
        Center,

        /** Overlay at bottom */
        Bottom
    }

    /**
     * Image fit mode
     */
    enum class ImageFit {
        /** Scale to fill, may crop */
        Cover,

        /** Scale to fit, may have letterboxing */
        Contain,

        /** Stretch to fill */
        Fill,

        /** Don't scale */
        None
    }

    companion object {
        /**
         * Create a simple image card
         */
        fun simple(
            imageUrl: String,
            title: String? = null,
            onPressed: (() -> Unit)? = null
        ) = ImageCard(
            imageUrl = imageUrl,
            title = title,
            onPressed = onPressed
        )

        /**
         * Create an image card with full overlay
         */
        fun withOverlay(
            imageUrl: String,
            title: String,
            subtitle: String? = null,
            overlayPosition: OverlayPosition = OverlayPosition.Bottom,
            onPressed: (() -> Unit)? = null
        ) = ImageCard(
            imageUrl = imageUrl,
            title = title,
            subtitle = subtitle,
            overlayPosition = overlayPosition,
            showGradient = true,
            onPressed = onPressed
        )
    }
}
