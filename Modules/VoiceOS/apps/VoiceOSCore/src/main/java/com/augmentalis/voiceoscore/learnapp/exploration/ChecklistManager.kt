/**
 * ChecklistManager.kt - Manages exploration checklists
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Manages exploration checklists for tracking app learning progress.
 * Supports real-time tracking and export to file.
 */
package com.augmentalis.voiceoscore.learnapp.exploration

import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Checklist Manager
 *
 * Manages exploration checklists for real-time progress tracking.
 * Used by ExplorationEngine to track which elements have been explored.
 */
class ChecklistManager {

    companion object {
        private const val TAG = "ChecklistManager"
    }

    // Current checklist state
    private var currentPackageName: String? = null
    private val screenChecklists = ConcurrentHashMap<String, ScreenChecklist>()
    private var startTime: Long = 0

    /**
     * Start a new checklist for an app
     *
     * @param packageName Package name of the app being explored
     */
    fun startChecklist(packageName: String) {
        currentPackageName = packageName
        screenChecklists.clear()
        startTime = System.currentTimeMillis()
        Log.d(TAG, "Started checklist for $packageName")
    }

    /**
     * Add a screen to the checklist
     *
     * @param screenHash Unique hash identifying the screen
     * @param screenName Optional friendly name for the screen
     * @param elementCount Number of elements on the screen
     */
    fun addScreen(screenHash: String, screenName: String?, elementCount: Int) {
        if (!screenChecklists.containsKey(screenHash)) {
            screenChecklists[screenHash] = ScreenChecklist(
                screenHash = screenHash,
                screenName = screenName ?: "Screen_${screenChecklists.size + 1}",
                totalElements = elementCount,
                completedElements = mutableSetOf(),
                addedAt = System.currentTimeMillis()
            )
            Log.d(TAG, "Added screen $screenHash with $elementCount elements")
        }
    }

    /**
     * Mark an element as completed on a screen
     *
     * @param screenHash Screen containing the element
     * @param elementUuid UUID of the completed element
     */
    fun markElementCompleted(screenHash: String, elementUuid: String) {
        screenChecklists[screenHash]?.let { checklist ->
            checklist.completedElements.add(elementUuid)
            checklist.lastUpdatedAt = System.currentTimeMillis()
        }
    }

    /**
     * Export the checklist to a file
     *
     * @param filePath Path to export file
     */
    fun exportToFile(filePath: String) {
        try {
            val file = File(filePath)
            file.parentFile?.mkdirs()

            val content = buildString {
                appendLine("# Exploration Checklist")
                appendLine("Package: $currentPackageName")
                appendLine("Started: ${formatTimestamp(startTime)}")
                appendLine("Exported: ${formatTimestamp(System.currentTimeMillis())}")
                appendLine("Overall Progress: ${String.format("%.1f", getOverallProgress())}%")
                appendLine()
                appendLine("## Screens (${screenChecklists.size})")
                appendLine()

                screenChecklists.values.sortedBy { it.addedAt }.forEach { checklist ->
                    val progress = checklist.getProgress()
                    val status = if (checklist.isComplete()) "✓" else "○"
                    appendLine("### $status ${checklist.screenName}")
                    appendLine("- Hash: ${checklist.screenHash}")
                    appendLine("- Progress: ${checklist.completedElements.size}/${checklist.totalElements} (${String.format("%.1f", progress)}%)")
                    appendLine()
                }
            }

            file.writeText(content)
            Log.d(TAG, "Exported checklist to $filePath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export checklist", e)
        }
    }

    /**
     * Get overall exploration progress
     *
     * @return Progress percentage (0-100)
     */
    fun getOverallProgress(): Float {
        if (screenChecklists.isEmpty()) return 0f

        val totalElements = screenChecklists.values.sumOf { it.totalElements }
        val completedElements = screenChecklists.values.sumOf { it.completedElements.size }

        return if (totalElements > 0) {
            (completedElements.toFloat() / totalElements) * 100f
        } else {
            0f
        }
    }

    /**
     * Get progress for a specific screen
     *
     * @param screenHash Screen hash
     * @return Progress percentage or null if screen not found
     */
    fun getScreenProgress(screenHash: String): Float? {
        return screenChecklists[screenHash]?.getProgress()
    }

    /**
     * Check if exploration is complete
     *
     * @return true if all screens are fully explored
     */
    fun isComplete(): Boolean {
        return screenChecklists.isNotEmpty() &&
               screenChecklists.values.all { it.isComplete() }
    }

    /**
     * Get summary statistics
     */
    fun getSummary(): ChecklistSummary {
        return ChecklistSummary(
            packageName = currentPackageName ?: "",
            totalScreens = screenChecklists.size,
            completedScreens = screenChecklists.values.count { it.isComplete() },
            totalElements = screenChecklists.values.sumOf { it.totalElements },
            completedElements = screenChecklists.values.sumOf { it.completedElements.size },
            overallProgress = getOverallProgress(),
            durationMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Reset the checklist
     */
    fun reset() {
        currentPackageName = null
        screenChecklists.clear()
        startTime = 0
    }

    private fun formatTimestamp(timestamp: Long): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date(timestamp))
    }
}

/**
 * Screen Checklist
 *
 * Checklist for a single screen.
 */
data class ScreenChecklist(
    val screenHash: String,
    val screenName: String,
    val totalElements: Int,
    val completedElements: MutableSet<String>,
    val addedAt: Long,
    var lastUpdatedAt: Long = addedAt
) {
    fun getProgress(): Float {
        return if (totalElements > 0) {
            (completedElements.size.toFloat() / totalElements) * 100f
        } else {
            100f
        }
    }

    fun isComplete(): Boolean {
        return completedElements.size >= totalElements
    }
}

/**
 * Checklist Summary
 *
 * Summary statistics for a checklist.
 */
data class ChecklistSummary(
    val packageName: String,
    val totalScreens: Int,
    val completedScreens: Int,
    val totalElements: Int,
    val completedElements: Int,
    val overallProgress: Float,
    val durationMs: Long
)
