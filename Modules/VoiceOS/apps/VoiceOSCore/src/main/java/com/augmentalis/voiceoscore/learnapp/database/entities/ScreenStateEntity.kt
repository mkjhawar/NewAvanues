/**
 * ScreenStateEntity.kt - Room entity for screen states
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 *
 * Room database entity for storing screen state data
 */

package com.augmentalis.voiceoscore.learnapp.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Screen State Entity
 *
 * Room entity storing screen state metadata.
 */
@Entity(
    tableName = "screen_states",
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
data class ScreenStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "screen_hash")
    val screenHash: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "activity_name")
    val activityName: String? = null,

    @ColumnInfo(name = "fingerprint")
    val fingerprint: String,

    @ColumnInfo(name = "element_count")
    val elementCount: Int,

    @ColumnInfo(name = "discovered_at")
    val discoveredAt: Long
)
