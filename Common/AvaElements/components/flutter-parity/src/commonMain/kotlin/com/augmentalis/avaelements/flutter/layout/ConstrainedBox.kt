package com.augmentalis.avaelements.flutter.layout

import kotlinx.serialization.Serializable

/**
 * A widget that imposes additional constraints on its child.
 *
 * A [ConstrainedBox] is useful when you want to apply additional constraints to a child
 * beyond what the parent might specify. The constraints from the parent and the
 * [ConstrainedBox] are combined, with the tighter constraint winning.
 *
 * This is equivalent to Flutter's [ConstrainedBox] widget.
 *
 * Example with minimum size:
 * ```kotlin
 * ConstrainedBox(
 *     constraints = BoxConstraints(
 *         minWidth = 200f,
 *         minHeight = 100f
 *     ),
 *     child = Container(
 *         color = Colors.Blue,
 *         child = Text("This will be at least 200x100")
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * ConstrainedBox(
 *   constraints: BoxConstraints(
 *     minWidth: 200,
 *     minHeight: 100,
 *   ),
 *   child: Container(
 *     color: Colors.blue,
 *     child: Text('This will be at least 200x100'),
 *   ),
 * )
 * ```
 *
 * Example with maximum size:
 * ```kotlin
 * ConstrainedBox(
 *     constraints = BoxConstraints(
 *         maxWidth = 300f,
 *         maxHeight = 200f
 *     ),
 *     child = Image(url = "large_image.jpg")
 * )
 * ```
 *
 * Example with exact size (using tight constraints):
 * ```kotlin
 * ConstrainedBox(
 *     constraints = BoxConstraints.tight(
 *         width = 150f,
 *         height = 150f
 *     ),
 *     child = Container(color = Colors.Red)
 * )
 * ```
 *
 * @property constraints The additional constraints to impose on the child
 * @property child The widget below this widget in the tree
 *
 * @see SizedBox
 * @see BoxConstraints
 * @since 2.1.0
 */
@Serializable
data class ConstrainedBoxComponent(
    val constraints: BoxConstraints,
    val child: Any
)

/**
 * Immutable layout constraints for box layout.
 *
 * A size respects a [BoxConstraints] if:
 * - [minWidth] <= size.width <= [maxWidth]
 * - [minHeight] <= size.height <= [maxHeight]
 *
 * The constraints themselves must satisfy these relations:
 * - 0 <= [minWidth] <= [maxWidth] <= infinity
 * - 0 <= [minHeight] <= [maxHeight] <= infinity
 *
 * Infinity is represented by [Float.POSITIVE_INFINITY].
 *
 * @property minWidth The minimum width that satisfies the constraints
 * @property maxWidth The maximum width that satisfies the constraints
 * @property minHeight The minimum height that satisfies the constraints
 * @property maxHeight The maximum height that satisfies the constraints
 *
 * @see ConstrainedBox
 * @since 2.1.0
 */
@Serializable
data class BoxConstraints(
    val minWidth: Float = 0f,
    val maxWidth: Float = Float.POSITIVE_INFINITY,
    val minHeight: Float = 0f,
    val maxHeight: Float = Float.POSITIVE_INFINITY
) {
    init {
        require(minWidth >= 0) { "minWidth must be non-negative, got $minWidth" }
        require(minHeight >= 0) { "minHeight must be non-negative, got $minHeight" }
        require(minWidth <= maxWidth) { "minWidth ($minWidth) must be <= maxWidth ($maxWidth)" }
        require(minHeight <= maxHeight) { "minHeight ($minHeight) must be <= maxHeight ($maxHeight)" }
    }

    /**
     * Whether there is exactly one width value that satisfies the constraints
     */
    val hasTightWidth: Boolean
        get() = minWidth >= maxWidth

    /**
     * Whether there is exactly one height value that satisfies the constraints
     */
    val hasTightHeight: Boolean
        get() = minHeight >= maxHeight

    /**
     * Whether there is exactly one size that satisfies the constraints
     */
    val isTight: Boolean
        get() = hasTightWidth && hasTightHeight

    /**
     * Whether the given size satisfies the constraints
     */
    fun isSatisfiedBy(width: Float, height: Float): Boolean {
        return width >= minWidth && width <= maxWidth &&
                height >= minHeight && height <= maxHeight
    }

    /**
     * Returns the width that both satisfies the constraints and is as close as possible to the given width
     */
    fun constrainWidth(width: Float): Float {
        return width.coerceIn(minWidth, maxWidth)
    }

    /**
     * Returns the height that both satisfies the constraints and is as close as possible to the given height
     */
    fun constrainHeight(height: Float): Float {
        return height.coerceIn(minHeight, maxHeight)
    }

    companion object {
        /**
         * Creates box constraints that require the given width and height
         */
        fun tight(width: Float, height: Float) = BoxConstraints(
            minWidth = width,
            maxWidth = width,
            minHeight = height,
            maxHeight = height
        )

        /**
         * Creates box constraints that require the given width but are loose in the height
         */
        fun tightWidth(width: Float) = BoxConstraints(
            minWidth = width,
            maxWidth = width,
            minHeight = 0f,
            maxHeight = Float.POSITIVE_INFINITY
        )

        /**
         * Creates box constraints that require the given height but are loose in the width
         */
        fun tightHeight(height: Float) = BoxConstraints(
            minWidth = 0f,
            maxWidth = Float.POSITIVE_INFINITY,
            minHeight = height,
            maxHeight = height
        )

        /**
         * Creates box constraints that forbid sizes larger than the given size
         */
        fun loose(width: Float, height: Float) = BoxConstraints(
            minWidth = 0f,
            maxWidth = width,
            minHeight = 0f,
            maxHeight = height
        )

        /**
         * Creates box constraints that expand to fill as much space as possible
         */
        fun expand(width: Float = Float.POSITIVE_INFINITY, height: Float = Float.POSITIVE_INFINITY) =
            BoxConstraints(
                minWidth = width,
                maxWidth = width,
                minHeight = height,
                maxHeight = height
            )
    }
}
