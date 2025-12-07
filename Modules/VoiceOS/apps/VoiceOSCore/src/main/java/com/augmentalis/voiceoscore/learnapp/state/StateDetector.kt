/**
 * StateDetector.kt - Interface for state detection implementations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13 01:41:12 PDT
 *
 * Defines the contract for state detector implementations following SOLID principles.
 * Each concrete detector is responsible for detecting a single app state.
 */
package com.augmentalis.voiceoscore.learnapp.state

/**
 * Interface for state detection implementations
 *
 * Each detector is responsible for analyzing UI patterns and determining
 * if a specific app state is present with a confidence score.
 */
interface StateDetector {

    /**
     * The app state this detector is responsible for detecting
     */
    val targetState: AppState

    /**
     * Detect the presence of the target state
     *
     * @param textContent List of text strings extracted from the UI
     * @param viewIds List of view resource IDs from the UI
     * @param classNames List of class names from the UI
     * @return Detection result with confidence score and indicators
     */
    fun detect(
        textContent: List<String>,
        viewIds: List<String>,
        classNames: List<String>
    ): StateDetectionResult

    /**
     * Scoring weights for different signal types
     *
     * These can be overridden by implementations to tune detection behavior
     */
    companion object {
        const val WEIGHT_FRAMEWORK_CLASS = 0.6f  // Highest confidence - framework classes are authoritative
        const val WEIGHT_RESOURCE_ID = 0.3f      // High confidence - developer intent
        const val WEIGHT_TEXT_KEYWORD = 0.25f    // Moderate confidence - can be localized
        const val WEIGHT_CLASS_NAME = 0.2f       // Lower confidence - generic patterns
        const val WEIGHT_CONTEXTUAL = 0.15f      // Lowest - supporting evidence
    }
}
