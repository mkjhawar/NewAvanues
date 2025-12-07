/**
 * StateDetectionPipeline.kt - Orchestrates multiple state detectors
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Runs all state detectors and returns the best match.
 * Follows Strategy + Pipeline patterns for extensibility.
 *
 * SOLID Principles:
 * - Open/Closed: Add new detectors without modifying pipeline
 * - Dependency Inversion: Depends on StateDetectionStrategy interface
 */
package com.augmentalis.voiceoscore.learnapp.state

/**
 * Pipeline for running multiple state detectors
 *
 * Orchestrates detection by running all registered detectors and selecting
 * the result with highest confidence. Supports filtering by minimum confidence.
 */
class StateDetectionPipeline(
    private val detectors: List<StateDetectionStrategy>
) {

    companion object {
        /**
         * Default minimum confidence threshold
         * States below this confidence are considered UNKNOWN
         */
        const val DEFAULT_MIN_CONFIDENCE = 0.7f
    }

    /**
     * Detect state using all registered detectors
     *
     * Runs all detectors in parallel (conceptually) and returns the detection
     * with highest confidence. If no detection meets minimum confidence threshold,
     * returns READY state as default.
     *
     * @param context Detection context with UI data
     * @param minConfidence Minimum confidence threshold (default: 0.7)
     * @return Best detection result
     */
    fun detectState(
        context: StateDetectionContext,
        minConfidence: Float = DEFAULT_MIN_CONFIDENCE
    ): StateDetectionResult {

        // Run all detectors
        val allResults = detectors.map { detector ->
            detector.detect(context)
        }

        // Find best result (highest confidence)
        val bestResult = allResults.maxByOrNull { it.confidence }

        // Check if best result meets minimum confidence
        return if (bestResult != null && bestResult.confidence >= minConfidence) {
            bestResult
        } else {
            // Default to READY if no confident detection
            StateDetectionResult(
                state = AppState.READY,
                confidence = 0.6f,
                indicators = listOf("No specific state detected - assuming ready")
            )
        }
    }

    /**
     * Detect all active states (multi-state detection)
     *
     * Returns ALL states that meet minimum confidence threshold,
     * sorted by confidence descending. Enables handling of multi-state
     * scenarios like ERROR + DIALOG or LOADING + DIALOG.
     *
     * @param context Detection context with UI data
     * @param minConfidence Minimum confidence threshold (default: 0.5 for multi-state)
     * @return List of detected states, sorted by confidence
     */
    fun detectAllStates(
        context: StateDetectionContext,
        minConfidence: Float = 0.5f
    ): List<StateDetectionResult> {

        // Run all detectors
        val allResults = detectors.map { detector ->
            detector.detect(context)
        }

        // Filter by confidence and sort
        val activeStates = allResults
            .filter { it.confidence >= minConfidence }
            .sortedByDescending { it.confidence }

        // If no confident states, return READY as default
        return if (activeStates.isEmpty()) {
            listOf(
                StateDetectionResult(
                    state = AppState.READY,
                    confidence = 0.6f,
                    indicators = listOf("No specific state detected - assuming ready")
                )
            )
        } else {
            activeStates
        }
    }

    /**
     * Get count of registered detectors
     *
     * @return Number of detectors in pipeline
     */
    fun getDetectorCount(): Int = detectors.size

    /**
     * Check if detector for specific state is registered
     *
     * @param state State to check
     * @return True if detector for this state exists
     */
    fun hasDetectorFor(state: AppState): Boolean {
        return detectors.any { it.targetState == state }
    }
}
