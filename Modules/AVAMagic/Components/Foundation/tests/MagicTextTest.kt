package net.ideahq.ideamagic.components.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MagicText Component Tests
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class MagicTextTest {

    @Test
    fun `text creation with default values`() {
        val text = MagicText(
            content = "Hello World"
        )

        assertEquals("Hello World", text.content)
        assertEquals(TextVariant.BODY1, text.variant)
        assertEquals(null, text.color)
        assertEquals(TextAlign.START, text.align)
        assertFalse(text.bold)
        assertFalse(text.italic)
        assertFalse(text.underline)
        assertEquals(null, text.maxLines)
    }

    @Test
    fun `text with all properties`() {
        val text = MagicText(
            content = "Title",
            variant = TextVariant.H1,
            color = "#000000",
            align = TextAlign.CENTER,
            bold = true,
            italic = true,
            underline = true,
            maxLines = 2
        )

        assertEquals("Title", text.content)
        assertEquals(TextVariant.H1, text.variant)
        assertEquals("#000000", text.color)
        assertEquals(TextAlign.CENTER, text.align)
        assertTrue(text.bold)
        assertTrue(text.italic)
        assertTrue(text.underline)
        assertEquals(2, text.maxLines)
    }

    @Test
    fun `text variants`() {
        val h1 = MagicText("H1", variant = TextVariant.H1)
        val h2 = MagicText("H2", variant = TextVariant.H2)
        val h3 = MagicText("H3", variant = TextVariant.H3)
        val body1 = MagicText("Body1", variant = TextVariant.BODY1)
        val body2 = MagicText("Body2", variant = TextVariant.BODY2)
        val caption = MagicText("Caption", variant = TextVariant.CAPTION)

        assertEquals(TextVariant.H1, h1.variant)
        assertEquals(TextVariant.H2, h2.variant)
        assertEquals(TextVariant.H3, h3.variant)
        assertEquals(TextVariant.BODY1, body1.variant)
        assertEquals(TextVariant.BODY2, body2.variant)
        assertEquals(TextVariant.CAPTION, caption.variant)
    }

    @Test
    fun `text alignment`() {
        val start = MagicText("Start", align = TextAlign.START)
        val center = MagicText("Center", align = TextAlign.CENTER)
        val end = MagicText("End", align = TextAlign.END)

        assertEquals(TextAlign.START, start.align)
        assertEquals(TextAlign.CENTER, center.align)
        assertEquals(TextAlign.END, end.align)
    }

    @Test
    fun `bold text`() {
        val text = MagicText(
            content = "Bold Text",
            bold = true
        )

        assertTrue(text.bold)
    }

    @Test
    fun `italic text`() {
        val text = MagicText(
            content = "Italic Text",
            italic = true
        )

        assertTrue(text.italic)
    }

    @Test
    fun `underlined text`() {
        val text = MagicText(
            content = "Underlined Text",
            underline = true
        )

        assertTrue(text.underline)
    }

    @Test
    fun `text with custom color`() {
        val text = MagicText(
            content = "Colored Text",
            color = "#FF0000"
        )

        assertEquals("#FF0000", text.color)
    }

    @Test
    fun `text with max lines`() {
        val text = MagicText(
            content = "Long text that should be truncated",
            maxLines = 3
        )

        assertEquals(3, text.maxLines)
    }

    @Test
    fun `heading use case`() {
        val text = MagicText(
            content = "Page Title",
            variant = TextVariant.H1,
            bold = true,
            align = TextAlign.CENTER
        )

        assertEquals(TextVariant.H1, text.variant)
        assertTrue(text.bold)
        assertEquals(TextAlign.CENTER, text.align)
    }

    @Test
    fun `paragraph use case`() {
        val text = MagicText(
            content = "This is a paragraph of body text.",
            variant = TextVariant.BODY1,
            maxLines = 5
        )

        assertEquals(TextVariant.BODY1, text.variant)
        assertEquals(5, text.maxLines)
    }

    @Test
    fun `caption use case`() {
        val text = MagicText(
            content = "Image caption",
            variant = TextVariant.CAPTION,
            color = "#666666",
            italic = true
        )

        assertEquals(TextVariant.CAPTION, text.variant)
        assertEquals("#666666", text.color)
        assertTrue(text.italic)
    }
}
