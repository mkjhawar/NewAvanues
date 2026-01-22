// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.database.repositories

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.UserPreferenceDTO
import com.augmentalis.database.dto.toDTO
import com.augmentalis.database.repositories.IUserPreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * SQLDelight implementation of IUserPreferenceRepository.
 */
class SQLDelightUserPreferenceRepository(
    private val database: VoiceOSDatabase
) : IUserPreferenceRepository {

    private val queries = database.userPreferenceQueries

    override suspend fun getValue(key: String): String? = withContext(Dispatchers.Default) {
        queries.getValue(key).executeAsOneOrNull()
    }

    override suspend fun getValue(key: String, default: String): String = withContext(Dispatchers.Default) {
        queries.getValue(key).executeAsOneOrNull() ?: default
    }

    override suspend fun setValue(key: String, value: String, type: String) = withContext(Dispatchers.Default) {
        queries.insert(key, value, type, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun exists(key: String): Boolean = withContext(Dispatchers.Default) {
        queries.exists(key).executeAsOne() > 0
    }

    override suspend fun getAll(): List<UserPreferenceDTO> = withContext(Dispatchers.Default) {
        queries.getAll().executeAsList().map { it.toDTO() }
    }

    override suspend fun getByType(type: String): List<UserPreferenceDTO> = withContext(Dispatchers.Default) {
        queries.getByType(type).executeAsList().map { it.toDTO() }
    }

    override suspend fun delete(key: String) = withContext(Dispatchers.Default) {
        queries.deleteByKey(key)
    }

    override suspend fun deleteByType(type: String) = withContext(Dispatchers.Default) {
        queries.deleteByType(type)
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }
}
