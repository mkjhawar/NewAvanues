package com.augmentalis.browseravanue.core

/**
 * Comprehensive error types for Browser operations
 *
 * Provides user-friendly messages and severity levels
 * for proper error handling and display.
 */
sealed class BrowserError {

    data class NetworkError(
        val message: String,
        val url: String,
        val statusCode: Int? = null
    ) : BrowserError()

    data class DatabaseError(
        val message: String,
        val operation: String? = null
    ) : BrowserError()

    data class InvalidUrl(
        val url: String,
        val reason: String
    ) : BrowserError()

    data class WebViewError(
        val errorCode: Int,
        val description: String,
        val failingUrl: String
    ) : BrowserError()

    data class SSLError(
        val url: String,
        val certificateError: String
    ) : BrowserError()

    object NoActiveTabs : BrowserError()

    object TabNotFound : BrowserError()

    data class FavoriteAlreadyExists(val url: String) : BrowserError()

    object FavoriteNotFound : BrowserError()

    data class PermissionDenied(val permission: String) : BrowserError()

    data class StorageError(val message: String) : BrowserError()

    data class Unknown(val message: String, val cause: Throwable? = null) : BrowserError()

    /**
     * Get user-friendly error message
     */
    fun getUserMessage(): String {
        return when (this) {
            is NetworkError -> "Network error: $message${statusCode?.let { " (HTTP $it)" } ?: ""}"
            is DatabaseError -> "Database error: $message${operation?.let { " during $it" } ?: ""}"
            is InvalidUrl -> "Invalid URL: $reason"
            is WebViewError -> "Page load failed: $description"
            is SSLError -> "Security certificate error for $url"
            NoActiveTabs -> "No active tabs available"
            TabNotFound -> "Tab not found"
            is FavoriteAlreadyExists -> "This page is already in favorites"
            FavoriteNotFound -> "Favorite not found"
            is PermissionDenied -> "Permission denied: $permission"
            is StorageError -> "Storage error: $message"
            is Unknown -> "Unknown error: $message"
        }
    }

    /**
     * Get error severity level
     */
    fun getSeverity(): ErrorSeverity {
        return when (this) {
            is NetworkError -> ErrorSeverity.WARNING
            is DatabaseError -> ErrorSeverity.ERROR
            is InvalidUrl -> ErrorSeverity.WARNING
            is WebViewError -> ErrorSeverity.ERROR
            is SSLError -> ErrorSeverity.CRITICAL
            NoActiveTabs -> ErrorSeverity.INFO
            TabNotFound -> ErrorSeverity.WARNING
            is FavoriteAlreadyExists -> ErrorSeverity.INFO
            FavoriteNotFound -> ErrorSeverity.WARNING
            is PermissionDenied -> ErrorSeverity.CRITICAL
            is StorageError -> ErrorSeverity.ERROR
            is Unknown -> ErrorSeverity.ERROR
        }
    }

    /**
     * Check if error is recoverable
     */
    fun isRecoverable(): Boolean {
        return when (this) {
            is NetworkError -> true
            is InvalidUrl -> true
            NoActiveTabs -> true
            is FavoriteAlreadyExists -> true
            is WebViewError -> errorCode != -2 // Not ERR_UNKNOWN_HOST
            else -> false
        }
    }
}

enum class ErrorSeverity {
    INFO,       // Informational, no action needed
    WARNING,    // Warning, user should be aware
    ERROR,      // Error, operation failed
    CRITICAL    // Critical, security/data risk
}
