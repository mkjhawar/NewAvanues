// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.data

import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.TouchGesture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TouchGestureRepo {
    
    private val dao get() = DatabaseManager.database.touchGestureDao()
    
    // Direct CRUD implementation - no inheritance
    suspend fun insert(entity: TouchGesture): Long = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }

    suspend fun insertAll(entities: List<TouchGesture>): List<Long> = withContext(Dispatchers.IO) {
        dao.insertAll(entities)
    }

    suspend fun update(entity: TouchGesture) = withContext(Dispatchers.IO) {
        dao.update(entity)
    }

    suspend fun delete(entity: TouchGesture) = withContext(Dispatchers.IO) {
        dao.delete(entity)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.getById(id)?.let { dao.delete(it) }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    suspend fun getById(id: Long): TouchGesture? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun getAll(): List<TouchGesture> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun count(): Long = withContext(Dispatchers.IO) {
        dao.getAll().size.toLong()
    }
    
    suspend fun getUserGestures(): List<TouchGesture> = withContext(Dispatchers.IO) {
        dao.getUserGesturesByUsage()
    }
    
    suspend fun getSystemGestures(): List<TouchGesture> = withContext(Dispatchers.IO) {
        dao.getSystemGestures()
    }
    
    suspend fun getMostUsedGestures(limit: Int = 20): List<TouchGesture> = withContext(Dispatchers.IO) {
        dao.getByMinUsageCount(1).take(limit)
    }
    
    suspend fun findGestureByName(name: String): TouchGesture? = withContext(Dispatchers.IO) {
        dao.getByName(name)
    }
    
    suspend fun incrementUsageCount(gestureId: Long) = withContext(Dispatchers.IO) {
        dao.incrementUsage(gestureId, System.currentTimeMillis())
    }
    
    suspend fun cleanupUnusedSystemGestures(days: Int = 90) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        val oldUnusedGestures = dao.getSystemGestures().filter { 
            it.lastUsed < cutoffTime && it.usageCount == 0 
        }
        dao.deleteAll(oldUnusedGestures)
    }
}