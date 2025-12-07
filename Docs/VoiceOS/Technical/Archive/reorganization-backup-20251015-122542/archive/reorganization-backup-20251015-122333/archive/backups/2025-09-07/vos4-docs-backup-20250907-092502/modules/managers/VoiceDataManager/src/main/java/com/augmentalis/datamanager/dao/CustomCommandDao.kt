// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.CustomCommand

@Dao
interface CustomCommandDao : BaseDao<CustomCommand> {
    
    @Query("SELECT * FROM custom_command ORDER BY name ASC")
    suspend fun getAll(): List<CustomCommand>
    
    @Query("SELECT * FROM custom_command WHERE id = :id")
    suspend fun getById(id: Long): CustomCommand?
    
    @Query("DELETE FROM custom_command WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM custom_command")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM custom_command")
    suspend fun count(): Long
    
    @Query("SELECT * FROM custom_command WHERE name = :name")
    suspend fun getByName(name: String): CustomCommand?
    
    @Query("SELECT * FROM custom_command WHERE language = :language ORDER BY name ASC")
    suspend fun getByLanguage(language: String): List<CustomCommand>
    
    @Query("SELECT * FROM custom_command WHERE isActive = :isActive ORDER BY name ASC")
    suspend fun getByActiveStatus(isActive: Boolean): List<CustomCommand>
    
    @Query("SELECT * FROM custom_command WHERE name = :name AND language = :language")
    suspend fun getByNameAndLanguage(name: String, language: String): CustomCommand?
    
    @Query("SELECT * FROM custom_command WHERE language = :language AND isActive = :isActive ORDER BY name ASC")
    suspend fun getByLanguageAndActiveStatus(language: String, isActive: Boolean): List<CustomCommand>
    
    @Query("SELECT * FROM custom_command WHERE isActive = 1 ORDER BY usageCount DESC LIMIT :limit")
    suspend fun getMostUsed(limit: Int): List<CustomCommand>
    
    @Query("SELECT * FROM custom_command WHERE lastUsed > :since ORDER BY lastUsed DESC")
    suspend fun getRecentlyUsed(since: Long): List<CustomCommand>
    
    @Query("UPDATE custom_command SET usageCount = usageCount + 1, lastUsed = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: Long)
    
    @Query("UPDATE custom_command SET isActive = :isActive WHERE id = :id")
    suspend fun setActiveStatus(id: Long, isActive: Boolean)
}