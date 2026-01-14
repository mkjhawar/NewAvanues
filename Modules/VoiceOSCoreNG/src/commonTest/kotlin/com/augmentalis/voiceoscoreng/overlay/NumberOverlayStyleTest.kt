/**
 * NumberOverlayStyleTest.kt - TDD tests for NumberOverlayStyle
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NumberOverlayStyleTest {

    // ==================== BadgeShape Enum Tests ====================

    @Test
    fun `BadgeShape has all expected values`() {
        val shapes = BadgeShape.entries
        assertEquals(4, shapes.size)
        assertTrue(shapes.contains(BadgeShape.FILLED_CIRCLE))
        assertTrue(shapes.contains(BadgeShape.OUTLINED_CIRCLE))
        assertTrue(shapes.contains(BadgeShape.SQUARE))
        assertTrue(shapes.contains(BadgeShape.ROUNDED_RECT))
    }

    @Test
    fun `BadgeShape ordinals are stable`() {
        assertEquals(0, BadgeShape.FILLED_CIRCLE.ordinal)
        assertEquals(1, BadgeShape.OUTLINED_CIRCLE.ordinal)
        assertEquals(2, BadgeShape.SQUARE.ordinal)
        assertEquals(3, BadgeShape.ROUNDED_RECT.ordinal)
    }

    // ==================== AnchorPoint Enum Tests ====================

    @Test
    fun `AnchorPoint has all expected values`() {
        val anchors = AnchorPoint.entries
        assertEquals(4, anchors.size)
        assertTrue(anchors.contains(AnchorPoint.TOP_LEFT))
        assertTrue(anchors.contains(AnchorPoint.TOP_RIGHT))
        assertTrue(anchors.contains(AnchorPoint.BOTTOM_LEFT))
        assertTrue(anchors.contains(AnchorPoint.BOTTOM_RIGHT))
    }

    @Test
    fun `AnchorPoint ordinals are stable`() {
        assertEquals(0, AnchorPoint.TOP_LEFT.ordinal)
        assertEquals(1, AnchorPoint.TOP_RIGHT.ordinal)
        assertEquals(2, AnchorPoint.BOTTOM_LEFT.ordinal)
        assertEquals(3, AnchorPoint.BOTTOM_RIGHT.ordinal)
    }

    // ==================== NumberOverlayStyle Default Values Tests ====================

    @Test
    fun `NumberOverlayStyle has correct default anchor point`() {
        val style = NumberOverlayStyle()
        assertEquals(AnchorPoint.TOP_RIGHT, style.anchorPoint)
    }

    @Test
    fun `NumberOverlayStyle has correct default offsets`() {
        val style = NumberOverlayStyle()
        assertEquals(-4f, style.offsetX)
        assertEquals(-4f, style.offsetY)
    }

    @Test
    fun `NumberOverlayStyle has correct default circle dimensions`() {
        val style = NumberOverlayStyle()
        assertEquals(16f, style.circleRadius)
        assertEquals(2f, style.strokeWidth)
    }

    @Test
    fun `NumberOverlayStyle has correct default colors`() {
        val style = NumberOverlayStyle()
        assertEquals(0xFF4CAF50, style.hasNameColor)      // Material Green 500
        assertEquals(0xFFFF9800, style.noNameColor)        // Material Orange 500
        assertEquals(0xFF757575, style.disabledColor)      // Grey 600
    }

    @Test
    fun `NumberOverlayStyle has correct default number styling`() {
        val style = NumberOverlayStyle()
        assertEquals(0xFFFFFFFF, style.numberColor)        // White
        assertEquals(14f, style.numberSize)
        assertEquals(700, style.fontWeight)                 // Bold
    }

    @Test
    fun `NumberOverlayStyle has correct default shadow settings`() {
        val style = NumberOverlayStyle()
        assertTrue(style.dropShadow)
        assertEquals(4f, style.shadowRadius)
        assertEquals(0x40000000, style.shadowColor)        // 25% black
        assertEquals(2f, style.shadowOffsetY)
    }

    @Test
    fun `NumberOverlayStyle has correct default badge style`() {
        val style = NumberOverlayStyle()
        assertEquals(BadgeShape.FILLED_CIRCLE, style.badgeStyle)
    }

    // ==================== NumberOverlayStyle Custom Values Tests ====================

    @Test
    fun `NumberOverlayStyle can be created with custom anchor point`() {
        val style = NumberOverlayStyle(anchorPoint = AnchorPoint.BOTTOM_LEFT)
        assertEquals(AnchorPoint.BOTTOM_LEFT, style.anchorPoint)
    }

    @Test
    fun `NumberOverlayStyle can be created with custom offsets`() {
        val style = NumberOverlayStyle(offsetX = 10f, offsetY = -10f)
        assertEquals(10f, style.offsetX)
        assertEquals(-10f, style.offsetY)
    }

    @Test
    fun `NumberOverlayStyle can be created with custom dimensions`() {
        val style = NumberOverlayStyle(circleRadius = 24f, strokeWidth = 4f)
        assertEquals(24f, style.circleRadius)
        assertEquals(4f, style.strokeWidth)
    }

    @Test
    fun `NumberOverlayStyle can be created with custom colors`() {
        val style = NumberOverlayStyle(
            hasNameColor = 0xFF0000FF,
            noNameColor = 0xFFFF0000,
            disabledColor = 0xFF808080
        )
        assertEquals(0xFF0000FF, style.hasNameColor)
        assertEquals(0xFFFF0000, style.noNameColor)
        assertEquals(0xFF808080, style.disabledColor)
    }

    @Test
    fun `NumberOverlayStyle can be created with custom typography`() {
        val style = NumberOverlayStyle(
            numberColor = 0xFF000000,
            numberSize = 20f,
            fontWeight = 400
        )
        assertEquals(0xFF000000, style.numberColor)
        assertEquals(20f, style.numberSize)
        assertEquals(400, style.fontWeight)
    }

    @Test
    fun `NumberOverlayStyle can be created with shadow disabled`() {
        val style = NumberOverlayStyle(dropShadow = false)
        assertFalse(style.dropShadow)
    }

    @Test
    fun `NumberOverlayStyle can be created with custom shadow settings`() {
        val style = NumberOverlayStyle(
            shadowRadius = 8f,
            shadowColor = 0x80000000,
            shadowOffsetY = 4f
        )
        assertEquals(8f, style.shadowRadius)
        assertEquals(0x80000000, style.shadowColor)
        assertEquals(4f, style.shadowOffsetY)
    }

    @Test
    fun `NumberOverlayStyle can be created with different badge shapes`() {
        val circleStyle = NumberOverlayStyle(badgeStyle = BadgeShape.FILLED_CIRCLE)
        val outlineStyle = NumberOverlayStyle(badgeStyle = BadgeShape.OUTLINED_CIRCLE)
        val squareStyle = NumberOverlayStyle(badgeStyle = BadgeShape.SQUARE)
        val rectStyle = NumberOverlayStyle(badgeStyle = BadgeShape.ROUNDED_RECT)

        assertEquals(BadgeShape.FILLED_CIRCLE, circleStyle.badgeStyle)
        assertEquals(BadgeShape.OUTLINED_CIRCLE, outlineStyle.badgeStyle)
        assertEquals(BadgeShape.SQUARE, squareStyle.badgeStyle)
        assertEquals(BadgeShape.ROUNDED_RECT, rectStyle.badgeStyle)
    }

    // ==================== NumberOverlayStyle Copy Tests ====================

    @Test
    fun `NumberOverlayStyle copy preserves unchanged fields`() {
        val original = NumberOverlayStyle(
            circleRadius = 20f,
            hasNameColor = 0xFF00FF00
        )
        val copied = original.copy(circleRadius = 24f)

        assertEquals(24f, copied.circleRadius)
        assertEquals(0xFF00FF00, copied.hasNameColor)  // Unchanged
    }

    @Test
    fun `NumberOverlayStyle equality works correctly`() {
        val style1 = NumberOverlayStyle(circleRadius = 20f)
        val style2 = NumberOverlayStyle(circleRadius = 20f)
        val style3 = NumberOverlayStyle(circleRadius = 24f)

        assertEquals(style1, style2)
        assertNotEquals(style1, style3)
    }

    // ==================== NumberOverlayStyles Predefined Styles Tests ====================

    @Test
    fun `DEFAULT style has standard settings`() {
        val style = NumberOverlayStyles.DEFAULT
        assertEquals(16f, style.circleRadius)
        assertEquals(2f, style.strokeWidth)
        assertEquals(0xFF4CAF50, style.hasNameColor)
        assertEquals(0xFFFF9800, style.noNameColor)
        assertEquals(BadgeShape.FILLED_CIRCLE, style.badgeStyle)
        assertTrue(style.dropShadow)
    }

    @Test
    fun `HIGH_CONTRAST style has enhanced visibility settings`() {
        val style = NumberOverlayStyles.HIGH_CONTRAST
        assertEquals(20f, style.circleRadius)  // Larger
        assertEquals(3f, style.strokeWidth)     // Thicker
        assertEquals(16f, style.numberSize)     // Larger text
        assertEquals(0xFF1B5E20, style.hasNameColor)  // Darker green
        assertEquals(0xFFE65100, style.noNameColor)   // Darker orange
    }

    @Test
    fun `LARGE_TEXT style has larger dimensions`() {
        val style = NumberOverlayStyles.LARGE_TEXT
        assertEquals(24f, style.circleRadius)
        assertEquals(20f, style.numberSize)
    }

    @Test
    fun `MINIMAL style has subtle appearance`() {
        val style = NumberOverlayStyles.MINIMAL
        assertFalse(style.dropShadow)
        assertEquals(0x804CAF50, style.hasNameColor)  // Semi-transparent green
        assertEquals(0x80FF9800, style.noNameColor)   // Semi-transparent orange
    }

    @Test
    fun `OUTLINED style uses outline badge`() {
        val style = NumberOverlayStyles.OUTLINED
        assertEquals(BadgeShape.OUTLINED_CIRCLE, style.badgeStyle)
    }

    @Test
    fun `COLORBLIND_FRIENDLY style uses blue and gold colors`() {
        val style = NumberOverlayStyles.COLORBLIND_FRIENDLY
        assertEquals(0xFF2196F3, style.hasNameColor)  // Blue
        assertEquals(0xFFFFC107, style.noNameColor)   // Gold
        assertEquals(BadgeShape.ROUNDED_RECT, style.badgeStyle)
    }

    @Test
    fun `SQUARE style uses square badge`() {
        val style = NumberOverlayStyles.SQUARE
        assertEquals(BadgeShape.SQUARE, style.badgeStyle)
    }

    @Test
    fun `ROUNDED_RECT style uses rounded rectangle badge`() {
        val style = NumberOverlayStyles.ROUNDED_RECT
        assertEquals(BadgeShape.ROUNDED_RECT, style.badgeStyle)
    }

    @Test
    fun `DARK_MODE style has optimized colors for dark backgrounds`() {
        val style = NumberOverlayStyles.DARK_MODE
        assertEquals(0xFF66BB6A, style.hasNameColor)    // Lighter green
        assertEquals(0xFFFFA726, style.noNameColor)     // Lighter orange
        assertEquals(0xFFE0E0E0, style.numberColor)     // Off-white
        assertEquals(0x80000000, style.shadowColor)     // 50% black shadow
    }

    @Test
    fun `LIGHT_MODE style has optimized colors for light backgrounds`() {
        val style = NumberOverlayStyles.LIGHT_MODE
        assertEquals(0xFF43A047, style.hasNameColor)    // Darker green
        assertEquals(0xFFFB8C00, style.noNameColor)     // Darker orange
        assertEquals(0xFFFFFFFF, style.numberColor)     // White
    }

    // ==================== Color Value Tests ====================

    @Test
    fun `color values are in correct ARGB format`() {
        val style = NumberOverlayStyle()
        // All colors should have alpha channel set (high byte non-zero for opaque colors)
        assertTrue(style.hasNameColor > 0xFF000000 || style.hasNameColor == 0xFF4CAF50)
        assertTrue(style.noNameColor > 0xFF000000 || style.noNameColor == 0xFFFF9800)
    }

    @Test
    fun `semi-transparent colors have correct alpha`() {
        val minimal = NumberOverlayStyles.MINIMAL
        // 0x80 = 128 = 50% alpha
        assertEquals(0x80, ((minimal.hasNameColor shr 24) and 0xFF).toInt())
        assertEquals(0x80, ((minimal.noNameColor shr 24) and 0xFF).toInt())
    }

    // ==================== Dimension Validation Tests ====================

    @Test
    fun `circle radius is positive in all predefined styles`() {
        assertTrue(NumberOverlayStyles.DEFAULT.circleRadius > 0)
        assertTrue(NumberOverlayStyles.HIGH_CONTRAST.circleRadius > 0)
        assertTrue(NumberOverlayStyles.LARGE_TEXT.circleRadius > 0)
        assertTrue(NumberOverlayStyles.MINIMAL.circleRadius > 0)
    }

    @Test
    fun `stroke width is positive in all predefined styles`() {
        assertTrue(NumberOverlayStyles.DEFAULT.strokeWidth > 0)
        assertTrue(NumberOverlayStyles.HIGH_CONTRAST.strokeWidth > 0)
        assertTrue(NumberOverlayStyles.OUTLINED.strokeWidth > 0)
    }

    @Test
    fun `number size is reasonable in all predefined styles`() {
        // Minimum readable size is ~10sp, maximum reasonable is ~30sp
        assertTrue(NumberOverlayStyles.DEFAULT.numberSize in 10f..30f)
        assertTrue(NumberOverlayStyles.HIGH_CONTRAST.numberSize in 10f..30f)
        assertTrue(NumberOverlayStyles.LARGE_TEXT.numberSize in 10f..30f)
    }

    // ==================== All Predefined Styles Exist Tests ====================

    @Test
    fun `all predefined styles are accessible`() {
        // Verify all styles can be accessed without exception
        val styles = listOf(
            NumberOverlayStyles.DEFAULT,
            NumberOverlayStyles.HIGH_CONTRAST,
            NumberOverlayStyles.LARGE_TEXT,
            NumberOverlayStyles.MINIMAL,
            NumberOverlayStyles.OUTLINED,
            NumberOverlayStyles.SQUARE,
            NumberOverlayStyles.ROUNDED_RECT,
            NumberOverlayStyles.DARK_MODE,
            NumberOverlayStyles.LIGHT_MODE,
            NumberOverlayStyles.COLORBLIND_FRIENDLY
        )
        assertEquals(10, styles.size)
    }

    // ==================== Data Class Features Tests ====================

    @Test
    fun `NumberOverlayStyle hashCode is consistent`() {
        val style1 = NumberOverlayStyle(circleRadius = 20f)
        val style2 = NumberOverlayStyle(circleRadius = 20f)
        assertEquals(style1.hashCode(), style2.hashCode())
    }

    @Test
    fun `NumberOverlayStyle toString contains key properties`() {
        val style = NumberOverlayStyle(circleRadius = 24f)
        val str = style.toString()
        assertTrue(str.contains("NumberOverlayStyle"))
        assertTrue(str.contains("circleRadius"))
    }
}
