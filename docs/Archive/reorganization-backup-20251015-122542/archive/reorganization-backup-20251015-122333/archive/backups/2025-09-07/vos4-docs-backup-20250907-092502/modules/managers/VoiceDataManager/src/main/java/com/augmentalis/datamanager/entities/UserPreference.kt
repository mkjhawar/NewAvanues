// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preference")
data class UserPreference(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val key: String = "",
    val value: String = "",
    val type: String = "", // "string", "boolean", "int", "float"
    val module: String = "" // which module owns this preference
)