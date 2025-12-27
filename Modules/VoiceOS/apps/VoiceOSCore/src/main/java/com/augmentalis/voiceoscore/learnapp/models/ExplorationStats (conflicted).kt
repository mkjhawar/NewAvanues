/**
 * ExplorationStats.kt - Final exploration statistics
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 *
 * Data class for final exploration statistics (after completion)
 */

package com.augmentalis.voiceoscore.learnapp.models

/**
 * Exploration Statistics
 *
 * Final statistics after exploration completes.
 * Saved to database and shown to user.
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
    val scrollableContainersFound: Int = 0,
    /** Overall exploration completeness percentage (0-100) */
    val completeness: Float = 0f,
    /** Number of elements that were clicked */
    val clickedElements: Int = 0,
    /** Number of elements that could be clicked (non-blocked) */
    val nonBlockedElements: Int = 0,
    /** Number of elements that were blocked (dangerous/critical) */
    val blockedElements: Int = 0
) {

    /**
     * Format duration as MM:SS
     */
    fun formatDuration(): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    /**
     * Calculate average elements per screen
     */
    fun averageElementsPerScreen(): Float {
        if (totalScreens == 0) return 0f
        return totalElements.toFloat() / totalScreens.toFloat()
    }

    /**
     * Calculate average edges per screen
     */
    fun averageEdgesPerScreen(): Float {
        if (totalScreens == 0) return 0f
        return totalEdges.toFloat() / totalScreens.toFloat()
    }

    override fun toString(): String {
        return "Stats($appName: $totalScreens screens, $totalElements elements, ${formatDuration()})"
    }
}
