/**
 * ExplorationItemTracker.kt - Tracks ALL items during exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-08
 *
 * Central tracker for all items scanned during exploration.
 * Provides:
 * - Item discovery tracking (VUID, screen, status)
 * - Click tracking (when clicked, where navigated)
 * - Block tracking (reason for blocking)
 * - Screen tracking (all screens discovered)
 * - Statistics and filtering
 */
package com.augmentalis.voiceoscore.learnapp.debugging

import android.util.Log
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks all exploration items and screens
 *
 * Thread-safe implementation using ConcurrentHashMap.
 * Items are keyed by their stableId for quick lookup.
 */
class ExplorationItemTracker {
    companion object {
        private const val TAG = "ExplorationItemTracker"
    }

    // Item storage: stableId -> ExplorationItem
    private val items = ConcurrentHashMap<String, ExplorationItem>()

    // Screen storage: screenHash -> ExplorationScreen
    private val screens = ConcurrentHashMap<String, ExplorationScreen>()

    // Current screen being explored
    private var currentScreenHash: String? = null

    // Callbacks for UI updates
    private var onItemsChangedListener: ((List<ExplorationItem>, ExplorationSummary) -> Unit)? = null

    /**
     * Set listener for item changes
     */
    fun setOnItemsChangedListener(listener: ((List<ExplorationItem>, ExplorationSummary) -> Unit)?) {
        onItemsChangedListener = listener
    }

    /**
     * Register a new screen
     *
     * @param screenHash Unique screen hash
     * @param activityName Activity class name
     * @param packageName Target app package
     * @param parentScreenHash Parent screen (if navigated from)
     */
    fun registerScreen(
        screenHash: String,
        activityName: String,
        packageName: String,
        parentScreenHash: String? = null
    ) {
        if (!screens.containsKey(screenHash)) {
            screens[screenHash] = ExplorationScreen(
                hash = screenHash,
                activityName = activityName,
                packageName = packageName,
                parentScreenHash = parentScreenHash
            )
            Log.i(TAG, "📱 Registered screen: ${activityName.substringAfterLast('.')} (${screenHash.take(8)}...)")
        }
        currentScreenHash = screenHash
    }

    /**
     * Register discovered items on a screen
     *
     * @param elements List of ElementInfo from exploration
     * @param screenHash Screen where items were found
     * @param screenName Activity name
     */
    fun registerItems(
        elements: List<ElementInfo>,
        screenHash: String,
        screenName: String
    ) {
        var newCount = 0
        var blockedCount = 0

        elements.forEach { element ->
            val stableId = element.stableId()
            val itemKey = "${screenHash}:${stableId}"

            if (!items.containsKey(itemKey)) {
                val item = ExplorationItem(
                    id = itemKey,
                    vuid = element.uuid,
                    displayName = element.getDisplayName(),
                    className = element.className.substringAfterLast('.'),
                    resourceId = element.resourceId.takeIf { it.isNotBlank() },
                    screenHash = screenHash,
                    screenName = screenName
                )
                items[itemKey] = item
                newCount++
            }
        }

        // Update screen stats
        screens[screenHash]?.let { screen ->
            screen.itemCount = items.values.count { it.screenHash == screenHash }
            screen.blockedCount = items.values.count { it.screenHash == screenHash && it.status == ItemStatus.BLOCKED }
            screen.clickedCount = items.values.count { it.screenHash == screenHash && it.status == ItemStatus.CLICKED }
        }

        if (newCount > 0) {
            Log.i(TAG, "📝 Registered $newCount new items on ${screenName.substringAfterLast('.')}")
            notifyListeners()
        }
    }

    /**
     * Mark an item as blocked
     *
     * @param stableId Element stable ID
     * @param screenHash Screen where item was found
     * @param reason Blocking reason
     */
    fun markBlocked(stableId: String, screenHash: String, reason: String) {
        val itemKey = "${screenHash}:${stableId}"
        items[itemKey]?.let { item ->
            items[itemKey] = item.copy(status = ItemStatus.BLOCKED, blockReason = reason)

            // Update screen stats
            screens[screenHash]?.let { screen ->
                screen.blockedCount = items.values.count { it.screenHash == screenHash && it.status == ItemStatus.BLOCKED }
            }

            Log.i(TAG, "🚫 Blocked: ${item.shortName()} - $reason")
            notifyListeners()
        }
    }

    /**
     * Mark items as blocked in bulk
     */
    fun markBlockedBulk(blockedItems: List<Pair<String, String>>, screenHash: String) {
        blockedItems.forEach { (stableId, reason) ->
            val itemKey = "${screenHash}:${stableId}"
            items[itemKey]?.let { item ->
                items[itemKey] = item.copy(status = ItemStatus.BLOCKED, blockReason = reason)
            }
        }

        // Update screen stats once
        screens[screenHash]?.let { screen ->
            screen.blockedCount = items.values.count { it.screenHash == screenHash && it.status == ItemStatus.BLOCKED }
        }

        if (blockedItems.isNotEmpty()) {
            Log.i(TAG, "🚫 Blocked ${blockedItems.size} items on screen ${screenHash.take(8)}")
            notifyListeners()
        }
    }

    /**
     * Mark an item as clicked
     *
     * @param stableId Element stable ID
     * @param screenHash Screen where item was clicked
     * @param navigatedTo Screen navigated to (if navigation occurred)
     */
    fun markClicked(stableId: String, screenHash: String, navigatedTo: String? = null) {
        val itemKey = "${screenHash}:${stableId}"
        items[itemKey]?.let { item ->
            items[itemKey] = item.copy(
                status = ItemStatus.CLICKED,
                clickedAt = System.currentTimeMillis(),
                navigatedTo = navigatedTo
            )

            // Update screen stats
            screens[screenHash]?.let { screen ->
                screen.clickedCount = items.values.count { it.screenHash == screenHash && it.status == ItemStatus.CLICKED }
            }

            Log.d(TAG, "✅ Clicked: ${item.shortName()}" + (navigatedTo?.let { " → ${it.take(8)}..." } ?: ""))
            notifyListeners()
        }
    }

    /**
     * Mark an item as currently being explored
     */
    fun markExploring(stableId: String, screenHash: String) {
        val itemKey = "${screenHash}:${stableId}"
        items[itemKey]?.let { item ->
            items[itemKey] = item.copy(status = ItemStatus.EXPLORING)
            notifyListeners()
        }
    }

    /**
     * Get all items (thread-safe copy)
     */
    fun getAllItems(): List<ExplorationItem> = items.values.toList().sortedByDescending { it.timestamp }

    /**
     * Get items filtered by status
     */
    fun getItemsByStatus(status: ItemStatus): List<ExplorationItem> =
        items.values.filter { it.status == status }.sortedByDescending { it.timestamp }

    /**
     * Get items for a specific screen
     */
    fun getItemsForScreen(screenHash: String): List<ExplorationItem> =
        items.values.filter { it.screenHash == screenHash }.sortedByDescending { it.timestamp }

    /**
     * Get all screens
     */
    fun getAllScreens(): List<ExplorationScreen> = screens.values.toList().sortedByDescending { it.timestamp }

    /**
     * Get screen by hash
     */
    fun getScreen(screenHash: String): ExplorationScreen? = screens[screenHash]

    /**
     * Get items grouped by screen
     */
    fun getItemsGroupedByScreen(): Map<ExplorationScreen, List<ExplorationItem>> {
        return screens.values.associateWith { screen ->
            items.values.filter { it.screenHash == screen.hash }.sortedByDescending { it.timestamp }
        }
    }

    /**
     * Get exploration summary
     */
    fun getSummary(): ExplorationSummary {
        return ExplorationSummary.calculate(items.values.toList(), screens.values.toList())
    }

    /**
     * Clear all tracking data
     */
    fun clear() {
        items.clear()
        screens.clear()
        currentScreenHash = null
        Log.i(TAG, "🔄 Tracker cleared")
        notifyListeners()
    }

    /**
     * Get count statistics
     */
    fun getCounts(): Triple<Int, Int, Int> {
        val total = items.size
        val clicked = items.values.count { it.status == ItemStatus.CLICKED }
        val blocked = items.values.count { it.status == ItemStatus.BLOCKED }
        return Triple(total, clicked, blocked)
    }

    /**
     * Notify listeners of changes
     */
    private fun notifyListeners() {
        onItemsChangedListener?.invoke(getAllItems(), getSummary())
    }

    /**
     * Export items to markdown format
     */
    fun exportToMarkdown(): String {
        val sb = StringBuilder()
        val summary = getSummary()

        sb.appendLine("# Exploration Debug Report")
        sb.appendLine()
        sb.appendLine("**Generated:** ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
        sb.appendLine()

        // Summary
        sb.appendLine("## Summary")
        sb.appendLine()
        sb.appendLine("| Metric | Value |")
        sb.appendLine("|--------|-------|")
        sb.appendLine("| Total Items | ${summary.totalItems} |")
        sb.appendLine("| Clicked | ${summary.clickedItems} |")
        sb.appendLine("| Blocked | ${summary.blockedItems} |")
        sb.appendLine("| Not Clicked | ${summary.discoveredItems} |")
        sb.appendLine("| Total Screens | ${summary.totalScreens} |")
        sb.appendLine("| Completion | ${summary.completionPercent}% |")
        sb.appendLine()

        // Screens
        sb.appendLine("## Screens")
        sb.appendLine()
        screens.values.sortedByDescending { it.timestamp }.forEach { screen ->
            sb.appendLine("### ${screen.shortName()} (${screen.hash.take(8)})")
            sb.appendLine()
            sb.appendLine("- Items: ${screen.itemCount}")
            sb.appendLine("- Clicked: ${screen.clickedCount}")
            sb.appendLine("- Blocked: ${screen.blockedCount}")
            sb.appendLine("- Completion: ${screen.completionPercent()}%")
            sb.appendLine()

            // Items on this screen
            val screenItems = getItemsForScreen(screen.hash)
            if (screenItems.isNotEmpty()) {
                sb.appendLine("| Status | Name | VUID | Class |")
                sb.appendLine("|--------|------|------|-------|")
                screenItems.forEach { item ->
                    val status = item.statusEmoji()
                    val vuid = item.vuid?.take(12) ?: "—"
                    sb.appendLine("| $status | ${item.shortName()} | $vuid | ${item.className} |")
                }
                sb.appendLine()
            }
        }

        return sb.toString()
    }
}
