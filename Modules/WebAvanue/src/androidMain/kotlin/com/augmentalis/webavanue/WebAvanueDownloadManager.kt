package com.augmentalis.webavanue

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.webkit.URLUtil
import androidx.core.content.ContextCompat
import com.augmentalis.webavanue.BrowserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * WebAvanue Download Manager
 *
 * Manages file downloads with comprehensive error handling and state tracking.
 *
 * PHASE 1 FIX:
 * - Adds detailed logging to trace download states
 * - Checks permissions before download
 * - Monitors download progress
 * - Handles errors with specific error messages
 * - Fixes downloads stuck in "pending" status
 *
 * @param context Android context
 * @param settingsFlow StateFlow of browser settings
 */
class WebAvanueDownloadManager(
    private val context: Context,
    private val settingsFlow: StateFlow<BrowserSettings>
) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val _downloadStates = MutableStateFlow<Map<Long, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<Long, DownloadState>> = _downloadStates.asStateFlow()

    companion object {
        private const val TAG = "WebAvanueDownloadManager"
    }

    /**
     * Initiates a download with proper error handling
     * CRITICAL FIX: Adds comprehensive logging and permission checks
     */
    suspend fun startDownload(
        url: String,
        filename: String,
        userAgent: String,
        mimeType: String?
    ): Result<Long> {
        Log.d(TAG, "Starting download: url=$url, filename=$filename, mimeType=$mimeType")

        // Check permissions first
        if (!hasStoragePermission()) {
            Log.e(TAG, "Storage permission not granted")
            return Result.failure(SecurityException("Storage permission required"))
        }

        // Validate URL
        if (!URLUtil.isValidUrl(url)) {
            Log.e(TAG, "Invalid URL: $url")
            return Result.failure(IllegalArgumentException("Invalid download URL"))
        }

        val settings = settingsFlow.value

        // Check WiFi restriction
        if (settings.downloadOverWiFiOnly && !isWiFiConnected()) {
            Log.w(TAG, "WiFi-only download blocked: not on WiFi")
            return Result.failure(IOException("WiFi required for downloads"))
        }

        return try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(filename)
                setDescription("Downloading $filename")
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )

                // Add headers
                addRequestHeader("User-Agent", userAgent)

                // Set MIME type
                mimeType?.let { setMimeType(it) }

                // CRITICAL FIX: Properly configure destination
                val downloadPath = settings.downloadPath ?: Environment.DIRECTORY_DOWNLOADS
                setDestinationInExternalPublicDir(downloadPath, filename)

                // Allow in metered networks if WiFi-only is disabled
                setAllowedNetworkTypes(
                    if (settings.downloadOverWiFiOnly) {
                        DownloadManager.Request.NETWORK_WIFI
                    } else {
                        DownloadManager.Request.NETWORK_WIFI or
                        DownloadManager.Request.NETWORK_MOBILE
                    }
                )

                // Enable roaming
                setAllowedOverRoaming(!settings.downloadOverWiFiOnly)
            }

            val downloadId = downloadManager.enqueue(request)
            Log.d(TAG, "Download enqueued: downloadId=$downloadId")

            // Start monitoring download progress
            monitorDownload(downloadId)

            Result.success(downloadId)

        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Monitors download progress and updates state
     * CRITICAL FIX: Adds detailed state logging
     */
    private fun monitorDownload(downloadId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val query = DownloadManager.Query().setFilterById(downloadId)

            while (true) {
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    )
                    val reason = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                    )
                    val bytesDownloaded = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    )
                    val bytesTotal = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    )

                    val progress = if (bytesTotal > 0) {
                        (bytesDownloaded * 100 / bytesTotal).toInt()
                    } else 0

                    Log.d(TAG, "Download $downloadId: status=$status, reason=$reason, " +
                            "progress=$progress%, bytes=$bytesDownloaded/$bytesTotal")

                    val newState = when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            Log.d(TAG, "Download completed: $downloadId")
                            DownloadState.Completed(downloadId, bytesTotal)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val errorMsg = getErrorMessage(reason)
                            Log.e(TAG, "Download failed: $downloadId, reason=$reason ($errorMsg)")
                            DownloadState.Failed(downloadId, errorMsg)
                        }
                        DownloadManager.STATUS_PAUSED -> {
                            val pauseReason = getPauseReason(reason)
                            Log.w(TAG, "Download paused: $downloadId, reason=$reason ($pauseReason)")
                            DownloadState.Paused(downloadId, progress, pauseReason)
                        }
                        DownloadManager.STATUS_PENDING -> {
                            Log.d(TAG, "Download pending: $downloadId")
                            DownloadState.Pending(downloadId)
                        }
                        DownloadManager.STATUS_RUNNING -> {
                            DownloadState.Running(downloadId, progress, bytesDownloaded, bytesTotal)
                        }
                        else -> {
                            Log.w(TAG, "Unknown download status: $status")
                            DownloadState.Unknown(downloadId, status)
                        }
                    }

                    updateState(downloadId, newState)

                    // Exit loop if download is complete or failed
                    if (status == DownloadManager.STATUS_SUCCESSFUL ||
                        status == DownloadManager.STATUS_FAILED) {
                        cursor.close()
                        break
                    }
                }
                cursor.close()

                // Check every 500ms
                delay(500)
            }
        }
    }

    private fun getErrorMessage(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "External storage not found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
            DownloadManager.ERROR_FILE_ERROR -> "File error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient storage space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code"
            DownloadManager.ERROR_UNKNOWN -> "Unknown error"
            else -> "Error code: $reason"
        }
    }

    private fun getPauseReason(reason: Int): String {
        return when (reason) {
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Waiting for WiFi"
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network"
            DownloadManager.PAUSED_WAITING_TO_RETRY -> "Waiting to retry"
            DownloadManager.PAUSED_UNKNOWN -> "Unknown reason"
            else -> "Pause code: $reason"
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isWiFiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun updateState(downloadId: Long, state: DownloadState) {
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            put(downloadId, state)
        }
    }
}

sealed class DownloadState {
    data class Pending(val id: Long) : DownloadState()
    data class Running(val id: Long, val progress: Int, val downloaded: Long, val total: Long) : DownloadState()
    data class Paused(val id: Long, val progress: Int, val reason: String) : DownloadState()
    data class Completed(val id: Long, val bytes: Long) : DownloadState()
    data class Failed(val id: Long, val error: String) : DownloadState()
    data class Unknown(val id: Long, val status: Int) : DownloadState()
}
