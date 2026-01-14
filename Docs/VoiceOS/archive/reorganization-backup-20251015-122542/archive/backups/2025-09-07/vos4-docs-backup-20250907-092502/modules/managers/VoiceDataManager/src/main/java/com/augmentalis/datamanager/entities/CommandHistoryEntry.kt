// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history_entry")
data class CommandHistoryEntry(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val originalText: String = "",
    val processedCommand: String = "",
    val confidence: Float = 0f,
    val timestamp: Long = 0L,
    val language: String = "",
    val engineUsed: String = "",
    val success: Boolean = false,
    val executionTimeMs: Long = 0L,
    val usageCount: Int = 1
)