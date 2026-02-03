package com.augmentalis.avaelements.flutter.material.chips

import kotlin.test.*

/**
 * Comprehensive unit tests for InputChip component
 *
 * @since 3.0.0-flutter-parity
 */
class InputChipTest {

    @Test
    fun `create InputChip with default values`() {
        val chip = MagicInput(label = "John Doe")

        assertEquals("MagicInput", chip.type)
        assertEquals("John Doe", chip.label)
        assertFalse(chip.selected)
        assertTrue(chip.enabled)
        assertEquals("close", chip.deleteIcon)
        assertTrue(chip.showCheckmark)
    }

    @Test
    fun `InputChip onDeleted callback triggers deletion`() {
        var deleted = false
        val chip = MagicInput(
            label = "Tag",
            onDeleted = { deleted = true }
        )

        chip.onDeleted?.invoke()
        assertTrue(deleted)
    }

    @Test
    fun `InputChip onSelected callback updates selection state`() {
        var selectedState = false
        val chip = MagicInput(
            label = "Tag",
            onSelected = { selectedState = it }
        )

        chip.onSelected?.invoke(true)
        assertTrue(selectedState)
    }

    @Test
    fun `InputChip accessibility description includes selection state`() {
        val selected = MagicInput(label = "Contact", selected = true)
        assertEquals("Contact, selected", selected.getAccessibilityDescription())

        val unselected = MagicInput(label = "Contact", selected = false)
        assertEquals("Contact, not selected", unselected.getAccessibilityDescription())
    }

    @Test
    fun `InputChip delete button accessibility description`() {
        val chip = MagicInput(
            label = "Tag",
            deleteButtonTooltipMessage = "Remove tag"
        )
        assertEquals("Remove tag", chip.getDeleteButtonAccessibilityDescription())
    }

    @Test
    fun `InputChip delete button accessibility falls back to default`() {
        val chip = MagicInput(label = "Tag")
        assertEquals("Remove Tag", chip.getDeleteButtonAccessibilityDescription())
    }

    @Test
    fun `InputChip simple factory creates basic chip with delete`() {
        val chip = InputChip.simple("User")

        assertEquals("User", chip.label)
        assertNull(chip.avatar)
    }

    @Test
    fun `InputChip withAvatar factory includes avatar`() {
        val chip = InputChip.withAvatar(
            label = "User",
            avatar = "user_avatar"
        )

        assertEquals("User", chip.label)
        assertEquals("user_avatar", chip.avatar)
    }

    @Test
    fun `InputChip selectable factory supports selection`() {
        var selectedState = false
        val chip = InputChip.selectable(
            label = "Filter",
            selected = false,
            onSelected = { selectedState = it }
        )

        assertEquals("Filter", chip.label)
        assertFalse(chip.selected)

        chip.onSelected?.invoke(true)
        assertTrue(selectedState)
    }

    @Test
    fun `InputChip full factory includes all features`() {
        var selectedState = false
        var deleted = false

        val chip = InputChip.full(
            label = "Contact",
            avatar = "contact_avatar",
            selected = false,
            onSelected = { selectedState = it },
            onDeleted = { deleted = true }
        )

        assertEquals("Contact", chip.label)
        assertEquals("contact_avatar", chip.avatar)
        assertFalse(chip.selected)

        chip.onSelected?.invoke(true)
        assertTrue(selectedState)

        chip.onDeleted?.invoke()
        assertTrue(deleted)
    }

    @Test
    fun `InputChip disabled state prevents interaction`() {
        val chip = MagicInput(
            label = "Test",
            enabled = false
        )

        assertFalse(chip.enabled)
    }

    @Test
    fun `InputChip supports custom delete icon`() {
        val chip = MagicInput(
            label = "Test",
            deleteIcon = "cancel"
        )

        assertEquals("cancel", chip.deleteIcon)
    }

    @Test
    fun `InputChip without checkmark hides selection indicator`() {
        val chip = MagicInput(
            label = "Test",
            selected = true,
            showCheckmark = false
        )

        assertTrue(chip.selected)
        assertFalse(chip.showCheckmark)
    }

    @Test
    fun `InputChip onPressed callback triggers action`() {
        var pressed = false
        val chip = MagicInput(
            label = "Test",
            onPressed = { pressed = true }
        )

        chip.onPressed?.invoke()
        assertTrue(pressed)
    }
}
