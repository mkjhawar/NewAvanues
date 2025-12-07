// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "touch_gesture")
data class TouchGesture(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val name: String = "",
    val gestureData: String = "", // Compact JSON of touch points/timing
    val description: String = "",
    val createdDate: Long = 0L,
    val usageCount: Int = 0,
    val isSystemGesture: Boolean = false, // false for user-created
    val associatedCommand: String? = null, // optional command link
    val lastUsed: Long = 0L
)