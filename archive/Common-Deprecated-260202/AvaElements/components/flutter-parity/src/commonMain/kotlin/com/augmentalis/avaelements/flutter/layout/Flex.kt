package com.augmentalis.avaelements.flutter.layout

import com.augmentalis.avaelements.core.types.Spacing
import kotlinx.serialization.Serializable

/**
 * A widget that displays its children in a one-dimensional array.
 *
 * [Flex] is a generic widget that provides a flexible layout similar to CSS Flexbox.
 * [Row] and [Column] are specialized versions of [Flex] with fixed directions.
 *
 * The [Flex] widget allows children to be laid out in the main axis (either horizontally
 * or vertically). In the cross axis, children are positioned according to their
 * [crossAxisAlignment].
 *
 * This is equivalent to Flutter's [Flex] widget.
 *
 * Example:
 * ```kotlin
 * Flex(
 *     direction = FlexDirection.Horizontal,
 *     mainAxisAlignment = MainAxisAlignment.SpaceBetween,
 *     crossAxisAlignment = CrossAxisAlignment.Center,
 *     children = listOf(
 *         Container(width = Size.dp(50f), height = Size.dp(50f), color = Colors.Red),
 *         Expanded(
 *             child = Container(height = Size.dp(50f), color = Colors.Blue)
 *         ),
 *         Container(width = Size.dp(50f), height = Size.dp(50f), color = Colors.Green)
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * Flex(
 *   direction: Axis.horizontal,
 *   mainAxisAlignment: MainAxisAlignment.spaceBetween,
 *   crossAxisAlignment: CrossAxisAlignment.center,
 *   children: [
 *     Container(width: 50, height: 50, color: Colors.red),
 *     Expanded(
 *       child: Container(height: 50, color: Colors.blue),
 *     ),
 *     Container(width: 50, height: 50, color: Colors.green),
 *   ],
 * )
 * ```
 *
 * @property direction The direction to use as the main axis
 * @property mainAxisAlignment How the children should be placed along the main axis
 * @property mainAxisSize How much space should be occupied in the main axis
 * @property crossAxisAlignment How the children should be placed along the cross axis
 * @property verticalDirection Determines the order to lay children out vertically
 * @property textDirection Determines the order to lay children out horizontally (for RTL support)
 * @property children The widgets to display
 *
 * @see Row
 * @see Column
 * @see Expanded
 * @see Flexible
 * @since 2.1.0
 */
@Serializable
data class FlexComponent(
    val direction: FlexDirection,
    val mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
    val mainAxisSize: MainAxisSize = MainAxisSize.Max,
    val crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.Center,
    val verticalDirection: VerticalDirection = VerticalDirection.Down,
    val textDirection: TextDirection? = null,
    val children: List<Any> = emptyList()
)

/**
 * The direction in which a flex layout places its children
 */
@Serializable
enum class FlexDirection {
    /**
     * Lay out children horizontally (left to right or right to left based on text direction)
     */
    Horizontal,

    /**
     * Lay out children vertically (top to bottom or bottom to top based on vertical direction)
     */
    Vertical
}

/**
 * How the children should be placed along the main axis in a flex layout
 */
@Serializable
enum class MainAxisAlignment {
    /**
     * Place the children as close to the start of the main axis as possible
     */
    Start,

    /**
     * Place the children as close to the end of the main axis as possible
     */
    End,

    /**
     * Place the children as close to the middle of the main axis as possible
     */
    Center,

    /**
     * Place the free space evenly between the children
     */
    SpaceBetween,

    /**
     * Place the free space evenly between the children as well as half of that space
     * before and after the first and last child
     */
    SpaceAround,

    /**
     * Place the free space evenly between the children as well as before and after
     * the first and last child
     */
    SpaceEvenly
}

/**
 * How much space should be occupied in the main axis
 */
@Serializable
enum class MainAxisSize {
    /**
     * Minimize the amount of free space along the main axis
     */
    Min,

    /**
     * Maximize the amount of free space along the main axis
     */
    Max
}

/**
 * How the children should be placed along the cross axis in a flex layout
 */
@Serializable
enum class CrossAxisAlignment {
    /**
     * Place the children with their start edge aligned with the start side of the cross axis
     */
    Start,

    /**
     * Place the children as close to the end of the cross axis as possible
     */
    End,

    /**
     * Place the children so that their centers align with the middle of the cross axis
     */
    Center,

    /**
     * Require the children to fill the cross axis
     */
    Stretch,

    /**
     * Place the children along the cross axis such that their baselines match
     */
    Baseline
}

/**
 * The reading direction for text (for RTL support)
 */
@Serializable
enum class TextDirection {
    /**
     * Left-to-right text direction
     */
    LTR,

    /**
     * Right-to-left text direction
     */
    RTL
}
