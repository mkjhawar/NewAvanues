package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A widget that animates the decoration of a [DecoratedBox].
 *
 * This widget can animate the decoration properties such as color, border, shadow, and
 * gradient. It's particularly useful for hover effects, focus states, and other UI
 * state transitions.
 *
 * This is equivalent to Flutter's [DecoratedBoxTransition] widget.
 *
 * Example:
 * ```kotlin
 * DecoratedBoxTransition(
 *     decoration = BoxDecoration(
 *         color = Color(0xFFFF0000),
 *         borderRadius = 8f,
 *         boxShadow = BoxShadow(
 *             blurRadius = 10f,
 *             color = Color(0x40000000)
 *         )
 *     ),
 *     position = DecorationPosition.Background,
 *     child = Container(
 *         width = Size.dp(100f),
 *         height = Size.dp(100f)
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * DecoratedBoxTransition(
 *   decoration: animation.drive(
 *     DecorationTween(
 *       begin: BoxDecoration(color: Colors.red),
 *       end: BoxDecoration(color: Colors.blue),
 *     ),
 *   ),
 *   position: DecorationPosition.background,
 *   child: Container(width: 100, height: 100),
 * )
 * ```
 *
 * Performance considerations:
 * - GPU-accelerated when possible
 * - Shadow animations can be expensive
 * - Targets 60 FPS for smooth transitions
 * - Consider using simpler transitions for complex decorations
 *
 * @property decoration The box decoration to apply
 * @property position Whether to paint the decoration behind or in front of the child
 * @property child The widget below this widget in the tree
 *
 * @see AnimatedContainer
 * @see FadeTransition
 * @see Container
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class DecoratedBoxTransition(
    val decoration: BoxDecoration,
    val child: Any,
    val position: DecorationPosition = DecorationPosition.Background
) {
    /**
     * Returns accessibility description for this transition.
     */
    fun getAccessibilityDescription(): String {
        return "Decorated box with ${decoration.getDescription()}"
    }

    /**
     * Box decoration properties.
     */
    @Serializable
    data class BoxDecoration(
        val color: Long? = null,
        val borderRadius: Float? = null,
        val boxShadow: BoxShadow? = null,
        val gradient: String? = null,
        val border: String? = null
    ) {
        fun getDescription(): String = buildString {
            color?.let { append("color, ") }
            borderRadius?.let { append("rounded corners, ") }
            boxShadow?.let { append("shadow, ") }
            gradient?.let { append("gradient, ") }
            border?.let { append("border") }
        }.trimEnd(',', ' ').ifEmpty { "no decoration" }
    }

    /**
     * Box shadow properties.
     */
    @Serializable
    data class BoxShadow(
        val color: Long = 0x40000000L,
        val blurRadius: Float = 0f,
        val spreadRadius: Float = 0f,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f
    )

    /**
     * Where to paint the decoration relative to the child.
     */
    enum class DecorationPosition {
        Background,
        Foreground
    }

    companion object {
        /**
         * Default animation duration in milliseconds.
         */
        const val DEFAULT_ANIMATION_DURATION = 300
    }
}
