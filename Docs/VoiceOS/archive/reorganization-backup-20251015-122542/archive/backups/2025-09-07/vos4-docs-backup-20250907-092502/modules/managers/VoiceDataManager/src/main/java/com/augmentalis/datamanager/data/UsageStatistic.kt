// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.data

import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.UsageStatistic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageStatisticRepo {
    
    private val dao get() = DatabaseManager.database.usageStatisticDao()
    
    // Direct CRUD implementation - no inheritance
    suspend fun insert(entity: UsageStatistic): Long = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }

    suspend fun insertAll(entities: List<UsageStatistic>): List<Long> = withContext(Dispatchers.IO) {
        dao.insertAll(entities)
    }

    suspend fun update(entity: UsageStatistic) = withContext(Dispatchers.IO) {
        dao.update(entity)
    }

    suspend fun delete(entity: UsageStatistic) = withContext(Dispatchers.IO) {
        dao.delete(entity)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    suspend fun getById(id: Long): UsageStatistic? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun getAll(): List<UsageStatistic> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun count(): Long = withContext(Dispatchers.IO) {
        dao.count()
    }

    suspend fun query(queryBuilder: () -> List<UsageStatistic>): List<UsageStatistic> = withContext(Dispatchers.IO) {
        queryBuilder()
    }
    
    suspend fun getStatsByType(type: String): List<UsageStatistic> = withContext(Dispatchers.IO) {
        dao.getByType(type)
    }
    
    suspend fun getMostUsedItems(type: String, limit: Int = 10): List<UsageStatistic> = withContext(Dispatchers.IO) {
        dao.getTopByType(type, limit)
    }
    
    suspend fun updateStatistic(type: String, identifier: String, timeMs: Long, success: Boolean) = withContext(Dispatchers.IO) {
        val existing = dao.getByTypeAndIdentifier(type, identifier).firstOrNull()
        
        if (existing != null) {
            val newCount = existing.count + 1
            val newTotalTime = existing.totalTimeMs + timeMs
            val successCount = if (success) (existing.successRate * existing.count / 100f + 1) else (existing.successRate * existing.count / 100f)
            val newSuccessRate = (successCount * 100f) / newCount
            
            dao.update(existing.copy(
                count = newCount,
                totalTimeMs = newTotalTime,
                successRate = newSuccessRate,
                lastUsed = System.currentTimeMillis()
            ))
        } else {
            dao.insert(UsageStatistic(
                type = type,
                identifier = identifier,
                count = 1,
                totalTimeMs = timeMs,
                successRate = if (success) 100f else 0f,
                lastUsed = System.currentTimeMillis(),
                dateRecorded = System.currentTimeMillis()
            ))
        }
    }
    
    suspend fun cleanupOldStatistics(days: Int) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        
        // Aggregate old data before deletion
        val oldStats = dao.getByDateRange(0, cutoffTime)
        
        // Group by type and identifier for aggregation
        val aggregated = oldStats.groupBy { "${it.type}_${it.identifier}" }
        
        @Suppress("UNUSED_PARAMETER")
        aggregated.forEach { (_, stats) ->
            if (stats.size > 1) {
                val totalCount = stats.sumOf { it.count }
                val totalTime = stats.sumOf { it.totalTimeMs }
                val avgSuccessRate = stats.map { it.successRate }.average().toFloat()
                val lastUsed = stats.maxOf { it.lastUsed }
                
                // Keep one aggregated record
                val aggregatedStat = stats.first().copy(
                    count = totalCount,
                    totalTimeMs = totalTime,
                    successRate = avgSuccessRate,
                    lastUsed = lastUsed,
                    dateRecorded = cutoffTime
                )
                dao.update(aggregatedStat)
                
                // Remove individual old records
                stats.drop(1).forEach { dao.delete(it) }
            }
        }
    }
    
    suspend fun getRecentStatistics(days: Int = 7): List<UsageStatistic> = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        dao.getRecentlyUsed(cutoffTime)
    }
}