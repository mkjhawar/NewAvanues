package com.augmentalis.avaelements.flutter.animation.transitions

import kotlinx.serialization.Serializable

/**
 * A widget that animates the default text style of its child.
 *
 * Animates the [TextStyle] that is used by [Text] widgets that don't have an explicit
 * style. This is useful for creating text animations such as color changes, size changes,
 * or weight changes.
 *
 * This is equivalent to Flutter's [DefaultTextStyleTransition] widget.
 *
 * Example:
 * ```kotlin
 * DefaultTextStyleTransition(
 *     style = TextStyle(
 *         fontSize = 24f,
 *         color = Color(0xFFFF0000),
 *         fontWeight = FontWeight.Bold
 *     ),
 *     child = Text("Animated Text")
 * )
 * ```
 *
 * Flutter equivalent:
 * ```dart
 * DefaultTextStyleTransition(
 *   style: animation.drive(
 *     TextStyleTween(
 *       begin: TextStyle(fontSize: 16, color: Colors.black),
 *       end: TextStyle(fontSize: 24, color: Colors.red, fontWeight: FontWeight.bold),
 *     ),
 *   ),
 *   child: Text('Animated Text'),
 * )
 * ```
 *
 * Performance considerations:
 * - GPU-accelerated when animating simple properties
 * - Font size changes trigger layout
 * - Targets 60 FPS for smooth transitions
 * - Color and weight changes are relatively cheap
 *
 * @property style The text style to animate to
 * @property child The widget below this widget in the tree
 * @property textAlign How the text should be aligned horizontally
 * @property softWrap Whether the text should break at soft line breaks
 * @property overflow How visual overflow should be handled
 * @property maxLines The maximum number of lines for the text to span
 *
 * @see AnimatedDefaultTextStyle
 * @see Text
 * @see TextStyle
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class DefaultTextStyleTransition(
    val style: TextStyle,
    val child: Any,
    val textAlign: TextAlign = TextAlign.Start,
    val softWrap: Boolean = true,
    val overflow: TextOverflow = TextOverflow.Clip,
    val maxLines: Int? = null
) {
    init {
        maxLines?.let { require(it > 0) { "maxLines must be positive, got $it" } }
    }

    /**
     * Returns accessibility description for this transition.
     */
    fun getAccessibilityDescription(): String {
        return buildString {
            append("Text styled with ")
            append("${style.fontSize}sp, ")
            append("${style.fontWeight}, ")
            append("color #${style.color.toString(16)}")
        }
    }

    /**
     * Text style properties.
     */
    @Serializable
    data class TextStyle(
        val fontSize: Float = 14f,
        val color: Long = 0xFF000000L,
        val fontWeight: FontWeight = FontWeight.Normal,
        val fontStyle: FontStyle = FontStyle.Normal,
        val letterSpacing: Float? = null,
        val wordSpacing: Float? = null,
        val height: Float? = null,
        val decoration: TextDecoration = TextDecoration.None
    )

    /**
     * Font weight options.
     */
    enum class FontWeight {
        Thin,       // 100
        ExtraLight, // 200
        Light,      // 300
        Normal,     // 400
        Medium,     // 500
        SemiBold,   // 600
        Bold,       // 700
        ExtraBold,  // 800
        Black       // 900
    }

    /**
     * Font style options.
     */
    enum class FontStyle {
        Normal,
        Italic
    }

    /**
     * Text decoration options.
     */
    enum class TextDecoration {
        None,
        Underline,
        Overline,
        LineThrough
    }

    /**
     * Text alignment options.
     */
    enum class TextAlign {
        Start,
        End,
        Left,
        Right,
        Center,
        Justify
    }

    /**
     * Text overflow options.
     */
    enum class TextOverflow {
        Clip,
        Fade,
        Ellipsis,
        Visible
    }

    companion object {
        /**
         * Default animation duration in milliseconds.
         */
        const val DEFAULT_ANIMATION_DURATION = 300
    }
}
