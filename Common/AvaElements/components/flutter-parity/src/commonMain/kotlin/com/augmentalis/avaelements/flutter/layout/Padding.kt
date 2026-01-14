package com.augmentalis.avaelements.flutter.layout

import com.augmentalis.avaelements.core.types.Spacing
import kotlinx.serialization.Serializable

/**
 * A widget that insets its child by the given padding.
 *
 * When passing [padding] to a widget, the widget insets (or pads) its child's bounds by that amount.
 *
 * This is equivalent to Flutter's [Padding] widget.
 *
 * Example:
 * ```kotlin
 * Padding(
 *     padding = Spacing.all(16f),
 *     child = Text("Hello, World!")
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * Padding(
 *   padding: EdgeInsets.all(16.0),
 *   child: Text('Hello, World!'),
 * )
 * ```
 *
 * Example with asymmetric padding:
 * ```kotlin
 * Padding(
 *     padding = Spacing.of(
 *         top = 8f,
 *         right = 16f,
 *         bottom = 8f,
 *         left = 16f
 *     ),
 *     child = Text("Asymmetric padding")
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * Padding(
 *   padding: EdgeInsets.only(
 *     top: 8.0,
 *     right: 16.0,
 *     bottom: 8.0,
 *     left: 16.0,
 *   ),
 *   child: Text('Asymmetric padding'),
 * )
 * ```
 *
 * @property padding The amount of space by which to inset the child
 * @property child The widget below this widget in the tree
 *
 * @see Container
 * @see Spacing
 * @since 2.1.0
 */
@Serializable
data class PaddingComponent(
    val padding: Spacing,
    val child: Any
)
