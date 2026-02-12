/**
 * OverlayNumberingExecutor.kt - Per-container numbering executor for overlay badges
 *
 * Implements the KMP NumbersOverlayExecutor interface with per-container scoping.
 * Lives at the app layer so it can access OverlayStateManager for rendering.
 *
 * Replaces the flat global avidToNumber map that was in OverlayStateManager with
 * per-container stable numbering. Each scroll container gets its own number sequence,
 * enabling multi-scroll-area screens to have independent numbering.
 *
 * Also centralizes screen transition logic (reset on app change, preserve on scroll)
 * that was previously scattered across DynamicCommandGenerator and OverlayStateManager.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import com.augmentalis.voiceoscore.NumbersOverlayExecutor
import com.augmentalis.voiceoscore.NumbersOverlayMode

class OverlayNumberingExecutor : NumbersOverlayExecutor {

    // Per-container numbering: containerAvid -> (avid -> assignedNumber)
    private val assignments = mutableMapOf<String, LinkedHashMap<String, Int>>()
    private val nextNumbers = mutableMapOf<String, Int>()

    // Screen transition tracking
    private var lastPackageName: String = ""
    private var lastIsTargetApp: Boolean = false

    // ===== Non-suspend methods for DynamicCommandGenerator =====

    /**
     * Handle screen context change. Resets numbering on app change,
     * resets on screen change for non-target apps, and resets on
     * major navigation within target apps.
     *
     * @param packageName Current app package name
     * @param isTargetApp Whether this app is in the TARGET_APPS list
     * @param isNewScreen Whether the screen hash changed
     * @param structuralChangeRatio 0.0-1.0 how much the top-level structure changed.
     *        Used to distinguish scroll (low ratio) from major navigation (high ratio)
     *        within target apps like Gmail (inbox → email detail).
     * @return true if numbering was reset
     */
    fun handleScreenContext(
        packageName: String,
        isTargetApp: Boolean,
        isNewScreen: Boolean,
        structuralChangeRatio: Float = 0f
    ): Boolean {
        val isAppChange = packageName != lastPackageName
        var didReset = false

        if (isAppChange) {
            lastPackageName = packageName
            clearAllAssignmentsInternal()
            didReset = true
        } else if (isNewScreen && !isTargetApp) {
            clearAllAssignmentsInternal()
            didReset = true
        } else if (isNewScreen && isTargetApp && structuralChangeRatio > MAJOR_NAVIGATION_THRESHOLD) {
            // Major navigation within a target app (e.g., Gmail inbox → email detail).
            // The structural change ratio is high because the screen layout is fundamentally
            // different, not just scrolled content. Reset numbering for the new screen.
            clearAllAssignmentsInternal()
            didReset = true
        }

        // Immediately clear stale overlay badges on screen/app transition.
        // Without this, old badges persist during the async element extraction gap
        // between transition detection and new overlay generation.
        if (didReset) {
            OverlayStateManager.clearOverlayItems()
        }

        lastIsTargetApp = isTargetApp
        return didReset
    }

    companion object {
        /** Threshold above which a screen change within a target app is considered
         *  major navigation (not scroll). Gmail inbox → email detail typically exceeds 0.8. */
        const val MAJOR_NAVIGATION_THRESHOLD = 0.6f
    }

    /**
     * Assign numbers to overlay items using per-container stable numbering.
     * Items with existing AVID->number mappings keep their numbers.
     * New items get the next available number.
     */
    fun assignNumbers(
        items: List<OverlayStateManager.NumberOverlayItem>,
        containerAvid: String? = null
    ): List<OverlayStateManager.NumberOverlayItem> {
        if (items.isEmpty()) return emptyList()

        val containerId = containerAvid ?: "root"
        val containerMap = assignments.getOrPut(containerId) { linkedMapOf() }

        val sorted = items.sortedWith(compareBy({ it.top }, { it.left }))
        val result = sorted.map { item ->
            val number = containerMap.getOrPut(item.avid) {
                val next = nextNumbers.getOrPut(containerId) { 1 }
                nextNumbers[containerId] = next + 1
                next
            }
            item.copy(number = number)
        }

        trimIfNeeded()
        return result
    }

    /**
     * Reset numbering for in-app navigation (activity/fragment transition).
     * Called from the app-level service when TYPE_WINDOW_STATE_CHANGED fires
     * within the same package. Clears all assignments so the next screen
     * starts numbering from 1.
     */
    fun resetForNavigation() {
        clearAllAssignmentsInternal()
    }

    private fun clearAllAssignmentsInternal() {
        assignments.clear()
        nextNumbers.clear()
    }

    private fun trimIfNeeded() {
        for ((_, containerMap) in assignments) {
            if (containerMap.size > 500) {
                val removeCount = containerMap.size - 500
                val iterator = containerMap.entries.iterator()
                repeat(removeCount) {
                    if (iterator.hasNext()) { iterator.next(); iterator.remove() }
                }
            }
        }
    }

    // ===== NumbersOverlayExecutor interface (suspend, for voice commands) =====

    override suspend fun setNumbersMode(mode: NumbersOverlayMode): Boolean {
        val mapped = when (mode) {
            NumbersOverlayMode.ON -> OverlayStateManager.NumbersOverlayMode.ON
            NumbersOverlayMode.OFF -> OverlayStateManager.NumbersOverlayMode.OFF
            NumbersOverlayMode.AUTO -> OverlayStateManager.NumbersOverlayMode.AUTO
        }
        OverlayStateManager.setNumbersOverlayMode(mapped)
        return true
    }

    override suspend fun getCurrentMode(): NumbersOverlayMode {
        return when (OverlayStateManager.numbersOverlayMode.value) {
            OverlayStateManager.NumbersOverlayMode.ON -> NumbersOverlayMode.ON
            OverlayStateManager.NumbersOverlayMode.OFF -> NumbersOverlayMode.OFF
            OverlayStateManager.NumbersOverlayMode.AUTO -> NumbersOverlayMode.AUTO
        }
    }

    override suspend fun getOrAssignNumber(avid: String, scrollContainerAvid: String?): Int {
        val containerId = scrollContainerAvid ?: "root"
        val containerMap = assignments.getOrPut(containerId) { linkedMapOf() }
        return containerMap.getOrPut(avid) {
            val next = nextNumbers.getOrPut(containerId) { 1 }
            nextNumbers[containerId] = next + 1
            next
        }
    }

    override suspend fun getAssignedNumber(avid: String, scrollContainerAvid: String?): Int? {
        return assignments[scrollContainerAvid ?: "root"]?.get(avid)
    }

    override suspend fun getAssignmentsForContainer(scrollContainerAvid: String?): Map<String, Int> {
        return assignments[scrollContainerAvid ?: "root"]?.toMap() ?: emptyMap()
    }

    override suspend fun clearNumberAssignments() {
        clearAllAssignmentsInternal()
        OverlayStateManager.clearOverlayItems()
    }

    override suspend fun clearContainerAssignments(scrollContainerAvid: String) {
        assignments.remove(scrollContainerAvid)
        nextNumbers.remove(scrollContainerAvid)
    }

    override suspend fun onScreenTransition(newScreenId: String, isMajorTransition: Boolean) {
        if (isMajorTransition) {
            clearAllAssignmentsInternal()
            OverlayStateManager.clearOverlayItems()
        }
    }
}
