package com.augmentalis.avaelements.flutter.material.navigation

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * SubMenu component - Flutter Material parity
 *
 * A cascading submenu that appears when hovering or clicking on a parent menu item.
 * Supports multiple levels of nesting for hierarchical navigation.
 *
 * **Flutter Equivalent:** `SubmenuButton` widget
 * **Material Design 3:** https://m3.material.io/components/menus/overview
 *
 * ## Features
 * - Multi-level menu nesting (unlimited depth)
 * - Hover and click triggers
 * - Automatic positioning (right, left, top, bottom)
 * - Collision detection (prevents overflow off screen)
 * - Keyboard navigation (Arrow keys to navigate, Enter to select)
 * - Close on outside click
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * SubMenu(
 *     label = "Export",
 *     icon = "save_alt",
 *     items = listOf(
 *         SubMenu.SubMenuItem(
 *             id = "pdf",
 *             label = "Export as PDF",
 *             icon = "picture_as_pdf"
 *         ),
 *         SubMenu.SubMenuItem(
 *             id = "csv",
 *             label = "Export as CSV",
 *             icon = "table_chart"
 *         ),
 *         SubMenu.SubMenuItem(
 *             id = "advanced",
 *             label = "Advanced",
 *             icon = "settings",
 *             children = listOf(
 *                 SubMenu.SubMenuItem(id = "custom", label = "Custom Format"),
 *                 SubMenu.SubMenuItem(id = "batch", label = "Batch Export")
 *             )
 *         )
 *     ),
 *     onItemClick = { itemId -> println("Clicked: $itemId") }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property label Display text for the submenu trigger
 * @property icon Optional icon identifier
 * @property items List of submenu items (can include nested submenus)
 * @property open Whether the submenu is currently open
 * @property enabled Whether the submenu trigger is enabled
 * @property trigger How the submenu is triggered (Hover, Click, or Both)
 * @property placement Preferred placement direction (Right, Left, Top, Bottom, Auto)
 * @property offset Offset from the trigger element in pixels
 * @property closeOnItemClick Whether to close submenu when an item is clicked
 * @property backgroundColor Background color of the submenu
 * @property elevation Shadow elevation
 * @property shape Custom shape for the submenu
 * @property contentPadding Custom padding for the submenu
 * @property contentDescription Accessibility description for TalkBack
 * @property onItemClick Callback invoked when an item is clicked (not serialized)
 * @property onOpenChange Callback invoked when submenu opens/closes (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class SubMenu(
    override val type: String = "SubMenu",
    override val id: String? = null,
    val label: String,
    val icon: String? = null,
    val items: List<SubMenuItem>,
    val open: Boolean = false,
    val enabled: Boolean = true,
    val trigger: TriggerMode = TriggerMode.Both,
    val placement: Placement = Placement.Auto,
    val offset: Float = 8f,
    val closeOnItemClick: Boolean = true,
    val backgroundColor: String? = null,
    val elevation: Float? = null,
    val shape: String? = null,
    val contentPadding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val contentDescription: String? = null,
    @Transient
    val onItemClick: ((String) -> Unit)? = null,
    @Transient
    val onOpenChange: ((Boolean) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * SubMenu item data class
     *
     * @property id Unique identifier for the item
     * @property label Display text for the item
     * @property icon Optional icon identifier
     * @property enabled Whether the item is enabled for interaction
     * @property divider Whether to show a divider after this item
     * @property children Optional nested submenu items (for cascading menus)
     * @property badge Optional badge text (e.g., "New", "Beta")
     * @property shortcut Optional keyboard shortcut text (e.g., "Ctrl+S")
     * @property destructive Whether this is a destructive action (shows in red)
     * @property onClick Callback invoked when item is clicked
     */
    data class SubMenuItem(
        val id: String,
        val label: String,
        val icon: String? = null,
        val enabled: Boolean = true,
        val divider: Boolean = false,
        val children: List<SubMenuItem>? = null,
        val badge: String? = null,
        val shortcut: String? = null,
        val destructive: Boolean = false,
        @Transient
        val onClick: (() -> Unit)? = null
    ) {
        /**
         * Check if this item has a nested submenu
         */
        fun hasSubmenu(): Boolean = children != null && children.isNotEmpty()

        /**
         * Get accessibility description
         */
        fun getAccessibilityDescription(): String {
            val parts = mutableListOf(label)
            if (hasSubmenu()) parts.add("has submenu")
            if (shortcut != null) parts.add("shortcut: $shortcut")
            if (badge != null) parts.add("badge: $badge")
            if (destructive) parts.add("destructive action")
            if (!enabled) parts.add("disabled")
            return parts.joinToString(", ")
        }

        /**
         * Get nesting level (0 = top level)
         */
        fun getNestingLevel(): Int {
            var level = 0
            var currentItems = children
            while (currentItems != null && currentItems.isNotEmpty()) {
                level++
                currentItems = currentItems.firstOrNull()?.children
            }
            return level
        }
    }

    /**
     * Trigger mode for opening submenu
     */
    enum class TriggerMode {
        /** Open on hover only */
        Hover,

        /** Open on click only */
        Click,

        /** Open on both hover and click */
        Both
    }

    /**
     * Placement direction for submenu
     */
    enum class Placement {
        /** Automatically determine best placement */
        Auto,

        /** Place to the right of trigger */
        Right,

        /** Place to the left of trigger */
        Left,

        /** Place above trigger */
        Top,

        /** Place below trigger */
        Bottom,

        /** Place to the right-top of trigger */
        RightTop,

        /** Place to the right-bottom of trigger */
        RightBottom,

        /** Place to the left-top of trigger */
        LeftTop,

        /** Place to the left-bottom of trigger */
        LeftBottom
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: label
        val state = if (open) "expanded" else "collapsed"
        val itemCount = items.size
        val hasNested = items.any { it.hasSubmenu() }
        val nestedDesc = if (hasNested) ", contains nested menus" else ""
        return "$base, $state, $itemCount items$nestedDesc"
    }

    /**
     * Get total item count including nested items
     */
    fun getTotalItemCount(): Int {
        var count = items.size
        items.forEach { item ->
            if (item.hasSubmenu()) {
                count += item.children!!.size
            }
        }
        return count
    }

    /**
     * Get maximum nesting depth
     */
    fun getMaxNestingDepth(): Int {
        return items.maxOfOrNull { it.getNestingLevel() } ?: 0
    }

    /**
     * Check if submenu contains dividers
     */
    fun hasDividers(): Boolean {
        return items.any { it.divider }
    }

    companion object {
        /**
         * Create a hover-triggered submenu
         */
        fun hover(
            label: String,
            items: List<SubMenuItem>,
            icon: String? = null,
            onItemClick: ((String) -> Unit)? = null
        ) = SubMenu(
            label = label,
            icon = icon,
            items = items,
            trigger = TriggerMode.Hover,
            onItemClick = onItemClick
        )

        /**
         * Create a click-triggered submenu
         */
        fun click(
            label: String,
            items: List<SubMenuItem>,
            icon: String? = null,
            onItemClick: ((String) -> Unit)? = null
        ) = SubMenu(
            label = label,
            icon = icon,
            items = items,
            trigger = TriggerMode.Click,
            onItemClick = onItemClick
        )

        /**
         * Create a context submenu (right-click menu)
         */
        fun contextMenu(
            items: List<SubMenuItem>,
            onItemClick: ((String) -> Unit)? = null
        ) = SubMenu(
            label = "Options",
            items = items,
            trigger = TriggerMode.Click,
            placement = Placement.Auto,
            onItemClick = onItemClick
        )

        /**
         * Create a cascading submenu with multiple levels
         */
        fun cascading(
            label: String,
            items: List<SubMenuItem>,
            icon: String? = null,
            placement: Placement = Placement.Right,
            onItemClick: ((String) -> Unit)? = null
        ) = SubMenu(
            label = label,
            icon = icon,
            items = items,
            trigger = TriggerMode.Both,
            placement = placement,
            onItemClick = onItemClick
        )
    }
}
