package com.augmentalis.avaelements.flutter.layout

import kotlinx.serialization.Serializable

/**
 * A widget that centers its child within itself.
 *
 * This widget will be as large as possible if its dimensions are constrained and
 * [widthFactor] and [heightFactor] are null. If a dimension is unconstrained and the
 * corresponding size factor is null then the widget will match its child's size in that dimension.
 * If a size factor is non-null then the corresponding dimension of this widget will be the product
 * of the child's dimension and the size factor.
 *
 * This is a convenience widget that is equivalent to an [Align] widget with [AlignmentGeometry.Center].
 *
 * This is equivalent to Flutter's [Center] widget.
 *
 * Example:
 * ```kotlin
 * Center(
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
 * Center(
 *   child: Container(
 *     width: 100,
 *     height: 100,
 *     color: Colors.blue,
 *   ),
 * )
 * ```
 *
 * Example with size factors:
 * ```kotlin
 * Center(
 *     widthFactor = 2.0f,
 *     heightFactor = 2.0f,
 *     child = Container(
 *         width = Size.dp(50f),
 *         height = Size.dp(50f),
 *         color = Colors.Red
 *     )
 * )
 * // Center widget will be 100x100 (child size * factor)
 * ```
 *
 * @property widthFactor If non-null, sets its width to the child's width multiplied by this factor.
 *                       Must be non-negative. If null, the widget takes all available width.
 * @property heightFactor If non-null, sets its height to the child's height multiplied by this factor.
 *                        Must be non-negative. If null, the widget takes all available height.
 * @property child The widget below this widget in the tree
 *
 * @see Align
 * @since 2.1.0
 */
@Serializable
data class CenterComponent(
    val widthFactor: Float? = null,
    val heightFactor: Float? = null,
    val child: Any
) {
    init {
        widthFactor?.let { require(it >= 0) { "widthFactor must be non-negative, got $it" } }
        heightFactor?.let { require(it >= 0) { "heightFactor must be non-negative, got $it" } }
    }
}
