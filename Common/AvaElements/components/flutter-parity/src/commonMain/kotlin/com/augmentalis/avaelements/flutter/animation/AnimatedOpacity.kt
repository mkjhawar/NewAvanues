package com.augmentalis.avaelements.flutter.animation

import kotlinx.serialization.Serializable

/**
 * A widget that animates its opacity implicitly.
 *
 * The AnimatedOpacity widget animates the opacity of its child over a given duration whenever
 * the given opacity changes. This is useful for fade-in and fade-out effects.
 *
 * This is equivalent to Flutter's [AnimatedOpacity] widget.
 *
 * Example (Fade in/out on tap):
 * ```kotlin
 * var visible by remember { mutableStateOf(true) }
 *
 * AnimatedOpacity(
 *     opacity = if (visible) 1.0f else 0.0f,
 *     duration = Duration.milliseconds(500),
 *     curve = Curves.EaseInOut,
 *     child = Container(
 *         width = Size.dp(200f),
 *         height = Size.dp(200f),
 *         color = Colors.Blue,
 *         child = Text("Fade Me")
 *     ),
 *     onEnd = { println("Fade animation completed") }
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * AnimatedOpacity(
 *   opacity: visible ? 1.0 : 0.0,
 *   duration: Duration(milliseconds: 500),
 *   curve: Curves.easeInOut,
 *   child: Container(
 *     width: 200.0,
 *     height: 200.0,
 *     color: Colors.blue,
 *     child: Text('Fade Me'),
 *   ),
 *   onEnd: () => print('Fade animation completed'),
 * )
 * ```
 *
 * Performance Considerations:
 * - Opacity animations are GPU-accelerated on Android
 * - Runs at 60 FPS using Compose's `animateFloatAsState`
 * - Does not trigger layout recomposition
 * - Child widget is not rebuilt during animation
 * - Opacity 0.0 still allocates space (use AnimatedVisibility for layout changes)
 *
 * Common Use Cases:
 * - Fade in/out transitions
 * - Loading state overlays
 * - Disabled state visual feedback
 * - Progressive disclosure of content
 * - Attention-drawing effects
 *
 * @property opacity The target opacity. Must be between 0.0 (transparent) and 1.0 (opaque)
 * @property duration The duration over which to animate the opacity
 * @property curve The curve to apply when animating the opacity
 * @property child The widget below this widget in the tree
 * @property onEnd Called when the animation completes
 * @property alwaysIncludeSemantics Whether to always include semantics even when opacity is 0.0
 *
 * @see AnimatedContainer
 * @see FadeTransition
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class AnimatedOpacity(
    val opacity: Float,
    val duration: Duration,
    val curve: Curve = Curve.Linear,
    val child: Any,
    val onEnd: (() -> Unit)? = null,
    val alwaysIncludeSemantics: Boolean = false
) {
    init {
        require(opacity in 0.0f..1.0f) {
            "opacity must be between 0.0 and 1.0, got $opacity"
        }
        require(duration.milliseconds > 0) {
            "duration must be positive, got ${duration.milliseconds}ms"
        }
    }

    companion object {
        /**
         * Default animation duration for opacity changes
         */
        const val DEFAULT_DURATION_MS = 200
    }
}
