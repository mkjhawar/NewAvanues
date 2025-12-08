// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.data

import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.UserSequence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserSequenceRepo {
    
    private val dao get() = DatabaseManager.database.userSequenceDao()
    
    // Direct CRUD implementation - no inheritance
    suspend fun insert(entity: UserSequence): Long = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }

    suspend fun insertAll(entities: List<UserSequence>): List<Long> = withContext(Dispatchers.IO) {
        dao.insertAll(entities)
    }

    suspend fun update(entity: UserSequence) = withContext(Dispatchers.IO) {
        dao.update(entity)
    }

    suspend fun delete(entity: UserSequence) = withContext(Dispatchers.IO) {
        dao.delete(entity)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    suspend fun getById(id: Long): UserSequence? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun getAll(): List<UserSequence> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun count(): Long = withContext(Dispatchers.IO) {
        dao.count()
    }

    suspend fun query(queryBuilder: () -> List<UserSequence>): List<UserSequence> = withContext(Dispatchers.IO) {
        queryBuilder()
    }
    
    suspend fun getSequencesByLanguage(language: String): List<UserSequence> = withContext(Dispatchers.IO) {
        dao.getByLanguage(language)
    }
    
    suspend fun findSequenceByTrigger(trigger: String): UserSequence? = withContext(Dispatchers.IO) {
        dao.getByTriggerPhrase(trigger)
    }
    
    suspend fun getMostUsedSequences(limit: Int = 10): List<UserSequence> = withContext(Dispatchers.IO) {
        dao.getMostUsed(limit)
    }
    
    suspend fun incrementUsageCount(sequenceId: Long) = withContext(Dispatchers.IO) {
        dao.getById(sequenceId)?.let { sequence ->
            val updated = sequence.copy(
                usageCount = sequence.usageCount + 1,
                lastUsed = System.currentTimeMillis()
            )
            dao.update(updated)
        }
    }
    
    suspend fun getRecentSequences(limit: Int = 20): List<UserSequence> = withContext(Dispatchers.IO) {
        val recent = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // Last 7 days
        dao.getRecentlyUsed(recent).take(limit)
    }
}