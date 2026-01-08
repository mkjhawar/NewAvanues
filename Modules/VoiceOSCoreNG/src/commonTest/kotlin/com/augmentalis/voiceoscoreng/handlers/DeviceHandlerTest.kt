/**
 * DeviceHandlerTest.kt - Tests for DeviceHandler
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * TDD tests for device control command handling (Phase 12).
 */
package com.augmentalis.voiceoscoreng.handlers

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mock implementation of IDeviceController for testing.
 */
class MockDeviceController : IDeviceController {
    var volumeLevel: Int = 50
    var brightnessLevel: Int = 50
    var screenOn: Boolean = true
    var flashlightOn: Boolean = false

    // Track method calls for verification
    var setVolumeCalled = false
    var setBrightnessCalled = false
    var setScreenStateCalled = false
    var setFlashlightCalled = false

    // Control return values for testing failure scenarios
    var shouldFailSetVolume = false
    var shouldFailSetBrightness = false
    var shouldFailSetScreenState = false
    var shouldFailSetFlashlight = false

    override fun setVolume(level: Int): Boolean {
        setVolumeCalled = true
        if (shouldFailSetVolume) return false
        volumeLevel = level.coerceIn(0, 100)
        return true
    }

    override fun getVolume(): Int = volumeLevel

    override fun setBrightness(level: Int): Boolean {
        setBrightnessCalled = true
        if (shouldFailSetBrightness) return false
        brightnessLevel = level.coerceIn(0, 100)
        return true
    }

    override fun getBrightness(): Int = brightnessLevel

    override fun setScreenState(on: Boolean): Boolean {
        setScreenStateCalled = true
        if (shouldFailSetScreenState) return false
        screenOn = on
        return true
    }

    override fun setFlashlight(on: Boolean): Boolean {
        setFlashlightCalled = true
        if (shouldFailSetFlashlight) return false
        flashlightOn = on
        return true
    }

    fun reset() {
        volumeLevel = 50
        brightnessLevel = 50
        screenOn = true
        flashlightOn = false
        setVolumeCalled = false
        setBrightnessCalled = false
        setScreenStateCalled = false
        setFlashlightCalled = false
        shouldFailSetVolume = false
        shouldFailSetBrightness = false
        shouldFailSetScreenState = false
        shouldFailSetFlashlight = false
    }
}

class DeviceHandlerTest {

    private lateinit var handler: DeviceHandler
    private lateinit var mockController: MockDeviceController

    @BeforeTest
    fun setup() {
        mockController = MockDeviceController()
        handler = DeviceHandler(mockController)
    }

    // ==================== handleCommand "volume up" Tests ====================

    @Test
    fun `handleCommand volume up increases volume by VOLUME_STEP`() {
        val initialVolume = handler.getCurrentVolume()
        val result = handler.handleCommand("volume up")

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_UP, result.control)
        assertEquals(initialVolume + DeviceHandler.VOLUME_STEP, handler.getCurrentVolume())
        assertNotNull(result.currentValue)
        assertEquals(initialVolume + DeviceHandler.VOLUME_STEP, result.currentValue)
        assertNull(result.error)
    }

    @Test
    fun `handleCommand VOLUME UP is case insensitive`() {
        val initialVolume = handler.getCurrentVolume()
        val result = handler.handleCommand("VOLUME UP")

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_UP, result.control)
        assertEquals(initialVolume + DeviceHandler.VOLUME_STEP, handler.getCurrentVolume())
    }

    @Test
    fun `handleCommand volume up with whitespace is trimmed`() {
        val initialVolume = handler.getCurrentVolume()
        val result = handler.handleCommand("  volume up  ")

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_UP, result.control)
        assertEquals(initialVolume + DeviceHandler.VOLUME_STEP, handler.getCurrentVolume())
    }

    @Test
    fun `handleCommand volume up does not exceed 100`() {
        // Set volume near max
        handler = DeviceHandler(mockController, initialVolume = 95)
        val result = handler.handleCommand("volume up")

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_UP, result.control)
        assertEquals(100, handler.getCurrentVolume())
        assertEquals(100, result.currentValue)
    }

    @Test
    fun `handleCommand volume up at max stays at 100`() {
        handler = DeviceHandler(mockController, initialVolume = 100)
        val result = handler.handleCommand("volume up")

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_UP, result.control)
        assertEquals(100, handler.getCurrentVolume())
    }

    // ==================== handleCommand "volume down" Tests ====================

    @Test
    fun `handleCommand volume down decreases volume by VOLUME_STEP`() {
        val initialVolume = handler.getCurrentVolume()
        val result = handler.handleCommand("volume down")

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_DOWN, result.control)
        assertEquals(initialVolume - DeviceHandler.VOLUME_STEP, handler.getCurrentVolume())
        assertEquals(initialVolume - DeviceHandler.VOLUME_STEP, result.currentValue)
        assertNull(result.error)
    }

    @Test
    fun `handleCommand volume down does not go below 0`() {
        handler = DeviceHandler(mockController, initialVolume = 5)
        val result = handler.handleCommand("volume down")

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_DOWN, result.control)
        assertEquals(0, handler.getCurrentVolume())
        assertEquals(0, result.currentValue)
    }

    @Test
    fun `handleCommand volume down at zero stays at 0`() {
        handler = DeviceHandler(mockController, initialVolume = 0)
        val result = handler.handleCommand("volume down")

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_DOWN, result.control)
        assertEquals(0, handler.getCurrentVolume())
    }

    // ==================== handleCommand "mute" Tests ====================

    @Test
    fun `handleCommand mute sets volume to 0`() {
        handler = DeviceHandler(mockController, initialVolume = 75)
        val result = handler.handleCommand("mute")

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_MUTE, result.control)
        assertEquals(0, handler.getCurrentVolume())
        assertEquals(0, result.currentValue)
        assertNull(result.error)
    }

    @Test
    fun `handleCommand mute when already muted stays at 0`() {
        handler = DeviceHandler(mockController, initialVolume = 0)
        val result = handler.handleCommand("mute")

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_MUTE, result.control)
        assertEquals(0, handler.getCurrentVolume())
    }

    // ==================== handleCommand "set volume N" Tests ====================

    @Test
    fun `handleCommand set volume 75 sets exact volume`() {
        val result = handler.handleCommand("set volume 75")

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_UP, result.control) // Uses VOLUME_UP for set operations
        assertEquals(75, handler.getCurrentVolume())
        assertEquals(75, result.currentValue)
    }

    @Test
    fun `handleCommand set volume 0 sets volume to minimum`() {
        val result = handler.handleCommand("set volume 0")

        assertTrue(result.success)
        assertEquals(0, handler.getCurrentVolume())
    }

    @Test
    fun `handleCommand set volume 100 sets volume to maximum`() {
        val result = handler.handleCommand("set volume 100")

        assertTrue(result.success)
        assertEquals(100, handler.getCurrentVolume())
    }

    @Test
    fun `handleCommand set volume above 100 clamps to 100`() {
        val result = handler.handleCommand("set volume 150")

        assertTrue(result.success)
        assertEquals(100, handler.getCurrentVolume())
    }

    @Test
    fun `handleCommand set volume below 0 clamps to 0`() {
        val result = handler.handleCommand("set volume -10")

        assertTrue(result.success)
        assertEquals(0, handler.getCurrentVolume())
    }

    @Test
    fun `handleCommand set volume with invalid number returns error`() {
        val result = handler.handleCommand("set volume abc")

        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Invalid volume"))
    }

    // ==================== handleCommand "brightness up" Tests ====================

    @Test
    fun `handleCommand brightness up increases brightness by BRIGHTNESS_STEP`() {
        val initialBrightness = handler.getCurrentBrightness()
        val result = handler.handleCommand("brightness up")

        assertTrue(result.success)
        assertEquals(DeviceControl.BRIGHTNESS_UP, result.control)
        assertEquals(initialBrightness + DeviceHandler.BRIGHTNESS_STEP, handler.getCurrentBrightness())
        assertEquals(initialBrightness + DeviceHandler.BRIGHTNESS_STEP, result.currentValue)
        assertNull(result.error)
    }

    @Test
    fun `handleCommand brightness up does not exceed 100`() {
        handler = DeviceHandler(mockController, initialBrightness = 95)
        val result = handler.handleCommand("brightness up")

        assertTrue(result.success)
        assertEquals(DeviceControl.BRIGHTNESS_UP, result.control)
        assertEquals(100, handler.getCurrentBrightness())
    }

    // ==================== handleCommand "brightness down" Tests ====================

    @Test
    fun `handleCommand brightness down decreases brightness by BRIGHTNESS_STEP`() {
        val initialBrightness = handler.getCurrentBrightness()
        val result = handler.handleCommand("brightness down")

        assertTrue(result.success)
        assertEquals(DeviceControl.BRIGHTNESS_DOWN, result.control)
        assertEquals(initialBrightness - DeviceHandler.BRIGHTNESS_STEP, handler.getCurrentBrightness())
        assertEquals(initialBrightness - DeviceHandler.BRIGHTNESS_STEP, result.currentValue)
        assertNull(result.error)
    }

    @Test
    fun `handleCommand brightness down does not go below 0`() {
        handler = DeviceHandler(mockController, initialBrightness = 5)
        val result = handler.handleCommand("brightness down")

        assertTrue(result.success)
        assertEquals(DeviceControl.BRIGHTNESS_DOWN, result.control)
        assertEquals(0, handler.getCurrentBrightness())
    }

    // ==================== handleCommand "set brightness N" Tests ====================

    @Test
    fun `handleCommand set brightness 80 sets exact brightness`() {
        val result = handler.handleCommand("set brightness 80")

        assertTrue(result.success)
        assertEquals(80, handler.getCurrentBrightness())
        assertEquals(80, result.currentValue)
    }

    @Test
    fun `handleCommand set brightness above 100 clamps to 100`() {
        val result = handler.handleCommand("set brightness 120")

        assertTrue(result.success)
        assertEquals(100, handler.getCurrentBrightness())
    }

    @Test
    fun `handleCommand set brightness below 0 clamps to 0`() {
        val result = handler.handleCommand("set brightness -5")

        assertTrue(result.success)
        assertEquals(0, handler.getCurrentBrightness())
    }

    // ==================== handleCommand "screen on/off" Tests ====================

    @Test
    fun `handleCommand screen on returns SCREEN_ON control`() {
        val result = handler.handleCommand("screen on")

        assertTrue(result.success)
        assertEquals(DeviceControl.SCREEN_ON, result.control)
        assertNull(result.currentValue) // Screen state doesn't have a numeric value
        assertNull(result.error)
    }

    @Test
    fun `handleCommand screen off returns SCREEN_OFF control`() {
        val result = handler.handleCommand("screen off")

        assertTrue(result.success)
        assertEquals(DeviceControl.SCREEN_OFF, result.control)
        assertNull(result.currentValue)
        assertNull(result.error)
    }

    // ==================== handleCommand "flashlight" Tests ====================

    @Test
    fun `handleCommand flashlight on turns flashlight on`() {
        val result = handler.handleCommand("flashlight on")

        assertTrue(result.success)
        assertEquals(DeviceControl.FLASHLIGHT_ON, result.control)
        assertTrue(handler.isFlashlightOn())
        assertNull(result.error)
    }

    @Test
    fun `handleCommand turn on flashlight is alias for flashlight on`() {
        val result = handler.handleCommand("turn on flashlight")

        assertTrue(result.success)
        assertEquals(DeviceControl.FLASHLIGHT_ON, result.control)
        assertTrue(handler.isFlashlightOn())
    }

    @Test
    fun `handleCommand flashlight off turns flashlight off`() {
        // First turn on
        handler.handleCommand("flashlight on")
        assertTrue(handler.isFlashlightOn())

        // Then turn off
        val result = handler.handleCommand("flashlight off")

        assertTrue(result.success)
        assertEquals(DeviceControl.FLASHLIGHT_OFF, result.control)
        assertFalse(handler.isFlashlightOn())
    }

    @Test
    fun `handleCommand turn off flashlight is alias for flashlight off`() {
        handler.handleCommand("flashlight on")
        val result = handler.handleCommand("turn off flashlight")

        assertTrue(result.success)
        assertEquals(DeviceControl.FLASHLIGHT_OFF, result.control)
        assertFalse(handler.isFlashlightOn())
    }

    @Test
    fun `handleCommand toggle flashlight toggles from off to on`() {
        assertFalse(handler.isFlashlightOn())

        val result = handler.handleCommand("toggle flashlight")

        assertTrue(result.success)
        assertEquals(DeviceControl.FLASHLIGHT_TOGGLE, result.control)
        assertTrue(handler.isFlashlightOn())
    }

    @Test
    fun `handleCommand toggle flashlight toggles from on to off`() {
        handler.handleCommand("flashlight on")
        assertTrue(handler.isFlashlightOn())

        val result = handler.handleCommand("toggle flashlight")

        assertTrue(result.success)
        assertEquals(DeviceControl.FLASHLIGHT_TOGGLE, result.control)
        assertFalse(handler.isFlashlightOn())
    }

    // ==================== handleCommand Unknown Command Tests ====================

    @Test
    fun `handleCommand unknown returns error`() {
        val result = handler.handleCommand("do something random")

        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Unknown device command"))
    }

    @Test
    fun `handleCommand empty string returns error`() {
        val result = handler.handleCommand("")

        assertFalse(result.success)
        assertNotNull(result.error)
    }

    // ==================== Direct Method Tests ====================

    @Test
    fun `volumeUp method increases volume`() {
        val initial = handler.getCurrentVolume()
        val result = handler.volumeUp()

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_UP, result.control)
        assertEquals(initial + DeviceHandler.VOLUME_STEP, handler.getCurrentVolume())
    }

    @Test
    fun `volumeDown method decreases volume`() {
        val initial = handler.getCurrentVolume()
        val result = handler.volumeDown()

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_DOWN, result.control)
        assertEquals(initial - DeviceHandler.VOLUME_STEP, handler.getCurrentVolume())
    }

    @Test
    fun `mute method sets volume to zero`() {
        handler = DeviceHandler(mockController, initialVolume = 80)
        val result = handler.mute()

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_MUTE, result.control)
        assertEquals(0, handler.getCurrentVolume())
    }

    @Test
    fun `brightnessUp method increases brightness`() {
        val initial = handler.getCurrentBrightness()
        val result = handler.brightnessUp()

        assertTrue(result.success)
        assertEquals(DeviceControl.BRIGHTNESS_UP, result.control)
        assertEquals(initial + DeviceHandler.BRIGHTNESS_STEP, handler.getCurrentBrightness())
    }

    @Test
    fun `brightnessDown method decreases brightness`() {
        val initial = handler.getCurrentBrightness()
        val result = handler.brightnessDown()

        assertTrue(result.success)
        assertEquals(DeviceControl.BRIGHTNESS_DOWN, result.control)
        assertEquals(initial - DeviceHandler.BRIGHTNESS_STEP, handler.getCurrentBrightness())
    }

    @Test
    fun `screenOn method returns success`() {
        val result = handler.screenOn()

        assertTrue(result.success)
        assertEquals(DeviceControl.SCREEN_ON, result.control)
    }

    @Test
    fun `screenOff method returns success`() {
        val result = handler.screenOff()

        assertTrue(result.success)
        assertEquals(DeviceControl.SCREEN_OFF, result.control)
    }

    @Test
    fun `flashlightOn method turns on flashlight`() {
        val result = handler.flashlightOn()

        assertTrue(result.success)
        assertEquals(DeviceControl.FLASHLIGHT_ON, result.control)
        assertTrue(handler.isFlashlightOn())
    }

    @Test
    fun `flashlightOff method turns off flashlight`() {
        handler.flashlightOn()
        val result = handler.flashlightOff()

        assertTrue(result.success)
        assertEquals(DeviceControl.FLASHLIGHT_OFF, result.control)
        assertFalse(handler.isFlashlightOn())
    }

    @Test
    fun `toggleFlashlight method toggles state`() {
        assertFalse(handler.isFlashlightOn())

        handler.toggleFlashlight()
        assertTrue(handler.isFlashlightOn())

        handler.toggleFlashlight()
        assertFalse(handler.isFlashlightOn())
    }

    // ==================== DeviceControl Enum Tests ====================

    @Test
    fun `DeviceControl enum has all expected values`() {
        val expectedControls = listOf(
            "VOLUME_UP", "VOLUME_DOWN", "VOLUME_MUTE",
            "BRIGHTNESS_UP", "BRIGHTNESS_DOWN",
            "SCREEN_ON", "SCREEN_OFF",
            "FLASHLIGHT_ON", "FLASHLIGHT_OFF", "FLASHLIGHT_TOGGLE"
        )

        val actualControls = DeviceControl.entries.map { it.name }

        assertEquals(expectedControls.size, actualControls.size)
        expectedControls.forEach { expected ->
            assertTrue(actualControls.contains(expected), "Missing DeviceControl: $expected")
        }
    }

    // ==================== DeviceControlResult Tests ====================

    @Test
    fun `DeviceControlResult success case`() {
        val result = DeviceControlResult(
            success = true,
            control = DeviceControl.VOLUME_UP,
            currentValue = 60
        )

        assertTrue(result.success)
        assertEquals(DeviceControl.VOLUME_UP, result.control)
        assertEquals(60, result.currentValue)
        assertNull(result.error)
    }

    @Test
    fun `DeviceControlResult failure case`() {
        val result = DeviceControlResult(
            success = false,
            control = DeviceControl.FLASHLIGHT_ON,
            error = "Flashlight not available"
        )

        assertFalse(result.success)
        assertEquals(DeviceControl.FLASHLIGHT_ON, result.control)
        assertNull(result.currentValue)
        assertEquals("Flashlight not available", result.error)
    }

    // ==================== DeviceController Integration Tests ====================

    @Test
    fun `volumeUp calls controller setVolume`() {
        handler.volumeUp()

        assertTrue(mockController.setVolumeCalled)
    }

    @Test
    fun `brightnessUp calls controller setBrightness`() {
        handler.brightnessUp()

        assertTrue(mockController.setBrightnessCalled)
    }

    @Test
    fun `screenOn calls controller setScreenState`() {
        handler.screenOn()

        assertTrue(mockController.setScreenStateCalled)
    }

    @Test
    fun `flashlightOn calls controller setFlashlight`() {
        handler.flashlightOn()

        assertTrue(mockController.setFlashlightCalled)
    }

    // ==================== Handler Without Controller Tests ====================

    @Test
    fun `handler works without controller for internal state`() {
        val handlerNoController = DeviceHandler()
        val initialVolume = handlerNoController.getCurrentVolume()

        val result = handlerNoController.volumeUp()

        assertTrue(result.success)
        assertEquals(initialVolume + DeviceHandler.VOLUME_STEP, handlerNoController.getCurrentVolume())
    }

    // ==================== Constants Tests ====================

    @Test
    fun `VOLUME_STEP is 10`() {
        assertEquals(10, DeviceHandler.VOLUME_STEP)
    }

    @Test
    fun `BRIGHTNESS_STEP is 10`() {
        assertEquals(10, DeviceHandler.BRIGHTNESS_STEP)
    }

    // ==================== Initial State Tests ====================

    @Test
    fun `default initial volume is 50`() {
        val defaultHandler = DeviceHandler()
        assertEquals(50, defaultHandler.getCurrentVolume())
    }

    @Test
    fun `default initial brightness is 50`() {
        val defaultHandler = DeviceHandler()
        assertEquals(50, defaultHandler.getCurrentBrightness())
    }

    @Test
    fun `default flashlight state is off`() {
        val defaultHandler = DeviceHandler()
        assertFalse(defaultHandler.isFlashlightOn())
    }

    @Test
    fun `can set custom initial volume`() {
        val customHandler = DeviceHandler(mockController, initialVolume = 75)
        assertEquals(75, customHandler.getCurrentVolume())
    }

    @Test
    fun `can set custom initial brightness`() {
        val customHandler = DeviceHandler(mockController, initialBrightness = 80)
        assertEquals(80, customHandler.getCurrentBrightness())
    }
}
