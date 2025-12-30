package com.augmentalis.avaelements.flutter.layout.scrolling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for PageView component
 *
 * Tests cover:
 * - Component creation with different modes
 * - Page controller
 * - Viewport fraction
 * - Page snapping behavior
 *
 * @since 2.1.0
 */
class PageViewTest {

    @Test
    fun `test pageview with builder`() {
        val component = PageViewComponent(
            itemBuilder = "page_builder_ref",
            itemCount = 5
        )

        assertEquals("page_builder_ref", component.itemBuilder)
        assertEquals(5, component.itemCount)
        assertEquals(ScrollDirection.Horizontal, component.scrollDirection)
        assertTrue(component.pageSnapping)
    }

    @Test
    fun `test pageview with children`() {
        val children = listOf("page1", "page2", "page3")

        val component = PageViewComponent(
            children = children
        )

        assertEquals(3, component.children?.size)
        assertEquals(null, component.itemBuilder)
    }

    @Test
    fun `test pageview with infinite pages`() {
        val component = PageViewComponent(
            itemBuilder = "page_builder_ref",
            itemCount = -1 // Infinite pages
        )

        assertEquals(-1, component.itemCount)
    }

    @Test
    fun `test pageview cannot have both children and builder`() {
        assertFailsWith<IllegalArgumentException> {
            PageViewComponent(
                children = listOf("page1", "page2"),
                itemBuilder = "page_builder_ref",
                itemCount = 2
            )
        }
    }

    @Test
    fun `test pageview must have either children or builder`() {
        assertFailsWith<IllegalArgumentException> {
            PageViewComponent()
        }
    }

    @Test
    fun `test vertical pageview`() {
        val component = PageViewComponent(
            itemBuilder = "page_builder_ref",
            itemCount = 10,
            scrollDirection = ScrollDirection.Vertical
        )

        assertEquals(ScrollDirection.Vertical, component.scrollDirection)
    }

    @Test
    fun `test page controller with initial page`() {
        val controller = PageController(
            initialPage = 2,
            viewportFraction = 1.0f
        )

        val component = PageViewComponent(
            itemBuilder = "page_builder_ref",
            itemCount = 5,
            controller = controller
        )

        assertEquals(2, component.controller?.initialPage)
    }

    @Test
    fun `test page controller with viewport fraction`() {
        val controller = PageController(
            initialPage = 0,
            viewportFraction = 0.8f
        )

        val component = PageViewComponent(
            itemBuilder = "page_builder_ref",
            itemCount = 5,
            controller = controller
        )

        assertEquals(0.8f, component.controller?.viewportFraction)
    }

    @Test
    fun `test page snapping disabled`() {
        val component = PageViewComponent(
            itemBuilder = "page_builder_ref",
            itemCount = 5,
            pageSnapping = false
        )

        assertFalse(component.pageSnapping)
    }

    @Test
    fun `test reverse layout`() {
        val component = PageViewComponent(
            itemBuilder = "page_builder_ref",
            itemCount = 5,
            reverse = true
        )

        assertTrue(component.reverse)
    }

    @Test
    fun `test negative initial page throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            PageController(
                initialPage = -1
            )
        }
    }

    @Test
    fun `test viewport fraction less than or equal to zero throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            PageController(
                viewportFraction = 0f
            )
        }
    }

    @Test
    fun `test viewport fraction greater than one throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            PageController(
                viewportFraction = 1.5f
            )
        }
    }

    @Test
    fun `test negative item count (not infinite) throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            PageViewComponent(
                itemBuilder = "page_builder_ref",
                itemCount = -2 // Only -1 is allowed for infinite
            )
        }
    }

    @Test
    fun `test platform physics`() {
        val component = PageViewComponent(
            itemBuilder = "page_builder_ref",
            itemCount = 5,
            physics = ScrollPhysics.Platform
        )

        assertEquals(ScrollPhysics.Platform, component.physics)
    }

    @Test
    fun `test bouncing physics`() {
        val component = PageViewComponent(
            itemBuilder = "page_builder_ref",
            itemCount = 5,
            physics = ScrollPhysics.Bouncing
        )

        assertEquals(ScrollPhysics.Bouncing, component.physics)
    }
}
