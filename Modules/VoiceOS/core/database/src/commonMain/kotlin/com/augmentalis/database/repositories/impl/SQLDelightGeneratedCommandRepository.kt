/**
 * SQLDelightGeneratedCommandRepository.kt - SQLDelight implementation of IGeneratedCommandRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 */

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.dto.toGeneratedCommandDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IGeneratedCommandRepository.
 *
 * NOTE: Uses Dispatchers.Default instead of Dispatchers.IO for KMP compatibility.
 * Dispatchers.IO is JVM-only and not available in common code.
 */
class SQLDelightGeneratedCommandRepository(
    private val database: VoiceOSDatabase
) : IGeneratedCommandRepository {

    private val queries = database.generatedCommandQueries

    override suspend fun insert(command: GeneratedCommandDTO): Long = withContext(Dispatchers.Default) {
        var insertedId: Long = 0
        database.transaction {
            queries.insert(
                elementHash = command.elementHash,
                commandText = command.commandText,
                actionType = command.actionType,
                confidence = command.confidence,
                synonyms = command.synonyms,
                isUserApproved = command.isUserApproved,
                usageCount = command.usageCount,
                lastUsed = command.lastUsed,
                createdAt = command.createdAt,
                appId = command.appId
            )
            insertedId = queries.lastInsertRowId().executeAsOne()
        }
        insertedId
    }

    override suspend fun insertBatch(commands: List<GeneratedCommandDTO>) = withContext(Dispatchers.Default) {
        require(commands.isNotEmpty()) { "Cannot insert empty batch of commands" }
        database.transaction {
            commands.forEach { command ->
                queries.insert(
                    elementHash = command.elementHash,
                    commandText = command.commandText,
                    actionType = command.actionType,
                    confidence = command.confidence,
                    synonyms = command.synonyms,
                    isUserApproved = command.isUserApproved,
                    usageCount = command.usageCount,
                    lastUsed = command.lastUsed,
                    createdAt = command.createdAt,
                    appId = command.appId
                )
            }
        }
    }

    override suspend fun getById(id: Long): GeneratedCommandDTO? = withContext(Dispatchers.Default) {
        queries.getById(id).executeAsOneOrNull()?.toGeneratedCommandDTO()
    }

    override suspend fun getByElement(elementHash: String): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getByElement(elementHash).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getAll(): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getAll().executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getAllCommands(): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getAllCommands().executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getByActionType(actionType: String): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getByActionType(actionType).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getHighConfidence(minConfidence: Double): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getHighConfidence(minConfidence).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getUserApproved(): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getUserApproved().executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun fuzzySearch(searchText: String): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(searchText.length <= 1000) { "Search text must not exceed 1000 characters (got ${searchText.length})" }
        queries.fuzzySearch(searchText).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun incrementUsage(id: Long, timestamp: Long) = withContext(Dispatchers.Default) {
        queries.incrementUsage(timestamp, id)
    }

    override suspend fun markApproved(id: Long) = withContext(Dispatchers.Default) {
        queries.markApproved(id)
    }

    override suspend fun updateConfidence(id: Long, confidence: Double) = withContext(Dispatchers.Default) {
        require(id > 0) { "ID must be positive (got $id)" }
        require(confidence in 0.0..1.0) { "Confidence must be between 0.0 and 1.0 (got $confidence)" }
        queries.updateConfidence(confidence, id)
    }

    override suspend fun deleteById(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteById(id)
    }

    override suspend fun deleteByElement(elementHash: String) = withContext(Dispatchers.Default) {
        queries.deleteByElement(elementHash)
    }

    override suspend fun deleteLowQuality(minConfidence: Double) = withContext(Dispatchers.Default) {
        require(minConfidence in 0.0..1.0) { "Minimum confidence must be between 0.0 and 1.0 (got $minConfidence)" }
        queries.deleteLowQuality(minConfidence)
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }

    override suspend fun getByPackage(packageName: String): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getByPackage(packageName).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun update(command: GeneratedCommandDTO) = withContext(Dispatchers.Default) {
        queries.update(
            elementHash = command.elementHash,
            commandText = command.commandText,
            actionType = command.actionType,
            confidence = command.confidence,
            synonyms = command.synonyms,
            isUserApproved = command.isUserApproved,
            usageCount = command.usageCount,
            lastUsed = command.lastUsed,
            appId = command.appId,
            id = command.id
        )
    }

    override suspend fun getAllPaginated(limit: Int, offset: Int): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(limit in 1..1000) { "Limit must be between 1 and 1000 (got $limit)" }
        require(offset >= 0) { "Offset must be non-negative (got $offset)" }
        queries.getAllPaginated(limit.toLong(), offset.toLong())
            .executeAsList()
            .map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getByPackagePaginated(packageName: String, limit: Int, offset: Int): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(packageName.isNotEmpty()) { "Package name cannot be empty" }
        require(limit in 1..1000) { "Limit must be between 1 and 1000 (got $limit)" }
        require(offset >= 0) { "Offset must be non-negative (got $offset)" }

        queries.getByPackagePaginated(packageName, limit.toLong(), offset.toLong())
            .executeAsList()
            .map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getByPackageKeysetPaginated(packageName: String, lastId: Long, limit: Int): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(packageName.isNotEmpty()) { "Package name cannot be empty" }
        require(lastId >= 0) { "Last ID must be non-negative (got $lastId)" }
        require(limit in 1..1000) { "Limit must be between 1 and 1000 (got $limit)" }

        queries.getByPackageKeysetPaginated(packageName, lastId, limit.toLong())
            .executeAsList()
            .map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getByActionTypePaginated(actionType: String, limit: Int, offset: Int): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(limit in 1..1000) { "Limit must be between 1 and 1000 (got $limit)" }
        require(offset >= 0) { "Offset must be non-negative (got $offset)" }
        queries.getByActionTypePaginated(actionType, limit.toLong(), offset.toLong())
            .executeAsList()
            .map { it.toGeneratedCommandDTO() }
    }
}
