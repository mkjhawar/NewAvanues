/**
 * AndroidCommandPersistence.kt - Android implementation of ICommandPersistence
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 *
 * Bridges VoiceOSCore command persistence to SQLDelight database via repository pattern.
 * Located in app module because it depends on database module which is not visible from VoiceOSCore.
 */
package com.augmentalis.voiceoscoreng

import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.ICommandPersistence
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Android implementation of ICommandPersistence using SQLDelight.
 *
 * Wraps an IGeneratedCommandRepository to provide the ICommandPersistence
 * interface required by VoiceOSCore for command storage.
 *
 * @param repository The generated command repository from VoiceOS database module
 */
class AndroidCommandPersistence(
    private val repository: IGeneratedCommandRepository
) : ICommandPersistence {

    override suspend fun insertBatch(commands: List<QuantizedCommand>) {
        val dtos = commands.map { it.toDTO() }
        repository.insertBatch(dtos)
    }

    override suspend fun insert(command: QuantizedCommand): Long? {
        val dto = command.toDTO()
        return repository.insert(dto)
    }

    override suspend fun getByPackage(packageName: String): List<QuantizedCommand> {
        return repository.getByPackage(packageName).map { it.toQuantizedCommand() }
    }

    override suspend fun countByPackage(packageName: String): Long {
        return repository.getByPackage(packageName).size.toLong()
    }

    override suspend fun deleteByPackage(packageName: String): Int {
        return repository.deleteCommandsByPackage(packageName)
    }

    /**
     * Convert QuantizedCommand to GeneratedCommandDTO for database storage.
     *
     * FIX (2026-01-19): Use metadata["elementHash"] instead of targetAvid.
     * Root cause: targetAvid contains VUID with prefix (e.g., "BTN:a3f2e1c9")
     * but scraped_element stores elementHash without prefix (e.g., "a3f2e1c9").
     * This mismatch caused FOREIGN KEY constraint failure (code 787).
     */
    private fun QuantizedCommand.toDTO(): GeneratedCommandDTO {
        return GeneratedCommandDTO(
            id = 0, // Auto-generated
            elementHash = this.metadata["elementHash"] ?: "",  // FIX: Use metadata hash, not VUID
            commandText = this.phrase,
            actionType = this.actionType.name,
            confidence = this.confidence.toDouble(),
            synonyms = null,
            isUserApproved = 0L,
            usageCount = 0L,
            lastUsed = null,
            createdAt = System.currentTimeMillis(),
            appId = this.metadata["packageName"] ?: ""  // Also use metadata for consistency
        )
    }

    /**
     * Convert GeneratedCommandDTO to QuantizedCommand for in-memory use.
     */
    private fun GeneratedCommandDTO.toQuantizedCommand(): QuantizedCommand {
        return QuantizedCommand(
            avid = this.id.toString(),
            phrase = this.commandText,
            actionType = CommandActionType.fromString(this.actionType),
            targetAvid = this.elementHash.takeIf { it.isNotBlank() },
            confidence = this.confidence.toFloat(),
            metadata = mapOf(
                "packageName" to this.appId
            )
        )
    }
}
