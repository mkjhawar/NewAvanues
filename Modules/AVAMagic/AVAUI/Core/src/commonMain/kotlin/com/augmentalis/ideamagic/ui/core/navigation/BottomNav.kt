package com.augmentalis.avamagic.ui.core.navigation

import com.augmentalis.avamagic.components.core.*

/**
 * BottomNav Component
 *
 * A bottom navigation bar for primary app-level navigation between 3-5 destinations.
 *
 * Features:
 * - Icon-based navigation items
 * - Text labels
 * - Optional badges for notifications
 * - Selection state tracking
 *
 * Platform mappings:
 * - Android: BottomNavigationView / NavigationBar
 * - iOS: UITabBar
 * - Web: Fixed bottom navigation
 *
 * Usage:
 * ```kotlin
 * BottomNav(
 *     items = listOf(
 *         BottomNavItem("home", "Home"),
 *         BottomNavItem("search", "Search"),
 *         BottomNavItem("profile", "Profile", badge = "3")
 *     ),
 *     selectedIndex = 0,
 *     onItemSelected = { index -> /* navigate */ }
 * )
 * ```
 */
data class BottomNavComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val items: List<BottomNavItem>,
    val selectedIndex: Int = 0,
    val onItemSelected: ((Int) -> Unit)? = null
) : Component {
    init {
        require(items.size in 2..5) { "BottomNav should have 2-5 items for optimal UX" }
        require(selectedIndex in items.indices) { "selectedIndex must be valid" }
    }
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * Navigation item in bottom navigation bar
 */
data class BottomNavItem(
    val icon: String,
    val label: String,
    val badge: String? = null
)
