// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.TouchGesture

@Dao
interface TouchGestureDao : BaseDao<TouchGesture> {
    
    @Query("SELECT * FROM touch_gesture ORDER BY name ASC")
    suspend fun getAll(): List<TouchGesture>
    
    @Query("SELECT * FROM touch_gesture WHERE id = :id")
    suspend fun getById(id: Long): TouchGesture?
    
    @Query("DELETE FROM touch_gesture")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM touch_gesture WHERE name = :name")
    suspend fun getByName(name: String): TouchGesture?
    
    @Query("SELECT * FROM touch_gesture WHERE isSystemGesture = :isSystemGesture ORDER BY name ASC")
    suspend fun getBySystemGestureStatus(isSystemGesture: Boolean): List<TouchGesture>
    
    @Query("SELECT * FROM touch_gesture WHERE name = :name AND isSystemGesture = :isSystemGesture")
    suspend fun getByNameAndSystemStatus(name: String, isSystemGesture: Boolean): TouchGesture?
    
    @Query("SELECT * FROM touch_gesture WHERE isSystemGesture = 0 ORDER BY usageCount DESC")
    suspend fun getUserGesturesByUsage(): List<TouchGesture>
    
    @Query("SELECT * FROM touch_gesture WHERE isSystemGesture = 1 ORDER BY name ASC")
    suspend fun getSystemGestures(): List<TouchGesture>
    
    @Query("SELECT * FROM touch_gesture WHERE associatedCommand IS NOT NULL ORDER BY name ASC")
    suspend fun getGesturesWithCommands(): List<TouchGesture>
    
    @Query("SELECT * FROM touch_gesture WHERE associatedCommand = :command")
    suspend fun getByAssociatedCommand(command: String): List<TouchGesture>
    
    @Query("SELECT * FROM touch_gesture WHERE lastUsed > :since ORDER BY lastUsed DESC")
    suspend fun getRecentlyUsed(since: Long): List<TouchGesture>
    
    @Query("SELECT * FROM touch_gesture WHERE usageCount >= :minCount ORDER BY usageCount DESC")
    suspend fun getByMinUsageCount(minCount: Int): List<TouchGesture>
    
    @Query("UPDATE touch_gesture SET usageCount = usageCount + 1, lastUsed = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: Long)
    
    @Query("UPDATE touch_gesture SET associatedCommand = :command WHERE id = :id")
    suspend fun updateAssociatedCommand(id: Long, command: String?)
}