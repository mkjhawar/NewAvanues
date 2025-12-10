// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.UserPreference

@Dao
interface UserPreferenceDao : BaseDao<UserPreference> {
    
    @Query("SELECT * FROM user_preference ORDER BY module ASC, key ASC")
    suspend fun getAll(): List<UserPreference>
    
    @Query("SELECT * FROM user_preference WHERE id = :id")
    suspend fun getById(id: Long): UserPreference?
    
    @Query("DELETE FROM user_preference WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM user_preference")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM user_preference")
    suspend fun count(): Long
    
    @Query("SELECT * FROM user_preference WHERE key = :key")
    suspend fun getByKey(key: String): List<UserPreference>
    
    @Query("SELECT * FROM user_preference WHERE module = :module ORDER BY key ASC")
    suspend fun getByModule(module: String): List<UserPreference>
    
    @Query("SELECT * FROM user_preference WHERE key = :key AND module = :module")
    suspend fun getByKeyAndModule(key: String, module: String): UserPreference?
    
    @Query("SELECT * FROM user_preference WHERE type = :type ORDER BY module ASC, key ASC")
    suspend fun getByType(type: String): List<UserPreference>
    
    @Query("SELECT value FROM user_preference WHERE key = :key AND module = :module")
    suspend fun getValue(key: String, module: String): String?
    
    @Query("UPDATE user_preference SET value = :value WHERE key = :key AND module = :module")
    suspend fun updateValue(key: String, module: String, value: String)
    
    @Query("DELETE FROM user_preference WHERE key = :key AND module = :module")
    suspend fun deleteByKeyAndModule(key: String, module: String)
    
    @Query("DELETE FROM user_preference WHERE module = :module")
    suspend fun deleteByModule(module: String)
    
    @Query("SELECT COUNT(*) FROM user_preference WHERE module = :module")
    suspend fun getCountByModule(module: String): Int
}