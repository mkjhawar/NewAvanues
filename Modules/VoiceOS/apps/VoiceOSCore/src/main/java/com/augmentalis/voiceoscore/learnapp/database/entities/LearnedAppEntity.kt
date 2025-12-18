/**
 * LearnedAppEntity.kt - Data class for learned apps (SQLDelight compatible)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-18
 *
 * Data class representing learned app metadata.
 * Used by SQLDelight adapter pattern (not Room).
 */

package com.augmentalis.voiceoscore.learnapp.database.entities

/**
 * Learned App Entity
 *
 * Data class storing metadata about learned apps.
 * This is a pure Kotlin data class without Room annotations.
 * SQLDelight handles database operations via LearnAppDatabaseAdapter.
 *
 * @property packageName Package name (primary key)
 * @property appName Human-readable app name
 * @property versionCode App version code
 * @property versionName App version name
 * @property firstLearnedAt When app was first learned (timestamp)
 * @property lastUpdatedAt When app was last updated (timestamp)
 * @property totalScreens Total screens discovered
 * @property totalElements Total elements mapped
 * @property appHash Hash of app structure (for update detection)
 * @property explorationStatus Exploration status (COMPLETE, PARTIAL, FAILED)
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
    val explorationStatus: String
)
