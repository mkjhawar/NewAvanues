package com.augmentalis.magicui.components.navigation

import kotlinx.serialization.Serializable

/**
 * MagicUI Navigation Components
 *
 * 4 navigation components for app navigation
 */

/**
 * AppBar component for top app bar
 */
@Serializable
data class AppBar(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val navigationIcon: String? = null,
    val actions: List<AppBarAction> = emptyList(),
    val variant: AppBarVariant = AppBarVariant.Standard,
    val scrollBehavior: ScrollBehavior = ScrollBehavior.None
)

/**
 * BottomNav component for bottom navigation
 */
@Serializable
data class BottomNav(
    val id: String,
    val items: List<BottomNavItem>,
    val selectedIndex: Int = 0
)

/**
 * Breadcrumb component for navigation trail
 */
@Serializable
data class Breadcrumb(
    val id: String,
    val items: List<BreadcrumbItem>,
    val separator: String = ">",
    val maxItems: Int? = null
)

/**
 * Pagination component for page navigation
 */
@Serializable
data class Pagination(
    val id: String,
    val currentPage: Int = 1,
    val totalPages: Int,
    val variant: PaginationVariant = PaginationVariant.Standard,
    val showFirstLast: Boolean = true,
    val siblingCount: Int = 1
)

// Supporting enums and data classes

@Serializable
data class AppBarAction(
    val id: String,
    val icon: String,
    val label: String? = null
)

@Serializable
enum class AppBarVariant {
    Standard,
    Large,
    Medium,
    Small
}

@Serializable
enum class ScrollBehavior {
    None,
    Collapse,
    Pin,
    Enter
}

@Serializable
data class BottomNavItem(
    val id: String,
    val label: String,
    val icon: String,
    val selectedIcon: String? = null,
    val enabled: Boolean = true,
    val badge: String? = null
)

@Serializable
data class BreadcrumbItem(
    val id: String,
    val label: String,
    val href: String? = null
)

@Serializable
enum class PaginationVariant {
    Standard,
    Simple,
    Compact
}
