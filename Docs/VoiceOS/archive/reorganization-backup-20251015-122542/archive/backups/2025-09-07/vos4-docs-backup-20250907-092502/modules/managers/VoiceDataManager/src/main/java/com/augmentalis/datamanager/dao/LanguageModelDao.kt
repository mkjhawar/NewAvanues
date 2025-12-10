// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

package com.augmentalis.datamanager.dao

import androidx.room.Dao
import androidx.room.Query
import com.augmentalis.datamanager.entities.LanguageModel

@Dao
interface LanguageModelDao : BaseDao<LanguageModel> {
    
    @Query("SELECT * FROM language_model ORDER BY languageCode ASC")
    suspend fun getAll(): List<LanguageModel>
    
    @Query("SELECT * FROM language_model WHERE id = :id")
    suspend fun getById(id: Long): LanguageModel?
    
    @Query("DELETE FROM language_model WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM language_model")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM language_model")
    suspend fun count(): Long
    
    @Query("SELECT * FROM language_model WHERE languageCode = :languageCode")
    suspend fun getByLanguageCode(languageCode: String): List<LanguageModel>
    
    @Query("SELECT * FROM language_model WHERE engine = :engine")
    suspend fun getByEngine(engine: String): List<LanguageModel>
    
    @Query("SELECT * FROM language_model WHERE languageCode = :languageCode AND engine = :engine")
    suspend fun getByLanguageCodeAndEngine(languageCode: String, engine: String): LanguageModel?
    
    @Query("SELECT * FROM language_model WHERE downloadStatus = :status")
    suspend fun getByDownloadStatus(status: String): List<LanguageModel>
    
    @Query("SELECT * FROM language_model WHERE downloadStatus = 'ready' ORDER BY languageCode ASC")
    suspend fun getReadyModels(): List<LanguageModel>
    
    @Query("SELECT * FROM language_model WHERE downloadStatus = 'downloading'")
    suspend fun getDownloadingModels(): List<LanguageModel>
    
    @Query("SELECT * FROM language_model WHERE downloadStatus = 'not_downloaded' ORDER BY languageCode ASC")
    suspend fun getAvailableForDownload(): List<LanguageModel>
    
    @Query("UPDATE language_model SET downloadStatus = :status WHERE id = :id")
    suspend fun updateDownloadStatus(id: Long, status: String)
    
    @Query("UPDATE language_model SET downloadStatus = :status, downloadDate = :downloadDate WHERE id = :id")
    suspend fun updateDownloadStatusWithDate(id: Long, status: String, downloadDate: Long)
    
    @Query("SELECT SUM(fileSize) FROM language_model WHERE downloadStatus = 'ready'")
    suspend fun getTotalDownloadedSize(): Long?
}