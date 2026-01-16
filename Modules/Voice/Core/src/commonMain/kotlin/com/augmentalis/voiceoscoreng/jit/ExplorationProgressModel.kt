/**
 * ExplorationProgressModel.kt - Exploration progress data model
 *
 * Cross-platform data class for exploration progress tracking.
 * Migrated from JITLearning library for KMP compatibility.
 *
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 * Migrated from: JITLearning/ExplorationProgress.kt
 *
 * @since 3.0.0 (KMP Migration)
 */

package com.augmentalis.voiceoscoreng.jit

/**
 * Exploration state enum for type-safe state representation.
 */
enum class ExplorationState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED
}

/**
 * Exploration Progress Model
 *
 * Contains progress metrics for an ongoing exploration session.
 * Cross-platform model for tracking automated UI exploration.
 *
 * ## Usage:
 * ```kotlin
 * val progress = ExplorationProgressModel.running(
 *     packageName = "com.example.app",
 *     screensExplored = 5,
 *     elementsDiscovered = 42,
 *     currentDepth = 2,
 *     progressPercent = 35,
 *     elapsedMs = 120000
 * )
 * ```
 *
 * @property screensExplored Number of screens explored
 * @property elementsDiscovered Number of elements discovered
 * @property currentDepth Current exploration depth
 * @property packageName Package being explored
 * @property state Current exploration state
 * @property pauseReason Pause reason if paused
 * @property progressPercent Progress percentage (0-100)
 * @property elapsedMs Time elapsed in milliseconds
 */
data class ExplorationProgressModel(
    val screensExplored: Int = 0,
    val elementsDiscovered: Int = 0,
    val currentDepth: Int = 0,
    val packageName: String = "",
    val state: ExplorationState = ExplorationState.IDLE,
    val pauseReason: String? = null,
    val progressPercent: Int = 0,
    val elapsedMs: Long = 0
) {

    /**
     * Check if exploration is currently active.
     */
    fun isActive(): Boolean = state == ExplorationState.RUNNING

    /**
     * Check if exploration is paused.
     */
    fun isPaused(): Boolean = state == ExplorationState.PAUSED

    /**
     * Check if exploration is completed.
     */
    fun isCompleted(): Boolean = state == ExplorationState.COMPLETED

    /**
     * Check if exploration has failed.
     */
    fun isFailed(): Boolean = state == ExplorationState.FAILED

    /**
     * Get human-readable status message.
     */
    fun getStatusMessage(): String {
        return when (state) {
            ExplorationState.IDLE -> "Exploration idle"
            ExplorationState.RUNNING -> "Exploring $packageName ($progressPercent%)"
            ExplorationState.PAUSED -> "Paused: ${pauseReason ?: "unknown reason"}"
            ExplorationState.COMPLETED -> "Completed: $screensExplored screens, $elementsDiscovered elements"
            ExplorationState.FAILED -> "Failed: ${pauseReason ?: "unknown error"}"
        }
    }

    /**
     * Get elapsed time in human-readable format.
     */
    fun getElapsedTimeFormatted(): String {
        val seconds = (elapsedMs / 1000) % 60
        val minutes = (elapsedMs / (1000 * 60)) % 60
        val hours = elapsedMs / (1000 * 60 * 60)

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    companion object {
        /**
         * Create an idle progress instance.
         */
        fun idle(): ExplorationProgressModel {
            return ExplorationProgressModel(state = ExplorationState.IDLE)
        }

        /**
         * Create a running progress instance.
         */
        fun running(
            packageName: String,
            screensExplored: Int,
            elementsDiscovered: Int,
            currentDepth: Int,
            progressPercent: Int,
            elapsedMs: Long
        ): ExplorationProgressModel {
            return ExplorationProgressModel(
                screensExplored = screensExplored,
                elementsDiscovered = elementsDiscovered,
                currentDepth = currentDepth,
                packageName = packageName,
                state = ExplorationState.RUNNING,
                progressPercent = progressPercent,
                elapsedMs = elapsedMs
            )
        }

        /**
         * Create a paused progress instance.
         */
        fun paused(
            packageName: String,
            screensExplored: Int,
            elementsDiscovered: Int,
            pauseReason: String,
            elapsedMs: Long = 0
        ): ExplorationProgressModel {
            return ExplorationProgressModel(
                screensExplored = screensExplored,
                elementsDiscovered = elementsDiscovered,
                packageName = packageName,
                state = ExplorationState.PAUSED,
                pauseReason = pauseReason,
                elapsedMs = elapsedMs
            )
        }

        /**
         * Create a completed progress instance.
         */
        fun completed(
            packageName: String,
            screensExplored: Int,
            elementsDiscovered: Int,
            elapsedMs: Long = 0
        ): ExplorationProgressModel {
            return ExplorationProgressModel(
                screensExplored = screensExplored,
                elementsDiscovered = elementsDiscovered,
                packageName = packageName,
                state = ExplorationState.COMPLETED,
                progressPercent = 100,
                elapsedMs = elapsedMs
            )
        }

        /**
         * Create a failed progress instance.
         */
        fun failed(
            packageName: String,
            screensExplored: Int,
            elementsDiscovered: Int,
            errorReason: String,
            elapsedMs: Long = 0
        ): ExplorationProgressModel {
            return ExplorationProgressModel(
                screensExplored = screensExplored,
                elementsDiscovered = elementsDiscovered,
                packageName = packageName,
                state = ExplorationState.FAILED,
                pauseReason = errorReason,
                elapsedMs = elapsedMs
            )
        }
    }
}
