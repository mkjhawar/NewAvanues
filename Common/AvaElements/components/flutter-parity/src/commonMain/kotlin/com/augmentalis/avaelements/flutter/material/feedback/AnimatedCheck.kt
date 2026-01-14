package com.augmentalis.avaelements.flutter.material.feedback

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * AnimatedCheck component - Flutter Material parity
 *
 * An animated checkmark icon with bouncy spring animation for success states.
 * Scales in with a pleasing spring effect to provide visual feedback.
 *
 * **Web Equivalent:** Custom animated check (Framer Motion), `CheckCircleIcon` animated
 * **Material Design 3:** https://m3.material.io/styles/motion/overview
 *
 * ## Features
 * - Bouncy scale-in animation using spring physics
 * - Smooth entrance/exit transitions
 * - Configurable size and color
 * - Green success color by default
 * - Circular checkmark icon
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * AnimatedCheck(
 *     visible = true,
 *     size = 64f,
 *     color = "#4CAF50",
 *     animationDuration = 500,
 *     contentDescription = "Payment successful"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether check is visible (triggers animation)
 * @property size Icon size in dp
 * @property color Optional icon color (hex string)
 * @property animationDuration Animation duration in milliseconds
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.2.0-feedback-components
 */
data class AnimatedCheck(
    override val type: String = "AnimatedCheck",
    override val id: String? = null,
    val visible: Boolean = true,
    val size: Float = 48f,
    val color: String? = null,
    val animationDuration: Int = 500,
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
               animationDuration > 0 && animationDuration <= 5000
    }

    companion object {
        /** Default success color (Material Green 500) */
        const val DEFAULT_SUCCESS_COLOR = "#4CAF50"

        /** Default icon size */
        const val DEFAULT_SIZE = 48f

        /** Default animation duration */
        const val DEFAULT_ANIMATION_DURATION = 500

        /**
         * Create a simple animated check
         */
        fun simple(
            visible: Boolean = true
        ) = AnimatedCheck(
            visible = visible
        )

        /**
         * Create a large animated check for emphasis
         */
        fun large(
            visible: Boolean = true,
            size: Float = 72f
        ) = AnimatedCheck(
            visible = visible,
            size = size
        )

        /**
         * Create an animated check with custom color
         */
        fun withColor(
            visible: Boolean = true,
            color: String,
            contentDescription: String? = null
        ) = AnimatedCheck(
            visible = visible,
            color = color,
            contentDescription = contentDescription
        )
    }
}
