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

package com.augmentalis.learnapp.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Learned App Entity
 *
 * Room entity storing metadata about learned apps.
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
@Entity(tableName = "learned_apps")
data class LearnedAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "version_code")
    val versionCode: Long,

    @ColumnInfo(name = "version_name")
    val versionName: String,

    @ColumnInfo(name = "first_learned_at")
    val firstLearnedAt: Long,

    @ColumnInfo(name = "last_updated_at")
    val lastUpdatedAt: Long,

    @ColumnInfo(name = "total_screens")
    val totalScreens: Int,

    @ColumnInfo(name = "total_elements")
    val totalElements: Int,

    @ColumnInfo(name = "app_hash")
    val appHash: String,

    @ColumnInfo(name = "exploration_status")
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
