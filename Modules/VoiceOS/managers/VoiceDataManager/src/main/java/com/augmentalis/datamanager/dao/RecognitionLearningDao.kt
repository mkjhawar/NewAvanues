// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.RecognitionLearning
import kotlinx.coroutines.flow.Flow

@Dao
interface RecognitionLearningDao : BaseDao<RecognitionLearning> {
    
    @Query("SELECT * FROM recognition_learning ORDER BY timestamp DESC")
    suspend fun getAll(): List<RecognitionLearning>
    
    @Query("SELECT * FROM recognition_learning WHERE id = :id")
    suspend fun getById(id: Long): RecognitionLearning?
    
    @Query("DELETE FROM recognition_learning")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM recognition_learning WHERE engine = :engine ORDER BY timestamp DESC")
    suspend fun getByEngine(engine: String): List<RecognitionLearning>
    
    @Query("SELECT * FROM recognition_learning WHERE type = :type ORDER BY timestamp DESC")
    suspend fun getByType(type: String): List<RecognitionLearning>
    
    @Query("SELECT * FROM recognition_learning WHERE keyValue = :keyValue ORDER BY timestamp DESC")
    suspend fun getByKeyValue(keyValue: String): List<RecognitionLearning>
    
    @Query("SELECT * FROM recognition_learning WHERE engine = :engine AND type = :type ORDER BY timestamp DESC")
    suspend fun getByEngineAndType(engine: String, type: String): List<RecognitionLearning>
    
    @Query("SELECT * FROM recognition_learning WHERE engine = :engine AND keyValue = :keyValue ORDER BY timestamp DESC")
    suspend fun getByEngineAndKeyValue(engine: String, keyValue: String): List<RecognitionLearning>
    
    @Query("SELECT * FROM recognition_learning WHERE type = :type AND keyValue = :keyValue ORDER BY timestamp DESC")
    suspend fun getByTypeAndKeyValue(type: String, keyValue: String): List<RecognitionLearning>
    
    @Query("SELECT * FROM recognition_learning WHERE engine = :engine AND type = :type AND keyValue = :keyValue")
    suspend fun getByEngineTypeAndKey(engine: String, type: String, keyValue: String): RecognitionLearning?
    
    @Query("SELECT * FROM recognition_learning WHERE lastUsed > :since ORDER BY lastUsed DESC")
    suspend fun getRecentlyUsed(since: Long): List<RecognitionLearning>
    
    @Query("SELECT * FROM recognition_learning WHERE usageCount >= :minCount ORDER BY usageCount DESC")
    suspend fun getByMinUsageCount(minCount: Int): List<RecognitionLearning>
    
    @Query("SELECT * FROM recognition_learning WHERE confidence >= :minConfidence ORDER BY confidence DESC")
    suspend fun getByMinConfidence(minConfidence: Float): List<RecognitionLearning>
    
    @Query("UPDATE recognition_learning SET usageCount = usageCount + 1, lastUsed = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: Long)
    
    @Query("UPDATE recognition_learning SET mappedValue = :mappedValue WHERE id = :id")
    suspend fun updateMappedValue(id: Long, mappedValue: String)
    
    @Query("DELETE FROM recognition_learning WHERE engine = :engine")
    suspend fun deleteByEngine(engine: String): Int
    
    @Query("DELETE FROM recognition_learning WHERE type = :type")
    suspend fun deleteByType(type: String)
    
    @Query("DELETE FROM recognition_learning WHERE engine = :engine AND type = :type")
    suspend fun deleteByEngineAndType(engine: String, type: String): Int
    
    @Query("DELETE FROM recognition_learning WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long): Int

    // Additional queries for ConfidenceTrackingRepository

    @Query("SELECT * FROM recognition_learning WHERE keyValue = :key AND engine = :engine LIMIT 1")
    suspend fun findByKeyAndEngine(key: String, engine: String): RecognitionLearning?

    @Query("SELECT * FROM recognition_learning WHERE keyValue = :key AND engine = :engine AND type = :type LIMIT 1")
    suspend fun findByKeyAndEngineAndType(key: String, engine: String, type: String): RecognitionLearning?

    @Query("SELECT * FROM recognition_learning WHERE engine = :engine")
    suspend fun findByEngine(engine: String): List<RecognitionLearning>

    @Query("SELECT * FROM recognition_learning WHERE type = :type")
    suspend fun findByType(type: String): List<RecognitionLearning>

    @Query("SELECT * FROM recognition_learning WHERE engine = :engine AND type = :type")
    suspend fun findByEngineAndType(engine: String, type: String): List<RecognitionLearning>

    @Query("SELECT * FROM recognition_learning WHERE engine = :engine AND type = :type")
    fun findByEngineAndTypeFlow(engine: String, type: String): Flow<List<RecognitionLearning>>

    @Query("SELECT * FROM recognition_learning WHERE type = :type")
    fun findByTypeFlow(type: String): Flow<List<RecognitionLearning>>
}