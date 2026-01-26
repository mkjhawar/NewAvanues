// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.CustomCommandDTO
import com.augmentalis.database.dto.toDTO
import com.augmentalis.database.repositories.ICommandRepository
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
        database.transactionWithResult {
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
            queries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun getById(id: Long): CustomCommandDTO? = withContext(Dispatchers.Default) {
        queries.getById(id).executeAsOneOrNull()?.let { cmd: com.augmentalis.database.app.Custom_command ->
            cmd.toDTO()
        }
    }

    override suspend fun getAll(): List<CustomCommandDTO> = withContext(Dispatchers.Default) {
        queries.getAll().executeAsList().map { cmd: com.augmentalis.database.app.Custom_command ->
            cmd.toDTO()
        }
    }

    override suspend fun getActive(): List<CustomCommandDTO> = withContext(Dispatchers.Default) {
        queries.getActive().executeAsList().map { cmd: com.augmentalis.database.app.Custom_command ->
            cmd.toDTO()
        }
    }

    override suspend fun getActiveByLanguage(language: String): List<CustomCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getActiveByLanguage(language).executeAsList().map { cmd: com.augmentalis.database.app.Custom_command ->
                cmd.toDTO()
            }
        }

    override suspend fun getByLanguage(language: String): List<CustomCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getByLanguage(language).executeAsList().map { cmd: com.augmentalis.database.app.Custom_command ->
                cmd.toDTO()
            }
        }

    override suspend fun getMostUsed(limit: Int): List<CustomCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getMostUsed(limit.toLong()).executeAsList().map { cmd: com.augmentalis.database.app.Custom_command ->
                cmd.toDTO()
            }
        }

    override suspend fun searchByName(query: String): List<CustomCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.searchByName(query).executeAsList().map { cmd: com.augmentalis.database.app.Custom_command ->
                cmd.toDTO()
            }
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
