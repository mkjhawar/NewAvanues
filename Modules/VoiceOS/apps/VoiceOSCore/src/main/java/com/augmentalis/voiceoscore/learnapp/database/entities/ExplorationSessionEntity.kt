/**
 * ExplorationSessionEntity.kt - Data class for exploration sessions (SQLDelight compatible)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-18
 *
 * Data class representing exploration session data.
 * Used by SQLDelight adapter pattern (not Room).
 */

package com.augmentalis.voiceoscore.learnapp.database.entities

/**
 * Exploration Session Entity
 *
 * Data class storing exploration session metadata.
 * This is a pure Kotlin data class without Room annotations.
 * SQLDelight handles database operations via LearnAppDatabaseAdapter.
 *
 * @property sessionId Session ID (primary key)
 * @property packageName Package name (foreign key)
 * @property startedAt When session started (timestamp)
 * @property completedAt When session completed (timestamp, null if running)
 * @property durationMs Duration in milliseconds
 * @property screensExplored Number of screens explored
 * @property elementsDiscovered Number of elements discovered
 * @property status Session status (RUNNING, COMPLETED, PAUSED, FAILED)
 */
data class ExplorationSessionEntity(
    val sessionId: String,
    val packageName: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val durationMs: Long? = null,
    val screensExplored: Int,
    val elementsDiscovered: Int,
    val status: String
)
