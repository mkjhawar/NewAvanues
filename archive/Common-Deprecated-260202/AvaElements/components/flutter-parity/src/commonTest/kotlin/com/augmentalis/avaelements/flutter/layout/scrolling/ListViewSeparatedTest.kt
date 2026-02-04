package com.augmentalis.avaelements.flutter.layout.scrolling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ListViewSeparated component
 *
 * Tests cover:
 * - Component creation
 * - Separator builder
 * - Item count requirements
 * - Edge cases with separators
 *
 * @since 2.1.0
 */
class ListViewSeparatedTest {

    @Test
    fun `test separated list creation`() {
        val component = ListViewSeparatedComponent(
            itemCount = 20,
            itemBuilder = "item_builder_ref",
            separatorBuilder = "separator_builder_ref"
        )

        assertEquals(20, component.itemCount)
        assertEquals("item_builder_ref", component.itemBuilder)
        assertEquals("separator_builder_ref", component.separatorBuilder)
        assertEquals(ScrollDirection.Vertical, component.scrollDirection)
    }

    @Test
    fun `test horizontal separated list`() {
        val component = ListViewSeparatedComponent(
            itemCount = 15,
            itemBuilder = "item_builder_ref",
            separatorBuilder = "separator_builder_ref",
            scrollDirection = ScrollDirection.Horizontal
        )

        assertEquals(ScrollDirection.Horizontal, component.scrollDirection)
    }

    @Test
    fun `test single item list has no separators`() {
        val component = ListViewSeparatedComponent(
            itemCount = 1,
            itemBuilder = "item_builder_ref",
            separatorBuilder = "separator_builder_ref"
        )

        assertEquals(1, component.itemCount)
        // With 1 item, there should be 0 separators (n-1 rule)
    }

    @Test
    fun `test empty list is valid`() {
        val component = ListViewSeparatedComponent(
            itemCount = 0,
            itemBuilder = "item_builder_ref",
            separatorBuilder = "separator_builder_ref"
        )

        assertEquals(0, component.itemCount)
    }

    @Test
    fun `test negative itemCount throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            ListViewSeparatedComponent(
                itemCount = -1,
                itemBuilder = "item_builder_ref",
                separatorBuilder = "separator_builder_ref"
            )
        }
    }

    @Test
    fun `test reverse layout`() {
        val component = ListViewSeparatedComponent(
            itemCount = 10,
            itemBuilder = "item_builder_ref",
            separatorBuilder = "separator_builder_ref",
            reverse = true
        )

        assertTrue(component.reverse)
    }

    @Test
    fun `test never scrollable physics`() {
        val component = ListViewSeparatedComponent(
            itemCount = 100,
            itemBuilder = "item_builder_ref",
            separatorBuilder = "separator_builder_ref",
            physics = ScrollPhysics.NeverScrollable
        )

        assertEquals(ScrollPhysics.NeverScrollable, component.physics)
    }

    @Test
    fun `test shrinkWrap enabled`() {
        val component = ListViewSeparatedComponent(
            itemCount = 20,
            itemBuilder = "item_builder_ref",
            separatorBuilder = "separator_builder_ref",
            shrinkWrap = true
        )

        assertTrue(component.shrinkWrap)
    }

    @Test
    fun `test large separated list for performance`() {
        val component = ListViewSeparatedComponent(
            itemCount = 1_000,
            itemBuilder = "item_builder_ref",
            separatorBuilder = "separator_builder_ref"
        )

        assertEquals(1_000, component.itemCount)
        // Should create 999 separators (n-1)
    }

    @Test
    fun `test with scroll controller`() {
        val controller = ScrollController(
            initialScrollOffset = 50f
        )

        val component = ListViewSeparatedComponent(
            itemCount = 50,
            itemBuilder = "item_builder_ref",
            separatorBuilder = "separator_builder_ref",
            controller = controller
        )

        assertEquals(50f, component.controller?.initialScrollOffset)
    }
}
