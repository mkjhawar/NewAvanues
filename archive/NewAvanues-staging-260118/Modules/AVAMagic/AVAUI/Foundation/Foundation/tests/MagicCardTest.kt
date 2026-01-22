package com.augmentalis.avamagic.components.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MagicCard Component Tests
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class MagicCardTest {

    @Test
    fun `card creation with default values`() {
        val card = MagicCard(
            content = listOf()
        )

        assertTrue(card.content.isEmpty())
        assertFalse(card.elevated)
        assertEquals(CardVariant.DEFAULT, card.variant)
        assertEquals(null, card.onClick)
    }

    @Test
    fun `card with all properties`() {
        val content = listOf("Header", "Body", "Footer")
        val card = MagicCard(
            content = content,
            elevated = true,
            variant = CardVariant.OUTLINED,
            onClick = {}
        )

        assertEquals(content, card.content)
        assertTrue(card.elevated)
        assertEquals(CardVariant.OUTLINED, card.variant)
        assertEquals(true, card.onClick != null)
    }

    @Test
    fun `card variants`() {
        val default = MagicCard(listOf(), variant = CardVariant.DEFAULT)
        val outlined = MagicCard(listOf(), variant = CardVariant.OUTLINED)
        val filled = MagicCard(listOf(), variant = CardVariant.FILLED)

        assertEquals(CardVariant.DEFAULT, default.variant)
        assertEquals(CardVariant.OUTLINED, outlined.variant)
        assertEquals(CardVariant.FILLED, filled.variant)
    }

    @Test
    fun `elevated card`() {
        val card = MagicCard(
            content = listOf(),
            elevated = true
        )

        assertTrue(card.elevated)
    }

    @Test
    fun `non-elevated card`() {
        val card = MagicCard(
            content = listOf(),
            elevated = false
        )

        assertFalse(card.elevated)
    }

    @Test
    fun `clickable card`() {
        var clicked = false
        val card = MagicCard(
            content = listOf(),
            onClick = { clicked = true }
        )

        card.onClick?.invoke()
        assertTrue(clicked)
    }

    @Test
    fun `non-clickable card`() {
        val card = MagicCard(
            content = listOf()
        )

        assertEquals(null, card.onClick)
    }

    @Test
    fun `card with complex content`() {
        val content = listOf(
            "Title Text",
            "Subtitle",
            "Body content goes here",
            "Action buttons"
        )
        val card = MagicCard(content = content)

        assertEquals(4, card.content.size)
        assertEquals("Title Text", card.content[0])
        assertEquals("Action buttons", card.content[3])
    }
}
