/**
 * ScreenStateEntity.kt - Data class for screen states (SQLDelight compatible)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-18
 *
 * Data class representing screen state data.
 * Used by SQLDelight adapter pattern (not Room).
 */

package com.augmentalis.voiceoscore.learnapp.database.entities

/**
 * Screen State Entity
 *
 * Data class storing screen state metadata.
 * This is a pure Kotlin data class without Room annotations.
 * SQLDelight handles database operations via LearnAppDatabaseAdapter.
 *
 * @property screenHash Screen hash (primary key)
 * @property packageName Package name (foreign key)
 * @property activityName Activity name
 * @property fingerprint Full SHA-256 fingerprint
 * @property elementCount Number of elements on screen
 * @property discoveredAt When screen was discovered
 */
data class ScreenStateEntity(
    val screenHash: String,
    val packageName: String,
    val activityName: String? = null,
    val fingerprint: String,
    val elementCount: Int,
    val discoveredAt: Long
)
