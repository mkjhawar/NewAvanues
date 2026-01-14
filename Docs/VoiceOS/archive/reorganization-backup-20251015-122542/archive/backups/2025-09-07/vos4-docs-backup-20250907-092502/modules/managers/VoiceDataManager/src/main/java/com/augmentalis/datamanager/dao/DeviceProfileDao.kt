// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.DeviceProfile

@Dao
interface DeviceProfileDao : BaseDao<DeviceProfile> {
    
    @Query("SELECT * FROM device_profile ORDER BY deviceType ASC")
    suspend fun getAll(): List<DeviceProfile>
    
    @Query("SELECT * FROM device_profile WHERE id = :id")
    suspend fun getById(id: Long): DeviceProfile?
    
    @Query("DELETE FROM device_profile WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM device_profile")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM device_profile")
    suspend fun count(): Long
    
    @Query("SELECT * FROM device_profile WHERE deviceType = :deviceType")
    suspend fun getByDeviceType(deviceType: String): List<DeviceProfile>
    
    @Query("SELECT * FROM device_profile WHERE isActive = :isActive")
    suspend fun getByActiveStatus(isActive: Boolean): List<DeviceProfile>
    
    @Query("SELECT * FROM device_profile WHERE deviceType = :deviceType AND isActive = :isActive")
    suspend fun getByDeviceTypeAndActiveStatus(deviceType: String, isActive: Boolean): List<DeviceProfile>
    
    @Query("SELECT * FROM device_profile WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveDevice(): DeviceProfile?
    
    @Query("SELECT * FROM device_profile WHERE deviceModel = :deviceModel")
    suspend fun getByDeviceModel(deviceModel: String): List<DeviceProfile>
    
    @Query("SELECT * FROM device_profile ORDER BY lastConnected DESC LIMIT :limit")
    suspend fun getRecentlyConnected(limit: Int): List<DeviceProfile>
    
    @Query("UPDATE device_profile SET isActive = 0")
    suspend fun deactivateAllDevices()
    
    @Query("UPDATE device_profile SET isActive = 1 WHERE id = :id")
    suspend fun setAsActiveDevice(id: Long)
    
    @Query("UPDATE device_profile SET lastConnected = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Long, timestamp: Long)
}