/**
 * Download State Management for AVA Model Downloads
 *
 * Sealed class hierarchy representing all possible states during model download:
 * - Idle: Not downloading
 * - Downloading: In progress with progress tracking
 * - Paused: Download paused, can be resumed
 * - Completed: Successfully downloaded
 * - Error: Download failed with error details
 *
 * Used with Kotlin Flow for reactive progress updates.
 *
 * Created: 2025-11-03
 * Author: AVA AI Team
 */

package com.augmentalis.llm.download

/**
 * Sealed class representing download states
 */
sealed class DownloadState {

    /**
     * Idle state - no download in progress
     */
    data object Idle : DownloadState()

    /**
     * Downloading state with progress information
     *
     * @property modelId Unique identifier for the model being downloaded
     * @property bytesDownloaded Number of bytes downloaded so far
     * @property totalBytes Total size of the file in bytes
     * @property progress Download progress as a percentage (0.0 to 1.0)
     * @property speedBytesPerSecond Current download speed in bytes per second
     * @property estimatedTimeRemainingMs Estimated time remaining in milliseconds
     */
    data class Downloading(
        val modelId: String,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progress: Float,
        val speedBytesPerSecond: Long = 0L,
        val estimatedTimeRemainingMs: Long = 0L
    ) : DownloadState() {

        /**
         * Get human-readable progress percentage
         */
        fun getProgressPercentage(): Int = (progress * 100).toInt()

        /**
         * Get human-readable download size
         */
        fun getDownloadedSize(): String = formatBytes(bytesDownloaded)

        /**
         * Get human-readable total size
         */
        fun getTotalSize(): String = formatBytes(totalBytes)

        /**
         * Get human-readable download speed
         */
        fun getSpeed(): String = "${formatBytes(speedBytesPerSecond)}/s"

        /**
         * Get human-readable time remaining
         */
        fun getTimeRemaining(): String = formatTime(estimatedTimeRemainingMs)

        private fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }

        private fun formatTime(milliseconds: Long): String {
            val seconds = milliseconds / 1000
            return when {
                seconds < 60 -> "${seconds}s"
                seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
                else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
            }
        }
    }

    /**
     * Paused state - download can be resumed
     *
     * @property modelId Unique identifier for the model
     * @property bytesDownloaded Number of bytes downloaded before pause
     * @property totalBytes Total size of the file in bytes
     * @property progress Download progress as a percentage (0.0 to 1.0)
     * @property pauseReason Optional reason for pause
     */
    data class Paused(
        val modelId: String,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progress: Float,
        val pauseReason: String? = null
    ) : DownloadState()

    /**
     * Completed state - download finished successfully
     *
     * @property modelId Unique identifier for the model
     * @property filePath Local file path where model is stored
     * @property fileSize Size of the downloaded file in bytes
     * @property checksum Optional checksum for verification
     * @property downloadDurationMs Total download time in milliseconds
     */
    data class Completed(
        val modelId: String,
        val filePath: String,
        val fileSize: Long,
        val checksum: String? = null,
        val downloadDurationMs: Long = 0L
    ) : DownloadState()

    /**
     * Error state - download failed
     *
     * @property modelId Unique identifier for the model
     * @property error Error that caused the failure
     * @property message Human-readable error message
     * @property code Optional error code for categorization
     * @property canRetry Whether the download can be retried
     * @property bytesDownloaded Number of bytes downloaded before error (for resume)
     */
    data class Error(
        val modelId: String,
        val error: Throwable,
        val message: String,
        val code: ErrorCode = ErrorCode.UNKNOWN,
        val canRetry: Boolean = true,
        val bytesDownloaded: Long = 0L
    ) : DownloadState()
}

/**
 * Error codes for download failures
 */
enum class ErrorCode {
    /** Unknown error */
    UNKNOWN,

    /** Network connection error */
    NETWORK_ERROR,

    /** HTTP error (4xx, 5xx) */
    HTTP_ERROR,

    /** Insufficient storage space */
    INSUFFICIENT_STORAGE,

    /** File I/O error */
    IO_ERROR,

    /** Checksum verification failed */
    CHECKSUM_MISMATCH,

    /** Download was cancelled by user */
    CANCELLED,

    /** Download timeout */
    TIMEOUT,

    /** Invalid URL or configuration */
    INVALID_CONFIG
}

/**
 * Extension functions for DownloadState
 */

/**
 * Check if download is in progress
 */
fun DownloadState.isInProgress(): Boolean = this is DownloadState.Downloading

/**
 * Check if download is paused
 */
fun DownloadState.isPaused(): Boolean = this is DownloadState.Paused

/**
 * Check if download is complete
 */
fun DownloadState.isComplete(): Boolean = this is DownloadState.Completed

/**
 * Check if download has error
 */
fun DownloadState.hasError(): Boolean = this is DownloadState.Error

/**
 * Check if download is idle
 */
fun DownloadState.isIdle(): Boolean = this is DownloadState.Idle

/**
 * Check if download can be resumed
 */
fun DownloadState.canResume(): Boolean = when (this) {
    is DownloadState.Paused -> true
    is DownloadState.Error -> canRetry && bytesDownloaded > 0
    else -> false
}

/**
 * Get model ID if available
 */
fun DownloadState.getModelId(): String? = when (this) {
    is DownloadState.Downloading -> modelId
    is DownloadState.Paused -> modelId
    is DownloadState.Completed -> modelId
    is DownloadState.Error -> modelId
    else -> null
}

/**
 * Get progress if available
 */
fun DownloadState.getProgress(): Float? = when (this) {
    is DownloadState.Downloading -> progress
    is DownloadState.Paused -> progress
    is DownloadState.Completed -> 1.0f
    else -> null
}
