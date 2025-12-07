// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gesture_learning_data")
data class GestureLearningData(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val gestureId: Long = 0L,
    val userId: String? = null, // Anonymous ID
    val successRate: Float = 0f,
    val averageVelocity: Float = 0f,
    val averagePressure: Float? = null,
    val commonMistakes: String = "", // JSON array
    val zonePreferences: String = "" // JSON map
)