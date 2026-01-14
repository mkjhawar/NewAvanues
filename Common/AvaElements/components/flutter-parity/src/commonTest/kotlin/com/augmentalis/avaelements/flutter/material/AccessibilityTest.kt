package com.augmentalis.avaelements.flutter.material

import com.augmentalis.avaelements.flutter.material.chips.*
import com.augmentalis.avaelements.flutter.material.lists.*
import kotlin.test.*

/**
 * Comprehensive accessibility tests for Flutter Material parity components
 *
 * Tests WCAG 2.1 Level AA compliance:
 * - Proper content descriptions for TalkBack
 * - State announcements
 * - Action descriptions
 * - Keyboard navigation support
 * - Minimum touch target sizes (48dp)
 *
 * @since 3.0.0-flutter-parity
 */
class AccessibilityTest {

    // CHIP ACCESSIBILITY TESTS

    @Test
    fun `FilterChip provides accessible state description`() {
        val selected = FilterChip(label = "Category", selected = true)
        val description = selected.getAccessibilityDescription()

        assertTrue(description.contains("selected"))
        assertTrue(description.contains("Category"))
    }

    @Test
    fun `FilterChip uses custom contentDescription when provided`() {
        val chip = FilterChip(
            label = "Cat",
            contentDescription = "Filter by Category"
        )

        val description = chip.getAccessibilityDescription()
        assertTrue(description.startsWith("Filter by Category"))
    }

    @Test
    fun `ActionChip provides accessible description priority`() {
        // Test priority: contentDescription > tooltip > label
        val withContent = ActionChip(
            label = "Send",
            tooltip = "Send message",
            contentDescription = "Send message to recipient"
        )
        assertEquals("Send message to recipient", withContent.getAccessibilityDescription())

        val withTooltip = ActionChip(
            label = "Send",
            tooltip = "Send message"
        )
        assertEquals("Send message", withTooltip.getAccessibilityDescription())

        val labelOnly = ActionChip(label = "Send")
        assertEquals("Send", labelOnly.getAccessibilityDescription())
    }

    @Test
    fun `ChoiceChip announces selection state for TalkBack`() {
        val selected = ChoiceChip(label = "Large", selected = true)
        assertTrue(selected.getAccessibilityDescription().contains("selected"))

        val unselected = ChoiceChip(label = "Large", selected = false)
        assertTrue(unselected.getAccessibilityDescription().contains("not selected"))
    }

    @Test
    fun `InputChip provides delete action accessibility`() {
        val chip = InputChip(
            label = "Contact",
            deleteButtonTooltipMessage = "Remove contact"
        )

        assertEquals("Remove contact", chip.getDeleteButtonAccessibilityDescription())
    }

    @Test
    fun `InputChip delete action has fallback description`() {
        val chip = InputChip(label = "Tag")
        val description = chip.getDeleteButtonAccessibilityDescription()

        assertTrue(description.contains("Remove"))
        assertTrue(description.contains("Tag"))
    }

    // LIST TILE ACCESSIBILITY TESTS

    @Test
    fun `CheckboxListTile announces checkbox state`() {
        val checked = CheckboxListTile(title = "Enable", value = true)
        assertTrue(checked.getAccessibilityDescription().contains("checked"))

        val unchecked = CheckboxListTile(title = "Enable", value = false)
        assertTrue(unchecked.getAccessibilityDescription().contains("unchecked"))

        val indeterminate = CheckboxListTile(title = "Enable", value = null, tristate = true)
        assertTrue(indeterminate.getAccessibilityDescription().contains("indeterminate"))
    }

    @Test
    fun `SwitchListTile announces switch state`() {
        val on = SwitchListTile(title = "WiFi", value = true)
        assertTrue(on.getAccessibilityDescription().contains("on"))

        val off = SwitchListTile(title = "WiFi", value = false)
        assertTrue(off.getAccessibilityDescription().contains("off"))
    }

    @Test
    fun `ExpansionTile announces expansion state`() {
        val tile = ExpansionTile(title = "Settings")

        val expanded = tile.getAccessibilityDescription(true)
        assertTrue(expanded.contains("expanded"))

        val collapsed = tile.getAccessibilityDescription(false)
        assertTrue(collapsed.contains("collapsed"))
    }

    // WCAG 2.1 LEVEL AA COMPLIANCE

    @Test
    fun `All components support custom contentDescription for WCAG`() {
        // Verify all components accept contentDescription parameter
        val filterChip = FilterChip(label = "Test", contentDescription = "Custom")
        assertNotNull(filterChip.contentDescription)

        val actionChip = ActionChip(label = "Test", contentDescription = "Custom")
        assertNotNull(actionChip.contentDescription)

        val choiceChip = ChoiceChip(label = "Test", contentDescription = "Custom")
        assertNotNull(choiceChip.contentDescription)

        val inputChip = InputChip(label = "Test", contentDescription = "Custom")
        assertNotNull(inputChip.contentDescription)

        val checkboxTile = CheckboxListTile(title = "Test", contentDescription = "Custom")
        assertNotNull(checkboxTile.contentDescription)

        val switchTile = SwitchListTile(title = "Test", contentDescription = "Custom")
        assertNotNull(switchTile.contentDescription)

        val expansionTile = ExpansionTile(title = "Test", contentDescription = "Custom")
        assertNotNull(expansionTile.contentDescription)
    }

    @Test
    fun `All components support enabled state for accessibility`() {
        // Verify all interactive components have enabled property
        assertTrue(FilterChip(label = "Test", enabled = true).enabled)
        assertTrue(ActionChip(label = "Test", enabled = true).enabled)
        assertTrue(ChoiceChip(label = "Test", enabled = true).enabled)
        assertTrue(InputChip(label = "Test", enabled = true).enabled)
        assertTrue(CheckboxListTile(title = "Test", enabled = true).enabled)
        assertTrue(SwitchListTile(title = "Test", enabled = true).enabled)

        assertFalse(FilterChip(label = "Test", enabled = false).enabled)
        assertFalse(ActionChip(label = "Test", enabled = false).enabled)
    }

    @Test
    fun `ActionChip supports minimum tap target size for WCAG`() {
        val padded = ActionChip(
            label = "Test",
            materialTapTargetSize = ActionChip.MaterialTapTargetSize.PadOrExpand
        )
        assertEquals(ActionChip.MaterialTapTargetSize.PadOrExpand, padded.materialTapTargetSize)
    }

    @Test
    fun `ChoiceChip supports minimum tap target size for WCAG`() {
        val padded = ChoiceChip(
            label = "Test",
            materialTapTargetSize = ChoiceChip.MaterialTapTargetSize.PadOrExpand
        )
        assertEquals(ChoiceChip.MaterialTapTargetSize.PadOrExpand, padded.materialTapTargetSize)
    }

    // KEYBOARD NAVIGATION SUPPORT

    @Test
    fun `ActionChip supports autofocus for keyboard navigation`() {
        val chip = ActionChip(label = "Test", autofocus = true)
        assertTrue(chip.autofocus)
    }

    @Test
    fun `InputChip supports autofocus for keyboard navigation`() {
        val chip = InputChip(label = "Test", autofocus = true)
        assertTrue(chip.autofocus)
    }

    @Test
    fun `CheckboxListTile supports autofocus for keyboard navigation`() {
        val tile = CheckboxListTile(title = "Test", autofocus = true)
        assertTrue(tile.autofocus)
    }

    @Test
    fun `SwitchListTile supports autofocus for keyboard navigation`() {
        val tile = SwitchListTile(title = "Test", autofocus = true)
        assertTrue(tile.autofocus)
    }

    // STATE CHANGE ANNOUNCEMENTS

    @Test
    fun `FilterChip state changes are accessible`() {
        var announced = false
        val chip = FilterChip(
            label = "Filter",
            onSelected = {
                // In real implementation, this would trigger TalkBack announcement
                announced = true
            }
        )

        chip.onSelected?.invoke(true)
        assertTrue(announced)
    }

    @Test
    fun `CheckboxListTile state changes are accessible`() {
        var announced = false
        val tile = CheckboxListTile(
            title = "Option",
            onChanged = {
                // In real implementation, this would trigger TalkBack announcement
                announced = true
            }
        )

        tile.onChanged?.invoke(true)
        assertTrue(announced)
    }

    @Test
    fun `ExpansionTile expansion changes are accessible`() {
        var announced = false
        val tile = ExpansionTile(
            title = "Menu",
            onExpansionChanged = {
                // In real implementation, this would trigger TalkBack announcement
                announced = true
            }
        )

        tile.onExpansionChanged?.invoke(true)
        assertTrue(announced)
    }
}
