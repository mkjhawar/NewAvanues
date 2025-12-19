package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A widget that animates the position of a widget relative to its normal position.
 *
 * The translation is expressed as an [Offset] scaled to the child's size. For example,
 * an [Offset] with a dx of 0.25 will result in a horizontal translation of one quarter
 * the width of the child.
 *
 * This is equivalent to Flutter's [SlideTransition] widget.
 *
 * Example:
 * ```kotlin
 * SlideTransition(
 *     position = Offset(0.0f, -1.0f), // Slide from top
 *     child = Container(
 *         width = Size.dp(200f),
 *         height = Size.dp(100f),
 *         color = Colors.Blue
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * SlideTransition(
 *   position: Tween<Offset>(
 *     begin: Offset(0.0, -1.0),
 *     end: Offset.zero,
 *   ).animate(animation),
 *   child: Container(
 *     width: 200,
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
 * - Efficient for complex widget hierarchies
 *
 * @property position The relative position offset (relative to child's size)
 *                    - Offset(1.0f, 0.0f) = one full width to the right
 *                    - Offset(0.0f, 1.0f) = one full height down
 *                    - Offset(-1.0f, 0.0f) = one full width to the left
 *                    - Offset(0.0f, -1.0f) = one full height up
 * @property child The widget below this widget in the tree
 * @property textDirection The text direction to use for resolving the position.
 *                         Only matters when the position has a horizontal component.
 *
 * @see FadeTransition
 * @see ScaleTransition
 * @see PositionedTransition
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class SlideTransition(
    val position: Offset,
    val child: Any,
    val textDirection: TextDirection = TextDirection.Ltr
) {
    /**
     * Returns accessibility description for this transition.
     */
    fun getAccessibilityDescription(): String {
        val (dx, dy) = position
        return buildString {
            append("Positioned at ")
            when {
                dx > 0 -> append("${(dx * 100).toInt()}% right")
                dx < 0 -> append("${(-dx * 100).toInt()}% left")
            }
            if (dx != 0.0f && dy != 0.0f) append(", ")
            when {
                dy > 0 -> append("${(dy * 100).toInt()}% down")
                dy < 0 -> append("${(-dy * 100).toInt()}% up")
            }
            if (dx == 0.0f && dy == 0.0f) append("center")
        }
    }

    /**
     * Represents a 2D offset.
     */
    @Serializable
    data class Offset(
        val dx: Float,
        val dy: Float
    ) {
        companion object {
            val zero = Offset(0.0f, 0.0f)
        }
    }

    /**
     * Text direction for layout.
     */
    enum class TextDirection {
        Ltr, // Left to right
        Rtl  // Right to left
    }

    companion object {
        /**
         * Default animation duration in milliseconds.
         */
        const val DEFAULT_ANIMATION_DURATION = 300

        /**
         * Common slide directions as predefined offsets.
         */
        object Directions {
            val fromTop = Offset(0.0f, -1.0f)
            val fromBottom = Offset(0.0f, 1.0f)
            val fromLeft = Offset(-1.0f, 0.0f)
            val fromRight = Offset(1.0f, 0.0f)
            val center = Offset.zero
        }
    }
}
