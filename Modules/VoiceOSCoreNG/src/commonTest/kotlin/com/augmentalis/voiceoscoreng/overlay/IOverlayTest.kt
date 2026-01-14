/**
 * IOverlayTest.kt - TDD tests for IOverlay interface contract
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Tests define the behavioral contract for IOverlay implementations.
 * Written FIRST following TDD methodology (RED phase).
 */
package com.augmentalis.voiceoscoreng.overlay

import com.augmentalis.voiceoscoreng.features.OverlayTheme
import com.augmentalis.voiceoscoreng.features.OverlayData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD tests for IPositionedOverlay interface.
 *
 * Tests the interface contract with a mock implementation to verify:
 * - Identity properties (id, zIndex)
 * - Visibility state transitions (show/hide)
 * - Position updates
 * - Size updates
 * - Theme application
 * - Dispose cleanup
 */
class IOverlayTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Mock Implementation for Testing
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Mock implementation of IPositionedOverlay for testing contract behavior.
     */
    private class MockOverlay(
        override val id: String = "mock-overlay-1",
        override val zIndex: Int = 0
    ) : IPositionedOverlay {
        private var _isVisible: Boolean = false
        override val isVisible: Boolean get() = _isVisible

        private var _x: Int = 0
        private var _y: Int = 0
        val x: Int get() = _x
        val y: Int get() = _y

        private var _width: Int = 100
        private var _height: Int = 100
        val width: Int get() = _width
        val height: Int get() = _height

        private var _theme: OverlayTheme = OverlayTheme.DEFAULT
        val theme: OverlayTheme get() = _theme

        private var _isDisposed: Boolean = false
        val isDisposed: Boolean get() = _isDisposed

        private var _lastData: OverlayData? = null
        val lastData: OverlayData? get() = _lastData

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

        override fun updatePosition(x: Int, y: Int) {
            if (!_isDisposed) {
                _x = x
                _y = y
            }
        }

        override fun updateSize(width: Int, height: Int) {
            if (!_isDisposed) {
                _width = width
                _height = height
            }
        }

        override fun setTheme(theme: OverlayTheme) {
            if (!_isDisposed) {
                _theme = theme
            }
        }

        override fun dispose() {
            _isDisposed = true
            _isVisible = false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Identity Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `overlay has unique id`() {
        val overlay1 = MockOverlay(id = "overlay-1")
        val overlay2 = MockOverlay(id = "overlay-2")

        assertEquals("overlay-1", overlay1.id)
        assertEquals("overlay-2", overlay2.id)
    }

    @Test
    fun `overlay id is stable across state changes`() {
        val overlay = MockOverlay(id = "stable-id")

        overlay.show()
        assertEquals("stable-id", overlay.id)

        overlay.hide()
        assertEquals("stable-id", overlay.id)

        overlay.updatePosition(100, 200)
        assertEquals("stable-id", overlay.id)

        overlay.dispose()
        assertEquals("stable-id", overlay.id)
    }

    @Test
    fun `overlay has zIndex for layering`() {
        val backOverlay = MockOverlay(id = "back", zIndex = 0)
        val frontOverlay = MockOverlay(id = "front", zIndex = 100)

        assertEquals(0, backOverlay.zIndex)
        assertEquals(100, frontOverlay.zIndex)
    }

    @Test
    fun `zIndex is stable`() {
        val overlay = MockOverlay(id = "test", zIndex = 50)

        overlay.show()
        assertEquals(50, overlay.zIndex)

        overlay.hide()
        assertEquals(50, overlay.zIndex)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Show/Hide State Transition Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `overlay starts hidden`() {
        val overlay = MockOverlay()

        assertFalse(overlay.isVisible, "Overlay should start hidden")
    }

    @Test
    fun `show makes overlay visible`() {
        val overlay = MockOverlay()

        overlay.show()

        assertTrue(overlay.isVisible, "Overlay should be visible after show()")
    }

    @Test
    fun `hide makes overlay invisible`() {
        val overlay = MockOverlay()
        overlay.show()
        assertTrue(overlay.isVisible)

        overlay.hide()

        assertFalse(overlay.isVisible, "Overlay should be hidden after hide()")
    }

    @Test
    fun `show after hide makes overlay visible again`() {
        val overlay = MockOverlay()

        overlay.show()
        overlay.hide()
        overlay.show()

        assertTrue(overlay.isVisible, "Overlay should be visible after show-hide-show cycle")
    }

    @Test
    fun `multiple show calls are idempotent`() {
        val overlay = MockOverlay()

        overlay.show()
        overlay.show()
        overlay.show()

        assertTrue(overlay.isVisible)
    }

    @Test
    fun `multiple hide calls are idempotent`() {
        val overlay = MockOverlay()
        overlay.show()

        overlay.hide()
        overlay.hide()
        overlay.hide()

        assertFalse(overlay.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Position Update Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `updatePosition changes overlay position`() {
        val overlay = MockOverlay()

        overlay.updatePosition(150, 250)

        assertEquals(150, overlay.x)
        assertEquals(250, overlay.y)
    }

    @Test
    fun `updatePosition accepts zero coordinates`() {
        val overlay = MockOverlay()
        overlay.updatePosition(100, 100)

        overlay.updatePosition(0, 0)

        assertEquals(0, overlay.x)
        assertEquals(0, overlay.y)
    }

    @Test
    fun `updatePosition accepts negative coordinates`() {
        val overlay = MockOverlay()

        overlay.updatePosition(-50, -100)

        assertEquals(-50, overlay.x)
        assertEquals(-100, overlay.y)
    }

    @Test
    fun `position updates are independent`() {
        val overlay = MockOverlay()

        overlay.updatePosition(10, 20)
        overlay.updatePosition(30, 20)

        assertEquals(30, overlay.x)
        assertEquals(20, overlay.y)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Size Update Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `updateSize changes overlay dimensions`() {
        val overlay = MockOverlay()

        overlay.updateSize(300, 400)

        assertEquals(300, overlay.width)
        assertEquals(400, overlay.height)
    }

    @Test
    fun `updateSize accepts zero dimensions`() {
        val overlay = MockOverlay()

        overlay.updateSize(0, 0)

        assertEquals(0, overlay.width)
        assertEquals(0, overlay.height)
    }

    @Test
    fun `updateSize accepts large dimensions`() {
        val overlay = MockOverlay()

        overlay.updateSize(1920, 1080)

        assertEquals(1920, overlay.width)
        assertEquals(1080, overlay.height)
    }

    @Test
    fun `size updates are independent`() {
        val overlay = MockOverlay()

        overlay.updateSize(100, 200)
        overlay.updateSize(150, 200)

        assertEquals(150, overlay.width)
        assertEquals(200, overlay.height)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Theme Application Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `setTheme applies default theme`() {
        val overlay = MockOverlay()
        val defaultTheme = OverlayTheme.DEFAULT

        overlay.setTheme(defaultTheme)

        assertEquals(defaultTheme, overlay.theme)
    }

    @Test
    fun `setTheme applies dark theme`() {
        val overlay = MockOverlay()
        val darkTheme = OverlayTheme.DARK

        overlay.setTheme(darkTheme)

        assertEquals(darkTheme, overlay.theme)
    }

    @Test
    fun `setTheme applies high contrast theme`() {
        val overlay = MockOverlay()
        val highContrastTheme = OverlayTheme.HIGH_CONTRAST

        overlay.setTheme(highContrastTheme)

        assertEquals(highContrastTheme, overlay.theme)
    }

    @Test
    fun `setTheme applies custom theme`() {
        val overlay = MockOverlay()
        val customTheme = OverlayTheme(
            primaryColor = 0xFFFF0000,
            backgroundColor = 0xFF00FF00
        )

        overlay.setTheme(customTheme)

        assertEquals(customTheme, overlay.theme)
        assertEquals(0xFFFF0000, overlay.theme.primaryColor)
        assertEquals(0xFF00FF00, overlay.theme.backgroundColor)
    }

    @Test
    fun `setTheme can be called multiple times`() {
        val overlay = MockOverlay()

        overlay.setTheme(OverlayTheme.DEFAULT)
        overlay.setTheme(OverlayTheme.DARK)
        overlay.setTheme(OverlayTheme.HIGH_CONTRAST)

        assertEquals(OverlayTheme.HIGH_CONTRAST, overlay.theme)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Dispose Cleanup Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `dispose hides overlay`() {
        val overlay = MockOverlay()
        overlay.show()
        assertTrue(overlay.isVisible)

        overlay.dispose()

        assertFalse(overlay.isVisible, "Overlay should be hidden after dispose()")
    }

    @Test
    fun `dispose marks overlay as disposed`() {
        val overlay = MockOverlay()

        overlay.dispose()

        assertTrue(overlay.isDisposed)
    }

    @Test
    fun `show does nothing after dispose`() {
        val overlay = MockOverlay()
        overlay.dispose()

        overlay.show()

        assertFalse(overlay.isVisible, "Show should have no effect after dispose")
    }

    @Test
    fun `updatePosition does nothing after dispose`() {
        val overlay = MockOverlay()
        overlay.updatePosition(10, 20)
        overlay.dispose()

        overlay.updatePosition(100, 200)

        // Position should remain at last value before dispose
        // (implementation choice: could be 0,0 or preserved)
        assertEquals(10, overlay.x)
        assertEquals(20, overlay.y)
    }

    @Test
    fun `updateSize does nothing after dispose`() {
        val overlay = MockOverlay()
        overlay.updateSize(300, 400)
        overlay.dispose()

        overlay.updateSize(500, 600)

        assertEquals(300, overlay.width)
        assertEquals(400, overlay.height)
    }

    @Test
    fun `setTheme does nothing after dispose`() {
        val overlay = MockOverlay()
        overlay.setTheme(OverlayTheme.DEFAULT)
        overlay.dispose()

        overlay.setTheme(OverlayTheme.DARK)

        assertEquals(OverlayTheme.DEFAULT, overlay.theme)
    }

    @Test
    fun `dispose can be called multiple times safely`() {
        val overlay = MockOverlay()

        overlay.dispose()
        overlay.dispose()
        overlay.dispose()

        assertTrue(overlay.isDisposed)
        assertFalse(overlay.isVisible)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Integration Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `full lifecycle - show update hide dispose`() {
        val overlay = MockOverlay(id = "lifecycle-test", zIndex = 10)

        // Initial state
        assertFalse(overlay.isVisible)
        assertEquals("lifecycle-test", overlay.id)
        assertEquals(10, overlay.zIndex)

        // Show
        overlay.show()
        assertTrue(overlay.isVisible)

        // Update position
        overlay.updatePosition(100, 200)
        assertEquals(100, overlay.x)
        assertEquals(200, overlay.y)

        // Update size
        overlay.updateSize(400, 300)
        assertEquals(400, overlay.width)
        assertEquals(300, overlay.height)

        // Set theme
        overlay.setTheme(OverlayTheme.DARK)
        assertEquals(OverlayTheme.DARK, overlay.theme)

        // Hide
        overlay.hide()
        assertFalse(overlay.isVisible)

        // Show again
        overlay.show()
        assertTrue(overlay.isVisible)

        // Dispose
        overlay.dispose()
        assertFalse(overlay.isVisible)
        assertTrue(overlay.isDisposed)
    }

    @Test
    fun `multiple overlays operate independently`() {
        val overlay1 = MockOverlay(id = "overlay-1", zIndex = 0)
        val overlay2 = MockOverlay(id = "overlay-2", zIndex = 10)

        overlay1.show()
        overlay1.updatePosition(10, 10)
        overlay1.setTheme(OverlayTheme.DEFAULT)

        overlay2.updatePosition(100, 100)
        overlay2.setTheme(OverlayTheme.DARK)

        // Verify independence
        assertTrue(overlay1.isVisible)
        assertFalse(overlay2.isVisible)

        assertEquals(10, overlay1.x)
        assertEquals(100, overlay2.x)

        assertEquals(OverlayTheme.DEFAULT, overlay1.theme)
        assertEquals(OverlayTheme.DARK, overlay2.theme)

        // Dispose one, other unaffected
        overlay1.dispose()
        assertTrue(overlay1.isDisposed)
        assertFalse(overlay2.isDisposed)
    }
}
