// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.data

import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.UserPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserPreferenceRepo {
    
    private val dao get() = DatabaseManager.database.userPreferenceDao()
    
    // Direct CRUD implementation - no inheritance
    suspend fun insert(entity: UserPreference): Long = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }
    
    suspend fun insertAll(entities: List<UserPreference>): List<Long> = withContext(Dispatchers.IO) {
        dao.insertAll(entities)
    }
    
    suspend fun update(entity: UserPreference) = withContext(Dispatchers.IO) {
        dao.update(entity)
    }
    
    suspend fun delete(entity: UserPreference) = withContext(Dispatchers.IO) {
        dao.delete(entity)
    }
    
    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }
    
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }
    
    suspend fun getById(id: Long): UserPreference? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }
    
    suspend fun getAll(): List<UserPreference> = withContext(Dispatchers.IO) {
        dao.getAll()
    }
    
    suspend fun count(): Long = withContext(Dispatchers.IO) {
        dao.count()
    }
    
    suspend fun query(queryBuilder: () -> List<UserPreference>): List<UserPreference> = withContext(Dispatchers.IO) {
        queryBuilder()
    }
    
    suspend fun getPreference(key: String, module: String? = null): UserPreference? = withContext(Dispatchers.IO) {
        val moduleToUse = module ?: "core"
        dao.getByKeyAndModule(key, moduleToUse)
    }
    
    suspend fun setPreference(key: String, value: String, type: String = "string", module: String = "core") = withContext(Dispatchers.IO) {
        val existing = getPreference(key, module)
        val preference = if (existing != null) {
            existing.copy(value = value, type = type)
        } else {
            UserPreference(key = key, value = value, type = type, module = module)
        }
        if (existing != null) {
            dao.update(preference)
        } else {
            dao.insert(preference)
        }
    }
    
    suspend fun getModulePreferences(module: String): List<UserPreference> = withContext(Dispatchers.IO) {
        dao.getByModule(module)
    }
    
    suspend fun deleteModulePreferences(module: String) = withContext(Dispatchers.IO) {
        dao.deleteByModule(module)
    }
    
    suspend fun getString(key: String, defaultValue: String = ""): String = withContext(Dispatchers.IO) {
        dao.getValue(key, "core") ?: defaultValue
    }
    
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        dao.getValue(key, "core")?.toBooleanStrictOrNull() ?: defaultValue
    }
    
    suspend fun getInt(key: String, defaultValue: Int = 0): Int = withContext(Dispatchers.IO) {
        dao.getValue(key, "core")?.toIntOrNull() ?: defaultValue
    }
    
    suspend fun getFloat(key: String, defaultValue: Float = 0f): Float = withContext(Dispatchers.IO) {
        dao.getValue(key, "core")?.toFloatOrNull() ?: defaultValue
    }
}