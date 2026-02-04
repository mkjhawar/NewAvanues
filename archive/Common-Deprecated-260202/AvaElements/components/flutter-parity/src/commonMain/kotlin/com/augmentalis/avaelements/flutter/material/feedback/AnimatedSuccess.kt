package com.augmentalis.avaelements.flutter.material.feedback

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * AnimatedSuccess component - Flutter Material parity
 *
 * An animated success icon with bouncy spring animation and particle effects.
 * Features a circular checkmark with celebratory animation for success states.
 *
 * **Web Equivalent:** Custom animated success (Framer Motion), `CheckCircleIcon` with confetti
 * **Material Design 3:** https://m3.material.io/styles/motion/overview
 *
 * ## Features
 * - Bouncy scale-in animation using spring physics
 * - Optional particle/confetti effects
 * - Checkmark draw animation
 * - Smooth entrance/exit transitions
 * - Configurable size and color
 * - Green success color by default
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * AnimatedSuccess(
 *     visible = true,
 *     size = 72f,
 *     color = "#4CAF50",
 *     animationDuration = 600,
 *     showParticles = true,
 *     contentDescription = "Order placed successfully"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether success icon is visible (triggers animation)
 * @property size Icon size in dp
 * @property color Optional icon color (hex string)
 * @property animationDuration Animation duration in milliseconds
 * @property showParticles Whether to show particle/confetti effects
 * @property particleCount Number of particles (if enabled)
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.2.0-feedback-components
 */
data class AnimatedSuccess(
    override val type: String = "AnimatedSuccess",
    override val id: String? = null,
    val visible: Boolean = true,
    val size: Float = 64f,
    val color: String? = null,
    val animationDuration: Int = 600,
    val showParticles: Boolean = false,
    val particleCount: Int = 20,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective icon color (default to success green)
     */
    fun getEffectiveColor(): String {
        return color ?: "#4CAF50"
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        return contentDescription ?: "Success"
    }

    /**
     * Validate size and animation parameters
     */
    fun areParametersValid(): Boolean {
        return size > 0f && size <= 200f &&
               animationDuration > 0 && animationDuration <= 5000 &&
               particleCount >= 0 && particleCount <= 100
    }

    companion object {
        /** Default success color (Material Green 500) */
        const val DEFAULT_SUCCESS_COLOR = "#4CAF50"

        /** Default icon size */
        const val DEFAULT_SIZE = 64f

        /** Default animation duration */
        const val DEFAULT_ANIMATION_DURATION = 600

        /** Default particle count */
        const val DEFAULT_PARTICLE_COUNT = 20

        /**
         * Create a simple animated success
         */
        fun simple(
            visible: Boolean = true
        ) = AnimatedSuccess(
            visible = visible
        )

        /**
         * Create a celebratory success with particles
         */
        fun celebration(
            visible: Boolean = true,
            size: Float = 80f
        ) = AnimatedSuccess(
            visible = visible,
            size = size,
            showParticles = true,
            particleCount = 30
        )

        /**
         * Create a large animated success for emphasis
         */
        fun large(
            visible: Boolean = true,
            size: Float = 96f
        ) = AnimatedSuccess(
            visible = visible,
            size = size
        )

        /**
         * Create an animated success with custom color
         */
        fun withColor(
            visible: Boolean = true,
            color: String,
            contentDescription: String? = null
        ) = AnimatedSuccess(
            visible = visible,
            color = color,
            contentDescription = contentDescription
        )

        /**
         * Create a subtle success (smaller, no particles)
         */
        fun subtle(
            visible: Boolean = true,
            size: Float = 48f
        ) = AnimatedSuccess(
            visible = visible,
            size = size,
            showParticles = false
        )
    }
}
