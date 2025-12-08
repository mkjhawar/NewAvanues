package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A widget that marks its child as being a candidate for hero animations.
 *
 * When a [PageRoute] is pushed or popped with the Navigator, the entire screen's content
 * is replaced. An old route disappears and a new route appears. If there's a common
 * element between the old and new routes, and it's wrapped in a [Hero] widget with
 * matching [tag]s, a hero animation will occur.
 *
 * This is equivalent to Flutter's [Hero] widget and is CRITICAL for smooth
 * cross-screen transitions in the Avanues ecosystem.
 *
 * Example:
 * ```kotlin
 * // On first screen
 * Hero(
 *     tag = "profile-image",
 *     child = Image(
 *         src = "user_avatar.png",
 *         width = Size.dp(50f),
 *         height = Size.dp(50f)
 *     )
 * )
 *
 * // On second screen
 * Hero(
 *     tag = "profile-image",
 *     child = Image(
 *         src = "user_avatar.png",
 *         width = Size.dp(200f),
 *         height = Size.dp(200f)
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * // On first screen
 * Hero(
 *   tag: 'profile-image',
 *   child: Image.network(
 *     'user_avatar.png',
 *     width: 50,
 *     height: 50,
 *   ),
 * )
 *
 * // On second screen
 * Hero(
 *   tag: 'profile-image',
 *   child: Image.network(
 *     'user_avatar.png',
 *     width: 200,
 *     height: 200,
 *   ),
 * )
 * ```
 *
 * Performance considerations:
 * - Uses GPU-accelerated shared element transitions
 * - Maintains 60 FPS during transitions
 * - Automatically handles different sizes, positions, and shapes
 * - Efficiently reuses widget instances where possible
 *
 * Hero animation behavior:
 * - The hero flies from its position in the old route to its position in the new route
 * - The hero's bounds are animated using a Tween<Rect>
 * - The hero's child widget is composited during the animation
 * - The animation curve can be customized with [flightShuttleBuilder]
 *
 * @property tag The identifier for this particular hero. This tag must be unique among
 *               all the heroes in the route from which the hero is flying, and among all
 *               the heroes in the route to which the hero is flying.
 * @property child The widget below this widget in the tree
 * @property createRectTween Defines how the destination hero's bounds change as it flies
 *                            from the starting route to the destination route
 * @property flightShuttleBuilder Optional builder for a custom widget to show during the
 *                                 hero flight animation
 * @property placeholderBuilder Optional builder for the widget to show in the original location
 *                               while the hero is in flight
 * @property transitionOnUserGestures Whether to enable hero transitions on user gesture-driven
 *                                     navigation (e.g., swipe to go back). Defaults to false.
 *
 * @see PageRoute
 * @see Navigator
 * @see AnimatedSwitcher
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class Hero(
    val tag: String,
    val child: Any,
    val createRectTween: String? = null,
    val flightShuttleBuilder: String? = null,
    val placeholderBuilder: String? = null,
    val transitionOnUserGestures: Boolean = false
) {
    init {
        require(tag.isNotBlank()) { "Hero tag must not be blank" }
    }

    /**
     * Returns accessibility description for this hero.
     */
    fun getAccessibilityDescription(): String {
        return "Shared element with tag: $tag"
    }

    /**
     * Default rect tween types for hero animations.
     */
    enum class RectTweenType {
        /**
         * Linear interpolation between rectangles.
         */
        Linear,

        /**
         * Material-style rect tween with emphasis on position changes.
         */
        Material
    }

    companion object {
        /**
         * Default animation duration in milliseconds for hero transitions.
         */
        const val DEFAULT_ANIMATION_DURATION = 300

        /**
         * Recommended minimum duration for hero animations to feel smooth.
         */
        const val MIN_RECOMMENDED_DURATION = 200

        /**
         * Recommended maximum duration to avoid feeling sluggish.
         */
        const val MAX_RECOMMENDED_DURATION = 500

        /**
         * Default curve for hero animations (Material ease in out curve).
         */
        const val DEFAULT_CURVE = "easeInOut"
    }
}
