package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A widget that animates the scale of a transformed widget.
 *
 * The scale is controlled by a value where 1.0 means no scaling, values less than 1.0
 * shrink the widget, and values greater than 1.0 enlarge it.
 *
 * This is equivalent to Flutter's [ScaleTransition] widget.
 *
 * Example:
 * ```kotlin
 * ScaleTransition(
 *     scale = 0.5f,
 *     alignment = Alignment.Center,
 *     child = Container(
 *         width = Size.dp(100f),
 *         height = Size.dp(100f),
 *         color = Colors.Blue
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * ScaleTransition(
 *   scale: animation,
 *   alignment: Alignment.center,
 *   child: Container(
 *     width: 100,
 *     height: 100,
 *     color: Colors.blue,
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
 * @property scale The scale factor (1.0 = normal size, 0.5 = half size, 2.0 = double size)
 *                Must be non-negative.
 * @property child The widget below this widget in the tree
 * @property alignment The alignment of the origin, relative to the size of the box.
 *                     Defaults to center.
 *
 * @see RotationTransition
 * @see SizeTransition
 * @see FadeTransition
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class ScaleTransition(
    val scale: Float,
    val child: Any,
    val alignment: Alignment = Alignment.Center
) {
    init {
        require(scale >= 0.0f) { "scale must be non-negative, got $scale" }
    }

    /**
     * Returns accessibility description for this transition.
     */
    fun getAccessibilityDescription(): String {
        return when {
            scale == 0.0f -> "Hidden (scaled to 0%)"
            scale == 1.0f -> "Normal size"
            scale < 1.0f -> "Scaled to ${(scale * 100).toInt()}% of normal size"
            else -> "Scaled to ${(scale * 100).toInt()}% of normal size"
        }
    }

    /**
     * Alignment options for the scale origin.
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
         * Normal scale (no scaling).
         */
        const val NORMAL_SCALE = 1.0f

        /**
         * Hidden scale (completely shrunk).
         */
        const val HIDDEN_SCALE = 0.0f

        /**
         * Typical scale for a "pop in" effect.
         */
        const val POP_SCALE = 1.2f
    }
}
