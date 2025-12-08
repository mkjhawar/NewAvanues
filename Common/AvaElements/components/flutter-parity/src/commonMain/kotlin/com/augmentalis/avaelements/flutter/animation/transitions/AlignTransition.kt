package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A widget that animates the alignment of a child within itself.
 *
 * The alignment is animated using an [AlignmentGeometry] animation. This is useful for
 * creating smooth transitions when changing the position of a child within its parent.
 *
 * This is equivalent to Flutter's [AlignTransition] widget.
 *
 * Example:
 * ```kotlin
 * AlignTransition(
 *     alignment = Alignment.BottomRight,
 *     child = Container(
 *         width = Size.dp(100f),
 *         height = Size.dp(100f),
 *         color = Colors.Blue
 *     )
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * AlignTransition(
 *   alignment: animation.drive(
 *     AlignmentTween(
 *       begin: Alignment.topLeft,
 *       end: Alignment.bottomRight,
 *     ),
 *   ),
 *   child: Container(
 *     width: 100,
 *     height: 100,
 *     color: Colors.blue,
 *   ),
 * )
 * ```
 *
 * Performance considerations:
 * - GPU-accelerated positioning
 * - Does not trigger layout
 * - Targets 60 FPS for smooth transitions
 * - Very efficient even for complex children
 *
 * @property alignment The alignment of the child within the parent
 * @property child The widget below this widget in the tree
 * @property widthFactor If non-null, the width of the widget is this value times the child's width
 * @property heightFactor If non-null, the height of the widget is this value times the child's height
 *
 * @see PositionedTransition
 * @see SlideTransition
 * @see Align
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class AlignTransition(
    val alignment: Alignment,
    val child: Any,
    val widthFactor: Float? = null,
    val heightFactor: Float? = null
) {
    init {
        widthFactor?.let { require(it >= 0.0f) { "widthFactor must be non-negative, got $it" } }
        heightFactor?.let { require(it >= 0.0f) { "heightFactor must be non-negative, got $it" } }
    }

    /**
     * Returns accessibility description for this transition.
     */
    fun getAccessibilityDescription(): String {
        return "Aligned to ${alignment.name.replace("([A-Z])".toRegex(), " $1").trim()}"
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
