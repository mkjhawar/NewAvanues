// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.data

import com.augmentalis.datamanager.core.DatabaseManager
import com.augmentalis.datamanager.entities.LanguageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LanguageModelRepo {
    
    private val dao get() = DatabaseManager.database.languageModelDao()
    
    // Direct CRUD implementation - no inheritance
    suspend fun insert(entity: LanguageModel): Long = withContext(Dispatchers.IO) {
        dao.insert(entity)
    }

    suspend fun insertAll(entities: List<LanguageModel>): List<Long> = withContext(Dispatchers.IO) {
        dao.insertAll(entities)
    }

    suspend fun update(entity: LanguageModel) = withContext(Dispatchers.IO) {
        dao.update(entity)
    }

    suspend fun delete(entity: LanguageModel) = withContext(Dispatchers.IO) {
        dao.delete(entity)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    suspend fun getById(id: Long): LanguageModel? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun getAll(): List<LanguageModel> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun count(): Long = withContext(Dispatchers.IO) {
        dao.count()
    }

    suspend fun query(queryBuilder: () -> List<LanguageModel>): List<LanguageModel> = withContext(Dispatchers.IO) {
        queryBuilder()
    }
    
    suspend fun getModelByLanguage(languageCode: String, engine: String): LanguageModel? = withContext(Dispatchers.IO) {
        dao.getByLanguageCodeAndEngine(languageCode, engine)
    }
    
    suspend fun getDownloadedModels(): List<LanguageModel> = withContext(Dispatchers.IO) {
        dao.getReadyModels()
    }
    
    suspend fun getModelsByEngine(engine: String): List<LanguageModel> = withContext(Dispatchers.IO) {
        dao.getByEngine(engine)
    }
    
    suspend fun updateDownloadStatus(modelId: Long, status: String) = withContext(Dispatchers.IO) {
        dao.getById(modelId)?.let { model ->
            val updated = model.copy(
                downloadStatus = status,
                downloadDate = if (status == "ready") System.currentTimeMillis() else model.downloadDate
            )
            dao.update(updated)
        }
    }
    
    suspend fun getTotalDownloadedSize(): Long = withContext(Dispatchers.IO) {
        dao.getTotalDownloadedSize() ?: 0L
    }
    
    suspend fun getAvailableLanguages(engine: String): List<String> = withContext(Dispatchers.IO) {
        dao.getByEngine(engine).map { it.languageCode }.distinct()
    }
}