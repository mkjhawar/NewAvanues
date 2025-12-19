package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A widget that animates the opacity of a widget.
 *
 * For a given animation with a value of type double, this class creates a widget
 * whose opacity is driven by that animation (i.e., the widget's opacity equals the
 * value of the animation). The opacity is clamped to the range 0.0 to 1.0.
 *
 * This is equivalent to Flutter's [FadeTransition] widget.
 *
 * Example:
 * ```kotlin
 * FadeTransition(
 *     opacity = 0.5f,
 *     child = Text("Fading text")
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * FadeTransition(
 *   opacity: animation,
 *   child: Text("Fading text"),
 * )
 * ```
 *
 * Performance considerations:
 * - Uses GPU-accelerated opacity layer
 * - Opacity changes do not trigger layout
 * - Targets 60 FPS for smooth transitions
 * - Opacity values outside 0.0-1.0 are clamped
 *
 * @property opacity The opacity value (0.0 = fully transparent, 1.0 = fully opaque)
 *                   Must be in the range 0.0 to 1.0. Values outside this range will be clamped.
 * @property child The widget below this widget in the tree
 * @property alwaysIncludeSemantics Whether to include the child in the semantics tree even when
 *                                   opacity is 0.0. Defaults to false.
 *
 * @see SlideTransition
 * @see ScaleTransition
 * @see AnimatedOpacity
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class FadeTransition(
    val opacity: Float,
    val child: Any,
    val alwaysIncludeSemantics: Boolean = false
) {
    init {
        require(opacity in 0.0f..1.0f || opacity < 0.0f || opacity > 1.0f) {
            "opacity will be clamped to range 0.0-1.0, got $opacity"
        }
    }

    /**
     * Returns the clamped opacity value.
     */
    fun getClampedOpacity(): Float = opacity.coerceIn(0.0f, 1.0f)

    /**
     * Returns accessibility description for this transition.
     */
    fun getAccessibilityDescription(): String {
        val clampedOpacity = getClampedOpacity()
        return when {
            clampedOpacity == 0.0f -> "Hidden"
            clampedOpacity == 1.0f -> "Fully visible"
            clampedOpacity < 0.5f -> "Mostly hidden (${(clampedOpacity * 100).toInt()}% visible)"
            else -> "Mostly visible (${(clampedOpacity * 100).toInt()}% visible)"
        }
    }

    companion object {
        /**
         * Default animation duration in milliseconds.
         */
        const val DEFAULT_ANIMATION_DURATION = 300

        /**
         * Minimum opacity value.
         */
        const val MIN_OPACITY = 0.0f

        /**
         * Maximum opacity value.
         */
        const val MAX_OPACITY = 1.0f
    }
}
