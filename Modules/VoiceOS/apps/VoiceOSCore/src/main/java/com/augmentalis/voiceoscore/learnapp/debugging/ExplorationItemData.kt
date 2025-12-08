/**
 * ExplorationItemData.kt - Data models for debug overlay item tracking
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-08
 * Rewritten: 2025-12-08 (Complete rewrite for scrollable list overlay)
 *
 * Tracks ALL items scanned during exploration with:
 * - Click status (clicked/not clicked/blocked)
 * - Source screen information
 * - VUID assignment
 * - Blocking reason (if applicable)
 */
package com.augmentalis.voiceoscore.learnapp.debugging

/**
 * Status of an item during exploration
 */
enum class ItemStatus {
    /** Item discovered but not yet clicked */
    DISCOVERED,
    /** Item was clicked during exploration */
    CLICKED,
    /** Item was blocked (dangerous element) */
    BLOCKED,
    /** Currently being explored */
    EXPLORING
}

/**
 * Represents a single UI element tracked during exploration
 *
 * @property id Unique identifier for this item (stableId or generated)
 * @property vuid Voice UUID if assigned
 * @property displayName Human-readable name (text or contentDescription)
 * @property className Element class name (e.g., "Button", "ImageView")
 * @property resourceId Android resource ID if available
 * @property screenHash Hash of screen where item was found
 * @property screenName Activity name of screen
 * @property status Current status (discovered/clicked/blocked)
 * @property blockReason Reason if blocked (e.g., "Call button (CRITICAL)")
 * @property timestamp When item was first discovered
 * @property clickedAt When item was clicked (if clicked)
 * @property navigatedTo Screen hash navigated to after click (if any)
 */
data class ExplorationItem(
    val id: String,
    val vuid: String?,
    val displayName: String,
    val className: String,
    val resourceId: String?,
    val screenHash: String,
    val screenName: String,
    var status: ItemStatus = ItemStatus.DISCOVERED,
    val blockReason: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    var clickedAt: Long? = null,
    var navigatedTo: String? = null
) {
    /**
     * Short display name (max 30 chars)
     */
    fun shortName(): String = if (displayName.length > 30) {
        displayName.take(27) + "..."
    } else {
        displayName
    }

    /**
     * Status emoji for display
     */
    fun statusEmoji(): String = when (status) {
        ItemStatus.DISCOVERED -> "⚪"  // White circle - not clicked
        ItemStatus.CLICKED -> "✅"     // Green check - clicked
        ItemStatus.BLOCKED -> "🚫"     // No entry - blocked
        ItemStatus.EXPLORING -> "🔄"   // Arrows - currently exploring
    }

    /**
     * Status color for display (ARGB)
     */
    fun statusColor(): Int = when (status) {
        ItemStatus.DISCOVERED -> 0xFF9E9E9E.toInt()  // Gray
        ItemStatus.CLICKED -> 0xFF4CAF50.toInt()     // Green
        ItemStatus.BLOCKED -> 0xFFF44336.toInt()     // Red
        ItemStatus.EXPLORING -> 0xFFFF9800.toInt()   // Orange
    }
}

/**
 * Represents a screen discovered during exploration
 *
 * @property hash Unique screen hash
 * @property activityName Activity class name
 * @property packageName Target app package
 * @property parentScreenHash Screen navigated from (if any)
 * @property itemCount Number of items on this screen
 * @property clickedCount Number of items clicked
 * @property blockedCount Number of items blocked
 * @property timestamp When screen was first discovered
 */
data class ExplorationScreen(
    val hash: String,
    val activityName: String,
    val packageName: String,
    val parentScreenHash: String?,
    var itemCount: Int = 0,
    var clickedCount: Int = 0,
    var blockedCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Short activity name (class name only)
     */
    fun shortName(): String = activityName.substringAfterLast('.')

    /**
     * Completion percentage for this screen
     */
    fun completionPercent(): Int {
        val clickable = itemCount - blockedCount
        return if (clickable > 0) {
            ((clickedCount.toFloat() / clickable) * 100).toInt()
        } else {
            100  // All blocked = 100% complete
        }
    }
}

/**
 * Overall exploration statistics
 *
 * @property totalItems Total items discovered
 * @property clickedItems Items clicked
 * @property blockedItems Items blocked
 * @property discoveredItems Items discovered but not clicked
 * @property totalScreens Total screens discovered
 * @property completionPercent Overall completion percentage
 */
data class ExplorationSummary(
    val totalItems: Int = 0,
    val clickedItems: Int = 0,
    val blockedItems: Int = 0,
    val discoveredItems: Int = 0,
    val totalScreens: Int = 0,
    val completionPercent: Int = 0
) {
    companion object {
        fun calculate(items: List<ExplorationItem>, screens: List<ExplorationScreen>): ExplorationSummary {
            val clicked = items.count { it.status == ItemStatus.CLICKED }
            val blocked = items.count { it.status == ItemStatus.BLOCKED }
            val discovered = items.count { it.status == ItemStatus.DISCOVERED }
            val total = items.size
            val clickable = total - blocked

            val completion = if (clickable > 0) {
                ((clicked.toFloat() / clickable) * 100).toInt()
            } else {
                100
            }

            return ExplorationSummary(
                totalItems = total,
                clickedItems = clicked,
                blockedItems = blocked,
                discoveredItems = discovered,
                totalScreens = screens.size,
                completionPercent = completion
            )
        }
    }
}

/**
 * Display mode for the overlay
 */
enum class OverlayDisplayMode {
    /** Show all items in a flat list */
    ALL_ITEMS,
    /** Group items by screen */
    BY_SCREEN,
    /** Show only clicked items */
    CLICKED_ONLY,
    /** Show only blocked items */
    BLOCKED_ONLY,
    /** Show summary statistics */
    SUMMARY
}

/**
 * Filter options for items
 */
data class ItemFilter(
    val showClicked: Boolean = true,
    val showDiscovered: Boolean = true,
    val showBlocked: Boolean = true,
    val screenHashFilter: String? = null  // Filter to specific screen
)
