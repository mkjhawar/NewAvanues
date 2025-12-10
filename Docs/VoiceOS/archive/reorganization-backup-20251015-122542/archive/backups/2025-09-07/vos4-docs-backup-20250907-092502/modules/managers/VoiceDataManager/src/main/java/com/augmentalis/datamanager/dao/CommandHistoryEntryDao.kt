// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.CommandHistoryEntry

@Dao
interface CommandHistoryEntryDao : BaseDao<CommandHistoryEntry> {
    
    @Query("SELECT * FROM command_history_entry ORDER BY timestamp DESC")
    suspend fun getAll(): List<CommandHistoryEntry>
    
    @Query("SELECT * FROM command_history_entry WHERE id = :id")
    suspend fun getById(id: Long): CommandHistoryEntry?
    
    @Query("DELETE FROM command_history_entry WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM command_history_entry")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM command_history_entry")
    suspend fun count(): Long
    
    @Query("SELECT * FROM command_history_entry WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<CommandHistoryEntry>
    
    @Query("SELECT * FROM command_history_entry WHERE language = :language ORDER BY timestamp DESC")
    suspend fun getByLanguage(language: String): List<CommandHistoryEntry>
    
    @Query("SELECT * FROM command_history_entry WHERE language = :language AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getByLanguageAndTimeRange(language: String, startTime: Long, endTime: Long): List<CommandHistoryEntry>
    
    @Query("SELECT * FROM command_history_entry WHERE success = :success ORDER BY timestamp DESC")
    suspend fun getBySuccess(success: Boolean): List<CommandHistoryEntry>
    
    @Query("SELECT * FROM command_history_entry WHERE engineUsed = :engine ORDER BY timestamp DESC")
    suspend fun getByEngine(engine: String): List<CommandHistoryEntry>
    
    @Query("SELECT * FROM command_history_entry ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<CommandHistoryEntry>
    
    @Query("SELECT * FROM command_history_entry ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentCommands(limit: Int): List<CommandHistoryEntry>
    
    @Query("SELECT * FROM command_history_entry WHERE success = 1 GROUP BY originalText ORDER BY SUM(usageCount) DESC LIMIT :limit")
    suspend fun getMostUsedCommands(limit: Int): List<CommandHistoryEntry>
    
    @Query("SELECT * FROM command_history_entry WHERE language = :language ORDER BY timestamp DESC")
    suspend fun getCommandsByLanguage(language: String): List<CommandHistoryEntry>
    
    @Query("SELECT CASE WHEN COUNT(*) = 0 THEN 0 ELSE CAST(SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS REAL) / COUNT(*) END FROM command_history_entry")
    suspend fun getSuccessRate(): Float
    
    @Query("DELETE FROM command_history_entry WHERE timestamp < :cutoffTime AND id NOT IN (SELECT id FROM command_history_entry ORDER BY timestamp DESC LIMIT :retainCount)")
    suspend fun cleanupOldEntries(cutoffTime: Long, retainCount: Int)
    
    @Query("DELETE FROM command_history_entry WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}