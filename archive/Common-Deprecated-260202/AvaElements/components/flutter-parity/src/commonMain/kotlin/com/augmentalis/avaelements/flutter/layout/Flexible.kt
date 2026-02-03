package com.augmentalis.avaelements.flutter.layout

import kotlinx.serialization.Serializable

/**
 * A widget that controls how a child of a [Row], [Column], or [Flex] flexes.
 *
 * Using a [Flexible] widget gives a child of a [Row], [Column], or [Flex] the flexibility to expand to fill
 * the available space in the main axis (e.g., horizontally for a [Row] or vertically for a [Column]),
 * but unlike [Expanded], [Flexible] does not require the child to fill the available space.
 *
 * A [Flexible] widget must be a descendant of a [Row], [Column], or [Flex].
 *
 * The difference between [Flexible] and [Expanded] is that [Flexible] allows the child to be smaller than
 * the available space, while [Expanded] forces the child to fill the available space.
 *
 * This is equivalent to Flutter's [Flexible] widget.
 *
 * Example:
 * ```kotlin
 * Row(
 *     children = listOf(
 *         Container(width = Size.dp(100f), color = Colors.Red),
 *         Flexible(
 *             flex = 1,
 *             fit = FlexFit.Loose,
 *             child = Container(
 *                 width = Size.dp(50f), // Can be smaller than available space
 *                 color = Colors.Blue
 *             )
 *         ),
 *         Expanded(
 *             child = Container(color = Colors.Green) // Must fill available space
 *         )
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * Row(
 *   children: [
 *     Container(width: 100, color: Colors.red),
 *     Flexible(
 *       flex: 1,
 *       fit: FlexFit.loose,
 *       child: Container(width: 50, color: Colors.blue),
 *     ),
 *     Expanded(
 *       child: Container(color: Colors.green),
 *     ),
 *   ],
 * )
 * ```
 *
 * @property flex The flex factor to use for this child. Must be non-negative.
 *               Defaults to 1. The available space is divided among children based on their flex factors.
 * @property fit How a flexible child should be inscribed into the available space.
 *              [FlexFit.Tight] forces the child to fill the available space (like [Expanded]).
 *              [FlexFit.Loose] allows the child to be smaller than the available space.
 * @property child The widget below this widget in the tree
 *
 * @see Expanded
 * @see Spacer
 * @since 2.1.0
 */
@Serializable
data class FlexibleComponent(
    val flex: Int = 1,
    val fit: FlexFit = FlexFit.Loose,
    val child: Any
) {
    init {
        require(flex >= 0) { "flex must be non-negative, got $flex" }
    }
}

/**
 * How a flexible child should be inscribed into the available space
 */
@Serializable
enum class FlexFit {
    /**
     * The child is forced to fill the available space.
     *
     * The [Expanded] widget uses this fit.
     */
    Tight,

    /**
     * The child can be at most as large as the available space (but is allowed to be smaller).
     *
     * The [Flexible] widget uses this fit by default.
     */
    Loose
}
