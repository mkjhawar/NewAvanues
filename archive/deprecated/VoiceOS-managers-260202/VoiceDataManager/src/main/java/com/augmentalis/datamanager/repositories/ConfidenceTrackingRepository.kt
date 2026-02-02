/**
 * ConfidenceTrackingRepository.kt - Learning system for confidence scoring
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: AI Assistant
 * Created: 2025-10-09
 * Updated: 2025-11-25 - Migrated from Room to SQLDelight
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
        private const val TYPE_CONFIDENCE_CORRECTION = "confidence_correction"
        private const val TYPE_LOW_CONFIDENCE_PATTERN = "low_confidence_pattern"
        private const val MIN_USAGE_COUNT_FOR_BOOST = 3
        private const val MAX_BOOST_AMOUNT = 0.15f // Max 15% confidence boost
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
                val boost = MAX_BOOST_AMOUNT
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

/**
 * Statistics for a specific command's confidence
 */
data class ConfidenceStats(
    val command: String,
    val averageConfidence: Float,
    val usageCount: Int,
    val lastUsed: Long,
    val needsAttention: Boolean
)

/**
 * Overall improvement metrics
 */
data class ImprovementMetrics(
    val totalCorrections: Int = 0,
    val totalLowConfidenceCommands: Int = 0,
    val averageCorrectionsPerCommand: Float = 0f,
    val averageLowConfidence: Float = 0f,
    val commandsNeedingAttention: Int = 0
)

/**
 * Strategy for merging imported learning data
 */
enum class MergeStrategy {
    REPLACE,  // Replace existing entries with imported ones
    MERGE,    // Merge usage counts and use best confidence
    SKIP      // Skip entries that already exist
}
