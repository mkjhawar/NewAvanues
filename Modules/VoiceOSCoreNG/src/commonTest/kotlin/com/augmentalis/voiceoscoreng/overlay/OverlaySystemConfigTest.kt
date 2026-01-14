/**
 * OverlaySystemConfigTest.kt - TDD tests for overlay system configuration data classes
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-01-06
 *
 * TDD RED phase: Tests written first before implementation.
 * These tests define the expected behavior of:
 * - OverlaySystemConfig data class
 * - OverlayPosition enum
 * - AccessibilityConfig data class
 */
package com.augmentalis.voiceoscoreng.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for OverlaySystemConfig data class
 */
class OverlaySystemConfigTest {

    // ==================== Default Values Tests ====================

    @Test
    fun `default config has enabled true`() {
        val config = OverlaySystemConfig()
        assertTrue(config.enabled)
    }

    @Test
    fun `default config has opacity 1_0f`() {
        val config = OverlaySystemConfig()
        assertEquals(1.0f, config.opacity, 0.001f)
    }

    @Test
    fun `default config has animationDuration 200L`() {
        val config = OverlaySystemConfig()
        assertEquals(200L, config.animationDuration)
    }

    @Test
    fun `default config has touchPassthrough false`() {
        val config = OverlaySystemConfig()
        assertFalse(config.touchPassthrough)
    }

    @Test
    fun `default config has autoHideDelay 0L`() {
        val config = OverlaySystemConfig()
        assertEquals(0L, config.autoHideDelay)
    }

    @Test
    fun `default config has position CENTER`() {
        val config = OverlaySystemConfig()
        assertEquals(OverlayPosition.CENTER, config.position)
    }

    @Test
    fun `default config has default AccessibilityConfig`() {
        val config = OverlaySystemConfig()
        assertEquals(AccessibilityConfig(), config.accessibility)
    }

    // ==================== Copy with Modifications Tests ====================

    @Test
    fun `copy with enabled false`() {
        val config = OverlaySystemConfig()
        val modified = config.copy(enabled = false)

        assertFalse(modified.enabled)
        assertTrue(config.enabled) // Original unchanged
    }

    @Test
    fun `copy with opacity 0_5f`() {
        val config = OverlaySystemConfig()
        val modified = config.copy(opacity = 0.5f)

        assertEquals(0.5f, modified.opacity, 0.001f)
        assertEquals(1.0f, config.opacity, 0.001f) // Original unchanged
    }

    @Test
    fun `copy with animationDuration 500L`() {
        val config = OverlaySystemConfig()
        val modified = config.copy(animationDuration = 500L)

        assertEquals(500L, modified.animationDuration)
        assertEquals(200L, config.animationDuration) // Original unchanged
    }

    @Test
    fun `copy with touchPassthrough true`() {
        val config = OverlaySystemConfig()
        val modified = config.copy(touchPassthrough = true)

        assertTrue(modified.touchPassthrough)
        assertFalse(config.touchPassthrough) // Original unchanged
    }

    @Test
    fun `copy with autoHideDelay 3000L`() {
        val config = OverlaySystemConfig()
        val modified = config.copy(autoHideDelay = 3000L)

        assertEquals(3000L, modified.autoHideDelay)
        assertEquals(0L, config.autoHideDelay) // Original unchanged
    }

    @Test
    fun `copy with position TOP_LEFT`() {
        val config = OverlaySystemConfig()
        val modified = config.copy(position = OverlayPosition.TOP_LEFT)

        assertEquals(OverlayPosition.TOP_LEFT, modified.position)
        assertEquals(OverlayPosition.CENTER, config.position) // Original unchanged
    }

    @Test
    fun `copy with custom accessibility config`() {
        val config = OverlaySystemConfig()
        val customAccessibility = AccessibilityConfig(largeText = true)
        val modified = config.copy(accessibility = customAccessibility)

        assertTrue(modified.accessibility.largeText)
        assertFalse(config.accessibility.largeText) // Original unchanged
    }

    @Test
    fun `copy with multiple modifications`() {
        val config = OverlaySystemConfig()
        val modified = config.copy(
            enabled = false,
            opacity = 0.8f,
            position = OverlayPosition.BOTTOM_CENTER,
            accessibility = AccessibilityConfig(highContrast = true)
        )

        assertFalse(modified.enabled)
        assertEquals(0.8f, modified.opacity, 0.001f)
        assertEquals(OverlayPosition.BOTTOM_CENTER, modified.position)
        assertTrue(modified.accessibility.highContrast)
    }

    // ==================== Equality Tests ====================

    @Test
    fun `two default configs are equal`() {
        val config1 = OverlaySystemConfig()
        val config2 = OverlaySystemConfig()

        assertEquals(config1, config2)
    }

    @Test
    fun `configs with different values are not equal`() {
        val config1 = OverlaySystemConfig()
        val config2 = OverlaySystemConfig(enabled = false)

        assertNotEquals(config1, config2)
    }

    @Test
    fun `hashCode is consistent for equal configs`() {
        val config1 = OverlaySystemConfig(opacity = 0.7f)
        val config2 = OverlaySystemConfig(opacity = 0.7f)

        assertEquals(config1.hashCode(), config2.hashCode())
    }

    // ==================== Opacity Bounds Tests ====================

    @Test
    fun `opacity can be set to 0_0f - fully transparent`() {
        val config = OverlaySystemConfig(opacity = 0.0f)
        assertEquals(0.0f, config.opacity, 0.001f)
    }

    @Test
    fun `opacity can be set to 1_0f - fully opaque`() {
        val config = OverlaySystemConfig(opacity = 1.0f)
        assertEquals(1.0f, config.opacity, 0.001f)
    }

    @Test
    fun `opacity can be set to 0_5f - half transparent`() {
        val config = OverlaySystemConfig(opacity = 0.5f)
        assertEquals(0.5f, config.opacity, 0.001f)
    }

    @Test
    fun `opacity can be set to boundary value 0_25f`() {
        val config = OverlaySystemConfig(opacity = 0.25f)
        assertEquals(0.25f, config.opacity, 0.001f)
    }

    @Test
    fun `opacity can be set to boundary value 0_75f`() {
        val config = OverlaySystemConfig(opacity = 0.75f)
        assertEquals(0.75f, config.opacity, 0.001f)
    }

    // ==================== Auto-Hide Tests ====================

    @Test
    fun `autoHideDelay of 0 means no auto-hide`() {
        val config = OverlaySystemConfig(autoHideDelay = 0L)
        assertEquals(0L, config.autoHideDelay)
    }

    @Test
    fun `autoHideDelay can be set to positive value`() {
        val config = OverlaySystemConfig(autoHideDelay = 5000L)
        assertEquals(5000L, config.autoHideDelay)
    }

    // ==================== toString Tests ====================

    @Test
    fun `toString contains class name`() {
        val config = OverlaySystemConfig()
        assertTrue(config.toString().contains("OverlaySystemConfig"))
    }

    @Test
    fun `toString contains property values`() {
        val config = OverlaySystemConfig(enabled = false, opacity = 0.5f)
        val str = config.toString()
        assertTrue(str.contains("enabled=false"))
        assertTrue(str.contains("opacity=0.5"))
    }
}

/**
 * Tests for OverlayPosition enum
 */
class OverlayPositionTest {

    @Test
    fun `TOP_LEFT exists`() {
        assertEquals(OverlayPosition.TOP_LEFT, OverlayPosition.valueOf("TOP_LEFT"))
    }

    @Test
    fun `TOP_CENTER exists`() {
        assertEquals(OverlayPosition.TOP_CENTER, OverlayPosition.valueOf("TOP_CENTER"))
    }

    @Test
    fun `TOP_RIGHT exists`() {
        assertEquals(OverlayPosition.TOP_RIGHT, OverlayPosition.valueOf("TOP_RIGHT"))
    }

    @Test
    fun `CENTER_LEFT exists`() {
        assertEquals(OverlayPosition.CENTER_LEFT, OverlayPosition.valueOf("CENTER_LEFT"))
    }

    @Test
    fun `CENTER exists`() {
        assertEquals(OverlayPosition.CENTER, OverlayPosition.valueOf("CENTER"))
    }

    @Test
    fun `CENTER_RIGHT exists`() {
        assertEquals(OverlayPosition.CENTER_RIGHT, OverlayPosition.valueOf("CENTER_RIGHT"))
    }

    @Test
    fun `BOTTOM_LEFT exists`() {
        assertEquals(OverlayPosition.BOTTOM_LEFT, OverlayPosition.valueOf("BOTTOM_LEFT"))
    }

    @Test
    fun `BOTTOM_CENTER exists`() {
        assertEquals(OverlayPosition.BOTTOM_CENTER, OverlayPosition.valueOf("BOTTOM_CENTER"))
    }

    @Test
    fun `BOTTOM_RIGHT exists`() {
        assertEquals(OverlayPosition.BOTTOM_RIGHT, OverlayPosition.valueOf("BOTTOM_RIGHT"))
    }

    @Test
    fun `all 9 positions exist`() {
        val positions = OverlayPosition.entries
        assertEquals(9, positions.size)
    }

    @Test
    fun `positions are in correct order`() {
        val positions = OverlayPosition.entries
        assertEquals(OverlayPosition.TOP_LEFT, positions[0])
        assertEquals(OverlayPosition.TOP_CENTER, positions[1])
        assertEquals(OverlayPosition.TOP_RIGHT, positions[2])
        assertEquals(OverlayPosition.CENTER_LEFT, positions[3])
        assertEquals(OverlayPosition.CENTER, positions[4])
        assertEquals(OverlayPosition.CENTER_RIGHT, positions[5])
        assertEquals(OverlayPosition.BOTTOM_LEFT, positions[6])
        assertEquals(OverlayPosition.BOTTOM_CENTER, positions[7])
        assertEquals(OverlayPosition.BOTTOM_RIGHT, positions[8])
    }
}

/**
 * Tests for AccessibilityConfig data class
 */
class AccessibilityConfigTest {

    // ==================== Default Values Tests ====================

    @Test
    fun `default config has largeText false`() {
        val config = AccessibilityConfig()
        assertFalse(config.largeText)
    }

    @Test
    fun `default config has highContrast false`() {
        val config = AccessibilityConfig()
        assertFalse(config.highContrast)
    }

    @Test
    fun `default config has reduceMotion false`() {
        val config = AccessibilityConfig()
        assertFalse(config.reduceMotion)
    }

    // ==================== Copy with Modifications Tests ====================

    @Test
    fun `copy with largeText true`() {
        val config = AccessibilityConfig()
        val modified = config.copy(largeText = true)

        assertTrue(modified.largeText)
        assertFalse(config.largeText) // Original unchanged
    }

    @Test
    fun `copy with highContrast true`() {
        val config = AccessibilityConfig()
        val modified = config.copy(highContrast = true)

        assertTrue(modified.highContrast)
        assertFalse(config.highContrast) // Original unchanged
    }

    @Test
    fun `copy with reduceMotion true`() {
        val config = AccessibilityConfig()
        val modified = config.copy(reduceMotion = true)

        assertTrue(modified.reduceMotion)
        assertFalse(config.reduceMotion) // Original unchanged
    }

    @Test
    fun `copy with all accessibility features enabled`() {
        val config = AccessibilityConfig()
        val modified = config.copy(
            largeText = true,
            highContrast = true,
            reduceMotion = true
        )

        assertTrue(modified.largeText)
        assertTrue(modified.highContrast)
        assertTrue(modified.reduceMotion)
    }

    // ==================== Equality Tests ====================

    @Test
    fun `two default configs are equal`() {
        val config1 = AccessibilityConfig()
        val config2 = AccessibilityConfig()

        assertEquals(config1, config2)
    }

    @Test
    fun `configs with different values are not equal`() {
        val config1 = AccessibilityConfig()
        val config2 = AccessibilityConfig(largeText = true)

        assertNotEquals(config1, config2)
    }

    @Test
    fun `hashCode is consistent for equal configs`() {
        val config1 = AccessibilityConfig(highContrast = true)
        val config2 = AccessibilityConfig(highContrast = true)

        assertEquals(config1.hashCode(), config2.hashCode())
    }

    // ==================== Constructor Tests ====================

    @Test
    fun `can create config with largeText only`() {
        val config = AccessibilityConfig(largeText = true)

        assertTrue(config.largeText)
        assertFalse(config.highContrast)
        assertFalse(config.reduceMotion)
    }

    @Test
    fun `can create config with highContrast only`() {
        val config = AccessibilityConfig(highContrast = true)

        assertFalse(config.largeText)
        assertTrue(config.highContrast)
        assertFalse(config.reduceMotion)
    }

    @Test
    fun `can create config with reduceMotion only`() {
        val config = AccessibilityConfig(reduceMotion = true)

        assertFalse(config.largeText)
        assertFalse(config.highContrast)
        assertTrue(config.reduceMotion)
    }

    // ==================== toString Tests ====================

    @Test
    fun `toString contains class name`() {
        val config = AccessibilityConfig()
        assertTrue(config.toString().contains("AccessibilityConfig"))
    }

    @Test
    fun `toString contains property values`() {
        val config = AccessibilityConfig(largeText = true, highContrast = false)
        val str = config.toString()
        assertTrue(str.contains("largeText=true"))
        assertTrue(str.contains("highContrast=false"))
    }
}
