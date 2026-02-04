package com.augmentalis.avaelements.flutter.animation

import kotlinx.serialization.Serializable

/**
 * A widget that animates the default text style for its descendant Text widgets implicitly.
 *
 * The AnimatedDefaultTextStyle widget animates changes to the default text style when the
 * style changes. This affects all descendant Text widgets that don't have an explicit style.
 *
 * This is equivalent to Flutter's [AnimatedDefaultTextStyle] widget.
 *
 * Example:
 * ```kotlin
 * var large by remember { mutableStateOf(false) }
 *
 * AnimatedDefaultTextStyle(
 *     style = TextStyle(
 *         fontSize = if (large) 32f else 16f,
 *         fontWeight = if (large) FontWeight.Bold else FontWeight.Normal,
 *         color = if (large) Colors.Blue else Colors.Black
 *     ),
 *     duration = Duration.milliseconds(300),
 *     curve = Curves.EaseInOut,
 *     child = Text("Animated Text Style")
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * AnimatedDefaultTextStyle(
 *   style: TextStyle(
 *     fontSize: large ? 32.0 : 16.0,
 *     fontWeight: large ? FontWeight.bold : FontWeight.normal,
 *     color: large ? Colors.blue : Colors.black,
 *   ),
 *   duration: Duration(milliseconds: 300),
 *   curve: Curves.easeInOut,
 *   child: Text('Animated Text Style'),
 * )
 * ```
 *
 * Animated Properties:
 * - Font size
 * - Font weight
 * - Font style (italic)
 * - Text color
 * - Letter spacing
 * - Word spacing
 * - Line height
 * - Text decoration
 *
 * Performance Considerations:
 * - Animates text properties smoothly at 60 FPS
 * - Uses Compose's text animation capabilities
 * - Text layout is recalculated during animation
 * - May trigger recomposition of child Text widgets
 *
 * Common Use Cases:
 * - Interactive typography
 * - Reading mode transitions
 * - Emphasis and de-emphasis
 * - Accessibility zoom
 * - Theme transitions
 *
 * @property style The target text style to animate to
 * @property duration The duration over which to animate the text style
 * @property curve The curve to apply when animating the text style
 * @property child The widget below this widget in the tree
 * @property textAlign How to align the text horizontally
 * @property softWrap Whether text should break at soft line breaks
 * @property overflow How visual overflow should be handled
 * @property maxLines Maximum number of lines for the text to span
 * @property onEnd Called when the animation completes
 *
 * @see AnimatedContainer
 * @see TextStyle
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class AnimatedDefaultTextStyle(
    val style: TextStyle,
    val duration: Duration,
    val curve: Curve = Curve.Linear,
    val child: Any,
    val textAlign: TextAlign? = null,
    val softWrap: Boolean = true,
    val overflow: TextOverflow = TextOverflow.Clip,
    val maxLines: Int? = null,
    val onEnd: (() -> Unit)? = null
) {
    init {
        require(duration.milliseconds > 0) {
            "duration must be positive, got ${duration.milliseconds}ms"
        }
        maxLines?.let {
            require(it > 0) { "maxLines must be positive, got $it" }
        }
    }
}

/**
 * Text style configuration
 */
@Serializable
data class TextStyle(
    val color: Color? = null,
    val fontSize: Float? = null,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val letterSpacing: Float? = null,
    val wordSpacing: Float? = null,
    val lineHeight: Float? = null,
    val decoration: TextDecoration? = null,
    val decorationColor: Color? = null,
    val decorationStyle: TextDecorationStyle? = null,
    val fontFamily: String? = null
) {
    companion object {
        /**
         * Default text style
         */
        val Default = TextStyle()
    }
}

/**
 * Font weight values
 */
@Serializable
enum class FontWeight {
    Thin,        // 100
    ExtraLight,  // 200
    Light,       // 300
    Normal,      // 400
    Medium,      // 500
    SemiBold,    // 600
    Bold,        // 700
    ExtraBold,   // 800
    Black;       // 900

    val value: Int
        get() = when (this) {
            Thin -> 100
            ExtraLight -> 200
            Light -> 300
            Normal -> 400
            Medium -> 500
            SemiBold -> 600
            Bold -> 700
            ExtraBold -> 800
            Black -> 900
        }
}

/**
 * Font style
 */
@Serializable
enum class FontStyle {
    Normal,
    Italic
}

/**
 * Text decoration
 */
@Serializable
enum class TextDecoration {
    None,
    Underline,
    Overline,
    LineThrough
}

/**
 * Text decoration style
 */
@Serializable
enum class TextDecorationStyle {
    Solid,
    Double,
    Dotted,
    Dashed,
    Wavy
}

/**
 * Text alignment
 */
@Serializable
enum class TextAlign {
    Left,
    Right,
    Center,
    Justify,
    Start,
    End
}

/**
 * Text overflow behavior
 */
@Serializable
enum class TextOverflow {
    Clip,
    Fade,
    Ellipsis,
    Visible
}
