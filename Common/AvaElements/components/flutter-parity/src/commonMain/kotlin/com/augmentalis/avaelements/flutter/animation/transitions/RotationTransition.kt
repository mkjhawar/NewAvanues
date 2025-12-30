package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.abs

/**
 * A widget that animates the rotation of a widget.
 *
 * The rotation value is in turns (360 degrees = 1.0 turn). A value of 0.5 turns rotates
 * the widget 180 degrees, 0.25 turns rotates 90 degrees, etc.
 *
 * This is equivalent to Flutter's [RotationTransition] widget.
 *
 * Example:
 * ```kotlin
 * RotationTransition(
 *     turns = 0.25f, // 90 degrees clockwise
 *     alignment = Alignment.Center,
 *     child = Icon(
 *         icon = Icons.ArrowForward,
 *         size = Size.dp(48f)
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * RotationTransition(
 *   turns: Tween<double>(begin: 0.0, end: 0.25).animate(animation),
 *   alignment: Alignment.center,
 *   child: Icon(
 *     Icons.arrow_forward,
 *     size: 48,
 *   ),
 * )
 * ```
 *
 * Performance considerations:
 * - Uses GPU-accelerated transform layer
 * - Does not trigger layout changes
 * - Targets 60 FPS for smooth transitions
 * - Very efficient even for complex widget hierarchies
 *
 * @property turns The rotation amount in turns (1.0 = 360 degrees, 0.5 = 180 degrees, etc.)
 *                 Positive values rotate clockwise, negative values rotate counter-clockwise.
 * @property child The widget below this widget in the tree
 * @property alignment The alignment of the origin, relative to the size of the box.
 *                     Defaults to center.
 *
 * @see ScaleTransition
 * @see FadeTransition
 * @see Transform
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class RotationTransition(
    val turns: Float,
    val child: Any,
    val alignment: Alignment = Alignment.Center
) {
    /**
     * Returns the rotation in degrees.
     */
    fun getTurnsDegrees(): Float = turns * 360f

    /**
     * Returns the rotation in radians.
     */
    fun getTurnsRadians(): Float = turns * 2f * PI.toFloat()

    /**
     * Returns accessibility description for this transition.
     */
    fun getAccessibilityDescription(): String {
        val degrees = getTurnsDegrees().toInt()
        val direction = if (turns >= 0) "clockwise" else "counter-clockwise"
        return "Rotated ${abs(degrees)} degrees $direction"
    }

    /**
     * Alignment options for the rotation origin.
     */
    enum class Alignment {
        TopLeft, TopCenter, TopRight,
        CenterLeft, Center, CenterRight,
        BottomLeft, BottomCenter, BottomRight
    }

    companion object {
        /**
         * Default animation duration in milliseconds.
         */
        const val DEFAULT_ANIMATION_DURATION = 300

        /**
         * Common rotation values.
         */
        object Rotations {
            const val QUARTER_TURN = 0.25f  // 90 degrees
            const val HALF_TURN = 0.5f      // 180 degrees
            const val THREE_QUARTER_TURN = 0.75f  // 270 degrees
            const val FULL_TURN = 1.0f      // 360 degrees
        }
    }
}
