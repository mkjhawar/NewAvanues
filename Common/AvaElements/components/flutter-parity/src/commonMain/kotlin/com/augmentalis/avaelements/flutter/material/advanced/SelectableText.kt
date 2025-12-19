package com.augmentalis.avaelements.flutter.material.advanced

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * SelectableText component - Flutter Material parity
 *
 * A text widget that allows users to select and copy text content.
 * Provides text selection with Material Design 3 selection handles and toolbar.
 *
 * **Flutter Equivalent:** `SelectableText`
 * **Material Design 3:** Part of text input components with selection
 *
 * ## Features
 * - Text selection with handles
 * - Copy to clipboard support
 * - Selection toolbar (copy, select all)
 * - Customizable selection colors
 * - Keyboard selection support
 * - Material3 theming
 * - Dark mode support
 * - TalkBack accessibility with "text field" role
 * - WCAG 2.1 Level AA compliant (4.5:1 contrast)
 *
 * ## Usage Example
 * ```kotlin
 * SelectableText(
 *     text = "This text can be selected and copied",
 *     style = TextStyle(
 *         fontSize = 16f,
 *         color = "onBackground"
 *     ),
 *     textAlign = TextAlign.Start,
 *     onSelectionChanged = { start, end ->
 *         // Handle selection change
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property text Text content to display
 * @property textStyle Text style (font, size, color, etc.)
 * @property strutStyle Strut style for vertical metrics
 * @property textAlign Horizontal alignment of text
 * @property textDirection Text direction (LTR/RTL)
 * @property textScaleFactor Factor to scale text size
 * @property showCursor Whether to show cursor when focused
 * @property autofocus Whether to autofocus this text
 * @property minLines Minimum number of lines to display
 * @property maxLines Maximum number of lines to display
 * @property cursorWidth Width of cursor
 * @property cursorHeight Height of cursor (null for default)
 * @property cursorRadius Corner radius of cursor
 * @property cursorColor Color of cursor
 * @property selectionHeightStyle Selection height style
 * @property selectionWidthStyle Selection width style
 * @property dragStartBehavior Drag start behavior for selection
 * @property enableInteractiveSelection Whether to enable text selection
 * @property selectionControls Custom selection controls
 * @property semanticsLabel Accessibility label override
 * @property scrollPhysics Scroll physics for scrollable text
 * @property textHeightBehavior Text height behavior configuration
 * @property textWidthBasis How to measure text width
 * @property contentDescription Accessibility description for TalkBack
 * @property onSelectionChanged Callback invoked when selection changes (not serialized)
 * @property onTap Callback invoked when text is tapped (not serialized)
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
@Serializable
data class SelectableText(
    override val type: String = "SelectableText",
    override val id: String? = null,
    val text: String,
    val textStyle: TextStyle? = null,
    val strutStyle: StrutStyle? = null,
    val textAlign: TextAlign = TextAlign.Start,
    val textDirection: TextDirection = TextDirection.LTR,
    val textScaleFactor: Float = 1.0f,
    val showCursor: Boolean = false,
    val autofocus: Boolean = false,
    val minLines: Int? = null,
    val maxLines: Int? = null,
    val cursorWidth: Float = 2.0f,
    val cursorHeight: Float? = null,
    val cursorRadius: Float = 2.0f,
    val cursorColor: String? = null,
    val selectionHeightStyle: String? = null,
    val selectionWidthStyle: String? = null,
    val dragStartBehavior: DragStartBehavior = DragStartBehavior.Start,
    val enableInteractiveSelection: Boolean = true,
    val selectionControls: String? = null,
    val semanticsLabel: String? = null,
    val scrollPhysics: String? = null,
    val textHeightBehavior: TextHeightBehavior? = null,
    val textWidthBasis: TextWidthBasis = TextWidthBasis.Parent,
    val contentDescription: String? = null,
    @Transient
    val onSelectionChanged: ((Int, Int) -> Unit)? = null,
    @Transient
    val onTap: (() -> Unit)? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override val style: ComponentStyle? = null

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        return contentDescription ?: semanticsLabel ?: text
    }

    /**
     * Get accessibility role
     */
    fun getAccessibilityRole(): String {
        return "selectable text"
    }

    /**
     * Check if text is multiline
     */
    fun isMultiline(): Boolean {
        return maxLines == null || maxLines > 1
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
     * Drag start behavior
     */
    enum class DragStartBehavior {
        /** Start drag from down position */
        Down,

        /** Start drag from first detected position */
        Start
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
         * Default cursor width (in dp)
         */
        const val DEFAULT_CURSOR_WIDTH = 2.0f

        /**
         * Default cursor radius (in dp)
         */
        const val DEFAULT_CURSOR_RADIUS = 2.0f

        /**
         * Create a simple selectable text
         */
        fun simple(
            text: String,
            textStyle: TextStyle? = null
        ) = SelectableText(
            text = text,
            textStyle = textStyle
        )

        /**
         * Create selectable text with custom alignment
         */
        fun withAlignment(
            text: String,
            textAlign: TextAlign,
            textStyle: TextStyle? = null
        ) = SelectableText(
            text = text,
            textAlign = textAlign,
            textStyle = textStyle
        )

        /**
         * Create multiline selectable text
         */
        fun multiline(
            text: String,
            maxLines: Int? = null,
            textStyle: TextStyle? = null
        ) = SelectableText(
            text = text,
            maxLines = maxLines,
            textStyle = textStyle
        )

        /**
         * Create selectable text with callback
         */
        fun withCallback(
            text: String,
            textStyle: TextStyle? = null,
            onSelectionChanged: ((Int, Int) -> Unit)? = null
        ) = SelectableText(
            text = text,
            textStyle = textStyle,
            onSelectionChanged = onSelectionChanged
        )

        /**
         * Create centered selectable text
         */
        fun centered(
            text: String,
            textStyle: TextStyle? = null
        ) = SelectableText(
            text = text,
            textAlign = TextAlign.Center,
            textStyle = textStyle
        )
    }
}

/**
 * Text style for SelectableText
 *
 * @property color Text color
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
 * @property backgroundColor Background color behind text
 */
@Serializable
data class TextStyle(
    val color: String? = null,
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
    val backgroundColor: String? = null
)
