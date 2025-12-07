package com.augmentalis.voiceoscore.learnapp.tracking

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks per-element click progress during LearnApp exploration.
 *
 * This is the core solution for Issue #1: Premature Learning Completion.
 *
 * ## Problem This Solves
 *
 * ### Issue #1: Apps Report Wrong Counts (Premature Completion)
 * **Before:** Screens marked "visited" after **first element clicked**
 * - RealWear Test App: Reports "4 screens, 13 elements" (actually 1 screen, 2 clickable items)
 * - Teams: Reports "7 screens, 496 elements" (only captures 3-4 screens)
 *
 * **Root Cause:**
 * - No tracking of which specific elements have been clicked
 * - Screen marked "complete" after visiting once (not after clicking all elements)
 * - Remaining clickable elements on screen never tested
 * - System moves to next screen too early
 *
 * ### Solution: Per-Element Click Tracking
 * Instead of tracking "screen visited" (boolean), track "which elements clicked" (set of UUIDs):
 * ```
 * Screen A: [Element1, Element2, Element3]
 *           ↓
 * Clicked: {Element1} → 33% complete
 *           ↓
 * Clicked: {Element1, Element2} → 67% complete
 *           ↓
 * Clicked: {Element1, Element2, Element3} → 100% complete ✅
 * ```
 *
 * ## Architecture
 *
 * ```
 * ExplorationEngine
 *       ↓
 * Discovers Screen A
 *       ↓
 * registerScreen(screenHash, [Element1, Element2, Element3])
 *       ↓
 * For each element:
 *   ├─ wasElementClicked(screenHash, elementUuid) ? → Skip if true
 *   ├─ clickElement()
 *   └─ markElementClicked(screenHash, elementUuid)
 *       ↓
 * getScreenProgress(screenHash)
 *   → completionPercent = 100%
 *   → isFullyExplored = true
 *       ↓
 * Move to next screen
 * ```
 *
 * ## Usage Example
 *
 * ```kotlin
 * val tracker = ElementClickTracker()
 *
 * // 1. Register screen with clickable elements
 * val elements = listOf(
 *     ElementInfo(uuid = "elem-1", alias = "Submit Button"),
 *     ElementInfo(uuid = "elem-2", alias = "Cancel Button"),
 *     ElementInfo(uuid = "elem-3", alias = "Options Menu")
 * )
 * tracker.registerScreen("screen-abc123", elements)
 *
 * // 2. Check if element was already clicked
 * for (element in elements) {
 *     if (tracker.wasElementClicked("screen-abc123", element.uuid)) {
 *         Log.d(TAG, "Already clicked: ${element.alias}, skipping")
 *         continue
 *     }
 *
 *     // 3. Click element
 *     clickElement(element)
 *
 *     // 4. Mark as clicked
 *     tracker.markElementClicked("screen-abc123", element.uuid)
 * }
 *
 * // 5. Check completion
 * val progress = tracker.getScreenProgress("screen-abc123")
 * if (progress.isFullyExplored) {
 *     Log.i(TAG, "Screen fully explored (${progress.completionPercent}%)")
 * }
 *
 * // 6. Get overall stats
 * val stats = tracker.getStats()
 * Log.i(TAG, "Overall completion: ${stats.overallCompleteness}%")
 * Log.i(TAG, "Screens: ${stats.totalScreens}, Elements: ${stats.totalElements}")
 * ```
 *
 * ## Thread Safety
 * This class is thread-safe using ConcurrentHashMap. Multiple threads can safely:
 * - Register screens
 * - Mark elements clicked
 * - Query progress
 *
 * However, ExplorationEngine should still use a single exploration thread to avoid
 * race conditions in UI interaction.
 *
 * @see com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine Primary consumer
 */
class ElementClickTracker {

    private val TAG = "ElementClickTracker"

    /**
     * Progress tracking for a single screen.
     *
     * @property screenHash MD5 hash identifying the screen (packageName + className + windowId)
     * @property totalClickableElements Total number of clickable elements discovered on this screen
     * @property clickedElementUuids Set of element UUIDs that have been clicked
     * @property completionPercent Percentage of elements clicked (0.0 to 100.0)
     * @property isFullyExplored True if all clickable elements have been tested
     */
    data class ScreenProgress(
        val screenHash: String,
        val totalClickableElements: Int,
        val clickedElementUuids: MutableSet<String> = mutableSetOf(),
        val completionPercent: Float = 0f,
        val isFullyExplored: Boolean = false
    ) {
        /**
         * Calculates the number of elements remaining to be clicked.
         */
        fun remainingElements(): Int {
            return totalClickableElements - clickedElementUuids.size
        }

        /**
         * Returns a human-readable summary of this screen's progress.
         */
        fun toLogString(): String {
            return "ScreenProgress(hash=${screenHash.take(8)}..., " +
                    "clicked=${clickedElementUuids.size}/$totalClickableElements, " +
                    "completion=${"%.1f".format(completionPercent)}%, " +
                    "fullyExplored=$isFullyExplored)"
        }
    }

    /**
     * Overall exploration statistics across all screens.
     *
     * @property totalScreens Total number of screens registered
     * @property fullyExploredScreens Number of screens that are 100% complete
     * @property partiallyExploredScreens Number of screens that are 1-99% complete
     * @property unexploredScreens Number of screens with 0% completion
     * @property totalElements Total clickable elements across all screens
     * @property clickedElements Total elements clicked across all screens
     * @property overallCompleteness Overall completion percentage (0.0 to 100.0)
     */
    data class ExplorationStats(
        val totalScreens: Int,
        val fullyExploredScreens: Int,
        val partiallyExploredScreens: Int,
        val unexploredScreens: Int,
        val totalElements: Int,
        val clickedElements: Int,
        val overallCompleteness: Float
    ) {
        /**
         * Returns a human-readable summary of overall progress.
         */
        fun toLogString(): String {
            return """
                |Exploration Statistics:
                |  Screens: $totalScreens total
                |    - Fully explored: $fullyExploredScreens (100%)
                |    - Partially explored: $partiallyExploredScreens (1-99%)
                |    - Unexplored: $unexploredScreens (0%)
                |  Elements: $clickedElements/$totalElements clicked
                |  Overall Completeness: ${"%.1f".format(overallCompleteness)}%
            """.trimMargin()
        }
    }

    /**
     * Map of screen hash → progress tracking.
     * Thread-safe via ConcurrentHashMap.
     */
    private val screenProgressMap = ConcurrentHashMap<String, ScreenProgress>()

    /**
     * Registers a new screen with its clickable elements.
     *
     * Call this when discovering a new screen. The tracker will initialize progress
     * tracking for all clickable elements on the screen.
     *
     * ## Behavior
     * - If screen already registered: Updates total element count (in case screen changed)
     * - If screen is new: Creates new ScreenProgress entry
     * - Non-clickable elements are ignored (only track clickable/interactive elements)
     *
     * ## What Counts as "Clickable"?
     * Elements with classification:
     * - SafeClickable (buttons, links, navigation items)
     * - Dangerous (delete buttons, logout, purchase)
     * - EditText (text input fields - "clicking" = focusing)
     * - LoginField (username/password fields)
     *
     * Elements to EXCLUDE:
     * - NonClickable (labels, images, decorative UI)
     * - Disabled (grayed out buttons)
     * - Dynamic content (live updating elements)
     *
     * @param screenHash MD5 hash identifying the screen
     * @param clickableElementUuids List of UUIDs for all clickable elements on screen
     */
    fun registerScreen(screenHash: String, clickableElementUuids: List<String>) {
        if (clickableElementUuids.isEmpty()) {
            Log.v(TAG, "📋 Screen $screenHash has no clickable elements, skipping registration")
            return
        }

        val existing = screenProgressMap[screenHash]
        if (existing != null) {
            // Screen already registered - update element count in case it changed
            Log.d(TAG, "📝 Updating screen $screenHash: ${existing.totalClickableElements} → ${clickableElementUuids.size} elements")
            val updated = existing.copy(totalClickableElements = clickableElementUuids.size)
            screenProgressMap[screenHash] = recalculateProgress(updated)
        } else {
            // New screen
            val progress = ScreenProgress(
                screenHash = screenHash,
                totalClickableElements = clickableElementUuids.size
            )
            screenProgressMap[screenHash] = progress
            Log.i(TAG, "✅ Registered new screen: $screenHash with ${clickableElementUuids.size} clickable elements")
        }
    }

    /**
     * Checks if a specific element has been clicked.
     *
     * Use this before attempting to click an element to avoid duplicate clicks.
     *
     * @param screenHash Screen hash where element exists
     * @param elementUuid UUID of the element to check
     * @return true if element was already clicked, false otherwise
     */
    fun wasElementClicked(screenHash: String, elementUuid: String): Boolean {
        val progress = screenProgressMap[screenHash] ?: return false
        return progress.clickedElementUuids.contains(elementUuid)
    }

    /**
     * Marks an element as clicked.
     *
     * Call this immediately after successfully clicking an element.
     * Updates progress percentages automatically.
     *
     * ## Thread Safety
     * This method is thread-safe. Multiple threads can mark different elements clicked
     * without data corruption.
     *
     * @param screenHash Screen hash where element exists
     * @param elementUuid UUID of the element that was clicked
     */
    fun markElementClicked(screenHash: String, elementUuid: String) {
        val progress = screenProgressMap[screenHash]
        if (progress == null) {
            Log.w(TAG, "⚠️ Cannot mark element clicked - screen not registered: $screenHash")
            return
        }

        // Add to clicked set
        val wasNew = progress.clickedElementUuids.add(elementUuid)
        if (!wasNew) {
            Log.v(TAG, "⏭️ Element already marked as clicked: $elementUuid")
            return
        }

        // Recalculate progress
        val updated = recalculateProgress(progress)
        screenProgressMap[screenHash] = updated

        Log.d(TAG, "✅ Marked element clicked: ${elementUuid.take(8)}... " +
                "(${updated.clickedElementUuids.size}/${updated.totalClickableElements}, " +
                "${"%.1f".format(updated.completionPercent)}%)")

        // Log milestone completions
        if (updated.isFullyExplored) {
            Log.i(TAG, "🎉 Screen fully explored: $screenHash (${updated.totalClickableElements} elements)")
        }
    }

    /**
     * Gets progress for a specific screen.
     *
     * @param screenHash Screen hash to query
     * @return ScreenProgress object, or null if screen not registered
     */
    fun getScreenProgress(screenHash: String): ScreenProgress? {
        return screenProgressMap[screenHash]
    }

    /**
     * Gets overall exploration statistics across all screens.
     *
     * Use this to determine if the app is fully learned and ready to mark
     * as complete in the database.
     *
     * ## When is App "Fully Learned"?
     * Recommended threshold: overallCompleteness >= 95%
     *
     * Why 95% and not 100%?
     * - Some elements may be unreachable (disabled, race conditions)
     * - Some screens may be transient (splash screens, loading screens)
     * - 95% indicates comprehensive coverage without perfection requirements
     *
     * @return ExplorationStats with overall progress metrics
     */
    fun getStats(): ExplorationStats {
        if (screenProgressMap.isEmpty()) {
            return ExplorationStats(
                totalScreens = 0,
                fullyExploredScreens = 0,
                partiallyExploredScreens = 0,
                unexploredScreens = 0,
                totalElements = 0,
                clickedElements = 0,
                overallCompleteness = 0f
            )
        }

        var fullyExplored = 0
        var partiallyExplored = 0
        var unexplored = 0
        var totalElements = 0
        var clickedElements = 0

        for (progress in screenProgressMap.values) {
            totalElements += progress.totalClickableElements
            clickedElements += progress.clickedElementUuids.size

            when {
                progress.isFullyExplored -> fullyExplored++
                progress.clickedElementUuids.isEmpty() -> unexplored++
                else -> partiallyExplored++
            }
        }

        val overallCompleteness = if (totalElements > 0) {
            (clickedElements.toFloat() / totalElements.toFloat()) * 100f
        } else {
            0f
        }

        return ExplorationStats(
            totalScreens = screenProgressMap.size,
            fullyExploredScreens = fullyExplored,
            partiallyExploredScreens = partiallyExplored,
            unexploredScreens = unexplored,
            totalElements = totalElements,
            clickedElements = clickedElements,
            overallCompleteness = overallCompleteness
        )
    }

    /**
     * Gets all registered screens.
     *
     * Useful for debugging and understanding what screens were discovered.
     *
     * @return List of screen hashes
     */
    fun getRegisteredScreens(): List<String> {
        return screenProgressMap.keys.toList()
    }

    /**
     * Clears all tracking data.
     *
     * Use this when starting a new app exploration session.
     * Should be called at the beginning of ExplorationEngine.startExploration().
     *
     * @return Number of screens that were cleared
     */
    fun clear(): Int {
        val count = screenProgressMap.size
        screenProgressMap.clear()
        Log.d(TAG, "🔄 Cleared tracking data ($count screens)")
        return count
    }

    /**
     * Recalculates completion percentage and fully-explored status.
     *
     * Internal helper method called after any progress update.
     *
     * @param progress ScreenProgress to recalculate
     * @return Updated ScreenProgress with new completion metrics
     */
    private fun recalculateProgress(progress: ScreenProgress): ScreenProgress {
        val clicked = progress.clickedElementUuids.size
        val total = progress.totalClickableElements

        val completionPercent = if (total > 0) {
            (clicked.toFloat() / total.toFloat()) * 100f
        } else {
            0f
        }

        val isFullyExplored = (clicked >= total) && (total > 0)

        return progress.copy(
            completionPercent = completionPercent,
            isFullyExplored = isFullyExplored
        )
    }

    /**
     * Gets diagnostic information for debugging.
     *
     * @return Map containing tracker state and statistics
     */
    fun getDiagnostics(): Map<String, Any> {
        val stats = getStats()
        return mapOf(
            "totalScreens" to screenProgressMap.size,
            "totalElements" to stats.totalElements,
            "clickedElements" to stats.clickedElements,
            "overallCompleteness" to stats.overallCompleteness,
            "fullyExploredScreens" to stats.fullyExploredScreens,
            "partiallyExploredScreens" to stats.partiallyExploredScreens,
            "unexploredScreens" to stats.unexploredScreens,
            "screenDetails" to screenProgressMap.values.map { it.toLogString() }
        )
    }
}
