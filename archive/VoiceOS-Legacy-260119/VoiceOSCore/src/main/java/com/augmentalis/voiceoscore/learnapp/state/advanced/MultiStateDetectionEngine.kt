/**
 * MultiStateDetectionEngine.kt - Detects multiple simultaneous states
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Extends state detection to handle multiple simultaneous states.
 * For example, an app can be in ERROR state while also having a DIALOG open.
 * Returns ranked list of detected states based on confidence scores.
 */
package com.augmentalis.voiceoscore.learnapp.state.advanced

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import com.augmentalis.voiceoscore.learnapp.state.AppState
import com.augmentalis.voiceoscore.learnapp.state.StateDetectionResult

/**
 * Multi-state detection result with ranked states
 */
data class MultiStateResult(
    val primaryState: StateDetectionResult,
    val secondaryStates: List<StateDetectionResult>,
    val allStates: List<StateDetectionResult>,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get all confident states (above threshold)
     */
    fun getConfidentStates(threshold: Float = 0.7f): List<StateDetectionResult> {
        return allStates.filter { it.confidence >= threshold }
    }

    /**
     * Check if specific state is present
     */
    fun hasState(state: AppState): Boolean {
        return allStates.any { it.state == state }
    }

    /**
     * Get confidence for specific state
     */
    fun getConfidenceFor(state: AppState): Float {
        return allStates.find { it.state == state }?.confidence ?: 0f
    }

    /**
     * Get human-readable description
     */
    fun getDescription(): String {
        val primary = "${primaryState.state} (${(primaryState.confidence * 100).toInt()}%)"
        val secondary = if (secondaryStates.isNotEmpty()) {
            " + " + secondaryStates.joinToString(", ") {
                "${it.state} (${(it.confidence * 100).toInt()}%)"
            }
        } else ""
        return primary + secondary
    }
}

/**
 * State combination rule
 */
data class StateCombinationRule(
    val primaryState: AppState,
    val allowedSecondaryStates: Set<AppState>,
    val conflictingStates: Set<AppState>
)

/**
 * Detects multiple simultaneous application states
 *
 * Uses parallel pattern matching to detect all applicable states,
 * then applies combination rules and confidence-based ranking to
 * return a coherent multi-state result.
 */
class MultiStateDetectionEngine(context: Context) {

    companion object {
        private const val TAG = "MultiStateDetectionEngine"

        // State combination rules
        private val COMBINATION_RULES = listOf(
            StateCombinationRule(
                primaryState = AppState.ERROR,
                allowedSecondaryStates = setOf(AppState.DIALOG, AppState.READY),
                conflictingStates = setOf(AppState.LOADING, AppState.LOGIN)
            ),
            StateCombinationRule(
                primaryState = AppState.LOADING,
                allowedSecondaryStates = setOf(AppState.DIALOG),
                conflictingStates = setOf(AppState.ERROR, AppState.READY)
            ),
            StateCombinationRule(
                primaryState = AppState.DIALOG,
                allowedSecondaryStates = setOf(
                    AppState.ERROR,
                    AppState.LOADING,
                    AppState.READY,
                    AppState.PERMISSION
                ),
                conflictingStates = setOf(AppState.LOGIN, AppState.TUTORIAL)
            ),
            StateCombinationRule(
                primaryState = AppState.PERMISSION,
                allowedSecondaryStates = setOf(AppState.DIALOG),
                conflictingStates = setOf(AppState.LOGIN, AppState.LOADING)
            ),
            StateCombinationRule(
                primaryState = AppState.LOGIN,
                allowedSecondaryStates = setOf(AppState.DIALOG, AppState.ERROR),
                conflictingStates = setOf(AppState.READY, AppState.LOADING)
            ),
            StateCombinationRule(
                primaryState = AppState.TUTORIAL,
                allowedSecondaryStates = setOf(AppState.DIALOG),
                conflictingStates = setOf(AppState.LOGIN, AppState.ERROR)
            ),
            StateCombinationRule(
                primaryState = AppState.READY,
                allowedSecondaryStates = setOf(
                    AppState.DIALOG,
                    AppState.LOADING,
                    AppState.ERROR,
                    AppState.EMPTY_STATE
                ),
                conflictingStates = setOf(AppState.LOGIN, AppState.TUTORIAL)
            ),
            StateCombinationRule(
                primaryState = AppState.EMPTY_STATE,
                allowedSecondaryStates = setOf(AppState.READY, AppState.DIALOG),
                conflictingStates = setOf(AppState.LOADING, AppState.ERROR)
            )
        )
    }

    // Developer settings (lazy initialized)
    private val developerSettings: LearnAppDeveloperSettings by lazy {
        LearnAppDeveloperSettings(context)
    }

    /**
     * Detect all applicable states in accessibility tree
     *
     * @param detections List of state detection results from individual detectors
     * @return Multi-state result with ranked states
     */
    fun detectMultipleStates(detections: List<StateDetectionResult>): MultiStateResult {
        // Sort by confidence descending
        val sortedDetections = detections
            .filter { it.confidence > 0f }
            .sortedByDescending { it.confidence }

        if (sortedDetections.isEmpty()) {
            val unknownState = StateDetectionResult(
                state = AppState.UNKNOWN,
                confidence = 0f,
                indicators = listOf("No detections")
            )
            return MultiStateResult(
                primaryState = unknownState,
                secondaryStates = emptyList(),
                allStates = listOf(unknownState)
            )
        }

        // Primary state is highest confidence
        val primaryState = sortedDetections.first()

        // Find valid secondary states
        val secondaryStates = findValidSecondaryStates(
            primaryState = primaryState,
            allDetections = sortedDetections.drop(1)
        )

        return MultiStateResult(
            primaryState = primaryState,
            secondaryStates = secondaryStates,
            allStates = listOf(primaryState) + secondaryStates
        )
    }

    /**
     * Find valid secondary states based on combination rules
     */
    private fun findValidSecondaryStates(
        primaryState: StateDetectionResult,
        allDetections: List<StateDetectionResult>
    ): List<StateDetectionResult> {
        val rule = COMBINATION_RULES.find { it.primaryState == primaryState.state }
            ?: return emptyList()

        return allDetections.filter { detection ->
            // Must meet confidence threshold
            detection.confidence >= developerSettings.getSecondaryStateConfidenceThreshold() &&
                    // Must be in allowed list
                    detection.state in rule.allowedSecondaryStates &&
                    // Must not be conflicting
                    detection.state !in rule.conflictingStates
        }
    }

    /**
     * Check if state combination is valid
     *
     * @param states List of states to check
     * @return True if combination is valid, false otherwise
     */
    fun isValidCombination(states: List<AppState>): Boolean {
        if (states.isEmpty()) return false
        if (states.size == 1) return true

        val primaryState = states.first()
        val secondaryStates = states.drop(1).toSet()

        val rule = COMBINATION_RULES.find { it.primaryState == primaryState }
            ?: return false

        // Check all secondary states are allowed
        val allAllowed = secondaryStates.all { it in rule.allowedSecondaryStates }

        // Check no conflicting states present
        val noConflicts = secondaryStates.none { it in rule.conflictingStates }

        return allAllowed && noConflicts
    }

    /**
     * Resolve state conflicts
     *
     * When conflicting states are detected, use confidence and rules to resolve
     *
     * @param detections List of potentially conflicting detections
     * @return Resolved multi-state result
     */
    fun resolveConflicts(detections: List<StateDetectionResult>): MultiStateResult {
        val multiState = detectMultipleStates(detections)

        // Check for conflicts in secondary states
        val primaryRule = COMBINATION_RULES.find { it.primaryState == multiState.primaryState.state }

        if (primaryRule != null) {
            val resolvedSecondary = multiState.secondaryStates.filter { secondary ->
                secondary.state !in primaryRule.conflictingStates
            }

            return MultiStateResult(
                primaryState = multiState.primaryState,
                secondaryStates = resolvedSecondary,
                allStates = listOf(multiState.primaryState) + resolvedSecondary,
                timestamp = multiState.timestamp
            )
        }

        return multiState
    }

    /**
     * Get possible secondary states for a primary state
     *
     * @param primaryState Primary state
     * @return Set of allowed secondary states
     */
    fun getAllowedSecondaryStates(primaryState: AppState): Set<AppState> {
        return COMBINATION_RULES.find { it.primaryState == primaryState }
            ?.allowedSecondaryStates
            ?: emptySet()
    }

    /**
     * Get conflicting states for a primary state
     *
     * @param primaryState Primary state
     * @return Set of conflicting states
     */
    fun getConflictingStates(primaryState: AppState): Set<AppState> {
        return COMBINATION_RULES.find { it.primaryState == primaryState }
            ?.conflictingStates
            ?: emptySet()
    }
}
