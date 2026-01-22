/**
 * NavigationEdgeEntity.kt - Data model for navigation edges
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 * Migrated to SQLDelight: 2025-12-17
 *
 * Data model for storing navigation graph edges
 * Uses SQLDelight schema from core/database module
 */

package com.augmentalis.voiceoscore.learnapp.database.entities

/**
 * Navigation Edge Data Model
 *
 * Data model storing navigation graph edges (screen transitions).
 * Corresponds to navigation_edges table in SQLDelight schema.
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
