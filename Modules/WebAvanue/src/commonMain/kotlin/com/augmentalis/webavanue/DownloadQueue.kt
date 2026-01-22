package com.augmentalis.webavanue

import com.augmentalis.webavanue.DownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * DownloadProgress - Platform-agnostic download progress information
 *
 * Contains progress details that can be used across all platforms.
 */
data class DownloadProgress(
    val downloadId: String,
    val bytesDownloaded: Long,
    val bytesTotal: Long,
    val status: DownloadStatus,
    val errorMessage: String? = null,
    val localPath: String? = null
) {
    /**
     * Progress as a percentage (0.0 to 1.0)
     */
    val progress: Float
        get() = if (bytesTotal > 0) bytesDownloaded.toFloat() / bytesTotal else 0f

    /**
     * Progress as a percentage (0 to 100)
     */
    val progressPercent: Int
        get() = (progress * 100).toInt()

    /**
     * Whether the download is complete
     */
    val isComplete: Boolean
        get() = status == DownloadStatus.COMPLETED

    /**
     * Whether the download failed
     */
    val isFailed: Boolean
        get() = status == DownloadStatus.FAILED

    /**
     * Whether the download is active
     */
    val isActive: Boolean
        get() = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PENDING
}

/**
 * DownloadRequest - Platform-agnostic download request information
 *
 * Used to initiate a download on any platform.
 */
data class DownloadRequest(
    val downloadId: String,
    val url: String,
    val filename: String,
    val mimeType: String? = null,
    val userAgent: String? = null,
    val cookies: String? = null,
    val expectedSize: Long = -1,
    val sourcePageUrl: String? = null,
    val sourcePageTitle: String? = null,
    val customPath: String? = null  // Optional custom download path (content:// URI from file picker)
)

/**
 * DownloadQueue - Platform-agnostic interface for download management
 *
 * Provides a common contract for download operations that can be implemented
 * differently on each platform (Android DownloadManager, iOS URLSession, etc.).
 */
interface DownloadQueue {

    /**
     * Enqueue a download request
     *
     * @param request Download request details
     * @return Download ID if queued successfully, null on failure
     */
    suspend fun enqueue(request: DownloadRequest): String?

    /**
     * Cancel a download
     *
     * @param downloadId Download ID to cancel
     * @return true if cancelled successfully
     */
    suspend fun cancel(downloadId: String): Boolean

    /**
     * Pause a download (if supported)
     *
     * @param downloadId Download ID to pause
     * @return true if paused successfully
     */
    suspend fun pause(downloadId: String): Boolean

    /**
     * Resume a paused download (if supported)
     *
     * @param downloadId Download ID to resume
     * @return true if resumed successfully
     */
    suspend fun resume(downloadId: String): Boolean

    /**
     * Get current progress for a download
     *
     * @param downloadId Download ID to query
     * @return DownloadProgress or null if not found
     */
    suspend fun getProgress(downloadId: String): DownloadProgress?

    /**
     * Observe progress updates for a download
     *
     * @param downloadId Download ID to observe
     * @return Flow of DownloadProgress updates
     */
    fun observeProgress(downloadId: String): Flow<DownloadProgress>

    /**
     * Observe all active downloads
     *
     * @return Flow of all active download progress
     */
    fun observeAllActive(): Flow<List<DownloadProgress>>

    /**
     * Get the local file path for a completed download
     *
     * @param downloadId Download ID
     * @return File path or null if not completed
     */
    suspend fun getDownloadedFilePath(downloadId: String): String?

    /**
     * Delete a downloaded file
     *
     * @param downloadId Download ID
     * @return true if deleted successfully
     */
    suspend fun deleteFile(downloadId: String): Boolean
}

/**
 * Filename utilities for generating safe download filenames
 */
object FilenameUtils {

    /**
     * Generate a safe filename from URL and content-disposition
     *
     * @param url Download URL
     * @param contentDisposition Content-Disposition header value
     * @param mimeType MIME type of the file
     * @return Safe filename
     */
    fun guessFilename(
        url: String,
        contentDisposition: String?,
        mimeType: String?
    ): String {
        // Try to extract from content-disposition first
        val dispositionFilename = contentDisposition?.let { parseContentDisposition(it) }
        if (!dispositionFilename.isNullOrBlank()) {
            return sanitizeFilename(dispositionFilename)
        }

        // Extract from URL path
        val urlFilename = extractFilenameFromUrl(url)
        if (!urlFilename.isNullOrBlank()) {
            // Add extension if missing
            val extension = guessExtension(mimeType)
            return if (!urlFilename.contains(".") && extension != null) {
                sanitizeFilename("$urlFilename.$extension")
            } else {
                sanitizeFilename(urlFilename)
            }
        }

        // Fallback to generic name
        val extension = guessExtension(mimeType) ?: "bin"
        return "download_${currentTimeMillis()}.$extension"
    }

    /**
     * Parse filename from Content-Disposition header
     */
    private fun parseContentDisposition(contentDisposition: String): String? {
        // Try to find filename* (RFC 5987)
        val filenameStar = Regex("filename\\*=(?:UTF-8''|utf-8'')([^;\\s]+)")
            .find(contentDisposition)?.groupValues?.get(1)
        if (!filenameStar.isNullOrBlank()) {
            return decodeUrlEncoded(filenameStar)
        }

        // Try to find filename="..."
        val filenameQuoted = Regex("filename=\"([^\"]+)\"")
            .find(contentDisposition)?.groupValues?.get(1)
        if (!filenameQuoted.isNullOrBlank()) {
            return filenameQuoted
        }

        // Try to find filename=...
        val filenameUnquoted = Regex("filename=([^;\\s]+)")
            .find(contentDisposition)?.groupValues?.get(1)
        if (!filenameUnquoted.isNullOrBlank()) {
            return filenameUnquoted
        }

        return null
    }

    /**
     * Extract filename from URL path
     */
    private fun extractFilenameFromUrl(url: String): String? {
        return try {
            val path = url.substringBefore("?").substringBefore("#")
            val lastSegment = path.substringAfterLast("/")
            if (lastSegment.isNotBlank() && lastSegment != url) {
                decodeUrlEncoded(lastSegment)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sanitize filename to remove unsafe characters
     */
    private fun sanitizeFilename(filename: String): String {
        return filename
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")  // Remove unsafe chars
            .replace("..", "_")                        // Remove path traversal
            .replace(Regex("\\s+"), "_")              // Replace whitespace
            .trim()
            .take(255)                                 // Limit length
    }

    /**
     * Decode URL-encoded string
     */
    private fun decodeUrlEncoded(encoded: String): String {
        return try {
            // Simple URL decoding
            encoded.replace("+", " ")
                .replace(Regex("%([0-9A-Fa-f]{2})")) { match ->
                    val hex = match.groupValues[1]
                    hex.toInt(16).toChar().toString()
                }
        } catch (e: Exception) {
            encoded
        }
    }

    /**
     * Guess file extension from MIME type
     */
    private fun guessExtension(mimeType: String?): String? {
        if (mimeType.isNullOrBlank()) return null

        return when (mimeType.lowercase()) {
            // Text
            "text/plain" -> "txt"
            "text/html" -> "html"
            "text/css" -> "css"
            "text/javascript", "application/javascript" -> "js"
            "text/xml", "application/xml" -> "xml"
            "application/json" -> "json"

            // Images
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "image/svg+xml" -> "svg"
            "image/bmp" -> "bmp"
            "image/ico", "image/x-icon" -> "ico"

            // Documents
            "application/pdf" -> "pdf"
            "application/msword" -> "doc"
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
            "application/vnd.ms-excel" -> "xls"
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
            "application/vnd.ms-powerpoint" -> "ppt"
            "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx"

            // Archives
            "application/zip" -> "zip"
            "application/x-rar-compressed", "application/vnd.rar" -> "rar"
            "application/x-7z-compressed" -> "7z"
            "application/gzip" -> "gz"
            "application/x-tar" -> "tar"

            // Audio
            "audio/mpeg" -> "mp3"
            "audio/wav" -> "wav"
            "audio/ogg" -> "ogg"
            "audio/aac" -> "aac"
            "audio/flac" -> "flac"

            // Video
            "video/mp4" -> "mp4"
            "video/webm" -> "webm"
            "video/x-msvideo" -> "avi"
            "video/quicktime" -> "mov"
            "video/x-matroska" -> "mkv"

            // Binary
            "application/octet-stream" -> "bin"

            else -> mimeType.substringAfter("/").substringBefore(";").take(10)
        }
    }
}

/**
 * Get current system time in milliseconds (platform-specific)
 */
internal expect fun currentTimeMillis(): Long
