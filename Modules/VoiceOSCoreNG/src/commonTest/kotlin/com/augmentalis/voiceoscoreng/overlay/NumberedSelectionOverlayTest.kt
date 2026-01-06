/**
 * NumberedSelectionOverlayTest.kt - TDD tests for NumberedSelectionOverlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * Comprehensive test suite for NumberedSelectionOverlay functionality.
 * Tests are written FIRST following TDD methodology.
 */
package com.augmentalis.voiceoscoreng.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Test suite for NumberedSelectionOverlay.
 *
 * Covers:
 * - Item management (add, update, remove, clear)
 * - Selection by number
 * - Badge color determination based on item state
 * - Badge positioning calculations
 * - Enabled/disabled states
 * - IOverlay contract compliance
 */
class NumberedSelectionOverlayTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Test Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private fun createTestItems(): List<NumberedItem> = listOf(
        NumberedItem(1, "Submit Button", Rect(100, 200, 300, 250), isEnabled = true, hasName = true),
        NumberedItem(2, "Cancel", Rect(100, 260, 300, 310), isEnabled = true, hasName = true),
        NumberedItem(3, "", Rect(100, 320, 300, 370), isEnabled = true, hasName = false),
        NumberedItem(4, "Disabled Button", Rect(100, 380, 300, 430), isEnabled = false, hasName = true)
    )

    private fun createOverlay(): NumberedSelectionOverlay {
        return NumberedSelectionOverlay(style = NumberOverlayStyles.DEFAULT)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Constructor Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `constructor creates overlay with default style`() {
        val overlay = NumberedSelectionOverlay()
        assertNotNull(overlay)
        assertEquals(NumberOverlayStyles.DEFAULT, overlay.style)
    }

    @Test
    fun `constructor creates overlay with custom style`() {
        val customStyle = NumberOverlayStyles.HIGH_CONTRAST
        val overlay = NumberedSelectionOverlay(style = customStyle)
        assertEquals(customStyle, overlay.style)
    }

    @Test
    fun `constructor creates overlay with custom id`() {
        val overlay = NumberedSelectionOverlay(id = "custom-overlay-id")
        assertEquals("custom-overlay-id", overlay.id)
    }

    @Test
    fun `constructor creates overlay with default id when not specified`() {
        val overlay = NumberedSelectionOverlay()
        assertTrue(overlay.id.isNotEmpty())
        assertTrue(overlay.id.startsWith("numbered-selection-overlay"))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // IOverlay Contract Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `overlay starts hidden`() {
        val overlay = createOverlay()
        assertFalse(overlay.isVisible)
    }

    @Test
    fun `show makes overlay visible`() {
        val overlay = createOverlay()
        overlay.show()
        assertTrue(overlay.isVisible)
    }

    @Test
    fun `hide makes overlay invisible`() {
        val overlay = createOverlay()
        overlay.show()
        overlay.hide()
        assertFalse(overlay.isVisible)
    }

    @Test
    fun `toggle switches visibility`() {
        val overlay = createOverlay()
        assertFalse(overlay.isVisible)

        overlay.toggle()
        assertTrue(overlay.isVisible)

        overlay.toggle()
        assertFalse(overlay.isVisible)
    }

    @Test
    fun `dispose cleans up overlay`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())
        overlay.show()

        overlay.dispose()

        assertFalse(overlay.isVisible)
        assertTrue(overlay.items.isEmpty())
    }

    @Test
    fun `show does nothing after dispose`() {
        val overlay = createOverlay()
        overlay.dispose()
        overlay.show()
        assertFalse(overlay.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Item Management Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `showItems sets items and shows overlay`() {
        val overlay = createOverlay()
        val items = createTestItems()

        overlay.showItems(items)

        assertEquals(4, overlay.items.size)
        assertTrue(overlay.isVisible)
    }

    @Test
    fun `updateItems changes items without affecting visibility`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())
        assertTrue(overlay.isVisible)

        val newItems = listOf(
            NumberedItem(1, "New Item", Rect(0, 0, 100, 50))
        )
        overlay.updateItems(newItems)

        assertEquals(1, overlay.items.size)
        assertEquals("New Item", overlay.items[0].label)
        assertTrue(overlay.isVisible)
    }

    @Test
    fun `updateItems on hidden overlay does not show it`() {
        val overlay = createOverlay()
        assertFalse(overlay.isVisible)

        overlay.updateItems(createTestItems())

        assertFalse(overlay.isVisible)
        assertEquals(4, overlay.items.size)
    }

    @Test
    fun `clearItems removes all items`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())
        assertEquals(4, overlay.items.size)

        overlay.clearItems()

        assertTrue(overlay.items.isEmpty())
    }

    @Test
    fun `clearItems hides overlay when autoHide is true`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())
        assertTrue(overlay.isVisible)

        overlay.clearItems(autoHide = true)

        assertTrue(overlay.items.isEmpty())
        assertFalse(overlay.isVisible)
    }

    @Test
    fun `clearItems keeps overlay visible when autoHide is false`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())
        assertTrue(overlay.isVisible)

        overlay.clearItems(autoHide = false)

        assertTrue(overlay.items.isEmpty())
        assertTrue(overlay.isVisible)
    }

    @Test
    fun `items property returns immutable copy`() {
        val overlay = createOverlay()
        val originalItems = createTestItems()
        overlay.showItems(originalItems)

        val retrievedItems = overlay.items

        assertEquals(originalItems.size, retrievedItems.size)
        // Verify it's a copy by checking class type (should be List, not MutableList)
        assertTrue(retrievedItems is List<NumberedItem>)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Selection Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `selectItem returns item when number exists`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())

        val result = overlay.selectItem(1)

        assertNotNull(result)
        assertEquals(1, result.number)
        assertEquals("Submit Button", result.label)
    }

    @Test
    fun `selectItem returns null when number does not exist`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())

        val result = overlay.selectItem(99)

        assertNull(result)
    }

    @Test
    fun `selectItem returns null for disabled item by default`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())

        // Item 4 is disabled
        val result = overlay.selectItem(4)

        assertNull(result)
    }

    @Test
    fun `selectItem returns disabled item when ignoreDisabled is false`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())

        val result = overlay.selectItem(4, ignoreDisabled = false)

        assertNotNull(result)
        assertEquals(4, result.number)
        assertFalse(result.isEnabled)
    }

    @Test
    fun `selectItem returns null when items are empty`() {
        val overlay = createOverlay()

        val result = overlay.selectItem(1)

        assertNull(result)
    }

    @Test
    fun `getItemByNumber returns correct item`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())

        val item = overlay.getItemByNumber(2)

        assertNotNull(item)
        assertEquals("Cancel", item.label)
    }

    @Test
    fun `getItemByNumber returns null for invalid number`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())

        val item = overlay.getItemByNumber(100)

        assertNull(item)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Badge Color Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `getBadgeColor returns hasNameColor for enabled item with name`() {
        val overlay = createOverlay()
        val item = NumberedItem(1, "Button", Rect(0, 0, 100, 50), isEnabled = true, hasName = true)

        val color = overlay.getBadgeColor(item)

        assertEquals(NumberOverlayStyles.DEFAULT.hasNameColor, color)
    }

    @Test
    fun `getBadgeColor returns noNameColor for enabled item without name`() {
        val overlay = createOverlay()
        val item = NumberedItem(1, "", Rect(0, 0, 100, 50), isEnabled = true, hasName = false)

        val color = overlay.getBadgeColor(item)

        assertEquals(NumberOverlayStyles.DEFAULT.noNameColor, color)
    }

    @Test
    fun `getBadgeColor returns disabledColor for disabled item`() {
        val overlay = createOverlay()
        val item = NumberedItem(1, "Disabled", Rect(0, 0, 100, 50), isEnabled = false, hasName = true)

        val color = overlay.getBadgeColor(item)

        assertEquals(NumberOverlayStyles.DEFAULT.disabledColor, color)
    }

    @Test
    fun `getBadgeColor uses custom style colors`() {
        val customStyle = NumberOverlayStyle(
            hasNameColor = 0xFFFF0000,
            noNameColor = 0xFF00FF00,
            disabledColor = 0xFF0000FF
        )
        val overlay = NumberedSelectionOverlay(style = customStyle)

        val enabledWithName = NumberedItem(1, "Button", Rect.EMPTY, isEnabled = true, hasName = true)
        val enabledNoName = NumberedItem(2, "", Rect.EMPTY, isEnabled = true, hasName = false)
        val disabled = NumberedItem(3, "Disabled", Rect.EMPTY, isEnabled = false)

        assertEquals(0xFFFF0000, overlay.getBadgeColor(enabledWithName))
        assertEquals(0xFF00FF00, overlay.getBadgeColor(enabledNoName))
        assertEquals(0xFF0000FF, overlay.getBadgeColor(disabled))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Badge Positioning Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `calculateBadgePosition returns top-right by default`() {
        val overlay = createOverlay()
        val bounds = Rect(100, 200, 300, 250)

        val position = overlay.calculateBadgePosition(bounds)

        // Default anchor is TOP_RIGHT with offset (-4, -4)
        // Badge at: right - circleRadius*2 + offsetX, top + offsetY
        val expectedX = 300 - 32 + (-4)  // right - badgeSize + offsetX
        val expectedY = 200 + (-4)        // top + offsetY

        assertEquals(expectedX, position.x)
        assertEquals(expectedY, position.y)
    }

    @Test
    fun `calculateBadgePosition respects TOP_LEFT anchor`() {
        val customStyle = NumberOverlayStyle(anchorPoint = AnchorPoint.TOP_LEFT)
        val overlay = NumberedSelectionOverlay(style = customStyle)
        val bounds = Rect(100, 200, 300, 250)

        val position = overlay.calculateBadgePosition(bounds)

        // TOP_LEFT: left + offsetX, top + offsetY
        val expectedX = 100 + (-4)
        val expectedY = 200 + (-4)

        assertEquals(expectedX, position.x)
        assertEquals(expectedY, position.y)
    }

    @Test
    fun `calculateBadgePosition respects BOTTOM_LEFT anchor`() {
        val customStyle = NumberOverlayStyle(anchorPoint = AnchorPoint.BOTTOM_LEFT)
        val overlay = NumberedSelectionOverlay(style = customStyle)
        val bounds = Rect(100, 200, 300, 250)

        val position = overlay.calculateBadgePosition(bounds)

        // BOTTOM_LEFT: left + offsetX, bottom - badgeSize + offsetY
        val expectedX = 100 + (-4)
        val expectedY = 250 - 32 + (-4)

        assertEquals(expectedX, position.x)
        assertEquals(expectedY, position.y)
    }

    @Test
    fun `calculateBadgePosition respects BOTTOM_RIGHT anchor`() {
        val customStyle = NumberOverlayStyle(anchorPoint = AnchorPoint.BOTTOM_RIGHT)
        val overlay = NumberedSelectionOverlay(style = customStyle)
        val bounds = Rect(100, 200, 300, 250)

        val position = overlay.calculateBadgePosition(bounds)

        // BOTTOM_RIGHT: right - badgeSize + offsetX, bottom - badgeSize + offsetY
        val expectedX = 300 - 32 + (-4)
        val expectedY = 250 - 32 + (-4)

        assertEquals(expectedX, position.x)
        assertEquals(expectedY, position.y)
    }

    @Test
    fun `calculateBadgePosition uses custom offsets`() {
        val customStyle = NumberOverlayStyle(
            anchorPoint = AnchorPoint.TOP_RIGHT,
            offsetX = 10f,
            offsetY = 5f
        )
        val overlay = NumberedSelectionOverlay(style = customStyle)
        val bounds = Rect(100, 200, 300, 250)

        val position = overlay.calculateBadgePosition(bounds)

        // TOP_RIGHT with custom offsets
        val expectedX = 300 - 32 + 10
        val expectedY = 200 + 5

        assertEquals(expectedX, position.x)
        assertEquals(expectedY, position.y)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Badge Size Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `getBadgeSize returns correct size from style`() {
        val overlay = createOverlay()
        val size = overlay.getBadgeSize()

        // Default circleRadius is 16, so diameter is 32
        assertEquals(32, size)
    }

    @Test
    fun `getBadgeSize uses custom style radius`() {
        val customStyle = NumberOverlayStyle(circleRadius = 20f)
        val overlay = NumberedSelectionOverlay(style = customStyle)

        val size = overlay.getBadgeSize()

        assertEquals(40, size)  // diameter = radius * 2
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Item Count Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `itemCount returns zero for empty overlay`() {
        val overlay = createOverlay()
        assertEquals(0, overlay.itemCount)
    }

    @Test
    fun `itemCount returns correct count after showItems`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())
        assertEquals(4, overlay.itemCount)
    }

    @Test
    fun `enabledItemCount returns only enabled items`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())

        // 3 enabled items (1, 2, 3), 1 disabled (4)
        assertEquals(3, overlay.enabledItemCount)
    }

    @Test
    fun `hasItems returns false when empty`() {
        val overlay = createOverlay()
        assertFalse(overlay.hasItems)
    }

    @Test
    fun `hasItems returns true when items exist`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())
        assertTrue(overlay.hasItems)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Update via IOverlay Interface Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `update with NumberedItems data updates items`() {
        val overlay = createOverlay()
        val items = listOf(
            NumberedItem(1, "First", Rect(0, 0, 100, 50)),
            NumberedItem(2, "Second", Rect(0, 60, 100, 110))
        )

        overlay.update(OverlayData.NumberedItems(items))

        assertEquals(2, overlay.items.size)
        assertEquals("First", overlay.items[0].label)
        assertEquals("Second", overlay.items[1].label)
    }

    @Test
    fun `update with non-NumberedItems data is ignored`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())
        val originalCount = overlay.itemCount

        // Try updating with Status data - should be ignored
        overlay.update(OverlayData.Status("Test", CommandState.LISTENING))

        assertEquals(originalCount, overlay.itemCount)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Style Modification Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `setStyle changes overlay style`() {
        val overlay = createOverlay()
        assertEquals(NumberOverlayStyles.DEFAULT, overlay.style)

        val newStyle = NumberOverlayStyles.HIGH_CONTRAST
        overlay.setStyle(newStyle)

        assertEquals(newStyle, overlay.style)
    }

    @Test
    fun `style change affects badge color calculation`() {
        val overlay = createOverlay()
        val item = NumberedItem(1, "Button", Rect.EMPTY, isEnabled = true, hasName = true)

        val originalColor = overlay.getBadgeColor(item)
        assertEquals(NumberOverlayStyles.DEFAULT.hasNameColor, originalColor)

        overlay.setStyle(NumberOverlayStyles.COLORBLIND_FRIENDLY)

        val newColor = overlay.getBadgeColor(item)
        assertEquals(NumberOverlayStyles.COLORBLIND_FRIENDLY.hasNameColor, newColor)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Label Display Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `shouldShowLabel returns true for item with name`() {
        val overlay = createOverlay()
        val item = NumberedItem(1, "Submit Button", Rect.EMPTY, hasName = true)

        assertTrue(overlay.shouldShowLabel(item))
    }

    @Test
    fun `shouldShowLabel returns false for item without name`() {
        val overlay = createOverlay()
        val item = NumberedItem(1, "", Rect.EMPTY, hasName = false)

        assertFalse(overlay.shouldShowLabel(item))
    }

    @Test
    fun `shouldShowLabel returns false for empty label even if hasName is true`() {
        val overlay = createOverlay()
        val item = NumberedItem(1, "", Rect.EMPTY, hasName = true)

        assertFalse(overlay.shouldShowLabel(item))
    }

    @Test
    fun `getTruncatedLabel truncates long labels`() {
        val overlay = createOverlay()
        val longLabel = "This is a very long label that should be truncated"

        val truncated = overlay.getTruncatedLabel(longLabel, maxLength = 20)

        assertEquals(20, truncated.length)
        assertTrue(truncated.endsWith("..."))
    }

    @Test
    fun `getTruncatedLabel returns original for short labels`() {
        val overlay = createOverlay()
        val shortLabel = "Short"

        val result = overlay.getTruncatedLabel(shortLabel, maxLength = 20)

        assertEquals("Short", result)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Accessibility Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `getAnnouncementText returns correct text for items`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())

        val announcement = overlay.getAnnouncementText()

        assertTrue(announcement.contains("4 items"))
        assertTrue(announcement.contains("number"))
    }

    @Test
    fun `getAnnouncementText handles single item`() {
        val overlay = createOverlay()
        overlay.showItems(listOf(NumberedItem(1, "Only", Rect.EMPTY)))

        val announcement = overlay.getAnnouncementText()

        assertTrue(announcement.contains("1 item"))
    }

    @Test
    fun `getAnnouncementText handles empty items`() {
        val overlay = createOverlay()

        val announcement = overlay.getAnnouncementText()

        assertTrue(announcement.contains("No items") || announcement.isEmpty())
    }

    @Test
    fun `getItemDescription returns descriptive text`() {
        val overlay = createOverlay()
        val item = NumberedItem(1, "Submit Button", Rect(100, 200, 300, 250))

        val description = overlay.getItemDescription(item)

        assertTrue(description.contains("1"))
        assertTrue(description.contains("Submit Button"))
    }

    @Test
    fun `getItemDescription handles item without name`() {
        val overlay = createOverlay()
        val item = NumberedItem(5, "", Rect.EMPTY, hasName = false)

        val description = overlay.getItemDescription(item)

        assertTrue(description.contains("5"))
        assertTrue(description.contains("item") || description.contains("element"))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Edge Cases
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `showItems with empty list shows overlay with no items`() {
        val overlay = createOverlay()

        overlay.showItems(emptyList())

        assertTrue(overlay.isVisible)
        assertTrue(overlay.items.isEmpty())
    }

    @Test
    fun `calculateBadgePosition handles zero-size bounds`() {
        val overlay = createOverlay()
        val bounds = Rect.EMPTY

        // Should not throw
        val position = overlay.calculateBadgePosition(bounds)

        assertNotNull(position)
    }

    @Test
    fun `calculateBadgePosition handles negative bounds`() {
        val overlay = createOverlay()
        val bounds = Rect(-100, -200, -50, -150)

        val position = overlay.calculateBadgePosition(bounds)

        assertNotNull(position)
    }

    @Test
    fun `selectItem handles number zero`() {
        val overlay = createOverlay()
        overlay.showItems(listOf(
            NumberedItem(0, "Zero", Rect.EMPTY),
            NumberedItem(1, "One", Rect.EMPTY)
        ))

        val result = overlay.selectItem(0)

        assertNotNull(result)
        assertEquals(0, result.number)
    }

    @Test
    fun `selectItem handles negative numbers`() {
        val overlay = createOverlay()
        overlay.showItems(createTestItems())

        val result = overlay.selectItem(-1)

        assertNull(result)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Instruction Text Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `getInstructionText returns default instruction`() {
        val overlay = createOverlay()

        val instruction = overlay.getInstructionText()

        assertTrue(instruction.isNotEmpty())
        assertTrue(instruction.contains("number") || instruction.contains("select"))
    }

    @Test
    fun `getInstructionText can be customized`() {
        val overlay = NumberedSelectionOverlay(
            instructionText = "Custom instruction text"
        )

        val instruction = overlay.getInstructionText()

        assertEquals("Custom instruction text", instruction)
    }
}

// Note: Position class is defined in NumberedSelectionOverlay.kt
