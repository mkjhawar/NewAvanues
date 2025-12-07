// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.data

import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.DeviceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceProfileRepo {
    
    private val dao get() = DatabaseManager.database.deviceProfileDao()
    
    // Direct CRUD implementation - no inheritance
    suspend fun insert(entity: DeviceProfile): Long = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }

    suspend fun insertAll(entities: List<DeviceProfile>): List<Long> = withContext(Dispatchers.IO) {
        dao.insertAll(entities)
    }

    suspend fun update(entity: DeviceProfile) = withContext(Dispatchers.IO) {
        dao.update(entity)
    }

    suspend fun delete(entity: DeviceProfile) = withContext(Dispatchers.IO) {
        dao.delete(entity)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    suspend fun getById(id: Long): DeviceProfile? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun getAll(): List<DeviceProfile> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun count(): Long = withContext(Dispatchers.IO) {
        dao.count()
    }

    suspend fun query(queryBuilder: () -> List<DeviceProfile>): List<DeviceProfile> = withContext(Dispatchers.IO) {
        queryBuilder()
    }
    
    suspend fun getActiveProfile(): DeviceProfile? = withContext(Dispatchers.IO) {
        dao.getActiveDevice()
    }
    
    suspend fun getProfilesByType(deviceType: String): List<DeviceProfile> = withContext(Dispatchers.IO) {
        dao.getByDeviceType(deviceType)
    }
    
    suspend fun setActiveProfile(profileId: Long) = withContext(Dispatchers.IO) {
        // Deactivate all profiles
        dao.getAll().forEach { profile ->
            if (profile.isActive) {
                dao.update(profile.copy(isActive = false))
            }
        }
        
        // Activate selected profile
        dao.getById(profileId)?.let { profile ->
            dao.update(profile.copy(
                isActive = true,
                lastConnected = System.currentTimeMillis()
            ))
        }
    }
    
    suspend fun updateLastConnected(profileId: Long) = withContext(Dispatchers.IO) {
        dao.getById(profileId)?.let { profile ->
            dao.update(profile.copy(lastConnected = System.currentTimeMillis()))
        }
    }
    
    suspend fun getRecentlyConnectedProfiles(limit: Int = 5): List<DeviceProfile> = withContext(Dispatchers.IO) {
        dao.getRecentlyConnected(limit)
    }
}