package com.augmentalis.magicui.ui.core.navigation

import com.augmentalis.magicui.components.core.*

/**
 * Tabs Component
 *
 * A tabbed navigation component for organizing content into separate views
 * where only one tab is visible at a time.
 *
 * Features:
 * - Text labels with optional icons
 * - Selection state tracking
 * - Associated content for each tab
 * - Scrollable for many tabs
 *
 * Platform mappings:
 * - Android: TabLayout + ViewPager
 * - iOS: UISegmentedControl / Custom tab bar
 * - macOS: NSTabView
 * - Web: Tab navigation with panels
 *
 * Usage:
 * ```kotlin
 * Tabs(
 *     tabs = listOf(
 *         Tab("Overview", icon = "dashboard"),
 *         Tab("Details", icon = "info"),
 *         Tab("Settings", icon = "settings")
 *     ),
 *     selectedIndex = 0,
 *     onTabSelected = { index -> /* switch tab */ }
 * )
 * ```
 */
data class TabsComponent(
    val tabs: List<Tab>,
    val selectedIndex: Int = 0,
    val id: String? = null,
    val style: Any? = null,
    val modifiers: List<Any> = emptyList(),
    val onTabSelected: ((Int) -> Unit)? = null
) {
    init {
        require(tabs.isNotEmpty()) { "Tabs must have at least one tab" }
        require(selectedIndex in tabs.indices) { "selectedIndex must be valid" }
    }

}

/**
 * Individual tab with label, optional icon, and content
 */
data class Tab(
    val label: String,
    val icon: String? = null,
    val content: Any? = null
)
