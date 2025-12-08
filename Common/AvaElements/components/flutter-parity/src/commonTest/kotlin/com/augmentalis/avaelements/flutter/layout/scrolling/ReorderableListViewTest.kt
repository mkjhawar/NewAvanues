package com.augmentalis.avaelements.flutter.layout.scrolling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ReorderableListView component
 *
 * Tests cover:
 * - Component creation
 * - Reorder callback
 * - Drag handles
 * - Proxy decorator
 *
 * @since 2.1.0
 */
class ReorderableListViewTest {

    @Test
    fun `test reorderable list creation`() {
        val component = ReorderableListViewComponent(
            itemCount = 10,
            itemBuilder = "item_builder_ref",
            onReorder = "reorder_callback_ref"
        )

        assertEquals(10, component.itemCount)
        assertEquals("item_builder_ref", component.itemBuilder)
        assertEquals("reorder_callback_ref", component.onReorder)
        assertTrue(component.buildDefaultDragHandles)
    }

    @Test
    fun `test horizontal reorderable list`() {
        val component = ReorderableListViewComponent(
            itemCount = 10,
            itemBuilder = "item_builder_ref",
            onReorder = "reorder_callback_ref",
            scrollDirection = ScrollDirection.Horizontal
        )

        assertEquals(ScrollDirection.Horizontal, component.scrollDirection)
    }

    @Test
    fun `test negative itemCount throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            ReorderableListViewComponent(
                itemCount = -1,
                itemBuilder = "item_builder_ref",
                onReorder = "reorder_callback_ref"
            )
        }
    }

    @Test
    fun `test empty list is valid`() {
        val component = ReorderableListViewComponent(
            itemCount = 0,
            itemBuilder = "item_builder_ref",
            onReorder = "reorder_callback_ref"
        )

        assertEquals(0, component.itemCount)
    }

    @Test
    fun `test single item list`() {
        val component = ReorderableListViewComponent(
            itemCount = 1,
            itemBuilder = "item_builder_ref",
            onReorder = "reorder_callback_ref"
        )

        assertEquals(1, component.itemCount)
    }

    @Test
    fun `test without default drag handles`() {
        val component = ReorderableListViewComponent(
            itemCount = 10,
            itemBuilder = "item_builder_ref",
            onReorder = "reorder_callback_ref",
            buildDefaultDragHandles = false
        )

        assertFalse(component.buildDefaultDragHandles)
    }

    @Test
    fun `test with proxy decorator`() {
        val component = ReorderableListViewComponent(
            itemCount = 10,
            itemBuilder = "item_builder_ref",
            onReorder = "reorder_callback_ref",
            proxyDecorator = "proxy_decorator_ref"
        )

        assertEquals("proxy_decorator_ref", component.proxyDecorator)
    }

    @Test
    fun `test with reorder start callback`() {
        val component = ReorderableListViewComponent(
            itemCount = 10,
            itemBuilder = "item_builder_ref",
            onReorder = "reorder_callback_ref",
            onReorderStart = "start_callback_ref"
        )

        assertEquals("start_callback_ref", component.onReorderStart)
    }

    @Test
    fun `test with reorder end callback`() {
        val component = ReorderableListViewComponent(
            itemCount = 10,
            itemBuilder = "item_builder_ref",
            onReorder = "reorder_callback_ref",
            onReorderEnd = "end_callback_ref"
        )

        assertEquals("end_callback_ref", component.onReorderEnd)
    }

    @Test
    fun `test reverse layout`() {
        val component = ReorderableListViewComponent(
            itemCount = 10,
            itemBuilder = "item_builder_ref",
            onReorder = "reorder_callback_ref",
            reverse = true
        )

        assertTrue(component.reverse)
    }

    @Test
    fun `test large reorderable list for performance`() {
        val component = ReorderableListViewComponent(
            itemCount = 500,
            itemBuilder = "item_builder_ref",
            onReorder = "reorder_callback_ref"
        )

        assertEquals(500, component.itemCount)
    }

    @Test
    fun `test reorderable drag start listener`() {
        val listener = ReorderableDragStartListener(
            index = 5,
            child = "child_widget"
        )

        assertEquals(5, listener.index)
        assertEquals("child_widget", listener.child)
    }

    @Test
    fun `test negative index in drag listener throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            ReorderableDragStartListener(
                index = -1,
                child = "child_widget"
            )
        }
    }

    @Test
    fun `test shrinkWrap enabled`() {
        val component = ReorderableListViewComponent(
            itemCount = 10,
            itemBuilder = "item_builder_ref",
            onReorder = "reorder_callback_ref",
            shrinkWrap = true
        )

        assertTrue(component.shrinkWrap)
    }
}
