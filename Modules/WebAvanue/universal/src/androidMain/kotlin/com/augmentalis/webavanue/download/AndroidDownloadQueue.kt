package com.augmentalis.webavanue.feature.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import com.augmentalis.webavanue.domain.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Android implementation of DownloadQueue using Android DownloadManager.
 *
 * Provides background download execution with:
 * - System notification for progress
 * - Automatic retry on network failure
 * - Proper file storage handling
 * - Respects user download path settings
 */
class AndroidDownloadQueue(
    private val context: Context,
    private val getDownloadPath: (() -> String?)? = null  // Callback to get current download path from settings
) : DownloadQueue {

    private val downloadManager: DownloadManager
        get() = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // Map internal download IDs to DownloadManager IDs
    private val downloadIdMap = mutableMapOf<String, Long>()
    private val reverseIdMap = mutableMapOf<Long, String>()

    override suspend fun enqueue(request: DownloadRequest): String? = withContext(Dispatchers.IO) {
        try {
            val dmRequest = DownloadManager.Request(Uri.parse(request.url)).apply {
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setTitle(request.filename)
                setDescription("Downloading file...")

                // Use custom download path from request if provided, otherwise check settings callback
                val customPath = request.customPath ?: getDownloadPath?.invoke()
                if (customPath != null && customPath.isNotBlank() && customPath.startsWith("content://")) {
                    try {
                        // Use SAF URI for custom path
                        val customUri = Uri.parse(customPath)
                        setDestinationUri(customUri)
                        println("AndroidDownloadQueue: Using custom path: $customPath")
                    } catch (e: Exception) {
                        // Invalid custom path - use default
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, request.filename)
                        println("AndroidDownloadQueue: Invalid custom path ($customPath), using default Downloads folder: ${e.message}")
                    }
                } else {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, request.filename)
                }

                request.mimeType?.let { setMimeType(it) }
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)

                // Add cookies from WebView
                request.cookies?.let { addRequestHeader("Cookie", it) }
                    ?: CookieManager.getInstance().getCookie(request.url)?.let {
                        addRequestHeader("Cookie", it)
                    }

                request.userAgent?.let { addRequestHeader("User-Agent", it) }
            }

            val managerId = downloadManager.enqueue(dmRequest)
            val internalId = "download_${System.currentTimeMillis()}_${(0..9999).random()}"

            downloadIdMap[internalId] = managerId
            reverseIdMap[managerId] = internalId

            internalId
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun cancel(downloadId: String): Boolean = withContext(Dispatchers.IO) {
        val managerId = downloadIdMap[downloadId] ?: return@withContext false
        val removed = downloadManager.remove(managerId)
        if (removed > 0) {
            downloadIdMap.remove(downloadId)
            reverseIdMap.remove(managerId)
        }
        removed > 0
    }

    override suspend fun pause(downloadId: String): Boolean {
        // Android DownloadManager doesn't support pause/resume natively
        return false
    }

    override suspend fun resume(downloadId: String): Boolean {
        // Android DownloadManager doesn't support pause/resume natively
        return false
    }

    override suspend fun getProgress(downloadId: String): DownloadProgress? = withContext(Dispatchers.IO) {
        val managerId = downloadIdMap[downloadId] ?: return@withContext null
        queryProgress(managerId, downloadId)
    }

    override fun observeProgress(downloadId: String): Flow<DownloadProgress> = flow {
        val managerId = downloadIdMap[downloadId] ?: return@flow

        while (true) {
            val progress = queryProgress(managerId, downloadId)
            if (progress != null) {
                emit(progress)
                if (progress.isComplete || progress.isFailed) break
            }
            delay(500)
        }
    }.flowOn(Dispatchers.IO)

    override fun observeAllActive(): Flow<List<DownloadProgress>> = flow {
        while (true) {
            val activeDownloads = downloadIdMap.mapNotNull { (internalId, managerId) ->
                queryProgress(managerId, internalId)?.takeIf { it.isActive }
            }
            emit(activeDownloads)
            if (activeDownloads.isEmpty()) break
            delay(500)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getDownloadedFilePath(downloadId: String): String? = withContext(Dispatchers.IO) {
        val managerId = downloadIdMap[downloadId] ?: return@withContext null
        downloadManager.getUriForDownloadedFile(managerId)?.path
    }

    override suspend fun deleteFile(downloadId: String): Boolean = withContext(Dispatchers.IO) {
        cancel(downloadId)
    }

    private fun queryProgress(managerId: Long, internalId: String): DownloadProgress? {
        val query = DownloadManager.Query().setFilterById(managerId)
        val cursor = downloadManager.query(query) ?: return null

        return cursor.use {
            if (it.moveToFirst()) {
                val bytesDownloaded = it.getLong(
                    it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                )
                val bytesTotal = it.getLong(
                    it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                )
                val status = it.getInt(
                    it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                )
                val reason = it.getInt(
                    it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                )
                val localUri = it.getString(
                    it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                )

                DownloadProgress(
                    downloadId = internalId,
                    bytesDownloaded = bytesDownloaded,
                    bytesTotal = bytesTotal,
                    status = mapStatus(status),
                    errorMessage = if (status == DownloadManager.STATUS_FAILED) reasonToString(reason) else null,
                    localPath = localUri
                )
            } else null
        }
    }

    private fun mapStatus(status: Int): DownloadStatus {
        return when (status) {
            DownloadManager.STATUS_PENDING -> DownloadStatus.PENDING
            DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
            DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
            DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
            DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
            else -> DownloadStatus.PENDING
        }
    }

    private fun reasonToString(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Storage not found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
            DownloadManager.ERROR_FILE_ERROR -> "File error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient storage space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code"
            DownloadManager.ERROR_UNKNOWN -> "Unknown error"
            else -> "Download failed"
        }
    }

    /**
     * Get internal ID for a DownloadManager ID (used by BroadcastReceiver)
     */
    fun getInternalId(managerId: Long): String? = reverseIdMap[managerId]

    /**
     * Get DownloadManager ID for an internal ID
     */
    fun getManagerId(internalId: String): Long? = downloadIdMap[internalId]
}
