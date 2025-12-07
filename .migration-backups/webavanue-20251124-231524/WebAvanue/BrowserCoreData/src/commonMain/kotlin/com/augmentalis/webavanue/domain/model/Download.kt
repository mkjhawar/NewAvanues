package com.augmentalis.webavanue.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Download status enum for tracking download progress
 */
enum class DownloadStatus {
    PENDING,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
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
    val mimeType: String? = null,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val createdAt: Instant,
    val completedAt: Instant? = null,
    val errorMessage: String? = null,
    val filePath: String? = null
) {
    /**
     * Progress as a percentage (0.0 to 1.0)
     */
    val progress: Float
        get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f

    /**
     * Whether the download is currently active
     */
    val isActive: Boolean
        get() = status == DownloadStatus.IN_PROGRESS || status == DownloadStatus.PENDING

    /**
     * Whether the download can be resumed
     */
    val canResume: Boolean
        get() = status == DownloadStatus.PAUSED || status == DownloadStatus.FAILED

    /**
     * Whether the download can be cancelled
     */
    val canCancel: Boolean
        get() = status == DownloadStatus.PENDING || status == DownloadStatus.IN_PROGRESS || status == DownloadStatus.PAUSED

    companion object {
        /**
         * Create a new download with generated ID
         */
        fun create(
            url: String,
            filename: String,
            mimeType: String? = null,
            totalBytes: Long = 0
        ): Download {
            return Download(
                id = generateId(),
                url = url,
                filename = filename,
                mimeType = mimeType,
                totalBytes = totalBytes,
                createdAt = Clock.System.now()
            )
        }

        private fun generateId(): String {
            return "download_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"
        }
    }
}
