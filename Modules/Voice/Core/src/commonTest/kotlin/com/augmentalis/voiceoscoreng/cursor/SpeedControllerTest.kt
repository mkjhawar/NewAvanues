package com.augmentalis.voiceoscoreng.cursor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD tests for SpeedController class.
 *
 * SpeedController manages cursor movement speed for the VoiceOS cursor system.
 * It provides multiple speed levels (SLOW, NORMAL, FAST, VERY_FAST) with
 * corresponding multipliers and pixel-per-step calculations.
 */
class SpeedControllerTest {

    // ==================== Initial State Tests ====================

    @Test
    fun `default constructor should set initial level to NORMAL`() {
        val controller = SpeedController()
        assertEquals(SpeedLevel.NORMAL, controller.getLevel())
    }

    @Test
    fun `constructor with custom level should set initial level correctly`() {
        val controllerSlow = SpeedController(SpeedLevel.SLOW)
        assertEquals(SpeedLevel.SLOW, controllerSlow.getLevel())

        val controllerFast = SpeedController(SpeedLevel.FAST)
        assertEquals(SpeedLevel.FAST, controllerFast.getLevel())

        val controllerVeryFast = SpeedController(SpeedLevel.VERY_FAST)
        assertEquals(SpeedLevel.VERY_FAST, controllerVeryFast.getLevel())
    }

    // ==================== SpeedLevel Enum Tests ====================

    @Test
    fun `SpeedLevel should have exactly four values`() {
        val entries = SpeedLevel.entries
        assertEquals(4, entries.size)
    }

    @Test
    fun `SpeedLevel should contain SLOW`() {
        assertNotNull(SpeedLevel.SLOW)
        assertEquals("SLOW", SpeedLevel.SLOW.name)
    }

    @Test
    fun `SpeedLevel should contain NORMAL`() {
        assertNotNull(SpeedLevel.NORMAL)
        assertEquals("NORMAL", SpeedLevel.NORMAL.name)
    }

    @Test
    fun `SpeedLevel should contain FAST`() {
        assertNotNull(SpeedLevel.FAST)
        assertEquals("FAST", SpeedLevel.FAST.name)
    }

    @Test
    fun `SpeedLevel should contain VERY_FAST`() {
        assertNotNull(SpeedLevel.VERY_FAST)
        assertEquals("VERY_FAST", SpeedLevel.VERY_FAST.name)
    }

    // ==================== setLevel Tests ====================

    @Test
    fun `setLevel should update current level`() {
        val controller = SpeedController()

        controller.setLevel(SpeedLevel.SLOW)
        assertEquals(SpeedLevel.SLOW, controller.getLevel())

        controller.setLevel(SpeedLevel.FAST)
        assertEquals(SpeedLevel.FAST, controller.getLevel())

        controller.setLevel(SpeedLevel.VERY_FAST)
        assertEquals(SpeedLevel.VERY_FAST, controller.getLevel())

        controller.setLevel(SpeedLevel.NORMAL)
        assertEquals(SpeedLevel.NORMAL, controller.getLevel())
    }

    // ==================== getMultiplier Tests ====================

    @Test
    fun `getMultiplier should return 0_5f for SLOW`() {
        val controller = SpeedController(SpeedLevel.SLOW)
        assertEquals(0.5f, controller.getMultiplier())
    }

    @Test
    fun `getMultiplier should return 1_0f for NORMAL`() {
        val controller = SpeedController(SpeedLevel.NORMAL)
        assertEquals(1.0f, controller.getMultiplier())
    }

    @Test
    fun `getMultiplier should return 2_0f for FAST`() {
        val controller = SpeedController(SpeedLevel.FAST)
        assertEquals(2.0f, controller.getMultiplier())
    }

    @Test
    fun `getMultiplier should return 4_0f for VERY_FAST`() {
        val controller = SpeedController(SpeedLevel.VERY_FAST)
        assertEquals(4.0f, controller.getMultiplier())
    }

    // ==================== getPixelsPerStep Tests ====================

    @Test
    fun `getPixelsPerStep should return 5 for SLOW`() {
        val controller = SpeedController(SpeedLevel.SLOW)
        // BASE_SPEED (10) * 0.5 = 5
        assertEquals(5, controller.getPixelsPerStep())
    }

    @Test
    fun `getPixelsPerStep should return 10 for NORMAL`() {
        val controller = SpeedController(SpeedLevel.NORMAL)
        // BASE_SPEED (10) * 1.0 = 10
        assertEquals(10, controller.getPixelsPerStep())
    }

    @Test
    fun `getPixelsPerStep should return 20 for FAST`() {
        val controller = SpeedController(SpeedLevel.FAST)
        // BASE_SPEED (10) * 2.0 = 20
        assertEquals(20, controller.getPixelsPerStep())
    }

    @Test
    fun `getPixelsPerStep should return 40 for VERY_FAST`() {
        val controller = SpeedController(SpeedLevel.VERY_FAST)
        // BASE_SPEED (10) * 4.0 = 40
        assertEquals(40, controller.getPixelsPerStep())
    }

    @Test
    fun `BASE_SPEED constant should be 10`() {
        assertEquals(10, SpeedController.BASE_SPEED)
    }

    // ==================== increaseSpeed Tests ====================

    @Test
    fun `increaseSpeed from SLOW should return NORMAL`() {
        val controller = SpeedController(SpeedLevel.SLOW)
        val result = controller.increaseSpeed()
        assertEquals(SpeedLevel.NORMAL, result)
        assertEquals(SpeedLevel.NORMAL, controller.getLevel())
    }

    @Test
    fun `increaseSpeed from NORMAL should return FAST`() {
        val controller = SpeedController(SpeedLevel.NORMAL)
        val result = controller.increaseSpeed()
        assertEquals(SpeedLevel.FAST, result)
        assertEquals(SpeedLevel.FAST, controller.getLevel())
    }

    @Test
    fun `increaseSpeed from FAST should return VERY_FAST`() {
        val controller = SpeedController(SpeedLevel.FAST)
        val result = controller.increaseSpeed()
        assertEquals(SpeedLevel.VERY_FAST, result)
        assertEquals(SpeedLevel.VERY_FAST, controller.getLevel())
    }

    @Test
    fun `increaseSpeed from VERY_FAST should stay at VERY_FAST`() {
        val controller = SpeedController(SpeedLevel.VERY_FAST)
        val result = controller.increaseSpeed()
        assertEquals(SpeedLevel.VERY_FAST, result)
        assertEquals(SpeedLevel.VERY_FAST, controller.getLevel())
    }

    @Test
    fun `increaseSpeed at max should not change level`() {
        val controller = SpeedController(SpeedLevel.VERY_FAST)

        // Call multiple times to ensure it stays at max
        controller.increaseSpeed()
        controller.increaseSpeed()
        controller.increaseSpeed()

        assertEquals(SpeedLevel.VERY_FAST, controller.getLevel())
    }

    // ==================== decreaseSpeed Tests ====================

    @Test
    fun `decreaseSpeed from VERY_FAST should return FAST`() {
        val controller = SpeedController(SpeedLevel.VERY_FAST)
        val result = controller.decreaseSpeed()
        assertEquals(SpeedLevel.FAST, result)
        assertEquals(SpeedLevel.FAST, controller.getLevel())
    }

    @Test
    fun `decreaseSpeed from FAST should return NORMAL`() {
        val controller = SpeedController(SpeedLevel.FAST)
        val result = controller.decreaseSpeed()
        assertEquals(SpeedLevel.NORMAL, result)
        assertEquals(SpeedLevel.NORMAL, controller.getLevel())
    }

    @Test
    fun `decreaseSpeed from NORMAL should return SLOW`() {
        val controller = SpeedController(SpeedLevel.NORMAL)
        val result = controller.decreaseSpeed()
        assertEquals(SpeedLevel.SLOW, result)
        assertEquals(SpeedLevel.SLOW, controller.getLevel())
    }

    @Test
    fun `decreaseSpeed from SLOW should stay at SLOW`() {
        val controller = SpeedController(SpeedLevel.SLOW)
        val result = controller.decreaseSpeed()
        assertEquals(SpeedLevel.SLOW, result)
        assertEquals(SpeedLevel.SLOW, controller.getLevel())
    }

    @Test
    fun `decreaseSpeed at min should not change level`() {
        val controller = SpeedController(SpeedLevel.SLOW)

        // Call multiple times to ensure it stays at min
        controller.decreaseSpeed()
        controller.decreaseSpeed()
        controller.decreaseSpeed()

        assertEquals(SpeedLevel.SLOW, controller.getLevel())
    }

    // ==================== reset Tests ====================

    @Test
    fun `reset should return to NORMAL from SLOW`() {
        val controller = SpeedController(SpeedLevel.SLOW)
        controller.reset()
        assertEquals(SpeedLevel.NORMAL, controller.getLevel())
    }

    @Test
    fun `reset should return to NORMAL from FAST`() {
        val controller = SpeedController(SpeedLevel.FAST)
        controller.reset()
        assertEquals(SpeedLevel.NORMAL, controller.getLevel())
    }

    @Test
    fun `reset should return to NORMAL from VERY_FAST`() {
        val controller = SpeedController(SpeedLevel.VERY_FAST)
        controller.reset()
        assertEquals(SpeedLevel.NORMAL, controller.getLevel())
    }

    @Test
    fun `reset should keep NORMAL if already NORMAL`() {
        val controller = SpeedController(SpeedLevel.NORMAL)
        controller.reset()
        assertEquals(SpeedLevel.NORMAL, controller.getLevel())
    }

    // ==================== Integration Tests ====================

    @Test
    fun `full speed cycle should work correctly`() {
        val controller = SpeedController(SpeedLevel.SLOW)

        // Increase through all levels
        assertEquals(SpeedLevel.SLOW, controller.getLevel())

        controller.increaseSpeed()
        assertEquals(SpeedLevel.NORMAL, controller.getLevel())

        controller.increaseSpeed()
        assertEquals(SpeedLevel.FAST, controller.getLevel())

        controller.increaseSpeed()
        assertEquals(SpeedLevel.VERY_FAST, controller.getLevel())

        // Decrease back down
        controller.decreaseSpeed()
        assertEquals(SpeedLevel.FAST, controller.getLevel())

        controller.decreaseSpeed()
        assertEquals(SpeedLevel.NORMAL, controller.getLevel())

        controller.decreaseSpeed()
        assertEquals(SpeedLevel.SLOW, controller.getLevel())
    }

    @Test
    fun `multiplier and pixels should update with level changes`() {
        val controller = SpeedController(SpeedLevel.NORMAL)

        assertEquals(1.0f, controller.getMultiplier())
        assertEquals(10, controller.getPixelsPerStep())

        controller.increaseSpeed()
        assertEquals(2.0f, controller.getMultiplier())
        assertEquals(20, controller.getPixelsPerStep())

        controller.reset()
        assertEquals(1.0f, controller.getMultiplier())
        assertEquals(10, controller.getPixelsPerStep())
    }

    @Test
    fun `all speed levels should have positive multipliers`() {
        SpeedLevel.entries.forEach { level ->
            val controller = SpeedController(level)
            assertTrue(controller.getMultiplier() > 0f, "Multiplier for $level should be positive")
        }
    }

    @Test
    fun `all speed levels should have positive pixels per step`() {
        SpeedLevel.entries.forEach { level ->
            val controller = SpeedController(level)
            assertTrue(controller.getPixelsPerStep() > 0, "Pixels per step for $level should be positive")
        }
    }

    @Test
    fun `speed multipliers should increase with level`() {
        val slow = SpeedController(SpeedLevel.SLOW).getMultiplier()
        val normal = SpeedController(SpeedLevel.NORMAL).getMultiplier()
        val fast = SpeedController(SpeedLevel.FAST).getMultiplier()
        val veryFast = SpeedController(SpeedLevel.VERY_FAST).getMultiplier()

        assertTrue(slow < normal, "SLOW multiplier should be less than NORMAL")
        assertTrue(normal < fast, "NORMAL multiplier should be less than FAST")
        assertTrue(fast < veryFast, "FAST multiplier should be less than VERY_FAST")
    }
}
