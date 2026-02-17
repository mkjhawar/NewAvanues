/*
 * Copyright (c) 2026 Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 * Created: 2026-02-11
 */

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.PhraseSuggestionDTO
import com.augmentalis.database.repositories.IPhraseSuggestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SQLDelightPhraseSuggestionRepository(
    private val database: VoiceOSDatabase
) : IPhraseSuggestionRepository {

    private val queries = database.phraseSuggestionQueries

    override suspend fun getAll(): List<PhraseSuggestionDTO> = withContext(Dispatchers.Default) {
        queries.getAll().executeAsList().map { row ->
            PhraseSuggestionDTO(
                id = row.id,
                commandId = row.command_id,
                originalPhrase = row.original_phrase,
                suggestedPhrase = row.suggested_phrase,
                locale = row.locale,
                createdAt = row.created_at,
                status = row.status,
                source = row.source
            )
        }
    }

    override suspend fun getPendingByLocale(locale: String): List<PhraseSuggestionDTO> =
        withContext(Dispatchers.Default) {
            queries.getPendingByLocale(locale).executeAsList().map { row ->
                PhraseSuggestionDTO(
                    id = row.id,
                    commandId = row.command_id,
                    originalPhrase = row.original_phrase,
                    suggestedPhrase = row.suggested_phrase,
                    locale = row.locale,
                    createdAt = row.created_at,
                    status = row.status,
                    source = row.source
                )
            }
        }

    override suspend fun getForCommand(commandId: String): List<PhraseSuggestionDTO> =
        withContext(Dispatchers.Default) {
            queries.getForCommand(commandId).executeAsList().map { row ->
                PhraseSuggestionDTO(
                    id = row.id,
                    commandId = row.command_id,
                    originalPhrase = row.original_phrase,
                    suggestedPhrase = row.suggested_phrase,
                    locale = row.locale,
                    createdAt = row.created_at,
                    status = row.status,
                    source = row.source
                )
            }
        }

    override suspend fun insert(dto: PhraseSuggestionDTO): Long =
        withContext(Dispatchers.Default) {
            queries.insert(
                command_id = dto.commandId,
                original_phrase = dto.originalPhrase,
                suggested_phrase = dto.suggestedPhrase,
                locale = dto.locale,
                created_at = dto.createdAt,
                status = dto.status,
                source = dto.source
            )
            queries.getLastInsertedId().executeAsOne()
        }

    override suspend fun updateStatus(id: Long, status: String): Unit =
        withContext(Dispatchers.Default) {
            queries.updateStatus(status, id)
        }

    override suspend fun getPendingCount(): Long = withContext(Dispatchers.Default) {
        queries.getPendingCount().executeAsOne()
    }

    override suspend fun deleteAll(): Unit = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }
}
