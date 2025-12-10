// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_statistics")
data class UsageStatistic(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val type: String = "", // "command", "gesture", "sequence"
    val identifier: String = "",
    val count: Int = 0,
    val totalTimeMs: Long = 0L,
    val successRate: Float = 0f,
    val lastUsed: Long = 0L,
    val dateRecorded: Long = 0L // for daily/weekly stats
)