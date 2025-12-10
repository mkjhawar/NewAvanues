/**
 * SQLDelightElementStateHistoryRepository.kt - SQLDelight implementation of IElementStateHistoryRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 */

package com.avanues.database.repositories.impl

import com.avanues.database.VoiceOSDatabase
import com.avanues.database.dto.ElementStateHistoryDTO
import com.avanues.database.dto.toElementStateHistoryDTO
import com.avanues.database.repositories.IElementStateHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IElementStateHistoryRepository.
 */
class SQLDelightElementStateHistoryRepository(
    private val database: VoiceOSDatabase
) : IElementStateHistoryRepository {

    private val queries = database.elementStateHistoryQueries

    override suspend fun insert(stateChange: ElementStateHistoryDTO): Long = withContext(Dispatchers.Default) {
        queries.insert(
            elementHash = stateChange.elementHash,
            screenHash = stateChange.screenHash,
            stateType = stateChange.stateType,
            oldValue = stateChange.oldValue,
            newValue = stateChange.newValue,
            changedAt = stateChange.changedAt,
            triggeredBy = stateChange.triggeredBy
        )
        queries.count().executeAsOne()
    }

    override suspend fun getById(id: Long): ElementStateHistoryDTO? = withContext(Dispatchers.Default) {
        queries.getById(id).executeAsOneOrNull()?.toElementStateHistoryDTO()
    }

    override suspend fun getByElement(elementHash: String): List<ElementStateHistoryDTO> = withContext(Dispatchers.Default) {
        queries.getByElement(elementHash).executeAsList().map { it.toElementStateHistoryDTO() }
    }

    override suspend fun getByScreen(screenHash: String): List<ElementStateHistoryDTO> = withContext(Dispatchers.Default) {
        queries.getByScreen(screenHash).executeAsList().map { it.toElementStateHistoryDTO() }
    }

    override suspend fun getByStateType(stateType: String): List<ElementStateHistoryDTO> = withContext(Dispatchers.Default) {
        queries.getByStateType(stateType).executeAsList().map { it.toElementStateHistoryDTO() }
    }

    override suspend fun getByTimeRange(startTime: Long, endTime: Long): List<ElementStateHistoryDTO> = withContext(Dispatchers.Default) {
        queries.getByTimeRange(startTime, endTime).executeAsList().map { it.toElementStateHistoryDTO() }
    }

    override suspend fun getByTrigger(triggeredBy: String): List<ElementStateHistoryDTO> = withContext(Dispatchers.Default) {
        queries.getByTrigger(triggeredBy).executeAsList().map { it.toElementStateHistoryDTO() }
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

    override suspend fun getCurrentState(elementHash: String, stateType: String): ElementStateHistoryDTO? = withContext(Dispatchers.Default) {
        // Get all state changes for this element and type, ordered by most recent
        queries.getByElement(elementHash)
            .executeAsList()
            .filter { it.stateType == stateType }
            .maxByOrNull { it.changedAt }
            ?.toElementStateHistoryDTO()
    }
}
