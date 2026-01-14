package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A widget that by default does a [FadeTransition] between a new widget and the widget
 * previously set on the [AnimatedSwitcher] as a child.
 *
 * If the "new" child is the same widget type and key as the "old" child, but with different
 * parameters, then [AnimatedSwitcher] will not do a transition between them. To force a
 * transition even if the widgets have the same key, provide a different [key] value.
 *
 * This is equivalent to Flutter's [AnimatedSwitcher] widget.
 *
 * Example:
 * ```kotlin
 * AnimatedSwitcher(
 *     duration = 300,
 *     child = if (showProfile) {
 *         ProfileView(key = "profile")
 *     } else {
 *         SettingsView(key = "settings")
 *     }
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * AnimatedSwitcher(
 *   duration: Duration(milliseconds: 300),
 *   child: showProfile
 *       ? ProfileView(key: ValueKey('profile'))
 *       : SettingsView(key: ValueKey('settings')),
 * )
 * ```
 *
 * Performance considerations:
 * - Default fade transition is GPU-accelerated
 * - Custom transitions can be more expensive
 * - Targets 60 FPS for smooth transitions
 * - Both old and new children are built during transition
 *
 * @property child The current child widget
 * @property duration The duration of the transition in milliseconds
 * @property reverseDuration The duration when the child is changing back, if different from [duration]
 * @property switchInCurve The animation curve to use when transitioning in a new child
 * @property switchOutCurve The animation curve to use when transitioning out the old child
 * @property transitionBuilder A builder that creates the transition animation
 * @property layoutBuilder Custom builder for layout during the transition
 *
 * @see AnimatedCrossFade
 * @see FadeTransition
 * @see Hero
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class AnimatedSwitcher(
    val child: Any?,
    val duration: Int = DEFAULT_ANIMATION_DURATION,
    val reverseDuration: Int? = null,
    val switchInCurve: String = "linear",
    val switchOutCurve: String = "linear",
    val transitionBuilder: String? = null,
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
        return if (child != null) {
            "Switching to new content"
        } else {
            "No content"
        }
    }

    /**
     * Default transition builders.
     */
    enum class DefaultTransition {
        /**
         * Fade transition (default).
         */
        Fade,

        /**
         * Scale transition.
         */
        Scale,

        /**
         * Rotation transition.
         */
        Rotation
    }

    companion object {
        /**
         * Default animation duration in milliseconds.
         */
        const val DEFAULT_ANIMATION_DURATION = 300
    }
}
