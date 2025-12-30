package com.augmentalis.webavanue.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Download status enum for tracking download progress
 */
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED;

    companion object {
        fun fromString(value: String): DownloadStatus {
            return entries.find { it.name == value } ?: PENDING
        }
    }
}

/**
 * Download model representing a file download.
 * Tracks download progress, status, and file information.
 */
@Serializable
data class Download(
    val id: String,
    val url: String,
    val filename: String,
    val filepath: String? = null,
    val mimeType: String? = null,
    val fileSize: Long = 0,
    val downloadedSize: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val errorMessage: String? = null,
    val downloadManagerId: Long? = null,
    val createdAt: Instant,
    val completedAt: Instant? = null,
    val sourcePageUrl: String? = null,
    val sourcePageTitle: String? = null,
    // Progress tracking fields
    val downloadSpeed: Long = 0,           // Bytes per second
    val estimatedTimeRemaining: Long = 0,  // Seconds
    val lastProgressUpdate: Long = 0       // Timestamp (ms)
) {
    /**
     * Progress as a percentage (0.0 to 1.0)
     */
    val progress: Float
        get() = if (fileSize > 0) downloadedSize.toFloat() / fileSize else 0f

    /**
     * Progress as a percentage (0 to 100)
     */
    val progressPercent: Int
        get() = (progress * 100).toInt()

    /**
     * Whether the download is currently active
     */
    val isActive: Boolean
        get() = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PENDING

    /**
     * Whether the download can be resumed
     */
    val canResume: Boolean
        get() = status == DownloadStatus.PAUSED || status == DownloadStatus.FAILED

    /**
     * Whether the download can be cancelled
     */
    val canCancel: Boolean
        get() = status == DownloadStatus.PENDING || status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED

    companion object {
        /**
         * Create a new download with generated ID
         */
        fun create(
            url: String,
            filename: String,
            mimeType: String? = null,
            fileSize: Long = 0,
            sourcePageUrl: String? = null,
            sourcePageTitle: String? = null
        ): Download {
            return Download(
                id = generateId(),
                url = url,
                filename = filename,
                mimeType = mimeType,
                fileSize = fileSize,
                createdAt = Clock.System.now(),
                sourcePageUrl = sourcePageUrl,
                sourcePageTitle = sourcePageTitle
            )
        }

        private fun generateId(): String {
            return "download_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"
        }
    }
}
