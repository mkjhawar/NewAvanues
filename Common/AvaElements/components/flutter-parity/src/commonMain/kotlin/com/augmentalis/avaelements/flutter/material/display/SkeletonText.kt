package com.augmentalis.avaelements.flutter.material.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * SkeletonText component - Flutter Material parity
 *
 * A Material Design 3 loading placeholder for text content.
 * Shows animated shimmer effect while content is loading.
 *
 * **Web Equivalent:** `Skeleton` variant="text" (MUI)
 * **Material Design 3:** https://m3.material.io/components/progress-indicators/overview
 *
 * ## Features
 * - Animated shimmer/pulse effect
 * - Multiple line support
 * - Configurable width and height
 * - Text variant sizes (h1, h2, body, caption)
 * - Wave or pulse animation
 * - Material3 theming support
 * - Dark mode support
 * - Reduced motion support
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * SkeletonText(
 *     variant = SkeletonText.Variant.Body1,
 *     lines = 3,
 *     width = null, // Full width
 *     animation = SkeletonText.Animation.Wave,
 *     lastLineWidth = 0.7f
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property variant Text variant size (h1, h2, body, caption)
 * @property lines Number of text lines to show
 * @property width Optional fixed width (null = full width)
 * @property height Optional height override
 * @property lastLineWidth Width of last line as fraction (0.0-1.0)
 * @property animation Animation type (wave, pulse, none)
 * @property animationDuration Animation duration in milliseconds
 * @property borderRadius Border radius in dp
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class SkeletonText(
    override val type: String = "SkeletonText",
    override val id: String? = null,
    val variant: Variant = Variant.Body1,
    val lines: Int = 1,
    val width: Float? = null,
    val height: Float? = null,
    val lastLineWidth: Float = 0.8f,
    val animation: Animation = Animation.Wave,
    val animationDuration: Int = 1500,
    val borderRadius: Float = 4f,
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
        return contentDescription ?: "Loading text content, $lines lines"
    }

    /**
     * Get height for variant in dp
     */
    fun getVariantHeight(): Float {
        return height ?: when (variant) {
            Variant.H1 -> 32f
            Variant.H2 -> 28f
            Variant.H3 -> 24f
            Variant.H4 -> 20f
            Variant.H5 -> 18f
            Variant.H6 -> 16f
            Variant.Body1 -> 16f
            Variant.Body2 -> 14f
            Variant.Caption -> 12f
        }
    }

    /**
     * Validate last line width is in valid range
     */
    fun isLastLineWidthValid(): Boolean {
        return lastLineWidth in 0f..1f
    }

    /**
     * Text variant
     */
    enum class Variant {
        H1, H2, H3, H4, H5, H6,
        Body1, Body2,
        Caption
    }

    /**
     * Animation type
     */
    enum class Animation {
        /** Wave shimmer effect */
        Wave,

        /** Pulse opacity effect */
        Pulse,

        /** No animation */
        None
    }

    companion object {
        /**
         * Default animation duration
         */
        const val DEFAULT_ANIMATION_DURATION = 1500

        /**
         * Create a single line skeleton
         */
        fun singleLine(
            variant: Variant = Variant.Body1,
            width: Float? = null,
            animation: Animation = Animation.Wave
        ) = SkeletonText(
            variant = variant,
            lines = 1,
            width = width,
            animation = animation
        )

        /**
         * Create a multi-line skeleton
         */
        fun multiLine(
            lines: Int,
            variant: Variant = Variant.Body1,
            lastLineWidth: Float = 0.8f,
            animation: Animation = Animation.Wave
        ) = SkeletonText(
            variant = variant,
            lines = lines,
            lastLineWidth = lastLineWidth,
            animation = animation
        )

        /**
         * Create a heading skeleton
         */
        fun heading(
            variant: Variant = Variant.H2,
            width: Float? = null
        ) = SkeletonText(
            variant = variant,
            lines = 1,
            width = width
        )
    }
}
