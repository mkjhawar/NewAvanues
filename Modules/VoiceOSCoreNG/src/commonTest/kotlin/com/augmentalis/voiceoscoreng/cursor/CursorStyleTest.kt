/**
 * CursorStyleTest.kt - TDD tests for CursorStyle data class
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 * Phase: 11 - Cursor System
 */
package com.augmentalis.voiceoscoreng.cursor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CursorStyleTest {

    // ==================== CursorShape Enum Tests ====================

    @Test
    fun `CursorShape has all expected values`() {
        val shapes = CursorShape.entries
        assertEquals(4, shapes.size)
        assertTrue(shapes.contains(CursorShape.CIRCLE))
        assertTrue(shapes.contains(CursorShape.CROSSHAIR))
        assertTrue(shapes.contains(CursorShape.POINTER))
        assertTrue(shapes.contains(CursorShape.DOT))
    }

    @Test
    fun `CursorShape ordinals are stable`() {
        assertEquals(0, CursorShape.CIRCLE.ordinal)
        assertEquals(1, CursorShape.CROSSHAIR.ordinal)
        assertEquals(2, CursorShape.POINTER.ordinal)
        assertEquals(3, CursorShape.DOT.ordinal)
    }

    // ==================== CursorStyle DEFAULT Preset Tests ====================

    @Test
    fun `DEFAULT style has correct shape`() {
        val style = CursorStyle.DEFAULT
        assertEquals(CursorShape.CIRCLE, style.shape)
    }

    @Test
    fun `DEFAULT style has correct size`() {
        val style = CursorStyle.DEFAULT
        assertEquals(32, style.size)
    }

    @Test
    fun `DEFAULT style has correct color - blue`() {
        val style = CursorStyle.DEFAULT
        assertEquals(0xFF2196F3L, style.color) // Blue
    }

    @Test
    fun `DEFAULT style has correct border color - white`() {
        val style = CursorStyle.DEFAULT
        assertEquals(0xFFFFFFFFL, style.borderColor) // White
    }

    @Test
    fun `DEFAULT style has correct border width`() {
        val style = CursorStyle.DEFAULT
        assertEquals(2, style.borderWidth)
    }

    @Test
    fun `DEFAULT style has correct opacity`() {
        val style = CursorStyle.DEFAULT
        assertEquals(0.8f, style.opacity)
    }

    @Test
    fun `DEFAULT style has pulse enabled`() {
        val style = CursorStyle.DEFAULT
        assertTrue(style.pulseEnabled)
    }

    @Test
    fun `DEFAULT style has normal pulse speed`() {
        val style = CursorStyle.DEFAULT
        assertEquals(1.0f, style.pulseSpeed)
    }

    // ==================== CursorStyle CROSSHAIR Preset Tests ====================

    @Test
    fun `CROSSHAIR style has crosshair shape`() {
        val style = CursorStyle.CROSSHAIR
        assertEquals(CursorShape.CROSSHAIR, style.shape)
    }

    @Test
    fun `CROSSHAIR style has larger size`() {
        val style = CursorStyle.CROSSHAIR
        assertEquals(48, style.size)
    }

    @Test
    fun `CROSSHAIR style has red color`() {
        val style = CursorStyle.CROSSHAIR
        assertEquals(0xFFFF0000L, style.color) // Red
    }

    @Test
    fun `CROSSHAIR style has thin border`() {
        val style = CursorStyle.CROSSHAIR
        assertEquals(1, style.borderWidth)
    }

    @Test
    fun `CROSSHAIR style has pulse disabled`() {
        val style = CursorStyle.CROSSHAIR
        assertFalse(style.pulseEnabled)
    }

    // ==================== CursorStyle MINIMAL Preset Tests ====================

    @Test
    fun `MINIMAL style has dot shape`() {
        val style = CursorStyle.MINIMAL
        assertEquals(CursorShape.DOT, style.shape)
    }

    @Test
    fun `MINIMAL style has small size`() {
        val style = CursorStyle.MINIMAL
        assertEquals(16, style.size)
    }

    @Test
    fun `MINIMAL style has no border`() {
        val style = CursorStyle.MINIMAL
        assertEquals(0, style.borderWidth)
    }

    @Test
    fun `MINIMAL style has pulse disabled`() {
        val style = CursorStyle.MINIMAL
        assertFalse(style.pulseEnabled)
    }

    // ==================== CursorStyle HIGH_CONTRAST Preset Tests ====================

    @Test
    fun `HIGH_CONTRAST style has circle shape`() {
        val style = CursorStyle.HIGH_CONTRAST
        assertEquals(CursorShape.CIRCLE, style.shape)
    }

    @Test
    fun `HIGH_CONTRAST style has yellow color`() {
        val style = CursorStyle.HIGH_CONTRAST
        assertEquals(0xFFFFFF00L, style.color) // Yellow
    }

    @Test
    fun `HIGH_CONTRAST style has black border`() {
        val style = CursorStyle.HIGH_CONTRAST
        assertEquals(0xFF000000L, style.borderColor) // Black
    }

    @Test
    fun `HIGH_CONTRAST style has thick border`() {
        val style = CursorStyle.HIGH_CONTRAST
        assertEquals(3, style.borderWidth)
    }

    @Test
    fun `HIGH_CONTRAST style has full opacity`() {
        val style = CursorStyle.HIGH_CONTRAST
        assertEquals(1.0f, style.opacity)
    }

    // ==================== Custom CursorStyle Creation Tests ====================

    @Test
    fun `custom style can be created with all parameters`() {
        val style = CursorStyle(
            shape = CursorShape.POINTER,
            size = 40,
            color = 0xFF00FF00L,  // Green
            borderColor = 0xFF0000FFL,  // Blue
            borderWidth = 4,
            opacity = 0.5f,
            pulseEnabled = true,
            pulseSpeed = 2.0f
        )

        assertEquals(CursorShape.POINTER, style.shape)
        assertEquals(40, style.size)
        assertEquals(0xFF00FF00L, style.color)
        assertEquals(0xFF0000FFL, style.borderColor)
        assertEquals(4, style.borderWidth)
        assertEquals(0.5f, style.opacity)
        assertTrue(style.pulseEnabled)
        assertEquals(2.0f, style.pulseSpeed)
    }

    @Test
    fun `custom style with default parameters matches DEFAULT preset`() {
        val custom = CursorStyle()
        val default = CursorStyle.DEFAULT

        assertEquals(default.shape, custom.shape)
        assertEquals(default.size, custom.size)
        assertEquals(default.color, custom.color)
        assertEquals(default.borderColor, custom.borderColor)
        assertEquals(default.borderWidth, custom.borderWidth)
        assertEquals(default.opacity, custom.opacity)
        assertEquals(default.pulseEnabled, custom.pulseEnabled)
        assertEquals(default.pulseSpeed, custom.pulseSpeed)
    }

    // ==================== Opacity Bounds Tests ====================

    @Test
    fun `opacity at minimum bound 0_0 is valid`() {
        val style = CursorStyle(opacity = 0.0f)
        assertEquals(0.0f, style.opacity)
    }

    @Test
    fun `opacity at maximum bound 1_0 is valid`() {
        val style = CursorStyle(opacity = 1.0f)
        assertEquals(1.0f, style.opacity)
    }

    @Test
    fun `opacity at mid range 0_5 is valid`() {
        val style = CursorStyle(opacity = 0.5f)
        assertEquals(0.5f, style.opacity)
    }

    @Test
    fun `DEFAULT preset opacity is within bounds`() {
        val style = CursorStyle.DEFAULT
        assertTrue(style.opacity >= 0.0f)
        assertTrue(style.opacity <= 1.0f)
    }

    @Test
    fun `HIGH_CONTRAST preset opacity is within bounds`() {
        val style = CursorStyle.HIGH_CONTRAST
        assertTrue(style.opacity >= 0.0f)
        assertTrue(style.opacity <= 1.0f)
    }

    // ==================== Data Class Features Tests ====================

    @Test
    fun `CursorStyle equality works correctly`() {
        val style1 = CursorStyle(size = 40)
        val style2 = CursorStyle(size = 40)
        val style3 = CursorStyle(size = 50)

        assertEquals(style1, style2)
        assertNotEquals(style1, style3)
    }

    @Test
    fun `CursorStyle hashCode is consistent`() {
        val style1 = CursorStyle(size = 40)
        val style2 = CursorStyle(size = 40)
        assertEquals(style1.hashCode(), style2.hashCode())
    }

    @Test
    fun `CursorStyle copy preserves unchanged fields`() {
        val original = CursorStyle(
            shape = CursorShape.CROSSHAIR,
            size = 50,
            color = 0xFF123456L
        )
        val copied = original.copy(size = 60)

        assertEquals(60, copied.size)
        assertEquals(CursorShape.CROSSHAIR, copied.shape)  // Unchanged
        assertEquals(0xFF123456L, copied.color)  // Unchanged
    }

    @Test
    fun `CursorStyle toString contains key properties`() {
        val style = CursorStyle(shape = CursorShape.DOT, size = 24)
        val str = style.toString()
        assertTrue(str.contains("CursorStyle"))
        assertTrue(str.contains("shape"))
        assertTrue(str.contains("size"))
    }

    // ==================== Color Value Format Tests ====================

    @Test
    fun `color values are in correct ARGB format`() {
        val style = CursorStyle.DEFAULT
        // Blue color 0xFF2196F3 - verify alpha is full (0xFF)
        val alpha = (style.color shr 24) and 0xFF
        assertEquals(0xFFL, alpha)
    }

    @Test
    fun `border color has full alpha`() {
        val style = CursorStyle.DEFAULT
        val alpha = (style.borderColor shr 24) and 0xFF
        assertEquals(0xFFL, alpha)
    }

    // ==================== Pulse Speed Tests ====================

    @Test
    fun `pulse speed normal value is 1_0`() {
        val style = CursorStyle()
        assertEquals(1.0f, style.pulseSpeed)
    }

    @Test
    fun `pulse speed can be set to fast value 2_0`() {
        val style = CursorStyle(pulseSpeed = 2.0f)
        assertEquals(2.0f, style.pulseSpeed)
    }

    @Test
    fun `pulse speed can be set to slow value 0_5`() {
        val style = CursorStyle(pulseSpeed = 0.5f)
        assertEquals(0.5f, style.pulseSpeed)
    }

    // ==================== Size Validation Tests ====================

    @Test
    fun `size is positive in all presets`() {
        assertTrue(CursorStyle.DEFAULT.size > 0)
        assertTrue(CursorStyle.CROSSHAIR.size > 0)
        assertTrue(CursorStyle.MINIMAL.size > 0)
        assertTrue(CursorStyle.HIGH_CONTRAST.size > 0)
    }

    @Test
    fun `border width is non-negative in all presets`() {
        assertTrue(CursorStyle.DEFAULT.borderWidth >= 0)
        assertTrue(CursorStyle.CROSSHAIR.borderWidth >= 0)
        assertTrue(CursorStyle.MINIMAL.borderWidth >= 0)
        assertTrue(CursorStyle.HIGH_CONTRAST.borderWidth >= 0)
    }

    // ==================== All Presets Accessible Tests ====================

    @Test
    fun `all predefined styles are accessible`() {
        val styles = listOf(
            CursorStyle.DEFAULT,
            CursorStyle.CROSSHAIR,
            CursorStyle.MINIMAL,
            CursorStyle.HIGH_CONTRAST
        )
        assertEquals(4, styles.size)
    }

    @Test
    fun `each preset has distinct characteristics`() {
        // Verify presets are not identical
        assertNotEquals(CursorStyle.DEFAULT.shape, CursorStyle.CROSSHAIR.shape)
        assertNotEquals(CursorStyle.DEFAULT.shape, CursorStyle.MINIMAL.shape)
        assertNotEquals(CursorStyle.CROSSHAIR.color, CursorStyle.HIGH_CONTRAST.color)
    }
}
