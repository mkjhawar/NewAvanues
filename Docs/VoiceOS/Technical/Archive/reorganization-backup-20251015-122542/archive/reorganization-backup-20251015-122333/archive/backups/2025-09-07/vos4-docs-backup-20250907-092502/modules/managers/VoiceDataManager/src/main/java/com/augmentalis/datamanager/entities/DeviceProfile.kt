// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_profile")
data class DeviceProfile(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val deviceType: String = "", // "smartglasses", "phone", "tablet"
    val deviceModel: String = "",
    val settings: String = "", // JSON configuration
    val calibrationData: String? = null, // JSON calibration
    val isActive: Boolean = false,
    val lastConnected: Long = 0L
)