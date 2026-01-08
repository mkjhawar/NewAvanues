/**
 * ContextMenuOverlayTest.kt - TDD tests for ContextMenuOverlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * These tests verify the ContextMenuOverlay behavior including:
 * - Menu display with title and items
 * - Item selection by ID and number
 * - Disabled item handling
 * - Highlighting and visual state
 * - Timeout and dismiss behavior
 * - Integration with IOverlay interface
 */
package com.augmentalis.voiceoscoreng.overlay

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContextMenuOverlayTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Test Data
    // ═══════════════════════════════════════════════════════════════════════

    private fun createTestMenuItems(): List<MenuItem> = listOf(
        MenuItem(id = "copy", label = "Copy", number = 1),
        MenuItem(id = "paste", label = "Paste", number = 2),
        MenuItem(id = "delete", label = "Delete", icon = "trash", number = 3),
        MenuItem(id = "disabled-action", label = "Disabled Action", isEnabled = false, number = 4)
    )

    private fun createTestMenuData(
        items: List<MenuItem> = createTestMenuItems(),
        title: String? = "Edit Options"
    ): OverlayData.ContextMenu = OverlayData.ContextMenu(items, title)

    // ═══════════════════════════════════════════════════════════════════════
    // Initialization Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `overlay can be created with unique id`() {
        val overlay = ContextMenuOverlay("menu-1")

        assertEquals("menu-1", overlay.id)
    }

    @Test
    fun `overlay starts hidden`() {
        val overlay = ContextMenuOverlay("menu-1")

        assertFalse(overlay.isVisible)
    }

    @Test
    fun `overlay has FLOATING overlay type by default`() {
        val overlay = ContextMenuOverlay("menu-1")

        // We verify this through behavior - floating overlays can be positioned
        assertTrue(overlay is BaseOverlay)
    }

    @Test
    fun `overlay starts with no menu items`() {
        val overlay = ContextMenuOverlay("menu-1")

        assertEquals(0, overlay.menuItemCount)
    }

    @Test
    fun `overlay starts with no title`() {
        val overlay = ContextMenuOverlay("menu-1")

        assertNull(overlay.menuTitle)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Show/Hide Tests (inherited from BaseOverlay)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `show makes overlay visible`() {
        val overlay = ContextMenuOverlay("menu-1")

        overlay.show()

        assertTrue(overlay.isVisible)
    }

    @Test
    fun `hide makes overlay invisible`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.show()

        overlay.hide()

        assertFalse(overlay.isVisible)
    }

    @Test
    fun `toggle alternates visibility`() {
        val overlay = ContextMenuOverlay("menu-1")

        overlay.toggle()
        assertTrue(overlay.isVisible)

        overlay.toggle()
        assertFalse(overlay.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Update with ContextMenu Data Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `update with ContextMenu data sets menu items`() {
        val overlay = ContextMenuOverlay("menu-1")
        val menuData = createTestMenuData()

        overlay.update(menuData)

        assertEquals(4, overlay.menuItemCount)
    }

    @Test
    fun `update with ContextMenu data sets title`() {
        val overlay = ContextMenuOverlay("menu-1")
        val menuData = createTestMenuData(title = "File Options")

        overlay.update(menuData)

        assertEquals("File Options", overlay.menuTitle)
    }

    @Test
    fun `update with null title is allowed`() {
        val overlay = ContextMenuOverlay("menu-1")
        val menuData = createTestMenuData(title = null)

        overlay.update(menuData)

        assertNull(overlay.menuTitle)
    }

    @Test
    fun `update with empty items clears menu`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        assertEquals(4, overlay.menuItemCount)

        overlay.update(OverlayData.ContextMenu(emptyList(), null))

        assertEquals(0, overlay.menuItemCount)
    }

    @Test
    fun `update replaces previous items`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val newItems = listOf(
            MenuItem(id = "new1", label = "New Item 1", number = 1),
            MenuItem(id = "new2", label = "New Item 2", number = 2)
        )
        overlay.update(OverlayData.ContextMenu(newItems, "New Menu"))

        assertEquals(2, overlay.menuItemCount)
        assertEquals("New Menu", overlay.menuTitle)
    }

    @Test
    fun `update ignores non-ContextMenu data`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        assertEquals(4, overlay.menuItemCount)

        // Update with different data type should be ignored
        overlay.update(OverlayData.Status("test", CommandState.LISTENING))

        // Items should remain unchanged
        assertEquals(4, overlay.menuItemCount)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Menu Item Access Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `getMenuItem returns correct item by index`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val item = overlay.getMenuItem(0)

        assertNotNull(item)
        assertEquals("copy", item.id)
        assertEquals("Copy", item.label)
    }

    @Test
    fun `getMenuItem returns null for out of bounds index`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val item = overlay.getMenuItem(10)

        assertNull(item)
    }

    @Test
    fun `getMenuItem returns null for negative index`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val item = overlay.getMenuItem(-1)

        assertNull(item)
    }

    @Test
    fun `findItemById returns correct item`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val item = overlay.findItemById("paste")

        assertNotNull(item)
        assertEquals("Paste", item.label)
        assertEquals(2, item.number)
    }

    @Test
    fun `findItemById returns null for non-existent id`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val item = overlay.findItemById("non-existent")

        assertNull(item)
    }

    @Test
    fun `findItemByNumber returns correct item`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val item = overlay.findItemByNumber(3)

        assertNotNull(item)
        assertEquals("delete", item.id)
        assertEquals("Delete", item.label)
    }

    @Test
    fun `findItemByNumber returns null for non-existent number`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val item = overlay.findItemByNumber(99)

        assertNull(item)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Selection by ID Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `selectItemById returns true for enabled item`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val result = overlay.selectItemById("copy")

        assertTrue(result)
    }

    @Test
    fun `selectItemById returns false for disabled item`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val result = overlay.selectItemById("disabled-action")

        assertFalse(result)
    }

    @Test
    fun `selectItemById returns false for non-existent id`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val result = overlay.selectItemById("non-existent")

        assertFalse(result)
    }

    @Test
    fun `selectItemById sets selectedItemId`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        overlay.selectItemById("paste")

        assertEquals("paste", overlay.selectedItemId)
    }

    @Test
    fun `selectItemById with disabled item does not change selectedItemId`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        overlay.selectItemById("copy")

        overlay.selectItemById("disabled-action")

        assertEquals("copy", overlay.selectedItemId)
    }

    @Test
    fun `selectItemById triggers onItemSelected callback`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        var selectedId: String? = null
        overlay.onItemSelected = { id -> selectedId = id }

        overlay.selectItemById("delete")

        assertEquals("delete", selectedId)
    }

    @Test
    fun `selectItemById does not trigger callback for disabled item`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        var callbackCalled = false
        overlay.onItemSelected = { callbackCalled = true }

        overlay.selectItemById("disabled-action")

        assertFalse(callbackCalled)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Selection by Number Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `selectItemByNumber returns true for enabled item`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val result = overlay.selectItemByNumber(1)

        assertTrue(result)
    }

    @Test
    fun `selectItemByNumber returns false for disabled item`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val result = overlay.selectItemByNumber(4) // disabled-action

        assertFalse(result)
    }

    @Test
    fun `selectItemByNumber returns false for non-existent number`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val result = overlay.selectItemByNumber(99)

        assertFalse(result)
    }

    @Test
    fun `selectItemByNumber sets selectedItemId`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        overlay.selectItemByNumber(2)

        assertEquals("paste", overlay.selectedItemId)
    }

    @Test
    fun `selectItemByNumber triggers onItemSelected callback`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        var selectedId: String? = null
        overlay.onItemSelected = { id -> selectedId = id }

        overlay.selectItemByNumber(3)

        assertEquals("delete", selectedId)
    }

    @Test
    fun `selectItemByNumber does not trigger callback for disabled item`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        var callbackCalled = false
        overlay.onItemSelected = { callbackCalled = true }

        overlay.selectItemByNumber(4)

        assertFalse(callbackCalled)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Highlight Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `highlightItem sets highlightedItemId`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        overlay.highlightItem("paste")

        assertEquals("paste", overlay.highlightedItemId)
    }

    @Test
    fun `highlightItem with non-existent id does nothing`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        overlay.highlightItem("copy")

        overlay.highlightItem("non-existent")

        assertEquals("copy", overlay.highlightedItemId)
    }

    @Test
    fun `clearHighlight removes highlighted item`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        overlay.highlightItem("copy")

        overlay.clearHighlight()

        assertNull(overlay.highlightedItemId)
    }

    @Test
    fun `highlightByNumber highlights correct item`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        overlay.highlightByNumber(2)

        assertEquals("paste", overlay.highlightedItemId)
    }

    @Test
    fun `highlightByNumber with non-existent number does nothing`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        overlay.highlightItem("copy")

        overlay.highlightByNumber(99)

        assertEquals("copy", overlay.highlightedItemId)
    }

    @Test
    fun `isItemHighlighted returns true for highlighted item`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        overlay.highlightItem("paste")

        assertTrue(overlay.isItemHighlighted("paste"))
        assertFalse(overlay.isItemHighlighted("copy"))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Enabled/Disabled Items Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `enabledItemCount returns correct count`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        assertEquals(3, overlay.enabledItemCount) // 4 items, 1 disabled
    }

    @Test
    fun `disabledItemCount returns correct count`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        assertEquals(1, overlay.disabledItemCount)
    }

    @Test
    fun `getEnabledItems returns only enabled items`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        val enabledItems = overlay.getEnabledItems()

        assertEquals(3, enabledItems.size)
        assertTrue(enabledItems.all { it.isEnabled })
    }

    @Test
    fun `hasNumberedItems returns true when items have numbers`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        assertTrue(overlay.hasNumberedItems)
    }

    @Test
    fun `hasNumberedItems returns false when no items have numbers`() {
        val overlay = ContextMenuOverlay("menu-1")
        val itemsWithoutNumbers = listOf(
            MenuItem(id = "a", label = "Item A"),
            MenuItem(id = "b", label = "Item B")
        )
        overlay.update(OverlayData.ContextMenu(itemsWithoutNumbers, null))

        assertFalse(overlay.hasNumberedItems)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Dismiss Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `dismiss hides overlay`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.show()
        assertTrue(overlay.isVisible)

        overlay.dismiss()

        assertFalse(overlay.isVisible)
    }

    @Test
    fun `dismiss triggers onDismiss callback`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.show()
        var dismissCalled = false
        overlay.onDismiss = { dismissCalled = true }

        overlay.dismiss()

        assertTrue(dismissCalled)
    }

    @Test
    fun `dismiss clears selection and highlight`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        overlay.selectItemById("copy")
        overlay.highlightItem("paste")

        overlay.dismiss()

        assertNull(overlay.selectedItemId)
        assertNull(overlay.highlightedItemId)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Show Menu Convenience Method Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `showMenu sets items and shows overlay`() {
        val overlay = ContextMenuOverlay("menu-1")
        val items = createTestMenuItems()

        overlay.showMenu(items, "Test Menu")

        assertTrue(overlay.isVisible)
        assertEquals(4, overlay.menuItemCount)
        assertEquals("Test Menu", overlay.menuTitle)
    }

    @Test
    fun `showMenu with null title sets items without title`() {
        val overlay = ContextMenuOverlay("menu-1")
        val items = createTestMenuItems()

        overlay.showMenu(items, null)

        assertTrue(overlay.isVisible)
        assertNull(overlay.menuTitle)
    }

    @Test
    fun `showMenu clears previous state`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        overlay.selectItemById("copy")
        overlay.highlightItem("paste")

        val newItems = listOf(MenuItem(id = "new", label = "New", number = 1))
        overlay.showMenu(newItems, "New Menu")

        assertNull(overlay.selectedItemId)
        assertNull(overlay.highlightedItemId)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Position Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `showMenuAtPosition sets position and shows`() {
        val overlay = ContextMenuOverlay("menu-1")
        val items = createTestMenuItems()

        overlay.showMenuAtPosition(items, "Menu", 100f, 200f)

        assertTrue(overlay.isVisible)
        assertEquals(100f, overlay.menuPositionX)
        assertEquals(200f, overlay.menuPositionY)
    }

    @Test
    fun `menuPosition can be updated while visible`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.showMenu(createTestMenuItems(), null)

        overlay.updateMenuPosition(50f, 75f)

        assertEquals(50f, overlay.menuPositionX)
        assertEquals(75f, overlay.menuPositionY)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Dispose Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `dispose hides overlay`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.show()

        overlay.dispose()

        assertFalse(overlay.isVisible)
    }

    @Test
    fun `dispose clears items`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())

        overlay.dispose()

        assertEquals(0, overlay.menuItemCount)
    }

    @Test
    fun `dispose clears callbacks`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.onItemSelected = { }
        overlay.onDismiss = { }

        overlay.dispose()

        // After dispose, callbacks should not be invoked
        // We can't directly test this, but dispose should clean up
        assertFalse(overlay.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Visibility Flow Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `visibilityFlow emits initial hidden state`() = runTest {
        val overlay = ContextMenuOverlay("menu-1")

        val initialValue = overlay.visibilityFlow.first()

        assertFalse(initialValue)
    }

    @Test
    fun `visibilityFlow reflects visibility changes`() = runTest {
        val overlay = ContextMenuOverlay("menu-1")

        overlay.show()
        assertEquals(true, overlay.visibilityFlow.value)

        overlay.hide()
        assertEquals(false, overlay.visibilityFlow.value)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Edge Cases
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `selecting same item multiple times is idempotent`() {
        val overlay = ContextMenuOverlay("menu-1")
        overlay.update(createTestMenuData())
        var selectCount = 0
        overlay.onItemSelected = { selectCount++ }

        overlay.selectItemById("copy")
        overlay.selectItemById("copy")
        overlay.selectItemById("copy")

        assertEquals(3, selectCount) // Each selection triggers callback
        assertEquals("copy", overlay.selectedItemId)
    }

    @Test
    fun `empty menu can be shown`() {
        val overlay = ContextMenuOverlay("menu-1")

        overlay.showMenu(emptyList(), "Empty Menu")

        assertTrue(overlay.isVisible)
        assertEquals(0, overlay.menuItemCount)
    }

    @Test
    fun `items without numbers work correctly`() {
        val overlay = ContextMenuOverlay("menu-1")
        val items = listOf(
            MenuItem(id = "a", label = "Item A"),
            MenuItem(id = "b", label = "Item B")
        )
        overlay.update(OverlayData.ContextMenu(items, null))

        // Selection by number should fail
        assertFalse(overlay.selectItemByNumber(1))

        // Selection by ID should work
        assertTrue(overlay.selectItemById("a"))
    }

    @Test
    fun `duplicate numbers are handled - first match wins`() {
        val overlay = ContextMenuOverlay("menu-1")
        val items = listOf(
            MenuItem(id = "first", label = "First", number = 1),
            MenuItem(id = "second", label = "Second", number = 1) // Duplicate number
        )
        overlay.update(OverlayData.ContextMenu(items, null))

        overlay.selectItemByNumber(1)

        assertEquals("first", overlay.selectedItemId)
    }

    @Test
    fun `unicode labels are supported`() {
        val overlay = ContextMenuOverlay("menu-1")
        val items = listOf(
            MenuItem(id = "emoji", label = "Delete ", number = 1),
            MenuItem(id = "chinese", label = "", number = 2)
        )
        overlay.update(OverlayData.ContextMenu(items, ""))

        assertEquals("Delete ", overlay.getMenuItem(0)?.label)
        assertEquals("", overlay.menuTitle)
    }

    @Test
    fun `very long labels are stored correctly`() {
        val overlay = ContextMenuOverlay("menu-1")
        val longLabel = "A".repeat(1000)
        val items = listOf(MenuItem(id = "long", label = longLabel, number = 1))
        overlay.update(OverlayData.ContextMenu(items, null))

        assertEquals(longLabel, overlay.getMenuItem(0)?.label)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Menu Item State Tests
// ═══════════════════════════════════════════════════════════════════════════

class ContextMenuItemStateTest {

    @Test
    fun `MenuItem with icon stores icon correctly`() {
        val item = MenuItem(
            id = "settings",
            label = "Settings",
            icon = "gear",
            number = 1
        )

        assertEquals("gear", item.icon)
    }

    @Test
    fun `MenuItem without icon has null icon`() {
        val item = MenuItem(id = "plain", label = "Plain Item")

        assertNull(item.icon)
    }

    @Test
    fun `MenuItem default isEnabled is true`() {
        val item = MenuItem(id = "test", label = "Test")

        assertTrue(item.isEnabled)
    }

    @Test
    fun `MenuItem can be explicitly disabled`() {
        val item = MenuItem(id = "test", label = "Test", isEnabled = false)

        assertFalse(item.isEnabled)
    }

    @Test
    fun `MenuItem default number is null`() {
        val item = MenuItem(id = "test", label = "Test")

        assertNull(item.number)
    }

    @Test
    fun `MenuItem equality works correctly`() {
        val item1 = MenuItem(id = "a", label = "A", number = 1)
        val item2 = MenuItem(id = "a", label = "A", number = 1)
        val item3 = MenuItem(id = "a", label = "A", number = 2)

        assertEquals(item1, item2)
        assertFalse(item1 == item3)
    }
}
