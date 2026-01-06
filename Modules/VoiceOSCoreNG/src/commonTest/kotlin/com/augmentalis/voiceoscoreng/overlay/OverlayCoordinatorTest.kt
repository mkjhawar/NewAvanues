/**
 * OverlayCoordinatorTest.kt - TDD tests for OverlayCoordinator
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * These tests verify the OverlayCoordinator behavior including:
 * - Overlay registration and management
 * - Priority-based overlay coordination
 * - Mutual exclusivity enforcement
 * - Z-order management
 * - Conflict resolution
 * - Show overlay and auto-hide conflicting overlays
 */
package com.augmentalis.voiceoscoreng.overlay

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OverlayCoordinatorTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Test Helpers - Mock Overlay Implementation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Test overlay implementation for verification.
     */
    private class MockOverlay(
        override val id: String,
        val priority: OverlayPriority = OverlayPriority.NORMAL
    ) : IOverlay {
        private val _isVisible = MutableStateFlow(false)
        override val isVisible: Boolean get() = _isVisible.value

        val visibilityFlow: StateFlow<Boolean> = _isVisible.asStateFlow()

        var showCallCount = 0
            private set
        var hideCallCount = 0
            private set
        var disposeCallCount = 0
            private set
        var lastData: OverlayData? = null
            private set

        private var _isDisposed = false
        val isDisposed: Boolean get() = _isDisposed

        override fun show() {
            if (!_isDisposed) {
                _isVisible.value = true
                showCallCount++
            }
        }

        override fun hide() {
            if (_isVisible.value) {
                _isVisible.value = false
                hideCallCount++
            }
        }

        override fun update(data: OverlayData) {
            if (!_isDisposed) {
                lastData = data
            }
        }

        override fun dispose() {
            _isDisposed = true
            hide()
            disposeCallCount++
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Registration Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `coordinator can register an overlay`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test-overlay")

        coordinator.register(overlay)

        assertNotNull(coordinator.getOverlay("test-overlay"))
    }

    @Test
    fun `coordinator tracks registered overlay by id`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("my-overlay-id")

        coordinator.register(overlay)

        assertEquals(overlay, coordinator.getOverlay("my-overlay-id"))
    }

    @Test
    fun `coordinator can register multiple overlays`() {
        val coordinator = OverlayCoordinator()
        val overlay1 = MockOverlay("overlay-1")
        val overlay2 = MockOverlay("overlay-2")
        val overlay3 = MockOverlay("overlay-3")

        coordinator.register(overlay1)
        coordinator.register(overlay2)
        coordinator.register(overlay3)

        assertEquals(3, coordinator.registeredCount)
        assertNotNull(coordinator.getOverlay("overlay-1"))
        assertNotNull(coordinator.getOverlay("overlay-2"))
        assertNotNull(coordinator.getOverlay("overlay-3"))
    }

    @Test
    fun `coordinator prevents duplicate registration`() {
        val coordinator = OverlayCoordinator()
        val overlay1 = MockOverlay("same-id")
        val overlay2 = MockOverlay("same-id")

        coordinator.register(overlay1)
        coordinator.register(overlay2)

        // Second registration should be ignored or replace the first
        assertEquals(1, coordinator.registeredCount)
    }

    @Test
    fun `coordinator can unregister an overlay`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("to-remove")

        coordinator.register(overlay)
        assertTrue(coordinator.unregister("to-remove"))

        assertNull(coordinator.getOverlay("to-remove"))
        assertEquals(0, coordinator.registeredCount)
    }

    @Test
    fun `unregister returns false for non-existent overlay`() {
        val coordinator = OverlayCoordinator()

        assertFalse(coordinator.unregister("non-existent"))
    }

    @Test
    fun `unregister hides overlay before removing`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("visible-overlay")

        coordinator.register(overlay)
        coordinator.show("visible-overlay")
        assertTrue(overlay.isVisible)

        coordinator.unregister("visible-overlay")

        assertFalse(overlay.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Show/Hide Basic Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `show makes registered overlay visible`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)
        coordinator.show("test")

        assertTrue(overlay.isVisible)
    }

    @Test
    fun `show returns true on success`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)
        val result = coordinator.show("test")

        assertTrue(result)
    }

    @Test
    fun `show returns false for non-existent overlay`() {
        val coordinator = OverlayCoordinator()

        val result = coordinator.show("non-existent")

        assertFalse(result)
    }

    @Test
    fun `hide makes overlay invisible`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)
        coordinator.show("test")
        coordinator.hide("test")

        assertFalse(overlay.isVisible)
    }

    @Test
    fun `hide returns true on success`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)
        coordinator.show("test")
        val result = coordinator.hide("test")

        assertTrue(result)
    }

    @Test
    fun `hide returns false for non-existent overlay`() {
        val coordinator = OverlayCoordinator()

        val result = coordinator.hide("non-existent")

        assertFalse(result)
    }

    @Test
    fun `hideAll hides all visible overlays`() {
        val coordinator = OverlayCoordinator()
        val overlay1 = MockOverlay("overlay-1")
        val overlay2 = MockOverlay("overlay-2")
        val overlay3 = MockOverlay("overlay-3")

        coordinator.register(overlay1)
        coordinator.register(overlay2)
        coordinator.register(overlay3)

        coordinator.show("overlay-1")
        coordinator.show("overlay-2")
        coordinator.show("overlay-3")

        coordinator.hideAll()

        assertFalse(overlay1.isVisible)
        assertFalse(overlay2.isVisible)
        assertFalse(overlay3.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Priority Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `overlay priorities are defined in correct order`() {
        // Verify priority order: STATUS > CONFIDENCE > NUMBERED > MENU > NORMAL
        assertTrue(OverlayPriority.STATUS.ordinal < OverlayPriority.CONFIDENCE.ordinal)
        assertTrue(OverlayPriority.CONFIDENCE.ordinal < OverlayPriority.NUMBERED.ordinal)
        assertTrue(OverlayPriority.NUMBERED.ordinal < OverlayPriority.MENU.ordinal)
        assertTrue(OverlayPriority.MENU.ordinal < OverlayPriority.NORMAL.ordinal)
    }

    @Test
    fun `register with priority sets overlay priority`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("status-overlay", OverlayPriority.STATUS)

        coordinator.register(overlay, OverlayPriority.STATUS)

        assertEquals(OverlayPriority.STATUS, coordinator.getPriority("status-overlay"))
    }

    @Test
    fun `higher priority overlay can show when lower priority is visible`() {
        val coordinator = OverlayCoordinator()
        val lowPriority = MockOverlay("menu", OverlayPriority.MENU)
        val highPriority = MockOverlay("status", OverlayPriority.STATUS)

        coordinator.register(lowPriority, OverlayPriority.MENU)
        coordinator.register(highPriority, OverlayPriority.STATUS)

        coordinator.show("menu")
        assertTrue(lowPriority.isVisible)

        coordinator.show("status")
        assertTrue(highPriority.isVisible)
    }

    @Test
    fun `showing high priority overlay hides conflicting lower priority overlays`() {
        val coordinator = OverlayCoordinator()
        val lowPriority = MockOverlay("numbered", OverlayPriority.NUMBERED)
        val highPriority = MockOverlay("status", OverlayPriority.STATUS)

        // Set them as conflicting (same group)
        coordinator.register(lowPriority, OverlayPriority.NUMBERED, group = "main")
        coordinator.register(highPriority, OverlayPriority.STATUS, group = "main")

        coordinator.show("numbered")
        assertTrue(lowPriority.isVisible)

        coordinator.show("status")
        assertTrue(highPriority.isVisible)
        assertFalse(lowPriority.isVisible, "Lower priority overlay in same group should be hidden")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Mutual Exclusivity Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `overlays in same exclusion group are mutually exclusive`() {
        val coordinator = OverlayCoordinator()
        val overlay1 = MockOverlay("overlay-a")
        val overlay2 = MockOverlay("overlay-b")

        coordinator.register(overlay1, group = "exclusive-group")
        coordinator.register(overlay2, group = "exclusive-group")

        coordinator.show("overlay-a")
        assertTrue(overlay1.isVisible)

        coordinator.show("overlay-b")
        assertTrue(overlay2.isVisible)
        assertFalse(overlay1.isVisible, "First overlay should be hidden when second in same group shows")
    }

    @Test
    fun `overlays in different groups can be visible simultaneously`() {
        val coordinator = OverlayCoordinator()
        val overlay1 = MockOverlay("overlay-a")
        val overlay2 = MockOverlay("overlay-b")

        coordinator.register(overlay1, group = "group-1")
        coordinator.register(overlay2, group = "group-2")

        coordinator.show("overlay-a")
        coordinator.show("overlay-b")

        assertTrue(overlay1.isVisible, "Overlays in different groups can both be visible")
        assertTrue(overlay2.isVisible)
    }

    @Test
    fun `ungrouped overlays are not mutually exclusive`() {
        val coordinator = OverlayCoordinator()
        val overlay1 = MockOverlay("overlay-a")
        val overlay2 = MockOverlay("overlay-b")

        coordinator.register(overlay1) // No group
        coordinator.register(overlay2) // No group

        coordinator.show("overlay-a")
        coordinator.show("overlay-b")

        assertTrue(overlay1.isVisible)
        assertTrue(overlay2.isVisible)
    }

    @Test
    fun `three overlays in same group only show one at a time`() {
        val coordinator = OverlayCoordinator()
        val overlay1 = MockOverlay("a")
        val overlay2 = MockOverlay("b")
        val overlay3 = MockOverlay("c")

        coordinator.register(overlay1, group = "selection")
        coordinator.register(overlay2, group = "selection")
        coordinator.register(overlay3, group = "selection")

        coordinator.show("a")
        assertTrue(overlay1.isVisible)
        assertFalse(overlay2.isVisible)
        assertFalse(overlay3.isVisible)

        coordinator.show("b")
        assertFalse(overlay1.isVisible)
        assertTrue(overlay2.isVisible)
        assertFalse(overlay3.isVisible)

        coordinator.show("c")
        assertFalse(overlay1.isVisible)
        assertFalse(overlay2.isVisible)
        assertTrue(overlay3.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Z-Order Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `getVisibleOverlaysOrdered returns overlays in priority order`() {
        val coordinator = OverlayCoordinator()
        val lowOverlay = MockOverlay("low", OverlayPriority.NORMAL)
        val midOverlay = MockOverlay("mid", OverlayPriority.NUMBERED)
        val highOverlay = MockOverlay("high", OverlayPriority.STATUS)

        coordinator.register(lowOverlay, OverlayPriority.NORMAL)
        coordinator.register(midOverlay, OverlayPriority.NUMBERED)
        coordinator.register(highOverlay, OverlayPriority.STATUS)

        coordinator.show("low")
        coordinator.show("mid")
        coordinator.show("high")

        val ordered = coordinator.getVisibleOverlaysOrdered()

        assertEquals(3, ordered.size)
        assertEquals("high", ordered[0].id)
        assertEquals("mid", ordered[1].id)
        assertEquals("low", ordered[2].id)
    }

    @Test
    fun `getVisibleOverlaysOrdered returns empty list when none visible`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)
        // Don't show it

        val ordered = coordinator.getVisibleOverlaysOrdered()

        assertTrue(ordered.isEmpty())
    }

    @Test
    fun `getZOrder returns correct index for overlay`() {
        val coordinator = OverlayCoordinator()
        val lowOverlay = MockOverlay("low", OverlayPriority.NORMAL)
        val highOverlay = MockOverlay("high", OverlayPriority.STATUS)

        coordinator.register(lowOverlay, OverlayPriority.NORMAL)
        coordinator.register(highOverlay, OverlayPriority.STATUS)

        coordinator.show("low")
        coordinator.show("high")

        // High priority should have z-order 0 (top)
        assertEquals(0, coordinator.getZOrder("high"))
        // Low priority should have z-order 1
        assertEquals(1, coordinator.getZOrder("low"))
    }

    @Test
    fun `getZOrder returns -1 for hidden overlay`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)

        assertEquals(-1, coordinator.getZOrder("test"))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Active Overlay State Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `visibleCount returns correct number`() {
        val coordinator = OverlayCoordinator()
        val overlay1 = MockOverlay("a")
        val overlay2 = MockOverlay("b")
        val overlay3 = MockOverlay("c")

        coordinator.register(overlay1)
        coordinator.register(overlay2)
        coordinator.register(overlay3)

        assertEquals(0, coordinator.visibleCount)

        coordinator.show("a")
        assertEquals(1, coordinator.visibleCount)

        coordinator.show("b")
        assertEquals(2, coordinator.visibleCount)

        coordinator.hide("a")
        assertEquals(1, coordinator.visibleCount)
    }

    @Test
    fun `isAnyVisible returns true when at least one overlay is visible`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)

        assertFalse(coordinator.isAnyVisible)

        coordinator.show("test")
        assertTrue(coordinator.isAnyVisible)

        coordinator.hide("test")
        assertFalse(coordinator.isAnyVisible)
    }

    @Test
    fun `getTopOverlay returns highest priority visible overlay`() {
        val coordinator = OverlayCoordinator()
        val lowOverlay = MockOverlay("low", OverlayPriority.NORMAL)
        val highOverlay = MockOverlay("high", OverlayPriority.STATUS)

        coordinator.register(lowOverlay, OverlayPriority.NORMAL)
        coordinator.register(highOverlay, OverlayPriority.STATUS)

        coordinator.show("low")
        assertEquals(lowOverlay, coordinator.getTopOverlay())

        coordinator.show("high")
        assertEquals(highOverlay, coordinator.getTopOverlay())
    }

    @Test
    fun `getTopOverlay returns null when none visible`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)

        assertNull(coordinator.getTopOverlay())
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Update Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `update forwards data to overlay`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)

        val data = OverlayData.Status("Testing", CommandState.PROCESSING)
        coordinator.update("test", data)

        assertEquals(data, overlay.lastData)
    }

    @Test
    fun `update returns false for non-existent overlay`() {
        val coordinator = OverlayCoordinator()

        val result = coordinator.update("non-existent", OverlayData.Status("Test", CommandState.LISTENING))

        assertFalse(result)
    }

    @Test
    fun `showWithData shows and updates overlay`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)

        val data = OverlayData.Confidence(0.95f, "click button")
        coordinator.showWithData("test", data)

        assertTrue(overlay.isVisible)
        assertEquals(data, overlay.lastData)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Lifecycle Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `dispose disposes all overlays`() {
        val coordinator = OverlayCoordinator()
        val overlay1 = MockOverlay("a")
        val overlay2 = MockOverlay("b")

        coordinator.register(overlay1)
        coordinator.register(overlay2)

        coordinator.dispose()

        assertTrue(overlay1.isDisposed)
        assertTrue(overlay2.isDisposed)
    }

    @Test
    fun `dispose clears all registrations`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)
        coordinator.dispose()

        assertEquals(0, coordinator.registeredCount)
        assertNull(coordinator.getOverlay("test"))
    }

    @Test
    fun `operations fail gracefully after dispose`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)
        coordinator.dispose()

        // These should not throw and should return appropriate failure values
        assertFalse(coordinator.show("test"))
        assertFalse(coordinator.hide("test"))
        assertFalse(coordinator.unregister("test"))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Flow Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `visibleOverlaysFlow emits updates`() = runTest {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)

        // Initial state
        assertEquals(0, coordinator.visibleOverlaysFlow.value.size)

        coordinator.show("test")
        assertEquals(1, coordinator.visibleOverlaysFlow.value.size)

        coordinator.hide("test")
        assertEquals(0, coordinator.visibleOverlaysFlow.value.size)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Transition Callback Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `onOverlayShown callback is invoked`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")
        var shownId: String? = null

        coordinator.register(overlay)
        coordinator.onOverlayShown = { id -> shownId = id }

        coordinator.show("test")

        assertEquals("test", shownId)
    }

    @Test
    fun `onOverlayHidden callback is invoked`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")
        var hiddenId: String? = null

        coordinator.register(overlay)
        coordinator.onOverlayHidden = { id -> hiddenId = id }

        coordinator.show("test")
        coordinator.hide("test")

        assertEquals("test", hiddenId)
    }

    @Test
    fun `onConflictResolved callback is invoked when hiding conflicting overlay`() {
        val coordinator = OverlayCoordinator()
        val overlay1 = MockOverlay("a")
        val overlay2 = MockOverlay("b")
        var conflictInfo: Pair<String, String>? = null

        coordinator.register(overlay1, group = "exclusive")
        coordinator.register(overlay2, group = "exclusive")
        coordinator.onConflictResolved = { hidden, shown -> conflictInfo = hidden to shown }

        coordinator.show("a")
        coordinator.show("b")

        assertNotNull(conflictInfo)
        assertEquals("a", conflictInfo?.first)
        assertEquals("b", conflictInfo?.second)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Edge Cases
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `show same overlay twice does not duplicate callbacks`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)

        coordinator.show("test")
        coordinator.show("test")

        assertEquals(1, overlay.showCallCount)
    }

    @Test
    fun `hide same overlay twice does not duplicate callbacks`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)
        coordinator.show("test")

        coordinator.hide("test")
        coordinator.hide("test")

        assertEquals(1, overlay.hideCallCount)
    }

    @Test
    fun `empty coordinator handles operations gracefully`() {
        val coordinator = OverlayCoordinator()

        assertFalse(coordinator.show("any"))
        assertFalse(coordinator.hide("any"))
        assertTrue(coordinator.getVisibleOverlaysOrdered().isEmpty())
        assertNull(coordinator.getTopOverlay())
        assertEquals(0, coordinator.registeredCount)
        assertEquals(0, coordinator.visibleCount)
    }

    @Test
    fun `coordinator handles rapid show hide cycles`() {
        val coordinator = OverlayCoordinator()
        val overlay = MockOverlay("test")

        coordinator.register(overlay)

        repeat(100) {
            coordinator.show("test")
            coordinator.hide("test")
        }

        assertFalse(overlay.isVisible)
        assertEquals(100, overlay.showCallCount)
        assertEquals(100, overlay.hideCallCount)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// OverlayPriority Tests
// ═══════════════════════════════════════════════════════════════════════════

class OverlayPriorityTest {

    @Test
    fun `OverlayPriority has all expected values`() {
        val priorities = OverlayPriority.values()

        assertEquals(5, priorities.size)
        assertTrue(priorities.contains(OverlayPriority.STATUS))
        assertTrue(priorities.contains(OverlayPriority.CONFIDENCE))
        assertTrue(priorities.contains(OverlayPriority.NUMBERED))
        assertTrue(priorities.contains(OverlayPriority.MENU))
        assertTrue(priorities.contains(OverlayPriority.NORMAL))
    }

    @Test
    fun `OverlayPriority STATUS has highest priority`() {
        assertEquals(0, OverlayPriority.STATUS.ordinal)
    }

    @Test
    fun `OverlayPriority NORMAL has lowest priority`() {
        val priorities = OverlayPriority.values()
        assertEquals(priorities.size - 1, OverlayPriority.NORMAL.ordinal)
    }

    @Test
    fun `compareTo works correctly for priorities`() {
        assertTrue(OverlayPriority.STATUS < OverlayPriority.CONFIDENCE)
        assertTrue(OverlayPriority.CONFIDENCE < OverlayPriority.NUMBERED)
        assertTrue(OverlayPriority.NUMBERED < OverlayPriority.MENU)
        assertTrue(OverlayPriority.MENU < OverlayPriority.NORMAL)
    }
}
