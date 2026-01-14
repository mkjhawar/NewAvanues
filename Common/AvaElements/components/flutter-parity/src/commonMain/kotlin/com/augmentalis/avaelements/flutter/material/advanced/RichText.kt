package com.augmentalis.avaelements.flutter.material.advanced

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Serializable

/**
 * RichText component - Flutter Material parity
 *
 * A text widget that displays text with multiple different styles using TextSpans.
 * Uses AnnotatedString under the hood for Compose implementation.
 *
 * **Flutter Equivalent:** `RichText`
 * **Material Design 3:** Part of typography components
 *
 * ## Features
 * - Multiple text styles in single text block
 * - Inline styling (bold, italic, color, size, etc.)
 * - Text alignment and overflow handling
 * - Soft/hard line breaks
 * - Material3 typography theming
 * - Dark mode support
 * - TalkBack accessibility with proper text semantics
 * - WCAG 2.1 Level AA compliant (4.5:1 contrast for body text)
 *
 * ## Usage Example
 * ```kotlin
 * RichText(
 *     spans = listOf(
 *         TextSpan(
 *             text = "Hello ",
 *             style = TextSpanStyle(fontWeight = "bold")
 *         ),
 *         TextSpan(
 *             text = "World",
 *             style = TextSpanStyle(
 *                 color = "primary",
 *                 fontSize = 18f,
 *                 fontStyle = "italic"
 *             )
 *         )
 *     ),
 *     textAlign = TextAlign.Start
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property spans List of text spans with individual styles
 * @property textAlign Horizontal alignment of text
 * @property textDirection Text direction (LTR/RTL)
 * @property softWrap Whether to break text at soft line breaks
 * @property overflow How to handle text overflow
 * @property textScaleFactor Factor to scale text size
 * @property maxLines Maximum number of lines to display
 * @property locale Locale for text rendering
 * @property strutStyle Strut style for vertical metrics
 * @property textWidthBasis How to measure text width
 * @property textHeightBehavior Text height behavior configuration
 * @property selectionColor Color for text selection
 * @property semanticsLabel Accessibility label override
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class RichText(
    override val type: String = "RichText",
    override val id: String? = null,
    val spans: List<TextSpan> = emptyList(),
    val textAlign: TextAlign = TextAlign.Start,
    val textDirection: TextDirection = TextDirection.LTR,
    val softWrap: Boolean = true,
    val overflow: TextOverflow = TextOverflow.Clip,
    val textScaleFactor: Float = 1.0f,
    val maxLines: Int? = null,
    val locale: String? = null,
    val strutStyle: StrutStyle? = null,
    val textWidthBasis: TextWidthBasis = TextWidthBasis.Parent,
    val textHeightBehavior: TextHeightBehavior? = null,
    val selectionColor: String? = null,
    val semanticsLabel: String? = null,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get combined text from all spans
     */
    fun getPlainText(): String {
        return spans.joinToString("") { it.text }
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        return contentDescription ?: semanticsLabel ?: getPlainText()
    }

    /**
     * Check if text will be truncated
     */
    fun willTruncate(): Boolean {
        return maxLines != null && overflow != TextOverflow.Clip
    }

    /**
     * Text alignment options
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
     * Text direction
     */
    enum class TextDirection {
        LTR,
        RTL
    }

    /**
     * Text overflow handling
     */
    enum class TextOverflow {
        Clip,
        Fade,
        Ellipsis,
        Visible
    }

    /**
     * Text width measurement basis
     */
    enum class TextWidthBasis {
        /** Measure based on parent constraints */
        Parent,

        /** Measure based on longest line */
        LongestLine
    }

    companion object {
        /**
         * Create a simple rich text with two spans
         */
        fun simple(
            normalText: String,
            styledText: String,
            styledStyle: TextSpanStyle
        ) = RichText(
            spans = listOf(
                TextSpan(text = normalText),
                TextSpan(text = styledText, style = styledStyle)
            )
        )

        /**
         * Create rich text with bold text
         */
        fun withBold(
            normalText: String,
            boldText: String
        ) = RichText(
            spans = listOf(
                TextSpan(text = normalText),
                TextSpan(
                    text = boldText,
                    style = TextSpanStyle(fontWeight = "bold")
                )
            )
        )

        /**
         * Create rich text with colored text
         */
        fun withColor(
            normalText: String,
            coloredText: String,
            color: String
        ) = RichText(
            spans = listOf(
                TextSpan(text = normalText),
                TextSpan(
                    text = coloredText,
                    style = TextSpanStyle(color = color)
                )
            )
        )

        /**
         * Create rich text from single span
         */
        fun fromSpan(span: TextSpan) = RichText(
            spans = listOf(span)
        )

        /**
         * Create rich text with center alignment
         */
        fun centered(spans: List<TextSpan>) = RichText(
            spans = spans,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Text span with individual styling
 *
 * @property text Text content for this span
 * @property style Style to apply to this span
 * @property children Child text spans (for nested styling)
 * @property recognizer Gesture recognizer for interactive text
 * @property mouseCursor Mouse cursor for hover (desktop/web)
 * @property onEnter Callback for mouse enter (desktop/web)
 * @property onExit Callback for mouse exit (desktop/web)
 * @property semanticsLabel Accessibility label override for this span
 * @property locale Locale for this span
 * @property spellOut Whether to spell out text for screen readers
 */
@Serializable
data class TextSpan(
    val text: String = "",
    val style: TextSpanStyle? = null,
    val children: List<TextSpan> = emptyList(),
    val recognizer: String? = null,
    val mouseCursor: String? = null,
    val onEnter: String? = null,
    val onExit: String? = null,
    val semanticsLabel: String? = null,
    val locale: String? = null,
    val spellOut: Boolean = false
)

/**
 * Style for text span
 *
 * @property color Text color
 * @property backgroundColor Background color
 * @property fontSize Font size in sp
 * @property fontWeight Font weight (normal, bold, w100-w900)
 * @property fontStyle Font style (normal, italic)
 * @property letterSpacing Letter spacing in em
 * @property wordSpacing Word spacing in em
 * @property textBaseline Text baseline (alphabetic, ideographic)
 * @property height Line height multiplier
 * @property decoration Text decoration (underline, overline, lineThrough)
 * @property decorationColor Decoration color
 * @property decorationStyle Decoration style (solid, double, dotted, dashed, wavy)
 * @property decorationThickness Decoration thickness
 * @property fontFamily Font family name
 * @property shadows List of shadow specifications
 */
@Serializable
data class TextSpanStyle(
    val color: String? = null,
    val backgroundColor: String? = null,
    val fontSize: Float? = null,
    val fontWeight: String? = null,
    val fontStyle: String? = null,
    val letterSpacing: Float? = null,
    val wordSpacing: Float? = null,
    val textBaseline: String? = null,
    val height: Float? = null,
    val decoration: String? = null,
    val decorationColor: String? = null,
    val decorationStyle: String? = null,
    val decorationThickness: Float? = null,
    val fontFamily: String? = null,
    val shadows: List<TextShadow> = emptyList()
)

/**
 * Text shadow specification
 */
@Serializable
data class TextShadow(
    val color: String,
    val offsetX: Float,
    val offsetY: Float,
    val blurRadius: Float
)

/**
 * Strut style for vertical text metrics
 */
@Serializable
data class StrutStyle(
    val fontFamily: String? = null,
    val fontSize: Float? = null,
    val height: Float? = null,
    val leading: Float? = null,
    val fontWeight: String? = null,
    val fontStyle: String? = null,
    val forceStrutHeight: Boolean = false
)

/**
 * Text height behavior configuration
 */
@Serializable
data class TextHeightBehavior(
    val applyHeightToFirstAscent: Boolean = true,
    val applyHeightToLastDescent: Boolean = true
)
