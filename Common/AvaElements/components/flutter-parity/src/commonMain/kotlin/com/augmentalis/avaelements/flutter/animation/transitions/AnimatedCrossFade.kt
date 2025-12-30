package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A widget that cross-fades between two given children and animates itself between their sizes.
 *
 * The animation is controlled by [crossFadeState]. When it is [CrossFadeState.ShowFirst],
 * [firstChild] is shown and [secondChild] is hidden. When it is [CrossFadeState.ShowSecond],
 * [secondChild] is shown and [firstChild] is hidden.
 *
 * This is equivalent to Flutter's [AnimatedCrossFade] widget.
 *
 * Example:
 * ```kotlin
 * AnimatedCrossFade(
 *     firstChild = Icon(icon = Icons.PlayArrow, size = Size.dp(48f)),
 *     secondChild = Icon(icon = Icons.Pause, size = Size.dp(48f)),
 *     crossFadeState = if (isPlaying) CrossFadeState.ShowSecond else CrossFadeState.ShowFirst,
 *     duration = 200
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * AnimatedCrossFade(
 *   firstChild: Icon(Icons.play_arrow, size: 48),
 *   secondChild: Icon(Icons.pause, size: 48),
 *   crossFadeState: isPlaying
 *       ? CrossFadeState.showSecond
 *       : CrossFadeState.showFirst,
 *   duration: Duration(milliseconds: 200),
 * )
 * ```
 *
 * Performance considerations:
 * - Uses GPU-accelerated cross-fade
 * - Smoothly animates between different sized children
 * - Targets 60 FPS for smooth transitions
 * - Both children are built during transition
 *
 * @property firstChild The first child widget
 * @property secondChild The second child widget
 * @property crossFadeState Which child to show
 * @property duration The duration of the cross-fade animation in milliseconds
 * @property reverseDuration The duration when reversing, if different from [duration]
 * @property firstCurve The curve to use for fading in the first child. Defaults to linear.
 * @property secondCurve The curve to use for fading in the second child. Defaults to linear.
 * @property sizeCurve The curve to use for animating between sizes. Defaults to linear.
 * @property alignment The alignment of the children during the animation
 * @property layoutBuilder Custom builder for layout during the transition
 *
 * @see AnimatedSwitcher
 * @see FadeTransition
 * @see SizeTransition
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class AnimatedCrossFade(
    val firstChild: Any,
    val secondChild: Any,
    val crossFadeState: CrossFadeState,
    val duration: Int = DEFAULT_ANIMATION_DURATION,
    val reverseDuration: Int? = null,
    val firstCurve: String = "linear",
    val secondCurve: String = "linear",
    val sizeCurve: String = "linear",
    val alignment: Alignment = Alignment.TopCenter,
    val layoutBuilder: String? = null
) {
    init {
        require(duration > 0) { "duration must be positive, got $duration" }
        reverseDuration?.let { require(it > 0) { "reverseDuration must be positive, got $it" } }
    }

    /**
     * Returns accessibility description for this transition.
     */
    fun getAccessibilityDescription(): String {
        return when (crossFadeState) {
            CrossFadeState.ShowFirst -> "Showing first child"
            CrossFadeState.ShowSecond -> "Showing second child"
        }
    }

    /**
     * Which child to show.
     */
    enum class CrossFadeState {
        ShowFirst,
        ShowSecond
    }

    /**
     * Alignment options.
     */
    enum class Alignment {
        TopLeft, TopCenter, TopRight,
        CenterLeft, Center, CenterRight,
        BottomLeft, BottomCenter, BottomRight
    }

    companion object {
        /**
         * Default animation duration in milliseconds.
         */
        const val DEFAULT_ANIMATION_DURATION = 300
    }
}
