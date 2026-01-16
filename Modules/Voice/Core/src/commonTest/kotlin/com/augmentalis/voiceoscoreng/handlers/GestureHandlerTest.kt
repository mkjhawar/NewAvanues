/**
 * GestureHandlerTest.kt - Tests for GestureHandler
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * TDD tests for gesture command handling.
 */
package com.augmentalis.voiceoscoreng.handlers

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GestureHandlerTest {

    private lateinit var handler: GestureHandler

    @BeforeTest
    fun setup() {
        handler = GestureHandler()
    }

    // ==================== handleCommand "tap" Tests ====================

    @Test
    fun `handleCommand tap returns TAP gesture`() {
        val result = handler.handleCommand("tap")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.TAP, result.gesture?.type)
        assertNull(result.error)
    }

    @Test
    fun `handleCommand click returns TAP gesture alias`() {
        val result = handler.handleCommand("click")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.TAP, result.gesture?.type)
        assertNull(result.error)
    }

    // ==================== handleCommand "double tap" Tests ====================

    @Test
    fun `handleCommand double tap returns DOUBLE_TAP gesture`() {
        val result = handler.handleCommand("double tap")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.DOUBLE_TAP, result.gesture?.type)
        assertNull(result.error)
    }

    @Test
    fun `handleCommand double click returns DOUBLE_TAP gesture alias`() {
        val result = handler.handleCommand("double click")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.DOUBLE_TAP, result.gesture?.type)
    }

    // ==================== handleCommand "long press" Tests ====================

    @Test
    fun `handleCommand long press returns LONG_PRESS gesture with 500ms duration`() {
        val result = handler.handleCommand("long press")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.LONG_PRESS, result.gesture?.type)
        assertEquals(500L, result.gesture?.duration)
    }

    @Test
    fun `handleCommand hold returns LONG_PRESS gesture alias`() {
        val result = handler.handleCommand("hold")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.LONG_PRESS, result.gesture?.type)
        assertEquals(500L, result.gesture?.duration)
    }

    // ==================== handleCommand "swipe" Tests ====================

    @Test
    fun `handleCommand swipe up returns SWIPE_UP gesture`() {
        val result = handler.handleCommand("swipe up")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.SWIPE_UP, result.gesture?.type)
        assertEquals(200, result.gesture?.distance)
    }

    @Test
    fun `handleCommand swipe down returns SWIPE_DOWN gesture`() {
        val result = handler.handleCommand("swipe down")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.SWIPE_DOWN, result.gesture?.type)
    }

    @Test
    fun `handleCommand swipe left returns SWIPE_LEFT gesture`() {
        val result = handler.handleCommand("swipe left")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.SWIPE_LEFT, result.gesture?.type)
    }

    @Test
    fun `handleCommand swipe right returns SWIPE_RIGHT gesture`() {
        val result = handler.handleCommand("swipe right")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.SWIPE_RIGHT, result.gesture?.type)
    }

    // ==================== handleCommand "pinch" Tests ====================

    @Test
    fun `handleCommand pinch in returns PINCH_IN gesture`() {
        val result = handler.handleCommand("pinch in")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.PINCH_IN, result.gesture?.type)
    }

    @Test
    fun `handleCommand zoom out returns PINCH_IN gesture alias`() {
        val result = handler.handleCommand("zoom out")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.PINCH_IN, result.gesture?.type)
    }

    @Test
    fun `handleCommand pinch out returns PINCH_OUT gesture`() {
        val result = handler.handleCommand("pinch out")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.PINCH_OUT, result.gesture?.type)
    }

    @Test
    fun `handleCommand zoom in returns PINCH_OUT gesture alias`() {
        val result = handler.handleCommand("zoom in")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.PINCH_OUT, result.gesture?.type)
    }

    // ==================== handleCommand "rotate" Tests ====================

    @Test
    fun `handleCommand rotate left returns ROTATE_LEFT gesture`() {
        val result = handler.handleCommand("rotate left")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.ROTATE_LEFT, result.gesture?.type)
    }

    @Test
    fun `handleCommand rotate right returns ROTATE_RIGHT gesture`() {
        val result = handler.handleCommand("rotate right")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.ROTATE_RIGHT, result.gesture?.type)
    }

    // ==================== handleCommand "tap at X,Y" Tests ====================

    @Test
    fun `handleCommand tap at X comma Y parses coordinates`() {
        val result = handler.handleCommand("tap at 100,200")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.TAP, result.gesture?.type)
        assertEquals(100, result.gesture?.x)
        assertEquals(200, result.gesture?.y)
    }

    @Test
    fun `handleCommand tap at X space Y parses coordinates`() {
        val result = handler.handleCommand("tap at 300 400")

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.TAP, result.gesture?.type)
        assertEquals(300, result.gesture?.x)
        assertEquals(400, result.gesture?.y)
    }

    @Test
    fun `handleCommand tap at with invalid format returns error`() {
        val result = handler.handleCommand("tap at invalid")

        assertFalse(result.success)
        assertNull(result.gesture)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Could not parse coordinates"))
    }

    // ==================== handleCommand Unknown Command Tests ====================

    @Test
    fun `handleCommand unknown returns error`() {
        val result = handler.handleCommand("do something random")

        assertFalse(result.success)
        assertNull(result.gesture)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Unknown gesture"))
    }

    @Test
    fun `handleCommand empty string returns error`() {
        val result = handler.handleCommand("")

        assertFalse(result.success)
        assertNull(result.gesture)
        assertNotNull(result.error)
    }

    // ==================== handleCommand Case Insensitivity Tests ====================

    @Test
    fun `handleCommand is case insensitive`() {
        val resultUpper = handler.handleCommand("TAP")
        val resultMixed = handler.handleCommand("Tap")
        val resultLower = handler.handleCommand("tap")

        assertTrue(resultUpper.success)
        assertTrue(resultMixed.success)
        assertTrue(resultLower.success)
        assertEquals(GestureType.TAP, resultUpper.gesture?.type)
        assertEquals(GestureType.TAP, resultMixed.gesture?.type)
        assertEquals(GestureType.TAP, resultLower.gesture?.type)
    }

    @Test
    fun `handleCommand trims whitespace`() {
        val result = handler.handleCommand("  tap  ")

        assertTrue(result.success)
        assertEquals(GestureType.TAP, result.gesture?.type)
    }

    // ==================== createGesture Tests ====================

    @Test
    fun `createGesture with default parameters`() {
        val result = handler.createGesture(GestureType.TAP)

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.TAP, result.gesture?.type)
        assertEquals(540, result.gesture?.x) // Default center x
        assertEquals(960, result.gesture?.y) // Default center y
        assertEquals(100L, result.gesture?.duration)
        assertEquals(200, result.gesture?.distance)
    }

    @Test
    fun `createGesture with custom x y coordinates`() {
        val result = handler.createGesture(GestureType.TAP, x = 100, y = 200)

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(100, result.gesture?.x)
        assertEquals(200, result.gesture?.y)
    }

    @Test
    fun `createGesture with custom duration`() {
        val result = handler.createGesture(GestureType.LONG_PRESS, duration = 1000L)

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.LONG_PRESS, result.gesture?.type)
        assertEquals(1000L, result.gesture?.duration)
    }

    @Test
    fun `createGesture with custom distance for swipe`() {
        val result = handler.createGesture(GestureType.SWIPE_UP, distance = 500)

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.SWIPE_UP, result.gesture?.type)
        assertEquals(500, result.gesture?.distance)
    }

    @Test
    fun `createGesture with all custom parameters`() {
        val result = handler.createGesture(
            type = GestureType.SWIPE_LEFT,
            x = 800,
            y = 1200,
            duration = 300L,
            distance = 400
        )

        assertTrue(result.success)
        assertNotNull(result.gesture)
        assertEquals(GestureType.SWIPE_LEFT, result.gesture?.type)
        assertEquals(800, result.gesture?.x)
        assertEquals(1200, result.gesture?.y)
        assertEquals(300L, result.gesture?.duration)
        assertEquals(400, result.gesture?.distance)
    }

    // ==================== getGestureTypes Tests ====================

    @Test
    fun `getGestureTypes returns all gesture types`() {
        val types = handler.getGestureTypes()

        assertEquals(11, types.size)
        assertTrue(types.contains(GestureType.TAP))
        assertTrue(types.contains(GestureType.DOUBLE_TAP))
        assertTrue(types.contains(GestureType.LONG_PRESS))
        assertTrue(types.contains(GestureType.SWIPE_UP))
        assertTrue(types.contains(GestureType.SWIPE_DOWN))
        assertTrue(types.contains(GestureType.SWIPE_LEFT))
        assertTrue(types.contains(GestureType.SWIPE_RIGHT))
        assertTrue(types.contains(GestureType.PINCH_IN))
        assertTrue(types.contains(GestureType.PINCH_OUT))
        assertTrue(types.contains(GestureType.ROTATE_LEFT))
        assertTrue(types.contains(GestureType.ROTATE_RIGHT))
    }

    // ==================== GestureConfig Tests ====================

    @Test
    fun `GestureConfig default values are correct`() {
        val config = GestureConfig(type = GestureType.TAP)

        assertEquals(GestureType.TAP, config.type)
        assertEquals(540, config.x)  // Default center
        assertEquals(960, config.y)  // Default center
        assertEquals(100L, config.duration)
        assertEquals(200, config.distance)
    }

    @Test
    fun `GestureConfig custom values are stored`() {
        val config = GestureConfig(
            type = GestureType.SWIPE_DOWN,
            x = 123,
            y = 456,
            duration = 250L,
            distance = 350
        )

        assertEquals(GestureType.SWIPE_DOWN, config.type)
        assertEquals(123, config.x)
        assertEquals(456, config.y)
        assertEquals(250L, config.duration)
        assertEquals(350, config.distance)
    }

    // ==================== GestureResult Tests ====================

    @Test
    fun `GestureResult success case`() {
        val config = GestureConfig(GestureType.TAP)
        val result = GestureResult(
            success = true,
            gesture = config
        )

        assertTrue(result.success)
        assertEquals(config, result.gesture)
        assertNull(result.error)
    }

    @Test
    fun `GestureResult failure case`() {
        val result = GestureResult(
            success = false,
            error = "Test error message"
        )

        assertFalse(result.success)
        assertNull(result.gesture)
        assertEquals("Test error message", result.error)
    }

    // ==================== GestureType Enum Tests ====================

    @Test
    fun `GestureType enum has all expected values`() {
        val expectedTypes = listOf(
            "TAP", "DOUBLE_TAP", "LONG_PRESS",
            "SWIPE_UP", "SWIPE_DOWN", "SWIPE_LEFT", "SWIPE_RIGHT",
            "PINCH_IN", "PINCH_OUT",
            "ROTATE_LEFT", "ROTATE_RIGHT"
        )

        val actualTypes = GestureType.entries.map { it.name }

        assertEquals(expectedTypes.size, actualTypes.size)
        expectedTypes.forEach { expected ->
            assertTrue(actualTypes.contains(expected), "Missing GestureType: $expected")
        }
    }
}
