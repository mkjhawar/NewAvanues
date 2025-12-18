/**
 * ElementClickTracker.kt - Tracks element clicks during exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Tracks which elements have been clicked during app exploration.
 * Maintains per-screen progress for exploration completeness tracking.
 */
package com.augmentalis.voiceoscore.learnapp.tracking

import java.util.concurrent.ConcurrentHashMap

/**
 * Element Click Tracker
 *
 * Tracks element interactions during exploration sessions.
 * Used by ExplorationEngine to determine which elements need exploration.
 */
class ElementClickTracker {

    // Screen hash -> Set of clicked element UUIDs
    private val clickedElements = ConcurrentHashMap<String, MutableSet<String>>()

    // Screen hash -> Total element count
    private val screenElementCounts = ConcurrentHashMap<String, Int>()

    // Total stats
    private var totalElementsRegistered = 0
    private var totalElementsClicked = 0

    /**
     * Clear all tracking data
     */
    fun clear() {
        clickedElements.clear()
        screenElementCounts.clear()
        totalElementsRegistered = 0
        totalElementsClicked = 0
    }

    /**
     * Register a screen with its clickable elements
     *
     * @param screenHash Unique hash identifying the screen
     * @param elementUuids List of UUIDs for clickable elements on the screen
     */
    fun registerScreen(screenHash: String, elementUuids: List<String>) {
        if (!clickedElements.containsKey(screenHash)) {
            clickedElements[screenHash] = ConcurrentHashMap.newKeySet()
            screenElementCounts[screenHash] = elementUuids.size
            totalElementsRegistered += elementUuids.size
        }
    }

    /**
     * Get tracking statistics
     *
     * @return Current tracking statistics
     */
    fun getStats(): TrackerStats {
        return TrackerStats(
            totalScreens = clickedElements.size,
            totalElements = totalElementsRegistered,
            elementsClicked = totalElementsClicked,
            screensCompleted = countCompletedScreens(),
            progressPercent = calculateOverallProgress()
        )
    }

    /**
     * Get progress for a specific screen
     *
     * @param screenHash Screen hash to check
     * @return ScreenProgress or null if screen not registered
     */
    fun getScreenProgress(screenHash: String): ScreenProgress? {
        val clicked = clickedElements[screenHash] ?: return null
        val total = screenElementCounts[screenHash] ?: return null

        return ScreenProgress(
            screenHash = screenHash,
            totalElements = total,
            clickedElements = clicked.size,
            isComplete = clicked.size >= total,
            progressPercent = if (total > 0) (clicked.size.toFloat() / total) * 100f else 100f
        )
    }

    /**
     * Mark an element as clicked
     *
     * @param screenHash Screen containing the element
     * @param elementUuid UUID of the clicked element
     */
    fun markElementClicked(screenHash: String, elementUuid: String) {
        val screenClicks = clickedElements[screenHash]
        if (screenClicks != null && !screenClicks.contains(elementUuid)) {
            screenClicks.add(elementUuid)
            totalElementsClicked++
        }
    }

    /**
     * Check if an element was already clicked
     *
     * @param screenHash Screen containing the element
     * @param elementUuid UUID of the element to check
     * @return true if element was clicked, false otherwise
     */
    fun wasElementClicked(screenHash: String, elementUuid: String): Boolean {
        return clickedElements[screenHash]?.contains(elementUuid) ?: false
    }

    /**
     * Get unclicked elements for a screen
     *
     * @param screenHash Screen hash
     * @param allElementUuids All element UUIDs on the screen
     * @return List of UUIDs that haven't been clicked
     */
    fun getUnclickedElements(screenHash: String, allElementUuids: List<String>): List<String> {
        val clicked = clickedElements[screenHash] ?: return allElementUuids
        return allElementUuids.filter { it !in clicked }
    }

    /**
     * Check if a screen is fully explored
     *
     * @param screenHash Screen hash to check
     * @return true if all elements have been clicked
     */
    fun isScreenComplete(screenHash: String): Boolean {
        val clicked = clickedElements[screenHash]?.size ?: 0
        val total = screenElementCounts[screenHash] ?: 0
        return total > 0 && clicked >= total
    }

    /**
     * Get all registered screen hashes
     */
    fun getRegisteredScreens(): Set<String> {
        return clickedElements.keys.toSet()
    }

    /**
     * Get count of completed screens
     */
    private fun countCompletedScreens(): Int {
        return clickedElements.keys.count { isScreenComplete(it) }
    }

    /**
     * Calculate overall exploration progress
     */
    private fun calculateOverallProgress(): Float {
        if (totalElementsRegistered == 0) return 0f
        return (totalElementsClicked.toFloat() / totalElementsRegistered) * 100f
    }
}

/**
 * Screen Progress
 *
 * Progress information for a single screen.
 */
data class ScreenProgress(
    val screenHash: String,
    val totalElements: Int,
    val clickedElements: Int,
    val isComplete: Boolean,
    val progressPercent: Float
)

/**
 * Tracker Stats
 *
 * Overall tracking statistics.
 */
data class TrackerStats(
    val totalScreens: Int,
    val totalElements: Int,
    val elementsClicked: Int,
    val screensCompleted: Int,
    val progressPercent: Float
) {
    /** Alias for progressPercent for backward compatibility */
    val overallCompleteness: Float get() = progressPercent
}
