/**
 * IExplorationEngine.kt - Main exploration engine interface
 *
 * Defines the contract for app exploration engines.
 * Platform implementations provide the actual exploration logic.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.commandmanager

import kotlinx.coroutines.flow.StateFlow

/**
 * Exploration state for external observers
 */
sealed class ExplorationEngineState {
    /** No exploration active */
    data object Idle : ExplorationEngineState()

    /** Exploration is initializing */
    data class Initializing(val packageName: String) : ExplorationEngineState()

    /** Exploration is actively running */
    data class Running(
        val packageName: String,
        val progress: ExplorationProgress
    ) : ExplorationEngineState()

    /** Exploration is paused */
    data class Paused(
        val packageName: String,
        val progress: ExplorationProgress,
        val reason: String
    ) : ExplorationEngineState()

    /** Exploration completed successfully */
    data class Completed(
        val packageName: String,
        val stats: ExplorationSummary
    ) : ExplorationEngineState()

    /** Exploration failed */
    data class Failed(
        val packageName: String,
        val error: Throwable,
        val partialProgress: ExplorationProgress?
    ) : ExplorationEngineState()
}

/**
 * Progress information during exploration
 */
data class ExplorationProgress(
    val appName: String,
    val screensExplored: Int,
    val estimatedTotalScreens: Int,
    val elementsDiscovered: Int,
    val elementsClicked: Int,
    val currentDepth: Int,
    val currentScreen: String,
    val elapsedTimeMs: Long
) {
    val progressPercent: Float
        get() = if (estimatedTotalScreens > 0) {
            (screensExplored.toFloat() / estimatedTotalScreens.toFloat() * 100f).coerceIn(0f, 100f)
        } else 0f
}

/**
 * Final statistics after exploration completes.
 *
 * Note: This is distinct from ExplorationStats in ExplorationStats.kt which
 * is used for AVU STA line format serialization.
 */
data class ExplorationSummary(
    val packageName: String,
    val appName: String,
    val totalScreens: Int,
    val totalElements: Int,
    val totalEdges: Int,
    val durationMs: Long,
    val maxDepth: Int,
    val dangerousElementsSkipped: Int,
    val loginScreensDetected: Int,
    val scrollableContainersFound: Int,
    val completeness: Float
) {
    override fun toString(): String {
        return "ExplorationSummary[$appName]: $totalScreens screens, " +
               "$totalElements elements, ${completeness.toInt()}% complete, " +
               "${durationMs / 1000}s"
    }
}

/**
 * Main exploration engine interface.
 *
 * Implementations handle the platform-specific details of:
 * - Starting/stopping exploration
 * - DFS traversal of app screens
 * - Element clicking and registration
 * - Progress tracking and reporting
 */
interface IExplorationEngine {

    /**
     * Observable state of the exploration engine
     */
    val state: StateFlow<ExplorationEngineState>

    /**
     * Current configuration
     */
    val config: ExplorationConfig

    /**
     * Cumulative tracking data
     */
    val tracking: CumulativeTracking

    /**
     * Start exploration of an app.
     *
     * @param packageName Package name to explore
     * @param sessionId Optional session ID for persistence
     */
    fun startExploration(packageName: String, sessionId: String? = null)

    /**
     * Stop exploration immediately.
     */
    fun stopExploration()

    /**
     * Pause exploration (can be resumed).
     *
     * @param reason Reason for pausing
     */
    suspend fun pause(reason: String = "User paused")

    /**
     * Resume paused exploration.
     */
    suspend fun resume()

    /**
     * Check if exploration is currently paused
     */
    fun isPaused(): Boolean

    /**
     * Check if exploration is currently running
     */
    fun isRunning(): Boolean

    /**
     * Set debug callback for exploration events
     *
     * @param callback Callback to receive events, or null to disable
     */
    fun setDebugCallback(callback: ExplorationDebugCallback?)

    /**
     * Get current exploration statistics
     */
    fun getStats(): String
}
