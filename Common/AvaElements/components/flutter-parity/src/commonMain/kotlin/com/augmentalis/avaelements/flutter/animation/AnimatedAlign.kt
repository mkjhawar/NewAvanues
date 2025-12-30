package com.augmentalis.avaelements.flutter.animation

import kotlinx.serialization.Serializable

/**
 * A widget that animates its alignment implicitly.
 *
 * The AnimatedAlign widget animates the alignment of its child within itself over a given
 * duration whenever the alignment changes.
 *
 * This is equivalent to Flutter's [AnimatedAlign] widget.
 *
 * Example:
 * ```kotlin
 * var alignRight by remember { mutableStateOf(false) }
 *
 * AnimatedAlign(
 *     alignment = if (alignRight) AlignmentGeometry.CenterRight else AlignmentGeometry.CenterLeft,
 *     duration = Duration.milliseconds(400),
 *     curve = Curves.EaseInOut,
 *     child = Container(
 *         width = Size.dp(50f),
 *         height = Size.dp(50f),
 *         color = Colors.Blue
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * AnimatedAlign(
 *   alignment: alignRight ? Alignment.centerRight : Alignment.centerLeft,
 *   duration: Duration(milliseconds: 400),
 *   curve: Curves.easeInOut,
 *   child: Container(
 *     width: 50.0,
 *     height: 50.0,
 *     color: Colors.blue,
 *   ),
 * )
 * ```
 *
 * Example with custom alignment:
 * ```kotlin
 * AnimatedAlign(
 *     alignment = AlignmentGeometry.Custom(
 *         x = if (selected) 0.5f else -0.5f,
 *         y = if (selected) 0.5f else -0.5f
 *     ),
 *     duration = Duration.milliseconds(300),
 *     child = Icon(Icons.Star)
 * )
 * ```
 *
 * Alignment Coordinate System:
 * - X axis: -1.0 (left) to 1.0 (right)
 * - Y axis: -1.0 (top) to 1.0 (bottom)
 * - Center is (0.0, 0.0)
 *
 * Performance Considerations:
 * - Alignment animations run at 60 FPS using Compose's BiasAlignment
 * - Does not trigger full relayout, only position changes
 * - Child widget is not rebuilt during animation
 * - GPU-accelerated positioning
 *
 * Common Use Cases:
 * - Floating element positioning
 * - Toggle switch animations
 * - Focus indicators
 * - Gravity-based layouts
 * - Responsive alignment transitions
 *
 * @property alignment The target alignment to animate to
 * @property duration The duration over which to animate the alignment
 * @property curve The curve to apply when animating the alignment
 * @property child The widget below this widget in the tree
 * @property widthFactor If non-null, sets width to child's width multiplied by this factor
 * @property heightFactor If non-null, sets height to child's height multiplied by this factor
 * @property onEnd Called when the animation completes
 *
 * @see AnimatedContainer
 * @see AnimatedPositioned
 * @see AlignmentGeometry
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class AnimatedAlign(
    val alignment: AlignmentGeometry,
    val duration: Duration,
    val curve: Curve = Curve.Linear,
    val child: Any,
    val widthFactor: Float? = null,
    val heightFactor: Float? = null,
    val onEnd: (() -> Unit)? = null
) {
    init {
        require(duration.milliseconds > 0) {
            "duration must be positive, got ${duration.milliseconds}ms"
        }
        widthFactor?.let {
            require(it >= 0) { "widthFactor must be non-negative, got $it" }
        }
        heightFactor?.let {
            require(it >= 0) { "heightFactor must be non-negative, got $it" }
        }
    }

    companion object {
        /**
         * Default animation duration for alignment changes
         */
        const val DEFAULT_DURATION_MS = 200
    }
}
