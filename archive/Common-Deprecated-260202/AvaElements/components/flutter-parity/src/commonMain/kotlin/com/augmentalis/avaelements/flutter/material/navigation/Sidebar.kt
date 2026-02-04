package com.augmentalis.avaelements.flutter.material.navigation

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Sidebar component - Flutter Material parity
 *
 * A collapsible side navigation panel for persistent or overlay navigation.
 * Similar to Drawer but supports persistent mode and collapsing behavior.
 *
 * **Flutter Equivalent:** `NavigationRail`, `Drawer` with persistent mode
 * **Material Design 3:** https://m3.material.io/components/navigation-rail/overview
 *
 * ## Features
 * - Collapsible/expandable behavior
 * - Persistent mode (always visible) or Overlay mode (mobile)
 * - Header section support
 * - Footer section support
 * - Width control (fixed or percentage)
 * - Smooth transition animations
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * Sidebar(
 *     visible = true,
 *     width = 280f,
 *     headerContent = "My App",
 *     items = listOf(
 *         Sidebar.SidebarItem(
 *             id = "home",
 *             label = "Home",
 *             icon = "home",
 *             selected = true
 *         ),
 *         Sidebar.SidebarItem(
 *             id = "settings",
 *             label = "Settings",
 *             icon = "settings"
 *         )
 *     ),
 *     collapsible = true,
 *     collapsed = false,
 *     mode = Sidebar.Mode.Persistent,
 *     onCollapseToggle = { collapsed -> println("Collapsed: $collapsed") },
 *     onItemClick = { itemId -> println("Clicked: $itemId") }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether the sidebar is visible
 * @property width Width of the sidebar in dp (default 280)
 * @property collapsedWidth Width when collapsed in dp (default 72)
 * @property headerContent Optional header content description
 * @property headerHeight Optional header height in dp
 * @property footerContent Optional footer content description
 * @property items List of sidebar navigation items
 * @property collapsible Whether the sidebar can be collapsed
 * @property collapsed Whether the sidebar is currently collapsed
 * @property mode Persistent (always visible) or Overlay (mobile modal)
 * @property backgroundColor Background color of the sidebar
 * @property elevation Shadow elevation
 * @property shape Custom shape for the sidebar
 * @property selectedItemColor Color for selected item
 * @property unselectedItemColor Color for unselected items
 * @property contentDescription Accessibility description for TalkBack
 * @property onCollapseToggle Callback invoked when collapse state changes (not serialized)
 * @property onItemClick Callback invoked when an item is clicked (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class Sidebar(
    override val type: String = "Sidebar",
    override val id: String? = null,
    val visible: Boolean = true,
    val width: Float = 280f,
    val collapsedWidth: Float = 72f,
    val headerContent: String? = null,
    val headerHeight: Float? = null,
    val footerContent: String? = null,
    val items: List<SidebarItem>,
    val collapsible: Boolean = true,
    val collapsed: Boolean = false,
    val mode: Mode = Mode.Persistent,
    val backgroundColor: String? = null,
    val elevation: Float? = null,
    val shape: String? = null,
    val selectedItemColor: String? = null,
    val unselectedItemColor: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onCollapseToggle: ((Boolean) -> Unit)? = null,
    @Transient
    val onItemClick: ((String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Sidebar item data class
     *
     * @property id Unique identifier for the item
     * @property label Display text for the item
     * @property icon Optional icon identifier
     * @property selected Whether this item is currently selected
     * @property enabled Whether the item is enabled for interaction
     * @property badge Optional badge text (e.g., notification count)
     * @property divider Whether to show a divider after this item
     * @property onClick Callback invoked when item is clicked
     */
    data class SidebarItem(
        val id: String,
        val label: String,
        val icon: String? = null,
        val selected: Boolean = false,
        val enabled: Boolean = true,
        val badge: String? = null,
        val divider: Boolean = false,
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
            if (!enabled) parts.add("disabled")
            return parts.joinToString(", ")
        }
    }

    /**
     * Sidebar display mode
     */
    enum class Mode {
        /** Always visible alongside content (desktop/tablet) */
        Persistent,

        /** Modal overlay above content (mobile) */
        Overlay
    }

    /**
     * Get effective width based on collapsed state
     */
    fun getEffectiveWidth(): Float {
        return if (collapsed) collapsedWidth else width
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Navigation sidebar"
        val state = if (collapsed) "collapsed" else "expanded"
        val modeDesc = when (mode) {
            Mode.Persistent -> "persistent"
            Mode.Overlay -> "overlay"
        }
        return "$base, $state, $modeDesc mode"
    }

    /**
     * Get selected item
     */
    fun getSelectedItem(): SidebarItem? {
        return items.firstOrNull { it.selected }
    }

    companion object {
        /**
         * Create a persistent sidebar (desktop/tablet)
         */
        fun persistent(
            items: List<SidebarItem>,
            collapsed: Boolean = false,
            onItemClick: ((String) -> Unit)? = null
        ) = Sidebar(
            items = items,
            collapsed = collapsed,
            mode = Mode.Persistent,
            onItemClick = onItemClick
        )

        /**
         * Create an overlay sidebar (mobile)
         */
        fun overlay(
            items: List<SidebarItem>,
            visible: Boolean = false,
            onItemClick: ((String) -> Unit)? = null
        ) = Sidebar(
            items = items,
            visible = visible,
            mode = Mode.Overlay,
            collapsible = false,
            onItemClick = onItemClick
        )

        /**
         * Create a navigation rail (collapsed sidebar with icons)
         */
        fun navigationRail(
            items: List<SidebarItem>,
            onItemClick: ((String) -> Unit)? = null
        ) = Sidebar(
            items = items,
            collapsed = true,
            collapsible = false,
            mode = Mode.Persistent,
            onItemClick = onItemClick
        )
    }
}
