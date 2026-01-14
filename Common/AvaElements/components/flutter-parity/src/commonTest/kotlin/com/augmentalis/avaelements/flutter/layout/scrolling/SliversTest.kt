package com.augmentalis.avaelements.flutter.layout.scrolling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for Sliver components (SliverList, SliverGrid, SliverFixedExtentList, SliverAppBar)
 *
 * Tests cover:
 * - SliverList with different delegates
 * - SliverGrid with different delegates
 * - SliverFixedExtentList
 * - SliverAppBar configurations
 * - Constraint validation
 *
 * @since 2.1.0
 */
class SliversTest {

    // SliverList Tests

    @Test
    fun `test sliver list with builder delegate`() {
        val delegate = SliverChildDelegate.Builder(
            builder = "builder_ref",
            childCount = 50
        )

        val sliverList = SliverList(delegate = delegate)

        assertTrue(sliverList.delegate is SliverChildDelegate.Builder)
        assertEquals(50, (sliverList.delegate as SliverChildDelegate.Builder).childCount)
    }

    @Test
    fun `test sliver list with infinite children`() {
        val delegate = SliverChildDelegate.Builder(
            builder = "builder_ref",
            childCount = null
        )

        val sliverList = SliverList(delegate = delegate)

        assertEquals(null, (sliverList.delegate as SliverChildDelegate.Builder).childCount)
    }

    @Test
    fun `test sliver list with fixed extent delegate`() {
        val children = listOf("child1", "child2", "child3")
        val delegate = SliverChildDelegate.FixedExtent(
            children = children
        )

        val sliverList = SliverList(delegate = delegate)

        assertTrue(sliverList.delegate is SliverChildDelegate.FixedExtent)
        assertEquals(3, (sliverList.delegate as SliverChildDelegate.FixedExtent).children.size)
    }

    @Test
    fun `test negative child count throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = -1
            )
        }
    }

    @Test
    fun `test empty children list throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SliverChildDelegate.FixedExtent(
                children = emptyList()
            )
        }
    }

    // SliverGrid Tests

    @Test
    fun `test sliver grid with fixed cross-axis count`() {
        val gridDelegate = SliverGridDelegate.WithFixedCrossAxisCount(
            crossAxisCount = 3,
            mainAxisSpacing = 8f,
            crossAxisSpacing = 8f
        )

        val delegate = SliverChildDelegate.Builder(
            builder = "builder_ref",
            childCount = 30
        )

        val sliverGrid = SliverGrid(
            gridDelegate = gridDelegate,
            delegate = delegate
        )

        assertTrue(sliverGrid.gridDelegate is SliverGridDelegate.WithFixedCrossAxisCount)
        assertEquals(3, (sliverGrid.gridDelegate as SliverGridDelegate.WithFixedCrossAxisCount).crossAxisCount)
    }

    @Test
    fun `test sliver grid with max cross-axis extent`() {
        val gridDelegate = SliverGridDelegate.WithMaxCrossAxisExtent(
            maxCrossAxisExtent = 150f,
            mainAxisSpacing = 4f,
            crossAxisSpacing = 4f
        )

        val delegate = SliverChildDelegate.Builder(
            builder = "builder_ref",
            childCount = 20
        )

        val sliverGrid = SliverGrid(
            gridDelegate = gridDelegate,
            delegate = delegate
        )

        assertTrue(sliverGrid.gridDelegate is SliverGridDelegate.WithMaxCrossAxisExtent)
        assertEquals(150f, (sliverGrid.gridDelegate as SliverGridDelegate.WithMaxCrossAxisExtent).maxCrossAxisExtent)
    }

    // SliverFixedExtentList Tests

    @Test
    fun `test sliver fixed extent list`() {
        val delegate = SliverChildDelegate.Builder(
            builder = "builder_ref",
            childCount = 100
        )

        val fixedExtentList = SliverFixedExtentList(
            itemExtent = 50f,
            delegate = delegate
        )

        assertEquals(50f, fixedExtentList.itemExtent)
    }

    @Test
    fun `test negative item extent throws exception`() {
        val delegate = SliverChildDelegate.Builder(
            builder = "builder_ref",
            childCount = 100
        )

        assertFailsWith<IllegalArgumentException> {
            SliverFixedExtentList(
                itemExtent = -10f,
                delegate = delegate
            )
        }
    }

    @Test
    fun `test zero item extent throws exception`() {
        val delegate = SliverChildDelegate.Builder(
            builder = "builder_ref",
            childCount = 100
        )

        assertFailsWith<IllegalArgumentException> {
            SliverFixedExtentList(
                itemExtent = 0f,
                delegate = delegate
            )
        }
    }

    // SliverAppBar Tests

    @Test
    fun `test sliver app bar with default values`() {
        val appBar = SliverAppBar(
            title = "My Title"
        )

        assertEquals("My Title", appBar.title)
        assertFalse(appBar.floating)
        assertFalse(appBar.pinned)
        assertFalse(appBar.snap)
        assertEquals(4f, appBar.elevation)
    }

    @Test
    fun `test sliver app bar pinned`() {
        val appBar = SliverAppBar(
            title = "My Title",
            pinned = true
        )

        assertTrue(appBar.pinned)
    }

    @Test
    fun `test sliver app bar floating`() {
        val appBar = SliverAppBar(
            title = "My Title",
            floating = true
        )

        assertTrue(appBar.floating)
    }

    @Test
    fun `test sliver app bar snap requires floating`() {
        assertFailsWith<IllegalArgumentException> {
            SliverAppBar(
                title = "My Title",
                snap = true,
                floating = false
            )
        }
    }

    @Test
    fun `test sliver app bar with expanded height`() {
        val appBar = SliverAppBar(
            title = "My Title",
            expandedHeight = 200f
        )

        assertEquals(200f, appBar.expandedHeight)
    }

    @Test
    fun `test negative expanded height throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SliverAppBar(
                title = "My Title",
                expandedHeight = -100f
            )
        }
    }

    @Test
    fun `test zero expanded height throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SliverAppBar(
                title = "My Title",
                expandedHeight = 0f
            )
        }
    }

    @Test
    fun `test negative elevation throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            SliverAppBar(
                title = "My Title",
                elevation = -1f
            )
        }
    }

    @Test
    fun `test sliver app bar with actions`() {
        val actions = listOf("action1", "action2", "action3")

        val appBar = SliverAppBar(
            title = "My Title",
            actions = actions
        )

        assertEquals(3, appBar.actions?.size)
    }

    @Test
    fun `test sliver app bar stretch`() {
        val appBar = SliverAppBar(
            title = "My Title",
            stretch = true
        )

        assertTrue(appBar.stretch)
    }

    @Test
    fun `test flexible space bar`() {
        val flexibleSpace = FlexibleSpaceBar(
            title = "My Title",
            centerTitle = true,
            collapseMode = CollapseMode.Parallax
        )

        assertEquals("My Title", flexibleSpace.title)
        assertTrue(flexibleSpace.centerTitle)
        assertEquals(CollapseMode.Parallax, flexibleSpace.collapseMode)
    }

    @Test
    fun `test flexible space bar collapse modes`() {
        val parallax = FlexibleSpaceBar(
            title = "Title",
            collapseMode = CollapseMode.Parallax
        )

        val pin = FlexibleSpaceBar(
            title = "Title",
            collapseMode = CollapseMode.Pin
        )

        val none = FlexibleSpaceBar(
            title = "Title",
            collapseMode = CollapseMode.None
        )

        assertEquals(CollapseMode.Parallax, parallax.collapseMode)
        assertEquals(CollapseMode.Pin, pin.collapseMode)
        assertEquals(CollapseMode.None, none.collapseMode)
    }

    @Test
    fun `test flexible space bar stretch modes`() {
        val stretchModes = listOf(
            StretchMode.ZoomBackground,
            StretchMode.BlurBackground,
            StretchMode.FadeTitle
        )

        val flexibleSpace = FlexibleSpaceBar(
            title = "Title",
            stretchModes = stretchModes
        )

        assertEquals(3, flexibleSpace.stretchModes.size)
        assertTrue(flexibleSpace.stretchModes.contains(StretchMode.ZoomBackground))
    }

    @Test
    fun `test sliver child delegate builder with automatic keep alives`() {
        val delegate = SliverChildDelegate.Builder(
            builder = "builder_ref",
            childCount = 50,
            addAutomaticKeepAlives = true,
            addRepaintBoundaries = true,
            addSemanticIndexes = true
        )

        assertTrue(delegate.addAutomaticKeepAlives)
        assertTrue(delegate.addRepaintBoundaries)
        assertTrue(delegate.addSemanticIndexes)
    }

    @Test
    fun `test sliver child delegate builder without optimizations`() {
        val delegate = SliverChildDelegate.Builder(
            builder = "builder_ref",
            childCount = 50,
            addAutomaticKeepAlives = false,
            addRepaintBoundaries = false,
            addSemanticIndexes = false
        )

        assertFalse(delegate.addAutomaticKeepAlives)
        assertFalse(delegate.addRepaintBoundaries)
        assertFalse(delegate.addSemanticIndexes)
    }
}
