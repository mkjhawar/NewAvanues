/**
 * BaseOverlayTest.kt - TDD tests for BaseOverlay abstract class
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * These tests verify the BaseOverlay abstract class behavior including:
 * - Visibility management (show, hide, toggle)
 * - Lifecycle callbacks (onShow, onHide, onDispose)
 * - Position management for POSITIONED overlays
 * - Coroutine scope management
 * - StateFlow for reactive visibility updates
 */
package com.augmentalis.voiceoscoreng.overlay

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BaseOverlayTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Test Implementation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Concrete test implementation of BaseOverlay for testing.
     * Tracks callback invocations for verification.
     */
    private class TestOverlayImpl(
        id: String = "test-overlay",
        overlayType: OverlayType = OverlayType.FLOATING
    ) : BaseOverlay(id, overlayType) {

        var showCallCount = 0
            private set
        var hideCallCount = 0
            private set
        var disposeCallCount = 0
            private set
        var positionChangeCallCount = 0
            private set

        var lastPositionX: Float? = null
            private set
        var lastPositionY: Float? = null
            private set

        var lastData: OverlayData? = null
            private set

        override fun onShow() {
            showCallCount++
        }

        override fun onHide() {
            hideCallCount++
        }

        override fun onDispose() {
            disposeCallCount++
        }

        override fun onPositionChanged(x: Float, y: Float) {
            positionChangeCallCount++
            lastPositionX = x
            lastPositionY = y
        }

        override fun update(data: OverlayData) {
            lastData = data
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Initial State Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `overlay starts hidden`() {
        val overlay = TestOverlayImpl()

        assertFalse(overlay.isVisible, "Overlay should start hidden")
    }

    @Test
    fun `overlay has correct id`() {
        val overlay = TestOverlayImpl(id = "my-custom-id")

        assertEquals("my-custom-id", overlay.id)
    }

    @Test
    fun `overlay id is stable across operations`() {
        val overlay = TestOverlayImpl(id = "stable-id")

        overlay.show()
        assertEquals("stable-id", overlay.id)

        overlay.hide()
        assertEquals("stable-id", overlay.id)

        overlay.dispose()
        assertEquals("stable-id", overlay.id)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Show Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `show makes overlay visible`() {
        val overlay = TestOverlayImpl()

        overlay.show()

        assertTrue(overlay.isVisible, "Overlay should be visible after show()")
    }

    @Test
    fun `show triggers onShow callback`() {
        val overlay = TestOverlayImpl()

        overlay.show()

        assertEquals(1, overlay.showCallCount, "onShow should be called once")
    }

    @Test
    fun `show does not call onShow if already visible`() {
        val overlay = TestOverlayImpl()
        overlay.show()

        overlay.show() // Second call

        assertEquals(1, overlay.showCallCount, "onShow should only be called once")
    }

    @Test
    fun `multiple show calls keep overlay visible`() {
        val overlay = TestOverlayImpl()

        overlay.show()
        overlay.show()
        overlay.show()

        assertTrue(overlay.isVisible)
        assertEquals(1, overlay.showCallCount)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Hide Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `hide makes overlay invisible`() {
        val overlay = TestOverlayImpl()
        overlay.show()

        overlay.hide()

        assertFalse(overlay.isVisible, "Overlay should be hidden after hide()")
    }

    @Test
    fun `hide triggers onHide callback`() {
        val overlay = TestOverlayImpl()
        overlay.show()

        overlay.hide()

        assertEquals(1, overlay.hideCallCount, "onHide should be called once")
    }

    @Test
    fun `hide does not call onHide if already hidden`() {
        val overlay = TestOverlayImpl()
        // Already hidden, try to hide again

        overlay.hide()

        assertEquals(0, overlay.hideCallCount, "onHide should not be called if already hidden")
    }

    @Test
    fun `multiple hide calls on hidden overlay have no effect`() {
        val overlay = TestOverlayImpl()
        overlay.show()
        overlay.hide()

        overlay.hide() // Second hide
        overlay.hide() // Third hide

        assertEquals(1, overlay.hideCallCount)
        assertFalse(overlay.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Toggle Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `toggle shows hidden overlay`() {
        val overlay = TestOverlayImpl()

        overlay.toggle()

        assertTrue(overlay.isVisible, "Toggle should show hidden overlay")
    }

    @Test
    fun `toggle hides visible overlay`() {
        val overlay = TestOverlayImpl()
        overlay.show()

        overlay.toggle()

        assertFalse(overlay.isVisible, "Toggle should hide visible overlay")
    }

    @Test
    fun `multiple toggles alternate visibility`() {
        val overlay = TestOverlayImpl()

        overlay.toggle() // hidden -> visible
        assertTrue(overlay.isVisible)

        overlay.toggle() // visible -> hidden
        assertFalse(overlay.isVisible)

        overlay.toggle() // hidden -> visible
        assertTrue(overlay.isVisible)

        overlay.toggle() // visible -> hidden
        assertFalse(overlay.isVisible)
    }

    @Test
    fun `toggle triggers appropriate callbacks`() {
        val overlay = TestOverlayImpl()

        overlay.toggle() // Should call onShow
        assertEquals(1, overlay.showCallCount)
        assertEquals(0, overlay.hideCallCount)

        overlay.toggle() // Should call onHide
        assertEquals(1, overlay.showCallCount)
        assertEquals(1, overlay.hideCallCount)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Dispose Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `dispose triggers onDispose callback`() {
        val overlay = TestOverlayImpl()

        overlay.dispose()

        assertEquals(1, overlay.disposeCallCount, "onDispose should be called once")
    }

    @Test
    fun `dispose hides visible overlay`() {
        val overlay = TestOverlayImpl()
        overlay.show()

        overlay.dispose()

        assertFalse(overlay.isVisible, "Overlay should be hidden after dispose")
    }

    @Test
    fun `dispose calls onHide if overlay was visible`() {
        val overlay = TestOverlayImpl()
        overlay.show()

        overlay.dispose()

        assertEquals(1, overlay.hideCallCount, "onHide should be called during dispose if visible")
    }

    @Test
    fun `dispose does not call onHide if overlay was already hidden`() {
        val overlay = TestOverlayImpl()
        // Overlay starts hidden

        overlay.dispose()

        assertEquals(0, overlay.hideCallCount, "onHide should not be called if already hidden")
    }

    @Test
    fun `callback order is onHide then onDispose when disposing visible overlay`() {
        var callOrder = mutableListOf<String>()

        val overlay = object : BaseOverlay("order-test") {
            override fun onHide() {
                callOrder.add("onHide")
            }
            override fun onDispose() {
                callOrder.add("onDispose")
            }
            override fun update(data: OverlayData) {}
        }
        overlay.show()

        overlay.dispose()

        assertEquals(listOf("onHide", "onDispose"), callOrder)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Position Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `updatePosition stores position values`() {
        val overlay = TestOverlayImpl(overlayType = OverlayType.POSITIONED)
        overlay.show()

        overlay.updatePosition(100f, 200f)

        assertEquals(100f, overlay.lastPositionX)
        assertEquals(200f, overlay.lastPositionY)
    }

    @Test
    fun `updatePosition triggers onPositionChanged when visible`() {
        val overlay = TestOverlayImpl(overlayType = OverlayType.POSITIONED)
        overlay.show()

        overlay.updatePosition(50f, 75f)

        assertEquals(1, overlay.positionChangeCallCount)
    }

    @Test
    fun `updatePosition does not trigger callback when hidden`() {
        val overlay = TestOverlayImpl(overlayType = OverlayType.POSITIONED)
        // Overlay is hidden

        overlay.updatePosition(50f, 75f)

        assertEquals(0, overlay.positionChangeCallCount)
    }

    @Test
    fun `updatePosition multiple times calls callback each time when visible`() {
        val overlay = TestOverlayImpl(overlayType = OverlayType.POSITIONED)
        overlay.show()

        overlay.updatePosition(10f, 20f)
        overlay.updatePosition(30f, 40f)
        overlay.updatePosition(50f, 60f)

        assertEquals(3, overlay.positionChangeCallCount)
        assertEquals(50f, overlay.lastPositionX)
        assertEquals(60f, overlay.lastPositionY)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Visibility Flow Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `visibilityFlow emits initial hidden state`() = runTest {
        val overlay = TestOverlayImpl()

        val initialValue = overlay.visibilityFlow.first()

        assertFalse(initialValue)
    }

    @Test
    fun `visibilityFlow reflects current isVisible state`() = runTest {
        val overlay = TestOverlayImpl()

        overlay.show()
        assertEquals(true, overlay.visibilityFlow.value)

        overlay.hide()
        assertEquals(false, overlay.visibilityFlow.value)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Overlay Type Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `overlay can be FULLSCREEN type`() {
        val overlay = TestOverlayImpl(overlayType = OverlayType.FULLSCREEN)

        overlay.show()

        assertTrue(overlay.isVisible)
    }

    @Test
    fun `overlay can be FLOATING type`() {
        val overlay = TestOverlayImpl(overlayType = OverlayType.FLOATING)

        overlay.show()

        assertTrue(overlay.isVisible)
    }

    @Test
    fun `overlay can be POSITIONED type`() {
        val overlay = TestOverlayImpl(overlayType = OverlayType.POSITIONED)

        overlay.show()

        assertTrue(overlay.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Show/Hide Cycle Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `show hide cycle works correctly`() {
        val overlay = TestOverlayImpl()

        // Cycle 1
        overlay.show()
        assertTrue(overlay.isVisible)
        overlay.hide()
        assertFalse(overlay.isVisible)

        // Cycle 2
        overlay.show()
        assertTrue(overlay.isVisible)
        overlay.hide()
        assertFalse(overlay.isVisible)

        assertEquals(2, overlay.showCallCount)
        assertEquals(2, overlay.hideCallCount)
    }

    @Test
    fun `callbacks are invoked in correct sequence`() {
        val callSequence = mutableListOf<String>()

        val overlay = object : BaseOverlay("sequence-test") {
            override fun onShow() {
                callSequence.add("show")
            }
            override fun onHide() {
                callSequence.add("hide")
            }
            override fun onDispose() {
                callSequence.add("dispose")
            }
            override fun update(data: OverlayData) {}
        }

        overlay.show()
        overlay.hide()
        overlay.show()
        overlay.dispose()

        assertEquals(listOf("show", "hide", "show", "hide", "dispose"), callSequence)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Edge Cases
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `empty id is allowed`() {
        val overlay = TestOverlayImpl(id = "")

        assertEquals("", overlay.id)
    }

    @Test
    fun `unicode id is supported`() {
        val overlay = TestOverlayImpl(id = "overlay-")

        assertEquals("overlay-", overlay.id)
    }

    @Test
    fun `position values can be negative`() {
        val overlay = TestOverlayImpl(overlayType = OverlayType.POSITIONED)
        overlay.show()

        overlay.updatePosition(-100f, -200f)

        assertEquals(-100f, overlay.lastPositionX)
        assertEquals(-200f, overlay.lastPositionY)
    }

    @Test
    fun `position values can be zero`() {
        val overlay = TestOverlayImpl(overlayType = OverlayType.POSITIONED)
        overlay.show()

        overlay.updatePosition(0f, 0f)

        assertEquals(0f, overlay.lastPositionX)
        assertEquals(0f, overlay.lastPositionY)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// OverlayType Tests
// ═══════════════════════════════════════════════════════════════════════════

class OverlayTypeTest {

    @Test
    fun `OverlayType has all expected values`() {
        val types = OverlayType.values()

        assertEquals(3, types.size)
        assertTrue(types.contains(OverlayType.FULLSCREEN))
        assertTrue(types.contains(OverlayType.FLOATING))
        assertTrue(types.contains(OverlayType.POSITIONED))
    }

    @Test
    fun `OverlayType FULLSCREEN name is correct`() {
        assertEquals("FULLSCREEN", OverlayType.FULLSCREEN.name)
    }

    @Test
    fun `OverlayType FLOATING name is correct`() {
        assertEquals("FLOATING", OverlayType.FLOATING.name)
    }

    @Test
    fun `OverlayType POSITIONED name is correct`() {
        assertEquals("POSITIONED", OverlayType.POSITIONED.name)
    }

    @Test
    fun `OverlayType valueOf works correctly`() {
        assertEquals(OverlayType.FULLSCREEN, OverlayType.valueOf("FULLSCREEN"))
        assertEquals(OverlayType.FLOATING, OverlayType.valueOf("FLOATING"))
        assertEquals(OverlayType.POSITIONED, OverlayType.valueOf("POSITIONED"))
    }
}
