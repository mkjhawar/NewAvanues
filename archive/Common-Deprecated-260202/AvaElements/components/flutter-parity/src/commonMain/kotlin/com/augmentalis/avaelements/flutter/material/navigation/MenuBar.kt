package com.augmentalis.avaelements.flutter.material.navigation

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * MenuBar component - Flutter Material parity
 *
 * A horizontal menu bar component typically found at the top of desktop applications.
 * Contains top-level menu items that can trigger actions or open dropdown menus.
 *
 * **Flutter Equivalent:** `MenuBar` widget
 * **Material Design 3:** https://m3.material.io/components/menus/overview
 *
 * ## Features
 * - Horizontal layout with top-level menu items
 * - Dropdown menu support (via Menu component)
 * - Keyboard navigation (Alt+Letter accelerators)
 * - Focus management
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * MenuBar(
 *     items = listOf(
 *         MenuBar.MenuBarItem(
 *             id = "file",
 *             label = "File",
 *             accelerator = "f",
 *             children = listOf(
 *                 Menu.MenuItem(id = "new", label = "New", accelerator = "n"),
 *                 Menu.MenuItem(id = "open", label = "Open", accelerator = "o"),
 *                 Menu.MenuItem(id = "divider", label = "", divider = true),
 *                 Menu.MenuItem(id = "exit", label = "Exit", accelerator = "x")
 *             )
 *         ),
 *         MenuBar.MenuBarItem(
 *             id = "edit",
 *             label = "Edit",
 *             accelerator = "e",
 *             children = listOf(
 *                 Menu.MenuItem(id = "cut", label = "Cut"),
 *                 Menu.MenuItem(id = "copy", label = "Copy"),
 *                 Menu.MenuItem(id = "paste", label = "Paste")
 *             )
 *         )
 *     ),
 *     onItemClick = { itemId -> println("Clicked: $itemId") }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property items List of top-level menu items
 * @property backgroundColor Background color of the menu bar
 * @property elevation Shadow elevation
 * @property height Custom height in dp (default 48)
 * @property contentPadding Custom padding for the menu bar
 * @property activeMenuId Currently active/open menu ID
 * @property showAccelerators Whether to show keyboard accelerators (underlined letters)
 * @property contentDescription Accessibility description for TalkBack
 * @property onItemClick Callback invoked when a menu item is clicked (not serialized)
 * @property onMenuOpen Callback invoked when a menu is opened (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class MenuBar(
    override val type: String = "MenuBar",
    override val id: String? = null,
    val items: List<MenuBarItem>,
    val backgroundColor: String? = null,
    val elevation: Float? = null,
    val height: Float = 48f,
    val contentPadding: com.augmentalis.avaelements.core.types.Spacing? = null,
    val activeMenuId: String? = null,
    val showAccelerators: Boolean = false,
    val contentDescription: String? = null,
    @Transient
    val onItemClick: ((String) -> Unit)? = null,
    @Transient
    val onMenuOpen: ((String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * MenuBar item data class
     *
     * @property id Unique identifier for the item
     * @property label Display text for the item
     * @property accelerator Keyboard accelerator character (underlined in label)
     * @property icon Optional icon identifier
     * @property enabled Whether the item is enabled for interaction
     * @property children Optional dropdown menu items
     * @property onClick Callback invoked when item is clicked
     */
    data class MenuBarItem(
        val id: String,
        val label: String,
        val accelerator: String? = null,
        val icon: String? = null,
        val enabled: Boolean = true,
        val children: List<Menu.MenuItem>? = null,
        @Transient
        val onClick: (() -> Unit)? = null
    ) {
        /**
         * Check if this item has a dropdown menu
         */
        fun hasDropdown(): Boolean = children != null && children.isNotEmpty()

        /**
         * Get formatted label with accelerator underline
         */
        fun getFormattedLabel(): String {
            if (accelerator == null || !accelerator.matches(Regex("[a-zA-Z]"))) {
                return label
            }
            val index = label.indexOf(accelerator, ignoreCase = true)
            return if (index >= 0) {
                "${label.substring(0, index)}_${label.substring(index, index + 1)}_${label.substring(index + 1)}"
            } else {
                label
            }
        }

        /**
         * Get accessibility description
         */
        fun getAccessibilityDescription(): String {
            val parts = mutableListOf(label)
            if (hasDropdown()) parts.add("has menu")
            if (accelerator != null) parts.add("accelerator: Alt+$accelerator")
            if (!enabled) parts.add("disabled")
            return parts.joinToString(", ")
        }
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Menu bar"
        val itemCount = items.size
        val activeDesc = if (activeMenuId != null) {
            val activeItem = items.find { it.id == activeMenuId }
            ", ${activeItem?.label ?: "menu"} open"
        } else {
            ""
        }
        return "$base with $itemCount menus$activeDesc"
    }

    /**
     * Check if a menu is currently open
     */
    fun isMenuOpen(menuId: String): Boolean {
        return activeMenuId == menuId
    }

    /**
     * Get active menu item
     */
    fun getActiveMenuItem(): MenuBarItem? {
        return items.find { it.id == activeMenuId }
    }

    companion object {
        /**
         * Create a standard desktop menu bar
         */
        fun desktop(
            items: List<MenuBarItem>,
            onItemClick: ((String) -> Unit)? = null
        ) = MenuBar(
            items = items,
            showAccelerators = true,
            onItemClick = onItemClick
        )

        /**
         * Create a simple menu bar without accelerators
         */
        fun simple(
            items: List<MenuBarItem>,
            onItemClick: ((String) -> Unit)? = null
        ) = MenuBar(
            items = items,
            showAccelerators = false,
            onItemClick = onItemClick
        )

        /**
         * Create a typical File/Edit/View menu bar
         */
        fun standard(
            fileItems: List<Menu.MenuItem>,
            editItems: List<Menu.MenuItem>,
            viewItems: List<Menu.MenuItem>,
            onItemClick: ((String) -> Unit)? = null
        ) = MenuBar(
            items = listOf(
                MenuBarItem(
                    id = "file",
                    label = "File",
                    accelerator = "f",
                    children = fileItems
                ),
                MenuBarItem(
                    id = "edit",
                    label = "Edit",
                    accelerator = "e",
                    children = editItems
                ),
                MenuBarItem(
                    id = "view",
                    label = "View",
                    accelerator = "v",
                    children = viewItems
                )
            ),
            showAccelerators = true,
            onItemClick = onItemClick
        )
    }
}
