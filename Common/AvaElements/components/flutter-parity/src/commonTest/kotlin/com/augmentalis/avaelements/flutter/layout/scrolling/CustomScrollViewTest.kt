package com.augmentalis.avaelements.flutter.layout.scrolling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for CustomScrollView component
 *
 * Tests cover:
 * - Component creation with slivers
 * - Different sliver types
 * - Constraint validation
 * - Edge cases
 *
 * @since 2.1.0
 */
class CustomScrollViewTest {

    @Test
    fun `test custom scroll view with single sliver`() {
        val sliverList = SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = 10
            )
        )

        val component = CustomScrollViewComponent(
            slivers = listOf(sliverList)
        )

        assertEquals(1, component.slivers.size)
        assertEquals(ScrollDirection.Vertical, component.scrollDirection)
        assertFalse(component.reverse)
    }

    @Test
    fun `test custom scroll view with multiple slivers`() {
        val sliverList = SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = 10
            )
        )

        val sliverGrid = SliverGrid(
            gridDelegate = SliverGridDelegate.WithFixedCrossAxisCount(3),
            delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = 9
            )
        )

        val component = CustomScrollViewComponent(
            slivers = listOf(sliverList, sliverGrid)
        )

        assertEquals(2, component.slivers.size)
    }

    @Test
    fun `test empty slivers list throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            CustomScrollViewComponent(
                slivers = emptyList()
            )
        }
    }

    @Test
    fun `test horizontal scroll direction`() {
        val sliverList = SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = 10
            )
        )

        val component = CustomScrollViewComponent(
            slivers = listOf(sliverList),
            scrollDirection = ScrollDirection.Horizontal
        )

        assertEquals(ScrollDirection.Horizontal, component.scrollDirection)
    }

    @Test
    fun `test reverse layout`() {
        val sliverList = SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = 10
            )
        )

        val component = CustomScrollViewComponent(
            slivers = listOf(sliverList),
            reverse = true
        )

        assertTrue(component.reverse)
    }

    @Test
    fun `test anchor validation`() {
        val sliverList = SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = 10
            )
        )

        val component = CustomScrollViewComponent(
            slivers = listOf(sliverList),
            anchor = 0.5f
        )

        assertEquals(0.5f, component.anchor)
    }

    @Test
    fun `test negative anchor throws exception`() {
        val sliverList = SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = 10
            )
        )

        assertFailsWith<IllegalArgumentException> {
            CustomScrollViewComponent(
                slivers = listOf(sliverList),
                anchor = -0.1f
            )
        }
    }

    @Test
    fun `test anchor greater than one throws exception`() {
        val sliverList = SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = 10
            )
        )

        assertFailsWith<IllegalArgumentException> {
            CustomScrollViewComponent(
                slivers = listOf(sliverList),
                anchor = 1.1f
            )
        }
    }

    @Test
    fun `test cache extent`() {
        val sliverList = SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = 10
            )
        )

        val component = CustomScrollViewComponent(
            slivers = listOf(sliverList),
            cacheExtent = 250f
        )

        assertEquals(250f, component.cacheExtent)
    }

    @Test
    fun `test negative cache extent throws exception`() {
        val sliverList = SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = 10
            )
        )

        assertFailsWith<IllegalArgumentException> {
            CustomScrollViewComponent(
                slivers = listOf(sliverList),
                cacheExtent = -100f
            )
        }
    }

    @Test
    fun `test sliver to box adapter`() {
        val adapter = SliverToBoxAdapter(
            child = "header_widget"
        )

        assertEquals("header_widget", adapter.child)
    }

    @Test
    fun `test sliver padding`() {
        val innerSliver = SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = 10
            )
        )

        // Note: Need to import proper Spacing type
        // This is a placeholder test
        // val padding = SliverPadding(
        //     padding = Spacing.all(16f),
        //     sliver = innerSliver
        // )
    }

    @Test
    fun `test sliver fill remaining`() {
        val fillRemaining = SliverFillRemaining(
            child = "footer_widget",
            hasScrollBody = true,
            fillOverscroll = false
        )

        assertEquals("footer_widget", fillRemaining.child)
        assertTrue(fillRemaining.hasScrollBody)
        assertFalse(fillRemaining.fillOverscroll)
    }

    @Test
    fun `test drag start behavior down`() {
        val sliverList = SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = 10
            )
        )

        val component = CustomScrollViewComponent(
            slivers = listOf(sliverList),
            dragStartBehavior = DragStartBehavior.Down
        )

        assertEquals(DragStartBehavior.Down, component.dragStartBehavior)
    }

    @Test
    fun `test drag start behavior start`() {
        val sliverList = SliverList(
            delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = 10
            )
        )

        val component = CustomScrollViewComponent(
            slivers = listOf(sliverList),
            dragStartBehavior = DragStartBehavior.Start
        )

        assertEquals(DragStartBehavior.Start, component.dragStartBehavior)
    }
}
