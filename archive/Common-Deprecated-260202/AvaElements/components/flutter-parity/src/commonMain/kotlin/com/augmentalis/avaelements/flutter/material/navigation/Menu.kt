package com.augmentalis.avaelements.flutter.material.navigation

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Menu component - Flutter Material parity
 *
 * A vertical or horizontal menu component with items, sections, dividers, and submenus.
 * Follows Material Design 3 specifications for menus and navigation.
 *
 * **Flutter Equivalent:** `Menu`, `PopupMenuButton` with nested items
 * **Material Design 3:** https://m3.material.io/components/menus/overview
 *
 * ## Features
 * - Nested submenus support
 * - Single/multiple selection modes
 * - Section dividers
 * - Icons for menu items
 * - Keyboard navigation (Arrow keys, Enter)
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * Menu(
 *     items = listOf(
 *         Menu.MenuItem(
 *             id = "file",
 *             label = "File",
 *             icon = "description",
 *             children = listOf(
 *                 Menu.MenuItem(id = "new", label = "New"),
 *                 Menu.MenuItem(id = "open", label = "Open"),
 *                 Menu.MenuItem(id = "divider", label = "", divider = true),
 *                 Menu.MenuItem(id = "exit", label = "Exit")
 *             )
 *         ),
 *         Menu.MenuItem(id = "edit", label = "Edit", icon = "edit"),
 *         Menu.MenuItem(id = "view", label = "View", icon = "visibility")
 *     ),
 *     selectionMode = Menu.SelectionMode.Single,
 *     onSelectionChanged = { index -> println("Selected: $index") }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property items List of menu items (can include nested submenus)
 * @property orientation Menu layout orientation (Vertical or Horizontal)
 * @property selectionMode Selection behavior (None, Single, Multiple)
 * @property selectedIndices Currently selected item indices (for multiple selection)
 * @property selectedIndex Currently selected item index (for single selection)
 * @property dense Whether to use dense vertical layout
 * @property backgroundColor Background color of the menu
 * @property elevation Shadow elevation
 * @property shape Custom shape for the menu container
 * @property contentPadding Custom padding for the menu
 * @property contentDescription Accessibility description for TalkBack
 * @property onSelectionChanged Callback invoked when selection changes (not serialized)
 * @property onItemClick Callback invoked when an item is clicked (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class Menu(
    override val type: String = "Menu",
    override val id: String? = null,
    val items: List<MenuItem>,
    val orientation: Orientation = Orientation.Vertical,
    val selectionMode: SelectionMode = SelectionMode.Single,
    val selectedIndices: List<Int> = emptyList(),
    val selectedIndex: Int? = null,
    val dense: Boolean = false,
    val backgroundColor: String? = null,
    val elevation: Float? = null,
    val shape: String? = null,
    val contentPadding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val contentDescription: String? = null,
    @Transient
    val onSelectionChanged: ((Int) -> Unit)? = null,
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
     * Menu item data class
     *
     * @property id Unique identifier for the item
     * @property label Display text for the item
     * @property icon Optional icon identifier
     * @property enabled Whether the item is enabled for interaction
     * @property divider Whether to show a divider after this item
     * @property children Optional nested submenu items
     * @property badge Optional badge text (e.g., "New", "5")
     * @property leadingWidget Optional leading widget description
     * @property trailingWidget Optional trailing widget description
     * @property onClick Callback invoked when item is clicked
     */
    data class MenuItem(
        val id: String,
        val label: String,
        val icon: String? = null,
        val enabled: Boolean = true,
        val divider: Boolean = false,
        val children: List<MenuItem>? = null,
        val badge: String? = null,
        val leadingWidget: String? = null,
        val trailingWidget: String? = null,
        @Transient
        val onClick: (() -> Unit)? = null
    ) {
        /**
         * Check if this is a submenu item
         */
        fun hasSubmenu(): Boolean = children != null && children.isNotEmpty()

        /**
         * Get accessibility description
         */
        fun getAccessibilityDescription(): String {
            val parts = mutableListOf(label)
            if (hasSubmenu()) parts.add("has submenu")
            if (badge != null) parts.add("badge: $badge")
            if (!enabled) parts.add("disabled")
            return parts.joinToString(", ")
        }
    }

    /**
     * Menu orientation
     */
    enum class Orientation {
        /** Vertical menu layout (default) */
        Vertical,

        /** Horizontal menu layout (menubar style) */
        Horizontal
    }

    /**
     * Selection mode for menu items
     */
    enum class SelectionMode {
        /** No selection tracking */
        None,

        /** Single item can be selected at a time */
        Single,

        /** Multiple items can be selected */
        Multiple
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Menu"
        val itemCount = items.size
        val modeDesc = when (selectionMode) {
            SelectionMode.None -> ""
            SelectionMode.Single -> ", single selection"
            SelectionMode.Multiple -> ", multiple selection"
        }
        return "$base with $itemCount items$modeDesc"
    }

    /**
     * Check if an item index is selected
     */
    fun isItemSelected(index: Int): Boolean {
        return when (selectionMode) {
            SelectionMode.Single -> selectedIndex == index
            SelectionMode.Multiple -> selectedIndices.contains(index)
            SelectionMode.None -> false
        }
    }

    companion object {
        /**
         * Create a simple vertical menu
         */
        fun vertical(
            items: List<MenuItem>,
            onItemClick: ((String) -> Unit)? = null
        ) = Menu(
            items = items,
            orientation = Orientation.Vertical,
            onItemClick = onItemClick
        )

        /**
         * Create a horizontal menubar
         */
        fun horizontal(
            items: List<MenuItem>,
            onItemClick: ((String) -> Unit)? = null
        ) = Menu(
            items = items,
            orientation = Orientation.Horizontal,
            onItemClick = onItemClick
        )

        /**
         * Create a context menu (vertical with no selection)
         */
        fun contextMenu(
            items: List<MenuItem>,
            onItemClick: ((String) -> Unit)? = null
        ) = Menu(
            items = items,
            orientation = Orientation.Vertical,
            selectionMode = SelectionMode.None,
            onItemClick = onItemClick
        )
    }
}
