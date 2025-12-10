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
 */
class SQLDelightGeneratedCommandRepository(
    private val database: VoiceOSDatabase
) : IGeneratedCommandRepository {

    private val queries = database.generatedCommandQueries

    override suspend fun insert(command: GeneratedCommandDTO): Long = withContext(Dispatchers.Default) {
        queries.insert(
            elementHash = command.elementHash,
            commandText = command.commandText,
            actionType = command.actionType,
            confidence = command.confidence,
            synonyms = command.synonyms,
            isUserApproved = command.isUserApproved,
            usageCount = command.usageCount,
            lastUsed = command.lastUsed,
            createdAt = command.createdAt
        )
        queries.count().executeAsOne() // Return last inserted ID (approximation)
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

<<<<<<< HEAD
    override suspend fun getAllCommands(): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getAllCommands().executeAsList().map { it.toGeneratedCommandDTO() }
    }

=======
>>>>>>> AVA-Development
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
        queries.fuzzySearch(searchText).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun incrementUsage(id: Long, timestamp: Long) = withContext(Dispatchers.Default) {
        queries.incrementUsage(timestamp, id)
    }

    override suspend fun markApproved(id: Long) = withContext(Dispatchers.Default) {
        queries.markApproved(id)
    }

    override suspend fun updateConfidence(id: Long, confidence: Double) = withContext(Dispatchers.Default) {
        queries.updateConfidence(confidence, id)
    }

    override suspend fun deleteById(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteById(id)
    }

    override suspend fun deleteByElement(elementHash: String) = withContext(Dispatchers.Default) {
        queries.deleteByElement(elementHash)
    }

    override suspend fun deleteLowQuality(minConfidence: Double) = withContext(Dispatchers.Default) {
        queries.deleteLowQuality(minConfidence)
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }
<<<<<<< HEAD

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
            id = command.id
        )
    }
=======
>>>>>>> AVA-Development
}
