package com.augmentalis.webavanue.repository

import com.augmentalis.webavanue.Download
import com.augmentalis.webavanue.DownloadStatus
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.toDbModel
import com.augmentalis.webavanue.toDomainModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Repository interface for download operations.
 *
 * Handles all download-related data persistence including:
 * - Download CRUD operations
 * - Progress tracking
 * - Status management
 * - Reactive observation via Flow
 */
interface DownloadRepository {
    /** Observes all downloads with real-time updates */
    fun observeDownloads(): Flow<List<Download>>

    /** Adds a new download */
    suspend fun addDownload(download: Download): Result<Download>

    /** Gets a download by ID */
    suspend fun getDownload(downloadId: String): Result<Download?>

    /** Gets a download by Android DownloadManager ID */
    suspend fun getDownloadByManagerId(managerId: Long): Download?

    /** Gets all downloads with pagination */
    suspend fun getAllDownloads(limit: Int = 100, offset: Int = 0): Result<List<Download>>

    /** Gets downloads by status */
    suspend fun getDownloadsByStatus(status: DownloadStatus): Result<List<Download>>

    /** Updates a download */
    suspend fun updateDownload(download: Download): Result<Unit>

    /** Updates download progress */
    suspend fun updateDownloadProgress(downloadId: String, downloadedSize: Long, status: DownloadStatus): Result<Unit>

    /** Sets the Android DownloadManager ID for a download */
    suspend fun setDownloadManagerId(downloadId: String, managerId: Long): Result<Unit>

    /** Marks a download as complete */
    suspend fun completeDownload(downloadId: String, filepath: String, downloadedSize: Long): Result<Unit>

    /** Marks a download as failed */
    suspend fun failDownload(downloadId: String, errorMessage: String): Result<Unit>

    /** Deletes a download */
    suspend fun deleteDownload(downloadId: String): Result<Unit>

    /** Clears all downloads */
    suspend fun clearAllDownloads(): Result<Unit>

    /** Searches downloads by filename */
    suspend fun searchDownloads(query: String): Result<List<Download>>

    /** Refreshes in-memory state from database */
    suspend fun refresh()

    /** Updates in-memory state directly (for fast startup) */
    suspend fun updateState(downloads: List<Download>)
}

/**
 * SQLDelight implementation of DownloadRepository.
 *
 * @param database SQLDelight database instance
 */
class DownloadRepositoryImpl(
    private val database: BrowserDatabase
) : DownloadRepository {

    private val queries = database.browserDatabaseQueries
    private val _downloads = MutableStateFlow<List<Download>>(emptyList())

    override fun observeDownloads(): Flow<List<Download>> = _downloads.asStateFlow()

    override suspend fun addDownload(download: Download): Result<Download> = withContext(Dispatchers.IO) {
        try {
            queries.insertDownload(download.toDbModel())
            refresh()
            Result.success(download)
        } catch (e: Exception) {
            Napier.e("Error adding download: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getDownload(downloadId: String): Result<Download?> = withContext(Dispatchers.IO) {
        try {
            val dbDownload = queries.selectDownloadById(downloadId).executeAsOneOrNull()
            Result.success(dbDownload?.toDomainModel())
        } catch (e: Exception) {
            Napier.e("Error getting download: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getDownloadByManagerId(managerId: Long): Download? = withContext(Dispatchers.IO) {
        try {
            queries.selectDownloadByManagerId(managerId).executeAsOneOrNull()?.toDomainModel()
        } catch (e: Exception) {
            Napier.e("Error getting download by manager ID: ${e.message}", e, tag = TAG)
            null
        }
    }

    override suspend fun getAllDownloads(limit: Int, offset: Int): Result<List<Download>> = withContext(Dispatchers.IO) {
        try {
            val downloads = queries.selectAllDownloads(limit.toLong(), offset.toLong())
                .executeAsList()
                .map { it.toDomainModel() }
            Result.success(downloads)
        } catch (e: Exception) {
            Napier.e("Error getting all downloads: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getDownloadsByStatus(status: DownloadStatus): Result<List<Download>> = withContext(Dispatchers.IO) {
        try {
            val downloads = queries.selectDownloadsByStatus(status.name)
                .executeAsList()
                .map { it.toDomainModel() }
            Result.success(downloads)
        } catch (e: Exception) {
            Napier.e("Error getting downloads by status: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun updateDownload(download: Download): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.insertDownload(download.toDbModel())
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error updating download: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun updateDownloadProgress(
        downloadId: String,
        downloadedSize: Long,
        status: DownloadStatus
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.updateDownloadProgress(downloadedSize, status.name, downloadId)
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error updating download progress: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun setDownloadManagerId(downloadId: String, managerId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.updateDownloadManagerId(managerId, downloadId)
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error setting download manager ID: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun completeDownload(
        downloadId: String,
        filepath: String,
        downloadedSize: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.updateDownloadComplete(
                filepath,
                downloadedSize,
                Clock.System.now().toEpochMilliseconds(),
                downloadId
            )
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error completing download: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun failDownload(downloadId: String, errorMessage: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.updateDownloadFailed(errorMessage, downloadId)
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error failing download: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun deleteDownload(downloadId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteDownload(downloadId)
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error deleting download: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun clearAllDownloads(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteAllDownloads()
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error clearing all downloads: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun searchDownloads(query: String): Result<List<Download>> = withContext(Dispatchers.IO) {
        try {
            val downloads = queries.searchDownloads(query)
                .executeAsList()
                .map { it.toDomainModel() }
            Result.success(downloads)
        } catch (e: Exception) {
            Napier.e("Error searching downloads: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun refresh() {
        try {
            _downloads.value = queries.selectAllDownloads(100, 0).executeAsList().map { it.toDomainModel() }
        } catch (e: Exception) {
            Napier.e("Error refreshing downloads: ${e.message}", e, tag = TAG)
        }
    }

    override suspend fun updateState(downloads: List<Download>) {
        _downloads.value = downloads
    }

    companion object {
        private const val TAG = "DownloadRepository"
    }
}
