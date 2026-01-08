package com.augmentalis.voiceoscoreng.cursor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD tests for BoundaryDetector class.
 * Tests boundary detection, safe bounds, clamping, and edge detection.
 */
class BoundaryDetectorTest {

    // ==================== ScreenBounds Tests ====================

    @Test
    fun `ScreenBounds safeWidth calculation without insets`() {
        val bounds = ScreenBounds(width = 1080, height = 1920)
        assertEquals(1080, bounds.safeWidth)
    }

    @Test
    fun `ScreenBounds safeHeight calculation without insets`() {
        val bounds = ScreenBounds(width = 1080, height = 1920)
        assertEquals(1920, bounds.safeHeight)
    }

    @Test
    fun `ScreenBounds safeWidth calculation with insets`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetLeft = 20,
            safeInsetRight = 20
        )
        assertEquals(1040, bounds.safeWidth)
    }

    @Test
    fun `ScreenBounds safeHeight calculation with insets`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetTop = 100,
            safeInsetBottom = 50
        )
        assertEquals(1770, bounds.safeHeight)
    }

    @Test
    fun `ScreenBounds all insets`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetTop = 100,
            safeInsetBottom = 50,
            safeInsetLeft = 20,
            safeInsetRight = 20
        )
        assertEquals(1040, bounds.safeWidth)
        assertEquals(1770, bounds.safeHeight)
    }

    // ==================== BoundaryDetector setBounds/getBounds Tests ====================

    @Test
    fun `getBounds returns default bounds`() {
        val detector = BoundaryDetector()
        val bounds = detector.getBounds()
        assertEquals(1080, bounds.width)
        assertEquals(1920, bounds.height)
    }

    @Test
    fun `setBounds updates bounds`() {
        val detector = BoundaryDetector()
        val newBounds = ScreenBounds(width = 2560, height = 1440)
        detector.setBounds(newBounds)
        val bounds = detector.getBounds()
        assertEquals(2560, bounds.width)
        assertEquals(1440, bounds.height)
    }

    @Test
    fun `constructor accepts custom bounds`() {
        val customBounds = ScreenBounds(width = 800, height = 600)
        val detector = BoundaryDetector(customBounds)
        val bounds = detector.getBounds()
        assertEquals(800, bounds.width)
        assertEquals(600, bounds.height)
    }

    // ==================== isWithinBounds Tests ====================

    @Test
    fun `isWithinBounds returns true for center position`() {
        val detector = BoundaryDetector()
        assertTrue(detector.isWithinBounds(540, 960))
    }

    @Test
    fun `isWithinBounds returns true for origin`() {
        val detector = BoundaryDetector()
        assertTrue(detector.isWithinBounds(0, 0))
    }

    @Test
    fun `isWithinBounds returns true for max valid position`() {
        val detector = BoundaryDetector()
        assertTrue(detector.isWithinBounds(1079, 1919))
    }

    @Test
    fun `isWithinBounds returns false for negative x`() {
        val detector = BoundaryDetector()
        assertFalse(detector.isWithinBounds(-1, 960))
    }

    @Test
    fun `isWithinBounds returns false for negative y`() {
        val detector = BoundaryDetector()
        assertFalse(detector.isWithinBounds(540, -1))
    }

    @Test
    fun `isWithinBounds returns false for x at width`() {
        val detector = BoundaryDetector()
        assertFalse(detector.isWithinBounds(1080, 960))
    }

    @Test
    fun `isWithinBounds returns false for y at height`() {
        val detector = BoundaryDetector()
        assertFalse(detector.isWithinBounds(540, 1920))
    }

    @Test
    fun `isWithinBounds returns false for both coordinates out of bounds`() {
        val detector = BoundaryDetector()
        assertFalse(detector.isWithinBounds(2000, 3000))
    }

    // ==================== isWithinSafeBounds Tests ====================

    @Test
    fun `isWithinSafeBounds returns true for center without insets`() {
        val detector = BoundaryDetector()
        assertTrue(detector.isWithinSafeBounds(540, 960))
    }

    @Test
    fun `isWithinSafeBounds returns true for center with insets`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetTop = 100,
            safeInsetBottom = 50,
            safeInsetLeft = 20,
            safeInsetRight = 20
        )
        val detector = BoundaryDetector(bounds)
        assertTrue(detector.isWithinSafeBounds(540, 960))
    }

    @Test
    fun `isWithinSafeBounds returns false for position in top inset`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetTop = 100
        )
        val detector = BoundaryDetector(bounds)
        assertFalse(detector.isWithinSafeBounds(540, 50))
    }

    @Test
    fun `isWithinSafeBounds returns false for position in bottom inset`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetBottom = 50
        )
        val detector = BoundaryDetector(bounds)
        assertFalse(detector.isWithinSafeBounds(540, 1900))
    }

    @Test
    fun `isWithinSafeBounds returns false for position in left inset`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetLeft = 20
        )
        val detector = BoundaryDetector(bounds)
        assertFalse(detector.isWithinSafeBounds(10, 960))
    }

    @Test
    fun `isWithinSafeBounds returns false for position in right inset`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetRight = 20
        )
        val detector = BoundaryDetector(bounds)
        assertFalse(detector.isWithinSafeBounds(1070, 960))
    }

    @Test
    fun `isWithinSafeBounds boundary at safe edge top-left`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetTop = 100,
            safeInsetLeft = 20
        )
        val detector = BoundaryDetector(bounds)
        // Exactly at safe boundary should be within safe bounds
        assertTrue(detector.isWithinSafeBounds(20, 100))
    }

    @Test
    fun `isWithinSafeBounds boundary at safe edge bottom-right`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetBottom = 50,
            safeInsetRight = 20
        )
        val detector = BoundaryDetector(bounds)
        // Max safe position is (width - right - 1, height - bottom - 1)
        assertTrue(detector.isWithinSafeBounds(1059, 1869))
    }

    // ==================== clamp Tests ====================

    @Test
    fun `clamp keeps position inside bounds unchanged`() {
        val detector = BoundaryDetector()
        val (x, y) = detector.clamp(540, 960)
        assertEquals(540, x)
        assertEquals(960, y)
    }

    @Test
    fun `clamp corrects negative x to zero`() {
        val detector = BoundaryDetector()
        val (x, y) = detector.clamp(-100, 960)
        assertEquals(0, x)
        assertEquals(960, y)
    }

    @Test
    fun `clamp corrects negative y to zero`() {
        val detector = BoundaryDetector()
        val (x, y) = detector.clamp(540, -100)
        assertEquals(540, x)
        assertEquals(0, y)
    }

    @Test
    fun `clamp corrects x exceeding width`() {
        val detector = BoundaryDetector()
        val (x, y) = detector.clamp(2000, 960)
        assertEquals(1079, x)
        assertEquals(960, y)
    }

    @Test
    fun `clamp corrects y exceeding height`() {
        val detector = BoundaryDetector()
        val (x, y) = detector.clamp(540, 3000)
        assertEquals(540, x)
        assertEquals(1919, y)
    }

    @Test
    fun `clamp corrects both coordinates when both exceed`() {
        val detector = BoundaryDetector()
        val (x, y) = detector.clamp(5000, 5000)
        assertEquals(1079, x)
        assertEquals(1919, y)
    }

    @Test
    fun `clamp corrects both coordinates when both negative`() {
        val detector = BoundaryDetector()
        val (x, y) = detector.clamp(-100, -200)
        assertEquals(0, x)
        assertEquals(0, y)
    }

    @Test
    fun `clamp at exact boundary remains at boundary`() {
        val detector = BoundaryDetector()
        val (x, y) = detector.clamp(0, 1919)
        assertEquals(0, x)
        assertEquals(1919, y)
    }

    // ==================== clampToSafe Tests ====================

    @Test
    fun `clampToSafe keeps position inside safe bounds unchanged`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetTop = 100,
            safeInsetBottom = 50,
            safeInsetLeft = 20,
            safeInsetRight = 20
        )
        val detector = BoundaryDetector(bounds)
        val (x, y) = detector.clampToSafe(540, 960)
        assertEquals(540, x)
        assertEquals(960, y)
    }

    @Test
    fun `clampToSafe corrects x to left safe inset`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetLeft = 20
        )
        val detector = BoundaryDetector(bounds)
        val (x, y) = detector.clampToSafe(5, 960)
        assertEquals(20, x)
        assertEquals(960, y)
    }

    @Test
    fun `clampToSafe corrects x to right safe boundary`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetRight = 20
        )
        val detector = BoundaryDetector(bounds)
        val (x, y) = detector.clampToSafe(1070, 960)
        assertEquals(1059, x)
        assertEquals(960, y)
    }

    @Test
    fun `clampToSafe corrects y to top safe inset`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetTop = 100
        )
        val detector = BoundaryDetector(bounds)
        val (x, y) = detector.clampToSafe(540, 50)
        assertEquals(540, x)
        assertEquals(100, y)
    }

    @Test
    fun `clampToSafe corrects y to bottom safe boundary`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetBottom = 50
        )
        val detector = BoundaryDetector(bounds)
        val (x, y) = detector.clampToSafe(540, 1900)
        assertEquals(540, x)
        assertEquals(1869, y)
    }

    @Test
    fun `clampToSafe corrects all coordinates with full insets`() {
        val bounds = ScreenBounds(
            width = 1080,
            height = 1920,
            safeInsetTop = 100,
            safeInsetBottom = 50,
            safeInsetLeft = 20,
            safeInsetRight = 20
        )
        val detector = BoundaryDetector(bounds)
        val (x, y) = detector.clampToSafe(0, 0)
        assertEquals(20, x)
        assertEquals(100, y)
    }

    @Test
    fun `clampToSafe without insets behaves like clamp`() {
        val detector = BoundaryDetector()
        val (x, y) = detector.clampToSafe(-100, 2000)
        assertEquals(0, x)
        assertEquals(1919, y)
    }

    // ==================== getEdge Tests ====================

    @Test
    fun `getEdge returns LEFT for x at zero`() {
        val detector = BoundaryDetector()
        assertEquals(BoundaryDetector.Edge.LEFT, detector.getEdge(0, 960))
    }

    @Test
    fun `getEdge returns RIGHT for x at max`() {
        val detector = BoundaryDetector()
        assertEquals(BoundaryDetector.Edge.RIGHT, detector.getEdge(1079, 960))
    }

    @Test
    fun `getEdge returns TOP for y at zero`() {
        val detector = BoundaryDetector()
        assertEquals(BoundaryDetector.Edge.TOP, detector.getEdge(540, 0))
    }

    @Test
    fun `getEdge returns BOTTOM for y at max`() {
        val detector = BoundaryDetector()
        assertEquals(BoundaryDetector.Edge.BOTTOM, detector.getEdge(540, 1919))
    }

    @Test
    fun `getEdge returns null for center position`() {
        val detector = BoundaryDetector()
        assertNull(detector.getEdge(540, 960))
    }

    @Test
    fun `getEdge returns null for position near but not at edge`() {
        val detector = BoundaryDetector()
        assertNull(detector.getEdge(1, 960))
        assertNull(detector.getEdge(540, 1))
        assertNull(detector.getEdge(1078, 960))
        assertNull(detector.getEdge(540, 1918))
    }

    @Test
    fun `getEdge prioritizes LEFT over TOP at top-left corner`() {
        val detector = BoundaryDetector()
        // When at (0, 0), LEFT should be detected first based on implementation order
        assertEquals(BoundaryDetector.Edge.LEFT, detector.getEdge(0, 0))
    }

    @Test
    fun `getEdge prioritizes LEFT over BOTTOM at bottom-left corner`() {
        val detector = BoundaryDetector()
        assertEquals(BoundaryDetector.Edge.LEFT, detector.getEdge(0, 1919))
    }

    @Test
    fun `getEdge prioritizes RIGHT over TOP at top-right corner`() {
        val detector = BoundaryDetector()
        assertEquals(BoundaryDetector.Edge.RIGHT, detector.getEdge(1079, 0))
    }

    @Test
    fun `getEdge prioritizes RIGHT over BOTTOM at bottom-right corner`() {
        val detector = BoundaryDetector()
        assertEquals(BoundaryDetector.Edge.RIGHT, detector.getEdge(1079, 1919))
    }

    @Test
    fun `getEdge works with custom bounds`() {
        val bounds = ScreenBounds(width = 800, height = 600)
        val detector = BoundaryDetector(bounds)
        assertEquals(BoundaryDetector.Edge.LEFT, detector.getEdge(0, 300))
        assertEquals(BoundaryDetector.Edge.RIGHT, detector.getEdge(799, 300))
        assertEquals(BoundaryDetector.Edge.TOP, detector.getEdge(400, 0))
        assertEquals(BoundaryDetector.Edge.BOTTOM, detector.getEdge(400, 599))
    }

    // ==================== Integration Tests ====================

    @Test
    fun `boundary detection works after bounds change`() {
        val detector = BoundaryDetector()
        assertTrue(detector.isWithinBounds(1000, 1800))

        detector.setBounds(ScreenBounds(width = 800, height = 600))
        assertFalse(detector.isWithinBounds(1000, 1800))
        assertTrue(detector.isWithinBounds(400, 300))
    }

    @Test
    fun `clamp works correctly after bounds change`() {
        val detector = BoundaryDetector()

        detector.setBounds(ScreenBounds(width = 100, height = 100))
        val (x, y) = detector.clamp(500, 500)
        assertEquals(99, x)
        assertEquals(99, y)
    }
}
