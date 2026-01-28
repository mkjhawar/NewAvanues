/**
 * IJitLearner.kt - JIT Learner Provider Interface
 *
 * Cross-platform interface for JIT (Just-In-Time) learning providers.
 * Defines the contract for screen learning and element discovery.
 *
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 *
 * @since 3.0.0 (KMP Migration)
 */

package com.augmentalis.voiceoscore

/**
 * JIT Learner Provider Interface
 *
 * Defines the contract for JIT learning implementations across platforms.
 * Platform-specific implementations handle accessibility node traversal
 * and element discovery.
 *
 * ## Responsibilities:
 * - Start/stop/pause JIT learning sessions
 * - Query current learning state
 * - Process screen changes and element discovery
 * - Handle exploration commands
 *
 * ## Usage:
 * ```kotlin
 * class AndroidJitLearner(service: AccessibilityService) : IJitLearner {
 *     override fun start(packageName: String) { ... }
 *     override fun queryState(): JitStateModel { ... }
 * }
 * ```
 */
interface IJitLearner {

    /**
     * Start JIT learning for a specific package.
     *
     * @param packageName Package to learn (e.g., "com.microsoft.teams")
     * @return true if learning started successfully
     */
    fun start(packageName: String): Boolean

    /**
     * Stop JIT learning.
     *
     * Stops all learning activities and clears session data.
     */
    fun stop()

    /**
     * Pause JIT learning.
     *
     * Temporarily suspends learning while preserving session state.
     */
    fun pause()

    /**
     * Resume paused JIT learning.
     *
     * @return true if resumed successfully
     */
    fun resume(): Boolean

    /**
     * Query current JIT state.
     *
     * @return Current state of the JIT learning session
     */
    fun queryState(): JitStateModel

    /**
     * Check if JIT learning is currently active.
     *
     * @return true if learning is in progress
     */
    fun isActive(): Boolean

    /**
     * Process a screen change event.
     *
     * Called when the accessibility service detects a new screen.
     *
     * @param event Screen change event data
     */
    fun onScreenChanged(event: ScreenChangeEvent)

    /**
     * Perform an exploration action.
     *
     * Executes a command as part of automated exploration.
     *
     * @param command Exploration command to execute
     * @return true if command was executed successfully
     */
    fun performAction(command: ExplorationCommand): Boolean

    /**
     * Get current exploration progress.
     *
     * @return Current progress of the exploration session
     */
    fun getExplorationProgress(): ExplorationProgressModel

    /**
     * Set learning callback for progress updates.
     *
     * @param callback Callback to receive learning updates
     */
    fun setLearningCallback(callback: JitLearningCallback?)
}

/**
 * Callback interface for JIT learning progress updates.
 */
interface JitLearningCallback {

    /**
     * Called when a new screen is discovered.
     *
     * @param event Screen change event for the new screen
     */
    fun onScreenDiscovered(event: ScreenChangeEvent)

    /**
     * Called when learning progress is updated.
     *
     * @param progress Current exploration progress
     */
    fun onProgressUpdated(progress: ExplorationProgressModel)

    /**
     * Called when learning state changes.
     *
     * @param state New JIT state
     */
    fun onStateChanged(state: JitStateModel)

    /**
     * Called when an error occurs during learning.
     *
     * @param error Error message
     * @param recoverable Whether the error is recoverable
     */
    fun onError(error: String, recoverable: Boolean)
}
