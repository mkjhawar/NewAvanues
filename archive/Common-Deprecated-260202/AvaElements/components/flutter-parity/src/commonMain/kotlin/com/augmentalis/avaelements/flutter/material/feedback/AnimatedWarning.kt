package com.augmentalis.avaelements.flutter.material.feedback

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * AnimatedWarning component - Flutter Material parity
 *
 * An animated warning/alert icon with pulse and scale animation for warning states.
 * Features an exclamation mark in a triangle with attention-grabbing animation.
 *
 * **Web Equivalent:** Custom animated warning (Framer Motion), `WarningIcon` animated
 * **Material Design 3:** https://m3.material.io/styles/motion/overview
 *
 * ## Features
 * - Pulse animation to draw attention
 * - Scale-in animation on appear
 * - Smooth entrance/exit transitions
 * - Configurable size and color
 * - Amber/orange warning color by default
 * - Triangle exclamation icon
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * AnimatedWarning(
 *     visible = true,
 *     size = 64f,
 *     color = "#FF9800",
 *     animationDuration = 500,
 *     pulseCount = 2,
 *     contentDescription = "Low disk space warning"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether warning icon is visible (triggers animation)
 * @property size Icon size in dp
 * @property color Optional icon color (hex string)
 * @property animationDuration Animation duration in milliseconds
 * @property pulseCount Number of pulse cycles
 * @property pulseIntensity Pulse scale factor (1.0 = no pulse)
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.2.0-feedback-components
 */
data class AnimatedWarning(
    override val type: String = "AnimatedWarning",
    override val id: String? = null,
    val visible: Boolean = true,
    val size: Float = 56f,
    val color: String? = null,
    val animationDuration: Int = 500,
    val pulseCount: Int = 2,
    val pulseIntensity: Float = 1.1f,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective icon color (default to warning amber/orange)
     */
    fun getEffectiveColor(): String {
        return color ?: "#FF9800"
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        return contentDescription ?: "Warning"
    }

    /**
     * Validate size and animation parameters
     */
    fun areParametersValid(): Boolean {
        return size > 0f && size <= 200f &&
               animationDuration > 0 && animationDuration <= 5000 &&
               pulseCount >= 0 && pulseCount <= 10 &&
               pulseIntensity >= 1.0f && pulseIntensity <= 2.0f
    }

    companion object {
        /** Default warning color (Material Orange 500) */
        const val DEFAULT_WARNING_COLOR = "#FF9800"

        /** Alternative warning color (Material Amber 500) */
        const val AMBER_WARNING_COLOR = "#FFC107"

        /** Default icon size */
        const val DEFAULT_SIZE = 56f

        /** Default animation duration */
        const val DEFAULT_ANIMATION_DURATION = 500

        /** Default pulse count */
        const val DEFAULT_PULSE_COUNT = 2

        /** Default pulse intensity */
        const val DEFAULT_PULSE_INTENSITY = 1.1f

        /**
         * Create a simple animated warning
         */
        fun simple(
            visible: Boolean = true
        ) = AnimatedWarning(
            visible = visible
        )

        /**
         * Create a large animated warning for emphasis
         */
        fun large(
            visible: Boolean = true,
            size: Float = 80f,
            pulseCount: Int = 3
        ) = AnimatedWarning(
            visible = visible,
            size = size,
            pulseCount = pulseCount
        )

        /**
         * Create an animated warning with custom color
         */
        fun withColor(
            visible: Boolean = true,
            color: String,
            contentDescription: String? = null
        ) = AnimatedWarning(
            visible = visible,
            color = color,
            contentDescription = contentDescription
        )

        /**
         * Create a subtle warning (no pulse)
         */
        fun subtle(
            visible: Boolean = true,
            size: Float = 48f
        ) = AnimatedWarning(
            visible = visible,
            size = size,
            pulseCount = 0
        )

        /**
         * Create an urgent warning (more pulses, larger)
         */
        fun urgent(
            visible: Boolean = true,
            size: Float = 72f
        ) = AnimatedWarning(
            visible = visible,
            size = size,
            pulseCount = 3,
            pulseIntensity = 1.15f
        )
    }
}
