/**
 * ExplorationDebugCallback.kt - Debug callback interface for exploration events
 *
 * Interface for receiving debug callbacks during app exploration.
 * Used for debug overlays, logging, and visualization tools.
 * KMP-compatible - no platform dependencies.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.ElementInfo

/**
 * Debug Callback Interface for Exploration Events.
 *
 * Implement this interface to receive real-time debug information
 * during app exploration. Useful for:
 * - Debug overlays showing exploration progress
 * - Real-time logging and visualization
 * - Testing and debugging exploration behavior
 *
 * All methods have default empty implementations so implementers
 * only need to override the events they care about.
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
 *         println("Explored $activityName with ${elements.size} elements")
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
     * Called when a screen is explored and elements are discovered.
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
    ) {}

    /**
     * Called when an element click causes navigation to a new screen.
     *
     * @param elementKey Identifier for the clicked element (screenHash:stableId)
     * @param destinationScreenHash Hash of the screen navigated to
     */
    fun onElementNavigated(elementKey: String, destinationScreenHash: String) {}

    /**
     * Called when exploration progress is updated.
     *
     * @param progress Current progress percentage (0-100)
     */
    fun onProgressUpdated(progress: Int) {}

    /**
     * Called when an element is clicked.
     *
     * @param stableId Element stable ID
     * @param screenHash Screen where element was clicked
     * @param vuid VUID if assigned
     */
    fun onElementClicked(stableId: String, screenHash: String, vuid: String?) {}

    /**
     * Called when an element is blocked (critical dangerous).
     *
     * @param stableId Element stable ID
     * @param screenHash Screen where element was found
     * @param reason Blocking reason
     */
    fun onElementBlocked(stableId: String, screenHash: String, reason: String) {}

    /**
     * Called when exploration encounters an error.
     *
     * @param error The error that occurred
     * @param context Additional context about where the error occurred
     */
    fun onError(error: Throwable, context: String) {}

    /**
     * Called when a screen is pushed onto the exploration stack.
     *
     * @param screenHash Hash of the new screen
     * @param depth Current stack depth
     * @param elementCount Number of elements on the screen
     */
    fun onStackPush(screenHash: String, depth: Int, elementCount: Int) {}

    /**
     * Called when a screen is popped from the exploration stack.
     *
     * @param screenHash Hash of the popped screen
     * @param remainingDepth Remaining stack depth
     */
    fun onStackPop(screenHash: String, remainingDepth: Int) {}

    /**
     * Called when exploration starts.
     *
     * @param packageName Package being explored
     */
    fun onExplorationStarted(packageName: String) {}

    /**
     * Called when exploration completes.
     *
     * @param packageName Package that was explored
     * @param stats Final statistics
     */
    fun onExplorationCompleted(packageName: String, stats: ExplorationStats) {}
}
