package com.augmentalis.avaelements.flutter.animation

import kotlinx.serialization.Serializable

/**
 * A widget that animates its scale transformation implicitly.
 *
 * The AnimatedScale widget animates the scale of its child over a given duration whenever
 * the scale factor changes. This creates smooth zoom in/out effects.
 *
 * This is equivalent to Flutter's [AnimatedScale] widget (introduced in Flutter 3.0).
 *
 * Example:
 * ```kotlin
 * var zoomed by remember { mutableStateOf(false) }
 *
 * AnimatedScale(
 *     scale = if (zoomed) 1.5f else 1.0f,
 *     duration = Duration.milliseconds(300),
 *     curve = Curves.EaseInOut,
 *     alignment = AlignmentGeometry.Center,
 *     child = Container(
 *         width = Size.dp(100f),
 *         height = Size.dp(100f),
 *         color = Colors.Blue,
 *         child = Text("Zoom Me")
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * AnimatedScale(
 *   scale: zoomed ? 1.5 : 1.0,
 *   duration: Duration(milliseconds: 300),
 *   curve: Curves.easeInOut,
 *   alignment: Alignment.center,
 *   child: Container(
 *     width: 100.0,
 *     height: 100.0,
 *     color: Colors.blue,
 *     child: Text('Zoom Me'),
 *   ),
 * )
 * ```
 *
 * Example with filter quality:
 * ```kotlin
 * AnimatedScale(
 *     scale = 2.0f,
 *     duration = Duration.milliseconds(500),
 *     alignment = AlignmentGeometry.TopLeft,
 *     filterQuality = FilterQuality.Medium,
 *     child = Image("avatar.png")
 * )
 * ```
 *
 * Scale Behavior:
 * - scale = 1.0: Original size (100%)
 * - scale = 0.5: Half size (50%)
 * - scale = 2.0: Double size (200%)
 * - scale = 0.0: Invisible (collapsed to point)
 *
 * Performance Considerations:
 * - Scale animations are GPU-accelerated using Compose's graphicsLayer
 * - Runs at 60 FPS with smooth interpolation
 * - Does not trigger layout recomposition
 * - Child widget is not rebuilt during animation
 * - Transform happens in graphics layer (very efficient)
 *
 * Common Use Cases:
 * - Button press feedback
 * - Attention-grabbing effects
 * - Zoom transitions
 * - Loading pulse animations
 * - Focus emphasis
 * - Image zoom interactions
 *
 * @property scale The target scale factor to animate to
 * @property duration The duration over which to animate the scale
 * @property curve The curve to apply when animating the scale
 * @property alignment The origin point for the scale transformation
 * @property child The widget below this widget in the tree
 * @property filterQuality The quality of image filtering during scaling
 * @property onEnd Called when the animation completes
 *
 * @see AnimatedContainer
 * @see AnimatedOpacity
 * @see AnimatedRotation
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class AnimatedScale(
    val scale: Float,
    val duration: Duration,
    val curve: Curve = Curve.Linear,
    val alignment: AlignmentGeometry = AlignmentGeometry.Center,
    val child: Any,
    val filterQuality: FilterQuality = FilterQuality.Low,
    val onEnd: (() -> Unit)? = null
) {
    init {
        require(duration.milliseconds > 0) {
            "duration must be positive, got ${duration.milliseconds}ms"
        }
        require(scale >= 0.0f) {
            "scale must be non-negative, got $scale"
        }
    }

    companion object {
        /**
         * Default animation duration for scale changes
         */
        const val DEFAULT_DURATION_MS = 200
    }
}

/**
 * Filter quality for image scaling
 *
 * Determines the quality of image filtering when scaling images.
 * Higher quality uses more CPU/GPU resources.
 */
@Serializable
enum class FilterQuality {
    /**
     * No filtering - fastest, lowest quality (nearest neighbor)
     */
    None,

    /**
     * Low quality filtering (bilinear)
     */
    Low,

    /**
     * Medium quality filtering (bilinear with mipmap)
     */
    Medium,

    /**
     * High quality filtering (bicubic)
     */
    High
}
