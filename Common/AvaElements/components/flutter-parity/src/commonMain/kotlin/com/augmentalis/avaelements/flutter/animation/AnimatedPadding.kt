package com.augmentalis.avaelements.flutter.animation

import com.augmentalis.avaelements.core.types.Spacing
import kotlinx.serialization.Serializable

/**
 * A widget that animates its padding implicitly.
 *
 * The AnimatedPadding widget animates the padding around its child over a given duration
 * whenever the padding changes. This creates smooth layout transitions.
 *
 * This is equivalent to Flutter's [AnimatedPadding] widget.
 *
 * Example:
 * ```kotlin
 * var expanded by remember { mutableStateOf(false) }
 *
 * AnimatedPadding(
 *     padding = if (expanded) Spacing.all(32f) else Spacing.all(8f),
 *     duration = Duration.milliseconds(300),
 *     curve = Curves.EaseInOut,
 *     child = Container(
 *         color = Colors.Blue,
 *         child = Text("Padded Content")
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * AnimatedPadding(
 *   padding: expanded ? EdgeInsets.all(32.0) : EdgeInsets.all(8.0),
 *   duration: Duration(milliseconds: 300),
 *   curve: Curves.easeInOut,
 *   child: Container(
 *     color: Colors.blue,
 *     child: Text('Padded Content'),
 *   ),
 * )
 * ```
 *
 * Example with asymmetric padding:
 * ```kotlin
 * AnimatedPadding(
 *     padding = Spacing.of(
 *         top = if (expanded) 40f else 10f,
 *         right = if (expanded) 20f else 10f,
 *         bottom = if (expanded) 40f else 10f,
 *         left = if (expanded) 20f else 10f
 *     ),
 *     duration = Duration.milliseconds(250),
 *     child = Text("Asymmetric padding")
 * )
 * ```
 *
 * Performance Considerations:
 * - Padding animations run at 60 FPS using Compose's layout animation
 * - Animates all four edges independently
 * - Triggers layout recomposition as padding changes
 * - Child widget is laid out with new padding each frame
 * - Use for responsive layout transitions
 *
 * Common Use Cases:
 * - Responsive spacing adjustments
 * - Focus state emphasis
 * - Container breathing animations
 * - Adaptive layout transitions
 * - Touch feedback padding changes
 *
 * @property padding The target padding to animate to
 * @property duration The duration over which to animate the padding
 * @property curve The curve to apply when animating the padding
 * @property child The widget below this widget in the tree
 * @property onEnd Called when the animation completes
 *
 * @see AnimatedContainer
 * @see Spacing
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class AnimatedPadding(
    val padding: Spacing,
    val duration: Duration,
    val curve: Curve = Curve.Linear,
    val child: Any,
    val onEnd: (() -> Unit)? = null
) {
    init {
        require(duration.milliseconds > 0) {
            "duration must be positive, got ${duration.milliseconds}ms"
        }
    }

    companion object {
        /**
         * Default animation duration for padding changes
         */
        const val DEFAULT_DURATION_MS = 200
    }
}
