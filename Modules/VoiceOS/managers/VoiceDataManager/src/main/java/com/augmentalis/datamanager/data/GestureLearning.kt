// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.data

import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.GestureLearningData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GestureLearningRepo {
    
    private val dao get() = DatabaseManager.database.gestureLearningDataDao()
    
    // Direct CRUD implementation - no inheritance
    suspend fun insert(entity: GestureLearningData): Long = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }

    suspend fun insertAll(entities: List<GestureLearningData>): List<Long> = withContext(Dispatchers.IO) {
        dao.insertAll(entities)
    }

    suspend fun update(entity: GestureLearningData) = withContext(Dispatchers.IO) {
        dao.update(entity)
    }

    suspend fun delete(entity: GestureLearningData) = withContext(Dispatchers.IO) {
        dao.delete(entity)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    suspend fun getById(id: Long): GestureLearningData? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun getAll(): List<GestureLearningData> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun count(): Long = withContext(Dispatchers.IO) {
        dao.count()
    }

    suspend fun query(queryBuilder: () -> List<GestureLearningData>): List<GestureLearningData> = withContext(Dispatchers.IO) {
        queryBuilder()
    }
    
    suspend fun getDataForGesture(gestureId: Long, userId: String = "default"): GestureLearningData? = withContext(Dispatchers.IO) {
        dao.getByGestureIdAndUserId(gestureId, userId)
    }
    
    suspend fun updateLearningData(
        gestureId: Long,
        success: Boolean,
        velocity: Float,
        pressure: Float? = null,
        mistakes: List<String> = emptyList(),
        zones: Map<String, Int> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        val existing = getDataForGesture(gestureId, "default")
        
        if (existing != null) {
            val totalAttempts = (existing.successRate * 100).toInt() + if (success) 1 else 0
            val newSuccessRate = if (success) {
                ((existing.successRate * 100 + 1) / (totalAttempts + 1))
            } else {
                (existing.successRate * 100 / (totalAttempts + 1))
            }
            
            val avgVelocity = (existing.averageVelocity + velocity) / 2
            val avgPressure = if (pressure != null && existing.averagePressure != null) {
                (existing.averagePressure + pressure) / 2
            } else {
                pressure ?: existing.averagePressure
            }
            
            dao.update(existing.copy(
                successRate = newSuccessRate,
                averageVelocity = avgVelocity,
                averagePressure = avgPressure,
                commonMistakes = updateMistakes(existing.commonMistakes, mistakes),
                zonePreferences = updateZones(existing.zonePreferences, zones)
            ))
        } else {
            dao.insert(GestureLearningData(
                gestureId = gestureId,
                successRate = if (success) 1f else 0f,
                averageVelocity = velocity,
                averagePressure = pressure,
                commonMistakes = mistakes.joinToString(","),
                zonePreferences = zones.entries.joinToString(",") { "${it.key}:${it.value}" }
            ))
        }
    }
    
    private fun updateMistakes(existing: String, new: List<String>): String {
        val current = if (existing.isNotEmpty()) existing.split(",") else emptyList()
        return (current + new).distinct().takeLast(10).joinToString(",")
    }
    
    private fun updateZones(existing: String, new: Map<String, Int>): String {
        val current = if (existing.isNotEmpty()) {
            existing.split(",").associate {
                val parts = it.split(":")
                parts[0] to parts[1].toInt()
            }.toMutableMap()
        } else {
            mutableMapOf()
        }
        
        new.forEach { (zone, count) ->
            current[zone] = (current[zone] ?: 0) + count
        }
        
        return current.entries.joinToString(",") { "${it.key}:${it.value}" }
    }
    
    suspend fun getBestPerformingGestures(limit: Int = 10): List<GestureLearningData> = withContext(Dispatchers.IO) {
        dao.getByMinSuccessRate(0.7f).take(limit)
    }
}