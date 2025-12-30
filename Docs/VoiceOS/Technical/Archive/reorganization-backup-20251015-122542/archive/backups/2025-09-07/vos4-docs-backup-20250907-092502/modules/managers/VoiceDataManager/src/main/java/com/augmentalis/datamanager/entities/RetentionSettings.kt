// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "retention_settings")
data class RetentionSettings(
    @PrimaryKey(autoGenerate = true) var id: Long = 1, // single record
    val commandHistoryRetainCount: Int = 50, // user configurable 25-200
    val commandHistoryMaxDays: Int = 30,
    val statisticsRetentionDays: Int = 90,
    val enableAutoCleanup: Boolean = true,
    val notifyBeforeCleanup: Boolean = true,
    val maxDatabaseSizeMB: Int = 100
)