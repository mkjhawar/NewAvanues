package com.augmentalis.avaelements.flutter.animation

import com.augmentalis.avaelements.core.types.Size
import kotlinx.serialization.Serializable

/**
 * A widget that animates its position implicitly within a Stack.
 *
 * The AnimatedPositioned widget is a positioned widget that animates its position when the
 * position properties change. It must be a descendant of a Stack widget.
 *
 * This is equivalent to Flutter's [AnimatedPositioned] widget.
 *
 * Example:
 * ```kotlin
 * var selected by remember { mutableStateOf(false) }
 *
 * Stack {
 *     AnimatedPositioned(
 *         duration = Duration.milliseconds(500),
 *         curve = Curves.FastOutSlowIn,
 *         left = if (selected) Size.dp(100f) else Size.dp(10f),
 *         top = if (selected) Size.dp(100f) else Size.dp(10f),
 *         child = Container(
 *             width = Size.dp(50f),
 *             height = Size.dp(50f),
 *             color = Colors.Red
 *         )
 *     )
 * }
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * Stack(
 *   children: [
 *     AnimatedPositioned(
 *       duration: Duration(milliseconds: 500),
 *       curve: Curves.fastOutSlowIn,
 *       left: selected ? 100.0 : 10.0,
 *       top: selected ? 100.0 : 10.0,
 *       child: Container(
 *         width: 50.0,
 *         height: 50.0,
 *         color: Colors.red,
 *       ),
 *     ),
 *   ],
 * )
 * ```
 *
 * Positioning Rules:
 * - At least one of [left], [right], [top], or [bottom] must be non-null
 * - If both [left] and [right] are non-null, [width] must be null
 * - If both [top] and [bottom] are non-null, [height] must be null
 * - Null values mean the widget can size itself in that dimension
 *
 * Performance Considerations:
 * - Position animations use Compose's Offset animation
 * - Runs at 60 FPS with hardware acceleration
 * - Does not trigger full Stack relayout, only position updates
 * - Child widget is not rebuilt during animation
 *
 * Common Use Cases:
 * - Animated overlays and tooltips
 * - Drag and drop with snap-back
 * - Floating action button position changes
 * - Picture-in-picture transitions
 * - Animated layout transitions
 *
 * @property duration The duration over which to animate the position
 * @property curve The curve to apply when animating the position
 * @property left The distance from the left edge of the Stack
 * @property top The distance from the top edge of the Stack
 * @property right The distance from the right edge of the Stack
 * @property bottom The distance from the bottom edge of the Stack
 * @property width The width of the child. Cannot be set if both left and right are set
 * @property height The height of the child. Cannot be set if both top and bottom are set
 * @property child The widget below this widget in the tree
 * @property onEnd Called when the animation completes
 *
 * @see AnimatedContainer
 * @see AnimatedAlign
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class AnimatedPositioned(
    val duration: Duration,
    val curve: Curve = Curve.Linear,
    val left: Size? = null,
    val top: Size? = null,
    val right: Size? = null,
    val bottom: Size? = null,
    val width: Size? = null,
    val height: Size? = null,
    val child: Any,
    val onEnd: (() -> Unit)? = null
) {
    init {
        require(duration.milliseconds > 0) {
            "duration must be positive, got ${duration.milliseconds}ms"
        }

        // At least one position must be specified
        require(left != null || top != null || right != null || bottom != null) {
            "At least one of left, top, right, or bottom must be non-null"
        }

        // Cannot specify both edges and width/height
        if (left != null && right != null) {
            require(width == null) {
                "Cannot specify both left and right along with width"
            }
        }

        if (top != null && bottom != null) {
            require(height == null) {
                "Cannot specify both top and bottom along with height"
            }
        }
    }

    companion object {
        /**
         * Creates an AnimatedPositioned that fills the entire Stack
         */
        fun fill(
            duration: Duration,
            curve: Curve = Curve.Linear,
            child: Any,
            onEnd: (() -> Unit)? = null
        ) = AnimatedPositioned(
            duration = duration,
            curve = curve,
            left = Size.dp(0f),
            top = Size.dp(0f),
            right = Size.dp(0f),
            bottom = Size.dp(0f),
            child = child,
            onEnd = onEnd
        )

        /**
         * Creates an AnimatedPositioned from a Rect
         */
        fun fromRect(
            duration: Duration,
            rect: Rect,
            curve: Curve = Curve.Linear,
            child: Any,
            onEnd: (() -> Unit)? = null
        ) = AnimatedPositioned(
            duration = duration,
            curve = curve,
            left = Size.dp(rect.left),
            top = Size.dp(rect.top),
            width = Size.dp(rect.width),
            height = Size.dp(rect.height),
            child = child,
            onEnd = onEnd
        )
    }
}

/**
 * A rectangle used for positioning
 */
@Serializable
data class Rect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height

    companion object {
        fun fromLTWH(left: Float, top: Float, width: Float, height: Float) =
            Rect(left, top, width, height)

        fun fromLTRB(left: Float, top: Float, right: Float, bottom: Float) =
            Rect(left, top, right - left, bottom - top)
    }
}
