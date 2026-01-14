package com.augmentalis.avaelements.flutter.layout

import kotlinx.serialization.Serializable

/**
 * A widget that aligns its child within itself and optionally sizes itself based on the child's size.
 *
 * For example, to align a box at the bottom right, you would pass this box a tight constraint that is
 * bigger than the child's natural size, with an alignment of [AlignmentGeometry.BottomEnd].
 *
 * This is equivalent to Flutter's [Align] widget.
 *
 * Example:
 * ```kotlin
 * Align(
 *     alignment = AlignmentGeometry.BottomEnd,
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
 * Align(
 *   alignment: Alignment.bottomRight,
 *   child: Container(
 *     width: 100,
 *     height: 100,
 *     color: Colors.blue,
 *   ),
 * )
 * ```
 *
 * @property alignment How to align the child within this widget's bounds.
 *                     The x and y values of the alignment range from -1.0 to 1.0.
 * @property widthFactor If non-null, sets its width to the child's width multiplied by this factor.
 *                       Must be non-negative. If null, the widget takes all available width.
 * @property heightFactor If non-null, sets its height to the child's height multiplied by this factor.
 *                        Must be non-negative. If null, the widget takes all available height.
 * @property child The widget below this widget in the tree
 *
 * @see Center
 * @see AlignmentGeometry
 * @since 2.1.0
 */
@Serializable
data class AlignComponent(
    val alignment: AlignmentGeometry = AlignmentGeometry.Center,
    val widthFactor: Float? = null,
    val heightFactor: Float? = null,
    val child: Any
) {
    init {
        widthFactor?.let { require(it >= 0) { "widthFactor must be non-negative, got $it" } }
        heightFactor?.let { require(it >= 0) { "heightFactor must be non-negative, got $it" } }
    }
}

/**
 * Base class for alignment that can be direction-aware (for RTL support)
 *
 * This corresponds to Flutter's AlignmentGeometry class.
 */
@Serializable
sealed class AlignmentGeometry {
    /**
     * The center point, both horizontally and vertically
     */
    @Serializable
    object Center : AlignmentGeometry()

    /**
     * Custom alignment with x and y coordinates
     *
     * @param x The horizontal alignment, from -1.0 (left) to 1.0 (right)
     * @param y The vertical alignment, from -1.0 (top) to 1.0 (bottom)
     */
    @Serializable
    data class Custom(val x: Float, val y: Float) : AlignmentGeometry() {
        init {
            require(x in -1.0f..1.0f) { "x must be between -1.0 and 1.0, got $x" }
            require(y in -1.0f..1.0f) { "y must be between -1.0 and 1.0, got $y" }
        }
    }

    companion object {
        // Vertical alignments
        val TopLeft = Custom(-1.0f, -1.0f)
        val TopCenter = Custom(0.0f, -1.0f)
        val TopEnd = Custom(1.0f, -1.0f)

        val CenterLeft = Custom(-1.0f, 0.0f)
        val CenterEnd = Custom(1.0f, 0.0f)

        val BottomLeft = Custom(-1.0f, 1.0f)
        val BottomCenter = Custom(0.0f, 1.0f)
        val BottomEnd = Custom(1.0f, 1.0f)

        // Aliases for common alignments (non-RTL-aware)
        val TopStart = TopLeft
        val BottomStart = BottomLeft
        val TopRight = TopEnd
        val BottomRight = BottomEnd
        val CenterRight = CenterEnd
    }
}
