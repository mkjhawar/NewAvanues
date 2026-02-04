package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A widget that prevents the user from interacting with widgets behind itself, and can
 * be animated.
 *
 * A modal barrier is often used in dialogs and popups to prevent the user from interacting
 * with the content behind the dialog. The barrier can be dismissible (tap to close) or
 * non-dismissible, and its color can be animated.
 *
 * This is equivalent to Flutter's [AnimatedModalBarrier] widget.
 *
 * Example:
 * ```kotlin
 * AnimatedModalBarrier(
 *     color = Color(0x80000000), // 50% black
 *     dismissible = true,
 *     onDismiss = { /* close dialog */ }
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * AnimatedModalBarrier(
 *   color: animation.drive(
 *     ColorTween(
 *       begin: Colors.transparent,
 *       end: Colors.black54,
 *     ),
 *   ),
 *   dismissible: true,
 * )
 * ```
 *
 * Performance considerations:
 * - Lightweight barrier rendering
 * - GPU-accelerated color animation
 * - Targets 60 FPS for smooth transitions
 * - Minimal impact on layout performance
 *
 * @property color The color of the modal barrier (ARGB hex format)
 * @property dismissible Whether the barrier can be dismissed by tapping
 * @property onDismiss Callback when the barrier is dismissed (only if dismissible)
 * @property semanticsLabel The semantic label for the barrier
 * @property barrierSemanticsDismissible Whether screen readers should announce the barrier as dismissible
 *
 * @see ModalBarrier
 * @see Dialog
 * @see BottomSheet
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class AnimatedModalBarrier(
    val color: Long,
    val dismissible: Boolean = true,
    val onDismiss: String? = null,
    val semanticsLabel: String? = null,
    val barrierSemanticsDismissible: Boolean = true
) {
    /**
     * Returns accessibility description for this barrier.
     */
    fun getAccessibilityDescription(): String {
        val label = semanticsLabel ?: "Modal barrier"
        return if (dismissible && barrierSemanticsDismissible) {
            "$label (tap to dismiss)"
        } else {
            label
        }
    }

    /**
     * Extracts the alpha component from the color (0-255).
     */
    fun getAlpha(): Int = ((color shr 24) and 0xFF).toInt()

    /**
     * Extracts the red component from the color (0-255).
     */
    fun getRed(): Int = ((color shr 16) and 0xFF).toInt()

    /**
     * Extracts the green component from the color (0-255).
     */
    fun getGreen(): Int = ((color shr 8) and 0xFF).toInt()

    /**
     * Extracts the blue component from the color (0-255).
     */
    fun getBlue(): Int = (color and 0xFF).toInt()

    /**
     * Returns the opacity as a fraction (0.0 to 1.0).
     */
    fun getOpacity(): Float = getAlpha() / 255f

    companion object {
        /**
         * Default animation duration in milliseconds.
         */
        const val DEFAULT_ANIMATION_DURATION = 300

        /**
         * Common barrier colors.
         */
        object Colors {
            const val TRANSPARENT = 0x00000000L
            const val BLACK_50 = 0x80000000L    // 50% black
            const val BLACK_70 = 0xB3000000L    // 70% black
            const val BLACK_90 = 0xE6000000L    // 90% black
            const val WHITE_50 = 0x80FFFFFFL    // 50% white
        }
    }
}
