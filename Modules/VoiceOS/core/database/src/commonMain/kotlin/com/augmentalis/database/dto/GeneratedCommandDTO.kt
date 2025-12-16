/**
 * GeneratedCommandDTO.kt - DTO for generated voice commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 *
 * Data Transfer Object for generated voice commands.
 * Maps to GeneratedCommand.sq schema.
 */

package com.augmentalis.database.dto

import com.augmentalis.database.Commands_generated

/**
 * DTO for generated voice commands
 *
 * Commands are auto-generated from UI elements and can be approved/refined by users.
 *
 * ## Version Tracking (Schema v3):
 * - appVersion: String representation of app version (e.g., "8.2024.11.123")
 * - versionCode: Integer version for efficient comparison (e.g., 82024)
 * - lastVerified: Timestamp when element was last seen in app
 * - isDeprecated: Whether command is deprecated (pending deletion)
 */
data class GeneratedCommandDTO(
    val id: Long,
    val elementHash: String,
    val commandText: String,
    val actionType: String,
    val confidence: Double,
    val synonyms: String?,
    val isUserApproved: Long = 0,  // SQLite Boolean (0/1)
    val usageCount: Long = 0,
    val lastUsed: Long? = null,
    val createdAt: Long,
    val appId: String = "",  // P2 Task 3: Package name for pagination

    // Version tracking (Schema v3)
    val appVersion: String = "",  // Version string (e.g., "8.2024.11.123")
    val versionCode: Long = 0,    // Version code for comparison (e.g., 82024)
    val lastVerified: Long? = null,  // Last time element was verified
    val isDeprecated: Long = 0    // SQLite Boolean: 0=active, 1=deprecated
)

/**
 * Extension to convert SQLDelight generated type to DTO
 * RENAMED (2025-12-05): Generated_command -> Commands_generated
 * UPDATED (2025-12-13): Added version tracking fields (Schema v3)
 */
fun Commands_generated.toGeneratedCommandDTO(): GeneratedCommandDTO {
    return GeneratedCommandDTO(
        id = id,
        elementHash = elementHash,
        commandText = commandText,
        actionType = actionType,
        confidence = confidence,
        synonyms = synonyms,
        isUserApproved = isUserApproved,
        usageCount = usageCount,
        lastUsed = lastUsed,
        createdAt = createdAt,
        appId = appId,
        appVersion = appVersion,
        versionCode = versionCode,
        lastVerified = lastVerified,
        isDeprecated = isDeprecated
    )
}
