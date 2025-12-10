package com.augmentalis.ava.core.domain.repository

import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.domain.model.Memory
import com.augmentalis.ava.core.domain.model.MemoryType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for memory operations
 */
interface MemoryRepository {

    suspend fun storeMemory(memory: Memory): Result<Memory>

    suspend fun updateMemory(memory: Memory): Result<Unit>

    fun getAllMemories(): Flow<List<Memory>>

    fun getMemoriesByType(memoryType: MemoryType): Flow<List<Memory>>

    fun getHighImportanceMemories(threshold: Float): Flow<List<Memory>>

    suspend fun getMemoryById(id: String): Result<Memory>

    suspend fun incrementAccessCount(id: String, timestamp: Long): Result<Unit>

    fun searchMemories(query: String): Flow<List<Memory>>

    suspend fun deleteMemory(id: String): Result<Unit>
}
