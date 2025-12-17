/**
 * ExplorationSessionEntity.kt - Room entity for exploration sessions
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/database/entities/ExplorationSessionEntity.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Room database entity for storing exploration session data
 */

package com.augmentalis.learnapp.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Exploration Session Entity
 *
 * Room entity storing exploration session metadata.
 *
 * @property sessionId Session ID (primary key)
 * @property packageName Package name (foreign key)
 * @property startedAt When session started (timestamp)
 * @property completedAt When session completed (timestamp, null if running)
 * @property durationMs Duration in milliseconds
 * @property screensExplored Number of screens explored
 * @property elementsDiscovered Number of elements discovered
 * @property status Session status (RUNNING, COMPLETED, PAUSED, FAILED)
 *
 * @since 1.0.0
 */
@Entity(
    tableName = "exploration_sessions",
    foreignKeys = [
        ForeignKey(
            entity = LearnedAppEntity::class,
            parentColumns = ["package_name"],
            childColumns = ["package_name"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("package_name")]
)
data class ExplorationSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "started_at")
    val startedAt: Long,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long? = null,

    @ColumnInfo(name = "screens_explored")
    val screensExplored: Int,

    @ColumnInfo(name = "elements_discovered")
    val elementsDiscovered: Int,

    @ColumnInfo(name = "status")
    val status: String  // RUNNING, COMPLETED, PAUSED, FAILED
)

/**
 * Session Status
 */
object SessionStatus {
    const val RUNNING = "RUNNING"
    const val COMPLETED = "COMPLETED"
    const val PAUSED = "PAUSED"
    const val FAILED = "FAILED"
}
