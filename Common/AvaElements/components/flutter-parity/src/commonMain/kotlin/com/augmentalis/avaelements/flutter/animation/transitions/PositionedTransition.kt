package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A widget that animates its position within a [Stack].
 *
 * Only works inside a [Stack] widget. The animation can change the child's position
 * using absolute values for top, right, bottom, and left.
 *
 * This is equivalent to Flutter's [PositionedTransition] widget.
 *
 * Example:
 * ```kotlin
 * Stack(
 *     children = listOf(
 *         PositionedTransition(
 *             rect = RelativeRect.fromLTRB(10f, 10f, 10f, null),
 *             child = Container(
 *                 width = Size.dp(100f),
 *                 height = Size.dp(100f),
 *                 color = Colors.Blue
 *             )
 *         )
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * Stack(
 *   children: [
 *     PositionedTransition(
 *       rect: RectAnimation,
 *       child: Container(
 *         width: 100,
 *         height: 100,
 *         color: Colors.blue,
 *       ),
 *     ),
 *   ],
 * )
 * ```
 *
 * Performance considerations:
 * - Uses GPU-accelerated positioning
 * - Triggers layout only when necessary
 * - Targets 60 FPS for smooth transitions
 * - More expensive than SlideTransition as it can trigger layout
 *
 * @property rect The bounding rectangle for the child widget, relative to the Stack
 * @property child The widget below this widget in the tree
 *
 * @see SlideTransition
 * @see RelativePositionedTransition
 * @see Positioned
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class PositionedTransition(
    val rect: RelativeRect,
    val child: Any
) {
    /**
     * Returns accessibility description for this transition.
     */
    fun getAccessibilityDescription(): String {
        return buildString {
            append("Positioned ")
            rect.left?.let { append("$it from left, ") }
            rect.top?.let { append("$it from top, ") }
            rect.right?.let { append("$it from right, ") }
            rect.bottom?.let { append("$it from bottom") }
        }.trimEnd(',', ' ')
    }

    /**
     * A rect specified by offsets from each of the sides of a rectangle.
     */
    @Serializable
    data class RelativeRect(
        val left: Float?,
        val top: Float?,
        val right: Float?,
        val bottom: Float?
    ) {
        companion object {
            /**
             * Creates a RelativeRect with specific values for each side.
             */
            fun fromLTRB(left: Float?, top: Float?, right: Float?, bottom: Float?) =
                RelativeRect(left, top, right, bottom)

            /**
             * A rect that covers the entire container.
             */
            val fill = RelativeRect(0f, 0f, 0f, 0f)
        }
    }

    companion object {
        /**
         * Default animation duration in milliseconds.
         */
        const val DEFAULT_ANIMATION_DURATION = 300
    }
}
