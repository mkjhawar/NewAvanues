/**
 * ConfidenceTrackingRepository.kt - Learning system for confidence scoring
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-09
 * Updated: 2026-01-28 - Migrated to KMP structure, uses common data models
 *
 * Tracks low-confidence commands and learns from user corrections to improve
 * future recognition accuracy. Integrates with SQLDelight database for persistence.
 *
 * NOTE: This repository needs full reimplementation for SQLDelight.
 * The RecognitionLearningRepository already provides the core functionality.
 * This class can be refactored to use RecognitionLearningRepository or removed.
 */
package com.augmentalis.datamanager.repositories

import android.content.Context
import android.util.Log
import com.augmentalis.datamanager.ConfidenceConstants
import com.augmentalis.datamanager.LearningTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for tracking and learning from confidence-based corrections
 *
 * TODO: Reimplement using RecognitionLearningRepository or remove if redundant
 */
class ConfidenceTrackingRepository(private val context: Context) {

    companion object {
        private const val TAG = "ConfidenceTracking"
    }

    // Delegate to RecognitionLearningRepository
    private val recognitionLearningRepo = RecognitionLearningRepository.getInstance(context)

    /**
     * Record a low-confidence command that was corrected by the user
     */
    suspend fun recordCorrection(
        recognizedText: String,
        correctedCommand: String,
        originalConfidence: Float,
        engine: String
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Recording correction: '$recognizedText' -> '$correctedCommand' (confidence: $originalConfidence)")
            // Use RecognitionLearningRepository
            recognitionLearningRepo.saveLearnedCommand(engine, recognizedText, correctedCommand, originalConfidence)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record correction", e)
        }
    }

    /**
     * Get learned correction for a recognized text
     */
    suspend fun getCorrection(
        recognizedText: String,
        engine: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            recognitionLearningRepo.getLearnedCommand(engine, recognizedText)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get correction", e)
            null
        }
    }

    /**
     * Calculate confidence boost for a command based on learning history
     */
    suspend fun applyLearningBoost(
        command: String,
        baseConfidence: Float,
        engine: String
    ): Float = withContext(Dispatchers.IO) {
        try {
            // Check if learned command exists
            val hasLearned = recognitionLearningRepo.hasLearnedCommand(engine, command)
            if (hasLearned) {
                val boost = ConfidenceConstants.MAX_BOOST_AMOUNT
                val boostedConfidence = (baseConfidence + boost).coerceIn(0f, 1f)
                Log.d(TAG, "Applied learning boost to '$command': " +
                        "${(baseConfidence * 100).toInt()}% -> ${(boostedConfidence * 100).toInt()}%")
                return@withContext boostedConfidence
            }
            baseConfidence
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply learning boost", e)
            baseConfidence
        }
    }
}
// Data classes ConfidenceStats, ImprovementMetrics, MergeStrategy moved to commonMain DataModels.kt
