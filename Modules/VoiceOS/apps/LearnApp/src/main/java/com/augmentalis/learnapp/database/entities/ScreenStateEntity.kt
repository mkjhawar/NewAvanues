/**
 * ScreenStateEntity.kt - Data class for screen states
 * Path: apps/LearnApp/src/main/java/com/augmentalis/learnapp/database/entities/ScreenStateEntity.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 * Updated: 2025-12-18 (Migrated from Room to SQLDelight)
 *
 * Data class for storing screen state data
 */

package com.augmentalis.learnapp.database.entities

/**
 * Screen State Entity
 *
 * Data class storing screen state metadata.
 * Used with SQLDelight database via core:database module.
 *
 * @property screenHash Screen hash (primary key)
 * @property packageName Package name (foreign key)
 * @property activityName Activity name
 * @property fingerprint Full SHA-256 fingerprint
 * @property elementCount Number of elements on screen
 * @property discoveredAt When screen was discovered
 *
 * @since 1.0.0
 */
data class ScreenStateEntity(
    val screenHash: String,
    val packageName: String,
    val activityName: String? = null,
    val fingerprint: String,
    val elementCount: Int,
    val discoveredAt: Long
)
