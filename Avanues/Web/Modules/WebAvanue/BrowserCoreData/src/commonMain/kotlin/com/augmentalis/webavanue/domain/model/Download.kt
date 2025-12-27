package com.augmentalis.webavanue.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Download status enum for tracking download progress
 */
enum class DownloadStatus {
    PENDING,      // Queued, not started
    DOWNLOADING,  // In progress
    PAUSED,       // User paused
    COMPLETED,    // Successfully finished
    FAILED,       // Error occurred
    CANCELLED;    // User cancelled

    companion object {
        fun fromString(value: String): DownloadStatus {
            return entries.find { it.name == value } ?: PENDING
        }
    }
}

/**
 * Download model representing a file download.
 * Tracks download progress, status, and file information.
 *
 * Matches database schema in BrowserDatabase.sq
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
    val sourcePageTitle: String? = null
) {
    /**
     * Progress as a percentage (0.0 to 1.0)
     */
    val progress: Float
        get() = if (fileSize > 0) downloadedSize.toFloat() / fileSize else 0f

    /**
     * Progress as integer percentage (0-100)
     */
    val progressPercent: Int
        get() = (progress * 100).toInt()

    /**
     * Whether the download is complete
     */
    val isComplete: Boolean
        get() = status == DownloadStatus.COMPLETED

    /**
     * Whether the download is currently active
     */
    val isInProgress: Boolean
        get() = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PENDING

    /**
     * Whether the download can be retried
     */
    val canRetry: Boolean
        get() = status == DownloadStatus.FAILED || status == DownloadStatus.CANCELLED

    /**
     * Whether the download can be cancelled
     */
    val canCancel: Boolean
        get() = status == DownloadStatus.PENDING || status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED

    /**
     * Whether the download can be paused
     */
    val canPause: Boolean
        get() = status == DownloadStatus.DOWNLOADING

    /**
     * Whether the download can be resumed
     */
    val canResume: Boolean
        get() = status == DownloadStatus.PAUSED

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
                sourcePageUrl = sourcePageUrl,
                sourcePageTitle = sourcePageTitle,
                createdAt = Clock.System.now()
            )
        }

        private fun generateId(): String {
            return "download_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"
        }
    }
}
