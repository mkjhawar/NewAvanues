/**
 * SQLDelightCommandRepositoryAdapter.kt - Bridge to VoiceOS/core/database
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Adapts VoiceOSCoreNG's ICommandRepository to use VoiceOS/core/database's
 * IGeneratedCommandRepository for SQLDelight persistence.
 */
package com.augmentalis.voiceoscoreng.repository

import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.voiceoscoreng.avu.CommandActionType
import com.augmentalis.voiceoscoreng.avu.QuantizedCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Adapter that bridges VoiceOSCoreNG's ICommandRepository to
 * VoiceOS/core/database's IGeneratedCommandRepository.
 *
 * This allows VoiceOSCoreNG to save commands to the same SQLDelight
 * database used by VoiceOSCore.
 *
 * @param delegate The underlying SQLDelight repository
 */
class SQLDelightCommandRepositoryAdapter(
    private val delegate: IGeneratedCommandRepository
) : ICommandRepository {

    // In-memory cache for flow observation
    private val screenFlows = mutableMapOf<String, MutableStateFlow<List<QuantizedCommand>>>()

    override suspend fun save(command: QuantizedCommand): Result<Unit> {
        return try {
            val dto = command.toDTO()
            delegate.insert(dto)
            refreshScreenFlow(command.packageName, command.screenId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveAll(commands: List<QuantizedCommand>): Result<Unit> {
        return try {
            val dtos = commands.map { it.toDTO() }
            delegate.insertBatch(dtos)
            // Refresh all affected screens
            commands.map { it.packageName to it.screenId }
                .toSet()
                .forEach { (pkg, screen) -> refreshScreenFlow(pkg, screen) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getByApp(packageName: String): List<QuantizedCommand> {
        return delegate.getByPackage(packageName).map { it.toQuantizedCommand() }
    }

    override suspend fun getByScreen(packageName: String, screenId: String): List<QuantizedCommand> {
        // Use fuzzy search on screenId stored in synonyms field
        return delegate.getByPackage(packageName)
            .filter { it.synonyms?.contains(screenId) == true }
            .map { it.toQuantizedCommand() }
    }

    override suspend fun getByVuid(vuid: String): QuantizedCommand? {
        // Search by element hash (which stores the VUID)
        return delegate.getByElement(vuid).firstOrNull()?.toQuantizedCommand()
    }

    override suspend fun deleteByScreen(packageName: String, screenId: String): Result<Unit> {
        return try {
            // Get commands for screen and delete individually
            val commands = getByScreen(packageName, screenId)
            commands.forEach { cmd ->
                delegate.deleteByElement(cmd.targetVuid ?: cmd.vuid)
            }
            refreshScreenFlow(packageName, screenId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteByApp(packageName: String): Result<Unit> {
        return try {
            delegate.deleteCommandsByPackage(packageName)
            // Clear all screen flows for this app
            screenFlows.keys
                .filter { it.startsWith("$packageName:") }
                .forEach { screenFlows.remove(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeByScreen(packageName: String, screenId: String): Flow<List<QuantizedCommand>> {
        val key = "$packageName:$screenId"
        return screenFlows.getOrPut(key) {
            MutableStateFlow(emptyList())
        }
    }

    override suspend fun countByApp(packageName: String): Long {
        return delegate.getByPackage(packageName).size.toLong()
    }

    // ========== Private Helpers ==========

    private suspend fun refreshScreenFlow(packageName: String?, screenId: String?) {
        if (packageName == null || screenId == null) return
        val key = "$packageName:$screenId"
        screenFlows[key]?.let { flow ->
            flow.value = getByScreen(packageName, screenId)
        }
    }

    /**
     * Convert QuantizedCommand to GeneratedCommandDTO for database storage.
     */
    private fun QuantizedCommand.toDTO(): GeneratedCommandDTO {
        val pkg = packageName ?: metadata["packageName"] ?: ""
        val screen = screenId ?: metadata["screenId"] ?: ""
        val version = appVersion ?: metadata["appVersion"] ?: ""
        val versionCode = metadata["versionCode"]?.toLongOrNull() ?: 0L

        return GeneratedCommandDTO(
            id = 0, // Auto-generated by database
            elementHash = targetVuid ?: uuid, // Use target VUID as element hash
            commandText = phrase,
            actionType = actionType.name,
            confidence = confidence.toDouble(),
            synonyms = buildSynonymsJson(screen, metadata),
            isUserApproved = if (metadata["userApproved"] == "true") 1L else 0L,
            usageCount = metadata["usageCount"]?.toLongOrNull() ?: 0L,
            lastUsed = metadata["lastUsed"]?.toLongOrNull(),
            createdAt = metadata["createdAt"]?.toLongOrNull() ?: currentTimeMillis(),
            appId = pkg,
            appVersion = version,
            versionCode = versionCode,
            lastVerified = currentTimeMillis(),
            isDeprecated = 0L
        )
    }

    /**
     * Convert GeneratedCommandDTO to QuantizedCommand.
     */
    private fun GeneratedCommandDTO.toQuantizedCommand(): QuantizedCommand {
        val screenId = extractScreenIdFromSynonyms(synonyms)
        val extraMetadata = extractMetadataFromSynonyms(synonyms)

        return QuantizedCommand(
            uuid = "cmd_${id}",
            phrase = commandText,
            actionType = CommandActionType.fromString(actionType),
            targetVuid = elementHash,
            confidence = confidence.toFloat(),
            metadata = mapOf(
                "packageName" to appId,
                "screenId" to screenId,
                "appVersion" to appVersion,
                "versionCode" to versionCode.toString(),
                "userApproved" to (isUserApproved == 1L).toString(),
                "usageCount" to usageCount.toString(),
                "lastUsed" to (lastUsed?.toString() ?: ""),
                "createdAt" to createdAt.toString(),
                "id" to id.toString()
            ) + extraMetadata
        )
    }

    /**
     * Build synonyms JSON containing screenId and extra metadata.
     * Format: screenId|key1=val1|key2=val2
     */
    private fun buildSynonymsJson(screenId: String, metadata: Map<String, String>): String {
        val parts = mutableListOf(screenId)
        metadata.filterKeys { it !in CORE_METADATA_KEYS }.forEach { (k, v) ->
            parts.add("$k=$v")
        }
        return parts.joinToString("|")
    }

    /**
     * Extract screenId from synonyms field.
     */
    private fun extractScreenIdFromSynonyms(synonyms: String?): String {
        if (synonyms.isNullOrBlank()) return ""
        return synonyms.split("|").firstOrNull() ?: ""
    }

    /**
     * Extract extra metadata from synonyms field.
     */
    private fun extractMetadataFromSynonyms(synonyms: String?): Map<String, String> {
        if (synonyms.isNullOrBlank()) return emptyMap()
        return synonyms.split("|")
            .drop(1) // Skip screenId
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx > 0) part.substring(0, idx) to part.substring(idx + 1)
                else null
            }
            .toMap()
    }

    companion object {
        private val CORE_METADATA_KEYS = setOf(
            "packageName", "screenId", "appVersion", "versionCode",
            "userApproved", "usageCount", "lastUsed", "createdAt", "id"
        )
    }
}

/**
 * Platform-specific current time.
 */
expect fun currentTimeMillis(): Long
