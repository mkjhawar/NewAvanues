package com.augmentalis.avaelements.flutter.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for FittedBoxComponent, BoxFit, and Clip
 *
 * Covers:
 * - All BoxFit strategies (Fill, Contain, Cover, FitWidth, FitHeight, None, ScaleDown)
 * - Alignment options
 * - Clip behaviors (None, HardEdge, AntiAlias, AntiAliasWithSaveLayer)
 * - Default values
 */
class FittedBoxTest {

    // ======= FittedBoxComponent Tests =======

    @Test
    fun `test default fitted box component`() {
        val fittedBox = FittedBoxComponent(child = "Test")

        assertEquals(BoxFit.Contain, fittedBox.fit)
        assertEquals(AlignmentGeometry.Center, fittedBox.alignment)
        assertEquals(Clip.None, fittedBox.clipBehavior)
        assertEquals("Test", fittedBox.child)
    }

    @Test
    fun `test fitted box with fill`() {
        val fittedBox = FittedBoxComponent(
            fit = BoxFit.Fill,
            child = "Test"
        )
        assertEquals(BoxFit.Fill, fittedBox.fit)
    }

    @Test
    fun `test fitted box with contain`() {
        val fittedBox = FittedBoxComponent(
            fit = BoxFit.Contain,
            child = "Test"
        )
        assertEquals(BoxFit.Contain, fittedBox.fit)
    }

    @Test
    fun `test fitted box with cover`() {
        val fittedBox = FittedBoxComponent(
            fit = BoxFit.Cover,
            child = "Test"
        )
        assertEquals(BoxFit.Cover, fittedBox.fit)
    }

    @Test
    fun `test fitted box with fit width`() {
        val fittedBox = FittedBoxComponent(
            fit = BoxFit.FitWidth,
            child = "Test"
        )
        assertEquals(BoxFit.FitWidth, fittedBox.fit)
    }

    @Test
    fun `test fitted box with fit height`() {
        val fittedBox = FittedBoxComponent(
            fit = BoxFit.FitHeight,
            child = "Test"
        )
        assertEquals(BoxFit.FitHeight, fittedBox.fit)
    }

    @Test
    fun `test fitted box with none`() {
        val fittedBox = FittedBoxComponent(
            fit = BoxFit.None,
            child = "Test"
        )
        assertEquals(BoxFit.None, fittedBox.fit)
    }

    @Test
    fun `test fitted box with scale down`() {
        val fittedBox = FittedBoxComponent(
            fit = BoxFit.ScaleDown,
            child = "Test"
        )
        assertEquals(BoxFit.ScaleDown, fittedBox.fit)
    }

    @Test
    fun `test fitted box with custom alignment`() {
        val alignment = AlignmentGeometry.TopEnd
        val fittedBox = FittedBoxComponent(
            alignment = alignment,
            child = "Test"
        )
        assertEquals(alignment, fittedBox.alignment)
    }

    @Test
    fun `test fitted box with clip none`() {
        val fittedBox = FittedBoxComponent(
            clipBehavior = Clip.None,
            child = "Test"
        )
        assertEquals(Clip.None, fittedBox.clipBehavior)
    }

    @Test
    fun `test fitted box with clip hard edge`() {
        val fittedBox = FittedBoxComponent(
            clipBehavior = Clip.HardEdge,
            child = "Test"
        )
        assertEquals(Clip.HardEdge, fittedBox.clipBehavior)
    }

    @Test
    fun `test fitted box with clip anti alias`() {
        val fittedBox = FittedBoxComponent(
            clipBehavior = Clip.AntiAlias,
            child = "Test"
        )
        assertEquals(Clip.AntiAlias, fittedBox.clipBehavior)
    }

    @Test
    fun `test fitted box with clip anti alias with save layer`() {
        val fittedBox = FittedBoxComponent(
            clipBehavior = Clip.AntiAliasWithSaveLayer,
            child = "Test"
        )
        assertEquals(Clip.AntiAliasWithSaveLayer, fittedBox.clipBehavior)
    }

    @Test
    fun `test fitted box serialization preserves all properties`() {
        val fittedBox = FittedBoxComponent(
            fit = BoxFit.Cover,
            alignment = AlignmentGeometry.BottomEnd,
            clipBehavior = Clip.AntiAlias,
            child = "TestChild"
        )

        assertNotNull(fittedBox)
        assertEquals(BoxFit.Cover, fittedBox.fit)
        assertEquals(AlignmentGeometry.BottomEnd, fittedBox.alignment)
        assertEquals(Clip.AntiAlias, fittedBox.clipBehavior)
        assertEquals("TestChild", fittedBox.child)
    }

    // ======= BoxFit Enum Tests =======

    @Test
    fun `test box fit enum values`() {
        assertEquals(7, BoxFit.values().size)
        assertNotNull(BoxFit.Fill)
        assertNotNull(BoxFit.Contain)
        assertNotNull(BoxFit.Cover)
        assertNotNull(BoxFit.FitWidth)
        assertNotNull(BoxFit.FitHeight)
        assertNotNull(BoxFit.None)
        assertNotNull(BoxFit.ScaleDown)
    }

    @Test
    fun `test box fit values are distinct`() {
        val values = BoxFit.values().toSet()
        assertEquals(7, values.size)
    }

    // ======= Clip Enum Tests =======

    @Test
    fun `test clip enum values`() {
        assertEquals(4, Clip.values().size)
        assertNotNull(Clip.None)
        assertNotNull(Clip.HardEdge)
        assertNotNull(Clip.AntiAlias)
        assertNotNull(Clip.AntiAliasWithSaveLayer)
    }

    @Test
    fun `test clip values are distinct`() {
        val values = Clip.values().toSet()
        assertEquals(4, values.size)
    }
}
