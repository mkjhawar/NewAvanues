package com.augmentalis.webavanue

/**
 * Sealed hierarchy of download-specific errors with user-friendly messages
 * and recovery mechanisms.
 *
 * Each error type includes:
 * - userMessage: Human-readable error message for display in UI
 * - technicalDetails: Technical information for logging/debugging
 * - isRecoverable: Whether the error can be recovered from user action
 */
sealed class DownloadError : Exception() {
    abstract val userMessage: String
    abstract val technicalDetails: String
    abstract val isRecoverable: Boolean

    /**
     * Insufficient storage space for download
     *
     * Recovery: User can free up storage space or choose different location
     */
    data class InsufficientStorage(
        val requiredBytes: Long,
        val availableBytes: Long
    ) : DownloadError() {
        override val userMessage: String
            get() {
                val requiredMB = requiredBytes / 1_000_000
                val availableMB = availableBytes / 1_000_000
                return "Not enough storage space. Need ${requiredMB}MB, but only ${availableMB}MB available."
            }
        override val technicalDetails = "Required: ${requiredBytes / 1_000_000}MB, Available: ${availableBytes / 1_000_000}MB"
        override val isRecoverable = true // User can free up space
    }

    /**
     * Network failure during download
     *
     * Recovery: User can retry when connection is restored
     */
    data class NetworkFailed(
        val url: String,
        val bytesDownloaded: Long = 0,
        val totalBytes: Long = 0,
        override val cause: Throwable? = null
    ) : DownloadError() {
        override val userMessage: String
            get() {
                return if (totalBytes > 0) {
                    val progress = (bytesDownloaded * 100 / totalBytes).toInt()
                    "Download failed at $progress%. Check your connection and try again."
                } else {
                    "Download failed. Check your connection and try again."
                }
            }
        override val technicalDetails = "Network error for: $url (${bytesDownloaded}/${totalBytes} bytes)"
        override val isRecoverable = true // Retry possible
    }

    /**
     * File system access denied - cannot write to destination
     *
     * Recovery: User can choose different location or grant permissions
     */
    data class FileAccessDenied(
        val path: String,
        override val cause: Throwable? = null
    ) : DownloadError() {
        override val userMessage = "Cannot save file to selected location. Choose a different folder or check permissions."
        override val technicalDetails = "Access denied: $path - ${cause?.message ?: "Unknown reason"}"
        override val isRecoverable = true // User can choose different path
    }

    /**
     * Server returned error response
     *
     * Recovery: May be retryable depending on status code
     */
    data class ServerError(
        val statusCode: Int,
        val url: String,
        override val cause: Throwable? = null
    ) : DownloadError() {
        override val userMessage: String
            get() = when (statusCode) {
                404 -> "File not found on server."
                403 -> "Access to file is forbidden."
                401 -> "Authentication required to download this file."
                500, 502, 503 -> "Server error. Please try again later."
                else -> "Download failed with error code $statusCode."
            }
        override val technicalDetails = "HTTP $statusCode for: $url"
        override val isRecoverable = statusCode in listOf(408, 429, 500, 502, 503, 504) // Retry on temporary errors
    }

    /**
     * File already exists at destination
     *
     * Recovery: User can overwrite, rename, or choose different location
     */
    data class FileAlreadyExists(
        val path: String,
        val fileName: String
    ) : DownloadError() {
        override val userMessage = "File '$fileName' already exists. Choose a different name or location."
        override val technicalDetails = "File exists: $path"
        override val isRecoverable = true // User can rename or overwrite
    }

    /**
     * Download cancelled by user
     *
     * Recovery: User can restart download
     */
    data class Cancelled(
        val url: String,
        val bytesDownloaded: Long = 0
    ) : DownloadError() {
        override val userMessage = "Download cancelled."
        override val technicalDetails = "User cancelled download from: $url (${bytesDownloaded} bytes downloaded)"
        override val isRecoverable = true // User can restart
    }

    /**
     * Invalid or malformed download URL
     *
     * Recovery: Cannot recover - invalid source
     */
    data class InvalidDownloadUrl(
        val url: String,
        val reason: String
    ) : DownloadError() {
        override val userMessage = "Cannot download from this address: $reason"
        override val technicalDetails = "Invalid download URL: $url"
        override val isRecoverable = false // Invalid URL cannot be fixed
    }
}
