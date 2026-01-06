/**
 * IOverlayContractTest.kt - Contract tests for IOverlay implementations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * These tests define the behavioral contract that any IOverlay implementation
 * must satisfy. Use this as an abstract test class for platform implementations.
 */
package com.augmentalis.voiceoscoreng.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Contract tests for IOverlay interface.
 *
 * Any implementation of IOverlay should pass these tests.
 * Platform-specific implementations can extend this to verify compliance.
 */
class IOverlayContractTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Test Implementation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Simple test implementation for verifying contract behavior.
     */
    private class TestOverlay(
        override val id: String = "test-overlay-1"
    ) : IOverlay {
        private var _isVisible: Boolean = false
        override val isVisible: Boolean get() = _isVisible

        private var _lastData: OverlayData? = null
        val lastData: OverlayData? get() = _lastData

        private var _isDisposed: Boolean = false
        val isDisposed: Boolean get() = _isDisposed

        override fun show() {
            if (!_isDisposed) {
                _isVisible = true
            }
        }

        override fun hide() {
            _isVisible = false
        }

        override fun update(data: OverlayData) {
            if (!_isDisposed) {
                _lastData = data
            }
        }

        override fun dispose() {
            _isDisposed = true
            _isVisible = false
            _lastData = null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Visibility Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `show makes overlay visible`() {
        val overlay = TestOverlay()
        assertFalse(overlay.isVisible, "Overlay should start hidden")

        overlay.show()

        assertTrue(overlay.isVisible, "Overlay should be visible after show()")
    }

    @Test
    fun `hide makes overlay invisible`() {
        val overlay = TestOverlay()
        overlay.show()
        assertTrue(overlay.isVisible)

        overlay.hide()

        assertFalse(overlay.isVisible, "Overlay should be hidden after hide()")
    }

    @Test
    fun `toggle switches visibility from hidden to visible`() {
        val overlay = TestOverlay()
        assertFalse(overlay.isVisible)

        overlay.toggle()

        assertTrue(overlay.isVisible, "Toggle should make hidden overlay visible")
    }

    @Test
    fun `toggle switches visibility from visible to hidden`() {
        val overlay = TestOverlay()
        overlay.show()
        assertTrue(overlay.isVisible)

        overlay.toggle()

        assertFalse(overlay.isVisible, "Toggle should hide visible overlay")
    }

    @Test
    fun `multiple toggles alternate visibility state`() {
        val overlay = TestOverlay()
        assertFalse(overlay.isVisible)

        overlay.toggle() // hidden -> visible
        assertTrue(overlay.isVisible)

        overlay.toggle() // visible -> hidden
        assertFalse(overlay.isVisible)

        overlay.toggle() // hidden -> visible
        assertTrue(overlay.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Dispose Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `dispose makes overlay invisible`() {
        val overlay = TestOverlay()
        overlay.show()
        assertTrue(overlay.isVisible)

        overlay.dispose()

        assertFalse(overlay.isVisible, "Overlay should be hidden after dispose()")
    }

    @Test
    fun `dispose cleans up resources`() {
        val overlay = TestOverlay()
        overlay.update(OverlayData.Status("Test", CommandState.LISTENING))
        overlay.show()

        overlay.dispose()

        assertTrue(overlay.isDisposed, "Overlay should be disposed")
        assertFalse(overlay.isVisible, "Overlay should be hidden")
    }

    @Test
    fun `show does nothing after dispose`() {
        val overlay = TestOverlay()
        overlay.dispose()

        overlay.show()

        assertFalse(overlay.isVisible, "Show should have no effect after dispose")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Update Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `update changes overlay data with Status`() {
        val overlay = TestOverlay()
        val statusData = OverlayData.Status("Processing command...", CommandState.PROCESSING)

        overlay.update(statusData)

        assertEquals(statusData, overlay.lastData)
    }

    @Test
    fun `update changes overlay data with Confidence`() {
        val overlay = TestOverlay()
        val confidenceData = OverlayData.Confidence(0.95f, "click submit button")

        overlay.update(confidenceData)

        assertEquals(confidenceData, overlay.lastData)
    }

    @Test
    fun `update changes overlay data with NumberedItems`() {
        val overlay = TestOverlay()
        val items = listOf(
            NumberedItem(1, "Submit", Rect(100, 200, 300, 250)),
            NumberedItem(2, "Cancel", Rect(100, 260, 300, 310)),
            NumberedItem(3, "Help", Rect(100, 320, 300, 370))
        )
        val numberedData = OverlayData.NumberedItems(items)

        overlay.update(numberedData)

        assertEquals(numberedData, overlay.lastData)
    }

    @Test
    fun `update changes overlay data with ContextMenu`() {
        val overlay = TestOverlay()
        val menuItems = listOf(
            MenuItem("copy", "Copy", number = 1),
            MenuItem("paste", "Paste", number = 2),
            MenuItem("delete", "Delete", icon = "trash", number = 3)
        )
        val menuData = OverlayData.ContextMenu(menuItems, "Edit Options")

        overlay.update(menuData)

        assertEquals(menuData, overlay.lastData)
    }

    @Test
    fun `update does nothing after dispose`() {
        val overlay = TestOverlay()
        overlay.dispose()

        overlay.update(OverlayData.Status("Test", CommandState.LISTENING))

        assertEquals(null, overlay.lastData, "Update should have no effect after dispose")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Identity Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `overlay has unique id`() {
        val overlay1 = TestOverlay("overlay-1")
        val overlay2 = TestOverlay("overlay-2")

        assertEquals("overlay-1", overlay1.id)
        assertEquals("overlay-2", overlay2.id)
    }

    @Test
    fun `overlay id is stable`() {
        val overlay = TestOverlay("my-overlay")

        overlay.show()
        assertEquals("my-overlay", overlay.id)

        overlay.hide()
        assertEquals("my-overlay", overlay.id)

        overlay.dispose()
        assertEquals("my-overlay", overlay.id)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Data Model Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `Rect calculates width correctly`() {
        val rect = Rect(10, 20, 110, 70)
        assertEquals(100, rect.width)
    }

    @Test
    fun `Rect calculates height correctly`() {
        val rect = Rect(10, 20, 110, 70)
        assertEquals(50, rect.height)
    }

    @Test
    fun `Rect calculates centerX correctly`() {
        val rect = Rect(10, 20, 110, 70)
        assertEquals(60, rect.centerX)
    }

    @Test
    fun `Rect calculates centerY correctly`() {
        val rect = Rect(10, 20, 110, 70)
        assertEquals(45, rect.centerY)
    }

    @Test
    fun `NumberedItem has correct default values`() {
        val item = NumberedItem(1, "Button", Rect(0, 0, 100, 50))

        assertEquals(1, item.number)
        assertEquals("Button", item.label)
        assertTrue(item.isEnabled)
        assertTrue(item.hasName)
    }

    @Test
    fun `NumberedItem can be disabled`() {
        val item = NumberedItem(1, "Button", Rect(0, 0, 100, 50), isEnabled = false)

        assertFalse(item.isEnabled)
    }

    @Test
    fun `NumberedItem can have no name`() {
        val item = NumberedItem(1, "1", Rect(0, 0, 100, 50), hasName = false)

        assertFalse(item.hasName)
    }

    @Test
    fun `MenuItem has correct default values`() {
        val item = MenuItem("action-id", "Action Label")

        assertEquals("action-id", item.id)
        assertEquals("Action Label", item.label)
        assertEquals(null, item.icon)
        assertTrue(item.isEnabled)
        assertEquals(null, item.number)
    }

    @Test
    fun `MenuItem can have icon and number`() {
        val item = MenuItem(
            id = "settings",
            label = "Settings",
            icon = "gear",
            number = 5
        )

        assertEquals("gear", item.icon)
        assertEquals(5, item.number)
    }

    @Test
    fun `MenuItem can be disabled`() {
        val item = MenuItem("disabled-action", "Disabled", isEnabled = false)

        assertFalse(item.isEnabled)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CommandState Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `CommandState has all expected values`() {
        val states = CommandState.values()

        assertEquals(5, states.size)
        assertTrue(states.contains(CommandState.LISTENING))
        assertTrue(states.contains(CommandState.PROCESSING))
        assertTrue(states.contains(CommandState.EXECUTING))
        assertTrue(states.contains(CommandState.SUCCESS))
        assertTrue(states.contains(CommandState.ERROR))
    }

    @Test
    fun `Status overlay data captures state transitions`() {
        val overlay = TestOverlay()

        // Simulate command lifecycle
        overlay.update(OverlayData.Status("Listening...", CommandState.LISTENING))
        assertEquals(CommandState.LISTENING, (overlay.lastData as OverlayData.Status).state)

        overlay.update(OverlayData.Status("Processing...", CommandState.PROCESSING))
        assertEquals(CommandState.PROCESSING, (overlay.lastData as OverlayData.Status).state)

        overlay.update(OverlayData.Status("Click button", CommandState.EXECUTING))
        assertEquals(CommandState.EXECUTING, (overlay.lastData as OverlayData.Status).state)

        overlay.update(OverlayData.Status("Done!", CommandState.SUCCESS))
        assertEquals(CommandState.SUCCESS, (overlay.lastData as OverlayData.Status).state)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // OverlayData Sealed Class Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `OverlayData Status contains message and state`() {
        val data = OverlayData.Status("Command recognized", CommandState.SUCCESS)

        assertEquals("Command recognized", data.message)
        assertEquals(CommandState.SUCCESS, data.state)
    }

    @Test
    fun `OverlayData Confidence contains value and text`() {
        val data = OverlayData.Confidence(0.87f, "open settings")

        assertEquals(0.87f, data.value)
        assertEquals("open settings", data.text)
    }

    @Test
    fun `OverlayData NumberedItems contains list of items`() {
        val items = listOf(
            NumberedItem(1, "First", Rect(0, 0, 100, 50)),
            NumberedItem(2, "Second", Rect(0, 60, 100, 110))
        )
        val data = OverlayData.NumberedItems(items)

        assertEquals(2, data.items.size)
        assertEquals("First", data.items[0].label)
        assertEquals("Second", data.items[1].label)
    }

    @Test
    fun `OverlayData ContextMenu contains items and optional title`() {
        val items = listOf(MenuItem("a", "Option A"), MenuItem("b", "Option B"))
        val dataWithTitle = OverlayData.ContextMenu(items, "Choose Option")
        val dataWithoutTitle = OverlayData.ContextMenu(items, null)

        assertEquals("Choose Option", dataWithTitle.title)
        assertEquals(null, dataWithoutTitle.title)
        assertEquals(2, dataWithTitle.items.size)
    }
}
