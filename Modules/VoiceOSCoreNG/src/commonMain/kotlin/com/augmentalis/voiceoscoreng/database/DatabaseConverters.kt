/**
 * DatabaseConverters.kt - Conversion between domain models and database DTOs
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Provides conversion functions between VoiceOSCoreNG domain models and
 * core/database DTOs. This enables clean architecture separation:
 * - QuantizedCommand: Runtime voice command matching (domain model)
 * - GeneratedCommandDTO: Database persistence (persistence model)
 */
package com.augmentalis.voiceoscoreng.database

import com.augmentalis.voiceoscoreng.avu.CommandActionType
import com.augmentalis.voiceoscoreng.avu.QuantizedCommand
import com.augmentalis.voiceoscoreng.speech.currentTimeMillis

// Note: These types come from core/database module
// import com.augmentalis.database.dto.GeneratedCommandDTO

/**
 * Lightweight DTO for command persistence.
 *
 * Maps to GeneratedCommandDTO from core/database.
 * Used when core/database dependency is not available (testing).
 */
data class CommandDTO(
    val id: Long = 0,
    val elementHash: String,
    val commandText: String,
    val actionType: String,
    val confidence: Double,
    val synonyms: String? = null,
    val isUserApproved: Long = 0,
    val usageCount: Long = 0,
    val lastUsed: Long? = null,
    val createdAt: Long = currentTimeMillis(),
    val appId: String = "",
    val appVersion: String = "",
    val versionCode: Long = 0,
    val lastVerified: Long? = null,
    val isDeprecated: Long = 0
)

// ============================================================================
// QuantizedCommand ↔ CommandDTO Conversion
// ============================================================================

/**
 * Convert QuantizedCommand to CommandDTO for database persistence.
 *
 * Maps domain model fields to persistence model:
 * - uuid → elementHash (element identifier)
 * - phrase → commandText (voice trigger)
 * - actionType → actionType (as string)
 * - confidence → confidence (Float to Double)
 * - metadata → individual fields (appId, appVersion, etc.)
 */
fun QuantizedCommand.toDTO(): CommandDTO {
    return CommandDTO(
        elementHash = targetVuid ?: uuid,
        commandText = phrase,
        actionType = actionType.name,
        confidence = confidence.toDouble(),
        appId = metadata["packageName"] ?: "",
        appVersion = metadata["appVersion"] ?: "",
        versionCode = metadata["versionCode"]?.toLongOrNull() ?: 0,
        createdAt = metadata["createdAt"]?.toLongOrNull() ?: currentTimeMillis()
    )
}

/**
 * Convert CommandDTO to QuantizedCommand for runtime use.
 *
 * Reconstructs domain model from persistence:
 * - elementHash → uuid and targetVuid
 * - commandText → phrase
 * - actionType → CommandActionType enum
 * - Individual fields → metadata map
 */
fun CommandDTO.toQuantizedCommand(): QuantizedCommand {
    return QuantizedCommand(
        uuid = elementHash,
        phrase = commandText,
        actionType = CommandActionType.fromString(actionType),
        targetVuid = elementHash,
        confidence = confidence.toFloat(),
        metadata = buildMap {
            if (appId.isNotEmpty()) put("packageName", appId)
            if (appVersion.isNotEmpty()) put("appVersion", appVersion)
            if (versionCode > 0) put("versionCode", versionCode.toString())
            put("createdAt", createdAt.toString())
            if (usageCount > 0) put("usageCount", usageCount.toString())
            if (isUserApproved == 1L) put("isUserApproved", "true")
        }
    )
}

/**
 * Convert list of CommandDTO to list of QuantizedCommand.
 */
fun List<CommandDTO>.toQuantizedCommands(): List<QuantizedCommand> =
    map { it.toQuantizedCommand() }

/**
 * Convert list of QuantizedCommand to list of CommandDTO.
 */
fun List<QuantizedCommand>.toDTOs(): List<CommandDTO> =
    map { it.toDTO() }

// ============================================================================
// VUID Entry Converters
// ============================================================================

/**
 * Lightweight DTO for VUID element persistence.
 *
 * Maps to VUIDElementDTO from core/database.
 */
data class VuidDTO(
    val vuid: String,
    val name: String,
    val type: String,
    val description: String? = null,
    val parentVuid: String? = null,
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val timestamp: Long = currentTimeMillis(),
    val metadataJson: String? = null,
    val positionJson: String? = null
)

/**
 * Create VuidDTO from basic element info.
 */
fun createVuidDTO(
    vuid: String,
    name: String,
    type: String,
    packageName: String? = null,
    activityName: String? = null
): VuidDTO {
    val metadata = buildString {
        append("{")
        packageName?.let { append("\"packageName\":\"$it\"") }
        if (packageName != null && activityName != null) append(",")
        activityName?.let { append("\"activityName\":\"$it\"") }
        append("}")
    }

    return VuidDTO(
        vuid = vuid,
        name = name,
        type = type,
        metadataJson = if (metadata != "{}") metadata else null
    )
}
