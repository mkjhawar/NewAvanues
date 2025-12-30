package com.augmentalis.avaelements.components.phase1.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import com.augmentalis.avaelements.core.Font
import com.augmentalis.avaelements.core.types.Color
import kotlinx.serialization.Transient

/**
 * Text component for displaying text content
 *
 * A cross-platform text display component supporting various styles,
 * alignments, and text decorations.
 *
 * @property id Unique identifier for the component
 * @property content Text content to display
 * @property font Font configuration (family, size, weight, style)
 * @property color Text color
 * @property align Text alignment
 * @property maxLines Maximum number of lines to display (null = unlimited)
 * @property overflow Behavior when text exceeds maxLines
 * @property selectable Whether text can be selected by user
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 2.0.0
 */

data class Text(
    override val type: String = "Text",
    override val id: String? = null,
    val content: String,
    val font: Font = Font.Body,
    val color: Color = Color.Black,
    val align: TextAlign = TextAlign.Start,
    val maxLines: Int? = null,
    val overflow: TextOverflow = TextOverflow.Clip,
    val selectable: Boolean = false,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Text alignment enumeration
     */
    
    enum class TextAlign {
        /** Align to start (left in LTR, right in RTL) */
        Start,

        /** Center alignment */
        Center,

        /** Align to end (right in LTR, left in RTL) */
        End,

        /** Justify (stretch to fill width) */
        Justify
    }

    /**
     * Text overflow behavior
     */
    
    enum class TextOverflow {
        /** Clip overflowing text */
        Clip,

        /** Add ellipsis (...) to overflowing text */
        Ellipsis,

        /** Show all text (may overflow container) */
        Visible
    }

    companion object {
        /**
         * Create title text
         */
        fun title(
            content: String,
            color: Color = Color.Black
        ) = Text(
            content = content,
            font = Font.Title,
            color = color
        )

        /**
         * Create heading text
         */
        fun heading(
            content: String,
            color: Color = Color.Black
        ) = Text(
            content = content,
            font = Font.Heading,
            color = color
        )

        /**
         * Create body text
         */
        fun body(
            content: String,
            color: Color = Color.Black
        ) = Text(
            content = content,
            font = Font.Body,
            color = color
        )

        /**
         * Create caption text
         */
        fun caption(
            content: String,
            color: Color = Color.Black
        ) = Text(
            content = content,
            font = Font.Caption,
            color = color
        )
    }
}
