// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.UsageStatistic

@Dao
interface UsageStatisticDao : BaseDao<UsageStatistic> {
    
    @Query("SELECT * FROM usage_statistics ORDER BY dateRecorded DESC")
    suspend fun getAll(): List<UsageStatistic>
    
    @Query("SELECT * FROM usage_statistics WHERE id = :id")
    suspend fun getById(id: Long): UsageStatistic?
    
    @Query("DELETE FROM usage_statistics WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM usage_statistics")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM usage_statistics")
    suspend fun count(): Long
    
    @Query("SELECT * FROM usage_statistics WHERE type = :type ORDER BY count DESC")
    suspend fun getByType(type: String): List<UsageStatistic>
    
    @Query("SELECT * FROM usage_statistics WHERE identifier = :identifier ORDER BY dateRecorded DESC")
    suspend fun getByIdentifier(identifier: String): List<UsageStatistic>
    
    @Query("SELECT * FROM usage_statistics WHERE type = :type AND identifier = :identifier ORDER BY dateRecorded DESC")
    suspend fun getByTypeAndIdentifier(type: String, identifier: String): List<UsageStatistic>
    
    @Query("SELECT * FROM usage_statistics WHERE type = :type AND identifier = :identifier AND dateRecorded = :date")
    suspend fun getByTypeIdentifierAndDate(type: String, identifier: String, date: Long): UsageStatistic?
    
    @Query("SELECT * FROM usage_statistics WHERE dateRecorded BETWEEN :startDate AND :endDate ORDER BY dateRecorded DESC")
    suspend fun getByDateRange(startDate: Long, endDate: Long): List<UsageStatistic>
    
    @Query("SELECT * FROM usage_statistics WHERE type = :type AND dateRecorded BETWEEN :startDate AND :endDate ORDER BY count DESC")
    suspend fun getByTypeAndDateRange(type: String, startDate: Long, endDate: Long): List<UsageStatistic>
    
    @Query("SELECT * FROM usage_statistics WHERE count >= :minCount ORDER BY count DESC")
    suspend fun getByMinCount(minCount: Int): List<UsageStatistic>
    
    @Query("SELECT * FROM usage_statistics WHERE successRate >= :minRate ORDER BY successRate DESC")
    suspend fun getByMinSuccessRate(minRate: Float): List<UsageStatistic>
    
    @Query("SELECT * FROM usage_statistics WHERE lastUsed > :since ORDER BY lastUsed DESC")
    suspend fun getRecentlyUsed(since: Long): List<UsageStatistic>
    
    @Query("SELECT SUM(count) FROM usage_statistics WHERE type = :type")
    suspend fun getTotalCountByType(type: String): Int?
    
    @Query("SELECT AVG(successRate) FROM usage_statistics WHERE type = :type")
    suspend fun getAverageSuccessRateByType(type: String): Float?
    
    @Query("SELECT * FROM usage_statistics WHERE type = :type ORDER BY count DESC LIMIT :limit")
    suspend fun getTopByType(type: String, limit: Int): List<UsageStatistic>
    
    @Query("DELETE FROM usage_statistics WHERE dateRecorded < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}