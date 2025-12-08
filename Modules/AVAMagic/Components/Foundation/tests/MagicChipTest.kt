package net.ideahq.ideamagic.components.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MagicChip Component Tests
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class MagicChipTest {

    @Test
    fun `chip creation with default values`() {
        val chip = MagicChip(
            label = "Tag"
        )

        assertEquals("Tag", chip.label)
        assertEquals(ChipVariant.DEFAULT, chip.variant)
        assertEquals(null, chip.icon)
        assertFalse(chip.deletable)
        assertEquals(null, chip.onClick)
        assertEquals(null, chip.onDelete)
    }

    @Test
    fun `chip with all properties`() {
        val chip = MagicChip(
            label = "Category",
            variant = ChipVariant.OUTLINED,
            icon = "tag",
            deletable = true,
            onClick = {},
            onDelete = {}
        )

        assertEquals("Category", chip.label)
        assertEquals(ChipVariant.OUTLINED, chip.variant)
        assertEquals("tag", chip.icon)
        assertTrue(chip.deletable)
        assertTrue(chip.onClick != null)
        assertTrue(chip.onDelete != null)
    }

    @Test
    fun `chip variants`() {
        val default = MagicChip("Default", variant = ChipVariant.DEFAULT)
        val outlined = MagicChip("Outlined", variant = ChipVariant.OUTLINED)
        val filled = MagicChip("Filled", variant = ChipVariant.FILLED)

        assertEquals(ChipVariant.DEFAULT, default.variant)
        assertEquals(ChipVariant.OUTLINED, outlined.variant)
        assertEquals(ChipVariant.FILLED, filled.variant)
    }

    @Test
    fun `chip with icon`() {
        val chip = MagicChip(
            label = "Icon Chip",
            icon = "star"
        )

        assertEquals("star", chip.icon)
    }

    @Test
    fun `deletable chip`() {
        var deleted = false
        val chip = MagicChip(
            label = "Delete Me",
            deletable = true,
            onDelete = { deleted = true }
        )

        assertTrue(chip.deletable)
        chip.onDelete?.invoke()
        assertTrue(deleted)
    }

    @Test
    fun `non-deletable chip`() {
        val chip = MagicChip(
            label = "Static",
            deletable = false
        )

        assertFalse(chip.deletable)
        assertEquals(null, chip.onDelete)
    }

    @Test
    fun `clickable chip`() {
        var clicked = false
        val chip = MagicChip(
            label = "Click Me",
            onClick = { clicked = true }
        )

        chip.onClick?.invoke()
        assertTrue(clicked)
    }

    @Test
    fun `chip with icon and delete`() {
        var deleted = false
        val chip = MagicChip(
            label = "Tag",
            icon = "tag",
            deletable = true,
            onDelete = { deleted = true }
        )

        assertEquals("tag", chip.icon)
        assertTrue(chip.deletable)
        chip.onDelete?.invoke()
        assertTrue(deleted)
    }

    @Test
    fun `filter chip use case`() {
        var selected = false
        val chip = MagicChip(
            label = "Active",
            variant = ChipVariant.FILLED,
            onClick = { selected = !selected }
        )

        assertEquals(ChipVariant.FILLED, chip.variant)
        chip.onClick?.invoke()
        assertTrue(selected)
    }
}
