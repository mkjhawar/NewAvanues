/**
 * ConfidenceTrackingRepository.kt - Learning system for confidence scoring
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: AI Assistant
 * Created: 2025-10-09
 *
 * Tracks low-confidence commands and learns from user corrections to improve
 * future recognition accuracy. Integrates with Room database for persistence.
 */
package com.augmentalis.datamanager.repositories

import android.content.Context
import android.util.Log
import com.augmentalis.datamanager.dao.RecognitionLearningDao
import com.augmentalis.datamanager.database.VoiceOSDatabase
import com.augmentalis.datamanager.entities.RecognitionLearning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for tracking and learning from confidence-based corrections
 */
class ConfidenceTrackingRepository(context: Context) {

    companion object {
        private const val TAG = "ConfidenceTracking"
        private const val TYPE_CONFIDENCE_CORRECTION = "confidence_correction"
        private const val TYPE_LOW_CONFIDENCE_PATTERN = "low_confidence_pattern"
        private const val MIN_USAGE_COUNT_FOR_BOOST = 3
        private const val MAX_BOOST_AMOUNT = 0.15f // Max 15% confidence boost
    }

    private val database = VoiceOSDatabase.getInstance(context)
    private val recognitionLearningDao: RecognitionLearningDao = database.recognitionLearningDao()

    /**
     * Record a low-confidence command that was corrected by the user
     *
     * @param recognizedText What the engine recognized
     * @param correctedCommand What the user actually meant
     * @param originalConfidence The original confidence score
     * @param engine Which engine was used
     */
    suspend fun recordCorrection(
        recognizedText: String,
        correctedCommand: String,
        originalConfidence: Float,
        engine: String
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Recording correction: '$recognizedText' -> '$correctedCommand' (confidence: $originalConfidence)")

            // Check if this correction already exists
            val existing = recognitionLearningDao.findByKeyAndEngine(
                recognizedText.lowercase(),
                engine
            )

            if (existing != null) {
                // Update existing correction
                val updated = existing.copy(
                    mappedValue = correctedCommand,
                    confidence = maxOf(originalConfidence, existing.confidence),
                    lastUsed = System.currentTimeMillis(),
                    usageCount = existing.usageCount + 1
                )
                recognitionLearningDao.update(updated)
                Log.d(TAG, "Updated existing correction (usage count: ${updated.usageCount})")
            } else {
                // Create new correction
                val newLearning = RecognitionLearning(
                    engine = engine,
                    type = TYPE_CONFIDENCE_CORRECTION,
                    keyValue = recognizedText.lowercase(),
                    mappedValue = correctedCommand,
                    confidence = originalConfidence,
                    timestamp = System.currentTimeMillis(),
                    lastUsed = System.currentTimeMillis(),
                    usageCount = 1
                )
                recognitionLearningDao.insert(newLearning)
                Log.d(TAG, "Created new correction learning entry")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record correction", e)
        }
    }

    /**
     * Record a low-confidence pattern (command that consistently has low confidence)
     *
     * @param command The command text
     * @param confidence Average confidence for this command
     * @param engine Which engine was used
     */
    suspend fun recordLowConfidencePattern(
        command: String,
        confidence: Float,
        engine: String
    ) = withContext(Dispatchers.IO) {
        try {
            val existing = recognitionLearningDao.findByKeyAndEngineAndType(
                command.lowercase(),
                engine,
                TYPE_LOW_CONFIDENCE_PATTERN
            )

            if (existing != null) {
                // Update with new average confidence
                val newAverage = (existing.confidence * existing.usageCount + confidence) /
                        (existing.usageCount + 1)

                val updated = existing.copy(
                    confidence = newAverage,
                    lastUsed = System.currentTimeMillis(),
                    usageCount = existing.usageCount + 1
                )
                recognitionLearningDao.update(updated)
            } else {
                val newPattern = RecognitionLearning(
                    engine = engine,
                    type = TYPE_LOW_CONFIDENCE_PATTERN,
                    keyValue = command.lowercase(),
                    mappedValue = "", // Not used for patterns
                    confidence = confidence,
                    timestamp = System.currentTimeMillis(),
                    lastUsed = System.currentTimeMillis(),
                    usageCount = 1
                )
                recognitionLearningDao.insert(newPattern)
            }

            Log.d(TAG, "Recorded low confidence pattern for '$command'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record low confidence pattern", e)
        }
    }

    /**
     * Get learned correction for a recognized text
     *
     * @param recognizedText The text that was recognized
     * @param engine Which engine was used
     * @return The corrected command, or null if no learning exists
     */
    suspend fun getCorrection(
        recognizedText: String,
        engine: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val learning = recognitionLearningDao.findByKeyAndEngineAndType(
                recognizedText.lowercase(),
                engine,
                TYPE_CONFIDENCE_CORRECTION
            )

            if (learning != null && learning.usageCount >= MIN_USAGE_COUNT_FOR_BOOST) {
                // Update last used timestamp
                recognitionLearningDao.update(
                    learning.copy(lastUsed = System.currentTimeMillis())
                )
                return@withContext learning.mappedValue
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get correction", e)
            null
        }
    }

    /**
     * Calculate confidence boost for a command based on learning history
     *
     * @param command The command text
     * @param baseConfidence The original confidence score
     * @param engine Which engine was used
     * @return Boosted confidence score
     */
    suspend fun applyLearningBoost(
        command: String,
        baseConfidence: Float,
        engine: String
    ): Float = withContext(Dispatchers.IO) {
        try {
            // Check if this command has a positive learning history
            val correction = recognitionLearningDao.findByKeyAndEngineAndType(
                command.lowercase(),
                engine,
                TYPE_CONFIDENCE_CORRECTION
            )

            if (correction != null && correction.usageCount >= MIN_USAGE_COUNT_FOR_BOOST) {
                // Apply boost based on usage frequency
                val boostMultiplier = minOf(
                    correction.usageCount.toFloat() / 10f,
                    1f
                )
                val boost = MAX_BOOST_AMOUNT * boostMultiplier

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

    /**
     * Get all low-confidence commands for analysis
     *
     * @param engine Optional engine filter
     * @param minUsageCount Minimum usage count to include
     * @return Flow of low-confidence learning entries
     */
    fun getLowConfidenceCommands(
        engine: String? = null,
        minUsageCount: Int = 1
    ): Flow<List<RecognitionLearning>> {
        val flow = if (engine != null) {
            recognitionLearningDao.findByEngineAndTypeFlow(engine, TYPE_LOW_CONFIDENCE_PATTERN)
        } else {
            recognitionLearningDao.findByTypeFlow(TYPE_LOW_CONFIDENCE_PATTERN)
        }

        // Filter by minimum usage count
        return flow.map { list ->
            list.filter { it.usageCount >= minUsageCount }
        }
    }

    /**
     * Get all corrections for analysis
     *
     * @param engine Optional engine filter
     * @return Flow of correction learning entries
     */
    fun getCorrections(engine: String? = null): Flow<List<RecognitionLearning>> {
        return if (engine != null) {
            recognitionLearningDao.findByEngineAndTypeFlow(engine, TYPE_CONFIDENCE_CORRECTION)
        } else {
            recognitionLearningDao.findByTypeFlow(TYPE_CONFIDENCE_CORRECTION)
        }
    }

    /**
     * Get confidence statistics for a specific command
     *
     * @param command The command to analyze
     * @param engine Which engine to check
     * @return ConfidenceStats or null if not found
     */
    suspend fun getConfidenceStats(
        command: String,
        engine: String
    ): ConfidenceStats? = withContext(Dispatchers.IO) {
        try {
            val pattern = recognitionLearningDao.findByKeyAndEngineAndType(
                command.lowercase(),
                engine,
                TYPE_LOW_CONFIDENCE_PATTERN
            )

            if (pattern != null) {
                ConfidenceStats(
                    command = command,
                    averageConfidence = pattern.confidence,
                    usageCount = pattern.usageCount,
                    lastUsed = pattern.lastUsed,
                    needsAttention = pattern.confidence < 0.7f && pattern.usageCount >= 5
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get confidence stats", e)
            null
        }
    }

    /**
     * Clean up old learning entries that haven't been used recently
     *
     * @param maxAgeDays Maximum age in days for entries to keep
     * @param minUsageCount Minimum usage count to preserve
     */
    suspend fun cleanupOldEntries(
        maxAgeDays: Int = 90,
        minUsageCount: Int = 3
    ) = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)

            val allEntries = recognitionLearningDao.getAll()
            var deletedCount = 0

            allEntries.forEach { entry ->
                if (entry.lastUsed < cutoffTime && entry.usageCount < minUsageCount) {
                    recognitionLearningDao.delete(entry)
                    deletedCount++
                }
            }

            Log.i(TAG, "Cleaned up $deletedCount old learning entries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old entries", e)
        }
    }

    /**
     * Export learning data for backup or analysis
     *
     * @param engine Optional engine filter
     * @return List of all learning entries
     */
    suspend fun exportLearningData(engine: String? = null): List<RecognitionLearning> =
        withContext(Dispatchers.IO) {
            try {
                if (engine != null) {
                    recognitionLearningDao.findByEngine(engine)
                } else {
                    recognitionLearningDao.getAll()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export learning data", e)
                emptyList()
            }
        }

    /**
     * Import learning data from backup
     *
     * @param entries Learning entries to import
     * @param mergeStrategy How to handle conflicts (replace/merge/skip)
     */
    suspend fun importLearningData(
        entries: List<RecognitionLearning>,
        mergeStrategy: MergeStrategy = MergeStrategy.MERGE
    ) = withContext(Dispatchers.IO) {
        try {
            var importedCount = 0
            var mergedCount = 0
            var skippedCount = 0

            entries.forEach { entry ->
                val existing = recognitionLearningDao.findByKeyAndEngineAndType(
                    entry.keyValue,
                    entry.engine,
                    entry.type
                )

                when {
                    existing == null -> {
                        recognitionLearningDao.insert(entry)
                        importedCount++
                    }
                    mergeStrategy == MergeStrategy.REPLACE -> {
                        recognitionLearningDao.update(entry.copy(id = existing.id))
                        importedCount++
                    }
                    mergeStrategy == MergeStrategy.MERGE -> {
                        val merged = existing.copy(
                            confidence = maxOf(existing.confidence, entry.confidence),
                            usageCount = existing.usageCount + entry.usageCount,
                            lastUsed = maxOf(existing.lastUsed, entry.lastUsed)
                        )
                        recognitionLearningDao.update(merged)
                        mergedCount++
                    }
                    else -> {
                        skippedCount++
                    }
                }
            }

            Log.i(TAG, "Import complete: imported=$importedCount, merged=$mergedCount, skipped=$skippedCount")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import learning data", e)
        }
    }

    /**
     * Get overall confidence improvement metrics
     */
    suspend fun getImprovementMetrics(engine: String? = null): ImprovementMetrics =
        withContext(Dispatchers.IO) {
            try {
                val corrections = if (engine != null) {
                    recognitionLearningDao.findByEngineAndType(engine, TYPE_CONFIDENCE_CORRECTION)
                } else {
                    recognitionLearningDao.findByType(TYPE_CONFIDENCE_CORRECTION)
                }

                val patterns = if (engine != null) {
                    recognitionLearningDao.findByEngineAndType(engine, TYPE_LOW_CONFIDENCE_PATTERN)
                } else {
                    recognitionLearningDao.findByType(TYPE_LOW_CONFIDENCE_PATTERN)
                }

                ImprovementMetrics(
                    totalCorrections = corrections.size,
                    totalLowConfidenceCommands = patterns.size,
                    averageCorrectionsPerCommand = if (corrections.isNotEmpty()) {
                        corrections.sumOf { it.usageCount }.toFloat() / corrections.size
                    } else 0f,
                    averageLowConfidence = if (patterns.isNotEmpty()) {
                        patterns.sumOf { it.confidence.toDouble() }.toFloat() / patterns.size
                    } else 0f,
                    commandsNeedingAttention = patterns.count {
                        it.confidence < 0.7f && it.usageCount >= 5
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get improvement metrics", e)
                ImprovementMetrics()
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
