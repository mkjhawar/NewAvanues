/**
 * SQLDelightScreenTransitionRepository.kt - SQLDelight implementation of IScreenTransitionRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 */

package com.avanues.database.repositories.impl

import com.avanues.database.VoiceOSDatabase
import com.avanues.database.dto.ScreenTransitionDTO
import com.avanues.database.dto.toScreenTransitionDTO
import com.avanues.database.repositories.IScreenTransitionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IScreenTransitionRepository.
 */
class SQLDelightScreenTransitionRepository(
    private val database: VoiceOSDatabase
) : IScreenTransitionRepository {

    private val queries = database.screenTransitionQueries

    override suspend fun insert(transition: ScreenTransitionDTO): Long = withContext(Dispatchers.Default) {
        queries.insert(
            fromScreenHash = transition.fromScreenHash,
            toScreenHash = transition.toScreenHash,
            triggerElementHash = transition.triggerElementHash,
            triggerAction = transition.triggerAction,
            transitionCount = transition.transitionCount,
            avgDurationMs = transition.avgDurationMs,
            lastTransitionAt = transition.lastTransitionAt
        )
        queries.count().executeAsOne()
    }

    override suspend fun getById(id: Long): ScreenTransitionDTO? = withContext(Dispatchers.Default) {
        queries.getById(id).executeAsOneOrNull()?.toScreenTransitionDTO()
    }

    override suspend fun getFromScreen(fromScreenHash: String): List<ScreenTransitionDTO> = withContext(Dispatchers.Default) {
        queries.getFromScreen(fromScreenHash).executeAsList().map { it.toScreenTransitionDTO() }
    }

    override suspend fun getToScreen(toScreenHash: String): List<ScreenTransitionDTO> = withContext(Dispatchers.Default) {
        queries.getToScreen(toScreenHash).executeAsList().map { it.toScreenTransitionDTO() }
    }

    override suspend fun getByTrigger(triggerElementHash: String): List<ScreenTransitionDTO> = withContext(Dispatchers.Default) {
        queries.getByTrigger(triggerElementHash).executeAsList().map { it.toScreenTransitionDTO() }
    }

    override suspend fun getFrequent(limit: Long): List<ScreenTransitionDTO> = withContext(Dispatchers.Default) {
        queries.getFrequent(limit).executeAsList().map { it.toScreenTransitionDTO() }
    }

    override suspend fun recordTransition(
        fromScreenHash: String,
        toScreenHash: String,
        durationMs: Long,
        timestamp: Long
    ) = withContext(Dispatchers.Default) {
        // For recordTransition, we always use NULL trigger and 'navigation' action
        val triggerElement: String? = null
        val triggerAction = "navigation"

        val existing = queries.getExistingTransition(
            fromScreenHash, toScreenHash, triggerElement, triggerAction
        ).executeAsOneOrNull()

        if (existing != null) {
            // Update: recalculate average duration
            val newCount = existing.transitionCount + 1
            val newAvg = (existing.avgDurationMs * existing.transitionCount + durationMs) / newCount
            queries.updateTransition(
                newAvg, timestamp, fromScreenHash, toScreenHash, triggerElement, triggerAction
            )
        } else {
            // Insert new transition
            queries.insertTransition(
                fromScreenHash, toScreenHash, triggerElement, triggerAction, durationMs, timestamp
            )
        }
    }

    override suspend fun deleteById(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteById(id)
    }

    override suspend fun deleteByScreen(screenHash: String) = withContext(Dispatchers.Default) {
        queries.deleteByScreen(screenHash, screenHash)
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }
}
