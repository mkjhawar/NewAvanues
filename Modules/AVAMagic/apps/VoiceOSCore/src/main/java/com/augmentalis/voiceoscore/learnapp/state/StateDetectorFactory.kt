/**
 * StateDetectorFactory.kt - Factory for creating state detection pipelines
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13 02:33:18 PDT
 *
 * Factory for creating state detection pipelines with default or custom
 * detector configurations. Uses singleton pattern for shared instances.
 */
package com.augmentalis.voiceoscore.learnapp.state

import com.augmentalis.voiceoscore.learnapp.state.detectors.*

/**
 * Factory for creating state detection pipelines
 *
 * Provides methods to create pipelines with default or custom detector
 * configurations. Manages singleton instances for efficiency.
 */
object StateDetectorFactory {

    /**
     * Lazy singleton instance of default pipeline
     */
    private val defaultPipelineInstance: StateDetectionPipeline by lazy {
        createDefaultPipeline()
    }

    /**
     * Create default pipeline with all standard detectors
     *
     * Creates a new pipeline instance with all 7 standard state detectors:
     * - LoginStateDetector
     * - LoadingStateDetector
     * - ErrorStateDetector
     * - PermissionStateDetector
     * - TutorialStateDetector
     * - EmptyStateDetector
     * - DialogStateDetector
     *
     * @return New pipeline instance
     */
    fun createDefaultPipeline(): StateDetectionPipeline {
        return StateDetectionPipeline(
            detectors = listOf(
                LoginStateDetector(),
                LoadingStateDetector(),
                ErrorStateDetector(),
                PermissionStateDetector(),
                TutorialStateDetector(),
                EmptyStateDetector(),
                DialogStateDetector()
            )
        )
    }

    /**
     * Get shared default pipeline instance
     *
     * Returns singleton instance for efficiency. Use this for most cases
     * unless you need custom detector configuration.
     *
     * @return Shared pipeline instance
     */
    fun getDefaultPipeline(): StateDetectionPipeline {
        return defaultPipelineInstance
    }

    /**
     * Create pipeline with custom detectors
     *
     * Use this when you need a custom detector configuration,
     * such as excluding certain detectors or adding custom ones.
     *
     * @param detectors List of detector implementations
     * @return New pipeline instance with custom detectors
     */
    fun createPipeline(detectors: List<StateDetectionStrategy>): StateDetectionPipeline {
        require(detectors.isNotEmpty()) {
            "Detector list cannot be empty"
        }
        return StateDetectionPipeline(detectors)
    }

    /**
     * Create pipeline with specific detector types
     *
     * Convenience method to create pipeline with only specific state types.
     * Useful for testing or specialized detection scenarios.
     *
     * @param states List of app states to detect
     * @return New pipeline instance with selected detectors
     */
    fun createPipelineForStates(states: List<AppState>): StateDetectionPipeline {
        require(states.isNotEmpty()) {
            "State list cannot be empty"
        }

        val detectors = states.mapNotNull { state ->
            when (state) {
                AppState.LOGIN -> LoginStateDetector()
                AppState.LOADING -> LoadingStateDetector()
                AppState.ERROR -> ErrorStateDetector()
                AppState.PERMISSION -> PermissionStateDetector()
                AppState.TUTORIAL -> TutorialStateDetector()
                AppState.EMPTY_STATE -> EmptyStateDetector()
                AppState.DIALOG -> DialogStateDetector()
                AppState.READY, AppState.UNKNOWN, AppState.BACKGROUND -> null
            }
        }

        require(detectors.isNotEmpty()) {
            "No valid detectors created for states: $states"
        }

        return StateDetectionPipeline(detectors)
    }

    /**
     * Get all available detector types
     *
     * @return List of all app states that have detectors
     */
    fun getAvailableDetectorTypes(): List<AppState> {
        return listOf(
            AppState.LOGIN,
            AppState.LOADING,
            AppState.ERROR,
            AppState.PERMISSION,
            AppState.TUTORIAL,
            AppState.EMPTY_STATE,
            AppState.DIALOG
        )
    }
}
