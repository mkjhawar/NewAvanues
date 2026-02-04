package com.augmentalis.avaelements.phase3

import kotlin.test.*

/**
 * Unit tests for Phase 3 Layout Components
 *
 * Tests verify component construction, default values, and property validation.
 */
class LayoutComponentsTest {

    // ==================== Grid Tests ====================

    @Test
    fun `Grid component should have correct defaults`() {
        val grid = Grid(id = "test-grid")

        assertEquals("test-grid", grid.id)
        assertEquals(2, grid.columns)
        assertEquals(8f, grid.spacing)
        assertTrue(grid.children.isEmpty())
    }

    @Test
    fun `Grid component should accept custom values`() {
        val grid = Grid(
            id = "custom-grid",
            columns = 3,
            spacing = 16f,
            children = emptyList()
        )

        assertEquals("custom-grid", grid.id)
        assertEquals(3, grid.columns)
        assertEquals(16f, grid.spacing)
    }

    @Test
    fun `Grid columns should be positive`() {
        val grid = Grid(id = "test", columns = 4)
        assertTrue(grid.columns > 0)
    }

    // ==================== Stack Tests ====================

    @Test
    fun `Stack component should have correct defaults`() {
        val stack = Stack(id = "test-stack")

        assertEquals("test-stack", stack.id)
        assertEquals(StackAlignment.Center, stack.alignment)
        assertTrue(stack.children.isEmpty())
    }

    @Test
    fun `Stack component should accept all alignment values`() {
        val alignments = listOf(
            StackAlignment.TopStart,
            StackAlignment.TopCenter,
            StackAlignment.TopEnd,
            StackAlignment.CenterStart,
            StackAlignment.Center,
            StackAlignment.CenterEnd,
            StackAlignment.BottomStart,
            StackAlignment.BottomCenter,
            StackAlignment.BottomEnd
        )

        alignments.forEach { alignment ->
            val stack = Stack(id = "test", alignment = alignment)
            assertEquals(alignment, stack.alignment)
        }
    }

    // ==================== Spacer Tests ====================

    @Test
    fun `Spacer component should have correct defaults`() {
        val spacer = Spacer(id = "test-spacer")

        assertEquals("test-spacer", spacer.id)
        assertNull(spacer.width)
        assertNull(spacer.height)
    }

    @Test
    fun `Spacer component should accept width only`() {
        val spacer = Spacer(id = "test", width = 16f)

        assertEquals(16f, spacer.width)
        assertNull(spacer.height)
    }

    @Test
    fun `Spacer component should accept height only`() {
        val spacer = Spacer(id = "test", height = 24f)

        assertNull(spacer.width)
        assertEquals(24f, spacer.height)
    }

    @Test
    fun `Spacer component should accept both dimensions`() {
        val spacer = Spacer(id = "test", width = 16f, height = 24f)

        assertEquals(16f, spacer.width)
        assertEquals(24f, spacer.height)
    }

    // ==================== Drawer Tests ====================

    @Test
    fun `Drawer component should have correct defaults`() {
        val drawer = Drawer(id = "test-drawer")

        assertEquals("test-drawer", drawer.id)
        assertFalse(drawer.open)
        assertEquals(DrawerAnchor.Start, drawer.anchor)
        assertNull(drawer.content)
        assertNull(drawer.mainContent)
        assertNull(drawer.onOpenChange)
    }

    @Test
    fun `Drawer component should accept all anchor positions`() {
        val anchors = listOf(
            DrawerAnchor.Start,
            DrawerAnchor.End,
            DrawerAnchor.Top,
            DrawerAnchor.Bottom
        )

        anchors.forEach { anchor ->
            val drawer = Drawer(id = "test", anchor = anchor)
            assertEquals(anchor, drawer.anchor)
        }
    }

    @Test
    fun `Drawer component should track open state`() {
        val drawerClosed = Drawer(id = "test", open = false)
        val drawerOpen = Drawer(id = "test", open = true)

        assertFalse(drawerClosed.open)
        assertTrue(drawerOpen.open)
    }

    // ==================== Tabs Tests ====================

    @Test
    fun `Tabs component should have correct defaults`() {
        val tabs = Tabs(
            id = "test-tabs",
            tabs = emptyList()
        )

        assertEquals("test-tabs", tabs.id)
        assertTrue(tabs.tabs.isEmpty())
        assertEquals(0, tabs.selectedIndex)
        assertEquals(TabVariant.Standard, tabs.variant)
        assertNull(tabs.onTabSelected)
    }

    @Test
    fun `Tabs component should accept tab items`() {
        val tabItems = listOf(
            Tab(id = "tab1", label = "Home"),
            Tab(id = "tab2", label = "Profile"),
            Tab(id = "tab3", label = "Settings")
        )

        val tabs = Tabs(id = "test", tabs = tabItems, selectedIndex = 1)

        assertEquals(3, tabs.tabs.size)
        assertEquals(1, tabs.selectedIndex)
        assertEquals("Home", tabs.tabs[0].label)
        assertEquals("Profile", tabs.tabs[1].label)
        assertEquals("Settings", tabs.tabs[2].label)
    }

    @Test
    fun `Tabs component should accept all variants`() {
        val variants = listOf(
            TabVariant.Standard,
            TabVariant.Scrollable,
            TabVariant.Fixed
        )

        variants.forEach { variant ->
            val tabs = Tabs(id = "test", tabs = emptyList(), variant = variant)
            assertEquals(variant, tabs.variant)
        }
    }

    @Test
    fun `Tab should have correct defaults`() {
        val tab = Tab(id = "test-tab", label = "Test")

        assertEquals("test-tab", tab.id)
        assertEquals("Test", tab.label)
        assertNull(tab.icon)
        assertTrue(tab.enabled)
        assertNull(tab.badge)
    }

    @Test
    fun `Tab should support badges and icons`() {
        val tab = Tab(
            id = "test",
            label = "Notifications",
            icon = "bell",
            badge = "5"
        )

        assertEquals("bell", tab.icon)
        assertEquals("5", tab.badge)
    }

    @Test
    fun `Tab should support disabled state`() {
        val tab = Tab(id = "test", label = "Disabled", enabled = false)
        assertFalse(tab.enabled)
    }
}
