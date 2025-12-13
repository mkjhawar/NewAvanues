package com.augmentalis.Avanues.web.app.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.documentfile.provider.DocumentFile

/**
 * DownloadHelper - Android-specific helper for managing file downloads
 *
 * Uses Android's DownloadManager for:
 * - Background download execution
 * - Automatic retry on network failure
 * - System notification for progress
 * - Proper file storage handling
 */
object DownloadHelper {

    /**
     * Start a download using Android DownloadManager
     *
     * @param context Android context
     * @param url URL to download
     * @param userAgent User agent string from WebView
     * @param contentDisposition Content-Disposition header (for filename)
     * @param mimeType MIME type of the file
     * @param contentLength File size in bytes (-1 if unknown)
     * @param customPath Optional custom download path (content:// URI from SAF)
     * @return DownloadManager ID for tracking, or -1 if failed
     */
    fun startDownload(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long,
        customPath: String? = null
    ): Long {
        try {
            // Check storage permission if required
            val permissionManager = com.augmentalis.webavanue.platform.DownloadPermissionManager(context)
            if (permissionManager.isPermissionRequired() && !permissionManager.isPermissionGranted()) {
                // Permission required but not granted
                println("DownloadHelper: Storage permission required but not granted. Using default Downloads folder.")
                // Fall through to use default Downloads folder
            }
            // Generate filename from URL or content-disposition
            val filename = guessFileName(url, contentDisposition, mimeType)

            // Create download request
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                // Set notification visibility
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                // Set title and description for notification
                setTitle(filename)
                setDescription("Downloading file...")

                // Set destination based on custom path or default
                if (customPath != null && customPath.isNotBlank() && customPath.startsWith("content://")) {
                    try {
                        // Parse custom URI from file picker (SAF)
                        val customUri = Uri.parse(customPath)

                        // Validate the URI using DocumentFile
                        val documentFile = DocumentFile.fromTreeUri(context, customUri)
                        if (documentFile != null && documentFile.exists() && documentFile.canWrite()) {
                            // Create a file in the selected directory with the download filename
                            val targetFile = documentFile.createFile(
                                mimeType ?: "application/octet-stream",
                                filename
                            )

                            if (targetFile != null && targetFile.uri != null) {
                                // Use the created file URI as destination
                                setDestinationUri(targetFile.uri)
                                Log.i("DownloadHelper", "Using custom download path: ${targetFile.uri}")
                            } else {
                                // Failed to create file - use default
                                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                                Log.w("DownloadHelper", "Failed to create file in custom path. Using default Downloads folder.")
                            }
                        } else {
                            // Invalid or inaccessible custom path - use default
                            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                            Log.w("DownloadHelper", "Custom path is not accessible. Using default Downloads folder.")
                        }
                    } catch (e: Exception) {
                        // Invalid custom path - use default and log error
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                        Log.e("DownloadHelper", "Error using custom path: ${e.message}. Falling back to default Downloads folder.", e)
                    }
                } else {
                    // Use default Downloads folder (no custom path or not a content:// URI)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                    if (customPath != null && customPath.isNotBlank()) {
                        Log.w("DownloadHelper", "Custom path provided but not a content:// URI. Using default Downloads folder.")
                    }
                }

                // Set MIME type if available
                mimeType?.let { setMimeType(it) }

                // Allow downloads over both WiFi and mobile data
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)

                // Add cookies from WebView
                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrBlank()) {
                    addRequestHeader("Cookie", cookies)
                }

                // Add user agent
                userAgent?.let { addRequestHeader("User-Agent", it) }
            }

            // Enqueue the download
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            return downloadManager.enqueue(request)

        } catch (e: Exception) {
            println("DownloadHelper: Failed to start download: ${e.message}")
            return -1
        }
    }

    /**
     * Query download progress
     *
     * @param context Android context
     * @param downloadId DownloadManager ID
     * @return DownloadProgress or null if not found
     */
    fun queryProgress(context: Context, downloadId: Long): DownloadProgress? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
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
                    bytesDownloaded = bytesDownloaded,
                    bytesTotal = bytesTotal,
                    status = status,
                    reason = reason,
                    localUri = localUri
                )
            } else null
        }
    }

    /**
     * Cancel a download
     *
     * @param context Android context
     * @param downloadId DownloadManager ID
     * @return Number of downloads removed (0 or 1)
     */
    fun cancelDownload(context: Context, downloadId: Long): Int {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.remove(downloadId)
    }

    /**
     * Get the file path for a completed download
     *
     * @param context Android context
     * @param downloadId DownloadManager ID
     * @return File URI or null
     */
    fun getDownloadedFileUri(context: Context, downloadId: Long): Uri? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.getUriForDownloadedFile(downloadId)
    }

    /**
     * Get MIME type for a completed download
     *
     * @param context Android context
     * @param downloadId DownloadManager ID
     * @return MIME type or null
     */
    fun getMimeType(context: Context, downloadId: Long): String? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.getMimeTypeForDownloadedFile(downloadId)
    }

    /**
     * Guess filename from URL, content-disposition, and MIME type
     */
    private fun guessFileName(
        url: String,
        contentDisposition: String?,
        mimeType: String?
    ): String {
        // Use Android's URLUtil to guess filename
        var filename = URLUtil.guessFileName(url, contentDisposition, mimeType)

        // Ensure we have an extension
        if (!filename.contains(".")) {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (!extension.isNullOrBlank()) {
                filename = "$filename.$extension"
            }
        }

        return filename
    }

    /**
     * Convert DownloadManager status to human-readable string
     */
    fun statusToString(status: Int): String {
        return when (status) {
            DownloadManager.STATUS_PENDING -> "Pending"
            DownloadManager.STATUS_RUNNING -> "Downloading"
            DownloadManager.STATUS_PAUSED -> "Paused"
            DownloadManager.STATUS_SUCCESSFUL -> "Completed"
            DownloadManager.STATUS_FAILED -> "Failed"
            else -> "Unknown"
        }
    }

    /**
     * Convert DownloadManager failure reason to human-readable string
     */
    fun reasonToString(reason: Int): String {
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
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Waiting for WiFi"
            DownloadManager.PAUSED_UNKNOWN -> "Paused"
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network"
            DownloadManager.PAUSED_WAITING_TO_RETRY -> "Waiting to retry"
            else -> "Unknown reason: $reason"
        }
    }
}

/**
 * Data class for download progress information
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val bytesTotal: Long,
    val status: Int,
    val reason: Int,
    val localUri: String?
) {
    val progress: Float
        get() = if (bytesTotal > 0) bytesDownloaded.toFloat() / bytesTotal else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val isComplete: Boolean
        get() = status == DownloadManager.STATUS_SUCCESSFUL

    val isFailed: Boolean
        get() = status == DownloadManager.STATUS_FAILED

    val isRunning: Boolean
        get() = status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING
}
