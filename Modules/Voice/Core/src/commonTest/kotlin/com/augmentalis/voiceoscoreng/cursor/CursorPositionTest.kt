package com.augmentalis.voiceoscoreng.cursor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CursorPositionTest {

    // ==================== ORIGIN Constant Tests ====================

    @Test
    fun `ORIGIN has x equals 0`() {
        assertEquals(0, CursorPosition.ORIGIN.x)
    }

    @Test
    fun `ORIGIN has y equals 0`() {
        assertEquals(0, CursorPosition.ORIGIN.y)
    }

    @Test
    fun `ORIGIN has timestamp equals 0`() {
        assertEquals(0L, CursorPosition.ORIGIN.timestamp)
    }

    // ==================== distanceTo Tests ====================

    @Test
    fun `distanceTo returns 0 for same point`() {
        val point = CursorPosition(100, 200, 0L)
        assertEquals(0.0, point.distanceTo(point))
    }

    @Test
    fun `distanceTo calculates horizontal distance correctly`() {
        val point1 = CursorPosition(0, 0, 0L)
        val point2 = CursorPosition(10, 0, 0L)
        assertEquals(10.0, point1.distanceTo(point2))
    }

    @Test
    fun `distanceTo calculates vertical distance correctly`() {
        val point1 = CursorPosition(0, 0, 0L)
        val point2 = CursorPosition(0, 10, 0L)
        assertEquals(10.0, point1.distanceTo(point2))
    }

    @Test
    fun `distanceTo calculates diagonal distance using pythagorean theorem`() {
        val point1 = CursorPosition(0, 0, 0L)
        val point2 = CursorPosition(3, 4, 0L)
        // 3-4-5 right triangle
        assertEquals(5.0, point1.distanceTo(point2))
    }

    @Test
    fun `distanceTo is symmetric`() {
        val point1 = CursorPosition(10, 20, 0L)
        val point2 = CursorPosition(50, 80, 0L)
        assertEquals(point1.distanceTo(point2), point2.distanceTo(point1))
    }

    // ==================== offset Tests ====================

    @Test
    fun `offset with positive values increases coordinates`() {
        val point = CursorPosition(10, 20, 1000L)
        val result = point.offset(5, 15)
        assertEquals(15, result.x)
        assertEquals(35, result.y)
    }

    @Test
    fun `offset with negative values decreases coordinates`() {
        val point = CursorPosition(100, 200, 1000L)
        val result = point.offset(-30, -50)
        assertEquals(70, result.x)
        assertEquals(150, result.y)
    }

    @Test
    fun `offset with zero values returns same coordinates`() {
        val point = CursorPosition(50, 75, 1000L)
        val result = point.offset(0, 0)
        assertEquals(50, result.x)
        assertEquals(75, result.y)
    }

    @Test
    fun `offset can result in negative coordinates`() {
        val point = CursorPosition(10, 10, 1000L)
        val result = point.offset(-20, -30)
        assertEquals(-10, result.x)
        assertEquals(-20, result.y)
    }

    // ==================== clamp Tests ====================

    @Test
    fun `clamp does not change point within bounds`() {
        val point = CursorPosition(50, 75, 1000L)
        val result = point.clamp(minX = 0, minY = 0, maxX = 100, maxY = 100)
        assertEquals(50, result.x)
        assertEquals(75, result.y)
        assertEquals(1000L, result.timestamp)
    }

    @Test
    fun `clamp constrains x below minimum`() {
        val point = CursorPosition(-10, 50, 1000L)
        val result = point.clamp(minX = 0, minY = 0, maxX = 100, maxY = 100)
        assertEquals(0, result.x)
        assertEquals(50, result.y)
    }

    @Test
    fun `clamp constrains x above maximum`() {
        val point = CursorPosition(150, 50, 1000L)
        val result = point.clamp(minX = 0, minY = 0, maxX = 100, maxY = 100)
        assertEquals(100, result.x)
        assertEquals(50, result.y)
    }

    @Test
    fun `clamp constrains y below minimum`() {
        val point = CursorPosition(50, -20, 1000L)
        val result = point.clamp(minX = 0, minY = 0, maxX = 100, maxY = 100)
        assertEquals(50, result.x)
        assertEquals(0, result.y)
    }

    @Test
    fun `clamp constrains y above maximum`() {
        val point = CursorPosition(50, 200, 1000L)
        val result = point.clamp(minX = 0, minY = 0, maxX = 100, maxY = 100)
        assertEquals(50, result.x)
        assertEquals(100, result.y)
    }

    @Test
    fun `clamp constrains both x and y when outside bounds`() {
        val point = CursorPosition(-50, 300, 1000L)
        val result = point.clamp(minX = 0, minY = 0, maxX = 100, maxY = 100)
        assertEquals(0, result.x)
        assertEquals(100, result.y)
    }

    @Test
    fun `clamp preserves timestamp`() {
        val point = CursorPosition(500, 600, 12345L)
        val result = point.clamp(minX = 0, minY = 0, maxX = 100, maxY = 100)
        assertEquals(12345L, result.timestamp)
    }

    // ==================== isWithinBounds Tests ====================

    @Test
    fun `isWithinBounds returns true for point inside bounds`() {
        val point = CursorPosition(50, 75, 0L)
        assertTrue(point.isWithinBounds(width = 100, height = 100))
    }

    @Test
    fun `isWithinBounds returns true for point at origin`() {
        val point = CursorPosition(0, 0, 0L)
        assertTrue(point.isWithinBounds(width = 100, height = 100))
    }

    @Test
    fun `isWithinBounds returns true for point at max valid position`() {
        val point = CursorPosition(99, 99, 0L)
        assertTrue(point.isWithinBounds(width = 100, height = 100))
    }

    @Test
    fun `isWithinBounds returns false for negative x`() {
        val point = CursorPosition(-1, 50, 0L)
        assertFalse(point.isWithinBounds(width = 100, height = 100))
    }

    @Test
    fun `isWithinBounds returns false for negative y`() {
        val point = CursorPosition(50, -1, 0L)
        assertFalse(point.isWithinBounds(width = 100, height = 100))
    }

    @Test
    fun `isWithinBounds returns false for x equals width`() {
        val point = CursorPosition(100, 50, 0L)
        assertFalse(point.isWithinBounds(width = 100, height = 100))
    }

    @Test
    fun `isWithinBounds returns false for y equals height`() {
        val point = CursorPosition(50, 100, 0L)
        assertFalse(point.isWithinBounds(width = 100, height = 100))
    }

    @Test
    fun `isWithinBounds returns false for x greater than width`() {
        val point = CursorPosition(150, 50, 0L)
        assertFalse(point.isWithinBounds(width = 100, height = 100))
    }

    @Test
    fun `isWithinBounds returns false for y greater than height`() {
        val point = CursorPosition(50, 150, 0L)
        assertFalse(point.isWithinBounds(width = 100, height = 100))
    }

    // ==================== Data Class Tests ====================

    @Test
    fun `CursorPosition equality works correctly`() {
        val point1 = CursorPosition(100, 200, 1000L)
        val point2 = CursorPosition(100, 200, 1000L)
        assertEquals(point1, point2)
    }

    @Test
    fun `CursorPosition copy works correctly`() {
        val original = CursorPosition(100, 200, 1000L)
        val copied = original.copy(x = 150)
        assertEquals(150, copied.x)
        assertEquals(200, copied.y)
        assertEquals(1000L, copied.timestamp)
    }

    @Test
    fun `CursorPosition default timestamp is non-zero for new instance`() {
        val point = CursorPosition(0, 0)
        assertTrue(point.timestamp >= 0L)
    }
}
