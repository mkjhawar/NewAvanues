/**
 * ExplorationSessionEntity.kt - Data class for exploration sessions
 * Path: apps/LearnApp/src/main/java/com/augmentalis/learnapp/database/entities/ExplorationSessionEntity.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 * Updated: 2025-12-18 (Migrated from Room to SQLDelight)
 *
 * Data class for storing exploration session data
 */

package com.augmentalis.learnapp.database.entities

/**
 * Exploration Session Entity
 *
 * Data class storing exploration session metadata.
 * Used with SQLDelight database via core:database module.
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
data class ExplorationSessionEntity(
    val sessionId: String,
    val packageName: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val durationMs: Long? = null,
    val screensExplored: Int,
    val elementsDiscovered: Int,
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
