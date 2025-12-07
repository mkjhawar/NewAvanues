// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "error_report")
data class ErrorReport(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val errorType: String = "",
    val errorMessage: String = "",
    val context: String = "", // Sanitized
    val timestamp: Long = 0L,
    val commandText: String? = null, // Anonymized
    val moduleAffected: String = "",
    val deviceId: String? = null, // Optional, only if user consents
    val sent: Boolean = false,
    val sentDate: Long? = null
)