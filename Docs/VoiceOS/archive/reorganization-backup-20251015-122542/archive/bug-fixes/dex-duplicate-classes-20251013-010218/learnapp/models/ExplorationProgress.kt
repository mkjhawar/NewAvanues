/**
 * ExplorationProgress.kt - Real-time exploration progress tracking
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/models/ExplorationProgress.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Data class for tracking real-time exploration progress
 */

package com.augmentalis.learnapp.models

/**
 * Exploration Progress
 *
 * Tracks real-time progress during app exploration.
 * Used for UI updates and progress monitoring.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val progress = ExplorationProgress(
 *     appName = "Instagram",
 *     screensExplored = 15,
 *     estimatedTotalScreens = 30,
 *     elementsDiscovered = 234,
 *     currentDepth = 5,
 *     currentScreen = "Profile → Settings",
 *     elapsedTimeMs = 150000L  // 2.5 minutes
 * )
 *
 * // Calculate percentage
 * val percentage = progress.calculatePercentage()  // 0.5f (50%)
 *
 * // Format duration
 * val duration = progress.formatDuration()  // "02:30"
 * ```
 *
 * @property appName Human-readable app name
 * @property screensExplored Number of screens explored so far
 * @property estimatedTotalScreens Estimated total screens (rough heuristic)
 * @property elementsDiscovered Number of UI elements discovered
 * @property currentDepth Current DFS depth (how deep in navigation tree)
 * @property currentScreen Description of current screen (e.g., "Home → Profile")
 * @property elapsedTimeMs Time elapsed since exploration started (milliseconds)
 *
 * @since 1.0.0
 */
data class ExplorationProgress(
    val appName: String,
    val screensExplored: Int = 0,
    val estimatedTotalScreens: Int = 0,
    val elementsDiscovered: Int = 0,
    val currentDepth: Int = 0,
    val currentScreen: String = "",
    val elapsedTimeMs: Long = 0L
) {

    /**
     * Calculate progress percentage
     *
     * Returns percentage of exploration completed (0.0 to 1.0).
     * Based on screens explored vs estimated total.
     *
     * @return Progress percentage (0.0-1.0)
     */
    fun calculatePercentage(): Float {
        if (estimatedTotalScreens == 0) return 0f
        return (screensExplored.toFloat() / estimatedTotalScreens.toFloat()).coerceIn(0f, 1.0f)
    }

    /**
     * Format elapsed time as MM:SS
     *
     * @return Formatted duration string
     */
    fun formatDuration(): String {
        val totalSeconds = elapsedTimeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    /**
     * Calculate estimated remaining time (milliseconds)
     *
     * Uses linear extrapolation based on current progress.
     *
     * @return Estimated remaining time in milliseconds
     */
    fun estimateRemainingTimeMs(): Long {
        if (screensExplored == 0) return 0L
        val timePerScreen = elapsedTimeMs / screensExplored
        val remainingScreens = (estimatedTotalScreens - screensExplored).coerceAtLeast(0)
        return timePerScreen * remainingScreens
    }

    /**
     * Check if exploration is taking too long (>30 minutes)
     *
     * @return true if elapsed time exceeds 30 minutes
     */
    fun isTimeout(): Boolean {
        val thirtyMinutes = 30 * 60 * 1000L
        return elapsedTimeMs > thirtyMinutes
    }

    /**
     * Create copy with updated elapsed time
     *
     * @param newElapsedMs New elapsed time in milliseconds
     * @return Updated progress
     */
    fun withUpdatedTime(newElapsedMs: Long): ExplorationProgress {
        return copy(elapsedTimeMs = newElapsedMs)
    }

    override fun toString(): String {
        return """
            Exploration Progress:
            - App: $appName
            - Screens: $screensExplored / ~$estimatedTotalScreens (${(calculatePercentage() * 100).toInt()}%)
            - Elements: $elementsDiscovered
            - Depth: $currentDepth
            - Time: ${formatDuration()}
            - Current: $currentScreen
        """.trimIndent()
    }
}
