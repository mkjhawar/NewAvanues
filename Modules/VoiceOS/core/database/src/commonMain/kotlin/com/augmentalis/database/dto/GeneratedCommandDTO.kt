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
    val appId: String = ""  // P2 Task 3: Package name for pagination
)

/**
 * Extension to convert SQLDelight generated type to DTO
 * RENAMED (2025-12-05): Generated_command -> Commands_generated
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
        appId = appId
    )
}
