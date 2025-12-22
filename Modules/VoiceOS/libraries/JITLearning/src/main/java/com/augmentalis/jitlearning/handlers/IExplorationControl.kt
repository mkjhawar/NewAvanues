/**
 * IExplorationControl.kt - Interface for exploration control operations
 *
 * Handles start/stop exploration, pause/resume, and state management.
 * Extracted from JITLearningService as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 2.2.0 (SOLID Refactoring)
 */

package com.augmentalis.jitlearning.handlers

import com.augmentalis.jitlearning.ExplorationProgress
import com.augmentalis.jitlearning.ExplorationProgressCallback

/**
 * Exploration Control Handler Interface
 *
 * Responsibilities:
 * - Start and stop exploration sessions
 * - Pause and resume exploration
 * - Track exploration state
 * - Manage exploration callbacks
 *
 * Single Responsibility: Exploration lifecycle management
 */
interface IExplorationControl {
    /**
     * Start exploration for target package.
     *
     * @param targetPackage Package name to explore
     * @param callback Optional progress callback
     * @return True if exploration started
     */
    suspend fun startExploration(
        targetPackage: String,
        callback: ExplorationProgressCallback? = null
    ): Boolean

    /**
     * Stop current exploration.
     */
    fun stopExploration()

    /**
     * Pause current exploration.
     */
    fun pauseExploration()

    /**
     * Resume paused exploration.
     */
    fun resumeExploration()

    /**
     * Check if exploration is currently running.
     *
     * @return True if exploring
     */
    fun isExploring(): Boolean

    /**
     * Check if exploration is paused.
     *
     * @return True if paused
     */
    fun isPaused(): Boolean

    /**
     * Get current exploration progress.
     *
     * @return Exploration progress data
     */
    fun getExplorationProgress(): ExplorationProgress

    /**
     * Set exploration progress callback.
     *
     * @param callback Callback to receive progress updates
     */
    fun setExplorationCallback(callback: ExplorationProgressCallback?)
}
