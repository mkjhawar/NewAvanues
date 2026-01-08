/**
 * UUIDAnalyticsDao.kt - Data Access Object for UUID analytics
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/dao/UUIDAnalyticsDao.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * DAO for UUID analytics CRUD and query operations
 */

package com.augmentalis.uuidcreator.database.dao

import androidx.room.*
import com.augmentalis.uuidcreator.database.entities.UUIDAnalyticsEntity

/**
 * Data Access Object for UUID analytics
 *
 * Provides operations for tracking and querying usage statistics.
 */
@Dao
interface UUIDAnalyticsDao {

    // ==================== CREATE ====================

    /**
     * Insert analytics record
     * Replaces existing record with same UUID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analytics: UUIDAnalyticsEntity)

    /**
     * Insert multiple analytics records
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(analyticsList: List<UUIDAnalyticsEntity>)

    // ==================== READ ====================

    /**
     * Get all analytics records
     */
    @Query("SELECT * FROM uuid_analytics")
    suspend fun getAll(): List<UUIDAnalyticsEntity>

    /**
     * Get analytics for specific UUID
     */
    @Query("SELECT * FROM uuid_analytics WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): UUIDAnalyticsEntity?

    /**
     * Get most frequently accessed elements
     */
    @Query("SELECT * FROM uuid_analytics ORDER BY access_count DESC LIMIT :limit")
    suspend fun getMostUsed(limit: Int = 10): List<UUIDAnalyticsEntity>

    /**
     * Get least frequently accessed elements
     */
    @Query("SELECT * FROM uuid_analytics ORDER BY access_count ASC LIMIT :limit")
    suspend fun getLeastUsed(limit: Int = 10): List<UUIDAnalyticsEntity>

    /**
     * Get most recently accessed elements
     */
    @Query("SELECT * FROM uuid_analytics ORDER BY last_accessed DESC LIMIT :limit")
    suspend fun getRecentlyUsed(limit: Int = 10): List<UUIDAnalyticsEntity>

    /**
     * Get elements by lifecycle state
     */
    @Query("SELECT * FROM uuid_analytics WHERE lifecycle_state = :state")
    suspend fun getByLifecycleState(state: String): List<UUIDAnalyticsEntity>

    /**
     * Get elements with high success rate (> threshold)
     */
    @Query("SELECT * FROM uuid_analytics WHERE success_count > 0 AND (CAST(success_count AS FLOAT) / (success_count + failure_count)) >= :threshold ORDER BY success_count DESC")
    suspend fun getHighSuccessRate(threshold: Float = 0.8f): List<UUIDAnalyticsEntity>

    /**
     * Get elements with high failure rate (> threshold)
     */
    @Query("SELECT * FROM uuid_analytics WHERE failure_count > 0 AND (CAST(failure_count AS FLOAT) / (success_count + failure_count)) >= :threshold ORDER BY failure_count DESC")
    suspend fun getHighFailureRate(threshold: Float = 0.2f): List<UUIDAnalyticsEntity>

    /**
     * Get total access count across all elements
     */
    @Query("SELECT SUM(access_count) FROM uuid_analytics")
    suspend fun getTotalAccessCount(): Long?

    /**
     * Get average execution time
     */
    @Query("SELECT AVG(execution_time_ms) FROM uuid_analytics WHERE access_count > 0")
    suspend fun getAverageExecutionTime(): Double?

    /**
     * Get elements accessed within time range
     */
    @Query("SELECT * FROM uuid_analytics WHERE last_accessed >= :startTime AND last_accessed <= :endTime")
    suspend fun getAccessedInRange(startTime: Long, endTime: Long): List<UUIDAnalyticsEntity>

    /**
     * Get never accessed elements
     */
    @Query("SELECT * FROM uuid_analytics WHERE access_count = 0")
    suspend fun getNeverAccessed(): List<UUIDAnalyticsEntity>

    /**
     * Get count by lifecycle state
     */
    @Query("SELECT COUNT(*) FROM uuid_analytics WHERE lifecycle_state = :state")
    suspend fun getCountByState(state: String): Int

    // ==================== UPDATE ====================

    /**
     * Update analytics record
     */
    @Update
    suspend fun update(analytics: UUIDAnalyticsEntity)

    /**
     * Increment access count and update last accessed timestamp
     */
    @Query("UPDATE uuid_analytics SET access_count = access_count + 1, last_accessed = :timestamp WHERE uuid = :uuid")
    suspend fun incrementAccessCount(uuid: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Increment success count
     */
    @Query("UPDATE uuid_analytics SET success_count = success_count + 1 WHERE uuid = :uuid")
    suspend fun incrementSuccessCount(uuid: String)

    /**
     * Increment failure count
     */
    @Query("UPDATE uuid_analytics SET failure_count = failure_count + 1 WHERE uuid = :uuid")
    suspend fun incrementFailureCount(uuid: String)

    /**
     * Add execution time
     */
    @Query("UPDATE uuid_analytics SET execution_time_ms = execution_time_ms + :timeMs WHERE uuid = :uuid")
    suspend fun addExecutionTime(uuid: String, timeMs: Long)

    /**
     * Update lifecycle state
     */
    @Query("UPDATE uuid_analytics SET lifecycle_state = :state WHERE uuid = :uuid")
    suspend fun updateLifecycleState(uuid: String, state: String)

    /**
     * Update last accessed timestamp
     */
    @Query("UPDATE uuid_analytics SET last_accessed = :timestamp WHERE uuid = :uuid")
    suspend fun updateLastAccessed(uuid: String, timestamp: Long = System.currentTimeMillis())

    // ==================== DELETE ====================

    /**
     * Delete analytics record
     */
    @Delete
    suspend fun delete(analytics: UUIDAnalyticsEntity)

    /**
     * Delete analytics for specific UUID
     */
    @Query("DELETE FROM uuid_analytics WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)

    /**
     * Delete analytics for never accessed elements
     */
    @Query("DELETE FROM uuid_analytics WHERE access_count = 0")
    suspend fun deleteNeverAccessed()

    /**
     * Delete analytics older than timestamp
     */
    @Query("DELETE FROM uuid_analytics WHERE last_accessed < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    /**
     * Delete all analytics
     */
    @Query("DELETE FROM uuid_analytics")
    suspend fun deleteAll()
}
