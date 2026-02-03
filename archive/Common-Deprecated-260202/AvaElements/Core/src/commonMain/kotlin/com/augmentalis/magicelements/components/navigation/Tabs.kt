package com.augmentalis.avaelements.components.navigation

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Color
import com.augmentalis.avaelements.core.types.Spacing
import com.augmentalis.avaelements.core.types.Border
import com.augmentalis.avaelements.core.types.Shadow

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
    override val type: String = "Tabs",
    val tabs: List<Tab>,
    val selectedIndex: Int = 0,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val onTabSelected: ((Int) -> Unit)? = null
) : Component {
    init {
        require(tabs.isNotEmpty()) { "Tabs must have at least one tab" }
        require(selectedIndex in tabs.indices) { "selectedIndex must be valid" }
    }

    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

/**
 * Individual tab with label, optional icon, and content
 */
data class Tab(
    val label: String,
    val icon: String? = null,
    val content: Component? = null
)
