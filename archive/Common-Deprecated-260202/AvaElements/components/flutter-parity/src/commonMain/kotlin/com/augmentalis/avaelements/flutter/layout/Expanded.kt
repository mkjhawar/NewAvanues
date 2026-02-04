package com.augmentalis.avaelements.flutter.layout

import kotlinx.serialization.Serializable

/**
 * A widget that expands a child of a [Row], [Column], or [Flex] so that the child fills the available space.
 *
 * Using an [Expanded] widget makes a child of a [Row], [Column], or [Flex] expand to fill the available space
 * along the main axis (e.g., horizontally for a [Row] or vertically for a [Column]). If multiple children are
 * expanded, the available space is divided among them according to the [flex] factor.
 *
 * An [Expanded] widget must be a descendant of a [Row], [Column], or [Flex].
 *
 * This is equivalent to Flutter's [Expanded] widget.
 *
 * Example:
 * ```kotlin
 * Row(
 *     children = listOf(
 *         Container(width = Size.dp(100f), color = Colors.Red),
 *         Expanded(
 *             flex = 2,
 *             child = Container(color = Colors.Blue)
 *         ),
 *         Expanded(
 *             flex = 1,
 *             child = Container(color = Colors.Green)
 *         )
 *     )
 * )
 * // Red container is 100dp wide
 * // Blue container takes 2/3 of remaining space
 * // Green container takes 1/3 of remaining space
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * Row(
 *   children: [
 *     Container(width: 100, color: Colors.red),
 *     Expanded(
 *       flex: 2,
 *       child: Container(color: Colors.blue),
 *     ),
 *     Expanded(
 *       flex: 1,
 *       child: Container(color: Colors.green),
 *     ),
 *   ],
 * )
 * ```
 *
 * @property flex The flex factor to use for this child. Must be non-negative.
 *               Defaults to 1. The available space is divided among children based on their flex factors.
 * @property child The widget below this widget in the tree
 *
 * @see Flexible
 * @see Spacer
 * @since 2.1.0
 */
@Serializable
data class ExpandedComponent(
    val flex: Int = 1,
    val child: Any
) {
    init {
        require(flex >= 0) { "flex must be non-negative, got $flex" }
    }
}
