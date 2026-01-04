// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.ContextPreferenceDTO
import com.augmentalis.database.dto.toDTO
import com.augmentalis.database.repositories.IContextPreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IContextPreferenceRepository.
 */
class SQLDelightContextPreferenceRepository(
    private val database: VoiceOSDatabase
) : IContextPreferenceRepository {

    private val queries = database.contextPreferenceQueries

    override suspend fun insert(preference: ContextPreferenceDTO): Long =
        withContext(Dispatchers.Default) {
            queries.insertPreference(
                command_id = preference.commandId,
                context_key = preference.contextKey,
                usage_count = preference.usageCount,
                success_count = preference.successCount,
                last_used_timestamp = preference.lastUsedTimestamp
            )
            queries.transactionWithResult {
                queries.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun getAll(): List<ContextPreferenceDTO> = withContext(Dispatchers.Default) {
        queries.getAllPreferences().executeAsList().map { it.toDTO() }
    }

    override suspend fun get(commandId: String, contextKey: String): ContextPreferenceDTO? =
        withContext(Dispatchers.Default) {
            queries.getPreference(commandId, contextKey).executeAsOneOrNull()?.toDTO()
        }

    override suspend fun getForCommand(commandId: String): List<ContextPreferenceDTO> =
        withContext(Dispatchers.Default) {
            queries.getPreferencesForCommand(commandId).executeAsList().map { it.toDTO() }
        }

    override suspend fun getMostUsedCommands(limit: Long): List<Pair<String, Long>> =
        withContext(Dispatchers.Default) {
            queries.getMostUsedCommands(limit).executeAsList().map {
                it.command_id to (it.total_usage ?: 0L)
            }
        }

    override suspend fun getMostUsedContexts(limit: Long): List<Pair<String, Long>> =
        withContext(Dispatchers.Default) {
            queries.getMostUsedContexts(limit).executeAsList().map {
                it.context_key to (it.total_usage ?: 0L)
            }
        }

    override suspend fun getRecent(limit: Long): List<ContextPreferenceDTO> =
        withContext(Dispatchers.Default) {
            queries.getRecentPreferences(limit).executeAsList().map { it.toDTO() }
        }

    override suspend fun countCommands(): Long = withContext(Dispatchers.Default) {
        queries.countCommandsTracked().executeAsOne()
    }

    override suspend fun countContexts(): Long = withContext(Dispatchers.Default) {
        queries.countContextsTracked().executeAsOne()
    }

    override suspend fun getAverageSuccessRate(): Double = withContext(Dispatchers.Default) {
        queries.getAverageSuccessRate().executeAsOne() ?: 0.0
    }

    override suspend fun update(preference: ContextPreferenceDTO) = withContext(Dispatchers.Default) {
        queries.updatePreference(
            usage_count = preference.usageCount,
            success_count = preference.successCount,
            last_used_timestamp = preference.lastUsedTimestamp,
            id = preference.id
        )
    }

    override suspend fun incrementCounts(
        commandId: String,
        contextKey: String,
        successIncrement: Long,
        timestamp: Long
    ) = withContext(Dispatchers.Default) {
        queries.incrementCounts(
            successIncrement,
            timestamp,
            commandId,
            contextKey
        )
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.Default) {
        queries.deletePreference(id)
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAllPreferences()
    }

    override suspend fun applyTimeDecay(currentTime: Long, decayFactor: Float) =
        withContext(Dispatchers.Default) {
            // Time decay implementation:
            // For each record, reduce its weight based on age
            // This is a placeholder - actual implementation would update records
            // For now, we'll skip this as it requires schema changes
            // TODO: Implement proper time decay with weighted counts
        }
}
