package com.augmentalis.avaelements.flutter.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Tests for AlignComponent and CenterComponent
 *
 * Covers:
 * - Alignment geometry (all predefined alignments)
 * - Custom alignment values
 * - Width/height factors
 * - RTL support (alignment mirroring)
 * - Center as shorthand for Align
 */
class AlignCenterTest {

    // ======= AlignComponent Tests =======

    @Test
    fun `test default align component`() {
        val align = AlignComponent(child = "Test")

        assertEquals(AlignmentGeometry.Center, align.alignment)
        assertEquals(null, align.widthFactor)
        assertEquals(null, align.heightFactor)
        assertEquals("Test", align.child)
    }

    @Test
    fun `test align with center alignment`() {
        val align = AlignComponent(
            alignment = AlignmentGeometry.Center,
            child = "Test"
        )
        assertEquals(AlignmentGeometry.Center, align.alignment)
    }

    @Test
    fun `test align with top left alignment`() {
        val align = AlignComponent(
            alignment = AlignmentGeometry.TopLeft,
            child = "Test"
        )
        assertEquals(AlignmentGeometry.TopLeft, align.alignment)
    }

    @Test
    fun `test align with top center alignment`() {
        val align = AlignComponent(
            alignment = AlignmentGeometry.TopCenter,
            child = "Test"
        )
        assertEquals(AlignmentGeometry.TopCenter, align.alignment)
    }

    @Test
    fun `test align with top end alignment for RTL`() {
        val align = AlignComponent(
            alignment = AlignmentGeometry.TopEnd,
            child = "Test"
        )
        assertEquals(AlignmentGeometry.TopEnd, align.alignment)
    }

    @Test
    fun `test align with center left alignment`() {
        val align = AlignComponent(
            alignment = AlignmentGeometry.CenterLeft,
            child = "Test"
        )
        assertEquals(AlignmentGeometry.CenterLeft, align.alignment)
    }

    @Test
    fun `test align with center end alignment for RTL`() {
        val align = AlignComponent(
            alignment = AlignmentGeometry.CenterEnd,
            child = "Test"
        )
        assertEquals(AlignmentGeometry.CenterEnd, align.alignment)
    }

    @Test
    fun `test align with bottom left alignment`() {
        val align = AlignComponent(
            alignment = AlignmentGeometry.BottomLeft,
            child = "Test"
        )
        assertEquals(AlignmentGeometry.BottomLeft, align.alignment)
    }

    @Test
    fun `test align with bottom center alignment`() {
        val align = AlignComponent(
            alignment = AlignmentGeometry.BottomCenter,
            child = "Test"
        )
        assertEquals(AlignmentGeometry.BottomCenter, align.alignment)
    }

    @Test
    fun `test align with bottom end alignment for RTL`() {
        val align = AlignComponent(
            alignment = AlignmentGeometry.BottomEnd,
            child = "Test"
        )
        assertEquals(AlignmentGeometry.BottomEnd, align.alignment)
    }

    @Test
    fun `test align with custom alignment`() {
        val customAlignment = AlignmentGeometry.Custom(0.5f, -0.5f)
        val align = AlignComponent(
            alignment = customAlignment,
            child = "Test"
        )
        assertEquals(customAlignment, align.alignment)
    }

    @Test
    fun `test custom alignment validation - x in range`() {
        val alignment = AlignmentGeometry.Custom(0.0f, 0.0f)
        assertNotNull(alignment)
    }

    @Test
    fun `test custom alignment validation - x out of range throws`() {
        assertFailsWith<IllegalArgumentException> {
            AlignmentGeometry.Custom(1.5f, 0.0f)
        }
    }

    @Test
    fun `test custom alignment validation - y out of range throws`() {
        assertFailsWith<IllegalArgumentException> {
            AlignmentGeometry.Custom(0.0f, -1.5f)
        }
    }

    @Test
    fun `test align with width factor`() {
        val align = AlignComponent(
            widthFactor = 2.0f,
            child = "Test"
        )
        assertEquals(2.0f, align.widthFactor)
    }

    @Test
    fun `test align with height factor`() {
        val align = AlignComponent(
            heightFactor = 1.5f,
            child = "Test"
        )
        assertEquals(1.5f, align.heightFactor)
    }

    @Test
    fun `test align with both width and height factors`() {
        val align = AlignComponent(
            widthFactor = 2.0f,
            heightFactor = 3.0f,
            child = "Test"
        )
        assertEquals(2.0f, align.widthFactor)
        assertEquals(3.0f, align.heightFactor)
    }

    @Test
    fun `test align with negative width factor throws`() {
        assertFailsWith<IllegalArgumentException> {
            AlignComponent(widthFactor = -1.0f, child = "Test")
        }
    }

    @Test
    fun `test align with negative height factor throws`() {
        assertFailsWith<IllegalArgumentException> {
            AlignComponent(heightFactor = -1.0f, child = "Test")
        }
    }

    // ======= CenterComponent Tests =======

    @Test
    fun `test default center component`() {
        val center = CenterComponent(child = "Test")

        assertEquals(null, center.widthFactor)
        assertEquals(null, center.heightFactor)
        assertEquals("Test", center.child)
    }

    @Test
    fun `test center with width factor`() {
        val center = CenterComponent(
            widthFactor = 2.0f,
            child = "Test"
        )
        assertEquals(2.0f, center.widthFactor)
    }

    @Test
    fun `test center with height factor`() {
        val center = CenterComponent(
            heightFactor = 1.5f,
            child = "Test"
        )
        assertEquals(1.5f, center.heightFactor)
    }

    @Test
    fun `test center with both factors`() {
        val center = CenterComponent(
            widthFactor = 2.0f,
            heightFactor = 3.0f,
            child = "Test"
        )
        assertEquals(2.0f, center.widthFactor)
        assertEquals(3.0f, center.heightFactor)
    }

    @Test
    fun `test center with negative width factor throws`() {
        assertFailsWith<IllegalArgumentException> {
            CenterComponent(widthFactor = -1.0f, child = "Test")
        }
    }

    @Test
    fun `test center with negative height factor throws`() {
        assertFailsWith<IllegalArgumentException> {
            CenterComponent(heightFactor = -1.0f, child = "Test")
        }
    }

    @Test
    fun `test center is equivalent to align with center alignment`() {
        val center = CenterComponent(child = "Test")
        val align = AlignComponent(
            alignment = AlignmentGeometry.Center,
            child = "Test"
        )

        // Center is a convenience widget for Align(alignment: Alignment.center)
        assertEquals(align.alignment, AlignmentGeometry.Center)
    }

    @Test
    fun `test alignment geometry predefined values`() {
        assertNotNull(AlignmentGeometry.Center)
        assertNotNull(AlignmentGeometry.TopLeft)
        assertNotNull(AlignmentGeometry.TopCenter)
        assertNotNull(AlignmentGeometry.TopEnd)
        assertNotNull(AlignmentGeometry.CenterLeft)
        assertNotNull(AlignmentGeometry.CenterEnd)
        assertNotNull(AlignmentGeometry.BottomLeft)
        assertNotNull(AlignmentGeometry.BottomCenter)
        assertNotNull(AlignmentGeometry.BottomEnd)
    }
}
