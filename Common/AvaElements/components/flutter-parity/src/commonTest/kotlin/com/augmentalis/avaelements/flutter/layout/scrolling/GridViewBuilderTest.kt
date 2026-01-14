package com.augmentalis.avaelements.flutter.layout.scrolling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for GridViewBuilder component
 *
 * Tests cover:
 * - Component creation with different grid delegates
 * - Fixed cross-axis count grid
 * - Maximum cross-axis extent grid
 * - Constraint validation
 * - Performance considerations
 *
 * @since 2.1.0
 */
class GridViewBuilderTest {

    @Test
    fun `test grid with fixed cross-axis count`() {
        val delegate = SliverGridDelegate.WithFixedCrossAxisCount(
            crossAxisCount = 3,
            mainAxisSpacing = 8f,
            crossAxisSpacing = 8f,
            childAspectRatio = 1.0f
        )

        val component = GridViewBuilderComponent(
            gridDelegate = delegate,
            itemCount = 100,
            itemBuilder = "builder_ref"
        )

        assertEquals(100, component.itemCount)
        assertEquals("builder_ref", component.itemBuilder)
        assertTrue(component.gridDelegate is SliverGridDelegate.WithFixedCrossAxisCount)
        assertEquals(3, (component.gridDelegate as SliverGridDelegate.WithFixedCrossAxisCount).crossAxisCount)
    }

    @Test
    fun `test grid with max cross-axis extent`() {
        val delegate = SliverGridDelegate.WithMaxCrossAxisExtent(
            maxCrossAxisExtent = 150f,
            mainAxisSpacing = 4f,
            crossAxisSpacing = 4f,
            childAspectRatio = 0.75f
        )

        val component = GridViewBuilderComponent(
            gridDelegate = delegate,
            itemCount = 50,
            itemBuilder = "builder_ref"
        )

        assertEquals(50, component.itemCount)
        assertTrue(component.gridDelegate is SliverGridDelegate.WithMaxCrossAxisExtent)
        assertEquals(150f, (component.gridDelegate as SliverGridDelegate.WithMaxCrossAxisExtent).maxCrossAxisExtent)
    }

    @Test
    fun `test infinite grid`() {
        val delegate = SliverGridDelegate.WithFixedCrossAxisCount(
            crossAxisCount = 2
        )

        val component = GridViewBuilderComponent(
            gridDelegate = delegate,
            itemCount = null,
            itemBuilder = "builder_ref"
        )

        assertEquals(null, component.itemCount)
    }

    @Test
    fun `test horizontal grid`() {
        val delegate = SliverGridDelegate.WithFixedCrossAxisCount(
            crossAxisCount = 3
        )

        val component = GridViewBuilderComponent(
            gridDelegate = delegate,
            itemCount = 100,
            itemBuilder = "builder_ref",
            scrollDirection = ScrollDirection.Horizontal
        )

        assertEquals(ScrollDirection.Horizontal, component.scrollDirection)
    }

    @Test
    fun `test grid with custom spacing`() {
        val delegate = SliverGridDelegate.WithFixedCrossAxisCount(
            crossAxisCount = 4,
            mainAxisSpacing = 16f,
            crossAxisSpacing = 12f
        )

        val component = GridViewBuilderComponent(
            gridDelegate = delegate,
            itemCount = 100,
            itemBuilder = "builder_ref"
        )

        val gridDelegate = component.gridDelegate as SliverGridDelegate.WithFixedCrossAxisCount
        assertEquals(16f, gridDelegate.mainAxisSpacing)
        assertEquals(12f, gridDelegate.crossAxisSpacing)
    }

    @Test
    fun `test grid with custom aspect ratio`() {
        val delegate = SliverGridDelegate.WithFixedCrossAxisCount(
            crossAxisCount = 2,
            childAspectRatio = 1.5f
        )

        val component = GridViewBuilderComponent(
            gridDelegate = delegate,
            itemCount = 50,
            itemBuilder = "builder_ref"
        )

        val gridDelegate = component.gridDelegate as SliverGridDelegate.WithFixedCrossAxisCount
        assertEquals(1.5f, gridDelegate.childAspectRatio)
    }

    @Test
    fun `test negative cross-axis count throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SliverGridDelegate.WithFixedCrossAxisCount(
                crossAxisCount = -1
            )
        }
    }

    @Test
    fun `test zero cross-axis count throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SliverGridDelegate.WithFixedCrossAxisCount(
                crossAxisCount = 0
            )
        }
    }

    @Test
    fun `test negative spacing throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SliverGridDelegate.WithFixedCrossAxisCount(
                crossAxisCount = 3,
                mainAxisSpacing = -8f
            )
        }
    }

    @Test
    fun `test negative aspect ratio throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SliverGridDelegate.WithFixedCrossAxisCount(
                crossAxisCount = 3,
                childAspectRatio = -1.0f
            )
        }
    }

    @Test
    fun `test zero aspect ratio throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SliverGridDelegate.WithFixedCrossAxisCount(
                crossAxisCount = 3,
                childAspectRatio = 0f
            )
        }
    }

    @Test
    fun `test negative max extent throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SliverGridDelegate.WithMaxCrossAxisExtent(
                maxCrossAxisExtent = -100f
            )
        }
    }

    @Test
    fun `test zero max extent throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SliverGridDelegate.WithMaxCrossAxisExtent(
                maxCrossAxisExtent = 0f
            )
        }
    }

    @Test
    fun `test large grid for performance`() {
        val delegate = SliverGridDelegate.WithFixedCrossAxisCount(
            crossAxisCount = 3
        )

        val component = GridViewBuilderComponent(
            gridDelegate = delegate,
            itemCount = 10_000,
            itemBuilder = "builder_ref"
        )

        assertEquals(10_000, component.itemCount)
    }

    @Test
    fun `test grid with main axis extent`() {
        val delegate = SliverGridDelegate.WithFixedCrossAxisCount(
            crossAxisCount = 3,
            mainAxisExtent = 100f
        )

        val component = GridViewBuilderComponent(
            gridDelegate = delegate,
            itemCount = 50,
            itemBuilder = "builder_ref"
        )

        val gridDelegate = component.gridDelegate as SliverGridDelegate.WithFixedCrossAxisCount
        assertEquals(100f, gridDelegate.mainAxisExtent)
    }

    @Test
    fun `test shrinkWrap grid`() {
        val delegate = SliverGridDelegate.WithFixedCrossAxisCount(
            crossAxisCount = 2
        )

        val component = GridViewBuilderComponent(
            gridDelegate = delegate,
            itemCount = 10,
            itemBuilder = "builder_ref",
            shrinkWrap = true
        )

        assertTrue(component.shrinkWrap)
    }
}
