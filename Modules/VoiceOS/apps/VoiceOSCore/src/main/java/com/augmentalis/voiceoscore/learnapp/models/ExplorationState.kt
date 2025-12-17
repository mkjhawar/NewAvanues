/**
 * ExplorationState.kt - Sealed class representing exploration states
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 *
 * Sealed class hierarchy for exploration state machine
 */

package com.augmentalis.voiceoscore.learnapp.models

/**
 * Exploration State
 *
 * Represents the current state of app exploration using a state machine pattern.
 */
sealed class ExplorationState {

    /**
     * Idle state - no exploration in progress
     */
    object Idle : ExplorationState()

    /**
     * Consent requested - waiting for user approval
     */
    data class ConsentRequested(
        val packageName: String,
        val appName: String
    ) : ExplorationState()

    /**
     * Consent cancelled - user declined to learn app
     */
    data class ConsentCancelled(
        val packageName: String
    ) : ExplorationState()

    /**
     * Running - exploration in progress
     */
    data class Running(
        val packageName: String,
        val progress: ExplorationProgress
    ) : ExplorationState()

    /**
     * Paused for login - login screen detected, waiting for user to login
     */
    data class PausedForLogin(
        val packageName: String,
        val progress: ExplorationProgress
    ) : ExplorationState()

    /**
     * Paused by user - user manually paused exploration
     */
    data class PausedByUser(
        val packageName: String,
        val progress: ExplorationProgress
    ) : ExplorationState()

    /**
     * Paused - generic paused state (auto or manual)
     */
    data class Paused(
        val packageName: String,
        val progress: ExplorationProgress,
        val reason: String = "Paused"
    ) : ExplorationState()

    /**
     * Completed - exploration finished successfully
     */
    data class Completed(
        val packageName: String,
        val stats: ExplorationStats
    ) : ExplorationState()

    /**
     * Failed - exploration failed due to error
     */
    data class Failed(
        val packageName: String,
        val error: Throwable,
        val partialProgress: ExplorationProgress? = null
    ) : ExplorationState()
}
