/**
 * ContextMenuOverlay.kt - Voice-activated context menu overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * KMP-compatible context menu overlay for VoiceOSCoreNG.
 * Provides a numbered menu of voice commands or actions that users
 * can select by saying a number or by ID.
 *
 * Features:
 * - Display menu with optional title
 * - Numbered items for voice selection ("say one", "say two")
 * - Selection by ID or number
 * - Disabled item support (grayed out, non-selectable)
 * - Highlight state for visual feedback
 * - Position management for context-sensitive placement
 *
 * Ported from VoiceOSCore legacy implementation:
 * - Modules/VoiceOS/apps/VoiceOSCore/.../ContextMenuOverlay.kt
 *
 * Platform implementations should override onShow/onHide/onDispose
 * to provide native rendering.
 */
package com.augmentalis.voiceoscoreng.features

/**
 * Context menu overlay for voice command selection.
 *
 * Displays a menu of available actions that users can select
 * by voice (saying the number) or programmatically.
 *
 * ## Usage
 *
 * ```kotlin
 * val overlay = ContextMenuOverlay("my-menu")
 *
 * // Set up callback
 * overlay.onItemSelected = { itemId ->
 *     when (itemId) {
 *         "copy" -> performCopy()
 *         "paste" -> performPaste()
 *     }
 * }
 *
 * // Show menu
 * val items = listOf(
 *     MenuItem("copy", "Copy", number = 1),
 *     MenuItem("paste", "Paste", number = 2),
 *     MenuItem("delete", "Delete", isEnabled = false, number = 3)
 * )
 * overlay.showMenu(items, "Edit Options")
 *
 * // Select by voice command number
 * overlay.selectItemByNumber(1) // Selects "Copy"
 *
 * // Or select by ID
 * overlay.selectItemById("paste")
 *
 * // Dismiss when done
 * overlay.dismiss()
 * ```
 *
 * ## Voice Integration
 *
 * When voice commands are recognized, call:
 * - `selectItemByNumber(n)` for "say one", "say two", etc.
 * - `selectItemById(id)` for direct command mapping
 *
 * ## Platform Implementation
 *
 * Platform-specific subclasses should override:
 * - `onShow()` - Create and display native menu view
 * - `onHide()` - Remove menu from display
 * - `onDispose()` - Release all resources
 * - `onMenuUpdated()` - Refresh menu content
 * - `onHighlightChanged()` - Update visual highlight
 * - `onPositionChanged()` - Move menu to new position
 *
 * @param id Unique identifier for this overlay instance
 */
class ContextMenuOverlay(
    id: String
) : BaseOverlay(id, OverlayType.FLOATING) {

    // ═══════════════════════════════════════════════════════════════════════
    // Menu State
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Current menu items.
     */
    private var _items: List<MenuItem> = emptyList()

    /**
     * Current menu title (nullable).
     */
    private var _title: String? = null

    /**
     * Currently highlighted item ID.
     */
    private var _highlightedItemId: String? = null

    /**
     * Currently selected item ID.
     */
    private var _selectedItemId: String? = null

    /**
     * Menu X position for positioned display.
     */
    private var _menuPositionX: Float = 0f

    /**
     * Menu Y position for positioned display.
     */
    private var _menuPositionY: Float = 0f

    // ═══════════════════════════════════════════════════════════════════════
    // Callbacks
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Callback invoked when an item is selected.
     * Receives the item ID of the selected item.
     */
    var onItemSelected: ((String) -> Unit)? = null

    /**
     * Callback invoked when the menu is dismissed.
     */
    var onDismiss: (() -> Unit)? = null

    // ═══════════════════════════════════════════════════════════════════════
    // Read-Only State Properties
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Number of menu items.
     */
    val menuItemCount: Int
        get() = _items.size

    /**
     * Current menu title (null if not set).
     */
    val menuTitle: String?
        get() = _title

    /**
     * Currently highlighted item ID (null if none highlighted).
     */
    val highlightedItemId: String?
        get() = _highlightedItemId

    /**
     * Currently selected item ID (null if none selected).
     */
    val selectedItemId: String?
        get() = _selectedItemId

    /**
     * Menu X position.
     */
    val menuPositionX: Float
        get() = _menuPositionX

    /**
     * Menu Y position.
     */
    val menuPositionY: Float
        get() = _menuPositionY

    /**
     * Number of enabled items.
     */
    val enabledItemCount: Int
        get() = _items.count { it.isEnabled }

    /**
     * Number of disabled items.
     */
    val disabledItemCount: Int
        get() = _items.count { !it.isEnabled }

    /**
     * Whether any items have numbers assigned.
     */
    val hasNumberedItems: Boolean
        get() = _items.any { it.number != null }

    // ═══════════════════════════════════════════════════════════════════════
    // Update Implementation (IOverlay)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update overlay with new data.
     *
     * Only accepts [OverlayData.ContextMenu] data type.
     * Other data types are ignored.
     *
     * @param data New overlay data (must be ContextMenu type)
     */
    override fun update(data: OverlayData) {
        when (data) {
            is OverlayData.ContextMenu -> {
                _items = data.items
                _title = data.title
                onMenuUpdated()
            }
            else -> {
                // Ignore non-ContextMenu data
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Item Access
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get menu item by index.
     *
     * @param index Zero-based index
     * @return MenuItem at index, or null if out of bounds
     */
    fun getMenuItem(index: Int): MenuItem? {
        return _items.getOrNull(index)
    }

    /**
     * Find menu item by ID.
     *
     * @param id Item ID to find
     * @return MenuItem with matching ID, or null if not found
     */
    fun findItemById(id: String): MenuItem? {
        return _items.find { it.id == id }
    }

    /**
     * Find menu item by number.
     *
     * @param number Item number to find
     * @return MenuItem with matching number, or null if not found
     */
    fun findItemByNumber(number: Int): MenuItem? {
        return _items.find { it.number == number }
    }

    /**
     * Get all enabled items.
     *
     * @return List of enabled MenuItems
     */
    fun getEnabledItems(): List<MenuItem> {
        return _items.filter { it.isEnabled }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Selection by ID
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Select a menu item by ID.
     *
     * Selection only succeeds if:
     * - Item with ID exists
     * - Item is enabled
     *
     * On successful selection:
     * - Sets [selectedItemId] to the item's ID
     * - Invokes [onItemSelected] callback
     *
     * @param id ID of the item to select
     * @return true if selection succeeded, false otherwise
     */
    fun selectItemById(id: String): Boolean {
        val item = findItemById(id)
        return if (item != null && item.isEnabled) {
            _selectedItemId = id
            onItemSelected?.invoke(id)
            true
        } else {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Selection by Number
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Select a menu item by number.
     *
     * Used for voice commands like "say one", "say two".
     *
     * Selection only succeeds if:
     * - Item with number exists
     * - Item is enabled
     *
     * On successful selection:
     * - Sets [selectedItemId] to the item's ID
     * - Invokes [onItemSelected] callback
     *
     * @param number Number of the item to select
     * @return true if selection succeeded, false otherwise
     */
    fun selectItemByNumber(number: Int): Boolean {
        val item = findItemByNumber(number)
        return if (item != null && item.isEnabled) {
            _selectedItemId = item.id
            onItemSelected?.invoke(item.id)
            true
        } else {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Highlighting
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Highlight a menu item by ID.
     *
     * Highlighting provides visual feedback (e.g., during voice navigation).
     * Only sets highlight if item exists.
     *
     * @param id ID of the item to highlight
     */
    fun highlightItem(id: String) {
        val item = findItemById(id)
        if (item != null) {
            _highlightedItemId = id
            onHighlightChanged()
        }
    }

    /**
     * Highlight a menu item by number.
     *
     * Only sets highlight if item with number exists.
     *
     * @param number Number of the item to highlight
     */
    fun highlightByNumber(number: Int) {
        val item = findItemByNumber(number)
        if (item != null) {
            _highlightedItemId = item.id
            onHighlightChanged()
        }
    }

    /**
     * Clear the current highlight.
     */
    fun clearHighlight() {
        _highlightedItemId = null
        onHighlightChanged()
    }

    /**
     * Check if a specific item is highlighted.
     *
     * @param id ID of the item to check
     * @return true if the item is currently highlighted
     */
    fun isItemHighlighted(id: String): Boolean {
        return _highlightedItemId == id
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Show Menu Convenience Methods
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Show menu with items and optional title.
     *
     * Convenience method that:
     * 1. Clears previous state
     * 2. Updates items and title
     * 3. Shows the overlay
     *
     * @param items Menu items to display
     * @param title Optional menu title
     */
    fun showMenu(items: List<MenuItem>, title: String?) {
        clearState()
        _items = items
        _title = title
        onMenuUpdated()
        show()
    }

    /**
     * Show menu at specific position.
     *
     * @param items Menu items to display
     * @param title Optional menu title
     * @param x X position in screen coordinates
     * @param y Y position in screen coordinates
     */
    fun showMenuAtPosition(items: List<MenuItem>, title: String?, x: Float, y: Float) {
        clearState()
        _items = items
        _title = title
        _menuPositionX = x
        _menuPositionY = y
        updatePosition(x, y)
        onMenuUpdated()
        show()
    }

    /**
     * Update menu position while visible.
     *
     * @param x New X position
     * @param y New Y position
     */
    fun updateMenuPosition(x: Float, y: Float) {
        _menuPositionX = x
        _menuPositionY = y
        if (isVisible) {
            updatePosition(x, y)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Dismiss
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Dismiss the menu.
     *
     * Hides the overlay and clears selection/highlight state.
     * Invokes [onDismiss] callback.
     */
    fun dismiss() {
        clearState()
        hide()
        onDismiss?.invoke()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Lifecycle Overrides
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Called when overlay becomes visible.
     * Platform implementations should create native menu view.
     */
    override fun onShow() {
        // Platform implementations override this
    }

    /**
     * Called when overlay becomes hidden.
     * Platform implementations should remove menu view.
     */
    override fun onHide() {
        // Platform implementations override this
    }

    /**
     * Called when overlay is disposed.
     * Clears all state and callbacks.
     */
    override fun onDispose() {
        clearState()
        _items = emptyList()
        _title = null
        onItemSelected = null
        onDismiss = null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Platform Callbacks
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Called when menu items or title are updated.
     * Platform implementations should refresh the menu display.
     */
    protected open fun onMenuUpdated() {
        // Platform implementations override this to refresh UI
    }

    /**
     * Called when highlight state changes.
     * Platform implementations should update visual highlight.
     */
    protected open fun onHighlightChanged() {
        // Platform implementations override this to update highlight
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Private Helpers
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Clear selection and highlight state.
     */
    private fun clearState() {
        _selectedItemId = null
        _highlightedItemId = null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Debug
    // ═══════════════════════════════════════════════════════════════════════

    override fun toString(): String {
        return "ContextMenuOverlay(id='$id', visible=$isVisible, items=${_items.size}, " +
            "title=$_title, highlighted=$_highlightedItemId, selected=$_selectedItemId)"
    }
}
