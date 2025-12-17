/**
 * ScreenTransitionEntity.kt - Screen navigation transitions for flow analysis
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-18
 */
package com.augmentalis.voiceoscore.scraping.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing screen-to-screen transitions
 *
 * Tracks navigation patterns and user flows between screens.
 * Enables understanding of:
 * - Common navigation paths
 * - Screen flow sequences
 * - Dead-end screens vs gateway screens
 * - User journey analysis
 *
 * @property id Auto-generated primary key
 * @property fromScreenHash Hash of source screen
 * @property toScreenHash Hash of destination screen
 * @property transitionCount Number of times this transition occurred
 * @property firstTransition Timestamp of first occurrence
 * @property lastTransition Timestamp of most recent occurrence
 * @property avgTransitionTime Average time between screens (milliseconds)
 */
@Entity(
    tableName = "screen_transitions",
    foreignKeys = [
        ForeignKey(
            entity = ScreenContextEntity::class,
            parentColumns = ["screen_hash"],
            childColumns = ["from_screen_hash"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScreenContextEntity::class,
            parentColumns = ["screen_hash"],
            childColumns = ["to_screen_hash"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("from_screen_hash"),
        Index("to_screen_hash"),
        Index(value = ["from_screen_hash", "to_screen_hash"], unique = true)
    ]
)
data class ScreenTransitionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "from_screen_hash")
    val fromScreenHash: String,

    @ColumnInfo(name = "to_screen_hash")
    val toScreenHash: String,

    @ColumnInfo(name = "transition_count")
    val transitionCount: Int = 1,

    @ColumnInfo(name = "first_transition")
    val firstTransition: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_transition")
    val lastTransition: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "avg_transition_time")
    val avgTransitionTime: Long? = null
)
