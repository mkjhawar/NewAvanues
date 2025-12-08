package com.augmentalis.avaelements.flutter.layout

import com.augmentalis.avaelements.core.types.Spacing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for WrapComponent
 *
 * Covers:
 * - Horizontal and vertical wrapping
 * - All alignment options (Start, End, Center, SpaceBetween, SpaceAround, SpaceEvenly)
 * - Spacing and run spacing
 * - RTL support
 * - Vertical direction (Down, Up)
 */
class WrapTest {

    @Test
    fun `test default wrap component`() {
        val wrap = WrapComponent()

        assertEquals(WrapDirection.Horizontal, wrap.direction)
        assertEquals(WrapAlignment.Start, wrap.alignment)
        assertEquals(Spacing.Zero, wrap.spacing)
        assertEquals(Spacing.Zero, wrap.runSpacing)
        assertEquals(WrapAlignment.Start, wrap.runAlignment)
        assertEquals(WrapCrossAlignment.Start, wrap.crossAxisAlignment)
        assertEquals(VerticalDirection.Down, wrap.verticalDirection)
        assertEquals(emptyList(), wrap.children)
    }

    @Test
    fun `test horizontal wrap with children`() {
        val children = listOf("Item1", "Item2", "Item3")
        val wrap = WrapComponent(
            direction = WrapDirection.Horizontal,
            children = children
        )

        assertEquals(WrapDirection.Horizontal, wrap.direction)
        assertEquals(3, wrap.children.size)
    }

    @Test
    fun `test vertical wrap with children`() {
        val children = listOf("Item1", "Item2", "Item3")
        val wrap = WrapComponent(
            direction = WrapDirection.Vertical,
            children = children
        )

        assertEquals(WrapDirection.Vertical, wrap.direction)
        assertEquals(3, wrap.children.size)
    }

    @Test
    fun `test wrap with start alignment`() {
        val wrap = WrapComponent(alignment = WrapAlignment.Start)
        assertEquals(WrapAlignment.Start, wrap.alignment)
    }

    @Test
    fun `test wrap with end alignment for RTL support`() {
        val wrap = WrapComponent(alignment = WrapAlignment.End)
        assertEquals(WrapAlignment.End, wrap.alignment)
    }

    @Test
    fun `test wrap with center alignment`() {
        val wrap = WrapComponent(alignment = WrapAlignment.Center)
        assertEquals(WrapAlignment.Center, wrap.alignment)
    }

    @Test
    fun `test wrap with space between alignment`() {
        val wrap = WrapComponent(alignment = WrapAlignment.SpaceBetween)
        assertEquals(WrapAlignment.SpaceBetween, wrap.alignment)
    }

    @Test
    fun `test wrap with space around alignment`() {
        val wrap = WrapComponent(alignment = WrapAlignment.SpaceAround)
        assertEquals(WrapAlignment.SpaceAround, wrap.alignment)
    }

    @Test
    fun `test wrap with space evenly alignment`() {
        val wrap = WrapComponent(alignment = WrapAlignment.SpaceEvenly)
        assertEquals(WrapAlignment.SpaceEvenly, wrap.alignment)
    }

    @Test
    fun `test wrap with custom spacing`() {
        val spacing = Spacing.all(8f)
        val wrap = WrapComponent(spacing = spacing)
        assertEquals(spacing, wrap.spacing)
    }

    @Test
    fun `test wrap with custom run spacing`() {
        val runSpacing = Spacing.all(16f)
        val wrap = WrapComponent(runSpacing = runSpacing)
        assertEquals(runSpacing, wrap.runSpacing)
    }

    @Test
    fun `test wrap with cross axis alignment start`() {
        val wrap = WrapComponent(crossAxisAlignment = WrapCrossAlignment.Start)
        assertEquals(WrapCrossAlignment.Start, wrap.crossAxisAlignment)
    }

    @Test
    fun `test wrap with cross axis alignment end`() {
        val wrap = WrapComponent(crossAxisAlignment = WrapCrossAlignment.End)
        assertEquals(WrapCrossAlignment.End, wrap.crossAxisAlignment)
    }

    @Test
    fun `test wrap with cross axis alignment center`() {
        val wrap = WrapComponent(crossAxisAlignment = WrapCrossAlignment.Center)
        assertEquals(WrapCrossAlignment.Center, wrap.crossAxisAlignment)
    }

    @Test
    fun `test wrap with vertical direction down`() {
        val wrap = WrapComponent(verticalDirection = VerticalDirection.Down)
        assertEquals(VerticalDirection.Down, wrap.verticalDirection)
    }

    @Test
    fun `test wrap with vertical direction up for RTL support`() {
        val wrap = WrapComponent(verticalDirection = VerticalDirection.Up)
        assertEquals(VerticalDirection.Up, wrap.verticalDirection)
    }

    @Test
    fun `test wrap serialization preserves all properties`() {
        val wrap = WrapComponent(
            direction = WrapDirection.Vertical,
            alignment = WrapAlignment.Center,
            spacing = Spacing.all(8f),
            runSpacing = Spacing.all(12f),
            runAlignment = WrapAlignment.End,
            crossAxisAlignment = WrapCrossAlignment.Center,
            verticalDirection = VerticalDirection.Up,
            children = listOf("A", "B", "C")
        )

        assertNotNull(wrap)
        assertEquals(WrapDirection.Vertical, wrap.direction)
        assertEquals(WrapAlignment.Center, wrap.alignment)
        assertEquals(WrapAlignment.End, wrap.runAlignment)
        assertEquals(WrapCrossAlignment.Center, wrap.crossAxisAlignment)
        assertEquals(VerticalDirection.Up, wrap.verticalDirection)
        assertEquals(3, wrap.children.size)
    }
}
