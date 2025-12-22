/**
 * ExplorationState.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Defines states for app exploration
 */
package com.augmentalis.voiceoscore.learnapp.models

/**
 * State of the exploration engine
 */
sealed class ExplorationState {
    /**
     * Idle state - not currently exploring
     */
    object Idle : ExplorationState()

    /**
     * Preparing to start exploration
     *
     * @param packageName The app being prepared for exploration
     */
    data class Preparing(val packageName: String) : ExplorationState()

    /**
     * Currently running exploration
     *
     * @param packageName The app being explored
     * @param currentScreen Current screen being explored
     * @param screensExplored Number of screens explored so far
     * @param elementsProcessed Number of elements processed so far
     * @param progress Progress percentage (0.0-1.0)
     */
    data class Running(
        val packageName: String,
        val currentScreen: String,
        val screensExplored: Int,
        val elementsProcessed: Int,
        val progress: Float
    ) : ExplorationState()

    /**
     * Exploration paused
     *
     * @param packageName The app being explored
     * @param reason Reason for pause
     */
    data class Paused(
        val packageName: String,
        val reason: String
    ) : ExplorationState()

    /**
     * Exploration completed successfully
     *
     * @param packageName The app that was explored
     * @param stats Summary statistics
     */
    data class Completed(
        val packageName: String,
        val stats: ExplorationStats
    ) : ExplorationState()

    /**
     * Exploration failed
     *
     * @param packageName The app being explored
     * @param error Error message
     * @param throwable Optional exception
     */
    data class Failed(
        val packageName: String,
        val error: String,
        val throwable: Throwable? = null
    ) : ExplorationState()
}

/**
 * Statistics from exploration
 */
data class ExplorationStats(
    val totalScreens: Int = 0,
    val totalElements: Int = 0,
    val commandsGenerated: Int = 0,
    val durationMs: Long = 0,
    val errors: Int = 0
)
