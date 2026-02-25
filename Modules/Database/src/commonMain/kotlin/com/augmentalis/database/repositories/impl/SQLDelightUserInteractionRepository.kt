/**
 * SQLDelightUserInteractionRepository.kt - SQLDelight implementation of IUserInteractionRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-25
 */

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.UserInteractionDTO
import com.augmentalis.database.dto.toUserInteractionDTO
import com.augmentalis.database.repositories.IUserInteractionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IUserInteractionRepository.
 */
class SQLDelightUserInteractionRepository(
    private val database: VoiceOSDatabase
) : IUserInteractionRepository {

    private val queries = database.userInteractionQueries

    override suspend fun insert(interaction: UserInteractionDTO): Long = withContext(Dispatchers.Default) {
        queries.insert(
            elementHash = interaction.elementHash,
            screenHash = interaction.screenHash,
            interactionType = interaction.interactionType,
            interactionTime = interaction.interactionTime,
            visibilityStart = interaction.visibilityStart,
            visibilityDuration = interaction.visibilityDuration
        )
        queries.count().executeAsOne()
    }

    override suspend fun getById(id: Long): UserInteractionDTO? = withContext(Dispatchers.Default) {
        queries.getById(id).executeAsOneOrNull()?.toUserInteractionDTO()
    }

    override suspend fun getByElement(elementHash: String): List<UserInteractionDTO> = withContext(Dispatchers.Default) {
        queries.getByElement(elementHash).executeAsList().map { it.toUserInteractionDTO() }
    }

    override suspend fun getByScreen(screenHash: String): List<UserInteractionDTO> = withContext(Dispatchers.Default) {
        queries.getByScreen(screenHash).executeAsList().map { it.toUserInteractionDTO() }
    }

    override suspend fun getByType(interactionType: String): List<UserInteractionDTO> = withContext(Dispatchers.Default) {
        queries.getByType(interactionType).executeAsList().map { it.toUserInteractionDTO() }
    }

    override suspend fun getByTimeRange(startTime: Long, endTime: Long): List<UserInteractionDTO> = withContext(Dispatchers.Default) {
        queries.getByTimeRange(startTime, endTime).executeAsList().map { it.toUserInteractionDTO() }
    }

    override suspend fun getRecent(limit: Long): List<UserInteractionDTO> = withContext(Dispatchers.Default) {
        queries.getRecent(limit).executeAsList().map { it.toUserInteractionDTO() }
    }

    override suspend fun deleteById(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteById(id)
    }

    override suspend fun deleteByElement(elementHash: String) = withContext(Dispatchers.Default) {
        queries.deleteByElement(elementHash)
    }

    override suspend fun deleteOlderThan(timestamp: Long) = withContext(Dispatchers.Default) {
        queries.deleteOlderThan(timestamp)
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }

    override suspend fun countByType(interactionType: String): Long = withContext(Dispatchers.Default) {
        queries.countByType().executeAsList().find { it.interactionType == interactionType }?.count ?: 0L
    }

    override suspend fun getInteractionCount(elementHash: String): Int = withContext(Dispatchers.Default) {
        queries.getByElement(elementHash).executeAsList().size
    }

    override suspend fun getSuccessFailureRatio(elementHash: String): com.augmentalis.database.repositories.SuccessRatio? = withContext(Dispatchers.Default) {
        val interactions = queries.getByElement(elementHash).executeAsList()
        if (interactions.isEmpty()) return@withContext null

        // For now, we consider all interactions successful since we don't track success/failure
        // TODO: Add success tracking to UserInteraction schema
        com.augmentalis.database.repositories.SuccessRatio(
            successful = interactions.size,
            failed = 0
        )
    }
}
