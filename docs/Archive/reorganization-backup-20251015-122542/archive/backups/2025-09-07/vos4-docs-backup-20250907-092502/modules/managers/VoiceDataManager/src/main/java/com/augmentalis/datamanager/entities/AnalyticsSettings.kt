// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analytics_settings")
data class AnalyticsSettings(
    @PrimaryKey(autoGenerate = true) var id: Long = 1, // single record
    val trackPerformance: Boolean = false, // OFF by default - privacy first
    val autoEnableOnErrors: Boolean = false, // OFF by default - require explicit consent
    val errorThreshold: Float = 0.10f,
    val sendAnonymousReports: Boolean = false,
    val includeDeviceId: Boolean = false, // For support purposes
    val userConsent: Boolean = false,
    val consentDate: Long? = null,
    val detailedLogDays: Int = 7,
    val aggregateOlderData: Boolean = true
)