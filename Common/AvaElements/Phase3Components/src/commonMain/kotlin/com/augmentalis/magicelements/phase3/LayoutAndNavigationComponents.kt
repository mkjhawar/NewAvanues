package com.augmentalis.avaelements.phase3

import com.augmentalis.avaelements.core.*

/**
 * Phase 3 Layout Components - Common Interface
 * 5 layout components for advanced layouts
 */

/**
 * Grid component for grid layout
 */
data class Grid(
    val id: String,
    val columns: Int = 2,
    val spacing: Float = 8f,
    val children: List<Component> = emptyList()
) : Component

/**
 * Stack component for layered layouts
 */
data class Stack(
    val id: String,
    val alignment: StackAlignment = StackAlignment.Center,
    val children: List<Component> = emptyList()
) : Component

/**
 * Spacer component for fixed spacing
 */
data class Spacer(
    val id: String,
    val width: Float? = null,
    val height: Float? = null
) : Component

/**
 * Drawer component for side panel
 */
data class Drawer(
    val id: String,
    val open: Boolean = false,
    val anchor: DrawerAnchor = DrawerAnchor.Start,
    val content: Component? = null,
    val mainContent: Component? = null,
    val onOpenChange: ((Boolean) -> Unit)? = null
) : Component

/**
 * Tabs component for tab navigation
 */
data class Tabs(
    val id: String,
    val tabs: List<Tab>,
    val selectedIndex: Int = 0,
    val variant: TabVariant = TabVariant.Standard,
    val onTabSelected: ((Int) -> Unit)? = null
) : Component

/**
 * Phase 3 Navigation Components - Common Interface
 * 4 navigation components
 */

/**
 * AppBar component for top app bar
 */
data class AppBar(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val navigationIcon: String? = null,
    val actions: List<AppBarAction> = emptyList(),
    val variant: AppBarVariant = AppBarVariant.Standard,
    val scrollBehavior: ScrollBehavior = ScrollBehavior.None,
    val onNavigationClick: (() -> Unit)? = null
) : Component

/**
 * BottomNav component for bottom navigation
 */
data class BottomNav(
    val id: String,
    val items: List<BottomNavItem>,
    val selectedIndex: Int = 0,
    val onItemSelected: ((Int) -> Unit)? = null
) : Component

/**
 * Breadcrumb component for navigation trail
 */
data class Breadcrumb(
    val id: String,
    val items: List<BreadcrumbItem>,
    val separator: String = ">",
    val maxItems: Int? = null,
    val onItemClick: ((Int) -> Unit)? = null
) : Component

/**
 * Pagination component for page navigation
 */
data class Pagination(
    val id: String,
    val currentPage: Int = 1,
    val totalPages: Int,
    val variant: PaginationVariant = PaginationVariant.Standard,
    val showFirstLast: Boolean = true,
    val siblingCount: Int = 1,
    val onPageChange: ((Int) -> Unit)? = null
) : Component

// Supporting enums and data classes

/**
 * Stack alignment
 */
enum class StackAlignment {
    TopStart,
    TopCenter,
    TopEnd,
    CenterStart,
    Center,
    CenterEnd,
    BottomStart,
    BottomCenter,
    BottomEnd
}

/**
 * Drawer anchor position
 */
enum class DrawerAnchor {
    Start,
    End,
    Top,
    Bottom
}

/**
 * Tab data
 */
data class Tab(
    val id: String,
    val label: String,
    val icon: String? = null,
    val enabled: Boolean = true,
    val badge: String? = null
)

/**
 * Tab variant
 */
enum class TabVariant {
    Standard,   // Full width tabs
    Scrollable, // Scrollable tabs
    Fixed       // Fixed width tabs
}

/**
 * App bar action
 */
data class AppBarAction(
    val id: String,
    val icon: String,
    val label: String? = null,
    val onClick: (() -> Unit)? = null
)

/**
 * App bar variant
 */
enum class AppBarVariant {
    Standard,   // Regular app bar
    Large,      // Large title
    Medium,     // Medium title
    Small       // Small/compact
}

/**
 * Scroll behavior for app bar
 */
enum class ScrollBehavior {
    None,       // No scroll behavior
    Collapse,   // Collapse on scroll
    Pin,        // Pin header
    Enter       // Enter always
}

/**
 * Bottom navigation item
 */
data class BottomNavItem(
    val id: String,
    val label: String,
    val icon: String,
    val selectedIcon: String? = null,
    val enabled: Boolean = true,
    val badge: String? = null
)

/**
 * Breadcrumb item
 */
data class BreadcrumbItem(
    val id: String,
    val label: String,
    val href: String? = null
)

/**
 * Pagination variant
 */
enum class PaginationVariant {
    Standard,   // Standard with numbers
    Simple,     // Just prev/next
    Compact     // Compact with ellipsis
}
