/**
 * ChecklistManager.kt - Element exploration checklist manager
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/exploration/ChecklistManager.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-04
 *
 * Real-time tracking of element exploration progress with exportable checklists
 */

package com.augmentalis.voiceoscore.learnapp.exploration

import com.augmentalis.voiceoscore.learnapp.models.ChecklistElement
import com.augmentalis.voiceoscore.learnapp.models.ElementChecklist
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.models.ExplorationChecklist
import java.io.File

/**
 * Checklist Manager
 *
 * Manages element exploration checklists during app learning.
 * Tracks which elements have been clicked on each screen and provides
 * progress metrics and exportable checklist files.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val manager = ChecklistManager()
 *
 * // Start new exploration
 * manager.startChecklist("com.instagram.android")
 *
 * // Add screen
 * manager.addScreen(
 *     screenHash = "abc123...",
 *     screenTitle = "Home",
 *     elements = listOf(...)
 * )
 *
 * // Mark element clicked
 * manager.markElementCompleted(screenHash, elementUuid)
 *
 * // Export to file
 * manager.exportToFile("/sdcard/Download/checklist.md")
 * ```
 *
 * @since 1.0.0
 */
class ChecklistManager {

    /**
     * Map of screen hash to element checklist
     */
    private val screenChecklists = mutableMapOf<String, ElementChecklist>()

    /**
     * Exploration start timestamp
     */
    private var startTime: Long = 0

    /**
     * Target app package name
     */
    private var packageName: String = ""

    /**
     * Start new checklist
     *
     * Initializes checklist tracking for a new app exploration session.
     *
     * @param pkg Package name of app being explored
     */
    fun startChecklist(pkg: String) {
        packageName = pkg
        startTime = System.currentTimeMillis()
        screenChecklists.clear()

        android.util.Log.i("ChecklistManager", "📋 Started checklist for: $pkg")
    }

    /**
     * Add screen to checklist
     *
     * Creates checklist entry for a newly discovered screen.
     * All elements start as pending (not clicked).
     *
     * @param screenHash Unique screen identifier
     * @param screenTitle Human-readable screen name
     * @param elements List of elements on this screen
     */
    fun addScreen(
        screenHash: String,
        screenTitle: String,
        elements: List<ElementInfo>
    ) {
        val checklistElements = elements.mapNotNull { elem ->
            // Only track clickable elements with UUIDs
            if (elem.uuid == null) {
                android.util.Log.w("ChecklistManager",
                    "Skipping element without UUID: ${elem.getDisplayName()}")
                return@mapNotNull null
            }

            ChecklistElement(
                uuid = elem.uuid!!,
                description = elem.getDisplayName(),
                type = elem.className.substringAfterLast('.'),
                bounds = elem.bounds,
                isClickable = elem.isClickable
            )
        }

        val checklist = ElementChecklist(
            screenHash = screenHash,
            screenTitle = screenTitle,
            totalElements = checklistElements.size,
            completedElements = 0,
            pendingElements = checklistElements,
            completedElementsList = emptyList(),
            progressPercent = 0
        )

        screenChecklists[screenHash] = checklist

        android.util.Log.d("ChecklistManager",
            "Added screen: ${screenTitle.take(30)} (${checklistElements.size} elements)")
    }

    /**
     * Mark element as completed
     *
     * Moves element from pending to completed list and updates progress.
     *
     * @param screenHash Screen containing the element
     * @param elementUuid UUID of element that was clicked
     */
    fun markElementCompleted(screenHash: String, elementUuid: String) {
        val checklist = screenChecklists[screenHash]
        if (checklist == null) {
            android.util.Log.w("ChecklistManager",
                "Screen not found in checklist: ${screenHash.take(8)}...")
            return
        }

        val element = checklist.pendingElements.find { it.uuid == elementUuid }
        if (element == null) {
            android.util.Log.v("ChecklistManager",
                "Element already completed or not in checklist: ${elementUuid.take(8)}...")
            return
        }

        val updatedChecklist = checklist.copy(
            completedElements = checklist.completedElements + 1,
            pendingElements = checklist.pendingElements - element,
            completedElementsList = checklist.completedElementsList + element,
            progressPercent = ((checklist.completedElements + 1) * 100 / checklist.totalElements)
        )

        screenChecklists[screenHash] = updatedChecklist

        android.util.Log.i("ChecklistManager",
            "✅ ${updatedChecklist.completedElements}/${updatedChecklist.totalElements} " +
            "elements completed on screen ${screenHash.take(8)}...")
    }

    /**
     * Get full exploration checklist
     *
     * @return Complete checklist with all screens and progress
     */
    fun getChecklist(): ExplorationChecklist {
        return ExplorationChecklist(
            packageName = packageName,
            startTime = startTime,
            screens = screenChecklists.values.toList(),
            totalScreens = screenChecklists.size,
            totalElements = screenChecklists.values.sumOf { it.totalElements },
            completedElements = screenChecklists.values.sumOf { it.completedElements }
        )
    }

    /**
     * Export checklist to markdown file
     *
     * Creates human-readable checklist file showing:
     * - Overall progress
     * - Per-screen progress
     * - Completed elements (checked)
     * - Pending elements (unchecked)
     *
     * @param outputPath File path for export
     */
    fun exportToFile(outputPath: String) {
        val checklist = getChecklist()
        val markdown = checklist.toMarkdown()

        try {
            File(outputPath).writeText(markdown)
            android.util.Log.i("ChecklistManager", "✅ Checklist exported to $outputPath")
        } catch (e: Exception) {
            android.util.Log.e("ChecklistManager", "Failed to export checklist", e)
        }
    }

    /**
     * Get overall progress percentage
     *
     * @return Progress percentage (0-100)
     */
    fun getOverallProgress(): Int {
        val checklist = getChecklist()
        return if (checklist.totalElements > 0) {
            (checklist.completedElements * 100 / checklist.totalElements)
        } else {
            0
        }
    }

    /**
     * Clear all checklist data
     *
     * Called when starting new exploration or cleanup.
     */
    fun clear() {
        screenChecklists.clear()
        startTime = 0
        packageName = ""
        android.util.Log.d("ChecklistManager", "Checklist cleared")
    }
}
