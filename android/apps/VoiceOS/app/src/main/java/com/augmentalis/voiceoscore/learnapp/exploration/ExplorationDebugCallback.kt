/**
 * ExplorationDebugCallback.kt - Debug callback interface for exploration events
 * Path: apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/exploration/ExplorationDebugCallback.kt
 *
 * Author: Manoj Jhawar (refactored by Claude)
 * Created: 2025-12-08
 * Refactored: 2026-01-15 (Extracted from ExplorationEngine.kt)
 *
 * Interface for receiving debug callbacks during app exploration.
 * Used for debug overlays, logging, and visualization tools.
 */

package com.augmentalis.voiceoscore.learnapp.exploration

import com.augmentalis.voiceoscore.learnapp.models.ElementInfo

/**
 * Debug Callback Interface for Exploration Events
 *
 * Implement this interface to receive real-time debug information
 * during app exploration. Useful for:
 * - Debug overlays showing exploration progress
 * - Real-time logging and visualization
 * - Testing and debugging exploration behavior
 *
 * ## Usage Example
 *
 * ```kotlin
 * class MyDebugOverlay : ExplorationDebugCallback {
 *     override fun onScreenExplored(
 *         elements: List<ElementInfo>,
 *         screenHash: String,
 *         activityName: String,
 *         packageName: String,
 *         parentScreenHash: String?
 *     ) {
 *         Log.d("Debug", "Explored $activityName with ${elements.size} elements")
 *     }
 *
 *     override fun onElementNavigated(elementKey: String, destinationScreenHash: String) {
 *         Log.d("Debug", "Navigation: $elementKey -> $destinationScreenHash")
 *     }
 *
 *     override fun onProgressUpdated(progress: Int) {
 *         updateProgressBar(progress)
 *     }
 * }
 *
 * // Attach to exploration engine
 * explorationEngine.setDebugCallback(MyDebugOverlay())
 * ```
 */
interface ExplorationDebugCallback {
    /**
     * Called when a screen is explored and elements are discovered
     *
     * @param elements List of discovered elements on current screen
     * @param screenHash Unique hash of the current screen state
     * @param activityName Current activity name
     * @param packageName Target app package
     * @param parentScreenHash Hash of the screen we navigated from (null if root)
     */
    fun onScreenExplored(
        elements: List<ElementInfo>,
        screenHash: String,
        activityName: String,
        packageName: String,
        parentScreenHash: String?
    )

    /**
     * Called when an element click causes navigation to a new screen
     *
     * @param elementKey Identifier for the clicked element (screenHash:stableId)
     * @param destinationScreenHash Hash of the screen navigated to
     */
    fun onElementNavigated(elementKey: String, destinationScreenHash: String)

    /**
     * Called when exploration progress is updated
     *
     * @param progress Current progress percentage (0-100)
     */
    fun onProgressUpdated(progress: Int)

    /**
     * Called when an element is clicked
     *
     * @param stableId Element stable ID
     * @param screenHash Screen where element was clicked
     * @param vuid VUID if assigned
     */
    fun onElementClicked(stableId: String, screenHash: String, vuid: String?) {}

    /**
     * Called when an element is blocked (critical dangerous)
     *
     * @param stableId Element stable ID
     * @param screenHash Screen where element was found
     * @param reason Blocking reason
     */
    fun onElementBlocked(stableId: String, screenHash: String, reason: String) {}
}
