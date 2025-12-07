// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.ErrorReport

@Dao
interface ErrorReportDao : BaseDao<ErrorReport> {
    
    @Query("SELECT * FROM error_report ORDER BY timestamp DESC")
    suspend fun getAll(): List<ErrorReport>
    
    @Query("SELECT * FROM error_report WHERE id = :id")
    suspend fun getById(id: Long): ErrorReport?
    
    @Query("DELETE FROM error_report WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM error_report")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM error_report")
    suspend fun count(): Long
    
    @Query("SELECT * FROM error_report WHERE errorType = :errorType ORDER BY timestamp DESC")
    suspend fun getByErrorType(errorType: String): List<ErrorReport>
    
    @Query("SELECT * FROM error_report WHERE sent = :sent ORDER BY timestamp DESC")
    suspend fun getBySentStatus(sent: Boolean): List<ErrorReport>
    
    @Query("SELECT * FROM error_report WHERE errorType = :errorType AND sent = :sent ORDER BY timestamp DESC")
    suspend fun getByErrorTypeAndSentStatus(errorType: String, sent: Boolean): List<ErrorReport>
    
    @Query("SELECT * FROM error_report WHERE moduleAffected = :module ORDER BY timestamp DESC")
    suspend fun getByModule(module: String): List<ErrorReport>
    
    @Query("SELECT * FROM error_report WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<ErrorReport>
    
    @Query("SELECT * FROM error_report WHERE sent = 0 ORDER BY timestamp ASC")
    suspend fun getPendingReports(): List<ErrorReport>
    
    @Query("UPDATE error_report SET sent = 1, sentDate = :sentDate WHERE id = :id")
    suspend fun markAsSent(id: Long, sentDate: Long)
    
    @Query("DELETE FROM error_report WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
    
    @Query("SELECT COUNT(*) FROM error_report WHERE errorType = :errorType AND timestamp > :since")
    suspend fun getErrorCountSince(errorType: String, since: Long): Int
}