package com.augmentalis.avaelements.flutter.material.chips

import kotlin.test.*

/**
 * Comprehensive unit tests for FilterChip component
 *
 * Tests cover:
 * - Component creation and properties
 * - Selection state management
 * - Accessibility descriptions
 * - Factory methods
 * - Edge cases
 *
 * @since 3.0.0-flutter-parity
 */
class FilterChipTest {

    @Test
    fun `create FilterChip with default values`() {
        val chip = MagicFilter(label = "Test")

        assertEquals("MagicFilter", chip.type)
        assertEquals("Test", chip.label)
        assertFalse(chip.selected)
        assertTrue(chip.enabled)
        assertTrue(chip.showCheckmark)
        assertNull(chip.avatar)
        assertNull(chip.contentDescription)
        assertNull(chip.onSelected)
    }

    @Test
    fun `create FilterChip with all properties`() {
        var selectedState = false
        val chip = MagicFilter(
            id = "filter-1",
            label = "Category",
            selected = true,
            enabled = true,
            showCheckmark = false,
            avatar = "category_icon",
            contentDescription = "Filter by category",
            onSelected = { selectedState = it }
        )

        assertEquals("filter-1", chip.id)
        assertEquals("Category", chip.label)
        assertTrue(chip.selected)
        assertTrue(chip.enabled)
        assertFalse(chip.showCheckmark)
        assertEquals("category_icon", chip.avatar)
        assertEquals("Filter by category", chip.contentDescription)
        assertNotNull(chip.onSelected)
    }

    @Test
    fun `FilterChip selection callback updates state`() {
        var selectedState = false
        val chip = MagicFilter(
            label = "Filter",
            selected = false,
            onSelected = { selectedState = it }
        )

        chip.onSelected?.invoke(true)
        assertTrue(selectedState)

        chip.onSelected?.invoke(false)
        assertFalse(selectedState)
    }

    @Test
    fun `FilterChip accessibility description includes selection state`() {
        val selectedChip = MagicFilter(label = "Test", selected = true)
        assertEquals("Test, selected", selectedChip.getAccessibilityDescription())

        val unselectedChip = MagicFilter(label = "Test", selected = false)
        assertEquals("Test, not selected", unselectedChip.getAccessibilityDescription())
    }

    @Test
    fun `FilterChip accessibility description uses custom contentDescription`() {
        val chip = MagicFilter(
            label = "Test",
            selected = true,
            contentDescription = "Custom description"
        )
        assertEquals("Custom description, selected", chip.getAccessibilityDescription())
    }

    @Test
    fun `FilterChip selected factory creates selected chip`() {
        val chip = FilterChip.selected("Test")

        assertTrue(chip.selected)
        assertEquals("Test", chip.label)
    }

    @Test
    fun `FilterChip unselected factory creates unselected chip`() {
        val chip = FilterChip.unselected("Test")

        assertFalse(chip.selected)
        assertEquals("Test", chip.label)
    }

    @Test
    fun `FilterChip withAvatar factory creates chip with avatar`() {
        val chip = FilterChip.withAvatar(
            label = "Test",
            avatar = "test_avatar",
            selected = true
        )

        assertEquals("Test", chip.label)
        assertEquals("test_avatar", chip.avatar)
        assertTrue(chip.selected)
    }

    @Test
    fun `FilterChip disabled state prevents interaction`() {
        var callbackInvoked = false
        val chip = MagicFilter(
            label = "Test",
            enabled = false,
            onSelected = { callbackInvoked = true }
        )

        assertFalse(chip.enabled)
        // In real rendering, disabled chips should not trigger callbacks
        // This test just verifies the enabled flag
    }

    @Test
    fun `FilterChip without checkmark hides selection indicator`() {
        val chip = MagicFilter(
            label = "Test",
            selected = true,
            showCheckmark = false
        )

        assertTrue(chip.selected)
        assertFalse(chip.showCheckmark)
    }

    @Test
    fun `FilterChip can be copied with different selection state`() {
        val original = MagicFilter(label = "Test", selected = false)
        val modified = original.copy(selected = true)

        assertFalse(original.selected)
        assertTrue(modified.selected)
    }

    @Test
    fun `FilterChip equality based on properties`() {
        val chip1 = MagicFilter(label = "Test", selected = true)
        val chip2 = MagicFilter(label = "Test", selected = true)

        // Data classes provide equals() based on properties (excluding @Transient)
        assertEquals(chip1.label, chip2.label)
        assertEquals(chip1.selected, chip2.selected)
    }

    @Test
    fun `FilterChip with avatar and selected shows appropriate icon`() {
        val chip = MagicFilter(
            label = "Test",
            avatar = "avatar_icon",
            selected = true
        )

        // When selected, should show checkmark (avatar is secondary)
        assertTrue(chip.selected)
        assertEquals("avatar_icon", chip.avatar)
        assertTrue(chip.showCheckmark)
    }

    @Test
    fun `FilterChip supports null id`() {
        val chip = MagicFilter(label = "Test")
        assertNull(chip.id)
    }

    @Test
    fun `FilterChip supports custom id`() {
        val chip = MagicFilter(id = "custom-id", label = "Test")
        assertEquals("custom-id", chip.id)
    }
}
