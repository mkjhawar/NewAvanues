/**
 * DebugOverlayManager.kt - Manages debug overlay state for exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Manages debug overlay state and metrics display during exploration.
 */
package com.augmentalis.voiceoscore.learnapp.ui

/**
 * Debug Overlay Manager
 *
 * Manages debug overlay state for exploration sessions.
 * Tracks metrics and provides data for debug displays.
 */
class DebugOverlayManager {
    private var screensExplored = 0
    private var elementsFound = 0
    private var commandsGenerated = 0
    private var errorsEncountered = 0
    private var startTime = 0L
    private var isEnabled = false
    private val elementTracking = mutableMapOf<String, MutableMap<String, Boolean>>()
    private val navigationTracking = mutableListOf<Triple<String, String, String>>()

    /**
     * Start tracking a new exploration session
     */
    fun startSession() {
        startTime = System.currentTimeMillis()
        reset()
    }

    /**
     * Reset all metrics
     */
    fun reset() {
        screensExplored = 0
        elementsFound = 0
        commandsGenerated = 0
        errorsEncountered = 0
    }

    /**
     * Increment screens explored
     */
    fun incrementScreens() {
        screensExplored++
    }

    /**
     * Add elements found
     */
    fun addElements(count: Int) {
        elementsFound += count
    }

    /**
     * Add commands generated
     */
    fun addCommands(count: Int) {
        commandsGenerated += count
    }

    /**
     * Record an error
     */
    fun recordError() {
        errorsEncountered++
    }

    /**
     * Get debug summary
     */
    fun getSummary(): DebugSummary {
        val elapsed = if (startTime > 0) System.currentTimeMillis() - startTime else 0L
        return DebugSummary(
            screensExplored = screensExplored,
            elementsFound = elementsFound,
            commandsGenerated = commandsGenerated,
            errorsEncountered = errorsEncountered,
            elapsedTimeMs = elapsed
        )
    }

    /**
     * Get formatted debug string
     */
    fun getDebugString(): String {
        val summary = getSummary()
        return """
            |Screens: ${summary.screensExplored}
            |Elements: ${summary.elementsFound}
            |Commands: ${summary.commandsGenerated}
            |Errors: ${summary.errorsEncountered}
            |Time: ${summary.elapsedTimeMs / 1000}s
        """.trimMargin()
    }

    /**
     * Set enabled state
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * Check if debug overlay is enabled
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * Called when a screen is explored
     */
    fun onScreenExplored(
        elements: List<com.augmentalis.voiceoscore.learnapp.models.ElementInfo>,
        screenHash: String,
        activityName: String,
        packageName: String,
        parentScreenHash: String?
    ) {
        incrementScreens()
        addElements(elements.size)

        // Initialize tracking for this screen
        if (!elementTracking.containsKey(screenHash)) {
            elementTracking[screenHash] = mutableMapOf()
            elements.forEach { element ->
                elementTracking[screenHash]!![element.stableId()] = false
            }
        }
    }

    /**
     * Record navigation from element click
     */
    fun recordNavigation(stableId: String, fromScreenHash: String, toScreenHash: String) {
        navigationTracking.add(Triple(stableId, fromScreenHash, toScreenHash))
    }

    /**
     * Mark an element as clicked
     */
    fun markItemClicked(stableId: String, screenHash: String, vuid: String?) {
        elementTracking[screenHash]?.set(stableId, true)
    }

    /**
     * Mark an element as blocked
     */
    fun markItemBlocked(stableId: String, screenHash: String, reason: String) {
        // Track blocked items (could add separate tracking if needed)
        recordError()
    }
}

/**
 * Debug Summary
 */
data class DebugSummary(
    val screensExplored: Int,
    val elementsFound: Int,
    val commandsGenerated: Int,
    val errorsEncountered: Int,
    val elapsedTimeMs: Long
)
