package com.augmentalis.ava.core.data.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.db.MemoryQueries
import com.augmentalis.ava.core.data.mapper.toDomain
import com.augmentalis.ava.core.data.mapper.toInsertParams
import com.augmentalis.ava.core.domain.model.Memory
import com.augmentalis.ava.core.domain.model.MemoryType
import com.augmentalis.ava.core.domain.repository.MemoryRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementation of MemoryRepository using SQLDelight
 *
 * Updated: Room removed, now uses SQLDelight queries directly
 */
class MemoryRepositoryImpl(
    private val memoryQueries: MemoryQueries
) : MemoryRepository {

    override suspend fun storeMemory(memory: Memory): Result<Memory> = withContext(Dispatchers.IO) {
        try {
            val params = memory.toInsertParams()
            memoryQueries.insert(
                id = params.id,
                memory_type = params.memory_type,
                content = params.content,
                embedding = params.embedding,
                importance = params.importance,
                created_at = params.created_at,
                last_accessed = params.last_accessed,
                access_count = params.access_count,
                metadata = params.metadata
            )
            Result.Success(memory)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to store memory: ${e.message}"
            )
        }
    }

    override suspend fun updateMemory(memory: Memory): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val params = memory.toInsertParams()
            // Using INSERT OR REPLACE for update
            memoryQueries.insert(
                id = params.id,
                memory_type = params.memory_type,
                content = params.content,
                embedding = params.embedding,
                importance = params.importance,
                created_at = params.created_at,
                last_accessed = params.last_accessed,
                access_count = params.access_count,
                metadata = params.metadata
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to update memory: ${e.message}"
            )
        }
    }

    override fun getAllMemories(): Flow<List<Memory>> {
        return memoryQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { memories -> memories.map { it.toDomain() } }
    }

    override fun getMemoriesByType(memoryType: MemoryType): Flow<List<Memory>> {
        return memoryQueries.selectByMemoryType(memoryType.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { memories -> memories.map { it.toDomain() } }
    }

    override fun getHighImportanceMemories(threshold: Float): Flow<List<Memory>> {
        return memoryQueries.selectByMinImportance(threshold.toDouble())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { memories -> memories.map { it.toDomain() } }
    }

    override suspend fun getMemoryById(id: String): Result<Memory> = withContext(Dispatchers.IO) {
        try {
            val memory = memoryQueries.selectById(id).executeAsOneOrNull()
            if (memory != null) {
                Result.Success(memory.toDomain())
            } else {
                Result.Error(
                    exception = NoSuchElementException("Memory not found"),
                    message = "Memory with id $id not found"
                )
            }
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to get memory: ${e.message}"
            )
        }
    }

    override suspend fun incrementAccessCount(id: String, timestamp: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            memoryQueries.updateAccessStats(last_accessed = timestamp, id = id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to increment access count: ${e.message}"
            )
        }
    }

    override fun searchMemories(query: String): Flow<List<Memory>> {
        return memoryQueries.searchByContent(query)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { memories -> memories.map { it.toDomain() } }
    }

    override suspend fun deleteMemory(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            memoryQueries.delete(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                exception = e,
                message = "Failed to delete memory: ${e.message}"
            )
        }
    }
}
