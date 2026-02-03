package com.augmentalis.avaelements.flutter.layout

import com.augmentalis.avaelements.core.types.Size
import kotlinx.serialization.Serializable

/**
 * A box with a specified size.
 *
 * If given a child, this widget forces its child to have a specific width and/or height
 * (assuming values are permitted by this widget's parent). If either the width or height is null,
 * this widget will size itself to match the child's size in that dimension.
 *
 * If not given a child, [SizedBox] will try to size itself as close to the specified height
 * and width as possible given the parent's constraints. If [height] or [width] is null or
 * unspecified, it will be treated as zero.
 *
 * The [SizedBox.expand] constructor can be used to make a SizedBox that sizes itself to fit
 * the parent. It is equivalent to setting width and height to [Size.Fill].
 *
 * This is equivalent to Flutter's [SizedBox] widget.
 *
 * Example with specific dimensions:
 * ```kotlin
 * SizedBox(
 *     width = Size.dp(200f),
 *     height = Size.dp(100f),
 *     child = Container(color = Colors.Blue)
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * SizedBox(
 *   width: 200,
 *   height: 100,
 *   child: Container(color: Colors.blue),
 * )
 * ```
 *
 * Example as spacer:
 * ```kotlin
 * Column(
 *     children = listOf(
 *         Text("First"),
 *         SizedBox(height = Size.dp(20f)), // Vertical spacer
 *         Text("Second")
 *     )
 * )
 * ```
 *
 * Example expanding to fill parent:
 * ```kotlin
 * SizedBox.expand(
 *     child = Container(color = Colors.Blue)
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * SizedBox.expand(
 *   child: Container(color: Colors.blue),
 * )
 * ```
 *
 * @property width The width of the box. If null, the box will size itself to its child's width.
 * @property height The height of the box. If null, the box will size itself to its child's height.
 * @property child The widget below this widget in the tree. If null, this widget will be as small as possible.
 *
 * @see ConstrainedBox
 * @see Container
 * @see Spacer
 * @since 2.1.0
 */
@Serializable
data class SizedBoxComponent(
    val width: Size? = null,
    val height: Size? = null,
    val child: Any? = null
) {
    companion object {
        /**
         * Creates a SizedBox that will become as large as its parent allows
         *
         * @param child The widget below this widget in the tree
         * @return SizedBox that expands to fill parent
         */
        fun expand(child: Any? = null) = SizedBoxComponent(
            width = Size.Fill,
            height = Size.Fill,
            child = child
        )

        /**
         * Creates a SizedBox that will become as small as possible
         *
         * @return Minimal SizedBox with no child
         */
        fun shrink() = SizedBoxComponent(
            width = Size.dp(0f),
            height = Size.dp(0f),
            child = null
        )

        /**
         * Creates a square SizedBox with the given dimension
         *
         * @param dimension The width and height of the box
         * @param child The widget below this widget in the tree
         * @return Square SizedBox
         */
        fun square(dimension: Size, child: Any? = null) = SizedBoxComponent(
            width = dimension,
            height = dimension,
            child = child
        )
    }
}
