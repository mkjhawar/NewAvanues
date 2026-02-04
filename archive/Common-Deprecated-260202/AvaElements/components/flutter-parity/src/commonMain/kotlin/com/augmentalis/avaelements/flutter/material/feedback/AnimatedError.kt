package com.augmentalis.avaelements.flutter.material.feedback

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * AnimatedError component - Flutter Material parity
 *
 * An animated error/cross icon with shake and scale animation for error states.
 * Provides visual feedback with attention-grabbing shake effect.
 *
 * **Web Equivalent:** Custom animated error (Framer Motion), `ErrorIcon` animated
 * **Material Design 3:** https://m3.material.io/styles/motion/overview
 *
 * ## Features
 * - Shake animation + scale for emphasis
 * - Smooth entrance/exit transitions
 * - Configurable size and color
 * - Red error color by default
 * - Circular cross/X icon
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * AnimatedError(
 *     visible = true,
 *     size = 64f,
 *     color = "#F44336",
 *     animationDuration = 500,
 *     shakeIntensity = 10f,
 *     contentDescription = "Payment failed"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether error icon is visible (triggers animation)
 * @property size Icon size in dp
 * @property color Optional icon color (hex string)
 * @property animationDuration Animation duration in milliseconds
 * @property shakeIntensity Shake amplitude in dp
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.2.0-feedback-components
 */
data class AnimatedError(
    override val type: String = "AnimatedError",
    override val id: String? = null,
    val visible: Boolean = true,
    val size: Float = 48f,
    val color: String? = null,
    val animationDuration: Int = 500,
    val shakeIntensity: Float = 10f,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective icon color (default to error red)
     */
    fun getEffectiveColor(): String {
        return color ?: "#F44336"
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        return contentDescription ?: "Error"
    }

    /**
     * Validate size and animation parameters
     */
    fun areParametersValid(): Boolean {
        return size > 0f && size <= 200f &&
               animationDuration > 0 && animationDuration <= 5000 &&
               shakeIntensity >= 0f && shakeIntensity <= 50f
    }

    companion object {
        /** Default error color (Material Red 500) */
        const val DEFAULT_ERROR_COLOR = "#F44336"

        /** Default icon size */
        const val DEFAULT_SIZE = 48f

        /** Default animation duration */
        const val DEFAULT_ANIMATION_DURATION = 500

        /** Default shake intensity */
        const val DEFAULT_SHAKE_INTENSITY = 10f

        /**
         * Create a simple animated error
         */
        fun simple(
            visible: Boolean = true
        ) = AnimatedError(
            visible = visible
        )

        /**
         * Create a large animated error for emphasis
         */
        fun large(
            visible: Boolean = true,
            size: Float = 72f,
            shakeIntensity: Float = 15f
        ) = AnimatedError(
            visible = visible,
            size = size,
            shakeIntensity = shakeIntensity
        )

        /**
         * Create an animated error with custom color
         */
        fun withColor(
            visible: Boolean = true,
            color: String,
            contentDescription: String? = null
        ) = AnimatedError(
            visible = visible,
            color = color,
            contentDescription = contentDescription
        )
    }
}
