// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.UserSequence

@Dao
interface UserSequenceDao : BaseDao<UserSequence> {
    
    @Query("SELECT * FROM user_sequence ORDER BY name ASC")
    suspend fun getAll(): List<UserSequence>
    
    @Query("SELECT * FROM user_sequence WHERE id = :id")
    suspend fun getById(id: Long): UserSequence?
    
    @Query("DELETE FROM user_sequence WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM user_sequence")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM user_sequence")
    suspend fun count(): Long
    
    @Query("SELECT * FROM user_sequence WHERE name = :name")
    suspend fun getByName(name: String): UserSequence?
    
    @Query("SELECT * FROM user_sequence WHERE language = :language ORDER BY name ASC")
    suspend fun getByLanguage(language: String): List<UserSequence>
    
    @Query("SELECT * FROM user_sequence WHERE name = :name AND language = :language")
    suspend fun getByNameAndLanguage(name: String, language: String): UserSequence?
    
    @Query("SELECT * FROM user_sequence WHERE triggerPhrase = :triggerPhrase")
    suspend fun getByTriggerPhrase(triggerPhrase: String): UserSequence?
    
    @Query("SELECT * FROM user_sequence WHERE triggerPhrase = :triggerPhrase AND language = :language")
    suspend fun getByTriggerPhraseAndLanguage(triggerPhrase: String, language: String): UserSequence?
    
    @Query("SELECT * FROM user_sequence WHERE lastUsed > :since ORDER BY lastUsed DESC")
    suspend fun getRecentlyUsed(since: Long): List<UserSequence>
    
    @Query("SELECT * FROM user_sequence WHERE usageCount >= :minCount ORDER BY usageCount DESC")
    suspend fun getByMinUsageCount(minCount: Int): List<UserSequence>
    
    @Query("SELECT * FROM user_sequence ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getMostUsed(limit: Int): List<UserSequence>
    
    @Query("SELECT * FROM user_sequence WHERE estimatedDurationMs <= :maxDurationMs ORDER BY name ASC")
    suspend fun getByMaxDuration(maxDurationMs: Long): List<UserSequence>
    
    @Query("SELECT * FROM user_sequence WHERE createdDate BETWEEN :startDate AND :endDate ORDER BY createdDate DESC")
    suspend fun getByCreatedDateRange(startDate: Long, endDate: Long): List<UserSequence>
    
    @Query("UPDATE user_sequence SET usageCount = usageCount + 1, lastUsed = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: Long)
    
    @Query("UPDATE user_sequence SET triggerPhrase = :triggerPhrase WHERE id = :id")
    suspend fun updateTriggerPhrase(id: Long, triggerPhrase: String)
    
    @Query("UPDATE user_sequence SET estimatedDurationMs = :durationMs WHERE id = :id")
    suspend fun updateEstimatedDuration(id: Long, durationMs: Long)
}