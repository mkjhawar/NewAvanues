/**
 * DataModels.kt - Common data models for VoiceDataManager
 *
 * Cross-platform data classes used by both Android and Desktop implementations.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.datamanager

/**
 * Data statistics model
 */
data class DataStatistics(
    val totalRecords: Int,
    val storageUsed: Long,
    val lastSync: Long,
    val dataBreakdown: Map<String, Int>,
    val retentionDays: Int,
    val autoCleanupEnabled: Boolean
)

/**
 * Storage info model
 */
data class StorageInfo(
    val databaseSize: Long,
    val availableSpace: Long,
    val storageLevel: StorageLevel,
    val percentUsed: Float
)

/**
 * Storage level enum
 */
enum class StorageLevel {
    NORMAL,
    MEDIUM,
    HIGH,
    CRITICAL
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

/**
 * Import options
 */
data class ImportOptions(
    val importPreferences: Boolean = true,
    val importCommandHistory: Boolean = true,
    val importCustomCommands: Boolean = true,
    val verifyChecksum: Boolean = false
)

/**
 * Learning type constants
 */
object LearningTypes {
    const val TYPE_LEARNED_COMMAND = "learned_command"
    const val TYPE_VOCABULARY_CACHE = "vocabulary_cache"
    const val TYPE_CONFIDENCE_CORRECTION = "confidence_correction"
    const val TYPE_LOW_CONFIDENCE_PATTERN = "low_confidence_pattern"
}

/**
 * Constants for confidence boosting
 */
object ConfidenceConstants {
    const val MIN_USAGE_COUNT_FOR_BOOST = 3
    const val MAX_BOOST_AMOUNT = 0.15f // Max 15% confidence boost
}
