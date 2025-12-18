/**
 * NavigationEdgeEntity.kt - Data class for navigation edges (SQLDelight compatible)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-18
 *
 * Data class representing navigation graph edges.
 * Used by SQLDelight adapter pattern (not Room).
 */

package com.augmentalis.voiceoscore.learnapp.database.entities

/**
 * Navigation Edge Entity
 *
 * Data class storing navigation graph edges (screen transitions).
 * This is a pure Kotlin data class without Room annotations.
 * SQLDelight handles database operations via LearnAppDatabaseAdapter.
 *
 * @property edgeId Edge ID (primary key)
 * @property packageName Package name (foreign key)
 * @property sessionId Session ID (foreign key)
 * @property fromScreenHash Source screen hash
 * @property clickedElementUuid Clicked element UUID
 * @property toScreenHash Destination screen hash
 * @property timestamp When edge was discovered
 */
data class NavigationEdgeEntity(
    val edgeId: String,
    val packageName: String,
    val sessionId: String,
    val fromScreenHash: String,
    val clickedElementUuid: String,
    val toScreenHash: String,
    val timestamp: Long
)
