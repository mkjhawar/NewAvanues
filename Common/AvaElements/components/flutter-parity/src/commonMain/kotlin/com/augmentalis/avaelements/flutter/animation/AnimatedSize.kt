package com.augmentalis.avaelements.flutter.animation

import kotlinx.serialization.Serializable

/**
 * A widget that automatically animates its size to match the size of its child.
 *
 * The AnimatedSize widget animates its size whenever its child's size changes. Unlike other
 * animated widgets, you don't specify target size values - instead, the widget measures its
 * child and animates to match that size.
 *
 * This is equivalent to Flutter's [AnimatedSize] widget.
 *
 * Example:
 * ```kotlin
 * var showDetails by remember { mutableStateOf(false) }
 *
 * AnimatedSize(
 *     duration = Duration.milliseconds(300),
 *     curve = Curves.FastOutSlowIn,
 *     alignment = AlignmentGeometry.TopCenter,
 *     child = Column {
 *         Text("Title")
 *         if (showDetails) {
 *             Text("These are the details that cause the size to change")
 *             Text("More details here")
 *         }
 *     }
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * AnimatedSize(
 *   duration: Duration(milliseconds: 300),
 *   curve: Curves.fastOutSlowIn,
 *   alignment: Alignment.topCenter,
 *   child: Column(
 *     children: [
 *       Text('Title'),
 *       if (showDetails) ...[
 *         Text('These are the details that cause the size to change'),
 *         Text('More details here'),
 *       ],
 *     ],
 *   ),
 * )
 * ```
 *
 * Key Differences from AnimatedContainer:
 * - AnimatedSize: Size determined by child, animates to match child size changes
 * - AnimatedContainer: Size explicitly specified, child fits within container
 *
 * Performance Considerations:
 * - Uses Compose's `animateContentSize()` modifier
 * - Runs at 60 FPS with smooth interpolation
 * - Triggers layout recomposition as size changes
 * - Child is measured and laid out at each animation frame
 * - Efficient for dynamic content size changes
 *
 * Common Use Cases:
 * - Expandable content sections
 * - Dynamic form fields
 * - Collapsible panels
 * - Progressive disclosure
 * - Auto-sizing containers
 * - Chat bubble animations
 *
 * @property duration The duration over which to animate the size changes
 * @property curve The curve to apply when animating the size
 * @property alignment How to align the child within the available space during animation
 * @property child The widget whose size changes will be animated
 * @property clipBehavior The content clipping behavior during animation
 * @property onEnd Called when the animation completes
 *
 * @see AnimatedContainer
 * @see AnimatedPadding
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class AnimatedSize(
    val duration: Duration,
    val curve: Curve = Curve.Linear,
    val alignment: AlignmentGeometry = AlignmentGeometry.TopCenter,
    val child: Any,
    val clipBehavior: Clip = Clip.HardEdge,
    val onEnd: (() -> Unit)? = null
) {
    init {
        require(duration.milliseconds > 0) {
            "duration must be positive, got ${duration.milliseconds}ms"
        }
    }

    companion object {
        /**
         * Default animation duration for size changes
         */
        const val DEFAULT_DURATION_MS = 200
    }
}

/**
 * Clipping behavior
 */
@Serializable
enum class Clip {
    /**
     * No clipping - content can overflow
     */
    None,

    /**
     * Clip with hard edges (no anti-aliasing)
     */
    HardEdge,

    /**
     * Clip with anti-aliasing
     */
    AntiAlias,

    /**
     * Clip with anti-aliasing and save layer (most expensive)
     */
    AntiAliasWithSaveLayer
}
