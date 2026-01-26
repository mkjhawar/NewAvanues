// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.VoiceCommandDTO
import com.augmentalis.database.dto.toVoiceCommandDTO
import com.augmentalis.database.repositories.IVoiceCommandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * SQLDelight implementation of IVoiceCommandRepository.
 */
class SQLDelightVoiceCommandRepository(
    private val database: VoiceOSDatabase
) : IVoiceCommandRepository {

    private val queries = database.voiceCommandQueries

    override suspend fun insert(command: VoiceCommandDTO): Long = withContext(Dispatchers.Default) {
        database.transactionWithResult {
            queries.insertCommand(
                command_id = command.commandId,
                locale = command.locale,
                trigger_phrase = command.triggerPhrase,
                action = command.action,
                category = command.category,
                priority = command.priority,
                is_enabled = command.isEnabled,
                created_at = command.createdAt,
                updated_at = command.updatedAt
            )
            queries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun getById(id: Long): VoiceCommandDTO? = withContext(Dispatchers.Default) {
        queries.getCommandById(id).executeAsOneOrNull()?.let { cmd: com.augmentalis.database.Commands_static ->
            cmd.toVoiceCommandDTO()
        }
    }

    override suspend fun getByCommandId(commandId: String): List<VoiceCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getCommandsByCommandId(commandId).executeAsList().map { cmd: com.augmentalis.database.Commands_static ->
                cmd.toVoiceCommandDTO()
            }
        }

    override suspend fun getByLocale(locale: String): List<VoiceCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getCommandsByLocale(locale).executeAsList().map { cmd: com.augmentalis.database.Commands_static ->
                cmd.toVoiceCommandDTO()
            }
        }

    override suspend fun getByLocaleWithFallback(locale: String): List<VoiceCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getCommandsByLocaleWithFallback(locale, locale).executeAsList().map { cmd: com.augmentalis.database.Commands_static ->
                cmd.toVoiceCommandDTO()
            }
        }

    override suspend fun getByCategory(category: String): List<VoiceCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.getCommandsByCategory(category).executeAsList().map { cmd: com.augmentalis.database.Commands_static ->
                cmd.toVoiceCommandDTO()
            }
        }

    override suspend fun getEnabled(): List<VoiceCommandDTO> = withContext(Dispatchers.Default) {
        queries.getEnabledCommands().executeAsList().map { cmd: com.augmentalis.database.Commands_static ->
            cmd.toVoiceCommandDTO()
        }
    }

    override suspend fun searchByTrigger(query: String): List<VoiceCommandDTO> =
        withContext(Dispatchers.Default) {
            queries.searchByTrigger(query).executeAsList().map { cmd: com.augmentalis.database.Commands_static ->
                cmd.toVoiceCommandDTO()
            }
        }

    override suspend fun getAll(): List<VoiceCommandDTO> = withContext(Dispatchers.Default) {
        queries.getAllCommands().executeAsList().map { cmd: com.augmentalis.database.Commands_static ->
            cmd.toVoiceCommandDTO()
        }
    }

    override suspend fun update(command: VoiceCommandDTO) = withContext(Dispatchers.Default) {
        queries.updateCommand(
            trigger_phrase = command.triggerPhrase,
            action = command.action,
            category = command.category,
            priority = command.priority,
            is_enabled = command.isEnabled,
            updated_at = Clock.System.now().toEpochMilliseconds(),
            id = command.id
        )
    }

    override suspend fun updateEnabledState(id: Long, isEnabled: Boolean) =
        withContext(Dispatchers.Default) {
            queries.updateEnabledState(
                is_enabled = if (isEnabled) 1L else 0L,
                updated_at = Clock.System.now().toEpochMilliseconds(),
                id = id
            )
        }

    override suspend fun delete(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteCommand(id)
    }

    override suspend fun deleteByCommandId(commandId: String) = withContext(Dispatchers.Default) {
        queries.deleteByCommandId(commandId)
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.countCommands().executeAsOne()
    }

    override suspend fun countByLocale(locale: String): Long = withContext(Dispatchers.Default) {
        queries.countByLocale(locale).executeAsOne()
    }
}
