/**
 * UUIDAnalyticsEntity.kt - Room entity for UUID analytics storage
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/entities/UUIDAnalyticsEntity.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Room entity for tracking usage analytics and performance metrics
 */

package com.augmentalis.uuidcreator.database.entities

import androidx.room.*

/**
 * Room entity for UUID analytics tracking
 *
 * Tracks usage patterns, performance metrics, and lifecycle state for each UUID element.
 */
@Entity(
    tableName = "uuid_analytics",
    indices = [
        Index(value = ["access_count"], name = "idx_analytics_access_count"),
        Index(value = ["last_accessed"], name = "idx_analytics_last_accessed"),
        Index(value = ["lifecycle_state"], name = "idx_analytics_lifecycle")
    ],
    foreignKeys = [
        ForeignKey(
            entity = UUIDElementEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["uuid"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UUIDAnalyticsEntity(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    val uuid: String,

    @ColumnInfo(name = "access_count")
    val accessCount: Long = 0,

    @ColumnInfo(name = "first_accessed")
    val firstAccessed: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "execution_time_ms")
    val executionTimeMs: Long = 0,  // Cumulative execution time

    @ColumnInfo(name = "success_count")
    val successCount: Long = 0,

    @ColumnInfo(name = "failure_count")
    val failureCount: Long = 0,

    @ColumnInfo(name = "lifecycle_state")
    val lifecycleState: String = LifecycleState.CREATED.name  // created, active, deprecated, deleted
) {
    /**
     * Lifecycle states for UUID elements
     */
    enum class LifecycleState {
        CREATED,     // Newly registered
        ACTIVE,      // Being used regularly
        DEPRECATED,  // Marked for removal
        DELETED      // Soft delete before cascade
    }
}
