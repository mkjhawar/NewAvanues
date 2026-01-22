/**
 * DynamicMenuHandler.kt - Handles dynamic menus to avoid re-scanning
 *
 * Part of LearnApp Safety System.
 * Manages sliding menus, overflow menus, and expandable content to:
 * 1. Query full menu content via AIDL (not just visible items)
 * 2. Cache menu items to avoid re-scanning when items scroll in/out of view
 * 3. Generate MNU IPC lines for AVU export
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md Section 5.3
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.learnappcore.safety

import android.graphics.Rect
import com.augmentalis.learnappcore.models.ElementInfo

/**
 * Type of menu detected.
 *
 * Used to determine scanning strategy and AVU export.
 */
enum class MenuType(val ipcCode: String, val description: String) {
    /**
     * 3-dot overflow menu (most common).
     * Opens as popup with limited visible items.
     */
    OVERFLOW("OVF", "Overflow menu (3-dot)"),

    /**
     * Navigation drawer (hamburger menu).
     * Slides from edge, may have nested sections.
     */
    DRAWER("DRW", "Navigation drawer"),

    /**
     * Bottom sheet menu.
     * Slides up from bottom, can expand.
     */
    BOTTOM_SHEET("BSH", "Bottom sheet"),

    /**
     * Dropdown/spinner menu.
     * Opens below trigger element.
     */
    DROPDOWN("DRP", "Dropdown menu"),

    /**
     * Context menu (long press).
     * Opens near element, position varies.
     */
    CONTEXT("CTX", "Context menu"),

    /**
     * Tab menu (horizontal scrolling tabs).
     * May have more tabs than visible.
     */
    TAB_BAR("TAB", "Tab bar"),

    /**
     * Expandable section (accordion).
     * Expands to show nested items.
     */
    EXPANDABLE("EXP", "Expandable section");

    companion object {
        fun fromIpcCode(code: String): MenuType? {
            return entries.find { it.ipcCode.equals(code, ignoreCase = true) }
        }
    }
}

/**
 * Represents a discovered menu with its items.
 *
 * @property menuId Unique identifier (resourceId or generated)
 * @property menuType Type of menu
 * @property triggerElement Element that opens this menu
 * @property totalItems Total items in menu (including hidden)
 * @property visibleItems Number of items visible without scrolling
 * @property items All menu items (cached after first query)
 * @property screenHash Screen where menu was found
 * @property discoveredTimestamp When menu was first found
 * @property lastQueryTimestamp When full content was last queried
 */
data class DiscoveredMenu(
    val menuId: String,
    val menuType: MenuType,
    val triggerElement: ElementInfo?,
    val totalItems: Int,
    val visibleItems: Int,
    val items: List<ElementInfo> = emptyList(),
    val screenHash: String = "",
    val discoveredTimestamp: Long = System.currentTimeMillis(),
    val lastQueryTimestamp: Long = 0
) {
    /**
     * Whether full menu content has been queried.
     */
    val hasFullContent: Boolean
        get() = items.isNotEmpty() && lastQueryTimestamp > 0

    /**
     * Whether menu has items beyond visible viewport.
     */
    val hasHiddenItems: Boolean
        get() = totalItems > visibleItems

    /**
     * Generate MNU IPC line for AVU export.
     *
     * Format: MNU:menu_id:total_items:visible_items:menu_type
     */
    fun toMnuLine(): String {
        return "MNU:$menuId:$totalItems:$visibleItems:${menuType.name}"
    }

    /**
     * Create updated menu with full item list.
     */
    fun withItems(fullItems: List<ElementInfo>): DiscoveredMenu {
        return copy(
            items = fullItems,
            totalItems = fullItems.size,
            lastQueryTimestamp = System.currentTimeMillis()
        )
    }

    companion object {
        /**
         * Parse from AVU MNU line.
         *
         * Input: "MNU:more_menu:15:5:OVERFLOW"
         */
        fun fromAvuLine(line: String): DiscoveredMenu? {
            if (!line.startsWith("MNU:")) return null
            val parts = line.split(":")
            if (parts.size < 5) return null

            val menuType = try {
                MenuType.valueOf(parts[4])
            } catch (e: IllegalArgumentException) {
                MenuType.OVERFLOW
            }

            return DiscoveredMenu(
                menuId = parts[1],
                menuType = menuType,
                triggerElement = null,
                totalItems = parts[2].toIntOrNull() ?: 0,
                visibleItems = parts[3].toIntOrNull() ?: 0
            )
        }
    }
}

/**
 * Dynamic Menu Handler - Manages menu discovery and caching
 *
 * Prevents re-scanning of menus when items scroll in/out of view.
 * Queries full menu content via AIDL when menu is first opened.
 */
object DynamicMenuHandler {

    // ============================================================
    // Menu trigger indicators (class names and resource IDs)
    // ============================================================
    val OVERFLOW_INDICATORS = listOf(
        "OverflowMenuButton",
        "ActionMenuItemView",
        "more_options",
        "overflow",
        "menu_button",
        "options_menu"
    )

    val DRAWER_INDICATORS = listOf(
        "NavigationView",
        "DrawerLayout",
        "navigation_drawer",
        "nav_drawer",
        "hamburger"
    )

    val BOTTOM_SHEET_INDICATORS = listOf(
        "BottomSheet",
        "BottomSheetBehavior",
        "bottom_sheet",
        "action_sheet"
    )

    val DROPDOWN_INDICATORS = listOf(
        "Spinner",
        "AutoCompleteTextView",
        "dropdown",
        "spinner",
        "select"
    )

    val EXPANDABLE_INDICATORS = listOf(
        "ExpandableListView",
        "ExpansionPanel",
        "expandable",
        "accordion",
        "collapsible"
    )

    // ============================================================
    // Cached menus: screenHash -> menuId -> DiscoveredMenu
    // ============================================================
    private val cachedMenus = mutableMapOf<String, MutableMap<String, DiscoveredMenu>>()

    /**
     * Detect if element is a menu trigger.
     *
     * @param element Element to check
     * @return MenuType if it's a trigger, null otherwise
     */
    fun detectMenuTrigger(element: ElementInfo): MenuType? {
        val className = element.className.lowercase()
        val resourceId = element.resourceId.lowercase()
        val label = element.getDisplayName().lowercase()

        return when {
            // Overflow menu (3-dot)
            OVERFLOW_INDICATORS.any { className.contains(it.lowercase()) ||
                                      resourceId.contains(it.lowercase()) } -> MenuType.OVERFLOW
            label == "more" || label == "more options" || label == "..." -> MenuType.OVERFLOW

            // Navigation drawer
            DRAWER_INDICATORS.any { className.contains(it.lowercase()) ||
                                    resourceId.contains(it.lowercase()) } -> MenuType.DRAWER

            // Bottom sheet
            BOTTOM_SHEET_INDICATORS.any { className.contains(it.lowercase()) ||
                                          resourceId.contains(it.lowercase()) } -> MenuType.BOTTOM_SHEET

            // Dropdown
            DROPDOWN_INDICATORS.any { className.contains(it.lowercase()) ||
                                      resourceId.contains(it.lowercase()) } -> MenuType.DROPDOWN

            // Expandable
            EXPANDABLE_INDICATORS.any { className.contains(it.lowercase()) ||
                                        resourceId.contains(it.lowercase()) } -> MenuType.EXPANDABLE

            // Check for tab bar
            className.contains("tab") || resourceId.contains("tab") -> MenuType.TAB_BAR

            else -> null
        }
    }

    /**
     * Detect if element is inside an open menu.
     *
     * @param element Element to check
     * @return MenuType if inside a menu, null otherwise
     */
    fun detectMenuContainer(element: ElementInfo): MenuType? {
        val className = element.className.lowercase()
        val resourceId = element.resourceId.lowercase()

        return when {
            // Popup/overflow menu items
            className.contains("popup") || className.contains("menuitem") -> MenuType.OVERFLOW
            resourceId.contains("menu_item") -> MenuType.OVERFLOW

            // Navigation drawer items
            className.contains("navigationview") ||
            className.contains("navigationmenuitem") -> MenuType.DRAWER

            // Bottom sheet content
            className.contains("bottomsheet") -> MenuType.BOTTOM_SHEET

            // Dropdown items
            className.contains("dropdownview") ||
            className.contains("listpopupwindow") -> MenuType.DROPDOWN

            else -> null
        }
    }

    /**
     * Register a discovered menu.
     *
     * Call when a menu trigger is clicked and menu opens.
     *
     * @param screenHash Current screen hash
     * @param triggerElement Element that was clicked
     * @param menuType Type of menu
     * @param visibleItems Currently visible menu items
     * @return DiscoveredMenu for further processing
     */
    fun registerMenu(
        screenHash: String,
        triggerElement: ElementInfo,
        menuType: MenuType,
        visibleItems: List<ElementInfo>
    ): DiscoveredMenu {
        val menuId = generateMenuId(triggerElement, menuType)

        // Check if already cached
        val existingMenu = getMenu(screenHash, menuId)
        if (existingMenu?.hasFullContent == true) {
            return existingMenu
        }

        // Create new menu entry
        val menu = DiscoveredMenu(
            menuId = menuId,
            menuType = menuType,
            triggerElement = triggerElement,
            totalItems = visibleItems.size, // Will be updated when full query completes
            visibleItems = visibleItems.size,
            items = visibleItems,
            screenHash = screenHash
        )

        // Cache it
        val screenMenus = cachedMenus.getOrPut(screenHash) { mutableMapOf() }
        screenMenus[menuId] = menu

        return menu
    }

    /**
     * Update menu with full content from AIDL query.
     *
     * Call after querying full menu content via JIT service.
     *
     * @param screenHash Screen hash
     * @param menuId Menu identifier
     * @param fullItems All menu items
     * @return Updated DiscoveredMenu
     */
    fun updateMenuWithFullContent(
        screenHash: String,
        menuId: String,
        fullItems: List<ElementInfo>
    ): DiscoveredMenu? {
        val menu = getMenu(screenHash, menuId) ?: return null

        val updatedMenu = menu.withItems(fullItems)
        cachedMenus[screenHash]?.set(menuId, updatedMenu)

        return updatedMenu
    }

    /**
     * Get cached menu by ID.
     *
     * @param screenHash Screen hash
     * @param menuId Menu identifier
     * @return Cached menu or null
     */
    fun getMenu(screenHash: String, menuId: String): DiscoveredMenu? {
        return cachedMenus[screenHash]?.get(menuId)
    }

    /**
     * Get all cached menus for a screen.
     *
     * @param screenHash Screen hash
     * @return List of cached menus
     */
    fun getMenusForScreen(screenHash: String): List<DiscoveredMenu> {
        return cachedMenus[screenHash]?.values?.toList() ?: emptyList()
    }

    /**
     * Check if a menu item has already been captured.
     *
     * Use to avoid re-processing items that scroll into view.
     *
     * @param screenHash Screen hash
     * @param menuId Menu identifier
     * @param itemStableId Stable ID of the item
     * @return true if item already captured
     */
    fun isItemCaptured(screenHash: String, menuId: String, itemStableId: String): Boolean {
        val menu = getMenu(screenHash, menuId) ?: return false
        return menu.items.any { it.stableId() == itemStableId }
    }

    /**
     * Generate unique menu ID from trigger element.
     */
    private fun generateMenuId(trigger: ElementInfo, type: MenuType): String {
        val prefix = type.ipcCode.lowercase()
        val identifier = when {
            trigger.resourceId.isNotEmpty() -> trigger.resourceId.substringAfterLast("/")
            trigger.text.isNotEmpty() -> trigger.text.take(10).replace(" ", "_")
            else -> "${trigger.bounds.centerX()}_${trigger.bounds.centerY()}"
        }
        return "${prefix}_$identifier"
    }

    /**
     * Clear cached menus for a screen.
     *
     * @param screenHash Screen hash
     */
    fun clearScreen(screenHash: String) {
        cachedMenus.remove(screenHash)
    }

    /**
     * Clear all cached menus.
     */
    fun reset() {
        cachedMenus.clear()
    }

    /**
     * Export all menus as AVU MNU lines.
     *
     * @return List of MNU IPC lines
     */
    fun exportMnuLines(): List<String> {
        return cachedMenus.values.flatMap { screenMenus ->
            screenMenus.values.map { it.toMnuLine() }
        }
    }

    /**
     * Import menus from AVU MNU lines.
     *
     * @param lines List of MNU IPC lines
     * @param screenHash Screen to associate menus with
     */
    fun importFromAvuLines(lines: List<String>, screenHash: String) {
        for (line in lines) {
            val menu = DiscoveredMenu.fromAvuLine(line) ?: continue
            val screenMenus = cachedMenus.getOrPut(screenHash) { mutableMapOf() }
            screenMenus[menu.menuId] = menu.copy(screenHash = screenHash)
        }
    }
}
