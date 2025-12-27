/**
 * LearnedAppEntity.kt - Room entity for learned apps
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/database/entities/LearnedAppEntity.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Room database entity for storing learned app metadata
 */

package com.augmentalis.voiceoscore.learnapp.database.entities


/**
 * Learned App Entity
 *
 * Data class storing metadata about learned apps (Room removed, now using SQLDelight).
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
 *
 * @since 1.0.0
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
