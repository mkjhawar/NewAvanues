/**
 * ExplorationSessionEntity.kt - Room entity for exploration sessions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 *
 * Room database entity for storing exploration session data
 */

package com.augmentalis.voiceoscore.learnapp.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Exploration Session Entity
 *
 * Room entity storing exploration session metadata.
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
