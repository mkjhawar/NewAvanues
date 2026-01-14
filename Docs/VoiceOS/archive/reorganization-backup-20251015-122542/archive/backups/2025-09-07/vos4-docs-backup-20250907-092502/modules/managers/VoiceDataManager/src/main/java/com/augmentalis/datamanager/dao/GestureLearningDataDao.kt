// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.GestureLearningData

@Dao
interface GestureLearningDataDao : BaseDao<GestureLearningData> {
    
    @Query("SELECT * FROM gesture_learning_data ORDER BY gestureId ASC")
    suspend fun getAll(): List<GestureLearningData>
    
    @Query("SELECT * FROM gesture_learning_data WHERE id = :id")
    suspend fun getById(id: Long): GestureLearningData?
    
    @Query("DELETE FROM gesture_learning_data WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM gesture_learning_data")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM gesture_learning_data")
    suspend fun count(): Long
    
    @Query("SELECT * FROM gesture_learning_data WHERE gestureId = :gestureId")
    suspend fun getByGestureId(gestureId: Long): List<GestureLearningData>
    
    @Query("SELECT * FROM gesture_learning_data WHERE userId = :userId")
    suspend fun getByUserId(userId: String): List<GestureLearningData>
    
    @Query("SELECT * FROM gesture_learning_data WHERE gestureId = :gestureId AND userId = :userId")
    suspend fun getByGestureIdAndUserId(gestureId: Long, userId: String): GestureLearningData?
    
    @Query("SELECT * FROM gesture_learning_data WHERE successRate >= :minRate ORDER BY successRate DESC")
    suspend fun getByMinSuccessRate(minRate: Float): List<GestureLearningData>
    
    @Query("SELECT * FROM gesture_learning_data WHERE successRate < :maxRate ORDER BY successRate ASC")
    suspend fun getByMaxSuccessRate(maxRate: Float): List<GestureLearningData>
    
    @Query("SELECT AVG(successRate) FROM gesture_learning_data WHERE gestureId = :gestureId")
    suspend fun getAverageSuccessRateForGesture(gestureId: Long): Float?
    
    @Query("SELECT AVG(successRate) FROM gesture_learning_data WHERE userId = :userId")
    suspend fun getAverageSuccessRateForUser(userId: String): Float?
    
    @Query("UPDATE gesture_learning_data SET successRate = :successRate WHERE gestureId = :gestureId AND userId = :userId")
    suspend fun updateSuccessRate(gestureId: Long, userId: String, successRate: Float)
}