/**
 * ConfidenceCalibrator.kt - Confidence score calibration framework
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Provides weight adjustment system for confidence scores with A/B testing
 * support and metrics collection. Enables dynamic calibration and future
 * ML-based auto-tuning.
 */
package com.augmentalis.voiceoscore.learnapp.state.advanced

import com.augmentalis.voiceoscore.learnapp.state.AppState
import com.augmentalis.voiceoscore.learnapp.state.StateDetectionResult
import kotlin.math.exp

/**
 * Weight adjustment configuration for a specific indicator
 */
data class IndicatorWeight(
    val indicatorType: String,
    val weight: Float,
    val enabled: Boolean = true,
    val description: String = ""
)

/**
 * Calibration profile for a specific app state
 */
data class StateCalibrationProfile(
    val state: AppState,
    val baseThreshold: Float,
    val indicatorWeights: Map<String, Float>,
    val temporalWeight: Float = 1.0f,
    val hierarchyWeight: Float = 1.0f,
    val negativeIndicatorSensitivity: Float = 1.0f
)

/**
 * Calibration metrics for analysis
 */
data class CalibrationMetrics(
    val totalDetections: Int,
    val correctDetections: Int,
    val falsePositives: Int,
    val falseNegatives: Int,
    val avgConfidence: Float,
    val accuracyRate: Float
) {
    fun getPrecision(): Float {
        val truePositives = correctDetections
        val total = truePositives + falsePositives
        return if (total > 0) truePositives.toFloat() / total else 0f
    }

    fun getRecall(): Float {
        val truePositives = correctDetections
        val total = truePositives + falseNegatives
        return if (total > 0) truePositives.toFloat() / total else 0f
    }

    fun getF1Score(): Float {
        val precision = getPrecision()
        val recall = getRecall()
        return if (precision + recall > 0) {
            2 * (precision * recall) / (precision + recall)
        } else 0f
    }
}

/**
 * A/B test variant configuration
 */
data class CalibrationVariant(
    val name: String,
    val profiles: Map<AppState, StateCalibrationProfile>,
    val enabled: Boolean = true
)

/**
 * Calibrates and adjusts confidence scores
 *
 * Provides systematic approach to confidence score calibration with:
 * - Weight adjustment for different indicators
 * - A/B testing support for profile comparison
 * - Metrics collection for analysis
 * - Foundation for future ML-based auto-tuning
 */
class ConfidenceCalibrator {

    companion object {
        private const val TAG = "ConfidenceCalibrator"

        // Default calibration profiles for each state
        private val DEFAULT_PROFILES = mapOf(
            AppState.LOGIN to StateCalibrationProfile(
                state = AppState.LOGIN,
                baseThreshold = 0.7f,
                indicatorWeights = mapOf(
                    "text_keywords" to 0.3f,
                    "input_fields" to 0.4f,
                    "button" to 0.3f
                ),
                temporalWeight = 1.0f,
                hierarchyWeight = 1.1f,
                negativeIndicatorSensitivity = 1.2f
            ),
            AppState.LOADING to StateCalibrationProfile(
                state = AppState.LOADING,
                baseThreshold = 0.65f,
                indicatorWeights = mapOf(
                    "progress_bar" to 0.5f,
                    "text_keywords" to 0.3f,
                    "minimal_content" to 0.2f
                ),
                temporalWeight = 0.8f,
                hierarchyWeight = 1.2f,
                negativeIndicatorSensitivity = 1.5f
            ),
            AppState.ERROR to StateCalibrationProfile(
                state = AppState.ERROR,
                baseThreshold = 0.75f,
                indicatorWeights = mapOf(
                    "error_keywords" to 0.6f,
                    "retry_button" to 0.4f
                ),
                temporalWeight = 1.1f,
                hierarchyWeight = 1.0f,
                negativeIndicatorSensitivity = 1.0f
            ),
            AppState.READY to StateCalibrationProfile(
                state = AppState.READY,
                baseThreshold = 0.6f,
                indicatorWeights = mapOf(
                    "default" to 1.0f
                ),
                temporalWeight = 1.2f,
                hierarchyWeight = 0.9f,
                negativeIndicatorSensitivity = 0.8f
            )
        )
    }

    // Current active calibration profiles
    private val activeProfiles = DEFAULT_PROFILES.toMutableMap()

    // Metrics collection
    private val metricsMap = mutableMapOf<AppState, MutableList<DetectionRecord>>()

    // A/B test variants
    private val variants = mutableMapOf<String, CalibrationVariant>()
    private var activeVariantName: String? = null

    /**
     * Calibrate confidence score for a detection result
     *
     * @param result Original detection result
     * @param temporalAdjustment Adjustment from temporal validator
     * @param hierarchyAdjustment Adjustment from hierarchy analysis
     * @param negativePenalty Penalty from negative indicators
     * @return Calibrated confidence score
     */
    fun calibrateConfidence(
        result: StateDetectionResult,
        temporalAdjustment: Float = 0f,
        hierarchyAdjustment: Float = 0f,
        negativePenalty: Float = 0f
    ): Float {
        val profile = activeProfiles[result.state] ?: return result.confidence

        // Apply indicator weights
        var calibratedScore = result.confidence

        // Apply temporal adjustment
        calibratedScore += temporalAdjustment * profile.temporalWeight

        // Apply hierarchy adjustment
        calibratedScore += hierarchyAdjustment * profile.hierarchyWeight

        // Apply negative indicator penalty
        calibratedScore -= negativePenalty * profile.negativeIndicatorSensitivity

        // Clamp to valid range
        return calibratedScore.coerceIn(0f, 1f)
    }

    /**
     * Update calibration profile for a state
     *
     * @param state State to update
     * @param profile New calibration profile
     */
    fun updateProfile(state: AppState, profile: StateCalibrationProfile) {
        activeProfiles[state] = profile
    }

    /**
     * Get current calibration profile for a state
     *
     * @param state State to query
     * @return Current calibration profile or null
     */
    fun getProfile(state: AppState): StateCalibrationProfile? {
        return activeProfiles[state]
    }

    /**
     * Record detection for metrics collection
     *
     * @param result Detection result
     * @param actualState Ground truth state (if known)
     * @param correct Whether detection was correct
     */
    fun recordDetection(
        result: StateDetectionResult,
        actualState: AppState? = null,
        correct: Boolean? = null
    ) {
        val record = DetectionRecord(
            detectedState = result.state,
            confidence = result.confidence,
            actualState = actualState,
            correct = correct,
            timestamp = result.timestamp
        )

        metricsMap.getOrPut(result.state) { mutableListOf() }.add(record)
    }

    /**
     * Get calibration metrics for a state
     *
     * @param state State to analyze
     * @return Calibration metrics or null
     */
    fun getMetrics(state: AppState): CalibrationMetrics? {
        val records = metricsMap[state] ?: return null
        if (records.isEmpty()) return null

        val total = records.size
        val correct = records.count { it.correct == true }
        val falsePositives = records.count { it.correct == false && it.detectedState == state }
        val falseNegatives = records.count { it.correct == false && it.actualState == state }
        val avgConfidence = records.map { it.confidence }.average().toFloat()
        val accuracyRate = if (total > 0) correct.toFloat() / total else 0f

        return CalibrationMetrics(
            totalDetections = total,
            correctDetections = correct,
            falsePositives = falsePositives,
            falseNegatives = falseNegatives,
            avgConfidence = avgConfidence,
            accuracyRate = accuracyRate
        )
    }

    /**
     * Create A/B test variant
     *
     * @param name Variant name
     * @param profiles Calibration profiles for this variant
     */
    fun createVariant(name: String, profiles: Map<AppState, StateCalibrationProfile>) {
        variants[name] = CalibrationVariant(
            name = name,
            profiles = profiles,
            enabled = true
        )
    }

    /**
     * Switch to A/B test variant
     *
     * @param variantName Variant to activate
     * @return True if variant exists and was activated
     */
    fun activateVariant(variantName: String): Boolean {
        val variant = variants[variantName] ?: return false

        if (variant.enabled) {
            activeVariantName = variantName
            activeProfiles.putAll(variant.profiles)
            return true
        }

        return false
    }

    /**
     * Reset to default calibration profiles
     */
    fun resetToDefaults() {
        activeProfiles.clear()
        activeProfiles.putAll(DEFAULT_PROFILES)
        activeVariantName = null
    }

    /**
     * Get current active variant name
     */
    fun getActiveVariant(): String? = activeVariantName

    /**
     * Clear metrics data
     */
    fun clearMetrics() {
        metricsMap.clear()
    }

    /**
     * Export metrics for analysis
     *
     * @return Map of state to list of detection records
     */
    fun exportMetrics(): Map<AppState, List<DetectionRecord>> {
        return metricsMap.mapValues { it.value.toList() }
    }

    /**
     * Auto-tune weights based on collected metrics (future ML integration point)
     *
     * @param state State to auto-tune
     * @return Suggested calibration profile
     */
    fun autoTune(state: AppState): StateCalibrationProfile? {
        val metrics = getMetrics(state) ?: return null
        val currentProfile = activeProfiles[state] ?: return null

        // TODO: Implement ML-based auto-tuning
        // For now, return adjusted profile based on simple heuristics

        val adjustmentFactor = if (metrics.accuracyRate < 0.7f) {
            // Low accuracy: increase sensitivity
            1.1f
        } else if (metrics.accuracyRate > 0.95f) {
            // Very high accuracy: might be over-tuned, slightly decrease
            0.95f
        } else {
            // Good accuracy: no change
            1.0f
        }

        return currentProfile.copy(
            negativeIndicatorSensitivity = currentProfile.negativeIndicatorSensitivity * adjustmentFactor
        )
    }
}

/**
 * Detection record for metrics
 */
data class DetectionRecord(
    val detectedState: AppState,
    val confidence: Float,
    val actualState: AppState?,
    val correct: Boolean?,
    val timestamp: Long
)
