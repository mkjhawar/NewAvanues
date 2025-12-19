/**
 * ExplorationSessionEntity.kt - Data model for exploration sessions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 * Migrated to SQLDelight: 2025-12-17
 *
 * Data model for storing exploration session data
 * Uses SQLDelight schema from core/database module
 */

package com.augmentalis.voiceoscore.learnapp.database.entities

/**
 * Exploration Session Data Model
 *
 * Data model storing exploration session metadata.
 * Corresponds to exploration_sessions table in SQLDelight schema.
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
