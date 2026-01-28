/**
 * ExplorationFrame.kt - DFS exploration stack frame
 *
 * Represents a screen state in the exploration stack for iterative DFS.
 * This is a KMP-compatible data class that can be used across all platforms.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.ElementInfo

/**
 * Represents a screen state in the exploration stack (for iterative DFS).
 *
 * Each frame captures the state of exploration on a single screen,
 * including discovered elements and navigation context.
 *
 * @property screenHash Unique identifier for this screen
 * @property activityName Activity name (if available)
 * @property elements All clickable elements on this screen
 * @property currentElementIndex Index of next element to click
 * @property depth Depth in navigation hierarchy (0 = root)
 * @property parentScreenHash Hash of parent screen (for BACK navigation)
 */
data class ExplorationFrame(
    val screenHash: String,
    val activityName: String?,
    val elements: MutableList<ElementInfo>,
    var currentElementIndex: Int = 0,
    val depth: Int,
    val parentScreenHash: String? = null
) {
    /**
     * Check if there are more elements to explore on this screen
     */
    fun hasMoreElements(): Boolean = currentElementIndex < elements.size

    /**
     * Get the next element to explore and advance the index
     * @return Next element or null if all elements explored
     */
    fun getNextElement(): ElementInfo? {
        return if (hasMoreElements()) {
            elements[currentElementIndex].also { currentElementIndex++ }
        } else null
    }

    /**
     * Get count of remaining elements to explore
     */
    fun remainingCount(): Int = (elements.size - currentElementIndex).coerceAtLeast(0)

    /**
     * Check if this is the root screen
     */
    fun isRoot(): Boolean = depth == 0 && parentScreenHash == null
}
