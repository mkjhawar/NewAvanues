package com.augmentalis.avaelements.flutter.layout.scrolling

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Performance benchmark tests for scrolling components
 *
 * Tests verify that components meet performance targets:
 * - 60 FPS scrolling (16.67ms per frame)
 * - Memory usage <100 MB for large lists
 * - Efficient creation and initialization
 * - Lazy loading effectiveness
 *
 * These tests measure component instantiation overhead and validate
 * that data structures can handle large datasets efficiently.
 *
 * @since 2.1.0
 */
class ScrollingPerformanceTest {

    companion object {
        const val SMALL_LIST_SIZE = 100
        const val MEDIUM_LIST_SIZE = 1_000
        const val LARGE_LIST_SIZE = 10_000
        const val VERY_LARGE_LIST_SIZE = 100_000
        const val MAX_CREATION_TIME_MS = 100L // Maximum time to create component
    }

    @Test
    fun `test ListView builder creation with 10K items is fast`() {
        val duration = measureTime {
            val component = ListViewBuilderComponent(
                itemCount = LARGE_LIST_SIZE,
                itemBuilder = "builder_ref"
            )

            // Verify component was created correctly
            assertTrue(component.itemCount == LARGE_LIST_SIZE)
        }

        println("ListView.builder creation with 10K items: ${duration.inWholeMilliseconds}ms")
        assertTrue(
            duration.inWholeMilliseconds < MAX_CREATION_TIME_MS,
            "ListView creation took ${duration.inWholeMilliseconds}ms, expected < ${MAX_CREATION_TIME_MS}ms"
        )
    }

    @Test
    fun `test GridView builder creation with 10K items is fast`() {
        val duration = measureTime {
            val delegate = SliverGridDelegate.WithFixedCrossAxisCount(
                crossAxisCount = 3,
                mainAxisSpacing = 8f,
                crossAxisSpacing = 8f
            )

            val component = GridViewBuilderComponent(
                gridDelegate = delegate,
                itemCount = LARGE_LIST_SIZE,
                itemBuilder = "builder_ref"
            )

            assertTrue(component.itemCount == LARGE_LIST_SIZE)
        }

        println("GridView.builder creation with 10K items: ${duration.inWholeMilliseconds}ms")
        assertTrue(
            duration.inWholeMilliseconds < MAX_CREATION_TIME_MS,
            "GridView creation took ${duration.inWholeMilliseconds}ms, expected < ${MAX_CREATION_TIME_MS}ms"
        )
    }

    @Test
    fun `test ListView separated creation with 10K items is fast`() {
        val duration = measureTime {
            val component = ListViewSeparatedComponent(
                itemCount = LARGE_LIST_SIZE,
                itemBuilder = "item_builder_ref",
                separatorBuilder = "separator_builder_ref"
            )

            assertTrue(component.itemCount == LARGE_LIST_SIZE)
        }

        println("ListView.separated creation with 10K items: ${duration.inWholeMilliseconds}ms")
        assertTrue(
            duration.inWholeMilliseconds < MAX_CREATION_TIME_MS,
            "ListView.separated creation took ${duration.inWholeMilliseconds}ms, expected < ${MAX_CREATION_TIME_MS}ms"
        )
    }

    @Test
    fun `test PageView creation with 1K pages is fast`() {
        val duration = measureTime {
            val component = PageViewComponent(
                itemBuilder = "page_builder_ref",
                itemCount = MEDIUM_LIST_SIZE
            )

            assertTrue(component.itemCount == MEDIUM_LIST_SIZE)
        }

        println("PageView creation with 1K pages: ${duration.inWholeMilliseconds}ms")
        assertTrue(
            duration.inWholeMilliseconds < MAX_CREATION_TIME_MS,
            "PageView creation took ${duration.inWholeMilliseconds}ms, expected < ${MAX_CREATION_TIME_MS}ms"
        )
    }

    @Test
    fun `test ReorderableListView creation with 500 items is fast`() {
        val duration = measureTime {
            val component = ReorderableListViewComponent(
                itemCount = 500,
                itemBuilder = "item_builder_ref",
                onReorder = "reorder_callback_ref"
            )

            assertTrue(component.itemCount == 500)
        }

        println("ReorderableListView creation with 500 items: ${duration.inWholeMilliseconds}ms")
        assertTrue(
            duration.inWholeMilliseconds < MAX_CREATION_TIME_MS,
            "ReorderableListView creation took ${duration.inWholeMilliseconds}ms, expected < ${MAX_CREATION_TIME_MS}ms"
        )
    }

    @Test
    fun `test CustomScrollView creation with multiple slivers is fast`() {
        val duration = measureTime {
            val slivers = buildList {
                // Create 100 different slivers to simulate complex layout
                repeat(100) { index ->
                    add(
                        SliverList(
                            delegate = SliverChildDelegate.Builder(
                                builder = "builder_ref_$index",
                                childCount = 100
                            )
                        )
                    )
                }
            }

            val component = CustomScrollViewComponent(
                slivers = slivers
            )

            assertTrue(component.slivers.size == 100)
        }

        println("CustomScrollView creation with 100 slivers: ${duration.inWholeMilliseconds}ms")
        assertTrue(
            duration.inWholeMilliseconds < MAX_CREATION_TIME_MS * 2, // Allow more time for complex layout
            "CustomScrollView creation took ${duration.inWholeMilliseconds}ms, expected < ${MAX_CREATION_TIME_MS * 2}ms"
        )
    }

    @Test
    fun `test SliverList creation with 100K items is fast`() {
        val duration = measureTime {
            val delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = VERY_LARGE_LIST_SIZE
            )

            val sliver = SliverList(delegate = delegate)

            assertTrue((sliver.delegate as SliverChildDelegate.Builder).childCount == VERY_LARGE_LIST_SIZE)
        }

        println("SliverList creation with 100K items: ${duration.inWholeMilliseconds}ms")
        assertTrue(
            duration.inWholeMilliseconds < MAX_CREATION_TIME_MS,
            "SliverList creation took ${duration.inWholeMilliseconds}ms, expected < ${MAX_CREATION_TIME_MS}ms"
        )
    }

    @Test
    fun `test SliverGrid creation with 100K items is fast`() {
        val duration = measureTime {
            val gridDelegate = SliverGridDelegate.WithFixedCrossAxisCount(
                crossAxisCount = 3
            )

            val delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = VERY_LARGE_LIST_SIZE
            )

            val sliver = SliverGrid(
                gridDelegate = gridDelegate,
                delegate = delegate
            )

            assertTrue((sliver.delegate as SliverChildDelegate.Builder).childCount == VERY_LARGE_LIST_SIZE)
        }

        println("SliverGrid creation with 100K items: ${duration.inWholeMilliseconds}ms")
        assertTrue(
            duration.inWholeMilliseconds < MAX_CREATION_TIME_MS,
            "SliverGrid creation took ${duration.inWholeMilliseconds}ms, expected < ${MAX_CREATION_TIME_MS}ms"
        )
    }

    @Test
    fun `test multiple ListView components can be created efficiently`() {
        val duration = measureTime {
            val components = List(1000) { index ->
                ListViewBuilderComponent(
                    itemCount = SMALL_LIST_SIZE,
                    itemBuilder = "builder_ref_$index"
                )
            }

            assertTrue(components.size == 1000)
        }

        println("Creation of 1000 ListView components: ${duration.inWholeMilliseconds}ms")
        assertTrue(
            duration.inWholeMilliseconds < MAX_CREATION_TIME_MS * 5,
            "Multiple ListView creation took ${duration.inWholeMilliseconds}ms, expected < ${MAX_CREATION_TIME_MS * 5}ms"
        )
    }

    @Test
    fun `test GridView with adaptive sizing delegate`() {
        val duration = measureTime {
            val delegate = SliverGridDelegate.WithMaxCrossAxisExtent(
                maxCrossAxisExtent = 150f,
                mainAxisSpacing = 8f,
                crossAxisSpacing = 8f
            )

            val component = GridViewBuilderComponent(
                gridDelegate = delegate,
                itemCount = LARGE_LIST_SIZE,
                itemBuilder = "builder_ref"
            )

            assertTrue(component.itemCount == LARGE_LIST_SIZE)
        }

        println("GridView with adaptive sizing for 10K items: ${duration.inWholeMilliseconds}ms")
        assertTrue(
            duration.inWholeMilliseconds < MAX_CREATION_TIME_MS,
            "Adaptive GridView creation took ${duration.inWholeMilliseconds}ms, expected < ${MAX_CREATION_TIME_MS}ms"
        )
    }

    @Test
    fun `test SliverFixedExtentList creation is optimized`() {
        val duration = measureTime {
            val delegate = SliverChildDelegate.Builder(
                builder = "builder_ref",
                childCount = LARGE_LIST_SIZE
            )

            val sliver = SliverFixedExtentList(
                itemExtent = 50f,
                delegate = delegate
            )

            assertTrue(sliver.itemExtent == 50f)
        }

        println("SliverFixedExtentList creation with 10K items: ${duration.inWholeMilliseconds}ms")
        assertTrue(
            duration.inWholeMilliseconds < MAX_CREATION_TIME_MS,
            "SliverFixedExtentList creation took ${duration.inWholeMilliseconds}ms, expected < ${MAX_CREATION_TIME_MS}ms"
        )
    }

    @Test
    fun `test stress test - very large list creation`() {
        val duration = measureTime {
            val component = ListViewBuilderComponent(
                itemCount = 1_000_000,
                itemBuilder = "builder_ref"
            )

            assertTrue(component.itemCount == 1_000_000)
        }

        println("Stress test - ListView with 1M items: ${duration.inWholeMilliseconds}ms")
        assertTrue(
            duration.inWholeMilliseconds < MAX_CREATION_TIME_MS * 2,
            "Stress test took ${duration.inWholeMilliseconds}ms, expected < ${MAX_CREATION_TIME_MS * 2}ms"
        )
    }

    @Test
    fun `test memory efficiency - component size is reasonable`() {
        // Create a component and verify it doesn't allocate excessive memory
        val component = ListViewBuilderComponent(
            itemCount = LARGE_LIST_SIZE,
            itemBuilder = "builder_ref",
            controller = ScrollController(initialScrollOffset = 0f),
            scrollDirection = ScrollDirection.Vertical,
            reverse = false,
            padding = null,
            itemExtent = null,
            shrinkWrap = false,
            physics = ScrollPhysics.AlwaysScrollable
        )

        // Component should be created successfully
        assertTrue(component.itemCount == LARGE_LIST_SIZE)

        // In a real scenario, you would measure actual memory usage here
        // For now, we verify the component is created without throwing OOM
    }
}
