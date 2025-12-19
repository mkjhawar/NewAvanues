package net.ideahq.ideamagic.components.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MagicButton Component Tests
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class MagicButtonTest {

    @Test
    fun `button creation with default values`() {
        val button = MagicButton(
            text = "Click Me",
            onClick = {}
        )

        assertEquals("Click Me", button.text)
        assertEquals(ButtonVariant.PRIMARY, button.variant)
        assertEquals(ButtonSize.MEDIUM, button.size)
        assertTrue(button.enabled)
        assertFalse(button.fullWidth)
        assertEquals(null, button.icon)
        assertEquals(IconPosition.START, button.iconPosition)
    }

    @Test
    fun `button with all properties`() {
        val button = MagicButton(
            text = "Submit",
            onClick = {},
            variant = ButtonVariant.SECONDARY,
            size = ButtonSize.LARGE,
            enabled = false,
            fullWidth = true,
            icon = "check",
            iconPosition = IconPosition.END
        )

        assertEquals("Submit", button.text)
        assertEquals(ButtonVariant.SECONDARY, button.variant)
        assertEquals(ButtonSize.LARGE, button.size)
        assertFalse(button.enabled)
        assertTrue(button.fullWidth)
        assertEquals("check", button.icon)
        assertEquals(IconPosition.END, button.iconPosition)
    }

    @Test
    fun `button variants`() {
        val primary = MagicButton("Primary", {}, variant = ButtonVariant.PRIMARY)
        val secondary = MagicButton("Secondary", {}, variant = ButtonVariant.SECONDARY)
        val outlined = MagicButton("Outlined", {}, variant = ButtonVariant.OUTLINED)
        val text = MagicButton("Text", {}, variant = ButtonVariant.TEXT)

        assertEquals(ButtonVariant.PRIMARY, primary.variant)
        assertEquals(ButtonVariant.SECONDARY, secondary.variant)
        assertEquals(ButtonVariant.OUTLINED, outlined.variant)
        assertEquals(ButtonVariant.TEXT, text.variant)
    }

    @Test
    fun `button sizes`() {
        val small = MagicButton("Small", {}, size = ButtonSize.SMALL)
        val medium = MagicButton("Medium", {}, size = ButtonSize.MEDIUM)
        val large = MagicButton("Large", {}, size = ButtonSize.LARGE)

        assertEquals(ButtonSize.SMALL, small.size)
        assertEquals(ButtonSize.MEDIUM, medium.size)
        assertEquals(ButtonSize.LARGE, large.size)
    }

    @Test
    fun `button with icon at start`() {
        val button = MagicButton(
            text = "Save",
            onClick = {},
            icon = "save",
            iconPosition = IconPosition.START
        )

        assertEquals("save", button.icon)
        assertEquals(IconPosition.START, button.iconPosition)
    }

    @Test
    fun `button with icon at end`() {
        val button = MagicButton(
            text = "Next",
            onClick = {},
            icon = "arrow-right",
            iconPosition = IconPosition.END
        )

        assertEquals("arrow-right", button.icon)
        assertEquals(IconPosition.END, button.iconPosition)
    }

    @Test
    fun `disabled button`() {
        val button = MagicButton(
            text = "Disabled",
            onClick = {},
            enabled = false
        )

        assertFalse(button.enabled)
    }

    @Test
    fun `full width button`() {
        val button = MagicButton(
            text = "Full Width",
            onClick = {},
            fullWidth = true
        )

        assertTrue(button.fullWidth)
    }

    @Test
    fun `button click handler`() {
        var clicked = false
        val button = MagicButton(
            text = "Click",
            onClick = { clicked = true }
        )

        button.onClick()
        assertTrue(clicked)
    }
}
