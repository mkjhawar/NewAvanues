package com.augmentalis.avaelements.flutter.layout.scrolling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ListViewBuilder component
 *
 * Tests cover:
 * - Component creation and validation
 * - Property defaults
 * - Constraint validation
 * - Edge cases
 * - Performance considerations
 *
 * @since 2.1.0
 */
class ListViewBuilderTest {

    @Test
    fun `test finite list creation`() {
        val component = ListViewBuilderComponent(
            itemCount = 100,
            itemBuilder = "builder_ref"
        )

        assertEquals(100, component.itemCount)
        assertEquals("builder_ref", component.itemBuilder)
        assertEquals(ScrollDirection.Vertical, component.scrollDirection)
        assertFalse(component.reverse)
        assertFalse(component.shrinkWrap)
        assertEquals(ScrollPhysics.AlwaysScrollable, component.physics)
    }

    @Test
    fun `test infinite list creation`() {
        val component = ListViewBuilderComponent(
            itemCount = null,
            itemBuilder = "builder_ref"
        )

        assertEquals(null, component.itemCount)
        assertEquals("builder_ref", component.itemBuilder)
    }

    @Test
    fun `test horizontal scroll direction`() {
        val component = ListViewBuilderComponent(
            itemCount = 50,
            itemBuilder = "builder_ref",
            scrollDirection = ScrollDirection.Horizontal
        )

        assertEquals(ScrollDirection.Horizontal, component.scrollDirection)
    }

    @Test
    fun `test reverse layout`() {
        val component = ListViewBuilderComponent(
            itemCount = 50,
            itemBuilder = "builder_ref",
            reverse = true
        )

        assertTrue(component.reverse)
    }

    @Test
    fun `test with scroll controller`() {
        val controller = ScrollController(
            initialScrollOffset = 100f,
            keepScrollOffset = true
        )

        val component = ListViewBuilderComponent(
            itemCount = 100,
            itemBuilder = "builder_ref",
            controller = controller
        )

        assertEquals(100f, component.controller?.initialScrollOffset)
        assertTrue(component.controller?.keepScrollOffset ?: false)
    }

    @Test
    fun `test with item extent`() {
        val component = ListViewBuilderComponent(
            itemCount = 100,
            itemBuilder = "builder_ref",
            itemExtent = 50f
        )

        assertEquals(50f, component.itemExtent)
    }

    @Test
    fun `test shrinkWrap enabled`() {
        val component = ListViewBuilderComponent(
            itemCount = 20,
            itemBuilder = "builder_ref",
            shrinkWrap = true
        )

        assertTrue(component.shrinkWrap)
    }

    @Test
    fun `test never scrollable physics`() {
        val component = ListViewBuilderComponent(
            itemCount = 100,
            itemBuilder = "builder_ref",
            physics = ScrollPhysics.NeverScrollable
        )

        assertEquals(ScrollPhysics.NeverScrollable, component.physics)
    }

    @Test
    fun `test bouncing scroll physics`() {
        val component = ListViewBuilderComponent(
            itemCount = 100,
            itemBuilder = "builder_ref",
            physics = ScrollPhysics.Bouncing
        )

        assertEquals(ScrollPhysics.Bouncing, component.physics)
    }

    @Test
    fun `test negative itemCount throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            ListViewBuilderComponent(
                itemCount = -1,
                itemBuilder = "builder_ref"
            )
        }
    }

    @Test
    fun `test zero itemCount is valid`() {
        val component = ListViewBuilderComponent(
            itemCount = 0,
            itemBuilder = "builder_ref"
        )

        assertEquals(0, component.itemCount)
    }

    @Test
    fun `test negative itemExtent throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            ListViewBuilderComponent(
                itemCount = 100,
                itemBuilder = "builder_ref",
                itemExtent = -10f
            )
        }
    }

    @Test
    fun `test zero itemExtent throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            ListViewBuilderComponent(
                itemCount = 100,
                itemBuilder = "builder_ref",
                itemExtent = 0f
            )
        }
    }

    @Test
    fun `test large item count for performance`() {
        // Should handle large lists efficiently
        val component = ListViewBuilderComponent(
            itemCount = 10_000,
            itemBuilder = "builder_ref"
        )

        assertEquals(10_000, component.itemCount)
    }

    @Test
    fun `test very large item count for performance`() {
        // Should handle very large lists (stress test)
        val component = ListViewBuilderComponent(
            itemCount = 1_000_000,
            itemBuilder = "builder_ref"
        )

        assertEquals(1_000_000, component.itemCount)
    }
}
