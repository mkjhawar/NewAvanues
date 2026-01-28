/**
 * DatabaseConverters.kt - Serialization helpers for QuantizedCommand
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 * Updated: 2026-01-07 - Simplified to platform-agnostic serialization
 *
 * Provides serialization/deserialization helpers for QuantizedCommand.
 * Platform-specific persistence implementations (Android, iOS, Desktop)
 * can use these helpers for consistent data format.
 *
 * For Android SQLDelight integration, see:
 * - AndroidCommandPersistence in androidMain
 */
package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.CommandActionType
import com.augmentalis.commandmanager.QuantizedCommand
import com.augmentalis.commandmanager.currentTimeMillis
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serializable representation of QuantizedCommand for persistence.
 *
 * This DTO is platform-agnostic and can be serialized to JSON for
 * various storage backends (SQLite, Core Data, files, etc.)
 */
@Serializable
data class CommandRecord(
    val uuid: String,
    val phrase: String,
    val actionType: String,
    val targetVuid: String?,
    val confidence: Float,
    val packageName: String = "",
    val appVersion: String = "",
    val versionCode: Long = 0,
    val createdAt: Long = currentTimeMillis(),
    val usageCount: Long = 0,
    val isUserApproved: Boolean = false,
    val isDeprecated: Boolean = false
)

// ============================================================================
// QuantizedCommand ↔ CommandRecord Conversion
// ============================================================================

/**
 * Convert QuantizedCommand to CommandRecord for serialization/persistence.
 */
fun QuantizedCommand.toCommandRecord(): CommandRecord {
    return CommandRecord(
        uuid = avid,
        phrase = phrase,
        actionType = actionType.name,
        targetVuid = targetAvid,
        confidence = confidence,
        packageName = metadata["packageName"] ?: "",
        appVersion = metadata["appVersion"] ?: "",
        versionCode = metadata["versionCode"]?.toLongOrNull() ?: 0,
        createdAt = metadata["createdAt"]?.toLongOrNull() ?: currentTimeMillis(),
        usageCount = metadata["usageCount"]?.toLongOrNull() ?: 0,
        isUserApproved = metadata["isUserApproved"] == "true",
        isDeprecated = metadata["isDeprecated"] == "true"
    )
}

/**
 * Convert CommandRecord back to QuantizedCommand.
 */
fun CommandRecord.toQuantizedCommand(): QuantizedCommand {
    return QuantizedCommand(
        avid = uuid,
        phrase = phrase,
        actionType = CommandActionType.fromString(actionType),
        targetAvid = targetVuid,
        confidence = confidence,
        metadata = buildMap {
            if (packageName.isNotEmpty()) put("packageName", packageName)
            if (appVersion.isNotEmpty()) put("appVersion", appVersion)
            if (versionCode > 0) put("versionCode", versionCode.toString())
            put("createdAt", createdAt.toString())
            if (usageCount > 0) put("usageCount", usageCount.toString())
            if (isUserApproved) put("isUserApproved", "true")
            if (isDeprecated) put("isDeprecated", "true")
        }
    )
}

/**
 * Convert list of CommandRecord to list of QuantizedCommand.
 */
fun List<CommandRecord>.toQuantizedCommands(): List<QuantizedCommand> =
    map { it.toQuantizedCommand() }

/**
 * Convert list of QuantizedCommand to list of CommandRecord.
 */
fun List<QuantizedCommand>.toCommandRecords(): List<CommandRecord> =
    map { it.toCommandRecord() }

// ============================================================================
// JSON Serialization
// ============================================================================

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Serialize QuantizedCommand to JSON string.
 */
fun QuantizedCommand.toJson(): String = json.encodeToString(toCommandRecord())

/**
 * Deserialize QuantizedCommand from JSON string.
 */
fun commandFromJson(jsonString: String): QuantizedCommand =
    json.decodeFromString<CommandRecord>(jsonString).toQuantizedCommand()

/**
 * Serialize list of QuantizedCommands to JSON string.
 */
fun List<QuantizedCommand>.toJsonArray(): String =
    json.encodeToString(map { it.toCommandRecord() })

/**
 * Deserialize list of QuantizedCommands from JSON string.
 */
fun commandsFromJsonArray(jsonString: String): List<QuantizedCommand> =
    json.decodeFromString<List<CommandRecord>>(jsonString).toQuantizedCommands()
