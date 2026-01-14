package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A widget that animates its own size and clips and aligns its child.
 *
 * [SizeTransition] acts as a [ClipRect] that animates either its width or height,
 * depending on the value of [axis]. The alignment of the child along the [axis] is
 * specified by the [axisAlignment].
 *
 * This is equivalent to Flutter's [SizeTransition] widget.
 *
 * Example:
 * ```kotlin
 * SizeTransition(
 *     sizeFactor = 0.5f,
 *     axis = Axis.Vertical,
 *     axisAlignment = 0.0f,
 *     child = Container(
 *         width = Size.dp(200f),
 *         height = Size.dp(100f),
 *         color = Colors.Blue
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * SizeTransition(
 *   sizeFactor: animation,
 *   axis: Axis.vertical,
 *   axisAlignment: 0.0,
 *   child: Container(
 *     width: 200,
 *     height: 100,
 *     color: Colors.blue,
 *   ),
 * )
 * ```
 *
 * Performance considerations:
 * - Triggers layout as size changes
 * - Uses clipping which can be expensive
 * - Targets 60 FPS for smooth transitions
 * - Consider FadeTransition for better performance when size doesn't matter
 *
 * @property sizeFactor The size factor (0.0 = collapsed, 1.0 = full size)
 *                      Must be in the range 0.0 to 1.0.
 * @property axis The axis along which to size the widget (horizontal or vertical)
 * @property axisAlignment The alignment of the child along the [axis]. Range: -1.0 to 1.0
 *                         - -1.0: align to the start (top for vertical, left for horizontal)
 *                         - 0.0: align to the center
 *                         - 1.0: align to the end (bottom for vertical, right for horizontal)
 * @property child The widget below this widget in the tree
 *
 * @see ScaleTransition
 * @see FadeTransition
 * @see AnimatedSize
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class SizeTransition(
    val sizeFactor: Float,
    val child: Any,
    val axis: Axis = Axis.Vertical,
    val axisAlignment: Float = 0.0f
) {
    init {
        require(sizeFactor in 0.0f..1.0f) {
            "sizeFactor must be in range 0.0-1.0, got $sizeFactor"
        }
        require(axisAlignment in -1.0f..1.0f) {
            "axisAlignment must be in range -1.0 to 1.0, got $axisAlignment"
        }
    }

    /**
     * Returns accessibility description for this transition.
     */
    fun getAccessibilityDescription(): String {
        val axisName = if (axis == Axis.Vertical) "height" else "width"
        val percentage = (sizeFactor * 100).toInt()
        val alignment = when {
            axisAlignment < -0.3f -> "start"
            axisAlignment > 0.3f -> "end"
            else -> "center"
        }
        return "Sized to $percentage% of $axisName, aligned to $alignment"
    }

    /**
     * Axis along which the size transition occurs.
     */
    enum class Axis {
        Horizontal,
        Vertical
    }

    companion object {
        /**
         * Default animation duration in milliseconds.
         */
        const val DEFAULT_ANIMATION_DURATION = 300

        /**
         * Collapsed state (0% size).
         */
        const val COLLAPSED = 0.0f

        /**
         * Expanded state (100% size).
         */
        const val EXPANDED = 1.0f
    }
}
