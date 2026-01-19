/**
 * ExplorationState.kt - Sealed class representing exploration states
 * Path: libraries/AvidCreator/src/main/java/com/augmentalis/learnapp/models/ExplorationState.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Sealed class hierarchy for exploration state machine
 */

package com.augmentalis.voiceoscore.learnapp.models

/**
 * Exploration State
 *
 * Represents the current state of app exploration using a state machine pattern.
 *
 * ## State Transitions
 *
 * ```
 * Idle → ConsentRequested → Running → Completed
 *                         ↓           ↓
 *                    Cancelled    PausedForLogin
 *                                     ↓
 *                                 Running
 *                                     ↓
 *                                PausedByUser
 *                                     ↓
 *                                 Running
 *                                     ↓
 *                                  Failed
 * ```
 *
 * ## Usage Example
 *
 * ```kotlin
 * val state: ExplorationState = ExplorationState.Running(
 *     progress = ExplorationProgress(
 *         screensExplored = 10,
 *         elementsDiscovered = 234,
 *         currentDepth = 5
 *     )
 * )
 *
 * when (state) {
 *     is ExplorationState.Idle -> println("Ready to start")
 *     is ExplorationState.Running -> println("Exploring... ${state.progress.screensExplored} screens")
 *     is ExplorationState.Completed -> println("Done! ${state.stats.totalScreens} screens")
 *     is ExplorationState.Failed -> println("Error: ${state.error.message}")
 * }
 * ```
 *
 * @since 1.0.0
 */
sealed class ExplorationState {

    /**
     * Idle state - no exploration in progress
     */
    object Idle : ExplorationState()

    /**
     * Consent requested - waiting for user approval
     *
     * @property packageName Package name of app requesting consent
     * @property appName Human-readable app name
     */
    data class ConsentRequested(
        val packageName: String,
        val appName: String
    ) : ExplorationState()

    /**
     * Consent cancelled - user declined to learn app
     *
     * @property packageName Package name of declined app
     */
    data class ConsentCancelled(
        val packageName: String
    ) : ExplorationState()

    /**
     * Preparing - exploration is being set up
     *
     * @property packageName Package being prepared for exploration
     */
    data class Preparing(
        val packageName: String
    ) : ExplorationState()

    /**
     * Running - exploration in progress
     *
     * @property packageName Package being explored
     * @property progress Current progress stats
     */
    data class Running(
        val packageName: String,
        val progress: ExplorationProgress
    ) : ExplorationState()

    /**
     * Paused for login - login screen detected, waiting for user to login
     *
     * @property packageName Package being explored
     * @property progress Progress before pause
     */
    data class PausedForLogin(
        val packageName: String,
        val progress: ExplorationProgress
    ) : ExplorationState()

    /**
     * Paused by user - user manually paused exploration
     *
     * @property packageName Package being explored
     * @property progress Progress before pause
     */
    data class PausedByUser(
        val packageName: String,
        val progress: ExplorationProgress
    ) : ExplorationState()

    /**
     * Paused - exploration paused (Phase 2: unified pause state)
     *
     * Replaces PausedForLogin and PausedByUser with a single unified state
     * that includes the pause reason for UI display.
     *
     * @property packageName Package being explored
     * @property progress Progress before pause
     * @property reason Reason for pause (e.g., "User paused", "Permission required", "Login screen detected")
     */
    data class Paused(
        val packageName: String,
        val progress: ExplorationProgress,
        val reason: String
    ) : ExplorationState()

    /**
     * Completed - exploration finished successfully
     *
     * @property packageName Package that was explored
     * @property stats Final exploration statistics
     */
    data class Completed(
        val packageName: String,
        val stats: ExplorationStats
    ) : ExplorationState()

    /**
     * Failed - exploration failed due to error
     *
     * @property packageName Package that was being explored
     * @property error The error that caused failure
     * @property partialProgress Progress before failure (if any)
     */
    data class Failed(
        val packageName: String,
        val error: Throwable,
        val partialProgress: ExplorationProgress? = null
    ) : ExplorationState()
}
