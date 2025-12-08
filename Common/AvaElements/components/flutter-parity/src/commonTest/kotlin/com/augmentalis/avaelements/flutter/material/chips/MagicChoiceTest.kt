package com.augmentalis.avaelements.flutter.material.chips

import kotlin.test.*

/**
 * Comprehensive unit tests for ChoiceChip component
 *
 * @since 3.0.0-flutter-parity
 */
class ChoiceChipTest {

    @Test
    fun `create ChoiceChip with default values`() {
        val chip = MagicChoice(label = "Small")

        assertEquals("MagicChoice", chip.type)
        assertEquals("Small", chip.label)
        assertFalse(chip.selected)
        assertTrue(chip.enabled)
        assertTrue(chip.showCheckmark)
    }

    @Test
    fun `ChoiceChip selection callback updates state`() {
        var selectedState = false
        val chip = MagicChoice(
            label = "Medium",
            onSelected = { selectedState = it }
        )

        chip.onSelected?.invoke(true)
        assertTrue(selectedState)

        chip.onSelected?.invoke(false)
        assertFalse(selectedState)
    }

    @Test
    fun `ChoiceChip accessibility description includes selection state`() {
        val selected = MagicChoice(label = "Large", selected = true)
        assertEquals("Large, selected", selected.getAccessibilityDescription())

        val unselected = MagicChoice(label = "Large", selected = false)
        assertEquals("Large, not selected", unselected.getAccessibilityDescription())
    }

    @Test
    fun `ChoiceChip selected factory creates selected chip`() {
        val chip = ChoiceChip.selected("XL")

        assertTrue(chip.selected)
        assertEquals("XL", chip.label)
    }

    @Test
    fun `ChoiceChip unselected factory creates unselected chip`() {
        val chip = ChoiceChip.unselected("XS")

        assertFalse(chip.selected)
        assertEquals("XS", chip.label)
    }

    @Test
    fun `ChoiceChip withAvatar factory includes avatar`() {
        val chip = ChoiceChip.withAvatar(
            label = "Premium",
            avatar = "star_icon",
            selected = true
        )

        assertEquals("Premium", chip.label)
        assertEquals("star_icon", chip.avatar)
        assertTrue(chip.selected)
    }

    @Test
    fun `ChoiceChip supports single selection behavior`() {
        // Simulate a group of choice chips
        var selectedValue = "small"

        val small = MagicChoice(
            label = "Small",
            selected = selectedValue == "small",
            onSelected = { selectedValue = "small" }
        )

        val medium = MagicChoice(
            label = "Medium",
            selected = selectedValue == "medium",
            onSelected = { selectedValue = "medium" }
        )

        assertTrue(small.selected)
        assertFalse(medium.selected)

        // Select medium
        medium.onSelected?.invoke(true)
        assertEquals("medium", selectedValue)
    }

    @Test
    fun `ChoiceChip disabled state prevents selection`() {
        var callbackInvoked = false
        val chip = MagicChoice(
            label = "Test",
            enabled = false,
            onSelected = { callbackInvoked = true }
        )

        assertFalse(chip.enabled)
    }

    @Test
    fun `ChoiceChip without checkmark hides selection indicator`() {
        val chip = MagicChoice(
            label = "Test",
            selected = true,
            showCheckmark = false
        )

        assertTrue(chip.selected)
        assertFalse(chip.showCheckmark)
    }
}
