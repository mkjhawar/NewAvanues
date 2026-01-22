/**
 * ExplorationController.kt - Implementation of exploration control operations
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

import android.util.Log
import com.augmentalis.jitlearning.ExplorationProgress
import com.augmentalis.jitlearning.ExplorationProgressCallback
import com.augmentalis.jitlearning.JITLearnerProvider

/**
 * Exploration Controller
 *
 * Manages exploration lifecycle and state.
 *
 * Features:
 * - Start/stop exploration
 * - Pause/resume
 * - Progress tracking
 * - Callback management
 *
 * Delegates to JITLearnerProvider for actual exploration logic.
 *
 * Thread Safety: Not thread-safe, caller must synchronize
 */
class ExplorationController(
    private val learnerProvider: JITLearnerProvider
) : IExplorationControl {

    companion object {
        private const val TAG = "ExplorationController"
    }

    private var isExploringFlag = false
    private var isPausedFlag = false
    private var currentCallback: ExplorationProgressCallback? = null

    override suspend fun startExploration(
        targetPackage: String,
        callback: ExplorationProgressCallback?
    ): Boolean {
        if (isExploringFlag) {
            Log.w(TAG, "Exploration already running")
            return false
        }

        Log.i(TAG, "Starting exploration for: $targetPackage")

        currentCallback = callback
        setExplorationCallback(callback)

        val success = learnerProvider.startExploration(targetPackage)
        if (success) {
            isExploringFlag = true
            isPausedFlag = false
        }

        return success
    }

    override fun stopExploration() {
        if (!isExploringFlag) {
            Log.w(TAG, "No exploration to stop")
            return
        }

        Log.i(TAG, "Stopping exploration")

        learnerProvider.stopExploration()
        isExploringFlag = false
        isPausedFlag = false
        currentCallback = null
    }

    override fun pauseExploration() {
        if (!isExploringFlag) {
            Log.w(TAG, "No exploration to pause")
            return
        }

        if (isPausedFlag) {
            Log.w(TAG, "Exploration already paused")
            return
        }

        Log.i(TAG, "Pausing exploration")

        learnerProvider.pauseExploration()
        isPausedFlag = true
    }

    override fun resumeExploration() {
        if (!isExploringFlag) {
            Log.w(TAG, "No exploration to resume")
            return
        }

        if (!isPausedFlag) {
            Log.w(TAG, "Exploration not paused")
            return
        }

        Log.i(TAG, "Resuming exploration")

        learnerProvider.resumeExploration()
        isPausedFlag = false
    }

    override fun isExploring(): Boolean = isExploringFlag

    override fun isPaused(): Boolean = isPausedFlag

    override fun getExplorationProgress(): ExplorationProgress {
        return learnerProvider.getExplorationProgress()
    }

    override fun setExplorationCallback(callback: ExplorationProgressCallback?) {
        currentCallback = callback
        learnerProvider.setExplorationCallback(callback)
    }
}
