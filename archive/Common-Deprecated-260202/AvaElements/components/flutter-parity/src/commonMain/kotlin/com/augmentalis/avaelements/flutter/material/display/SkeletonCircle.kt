package com.augmentalis.avaelements.flutter.material.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * SkeletonCircle component - Flutter Material parity
 *
 * A Material Design 3 loading placeholder for circular content (avatars, icons, etc.).
 * Shows animated shimmer effect while content is loading.
 *
 * **Web Equivalent:** `Skeleton` variant="circular" (MUI)
 * **Material Design 3:** https://m3.material.io/components/progress-indicators/overview
 *
 * ## Features
 * - Animated shimmer/pulse effect
 * - Configurable diameter
 * - Wave or pulse animation
 * - Material3 theming support
 * - Dark mode support
 * - Reduced motion support
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * SkeletonCircle(
 *     diameter = 40f,
 *     animation = SkeletonCircle.Animation.Wave
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property diameter Circle diameter in dp
 * @property animation Animation type (wave, pulse, none)
 * @property animationDuration Animation duration in milliseconds
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class SkeletonCircle(
    override val type: String = "SkeletonCircle",
    override val id: String? = null,
    val diameter: Float = 40f,
    val animation: Animation = Animation.Wave,
    val animationDuration: Int = 1500,
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
        return contentDescription ?: "Loading circular content"
    }

    /**
     * Get radius from diameter
     */
    fun getRadius(): Float {
        return diameter / 2f
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
         * Create a small skeleton circle (avatar size)
         */
        fun small(animation: Animation = Animation.Wave) = SkeletonCircle(
            diameter = 32f,
            animation = animation
        )

        /**
         * Create a medium skeleton circle (default avatar size)
         */
        fun medium(animation: Animation = Animation.Wave) = SkeletonCircle(
            diameter = 40f,
            animation = animation
        )

        /**
         * Create a large skeleton circle
         */
        fun large(animation: Animation = Animation.Wave) = SkeletonCircle(
            diameter = 56f,
            animation = animation
        )
    }
}
