/**
 * ElementChecklist.kt - Element traversal checklist models
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/models/ElementChecklist.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-04
 *
 * Real-time checklist system for tracking element exploration progress
 */

package com.augmentalis.voiceoscore.learnapp.models

import android.graphics.Rect
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Element Checklist
 *
 * Tracks which elements on a screen have been clicked vs pending.
 * Provides progress metrics and exportable markdown representation.
 *
 * @property screenHash Unique hash identifying the screen
 * @property screenTitle Human-readable screen title
 * @property totalElements Total clickable elements on screen
 * @property completedElements Number of elements clicked
 * @property pendingElements List of elements not yet clicked
 * @property completedElementsList List of elements already clicked
 * @property progressPercent Completion percentage (0-100)
 */
data class ElementChecklist(
    val screenHash: String,
    val screenTitle: String,
    val totalElements: Int,
    val completedElements: Int,
    val pendingElements: List<ChecklistElement>,
    val completedElementsList: List<ChecklistElement>,
    val progressPercent: Int
) {
    /**
     * Convert to markdown format for export
     *
     * @return Markdown representation
     */
    fun toMarkdown(): String {
        return buildString {
            appendLine("## Screen: $screenTitle ($screenHash)")
            appendLine("**Progress:** $completedElements/$totalElements ($progressPercent%)")
            appendLine()
            appendLine("### ✅ Completed ($completedElements)")
            completedElementsList.forEach {
                appendLine("- [x] ${it.description} (${it.type}) - UUID: ${it.uuid.take(8)}...")
            }
            appendLine()
            appendLine("### ⏳ Pending (${pendingElements.size})")
            pendingElements.forEach {
                appendLine("- [ ] ${it.description} (${it.type}) - UUID: ${it.uuid.take(8)}...")
            }
        }
    }
}

/**
 * Checklist Element
 *
 * Represents a single element in the checklist.
 *
 * @property uuid Element UUID
 * @property description Element description (text or contentDescription)
 * @property type Element type (Button, TextView, etc.)
 * @property bounds Element screen bounds
 * @property isClickable Whether element is clickable
 */
data class ChecklistElement(
    val uuid: String,
    val description: String,
    val type: String,
    val bounds: Rect,
    val isClickable: Boolean
)

/**
 * Exploration Checklist
 *
 * Full exploration checklist for entire app.
 * Aggregates all screen checklists with overall progress metrics.
 *
 * @property packageName App package name
 * @property startTime Exploration start timestamp
 * @property screens List of screen checklists
 * @property totalScreens Total screens explored
 * @property totalElements Total elements discovered
 * @property completedElements Total elements clicked
 */
data class ExplorationChecklist(
    val packageName: String,
    val startTime: Long,
    val screens: List<ElementChecklist>,
    val totalScreens: Int,
    val totalElements: Int,
    val completedElements: Int
) {
    /**
     * Convert to markdown format for export
     *
     * @return Full markdown document
     */
    fun toMarkdown(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val overallPercent = if (totalElements > 0) {
            (completedElements * 100 / totalElements)
        } else {
            0
        }

        return buildString {
            appendLine("# Exploration Checklist: $packageName")
            appendLine()
            appendLine("**Started:** ${dateFormat.format(startTime)}")
            appendLine("**Total Screens:** $totalScreens")
            appendLine("**Total Elements:** $totalElements")
            appendLine("**Completed:** $completedElements ($overallPercent%)")
            appendLine()
            screens.forEach {
                appendLine(it.toMarkdown())
                appendLine()
            }
        }
    }
}
