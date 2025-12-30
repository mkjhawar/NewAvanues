package com.augmentalis.avaelements.flutter.material.navigation

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * VerticalTabs component - Flutter Material parity
 *
 * A vertical tab navigation component for switching between different views or sections.
 * Similar to TabBar but arranged vertically, typically used in settings panels or sidebars.
 *
 * **Flutter Equivalent:** `TabBar` with vertical orientation, `VerticalTabView`
 * **Material Design 3:** https://m3.material.io/components/tabs/overview
 *
 * ## Features
 * - Vertical tab arrangement
 * - Icon + label support
 * - Badge/notification indicators
 * - Selected state highlighting
 * - Scrollable mode for many tabs
 * - Tab groups/sections with dividers
 * - Keyboard navigation (Arrow keys)
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 * - Minimum 48dp touch target
 *
 * ## Usage Example
 * ```kotlin
 * VerticalTabs(
 *     tabs = listOf(
 *         VerticalTabs.Tab(
 *             id = "general",
 *             label = "General",
 *             icon = "settings",
 *             selected = true
 *         ),
 *         VerticalTabs.Tab(
 *             id = "notifications",
 *             label = "Notifications",
 *             icon = "notifications",
 *             badge = "3"
 *         ),
 *         VerticalTabs.Tab(
 *             id = "privacy",
 *             label = "Privacy",
 *             icon = "lock"
 *         ),
 *         VerticalTabs.Tab(
 *             id = "about",
 *             label = "About",
 *             icon = "info"
 *         )
 *     ),
 *     width = 200f,
 *     onTabSelected = { tabId -> println("Selected tab: $tabId") }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property tabs List of tab definitions
 * @property selectedTabId ID of the currently selected tab
 * @property width Width of the tab strip in dp (default 200)
 * @property scrollable Whether tabs can scroll if they exceed available height
 * @property dense Whether to use dense vertical spacing
 * @property showLabels Whether to show tab labels (false = icons only)
 * @property showIcons Whether to show tab icons
 * @property labelPosition Position of label relative to icon
 * @property indicatorWidth Width of the selection indicator in dp
 * @property indicatorColor Color of the selection indicator
 * @property selectedTabColor Color for selected tab
 * @property unselectedTabColor Color for unselected tabs
 * @property backgroundColor Background color of the tab strip
 * @property elevation Shadow elevation
 * @property dividerColor Color for section dividers
 * @property contentDescription Accessibility description for TalkBack
 * @property onTabSelected Callback invoked when a tab is selected (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class VerticalTabs(
    override val type: String = "VerticalTabs",
    override val id: String? = null,
    val tabs: List<Tab>,
    val selectedTabId: String? = null,
    val width: Float = 200f,
    val scrollable: Boolean = false,
    val dense: Boolean = false,
    val showLabels: Boolean = true,
    val showIcons: Boolean = true,
    val labelPosition: LabelPosition = LabelPosition.Right,
    val indicatorWidth: Float = 4f,
    val indicatorColor: String? = null,
    val selectedTabColor: String? = null,
    val unselectedTabColor: String? = null,
    val backgroundColor: String? = null,
    val elevation: Float? = null,
    val dividerColor: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onTabSelected: ((String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Tab data class
     *
     * @property id Unique identifier for the tab
     * @property label Display text for the tab
     * @property icon Optional icon identifier
     * @property selected Whether this tab is currently selected
     * @property enabled Whether the tab is enabled for interaction
     * @property badge Optional badge text (e.g., notification count)
     * @property group Optional group name for organizing tabs into sections
     * @property divider Whether to show a divider after this tab
     * @property tooltip Optional tooltip text on hover
     * @property onClick Callback invoked when tab is clicked
     */
    data class Tab(
        val id: String,
        val label: String,
        val icon: String? = null,
        val selected: Boolean = false,
        val enabled: Boolean = true,
        val badge: String? = null,
        val group: String? = null,
        val divider: Boolean = false,
        val tooltip: String? = null,
        @Transient
        val onClick: (() -> Unit)? = null
    ) {
        /**
         * Get accessibility description
         */
        fun getAccessibilityDescription(): String {
            val parts = mutableListOf(label)
            if (selected) parts.add("selected")
            if (badge != null) parts.add("badge: $badge")
            if (group != null) parts.add("in $group group")
            if (!enabled) parts.add("disabled")
            return parts.joinToString(", ")
        }

        /**
         * Check if tab has badge
         */
        fun hasBadge(): Boolean = badge != null && badge.isNotBlank()

        /**
         * Check if tab has icon
         */
        fun hasIcon(): Boolean = icon != null && icon.isNotBlank()
    }

    /**
     * Label position relative to icon
     */
    enum class LabelPosition {
        /** Label appears to the right of icon (horizontal layout) */
        Right,

        /** Label appears to the left of icon (horizontal layout) */
        Left,

        /** Label appears below icon (vertical layout) */
        Bottom,

        /** Label appears above icon (vertical layout) */
        Top
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Vertical tabs"
        val tabCount = tabs.size
        val selectedTab = getSelectedTab()
        val selectedDesc = if (selectedTab != null) {
            ", ${selectedTab.label} selected"
        } else {
            ""
        }
        return "$base with $tabCount tabs$selectedDesc"
    }

    /**
     * Get currently selected tab
     */
    fun getSelectedTab(): Tab? {
        return tabs.find { it.id == selectedTabId || it.selected }
    }

    /**
     * Get selected tab index
     */
    fun getSelectedTabIndex(): Int {
        return tabs.indexOfFirst { it.id == selectedTabId || it.selected }
    }

    /**
     * Get tabs by group
     */
    fun getTabsByGroup(groupName: String): List<Tab> {
        return tabs.filter { it.group == groupName }
    }

    /**
     * Get all unique groups
     */
    fun getGroups(): List<String> {
        return tabs.mapNotNull { it.group }.distinct()
    }

    /**
     * Check if tabs have groups
     */
    fun hasGroups(): Boolean {
        return tabs.any { it.group != null }
    }

    /**
     * Get total badge count
     */
    fun getTotalBadgeCount(): Int {
        return tabs.mapNotNull { it.badge?.toIntOrNull() }.sum()
    }

    /**
     * Check if tabs should scroll
     */
    fun shouldScroll(): Boolean {
        return scrollable && tabs.size > 10 // Arbitrary threshold
    }

    companion object {
        /**
         * Create standard vertical tabs
         */
        fun standard(
            tabs: List<Tab>,
            selectedTabId: String? = null,
            onTabSelected: ((String) -> Unit)? = null
        ) = VerticalTabs(
            tabs = tabs,
            selectedTabId = selectedTabId,
            showLabels = true,
            showIcons = true,
            onTabSelected = onTabSelected
        )

        /**
         * Create icon-only vertical tabs (compact mode)
         */
        fun iconOnly(
            tabs: List<Tab>,
            selectedTabId: String? = null,
            width: Float = 72f,
            onTabSelected: ((String) -> Unit)? = null
        ) = VerticalTabs(
            tabs = tabs,
            selectedTabId = selectedTabId,
            width = width,
            showLabels = false,
            showIcons = true,
            onTabSelected = onTabSelected
        )

        /**
         * Create scrollable vertical tabs for many items
         */
        fun scrollable(
            tabs: List<Tab>,
            selectedTabId: String? = null,
            onTabSelected: ((String) -> Unit)? = null
        ) = VerticalTabs(
            tabs = tabs,
            selectedTabId = selectedTabId,
            scrollable = true,
            onTabSelected = onTabSelected
        )

        /**
         * Create grouped vertical tabs with sections
         */
        fun grouped(
            tabs: List<Tab>,
            selectedTabId: String? = null,
            onTabSelected: ((String) -> Unit)? = null
        ) = VerticalTabs(
            tabs = tabs,
            selectedTabId = selectedTabId,
            showLabels = true,
            showIcons = true,
            onTabSelected = onTabSelected
        )

        /**
         * Create dense vertical tabs (compact spacing)
         */
        fun dense(
            tabs: List<Tab>,
            selectedTabId: String? = null,
            width: Float = 160f,
            onTabSelected: ((String) -> Unit)? = null
        ) = VerticalTabs(
            tabs = tabs,
            selectedTabId = selectedTabId,
            width = width,
            dense = true,
            onTabSelected = onTabSelected
        )
    }
}
