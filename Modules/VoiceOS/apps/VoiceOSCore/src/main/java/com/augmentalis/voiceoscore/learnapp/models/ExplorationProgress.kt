/**
 * ExplorationProgress.kt - Real-time exploration progress tracking
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 *
 * Data class for tracking real-time exploration progress
 */

package com.augmentalis.voiceoscore.learnapp.models

/**
 * Exploration Progress
 *
 * Tracks real-time progress during app exploration.
 * Used for UI updates and progress monitoring.
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
     */
    fun calculatePercentage(): Float {
        if (estimatedTotalScreens == 0) return 0f
        return (screensExplored.toFloat() / estimatedTotalScreens.toFloat()).coerceIn(0f, 1.0f)
    }

    /**
     * Format elapsed time as MM:SS
     */
    fun formatDuration(): String {
        val totalSeconds = elapsedTimeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    /**
     * Calculate estimated remaining time (milliseconds)
     */
    fun estimateRemainingTimeMs(): Long {
        if (screensExplored == 0) return 0L
        val timePerScreen = elapsedTimeMs / screensExplored
        val remainingScreens = (estimatedTotalScreens - screensExplored).coerceAtLeast(0)
        return timePerScreen * remainingScreens
    }

    /**
     * Check if exploration is taking too long (>30 minutes)
     */
    fun isTimeout(): Boolean {
        val thirtyMinutes = 30 * 60 * 1000L
        return elapsedTimeMs > thirtyMinutes
    }

    /**
     * Create copy with updated elapsed time
     */
    fun withUpdatedTime(newElapsedMs: Long): ExplorationProgress {
        return copy(elapsedTimeMs = newElapsedMs)
    }

    override fun toString(): String {
        return "Progress($appName: $screensExplored/$estimatedTotalScreens screens, $elementsDiscovered elements, ${formatDuration()})"
    }
}
