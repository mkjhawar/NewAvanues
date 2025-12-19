/**
 * ScreenStateEntity.kt - Data model for screen states
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 * Migrated to SQLDelight: 2025-12-17
 *
 * Data model for storing screen state data
 * Uses SQLDelight schema from core/database module
 */

package com.augmentalis.voiceoscore.learnapp.database.entities

/**
 * Screen State Data Model
 *
 * Data model storing screen state metadata.
 * Corresponds to screen_states table in SQLDelight schema.
 */
data class ScreenStateEntity(
    val screenHash: String,
    val packageName: String,
    val activityName: String? = null,
    val fingerprint: String,
    val elementCount: Int,
    val discoveredAt: Long
)
