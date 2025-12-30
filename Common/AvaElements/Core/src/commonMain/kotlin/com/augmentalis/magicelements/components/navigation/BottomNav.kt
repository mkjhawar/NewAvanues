package com.augmentalis.avaelements.components.navigation

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

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
    override val type: String = "BottomNav",
    val items: List<BottomNavItem>,
    val selectedIndex: Int = 0,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onItemSelected: ((Int) -> Unit)? = null
) : Component {
    init {
        require(items.size in 2..5) { "BottomNav should have 2-5 items for optimal UX" }
        require(selectedIndex in items.indices) { "selectedIndex must be valid" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Navigation item in bottom navigation bar
 */
data class BottomNavItem(
    val icon: String,
    val label: String,
    val badge: String? = null
)
