/**
 * ExplorationStats.kt - Final exploration statistics
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/models/ExplorationStats.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Data class for final exploration statistics (after completion)
 */

package com.augmentalis.learnapp.models

/**
 * Exploration Statistics
 *
 * Final statistics after exploration completes.
 * Saved to database and shown to user.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val stats = ExplorationStats(
 *     packageName = "com.instagram.android",
 *     appName = "Instagram",
 *     totalScreens = 25,
 *     totalElements = 456,
 *     totalEdges = 89,
 *     durationMs = 180000L,  // 3 minutes
 *     maxDepth = 12,
 *     dangerousElementsSkipped = 3,
 *     loginScreensDetected = 0,
 *     scrollableContainersFound = 15
 * )
 *
 * println(stats)  // Pretty-printed summary
 * ```
 *
 * @property packageName Package name of explored app
 * @property appName Human-readable app name
 * @property totalScreens Total unique screens discovered
 * @property totalElements Total UI elements mapped
 * @property totalEdges Total navigation edges (transitions)
 * @property durationMs Total exploration time in milliseconds
 * @property maxDepth Maximum DFS depth reached
 * @property dangerousElementsSkipped Number of dangerous elements skipped
 * @property loginScreensDetected Number of login screens detected
 * @property scrollableContainersFound Number of scrollable containers found
 *
 * @since 1.0.0
 */
data class ExplorationStats(
    val packageName: String,
    val appName: String,
    val totalScreens: Int,
    val totalElements: Int,
    val totalEdges: Int,
    val durationMs: Long,
    val maxDepth: Int,
    val dangerousElementsSkipped: Int = 0,
    val loginScreensDetected: Int = 0,
    val scrollableContainersFound: Int = 0
) {

    /**
     * Format duration as MM:SS
     *
     * @return Formatted duration string
     */
    fun formatDuration(): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    /**
     * Calculate average elements per screen
     *
     * @return Average number of elements per screen
     */
    fun averageElementsPerScreen(): Float {
        if (totalScreens == 0) return 0f
        return totalElements.toFloat() / totalScreens.toFloat()
    }

    /**
     * Calculate average edges per screen
     *
     * @return Average number of outgoing edges per screen
     */
    fun averageEdgesPerScreen(): Float {
        if (totalScreens == 0) return 0f
        return totalEdges.toFloat() / totalScreens.toFloat()
    }

    override fun toString(): String {
        return """
            Exploration Complete: $appName

            📊 Statistics:
            - Screens Explored: $totalScreens
            - Elements Mapped: $totalElements
            - Navigation Edges: $totalEdges
            - Max Depth: $maxDepth
            - Duration: ${formatDuration()}

            🛡️ Safety:
            - Dangerous Elements Skipped: $dangerousElementsSkipped
            - Login Screens Detected: $loginScreensDetected

            📜 Details:
            - Scrollable Containers: $scrollableContainersFound
            - Avg Elements/Screen: ${"%.1f".format(averageElementsPerScreen())}
            - Avg Edges/Screen: ${"%.1f".format(averageEdgesPerScreen())}
        """.trimIndent()
    }
}
