/**
 * LearnedAppEntity.kt - Data model for learned apps
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 * Migrated to SQLDelight: 2025-12-17
 *
 * Data model for storing learned app metadata
 * Uses SQLDelight schema from core/database module
 */

package com.augmentalis.voiceoscore.learnapp.database.entities

/**
 * Learned App Data Model
 *
 * Data model storing metadata about learned apps.
 * Corresponds to learned_apps table in SQLDelight schema.
 */
data class LearnedAppEntity(
    val packageName: String,
    val appName: String,
    val versionCode: Long,
    val versionName: String,
    val firstLearnedAt: Long,
    val lastUpdatedAt: Long,
    val totalScreens: Int,
    val totalElements: Int,
    val appHash: String,
    val explorationStatus: String  // COMPLETE, PARTIAL, FAILED
)

/**
 * Exploration Status
 */
object ExplorationStatus {
    const val COMPLETE = "COMPLETE"
    const val PARTIAL = "PARTIAL"
    const val FAILED = "FAILED"
}
