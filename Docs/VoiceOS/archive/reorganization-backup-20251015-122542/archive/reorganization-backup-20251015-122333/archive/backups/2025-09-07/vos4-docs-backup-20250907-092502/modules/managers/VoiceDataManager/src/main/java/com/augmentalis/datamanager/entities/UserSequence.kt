// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.augmentalis.datamanager.entities.StringListConverter

@Entity(tableName = "user_sequence")
@TypeConverters(StringListConverter::class)
data class UserSequence(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val name: String = "",
    val description: String = "",
    val steps: List<String> = emptyList(), // JSON array of commands/gestures
    val triggerPhrase: String = "",
    val language: String = "",
    val createdDate: Long = 0L,
    val lastUsed: Long = 0L,
    val usageCount: Int = 0,
    val estimatedDurationMs: Long = 0L
)