/**
 * NumberedSelectionOverlay.kt - Numbered item selection overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * KMP-compatible implementation of numbered selection overlay.
 * Displays numbered badges over UI elements for voice selection.
 *
 * Ported from VoiceOS VoiceOSCore module:
 * Original: com.augmentalis.voiceoscore.accessibility.overlays.NumberedSelectionOverlay
 *
 * Platform implementations provide native rendering via IOverlay interface.
 * This class provides the core logic: item management, selection, positioning.
 */
package com.augmentalis.voiceoscore

import kotlin.math.roundToInt

/**
 * Simple position class for badge coordinates.
 */
data class Position(val x: Int, val y: Int)

/**
 * Overlay that displays numbered badges for voice selection.
 *
 * Shows numbered badges positioned over UI elements, allowing users
 * to select items by saying the corresponding number.
 *
 * Features:
 * - Numbered badges with color-coded states (has name, no name, disabled)
 * - Configurable badge positioning (anchor points)
 * - Customizable styling via NumberOverlayStyle
 * - Accessibility support (announcements, descriptions)
 *
 * Usage:
 * ```kotlin
 * val overlay = NumberedSelectionOverlay()
 *
 * // Show items for selection
 * val items = listOf(
 *     NumberedItem(1, "Submit", Rect(100, 200, 300, 250)),
 *     NumberedItem(2, "Cancel", Rect(100, 260, 300, 310))
 * )
 * overlay.showItems(items)
 *
 * // Handle selection
 * val selected = overlay.selectItem(1)
 * if (selected != null) {
 *     println("Selected: ${selected.label}")
 * }
 *
 * // Clean up
 * overlay.dispose()
 * ```
 *
 * @property id Unique identifier for this overlay instance
 * @property style Style configuration for badge rendering
 * @property instructionText Custom instruction text (defaults to "Say a number to select")
 * @property maxVisibleBadges Maximum number of badges to display (default 9). Items beyond this
 *           limit are available via [getOverflowItems] and can still be selected by number.
 *
 * @see NumberOverlayStyle for styling options
 * @see NumberedItem for item data structure
 * @see IOverlay for the overlay contract
 */
class NumberedSelectionOverlay(
    override val id: String = "numbered-selection-overlay-${System.currentTimeMillis()}",
    style: NumberOverlayStyle = NumberOverlayStyles.DEFAULT,
    private val instructionText: String? = null,
    val maxVisibleBadges: Int = 9
) : IOverlay {

    // ═══════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════

    @Volatile
    private var _isVisible: Boolean = false
    override val isVisible: Boolean get() = _isVisible

    @Volatile
    private var _style: NumberOverlayStyle = style
    val style: NumberOverlayStyle get() = _style

    private var _items: MutableList<NumberedItem> = mutableListOf()

    /**
     * Current disambiguation display configuration.
     * Platform renderers should use this to determine highlight style, animations, etc.
     */
    @Volatile
    private var _displayConfig: DisambiguationOverlayConfig = DisambiguationOverlayConfig.DEFAULT
    val displayConfig: DisambiguationOverlayConfig get() = _displayConfig

    /**
     * Current items displayed in the overlay.
     * Returns an immutable copy.
     */
    val items: List<NumberedItem> get() = _items.toList()

    /**
     * Number of items currently in the overlay.
     */
    val itemCount: Int get() = _items.size

    /**
     * Number of enabled items in the overlay.
     */
    val enabledItemCount: Int get() = _items.count { it.isEnabled }

    /**
     * Whether the overlay has any items.
     */
    val hasItems: Boolean get() = _items.isNotEmpty()

    /**
     * Whether there are more items than can be displayed.
     * True when itemCount exceeds maxVisibleBadges.
     */
    val hasOverflow: Boolean get() = _items.size > maxVisibleBadges

    @Volatile
    private var _isDisposed: Boolean = false

    // ═══════════════════════════════════════════════════════════════════════
    // IOverlay Implementation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Show the overlay.
     * Has no effect if already disposed.
     */
    override fun show() {
        if (!_isDisposed) {
            _isVisible = true
        }
    }

    /**
     * Hide the overlay.
     */
    override fun hide() {
        _isVisible = false
    }

    /**
     * Toggle overlay visibility.
     */
    override fun toggle() {
        if (_isVisible) hide() else show()
    }

    /**
     * Update overlay with new data.
     *
     * Only processes [OverlayData.NumberedItems] data.
     * Other data types are ignored.
     */
    override fun update(data: OverlayData) {
        if (_isDisposed) return

        when (data) {
            is OverlayData.NumberedItems -> {
                _items.clear()
                _items.addAll(data.items)
                _displayConfig = data.displayConfig
            }
            else -> {
                // Ignore non-NumberedItems data
            }
        }
    }

    /**
     * Dispose the overlay and release resources.
     */
    override fun dispose() {
        _isDisposed = true
        _isVisible = false
        _items.clear()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Item Management
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Show the overlay with the given items.
     *
     * Sets the items and makes the overlay visible.
     *
     * @param items List of items to display
     */
    fun showItems(items: List<NumberedItem>) {
        if (_isDisposed) return

        _items.clear()
        _items.addAll(items)
        _isVisible = true
    }

    /**
     * Update items without changing visibility.
     *
     * @param items New list of items
     */
    fun updateItems(items: List<NumberedItem>) {
        if (_isDisposed) return

        _items.clear()
        _items.addAll(items)
    }

    /**
     * Clear all items from the overlay.
     *
     * @param autoHide If true, also hides the overlay
     */
    fun clearItems(autoHide: Boolean = true) {
        _items.clear()
        if (autoHide) {
            _isVisible = false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Overflow Management
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get the items that should be visibly displayed.
     *
     * Returns the first [maxVisibleBadges] items from the list.
     * These are the items that should have badges rendered on screen.
     *
     * @return List of items to display (up to maxVisibleBadges)
     */
    fun getVisibleItems(): List<NumberedItem> {
        return _items.take(maxVisibleBadges)
    }

    /**
     * Get the items that overflow beyond the visible limit.
     *
     * Returns items beyond the [maxVisibleBadges] threshold.
     * These items exist but should not have badges rendered initially.
     * Platform implementations may provide a "more" indicator or expandable view.
     *
     * @return List of overflow items (empty if no overflow)
     */
    fun getOverflowItems(): List<NumberedItem> {
        return if (_items.size > maxVisibleBadges) {
            _items.drop(maxVisibleBadges)
        } else {
            emptyList()
        }
    }

    /**
     * Get the count of overflow items.
     *
     * @return Number of items beyond maxVisibleBadges (0 if no overflow)
     */
    fun getOverflowCount(): Int {
        return maxOf(0, _items.size - maxVisibleBadges)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Selection
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Select an item by its number.
     *
     * Returns the item if found and enabled (or if ignoreDisabled is false).
     * Returns null if not found or if disabled (when ignoreDisabled is true).
     *
     * @param number The number of the item to select
     * @param ignoreDisabled If true, returns null for disabled items
     * @return The selected item, or null if not found/disabled
     */
    fun selectItem(number: Int, ignoreDisabled: Boolean = true): NumberedItem? {
        val item = _items.find { it.number == number }

        return when {
            item == null -> null
            ignoreDisabled && !item.isEnabled -> null
            else -> item
        }
    }

    /**
     * Get an item by its number without selection logic.
     *
     * @param number The number of the item
     * @return The item, or null if not found
     */
    fun getItemByNumber(number: Int): NumberedItem? {
        return _items.find { it.number == number }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Badge Color
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get the badge color for an item based on its state.
     *
     * Color logic:
     * - Disabled items: disabledColor (grey)
     * - Enabled with name: hasNameColor (green)
     * - Enabled without name: noNameColor (orange)
     *
     * @param item The item to get the color for
     * @return Color as ARGB Long value
     */
    fun getBadgeColor(item: NumberedItem): Long {
        return when {
            !item.isEnabled -> _style.disabledColor
            item.hasName -> _style.hasNameColor
            else -> _style.noNameColor
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Badge Positioning
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Calculate the badge position for the given element bounds.
     *
     * Position is calculated based on:
     * - Anchor point (TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT)
     * - Offset values from style
     * - Badge size (diameter)
     *
     * @param bounds The bounds of the UI element
     * @return Position for the badge's top-left corner
     */
    fun calculateBadgePosition(bounds: Rect): Position {
        val badgeSize = getBadgeSize()
        val offsetX = _style.offsetX.roundToInt()
        val offsetY = _style.offsetY.roundToInt()

        return when (_style.anchorPoint) {
            AnchorPoint.TOP_LEFT -> Position(
                x = bounds.left + offsetX,
                y = bounds.top + offsetY
            )
            AnchorPoint.TOP_RIGHT -> Position(
                x = bounds.right - badgeSize + offsetX,
                y = bounds.top + offsetY
            )
            AnchorPoint.BOTTOM_LEFT -> Position(
                x = bounds.left + offsetX,
                y = bounds.bottom - badgeSize + offsetY
            )
            AnchorPoint.BOTTOM_RIGHT -> Position(
                x = bounds.right - badgeSize + offsetX,
                y = bounds.bottom - badgeSize + offsetY
            )
        }
    }

    /**
     * Get the badge size (diameter) in pixels.
     *
     * @return Badge diameter based on style's circle radius
     */
    fun getBadgeSize(): Int {
        return (_style.circleRadius * 2).roundToInt()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Style
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Change the overlay style.
     *
     * @param newStyle The new style to apply
     */
    fun setStyle(newStyle: NumberOverlayStyle) {
        _style = newStyle
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Label Display
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Determine if a label should be shown for the item.
     *
     * Shows label only if the item has a name AND the label is not empty.
     *
     * @param item The item to check
     * @return True if label should be displayed
     */
    fun shouldShowLabel(item: NumberedItem): Boolean {
        return item.hasName && item.label.isNotEmpty()
    }

    /**
     * Get a truncated version of the label if needed.
     *
     * @param label The original label
     * @param maxLength Maximum length before truncation
     * @return Truncated label with "..." if too long, or original if within limit
     */
    fun getTruncatedLabel(label: String, maxLength: Int = 20): String {
        return if (label.length > maxLength) {
            label.take(maxLength - 3) + "..."
        } else {
            label
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Accessibility
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get announcement text for screen readers.
     *
     * Describes the current state of the overlay for accessibility.
     * Includes overflow information when items exceed maxVisibleBadges.
     *
     * @return Announcement text for TTS/accessibility services
     */
    fun getAnnouncementText(): String {
        return when {
            _items.isEmpty() -> "No items available"
            _items.size == 1 -> "1 item available. Say a number to select."
            hasOverflow -> {
                val overflowCount = getOverflowCount()
                "${_items.size} items available, showing first $maxVisibleBadges. $overflowCount more items available. Say a number to select."
            }
            else -> "${_items.size} items available. Say a number to select."
        }
    }

    /**
     * Get description text for a specific item.
     *
     * @param item The item to describe
     * @return Description for accessibility
     */
    fun getItemDescription(item: NumberedItem): String {
        val typeDescription = if (item.hasName && item.label.isNotEmpty()) {
            item.label
        } else {
            "item ${item.number}"
        }

        val stateDescription = when {
            !item.isEnabled -> ", disabled"
            else -> ""
        }

        return "${item.number}: $typeDescription$stateDescription"
    }

    /**
     * Get the instruction text to display.
     *
     * @return Instruction text for users
     */
    fun getInstructionText(): String {
        return instructionText ?: _displayConfig.popupMessage
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Disambiguation Display Helpers (for platform renderers)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Whether to show flashing/animated stroke around elements.
     */
    fun shouldShowFlashingStroke(): Boolean =
        _displayConfig.highlightStyle == DisambiguationHighlightStyle.FLASHING_STROKE

    /**
     * Whether to show solid highlight overlay on elements.
     */
    fun shouldShowSolidHighlight(): Boolean =
        _displayConfig.highlightStyle == DisambiguationHighlightStyle.SOLID_HIGHLIGHT

    /**
     * Whether to show pulsing glow effect around elements.
     */
    fun shouldShowPulsingGlow(): Boolean =
        _displayConfig.highlightStyle == DisambiguationHighlightStyle.PULSING_GLOW

    /**
     * Whether badges should be animated (pulse/flash).
     */
    fun shouldAnimateBadges(): Boolean = _displayConfig.badgeAnimationEnabled

    /**
     * Whether to show instruction popup.
     */
    fun shouldShowPopup(): Boolean = _displayConfig.showPopup

    /**
     * Get stroke width in dp for highlight rendering.
     */
    fun getHighlightStrokeWidth(): Float = _displayConfig.strokeWidth

    /**
     * Get stroke color as ARGB Long for highlight rendering.
     */
    fun getHighlightStrokeColor(): Long = _displayConfig.strokeColor

    /**
     * Get animation cycle duration in milliseconds.
     */
    fun getAnimationDurationMs(): Long = _displayConfig.animationDurationMs

    /**
     * Get popup fade delay in milliseconds.
     */
    fun getPopupFadeDelayMs(): Long = _displayConfig.popupFadeDelayMs

    // ═══════════════════════════════════════════════════════════════════════
    // Platform Time Stub (for ID generation)
    // ═══════════════════════════════════════════════════════════════════════

    companion object {
        /**
         * Generate a unique ID for overlay instances.
         * Uses simple counter for KMP compatibility.
         */
        @Volatile
        private var idCounter: Long = 0

        internal fun generateId(): String {
            return "numbered-selection-overlay-${++idCounter}"
        }
    }
}

/**
 * Platform-agnostic time access.
 * Actual implementations provided by platform source sets.
 */
internal object System {
    private var counter: Long = 0

    fun currentTimeMillis(): Long {
        return ++counter
    }
}
