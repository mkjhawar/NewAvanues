package com.augmentalis.avaelements.flutter.animation

import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Size
import kotlinx.serialization.Serializable

/**
 * A container that animates changes to its properties over a given duration.
 *
 * The AnimatedContainer automatically animates between the old and new values of properties
 * when they change. Properties that can be animated include:
 * - [alignment] - The alignment of the child within the container
 * - [padding] - The padding inside the container
 * - [color] - The background color
 * - [decoration] - Border, shadow, and gradient effects
 * - [width] and [height] - The container dimensions
 * - [margin] - The outer spacing around the container
 *
 * This is equivalent to Flutter's [AnimatedContainer] widget.
 *
 * Example:
 * ```kotlin
 * var selected by remember { mutableStateOf(false) }
 *
 * AnimatedContainer(
 *     duration = Duration.milliseconds(300),
 *     width = if (selected) Size.dp(200f) else Size.dp(100f),
 *     height = if (selected) Size.dp(200f) else Size.dp(100f),
 *     color = if (selected) Colors.Blue else Colors.Red,
 *     curve = Curves.EaseInOut,
 *     child = Text("Tap Me"),
 *     onEnd = { println("Animation completed") }
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * AnimatedContainer(
 *   duration: Duration(milliseconds: 300),
 *   width: selected ? 200.0 : 100.0,
 *   height: selected ? 200.0 : 100.0,
 *   color: selected ? Colors.blue : Colors.red,
 *   curve: Curves.easeInOut,
 *   child: Text('Tap Me'),
 *   onEnd: () => print('Animation completed'),
 * )
 * ```
 *
 * Performance Considerations:
 * - Animations run at 60 FPS on Android using Jetpack Compose's animation framework
 * - Multiple property animations are synchronized and run in parallel
 * - Layout changes trigger recomposition only for affected components
 * - Uses hardware acceleration for transform animations
 *
 * @property duration The duration over which to animate the parameters of this container
 * @property curve The curve to apply when animating the parameters of this container
 * @property alignment Align the child within the container
 * @property padding Empty space to inscribe inside the decoration
 * @property color The color to paint behind the child
 * @property decoration The decoration to paint behind the child
 * @property width The width of the container
 * @property height The height of the container
 * @property margin Empty space to surround the decoration and child
 * @property transform A transformation to apply before painting the container
 * @property child The child widget contained by the container
 * @property onEnd Called every time an animation completes
 *
 * @see AnimatedOpacity
 * @see AnimatedPadding
 * @see AnimatedAlign
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class AnimatedContainer(
    val duration: Duration,
    val curve: Curve = Curve.Linear,
    val alignment: AlignmentGeometry? = null,
    val padding: Spacing? = null,
    val color: Color? = null,
    val decoration: BoxDecoration? = null,
    val width: Size? = null,
    val height: Size? = null,
    val margin: Spacing? = null,
    val transform: Matrix4? = null,
    val child: Any? = null,
    val onEnd: (() -> Unit)? = null
) {
    init {
        require(duration.milliseconds > 0) {
            "duration must be positive, got ${duration.milliseconds}ms"
        }
        if (color != null && decoration != null) {
            require(decoration.color == null) {
                "Cannot provide both a color and a decoration with a color. " +
                "To provide both, use 'decoration: BoxDecoration(color: color)'"
            }
        }
    }
}

/**
 * Duration for animations
 *
 * @property milliseconds Duration in milliseconds
 */
@Serializable
data class Duration(val milliseconds: Int) {
    companion object {
        fun milliseconds(ms: Int) = Duration(ms)
        fun seconds(s: Int) = Duration(s * 1000)

        val zero = Duration(0)
    }
}

/**
 * Animation curves for easing
 *
 * Maps to Compose's Easing functions
 */
@Serializable
sealed class Curve {
    @Serializable
    object Linear : Curve()

    @Serializable
    object EaseIn : Curve()

    @Serializable
    object EaseOut : Curve()

    @Serializable
    object EaseInOut : Curve()

    @Serializable
    object FastOutSlowIn : Curve()

    @Serializable
    object BounceIn : Curve()

    @Serializable
    object BounceOut : Curve()

    @Serializable
    object ElasticIn : Curve()

    @Serializable
    object ElasticOut : Curve()

    @Serializable
    data class Cubic(val a: Float, val b: Float, val c: Float, val d: Float) : Curve()
}

/**
 * Alignment geometry for widget alignment
 */
@Serializable
sealed class AlignmentGeometry {
    @Serializable
    object TopLeft : AlignmentGeometry()

    @Serializable
    object TopCenter : AlignmentGeometry()

    @Serializable
    object TopRight : AlignmentGeometry()

    @Serializable
    object CenterLeft : AlignmentGeometry()

    @Serializable
    object Center : AlignmentGeometry()

    @Serializable
    object CenterRight : AlignmentGeometry()

    @Serializable
    object BottomLeft : AlignmentGeometry()

    @Serializable
    object BottomCenter : AlignmentGeometry()

    @Serializable
    object BottomRight : AlignmentGeometry()

    @Serializable
    data class Custom(val x: Float, val y: Float) : AlignmentGeometry() {
        init {
            require(x in -1.0f..1.0f) { "x must be between -1.0 and 1.0, got $x" }
            require(y in -1.0f..1.0f) { "y must be between -1.0 and 1.0, got $y" }
        }
    }
}

/**
 * Color representation
 */
@Serializable
data class Color(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = 255
) {
    init {
        require(red in 0..255) { "red must be 0-255, got $red" }
        require(green in 0..255) { "green must be 0-255, got $green" }
        require(blue in 0..255) { "blue must be 0-255, got $blue" }
        require(alpha in 0..255) { "alpha must be 0-255, got $alpha" }
    }

    companion object {
        val Transparent = Color(0, 0, 0, 0)
        val Black = Color(0, 0, 0)
        val White = Color(255, 255, 255)
        val Red = Color(255, 0, 0)
        val Green = Color(0, 255, 0)
        val Blue = Color(0, 0, 255)

        fun argb(alpha: Int, red: Int, green: Int, blue: Int) = Color(red, green, blue, alpha)
        fun rgb(red: Int, green: Int, blue: Int) = Color(red, green, blue)
    }
}

/**
 * Box decoration for borders, shadows, and gradients
 */
@Serializable
data class BoxDecoration(
    val color: Color? = null,
    val border: Border? = null,
    val borderRadius: BorderRadius? = null,
    val boxShadow: List<BoxShadow>? = null,
    val gradient: Gradient? = null,
    val shape: BoxShape = BoxShape.Rectangle
)

/**
 * Border configuration
 */
@Serializable
data class Border(
    val top: BorderSide? = null,
    val right: BorderSide? = null,
    val bottom: BorderSide? = null,
    val left: BorderSide? = null
) {
    companion object {
        fun all(side: BorderSide) = Border(side, side, side, side)
    }
}

/**
 * Border side configuration
 */
@Serializable
data class BorderSide(
    val color: Color,
    val width: Float = 1.0f,
    val style: BorderStyle = BorderStyle.Solid
)

/**
 * Border style
 */
@Serializable
enum class BorderStyle {
    Solid,
    None
}

/**
 * Border radius
 */
@Serializable
data class BorderRadius(
    val topLeft: Float = 0f,
    val topRight: Float = 0f,
    val bottomRight: Float = 0f,
    val bottomLeft: Float = 0f
) {
    companion object {
        fun circular(radius: Float) = BorderRadius(radius, radius, radius, radius)
        fun all(radius: Float) = circular(radius)
    }
}

/**
 * Box shadow for elevation effects
 */
@Serializable
data class BoxShadow(
    val color: Color,
    val offset: Offset = Offset(0f, 0f),
    val blurRadius: Float = 0f,
    val spreadRadius: Float = 0f
)

/**
 * 2D offset
 */
@Serializable
data class Offset(val x: Float, val y: Float) {
    companion object {
        val zero = Offset(0f, 0f)
    }
}

/**
 * Gradient types
 */
@Serializable
sealed class Gradient {
    @Serializable
    data class Linear(
        val colors: List<Color>,
        val begin: AlignmentGeometry = AlignmentGeometry.TopCenter,
        val end: AlignmentGeometry = AlignmentGeometry.BottomCenter,
        val stops: List<Float>? = null
    ) : Gradient()

    @Serializable
    data class Radial(
        val colors: List<Color>,
        val center: AlignmentGeometry = AlignmentGeometry.Center,
        val radius: Float = 0.5f,
        val stops: List<Float>? = null
    ) : Gradient()
}

/**
 * Box shape
 */
@Serializable
enum class BoxShape {
    Rectangle,
    Circle
}

/**
 * 4x4 transformation matrix
 */
@Serializable
data class Matrix4(
    val values: List<Float>
) {
    init {
        require(values.size == 16) { "Matrix4 requires 16 values, got ${values.size}" }
    }

    companion object {
        fun identity() = Matrix4(
            listOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
            )
        )

        fun rotationZ(radians: Float): Matrix4 {
            val cos = kotlin.math.cos(radians)
            val sin = kotlin.math.sin(radians)
            return Matrix4(
                listOf(
                    cos.toFloat(), sin.toFloat(), 0f, 0f,
                    -sin.toFloat(), cos.toFloat(), 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f
                )
            )
        }
    }
}
