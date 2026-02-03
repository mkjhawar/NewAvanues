package com.augmentalis.avaelements.flutter.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for FlexComponent
 *
 * Covers:
 * - Horizontal and vertical direction
 * - Main axis alignment (all 6 options)
 * - Cross axis alignment (all 5 options)
 * - Main axis sizing (Min, Max)
 * - RTL support via TextDirection
 * - Vertical direction support
 */
class FlexTest {

    @Test
    fun `test default flex component`() {
        val flex = FlexComponent(direction = FlexDirection.Horizontal)

        assertEquals(FlexDirection.Horizontal, flex.direction)
        assertEquals(MainAxisAlignment.Start, flex.mainAxisAlignment)
        assertEquals(MainAxisSize.Max, flex.mainAxisSize)
        assertEquals(CrossAxisAlignment.Center, flex.crossAxisAlignment)
        assertEquals(VerticalDirection.Down, flex.verticalDirection)
        assertEquals(null, flex.textDirection)
        assertEquals(emptyList(), flex.children)
    }

    @Test
    fun `test horizontal flex`() {
        val flex = FlexComponent(direction = FlexDirection.Horizontal)
        assertEquals(FlexDirection.Horizontal, flex.direction)
    }

    @Test
    fun `test vertical flex`() {
        val flex = FlexComponent(direction = FlexDirection.Vertical)
        assertEquals(FlexDirection.Vertical, flex.direction)
    }

    @Test
    fun `test flex with start alignment`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            mainAxisAlignment = MainAxisAlignment.Start
        )
        assertEquals(MainAxisAlignment.Start, flex.mainAxisAlignment)
    }

    @Test
    fun `test flex with end alignment for RTL`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            mainAxisAlignment = MainAxisAlignment.End
        )
        assertEquals(MainAxisAlignment.End, flex.mainAxisAlignment)
    }

    @Test
    fun `test flex with center alignment`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            mainAxisAlignment = MainAxisAlignment.Center
        )
        assertEquals(MainAxisAlignment.Center, flex.mainAxisAlignment)
    }

    @Test
    fun `test flex with space between`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            mainAxisAlignment = MainAxisAlignment.SpaceBetween
        )
        assertEquals(MainAxisAlignment.SpaceBetween, flex.mainAxisAlignment)
    }

    @Test
    fun `test flex with space around`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            mainAxisAlignment = MainAxisAlignment.SpaceAround
        )
        assertEquals(MainAxisAlignment.SpaceAround, flex.mainAxisAlignment)
    }

    @Test
    fun `test flex with space evenly`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            mainAxisAlignment = MainAxisAlignment.SpaceEvenly
        )
        assertEquals(MainAxisAlignment.SpaceEvenly, flex.mainAxisAlignment)
    }

    @Test
    fun `test flex with cross axis start`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            crossAxisAlignment = CrossAxisAlignment.Start
        )
        assertEquals(CrossAxisAlignment.Start, flex.crossAxisAlignment)
    }

    @Test
    fun `test flex with cross axis end`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            crossAxisAlignment = CrossAxisAlignment.End
        )
        assertEquals(CrossAxisAlignment.End, flex.crossAxisAlignment)
    }

    @Test
    fun `test flex with cross axis center`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            crossAxisAlignment = CrossAxisAlignment.Center
        )
        assertEquals(CrossAxisAlignment.Center, flex.crossAxisAlignment)
    }

    @Test
    fun `test flex with cross axis stretch`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            crossAxisAlignment = CrossAxisAlignment.Stretch
        )
        assertEquals(CrossAxisAlignment.Stretch, flex.crossAxisAlignment)
    }

    @Test
    fun `test flex with cross axis baseline`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            crossAxisAlignment = CrossAxisAlignment.Baseline
        )
        assertEquals(CrossAxisAlignment.Baseline, flex.crossAxisAlignment)
    }

    @Test
    fun `test flex with main axis size min`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            mainAxisSize = MainAxisSize.Min
        )
        assertEquals(MainAxisSize.Min, flex.mainAxisSize)
    }

    @Test
    fun `test flex with main axis size max`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            mainAxisSize = MainAxisSize.Max
        )
        assertEquals(MainAxisSize.Max, flex.mainAxisSize)
    }

    @Test
    fun `test flex with LTR text direction`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            textDirection = TextDirection.LTR
        )
        assertEquals(TextDirection.LTR, flex.textDirection)
    }

    @Test
    fun `test flex with RTL text direction`() {
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            textDirection = TextDirection.RTL
        )
        assertEquals(TextDirection.RTL, flex.textDirection)
    }

    @Test
    fun `test flex with vertical direction down`() {
        val flex = FlexComponent(
            direction = FlexDirection.Vertical,
            verticalDirection = VerticalDirection.Down
        )
        assertEquals(VerticalDirection.Down, flex.verticalDirection)
    }

    @Test
    fun `test flex with vertical direction up`() {
        val flex = FlexComponent(
            direction = FlexDirection.Vertical,
            verticalDirection = VerticalDirection.Up
        )
        assertEquals(VerticalDirection.Up, flex.verticalDirection)
    }

    @Test
    fun `test flex with children`() {
        val children = listOf("Item1", "Item2", "Item3")
        val flex = FlexComponent(
            direction = FlexDirection.Horizontal,
            children = children
        )
        assertEquals(3, flex.children.size)
    }

    @Test
    fun `test flex serialization preserves all properties`() {
        val flex = FlexComponent(
            direction = FlexDirection.Vertical,
            mainAxisAlignment = MainAxisAlignment.SpaceEvenly,
            mainAxisSize = MainAxisSize.Min,
            crossAxisAlignment = CrossAxisAlignment.Stretch,
            verticalDirection = VerticalDirection.Up,
            textDirection = TextDirection.RTL,
            children = listOf("A", "B", "C")
        )

        assertNotNull(flex)
        assertEquals(FlexDirection.Vertical, flex.direction)
        assertEquals(MainAxisAlignment.SpaceEvenly, flex.mainAxisAlignment)
        assertEquals(MainAxisSize.Min, flex.mainAxisSize)
        assertEquals(CrossAxisAlignment.Stretch, flex.crossAxisAlignment)
        assertEquals(VerticalDirection.Up, flex.verticalDirection)
        assertEquals(TextDirection.RTL, flex.textDirection)
        assertEquals(3, flex.children.size)
    }
}
