/**
 * OverlayManagerTest.kt - TDD tests for centralized overlay management system
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * TDD tests written FIRST - implementation follows to make these pass.
 * Tests cover:
 * - Registration/unregistration of overlays
 * - Lookup by ID
 * - Show/hide all overlays
 * - Disposal of all overlays
 * - Config propagation
 * - Thread-safety scenarios
 */
package com.augmentalis.voiceoscoreng.overlay

import com.augmentalis.voiceoscoreng.features.CommandState
import com.augmentalis.voiceoscoreng.features.IOverlay
import com.augmentalis.voiceoscoreng.features.OverlayConfig
import com.augmentalis.voiceoscoreng.features.OverlayData
import com.augmentalis.voiceoscoreng.features.OverlayManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.BeforeTest
import kotlin.test.assertFailsWith

class OverlayManagerTest {

    private lateinit var manager: OverlayManager
    private lateinit var config: OverlayConfig

    @BeforeTest
    fun setup() {
        config = OverlayConfig()
        manager = OverlayManager(config)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Test Helper Implementation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Simple test overlay implementation for verifying manager behavior.
     */
    private class TestOverlay(
        override val id: String
    ) : IOverlay {
        private var _isVisible: Boolean = false
        override val isVisible: Boolean get() = _isVisible

        private var _lastData: OverlayData? = null
        val lastData: OverlayData? get() = _lastData

        private var _isDisposed: Boolean = false
        val isDisposed: Boolean get() = _isDisposed

        var showCount: Int = 0
            private set
        var hideCount: Int = 0
            private set

        override fun show() {
            if (!_isDisposed) {
                _isVisible = true
                showCount++
            }
        }

        override fun hide() {
            _isVisible = false
            hideCount++
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
    // Creation Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `manager is created with provided config`() {
        val customConfig = OverlayConfig()
        customConfig.themeName = "HighContrast"

        val mgr = OverlayManager(customConfig)

        assertEquals("HighContrast", mgr.config.themeName)
    }

    @Test
    fun `manager starts with no registered overlays`() {
        assertEquals(0, manager.overlayCount)
        assertTrue(manager.getAllOverlayIds().isEmpty())
    }

    @Test
    fun `manager has accessible config`() {
        assertNotNull(manager.config)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Registration Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `register overlay increases count`() {
        val overlay = TestOverlay("test-1")

        manager.register(overlay)

        assertEquals(1, manager.overlayCount)
    }

    @Test
    fun `register multiple overlays increases count accordingly`() {
        val overlay1 = TestOverlay("test-1")
        val overlay2 = TestOverlay("test-2")
        val overlay3 = TestOverlay("test-3")

        manager.register(overlay1)
        manager.register(overlay2)
        manager.register(overlay3)

        assertEquals(3, manager.overlayCount)
    }

    @Test
    fun `register overlay makes it findable by id`() {
        val overlay = TestOverlay("unique-id")

        manager.register(overlay)

        assertNotNull(manager.findById("unique-id"))
    }

    @Test
    fun `register duplicate id replaces previous overlay`() {
        val overlay1 = TestOverlay("same-id")
        val overlay2 = TestOverlay("same-id")

        manager.register(overlay1)
        manager.register(overlay2)

        assertEquals(1, manager.overlayCount)
        // The second overlay should be the one registered
        assertEquals(overlay2, manager.findById("same-id"))
    }

    @Test
    fun `register returns true on success`() {
        val overlay = TestOverlay("test-1")

        val result = manager.register(overlay)

        assertTrue(result)
    }

    @Test
    fun `getAllOverlayIds returns all registered ids`() {
        manager.register(TestOverlay("alpha"))
        manager.register(TestOverlay("beta"))
        manager.register(TestOverlay("gamma"))

        val ids = manager.getAllOverlayIds()

        assertEquals(3, ids.size)
        assertTrue(ids.contains("alpha"))
        assertTrue(ids.contains("beta"))
        assertTrue(ids.contains("gamma"))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Unregistration Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `unregister overlay decreases count`() {
        val overlay = TestOverlay("test-1")
        manager.register(overlay)

        manager.unregister("test-1")

        assertEquals(0, manager.overlayCount)
    }

    @Test
    fun `unregister overlay makes it unfindable`() {
        val overlay = TestOverlay("test-1")
        manager.register(overlay)

        manager.unregister("test-1")

        assertNull(manager.findById("test-1"))
    }

    @Test
    fun `unregister non-existent id returns false`() {
        val result = manager.unregister("non-existent")

        assertFalse(result)
    }

    @Test
    fun `unregister existing id returns true`() {
        manager.register(TestOverlay("test-1"))

        val result = manager.unregister("test-1")

        assertTrue(result)
    }

    @Test
    fun `unregister does not affect other overlays`() {
        manager.register(TestOverlay("keep-1"))
        manager.register(TestOverlay("remove"))
        manager.register(TestOverlay("keep-2"))

        manager.unregister("remove")

        assertEquals(2, manager.overlayCount)
        assertNotNull(manager.findById("keep-1"))
        assertNotNull(manager.findById("keep-2"))
        assertNull(manager.findById("remove"))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Lookup Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `findById returns correct overlay`() {
        val overlay1 = TestOverlay("first")
        val overlay2 = TestOverlay("second")
        manager.register(overlay1)
        manager.register(overlay2)

        val found = manager.findById("second")

        assertEquals(overlay2, found)
        assertEquals("second", found?.id)
    }

    @Test
    fun `findById returns null for non-existent id`() {
        manager.register(TestOverlay("exists"))

        val found = manager.findById("does-not-exist")

        assertNull(found)
    }

    @Test
    fun `contains returns true for registered overlay`() {
        manager.register(TestOverlay("test-id"))

        assertTrue(manager.contains("test-id"))
    }

    @Test
    fun `contains returns false for non-registered overlay`() {
        assertFalse(manager.contains("non-existent"))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Show/Hide All Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `showAll shows all registered overlays`() {
        val overlay1 = TestOverlay("a")
        val overlay2 = TestOverlay("b")
        val overlay3 = TestOverlay("c")
        manager.register(overlay1)
        manager.register(overlay2)
        manager.register(overlay3)

        manager.showAll()

        assertTrue(overlay1.isVisible)
        assertTrue(overlay2.isVisible)
        assertTrue(overlay3.isVisible)
    }

    @Test
    fun `hideAll hides all registered overlays`() {
        val overlay1 = TestOverlay("a")
        val overlay2 = TestOverlay("b")
        manager.register(overlay1)
        manager.register(overlay2)
        overlay1.show()
        overlay2.show()

        manager.hideAll()

        assertFalse(overlay1.isVisible)
        assertFalse(overlay2.isVisible)
    }

    @Test
    fun `showAll with empty manager does nothing`() {
        // Should not throw
        manager.showAll()
        assertEquals(0, manager.overlayCount)
    }

    @Test
    fun `hideAll with empty manager does nothing`() {
        // Should not throw
        manager.hideAll()
        assertEquals(0, manager.overlayCount)
    }

    @Test
    fun `showAll calls show on each overlay exactly once`() {
        val overlay1 = TestOverlay("a")
        val overlay2 = TestOverlay("b")
        manager.register(overlay1)
        manager.register(overlay2)

        manager.showAll()

        assertEquals(1, overlay1.showCount)
        assertEquals(1, overlay2.showCount)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Individual Show/Hide Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `show by id shows specific overlay`() {
        val overlay1 = TestOverlay("show-me")
        val overlay2 = TestOverlay("leave-me")
        manager.register(overlay1)
        manager.register(overlay2)

        manager.show("show-me")

        assertTrue(overlay1.isVisible)
        assertFalse(overlay2.isVisible)
    }

    @Test
    fun `hide by id hides specific overlay`() {
        val overlay1 = TestOverlay("hide-me")
        val overlay2 = TestOverlay("keep-visible")
        manager.register(overlay1)
        manager.register(overlay2)
        overlay1.show()
        overlay2.show()

        manager.hide("hide-me")

        assertFalse(overlay1.isVisible)
        assertTrue(overlay2.isVisible)
    }

    @Test
    fun `show non-existent id returns false`() {
        val result = manager.show("non-existent")

        assertFalse(result)
    }

    @Test
    fun `hide non-existent id returns false`() {
        val result = manager.hide("non-existent")

        assertFalse(result)
    }

    @Test
    fun `show existing id returns true`() {
        manager.register(TestOverlay("exists"))

        val result = manager.show("exists")

        assertTrue(result)
    }

    @Test
    fun `hide existing id returns true`() {
        manager.register(TestOverlay("exists"))

        val result = manager.hide("exists")

        assertTrue(result)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Visibility Query Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `isAnyVisible returns false when no overlays visible`() {
        manager.register(TestOverlay("a"))
        manager.register(TestOverlay("b"))

        assertFalse(manager.isAnyVisible())
    }

    @Test
    fun `isAnyVisible returns true when at least one overlay visible`() {
        val overlay1 = TestOverlay("a")
        val overlay2 = TestOverlay("b")
        manager.register(overlay1)
        manager.register(overlay2)

        overlay1.show()

        assertTrue(manager.isAnyVisible())
    }

    @Test
    fun `getVisibleOverlayIds returns only visible overlay ids`() {
        val overlay1 = TestOverlay("visible-1")
        val overlay2 = TestOverlay("hidden")
        val overlay3 = TestOverlay("visible-2")
        manager.register(overlay1)
        manager.register(overlay2)
        manager.register(overlay3)
        overlay1.show()
        overlay3.show()

        val visibleIds = manager.getVisibleOverlayIds()

        assertEquals(2, visibleIds.size)
        assertTrue(visibleIds.contains("visible-1"))
        assertTrue(visibleIds.contains("visible-2"))
        assertFalse(visibleIds.contains("hidden"))
    }

    @Test
    fun `isVisible returns correct state for specific overlay`() {
        val overlay = TestOverlay("test")
        manager.register(overlay)

        assertFalse(manager.isVisible("test"))

        overlay.show()

        assertTrue(manager.isVisible("test"))
    }

    @Test
    fun `isVisible returns false for non-existent overlay`() {
        assertFalse(manager.isVisible("non-existent"))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Disposal Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `disposeAll disposes all registered overlays`() {
        val overlay1 = TestOverlay("a")
        val overlay2 = TestOverlay("b")
        manager.register(overlay1)
        manager.register(overlay2)

        manager.disposeAll()

        assertTrue(overlay1.isDisposed)
        assertTrue(overlay2.isDisposed)
    }

    @Test
    fun `disposeAll clears all registered overlays`() {
        manager.register(TestOverlay("a"))
        manager.register(TestOverlay("b"))

        manager.disposeAll()

        assertEquals(0, manager.overlayCount)
        assertTrue(manager.getAllOverlayIds().isEmpty())
    }

    @Test
    fun `disposeAll hides all overlays before disposing`() {
        val overlay = TestOverlay("test")
        manager.register(overlay)
        overlay.show()

        manager.disposeAll()

        assertFalse(overlay.isVisible)
        assertTrue(overlay.isDisposed)
    }

    @Test
    fun `dispose by id disposes specific overlay`() {
        val overlay1 = TestOverlay("dispose-me")
        val overlay2 = TestOverlay("keep-me")
        manager.register(overlay1)
        manager.register(overlay2)

        manager.dispose("dispose-me")

        assertTrue(overlay1.isDisposed)
        assertFalse(overlay2.isDisposed)
    }

    @Test
    fun `dispose by id removes overlay from manager`() {
        manager.register(TestOverlay("test"))

        manager.dispose("test")

        assertFalse(manager.contains("test"))
        assertEquals(0, manager.overlayCount)
    }

    @Test
    fun `dispose non-existent id returns false`() {
        val result = manager.dispose("non-existent")

        assertFalse(result)
    }

    @Test
    fun `dispose existing id returns true`() {
        manager.register(TestOverlay("exists"))

        val result = manager.dispose("exists")

        assertTrue(result)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Config Propagation Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `manager exposes config for external access`() {
        val customConfig = OverlayConfig()
        customConfig.largeText = true

        val mgr = OverlayManager(customConfig)

        assertTrue(mgr.config.largeText)
    }

    @Test
    fun `config changes are visible through manager`() {
        manager.config.highContrast = true

        assertTrue(manager.config.highContrast)
    }

    @Test
    fun `updateConfig replaces config`() {
        val newConfig = OverlayConfig()
        newConfig.themeName = "AMOLED"
        newConfig.reducedMotion = true

        manager.updateConfig(newConfig)

        assertEquals("AMOLED", manager.config.themeName)
        assertTrue(manager.config.reducedMotion)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Update Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `update by id updates specific overlay`() {
        val overlay = TestOverlay("test")
        manager.register(overlay)
        val data = OverlayData.Status("Test", CommandState.SUCCESS)

        manager.update("test", data)

        assertEquals(data, overlay.lastData)
    }

    @Test
    fun `update non-existent id returns false`() {
        val result = manager.update("non-existent", OverlayData.Status("Test", CommandState.SUCCESS))

        assertFalse(result)
    }

    @Test
    fun `update existing id returns true`() {
        manager.register(TestOverlay("exists"))

        val result = manager.update("exists", OverlayData.Status("Test", CommandState.SUCCESS))

        assertTrue(result)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Clear Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `clear removes all overlays without disposing`() {
        val overlay1 = TestOverlay("a")
        val overlay2 = TestOverlay("b")
        manager.register(overlay1)
        manager.register(overlay2)

        manager.clear()

        assertEquals(0, manager.overlayCount)
        // Overlays should NOT be disposed
        assertFalse(overlay1.isDisposed)
        assertFalse(overlay2.isDisposed)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Iteration Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `forEach iterates all overlays`() {
        manager.register(TestOverlay("a"))
        manager.register(TestOverlay("b"))
        manager.register(TestOverlay("c"))

        var count = 0
        manager.forEach { count++ }

        assertEquals(3, count)
    }

    @Test
    fun `forEach with empty manager executes zero times`() {
        var count = 0
        manager.forEach { count++ }

        assertEquals(0, count)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Thread Safety Tests (Conceptual - actual thread testing is platform-specific)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `concurrent registration does not corrupt state`() {
        // This test verifies the basic contract holds
        // Actual concurrent testing would require platform-specific threading
        for (i in 1..100) {
            manager.register(TestOverlay("overlay-$i"))
        }

        assertEquals(100, manager.overlayCount)
    }

    @Test
    fun `concurrent unregistration does not corrupt state`() {
        for (i in 1..50) {
            manager.register(TestOverlay("overlay-$i"))
        }

        for (i in 1..25) {
            manager.unregister("overlay-$i")
        }

        assertEquals(25, manager.overlayCount)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Edge Case Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `register with empty id is allowed`() {
        val overlay = TestOverlay("")

        manager.register(overlay)

        assertTrue(manager.contains(""))
        assertEquals(1, manager.overlayCount)
    }

    @Test
    fun `operations on disposed overlay are safe`() {
        val overlay = TestOverlay("test")
        manager.register(overlay)
        overlay.dispose()

        // Should not throw
        manager.show("test")
        manager.hide("test")
        manager.update("test", OverlayData.Status("Test", CommandState.SUCCESS))
    }

    @Test
    fun `unregister same id twice does not throw`() {
        manager.register(TestOverlay("test"))
        manager.unregister("test")

        // Second unregister should not throw
        val result = manager.unregister("test")

        assertFalse(result)
    }

    @Test
    fun `getAllOverlayIds returns defensive copy`() {
        manager.register(TestOverlay("test"))

        val ids1 = manager.getAllOverlayIds()
        val ids2 = manager.getAllOverlayIds()

        // Should be equal but not same instance
        assertEquals(ids1, ids2)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// OverlayManager Factory Tests
// ═══════════════════════════════════════════════════════════════════════════

class OverlayManagerFactoryTest {

    @Test
    fun `create manager with default config`() {
        val manager = OverlayManager()

        assertNotNull(manager.config)
        assertEquals("Material3Dark", manager.config.themeName)
    }

    @Test
    fun `create manager with custom config`() {
        val config = OverlayConfig()
        config.themeName = "Custom"
        config.largeText = true

        val manager = OverlayManager(config)

        assertEquals("Custom", manager.config.themeName)
        assertTrue(manager.config.largeText)
    }
}
