/**
 * NumberOverlayRendererTest.kt - TDD tests for NumberOverlayRenderer
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * TDD test suite for NumberOverlayRenderer - written BEFORE implementation.
 *
 * Tests cover:
 * - Badge position calculation for all anchor points
 * - Color selection based on element state
 * - Text bounds calculation for numbers
 * - Touch bounds for hit testing
 * - Rendering parameter generation
 */
package com.augmentalis.voiceoscoreng.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NumberOverlayRendererTest {

    // ═══════════════════════════════════════════════════════════════════════════
    // Test Fixtures
    // ═══════════════════════════════════════════════════════════════════════════

    private val defaultStyle = NumberOverlayStyle()
    private val renderer = NumberOverlayRenderer(defaultStyle)

    // Standard element bounds for testing (100x50 element at position 200,300)
    private val standardBounds = Rect(
        left = 200,
        top = 300,
        right = 300,
        bottom = 350
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // Badge Position Calculation Tests - TOP_RIGHT (default)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `calculateBadgeCenter returns TOP_RIGHT position by default`() {
        val center = renderer.calculateBadgeCenter(standardBounds)

        // TOP_RIGHT: right edge - offsetX - radius, top edge + offsetY + radius
        // With default style: offset=-4, radius=16
        // x = 300 - (-4) - 16 = 288
        // y = 300 + (-4) + 16 = 312
        assertEquals(288f, center.x)
        assertEquals(312f, center.y)
    }

    @Test
    fun `calculateBadgeCenter with TOP_RIGHT anchor positions near top-right corner`() {
        val style = NumberOverlayStyle(anchorPoint = AnchorPoint.TOP_RIGHT)
        val renderer = NumberOverlayRenderer(style)
        val center = renderer.calculateBadgeCenter(standardBounds)

        // Badge center should be inside the element bounds (near top-right)
        assertTrue(center.x < standardBounds.right)
        assertTrue(center.y > standardBounds.top)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Badge Position Calculation Tests - TOP_LEFT
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `calculateBadgeCenter with TOP_LEFT anchor positions near top-left corner`() {
        val style = NumberOverlayStyle(anchorPoint = AnchorPoint.TOP_LEFT)
        val renderer = NumberOverlayRenderer(style)
        val center = renderer.calculateBadgeCenter(standardBounds)

        // TOP_LEFT: left + offsetX + radius, top + offsetY + radius
        // x = 200 + (-4) + 16 = 212
        // y = 300 + (-4) + 16 = 312
        assertEquals(212f, center.x)
        assertEquals(312f, center.y)
    }

    @Test
    fun `calculateBadgeCenter with TOP_LEFT anchor is inside element bounds`() {
        val style = NumberOverlayStyle(anchorPoint = AnchorPoint.TOP_LEFT)
        val renderer = NumberOverlayRenderer(style)
        val center = renderer.calculateBadgeCenter(standardBounds)

        assertTrue(center.x > standardBounds.left)
        assertTrue(center.y > standardBounds.top)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Badge Position Calculation Tests - BOTTOM_RIGHT
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `calculateBadgeCenter with BOTTOM_RIGHT anchor positions near bottom-right corner`() {
        val style = NumberOverlayStyle(anchorPoint = AnchorPoint.BOTTOM_RIGHT)
        val renderer = NumberOverlayRenderer(style)
        val center = renderer.calculateBadgeCenter(standardBounds)

        // BOTTOM_RIGHT: right - offsetX - radius, bottom - offsetY - radius
        // x = 300 - (-4) - 16 = 288
        // y = 350 - (-4) - 16 = 338
        assertEquals(288f, center.x)
        assertEquals(338f, center.y)
    }

    @Test
    fun `calculateBadgeCenter with BOTTOM_RIGHT is inside element bounds`() {
        val style = NumberOverlayStyle(anchorPoint = AnchorPoint.BOTTOM_RIGHT)
        val renderer = NumberOverlayRenderer(style)
        val center = renderer.calculateBadgeCenter(standardBounds)

        assertTrue(center.x < standardBounds.right)
        assertTrue(center.y < standardBounds.bottom)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Badge Position Calculation Tests - BOTTOM_LEFT
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `calculateBadgeCenter with BOTTOM_LEFT anchor positions near bottom-left corner`() {
        val style = NumberOverlayStyle(anchorPoint = AnchorPoint.BOTTOM_LEFT)
        val renderer = NumberOverlayRenderer(style)
        val center = renderer.calculateBadgeCenter(standardBounds)

        // BOTTOM_LEFT: left + offsetX + radius, bottom - offsetY - radius
        // x = 200 + (-4) + 16 = 212
        // y = 350 - (-4) - 16 = 338
        assertEquals(212f, center.x)
        assertEquals(338f, center.y)
    }

    @Test
    fun `calculateBadgeCenter with BOTTOM_LEFT is inside element bounds`() {
        val style = NumberOverlayStyle(anchorPoint = AnchorPoint.BOTTOM_LEFT)
        val renderer = NumberOverlayRenderer(style)
        val center = renderer.calculateBadgeCenter(standardBounds)

        assertTrue(center.x > standardBounds.left)
        assertTrue(center.y < standardBounds.bottom)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Badge Position with Custom Offset Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `calculateBadgeCenter respects custom offset values`() {
        val style = NumberOverlayStyle(
            anchorPoint = AnchorPoint.TOP_RIGHT,
            offsetX = 10f,
            offsetY = 5f,
            circleRadius = 20f
        )
        val renderer = NumberOverlayRenderer(style)
        val center = renderer.calculateBadgeCenter(standardBounds)

        // TOP_RIGHT with custom offsets:
        // x = 300 - 10 - 20 = 270
        // y = 300 + 5 + 20 = 325
        assertEquals(270f, center.x)
        assertEquals(325f, center.y)
    }

    @Test
    fun `calculateBadgeCenter respects custom radius`() {
        val style = NumberOverlayStyle(circleRadius = 24f)
        val renderer = NumberOverlayRenderer(style)
        val center = renderer.calculateBadgeCenter(standardBounds)

        // TOP_RIGHT with larger radius:
        // x = 300 - (-4) - 24 = 280
        // y = 300 + (-4) + 24 = 320
        assertEquals(280f, center.x)
        assertEquals(320f, center.y)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Color Selection Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `selectBadgeColor returns hasNameColor for enabled element with name`() {
        val item = NumberedItem(
            number = 1,
            label = "Submit",
            bounds = standardBounds,
            isEnabled = true,
            hasName = true
        )

        val color = renderer.selectBadgeColor(item)
        assertEquals(defaultStyle.hasNameColor, color)
    }

    @Test
    fun `selectBadgeColor returns noNameColor for enabled element without name`() {
        val item = NumberedItem(
            number = 1,
            label = "",
            bounds = standardBounds,
            isEnabled = true,
            hasName = false
        )

        val color = renderer.selectBadgeColor(item)
        assertEquals(defaultStyle.noNameColor, color)
    }

    @Test
    fun `selectBadgeColor returns disabledColor for disabled element`() {
        val item = NumberedItem(
            number = 1,
            label = "Disabled Button",
            bounds = standardBounds,
            isEnabled = false,
            hasName = true
        )

        val color = renderer.selectBadgeColor(item)
        assertEquals(defaultStyle.disabledColor, color)
    }

    @Test
    fun `selectBadgeColor prioritizes disabled state over hasName`() {
        // If element is disabled, color should be disabledColor regardless of hasName
        val itemWithName = NumberedItem(
            number = 1,
            label = "Named",
            bounds = standardBounds,
            isEnabled = false,
            hasName = true
        )
        val itemWithoutName = NumberedItem(
            number = 2,
            label = "",
            bounds = standardBounds,
            isEnabled = false,
            hasName = false
        )

        assertEquals(defaultStyle.disabledColor, renderer.selectBadgeColor(itemWithName))
        assertEquals(defaultStyle.disabledColor, renderer.selectBadgeColor(itemWithoutName))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Touch Bounds Calculation Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getTouchBounds returns expanded rectangle around badge center`() {
        val touchBounds = renderer.getTouchBounds(standardBounds)

        // Touch bounds should be centered on badge center with expanded radius
        // Badge center for TOP_RIGHT: (288, 312)
        // Touch radius = circleRadius + touch padding (8)
        // So bounds = (288-24, 312-24, 288+24, 312+24) = (264, 288, 312, 336)
        assertNotNull(touchBounds)
        assertTrue(touchBounds.width > defaultStyle.circleRadius * 2)
        assertTrue(touchBounds.height > defaultStyle.circleRadius * 2)
    }

    @Test
    fun `getTouchBounds includes touch padding for easier interaction`() {
        val touchBounds = renderer.getTouchBounds(standardBounds)

        // Touch padding should be added (default 8px)
        val expectedMinSize = (defaultStyle.circleRadius + NumberOverlayRenderer.TOUCH_PADDING) * 2
        assertTrue(touchBounds.width >= expectedMinSize)
        assertTrue(touchBounds.height >= expectedMinSize)
    }

    @Test
    fun `getTouchBounds is square`() {
        val touchBounds = renderer.getTouchBounds(standardBounds)
        assertEquals(touchBounds.width, touchBounds.height)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Contains (Hit Test) Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `contains returns true for point at badge center`() {
        // Badge center for TOP_RIGHT: (288, 312)
        assertTrue(renderer.contains(standardBounds, 288f, 312f))
    }

    @Test
    fun `contains returns true for point within touch bounds`() {
        // Badge center for TOP_RIGHT: (288, 312)
        // Touch radius = 16 + 8 = 24
        assertTrue(renderer.contains(standardBounds, 280f, 320f))
        assertTrue(renderer.contains(standardBounds, 290f, 310f))
    }

    @Test
    fun `contains returns false for point outside touch bounds`() {
        // Badge center for TOP_RIGHT: (288, 312)
        // Touch radius = 16 + 8 = 24
        // Point at (200, 300) is far from badge center
        assertFalse(renderer.contains(standardBounds, 200f, 300f))
    }

    @Test
    fun `contains returns false for point at element corner opposite to badge`() {
        // Badge is at TOP_RIGHT, point at BOTTOM_LEFT should be outside
        assertFalse(renderer.contains(standardBounds, 200f, 350f))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Badge Size Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getBadgeSize returns diameter`() {
        val size = renderer.getBadgeSize()
        assertEquals((defaultStyle.circleRadius * 2).toInt(), size)
    }

    @Test
    fun `getBadgeSize respects custom radius`() {
        val style = NumberOverlayStyle(circleRadius = 24f)
        val renderer = NumberOverlayRenderer(style)

        assertEquals(48, renderer.getBadgeSize())
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Rendering Parameters Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getRenderingParams returns all necessary parameters for badge drawing`() {
        val item = NumberedItem(
            number = 5,
            label = "Test",
            bounds = standardBounds,
            isEnabled = true,
            hasName = true
        )

        val params = renderer.getRenderingParams(item)

        assertNotNull(params)
        assertNotNull(params.center)
        assertNotNull(params.badgeColor)
        assertNotNull(params.textColor)
        assertNotNull(params.radius)
        assertEquals(5, params.number)
    }

    @Test
    fun `getRenderingParams includes shadow parameters when enabled`() {
        val style = NumberOverlayStyle(dropShadow = true)
        val renderer = NumberOverlayRenderer(style)
        val item = NumberedItem(
            number = 1,
            label = "Test",
            bounds = standardBounds,
            isEnabled = true,
            hasName = true
        )

        val params = renderer.getRenderingParams(item)

        assertTrue(params.hasShadow)
        assertEquals(style.shadowColor, params.shadowColor)
        assertEquals(style.shadowRadius, params.shadowRadius)
        assertEquals(style.shadowOffsetY, params.shadowOffsetY)
    }

    @Test
    fun `getRenderingParams excludes shadow when disabled`() {
        val style = NumberOverlayStyle(dropShadow = false)
        val renderer = NumberOverlayRenderer(style)
        val item = NumberedItem(
            number = 1,
            label = "Test",
            bounds = standardBounds,
            isEnabled = true,
            hasName = true
        )

        val params = renderer.getRenderingParams(item)

        assertFalse(params.hasShadow)
    }

    @Test
    fun `getRenderingParams includes stroke width for non-outlined styles`() {
        val style = NumberOverlayStyle(
            badgeStyle = BadgeShape.FILLED_CIRCLE,
            strokeWidth = 3f
        )
        val renderer = NumberOverlayRenderer(style)
        val item = NumberedItem(
            number = 1,
            label = "Test",
            bounds = standardBounds,
            isEnabled = true,
            hasName = true
        )

        val params = renderer.getRenderingParams(item)

        assertTrue(params.hasStroke)
        assertEquals(3f, params.strokeWidth)
    }

    @Test
    fun `getRenderingParams sets stroke as badge color for outlined style`() {
        val style = NumberOverlayStyle(
            badgeStyle = BadgeShape.OUTLINED_CIRCLE
        )
        val renderer = NumberOverlayRenderer(style)
        val item = NumberedItem(
            number = 1,
            label = "Test",
            bounds = standardBounds,
            isEnabled = true,
            hasName = true
        )

        val params = renderer.getRenderingParams(item)

        // For outlined style, stroke IS the visible part
        assertFalse(params.hasStroke) // No separate white stroke
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Badge Shape Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getRenderingParams includes correct shape`() {
        val style = NumberOverlayStyle(badgeStyle = BadgeShape.SQUARE)
        val renderer = NumberOverlayRenderer(style)
        val item = NumberedItem(
            number = 1,
            label = "Test",
            bounds = standardBounds,
            isEnabled = true,
            hasName = true
        )

        val params = renderer.getRenderingParams(item)

        assertEquals(BadgeShape.SQUARE, params.shape)
    }

    @Test
    fun `getRenderingParams includes corner radius for rounded rect`() {
        val style = NumberOverlayStyle(badgeStyle = BadgeShape.ROUNDED_RECT)
        val renderer = NumberOverlayRenderer(style)
        val item = NumberedItem(
            number = 1,
            label = "Test",
            bounds = standardBounds,
            isEnabled = true,
            hasName = true
        )

        val params = renderer.getRenderingParams(item)

        assertEquals(BadgeShape.ROUNDED_RECT, params.shape)
        assertTrue(params.cornerRadius > 0)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Text Parameters Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getRenderingParams includes text size from style`() {
        val style = NumberOverlayStyle(numberSize = 18f)
        val renderer = NumberOverlayRenderer(style)
        val item = NumberedItem(
            number = 1,
            label = "Test",
            bounds = standardBounds,
            isEnabled = true,
            hasName = true
        )

        val params = renderer.getRenderingParams(item)

        assertEquals(18f, params.textSize)
    }

    @Test
    fun `getRenderingParams includes font weight from style`() {
        val style = NumberOverlayStyle(fontWeight = 500)
        val renderer = NumberOverlayRenderer(style)
        val item = NumberedItem(
            number = 1,
            label = "Test",
            bounds = standardBounds,
            isEnabled = true,
            hasName = true
        )

        val params = renderer.getRenderingParams(item)

        assertEquals(500, params.fontWeight)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Number Display Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getRenderingParams displays single digit numbers correctly`() {
        val item = NumberedItem(
            number = 5,
            label = "Test",
            bounds = standardBounds,
            isEnabled = true,
            hasName = true
        )

        val params = renderer.getRenderingParams(item)

        assertEquals(5, params.number)
        assertEquals("5", params.numberText)
    }

    @Test
    fun `getRenderingParams displays double digit numbers correctly`() {
        val item = NumberedItem(
            number = 42,
            label = "Test",
            bounds = standardBounds,
            isEnabled = true,
            hasName = true
        )

        val params = renderer.getRenderingParams(item)

        assertEquals(42, params.number)
        assertEquals("42", params.numberText)
    }

    @Test
    fun `getRenderingParams displays triple digit numbers correctly`() {
        val item = NumberedItem(
            number = 123,
            label = "Test",
            bounds = standardBounds,
            isEnabled = true,
            hasName = true
        )

        val params = renderer.getRenderingParams(item)

        assertEquals(123, params.number)
        assertEquals("123", params.numberText)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Batch Rendering Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getBatchRenderingParams processes multiple items`() {
        val items = listOf(
            NumberedItem(1, "First", Rect(0, 0, 100, 50), true, true),
            NumberedItem(2, "Second", Rect(0, 60, 100, 110), true, false),
            NumberedItem(3, "Third", Rect(0, 120, 100, 170), false, true)
        )

        val paramsList = renderer.getBatchRenderingParams(items)

        assertEquals(3, paramsList.size)
        assertEquals(1, paramsList[0].number)
        assertEquals(2, paramsList[1].number)
        assertEquals(3, paramsList[2].number)
    }

    @Test
    fun `getBatchRenderingParams assigns correct colors to each item`() {
        val items = listOf(
            NumberedItem(1, "Enabled+Name", Rect(0, 0, 100, 50), true, true),
            NumberedItem(2, "Enabled-Name", Rect(0, 60, 100, 110), true, false),
            NumberedItem(3, "Disabled", Rect(0, 120, 100, 170), false, true)
        )

        val paramsList = renderer.getBatchRenderingParams(items)

        assertEquals(defaultStyle.hasNameColor, paramsList[0].badgeColor)
        assertEquals(defaultStyle.noNameColor, paramsList[1].badgeColor)
        assertEquals(defaultStyle.disabledColor, paramsList[2].badgeColor)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Edge Cases Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `calculateBadgeCenter handles zero-size bounds`() {
        val emptyBounds = Rect.EMPTY
        val center = renderer.calculateBadgeCenter(emptyBounds)

        // Should still calculate a position (at origin with offsets)
        assertNotNull(center)
    }

    @Test
    fun `calculateBadgeCenter handles very small element bounds`() {
        val smallBounds = Rect(100, 100, 110, 110) // 10x10 element
        val center = renderer.calculateBadgeCenter(smallBounds)

        // Badge may extend beyond small element, but calculation should work
        assertNotNull(center)
    }

    @Test
    fun `getBatchRenderingParams handles empty list`() {
        val paramsList = renderer.getBatchRenderingParams(emptyList())
        assertTrue(paramsList.isEmpty())
    }

    @Test
    fun `MAX_DISPLAY_NUMBER constant is 999`() {
        assertEquals(999, NumberOverlayRenderer.MAX_DISPLAY_NUMBER)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Style Preset Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `renderer with HIGH_CONTRAST style uses larger radius`() {
        val renderer = NumberOverlayRenderer(NumberOverlayStyles.HIGH_CONTRAST)
        assertEquals(40, renderer.getBadgeSize()) // 20 * 2
    }

    @Test
    fun `renderer with LARGE_TEXT style uses larger radius`() {
        val renderer = NumberOverlayRenderer(NumberOverlayStyles.LARGE_TEXT)
        assertEquals(48, renderer.getBadgeSize()) // 24 * 2
    }

    @Test
    fun `renderer with MINIMAL style has no shadow`() {
        val renderer = NumberOverlayRenderer(NumberOverlayStyles.MINIMAL)
        val item = NumberedItem(1, "Test", standardBounds, true, true)
        val params = renderer.getRenderingParams(item)

        assertFalse(params.hasShadow)
    }

    @Test
    fun `renderer with OUTLINED style uses correct badge shape`() {
        val renderer = NumberOverlayRenderer(NumberOverlayStyles.OUTLINED)
        val item = NumberedItem(1, "Test", standardBounds, true, true)
        val params = renderer.getRenderingParams(item)

        assertEquals(BadgeShape.OUTLINED_CIRCLE, params.shape)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// RectF Test Extension
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Additional tests for RectF bounds calculations
 */
class RectFTest {

    @Test
    fun `RectF width is calculated correctly`() {
        val rect = RectF(10f, 20f, 110f, 70f)
        assertEquals(100f, rect.width)
    }

    @Test
    fun `RectF height is calculated correctly`() {
        val rect = RectF(10f, 20f, 110f, 70f)
        assertEquals(50f, rect.height)
    }

    @Test
    fun `RectF contains returns true for point inside`() {
        val rect = RectF(0f, 0f, 100f, 100f)
        assertTrue(rect.contains(50f, 50f))
        assertTrue(rect.contains(0f, 0f))
        assertTrue(rect.contains(99.9f, 99.9f))
    }

    @Test
    fun `RectF contains returns false for point outside`() {
        val rect = RectF(0f, 0f, 100f, 100f)
        assertFalse(rect.contains(-1f, 50f))
        assertFalse(rect.contains(50f, -1f))
        assertFalse(rect.contains(101f, 50f))
        assertFalse(rect.contains(50f, 101f))
    }
}
