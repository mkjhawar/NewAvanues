package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A widget that animates its position within a [Stack] using relative coordinates.
 *
 * Similar to [PositionedTransition], but uses [RelativeRect] which specifies the
 * position using relative coordinates. This makes it easier to create animations
 * that work well with different screen sizes.
 *
 * This is equivalent to Flutter's [RelativePositionedTransition] widget.
 *
 * Example:
 * ```kotlin
 * Stack(
 *     children = listOf(
 *         RelativePositionedTransition(
 *             rect = RelativeRect.fromLTRB(0.1f, 0.1f, 0.1f, 0.1f),
 *             size = Size.dp(200f) to Size.dp(200f),
 *             child = Container(color = Colors.Blue)
 *         )
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * Stack(
 *   children: [
 *     RelativePositionedTransition(
 *       rect: RectAnimation,
 *       size: Size(200, 200),
 *       child: Container(color: Colors.blue),
 *     ),
 *   ],
 * )
 * ```
 *
 * Performance considerations:
 * - GPU-accelerated positioning
 * - Triggers layout when size changes
 * - Targets 60 FPS for smooth transitions
 * - More flexible than PositionedTransition for responsive layouts
 *
 * @property rect The bounding rectangle using relative coordinates (0.0-1.0)
 * @property size The size of the container (width, height)
 * @property child The widget below this widget in the tree
 *
 * @see PositionedTransition
 * @see SlideTransition
 * @see Stack
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class RelativePositionedTransition(
    val rect: RelativeRect,
    val size: Pair<Float, Float>,
    val child: Any
) {
    init {
        require(size.first >= 0f) { "width must be non-negative, got ${size.first}" }
        require(size.second >= 0f) { "height must be non-negative, got ${size.second}" }
    }

    /**
     * Returns accessibility description for this transition.
     */
    fun getAccessibilityDescription(): String {
        return buildString {
            append("Positioned at ")
            rect.left?.let { append("${(it * 100).toInt()}% from left, ") }
            rect.top?.let { append("${(it * 100).toInt()}% from top, ") }
            rect.right?.let { append("${(it * 100).toInt()}% from right, ") }
            rect.bottom?.let { append("${(it * 100).toInt()}% from bottom") }
        }.trimEnd(',', ' ')
    }

    /**
     * A rect specified by relative offsets (0.0-1.0) from each side.
     */
    @Serializable
    data class RelativeRect(
        val left: Float?,
        val top: Float?,
        val right: Float?,
        val bottom: Float?
    ) {
        init {
            left?.let { require(it in 0.0f..1.0f) { "left must be in range 0.0-1.0, got $it" } }
            top?.let { require(it in 0.0f..1.0f) { "top must be in range 0.0-1.0, got $it" } }
            right?.let { require(it in 0.0f..1.0f) { "right must be in range 0.0-1.0, got $it" } }
            bottom?.let { require(it in 0.0f..1.0f) { "bottom must be in range 0.0-1.0, got $it" } }
        }

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

            /**
             * A rect centered with 10% margin on all sides.
             */
            val centered = RelativeRect(0.1f, 0.1f, 0.1f, 0.1f)
        }
    }

    companion object {
        /**
         * Default animation duration in milliseconds.
         */
        const val DEFAULT_ANIMATION_DURATION = 300
    }
}
