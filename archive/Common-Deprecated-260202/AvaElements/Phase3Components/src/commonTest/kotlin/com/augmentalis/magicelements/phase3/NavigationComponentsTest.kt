package com.augmentalis.avaelements.phase3

import kotlin.test.*

/**
 * Unit tests for Phase 3 Navigation Components
 *
 * Tests verify component construction, default values, and property validation.
 */
class NavigationComponentsTest {

    // ==================== AppBar Tests ====================

    @Test
    fun `AppBar component should have correct defaults`() {
        val appBar = AppBar(
            id = "test-appbar",
            title = "Test Title"
        )

        assertEquals("test-appbar", appBar.id)
        assertEquals("Test Title", appBar.title)
        assertNull(appBar.subtitle)
        assertNull(appBar.navigationIcon)
        assertTrue(appBar.actions.isEmpty())
        assertEquals(AppBarVariant.Standard, appBar.variant)
        assertEquals(ScrollBehavior.None, appBar.scrollBehavior)
        assertNull(appBar.onNavigationClick)
    }

    @Test
    fun `AppBar component should accept all variants`() {
        val variants = listOf(
            AppBarVariant.Standard,
            AppBarVariant.Large,
            AppBarVariant.Medium,
            AppBarVariant.Small
        )

        variants.forEach { variant ->
            val appBar = AppBar(id = "test", title = "Title", variant = variant)
            assertEquals(variant, appBar.variant)
        }
    }

    @Test
    fun `AppBar component should accept all scroll behaviors`() {
        val behaviors = listOf(
            ScrollBehavior.None,
            ScrollBehavior.Collapse,
            ScrollBehavior.Pin,
            ScrollBehavior.Enter
        )

        behaviors.forEach { behavior ->
            val appBar = AppBar(id = "test", title = "Title", scrollBehavior = behavior)
            assertEquals(behavior, appBar.scrollBehavior)
        }
    }

    @Test
    fun `AppBar component should support subtitle`() {
        val appBar = AppBar(
            id = "test",
            title = "Main Title",
            subtitle = "Subtitle"
        )

        assertEquals("Main Title", appBar.title)
        assertEquals("Subtitle", appBar.subtitle)
    }

    @Test
    fun `AppBar component should support actions`() {
        val actions = listOf(
            AppBarAction(id = "action1", icon = "search"),
            AppBarAction(id = "action2", icon = "settings")
        )

        val appBar = AppBar(
            id = "test",
            title = "Title",
            actions = actions
        )

        assertEquals(2, appBar.actions.size)
        assertEquals("search", appBar.actions[0].icon)
        assertEquals("settings", appBar.actions[1].icon)
    }

    @Test
    fun `AppBarAction should have correct properties`() {
        val action = AppBarAction(
            id = "test-action",
            icon = "search",
            label = "Search"
        )

        assertEquals("test-action", action.id)
        assertEquals("search", action.icon)
        assertEquals("Search", action.label)
        assertNull(action.onClick)
    }

    // ==================== BottomNav Tests ====================

    @Test
    fun `BottomNav component should have correct defaults`() {
        val bottomNav = BottomNav(
            id = "test-bottomnav",
            items = emptyList()
        )

        assertEquals("test-bottomnav", bottomNav.id)
        assertTrue(bottomNav.items.isEmpty())
        assertEquals(0, bottomNav.selectedIndex)
        assertNull(bottomNav.onItemSelected)
    }

    @Test
    fun `BottomNav component should accept navigation items`() {
        val items = listOf(
            BottomNavItem(id = "home", label = "Home", icon = "home"),
            BottomNavItem(id = "search", label = "Search", icon = "search"),
            BottomNavItem(id = "profile", label = "Profile", icon = "person")
        )

        val bottomNav = BottomNav(
            id = "test",
            items = items,
            selectedIndex = 1
        )

        assertEquals(3, bottomNav.items.size)
        assertEquals(1, bottomNav.selectedIndex)
        assertEquals("Home", bottomNav.items[0].label)
        assertEquals("Search", bottomNav.items[1].label)
    }

    @Test
    fun `BottomNavItem should have correct defaults`() {
        val item = BottomNavItem(
            id = "test-item",
            label = "Test",
            icon = "star"
        )

        assertEquals("test-item", item.id)
        assertEquals("Test", item.label)
        assertEquals("star", item.icon)
        assertNull(item.selectedIcon)
        assertTrue(item.enabled)
        assertNull(item.badge)
    }

    @Test
    fun `BottomNavItem should support badges`() {
        val item = BottomNavItem(
            id = "test",
            label = "Messages",
            icon = "message",
            badge = "3"
        )

        assertEquals("3", item.badge)
    }

    @Test
    fun `BottomNavItem should support selected icon`() {
        val item = BottomNavItem(
            id = "test",
            label = "Home",
            icon = "home-outline",
            selectedIcon = "home-filled"
        )

        assertEquals("home-outline", item.icon)
        assertEquals("home-filled", item.selectedIcon)
    }

    // ==================== Breadcrumb Tests ====================

    @Test
    fun `Breadcrumb component should have correct defaults`() {
        val breadcrumb = Breadcrumb(
            id = "test-breadcrumb",
            items = emptyList()
        )

        assertEquals("test-breadcrumb", breadcrumb.id)
        assertTrue(breadcrumb.items.isEmpty())
        assertEquals(">", breadcrumb.separator)
        assertNull(breadcrumb.maxItems)
        assertNull(breadcrumb.onItemClick)
    }

    @Test
    fun `Breadcrumb component should accept custom separator`() {
        val breadcrumb = Breadcrumb(
            id = "test",
            items = emptyList(),
            separator = "/"
        )

        assertEquals("/", breadcrumb.separator)
    }

    @Test
    fun `Breadcrumb component should support maxItems`() {
        val items = List(5) { index ->
            BreadcrumbItem(id = "item$index", label = "Level $index")
        }

        val breadcrumb = Breadcrumb(
            id = "test",
            items = items,
            maxItems = 3
        )

        assertEquals(5, breadcrumb.items.size)
        assertEquals(3, breadcrumb.maxItems)
    }

    @Test
    fun `BreadcrumbItem should have correct properties`() {
        val item = BreadcrumbItem(
            id = "test-item",
            label = "Home",
            href = "/home"
        )

        assertEquals("test-item", item.id)
        assertEquals("Home", item.label)
        assertEquals("/home", item.href)
    }

    // ==================== Pagination Tests ====================

    @Test
    fun `Pagination component should have correct defaults`() {
        val pagination = Pagination(
            id = "test-pagination",
            totalPages = 10
        )

        assertEquals("test-pagination", pagination.id)
        assertEquals(1, pagination.currentPage)
        assertEquals(10, pagination.totalPages)
        assertEquals(PaginationVariant.Standard, pagination.variant)
        assertTrue(pagination.showFirstLast)
        assertEquals(1, pagination.siblingCount)
        assertNull(pagination.onPageChange)
    }

    @Test
    fun `Pagination component should accept all variants`() {
        val variants = listOf(
            PaginationVariant.Standard,
            PaginationVariant.Simple,
            PaginationVariant.Compact
        )

        variants.forEach { variant ->
            val pagination = Pagination(
                id = "test",
                totalPages = 10,
                variant = variant
            )
            assertEquals(variant, pagination.variant)
        }
    }

    @Test
    fun `Pagination component should support current page`() {
        val pagination = Pagination(
            id = "test",
            currentPage = 5,
            totalPages = 10
        )

        assertEquals(5, pagination.currentPage)
        assertEquals(10, pagination.totalPages)
    }

    @Test
    fun `Pagination component should support sibling count`() {
        val pagination = Pagination(
            id = "test",
            totalPages = 100,
            siblingCount = 2
        )

        assertEquals(2, pagination.siblingCount)
    }

    @Test
    fun `Pagination component should support showFirstLast toggle`() {
        val withFirstLast = Pagination(
            id = "test1",
            totalPages = 10,
            showFirstLast = true
        )

        val withoutFirstLast = Pagination(
            id = "test2",
            totalPages = 10,
            showFirstLast = false
        )

        assertTrue(withFirstLast.showFirstLast)
        assertFalse(withoutFirstLast.showFirstLast)
    }

    @Test
    fun `Pagination should handle single page`() {
        val pagination = Pagination(
            id = "test",
            totalPages = 1
        )

        assertEquals(1, pagination.totalPages)
        assertEquals(1, pagination.currentPage)
    }

    @Test
    fun `Pagination should handle large page counts`() {
        val pagination = Pagination(
            id = "test",
            currentPage = 50,
            totalPages = 1000
        )

        assertEquals(50, pagination.currentPage)
        assertEquals(1000, pagination.totalPages)
    }
}
