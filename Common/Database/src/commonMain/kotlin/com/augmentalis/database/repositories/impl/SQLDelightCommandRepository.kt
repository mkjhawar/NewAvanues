// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.avanues.database.repositories.impl

import com.avanues.database.VoiceOSDatabase
import com.avanues.database.dto.CustomCommandDTO
import com.avanues.database.dto.toDTO
import com.avanues.database.repositories.ICommandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * SQLDelight implementation of ICommandRepository.
 */
class SQLDelightCommandRepository(
    private val database: VoiceOSDatabase
) : ICommandRepository {

    private val queries = database.customCommandQueries

    override suspend fun insert(command: CustomCommandDTO): Long = withContext(Dispatchers.Default) {
        queries.insert(
            name = command.name,
            description = command.description,
            phrases = command.phrases.joinToString("|"),
            action = command.action,
            parameters = command.parameters,
            language = command.language,
            isActive = if (command.isActive) 1L else 0L,
            usageCount = command.usageCount,
            lastUsed = command.lastUsed,
            createdAt = command.createdAt,
            updatedAt = command.updatedAt
        )
        queries.transactionWithResult {
            queries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun getById(id: Long): CustomCommandDTO? = withContext(Dispatchers.Default) {
        queries.getById(id).executeAsOneOrNull()?.toDTO()
    }

    override suspend fun getAll(): List<CustomCommandDTO> = withContext(Dispatchers.Default) {
        queries.getAll().executeAsList().map { it.toDTO() }
    }

    override suspend fun getActive(): List<CustomCommandDTO> = withContext(Dispatchers.Default) {
        queries.getActive().executeAsList().map { it.toDTO() }
    }

    override suspend fun getActiveByLanguage(language: String): List<CustomCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getActiveByLanguage(language).executeAsList().map { it.toDTO() }
        }

    override suspend fun getByLanguage(language: String): List<CustomCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getByLanguage(language).executeAsList().map { it.toDTO() }
        }

    override suspend fun getMostUsed(limit: Int): List<CustomCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getMostUsed(limit.toLong()).executeAsList().map { it.toDTO() }
        }

    override suspend fun searchByName(query: String): List<CustomCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.searchByName(query).executeAsList().map { it.toDTO() }
        }

    override suspend fun update(command: CustomCommandDTO) = withContext(Dispatchers.Default) {
        queries.update(
            name = command.name,
            description = command.description,
            phrases = command.phrases.joinToString("|"),
            action = command.action,
            parameters = command.parameters,
            language = command.language,
            isActive = if (command.isActive) 1L else 0L,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            id = command.id
        )
    }

    override suspend fun incrementUsage(id: Long) = withContext(Dispatchers.Default) {
        queries.incrementUsage(Clock.System.now().toEpochMilliseconds(), id)
    }

    override suspend fun setActiveStatus(id: Long, isActive: Boolean) =
        withContext(Dispatchers.Default) {
            queries.setActiveStatus(
                isActive = if (isActive) 1L else 0L,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                id = id
            )
        }

    override suspend fun delete(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteById(id)
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }

    override suspend fun countActive(): Long = withContext(Dispatchers.Default) {
        queries.countActive().executeAsOne()
    }
}
