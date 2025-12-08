package com.augmentalis.avaelements.flutter.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for LayoutUtilities helper functions
 *
 * Covers:
 * - BoxFit size calculations
 * - Alignment offset calculations
 * - Constraint merging
 * - Flex space distribution
 * - Main axis spacing calculations
 * - RTL mirroring
 * - String parsing utilities
 * - Extension functions
 */
class LayoutUtilitiesTest {

    // ======= BoxFit Calculations =======

    @Test
    fun `test calculate fitted size - fill distorts aspect ratio`() {
        val (width, height) = calculateFittedSize(
            containerWidth = 200f,
            containerHeight = 100f,
            childWidth = 100f,
            childHeight = 100f,
            fit = BoxFit.Fill
        )

        assertEquals(200f, width)
        assertEquals(100f, height)
    }

    @Test
    fun `test calculate fitted size - contain maintains aspect ratio`() {
        val (width, height) = calculateFittedSize(
            containerWidth = 200f,
            containerHeight = 100f,
            childWidth = 100f,
            childHeight = 100f,
            fit = BoxFit.Contain
        )

        // Child is square, container is wider - scale by height
        assertEquals(100f, width)
        assertEquals(100f, height)
    }

    @Test
    fun `test calculate fitted size - cover fills container`() {
        val (width, height) = calculateFittedSize(
            containerWidth = 200f,
            containerHeight = 100f,
            childWidth = 100f,
            childHeight = 100f,
            fit = BoxFit.Cover
        )

        // Scale to cover - child will be clipped
        assertEquals(200f, width)
        assertEquals(200f, height)
    }

    @Test
    fun `test calculate fitted size - fit width`() {
        val (width, height) = calculateFittedSize(
            containerWidth = 200f,
            containerHeight = 100f,
            childWidth = 100f,
            childHeight = 100f,
            fit = BoxFit.FitWidth
        )

        assertEquals(200f, width)
        assertEquals(200f, height) // Maintains aspect ratio
    }

    @Test
    fun `test calculate fitted size - fit height`() {
        val (width, height) = calculateFittedSize(
            containerWidth = 200f,
            containerHeight = 100f,
            childWidth = 100f,
            childHeight = 100f,
            fit = BoxFit.FitHeight
        )

        assertEquals(100f, width)
        assertEquals(100f, height)
    }

    @Test
    fun `test calculate fitted size - none keeps original size`() {
        val (width, height) = calculateFittedSize(
            containerWidth = 200f,
            containerHeight = 100f,
            childWidth = 150f,
            childHeight = 80f,
            fit = BoxFit.None
        )

        assertEquals(150f, width)
        assertEquals(80f, height)
    }

    @Test
    fun `test calculate fitted size - scale down when too large`() {
        val (width, height) = calculateFittedSize(
            containerWidth = 200f,
            containerHeight = 100f,
            childWidth = 300f,
            childHeight = 300f,
            fit = BoxFit.ScaleDown
        )

        // Should scale down like Contain
        assertEquals(100f, width)
        assertEquals(100f, height)
    }

    @Test
    fun `test calculate fitted size - scale down when fits already`() {
        val (width, height) = calculateFittedSize(
            containerWidth = 200f,
            containerHeight = 100f,
            childWidth = 50f,
            childHeight = 50f,
            fit = BoxFit.ScaleDown
        )

        // Should keep original size
        assertEquals(50f, width)
        assertEquals(50f, height)
    }

    // ======= Alignment Calculations =======

    @Test
    fun `test calculate alignment offset - start`() {
        val offset = calculateAlignmentOffset(
            parentSize = 100f,
            childSize = 50f,
            alignment = -1.0f // Start
        )

        assertEquals(0f, offset)
    }

    @Test
    fun `test calculate alignment offset - center`() {
        val offset = calculateAlignmentOffset(
            parentSize = 100f,
            childSize = 50f,
            alignment = 0.0f // Center
        )

        assertEquals(25f, offset)
    }

    @Test
    fun `test calculate alignment offset - end`() {
        val offset = calculateAlignmentOffset(
            parentSize = 100f,
            childSize = 50f,
            alignment = 1.0f // End
        )

        assertEquals(50f, offset)
    }

    // ======= Constraint Merging =======

    @Test
    fun `test merge constraints - tighter child constraints win`() {
        val parent = BoxConstraints(minWidth = 0f, maxWidth = 500f, minHeight = 0f, maxHeight = 500f)
        val child = BoxConstraints(minWidth = 100f, maxWidth = 300f, minHeight = 50f, maxHeight = 200f)

        val merged = mergeConstraints(parent, child)

        assertEquals(100f, merged.minWidth)
        assertEquals(300f, merged.maxWidth)
        assertEquals(50f, merged.minHeight)
        assertEquals(200f, merged.maxHeight)
    }

    // ======= Flex Space Distribution =======

    @Test
    fun `test distribute flex space evenly`() {
        val allocated = distributeFlexSpace(300f, listOf(1, 1, 1))

        assertEquals(3, allocated.size)
        assertEquals(100f, allocated[0])
        assertEquals(100f, allocated[1])
        assertEquals(100f, allocated[2])
    }

    @Test
    fun `test distribute flex space with different factors`() {
        val allocated = distributeFlexSpace(300f, listOf(2, 1, 1))

        assertEquals(3, allocated.size)
        assertEquals(150f, allocated[0])
        assertEquals(75f, allocated[1])
        assertEquals(75f, allocated[2])
    }

    @Test
    fun `test distribute flex space with zero flex`() {
        val allocated = distributeFlexSpace(300f, listOf(0, 0, 0))

        assertEquals(3, allocated.size)
        assertEquals(0f, allocated[0])
        assertEquals(0f, allocated[1])
        assertEquals(0f, allocated[2])
    }

    // ======= Main Axis Spacing =======

    @Test
    fun `test calculate main axis spacing - start`() {
        val (leading, spacing, trailing) = calculateMainAxisSpacing(
            totalSpace = 100f,
            childCount = 3,
            alignment = MainAxisAlignment.Start
        )

        assertEquals(0f, leading)
        assertEquals(0f, spacing)
        assertEquals(100f, trailing)
    }

    @Test
    fun `test calculate main axis spacing - end`() {
        val (leading, spacing, trailing) = calculateMainAxisSpacing(
            totalSpace = 100f,
            childCount = 3,
            alignment = MainAxisAlignment.End
        )

        assertEquals(100f, leading)
        assertEquals(0f, spacing)
        assertEquals(0f, trailing)
    }

    @Test
    fun `test calculate main axis spacing - center`() {
        val (leading, spacing, trailing) = calculateMainAxisSpacing(
            totalSpace = 100f,
            childCount = 3,
            alignment = MainAxisAlignment.Center
        )

        assertEquals(50f, leading)
        assertEquals(0f, spacing)
        assertEquals(50f, trailing)
    }

    @Test
    fun `test calculate main axis spacing - space between`() {
        val (leading, spacing, trailing) = calculateMainAxisSpacing(
            totalSpace = 100f,
            childCount = 3,
            alignment = MainAxisAlignment.SpaceBetween
        )

        assertEquals(0f, leading)
        assertEquals(50f, spacing)
        assertEquals(0f, trailing)
    }

    @Test
    fun `test calculate main axis spacing - space around`() {
        val (leading, spacing, trailing) = calculateMainAxisSpacing(
            totalSpace = 120f,
            childCount = 3,
            alignment = MainAxisAlignment.SpaceAround
        )

        assertEquals(20f, leading)
        assertEquals(40f, spacing)
        assertEquals(20f, trailing)
    }

    @Test
    fun `test calculate main axis spacing - space evenly`() {
        val (leading, spacing, trailing) = calculateMainAxisSpacing(
            totalSpace = 100f,
            childCount = 3,
            alignment = MainAxisAlignment.SpaceEvenly
        )

        assertEquals(25f, leading)
        assertEquals(25f, spacing)
        assertEquals(25f, trailing)
    }

    // ======= RTL Support =======

    @Test
    fun `test mirror alignment for RTL - LTR unchanged`() {
        val mirrored = mirrorAlignmentForRtl(0.5f, isRtl = false)
        assertEquals(0.5f, mirrored)
    }

    @Test
    fun `test mirror alignment for RTL - RTL flipped`() {
        val mirrored = mirrorAlignmentForRtl(0.5f, isRtl = true)
        assertEquals(-0.5f, mirrored)
    }

    // ======= String Parsing =======

    @Test
    fun `test alignment from string - center`() {
        assertEquals(AlignmentGeometry.Center, alignmentFromString("center"))
        assertEquals(AlignmentGeometry.Center, alignmentFromString("CENTER"))
    }

    @Test
    fun `test alignment from string - top left`() {
        assertEquals(AlignmentGeometry.TopLeft, alignmentFromString("topLeft"))
    }

    @Test
    fun `test alignment from string - bottom end`() {
        assertEquals(AlignmentGeometry.BottomEnd, alignmentFromString("bottomEnd"))
        assertEquals(AlignmentGeometry.BottomEnd, alignmentFromString("bottomRight"))
    }

    @Test
    fun `test box fit from string - contain`() {
        assertEquals(BoxFit.Contain, boxFitFromString("contain"))
        assertEquals(BoxFit.Contain, boxFitFromString("CONTAIN"))
    }

    @Test
    fun `test box fit from string - cover`() {
        assertEquals(BoxFit.Cover, boxFitFromString("cover"))
    }

    @Test
    fun `test box fit from string - invalid defaults to contain`() {
        assertEquals(BoxFit.Contain, boxFitFromString("invalid"))
    }

    // ======= Constraint Utilities =======

    @Test
    fun `test has flexibility - true when flexible`() {
        val constraints = BoxConstraints(minWidth = 0f, maxWidth = 100f, minHeight = 0f, maxHeight = 100f)
        assertTrue(hasFlexibility(constraints))
    }

    @Test
    fun `test has flexibility - false when tight`() {
        val constraints = BoxConstraints.tight(100f, 100f)
        assertFalse(hasFlexibility(constraints))
    }

    @Test
    fun `test constrain size within bounds`() {
        val constraints = BoxConstraints(
            minWidth = 100f,
            maxWidth = 300f,
            minHeight = 50f,
            maxHeight = 200f
        )

        val (w, h) = constrainSize(200f, 100f, constraints)
        assertEquals(200f, w)
        assertEquals(100f, h)
    }

    @Test
    fun `test constrain size - clamps to min`() {
        val constraints = BoxConstraints(
            minWidth = 100f,
            maxWidth = 300f,
            minHeight = 50f,
            maxHeight = 200f
        )

        val (w, h) = constrainSize(50f, 25f, constraints)
        assertEquals(100f, w)
        assertEquals(50f, h)
    }

    @Test
    fun `test constrain size - clamps to max`() {
        val constraints = BoxConstraints(
            minWidth = 100f,
            maxWidth = 300f,
            minHeight = 50f,
            maxHeight = 200f
        )

        val (w, h) = constrainSize(400f, 250f, constraints)
        assertEquals(300f, w)
        assertEquals(200f, h)
    }

    // ======= Extension Functions =======

    @Test
    fun `test alignment is start`() {
        assertTrue(AlignmentGeometry.TopLeft.isStart())
        assertTrue(AlignmentGeometry.CenterLeft.isStart())
        assertFalse(AlignmentGeometry.Center.isStart())
    }

    @Test
    fun `test alignment is end`() {
        assertTrue(AlignmentGeometry.TopEnd.isEnd())
        assertTrue(AlignmentGeometry.CenterEnd.isEnd())
        assertFalse(AlignmentGeometry.Center.isEnd())
    }

    @Test
    fun `test alignment is center`() {
        assertTrue(AlignmentGeometry.Center.isCenter())
        assertFalse(AlignmentGeometry.TopLeft.isCenter())
    }

    @Test
    fun `test alignment flip horizontal`() {
        assertEquals(AlignmentGeometry.TopEnd, AlignmentGeometry.TopLeft.flipHorizontal())
        assertEquals(AlignmentGeometry.TopLeft, AlignmentGeometry.TopEnd.flipHorizontal())
        assertEquals(AlignmentGeometry.Center, AlignmentGeometry.Center.flipHorizontal())
    }

    @Test
    fun `test alignment flip vertical`() {
        assertEquals(AlignmentGeometry.BottomLeft, AlignmentGeometry.TopLeft.flipVertical())
        assertEquals(AlignmentGeometry.TopLeft, AlignmentGeometry.BottomLeft.flipVertical())
        assertEquals(AlignmentGeometry.Center, AlignmentGeometry.Center.flipVertical())
    }
}
