/**
 * NavigationEdgeEntity.kt - Room entity for navigation edges
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 *
 * Room database entity for storing navigation graph edges
 */

package com.augmentalis.voiceoscore.learnapp.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Navigation Edge Entity
 *
 * Room entity storing navigation graph edges (screen transitions).
 */
@Entity(
    tableName = "navigation_edges",
    foreignKeys = [
        ForeignKey(
            entity = LearnedAppEntity::class,
            parentColumns = ["package_name"],
            childColumns = ["package_name"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExplorationSessionEntity::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("from_screen_hash"),
        Index("to_screen_hash"),
        Index("package_name"),
        Index("session_id")
    ]
)
data class NavigationEdgeEntity(
    @PrimaryKey
    @ColumnInfo(name = "edge_id")
    val edgeId: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "from_screen_hash")
    val fromScreenHash: String,

    @ColumnInfo(name = "clicked_element_uuid")
    val clickedElementUuid: String,

    @ColumnInfo(name = "to_screen_hash")
    val toScreenHash: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)
