package com.augmentalis.webavanue

/**
 * Sealed hierarchy of tab-specific errors with user-friendly messages
 * and recovery mechanisms.
 *
 * Each error type includes:
 * - userMessage: Human-readable error message for display in UI
 * - technicalDetails: Technical information for logging/debugging
 * - isRecoverable: Whether the error can be recovered from user action
 */
sealed class TabError : Exception() {
    abstract val userMessage: String
    abstract val technicalDetails: String
    abstract val isRecoverable: Boolean

    /**
     * Database capacity exceeded - cannot create more tabs
     *
     * Recovery: User can close existing tabs to free up capacity
     */
    data class DatabaseFull(
        val maxTabs: Int,
        val currentTabs: Int
    ) : TabError() {
        override val userMessage = "Cannot open more tabs. Maximum $maxTabs tabs reached."
        override val technicalDetails = "Database capacity: $currentTabs/$maxTabs tabs"
        override val isRecoverable = true // User can close tabs
    }

    /**
     * Invalid URL provided - cannot navigate or create tab
     *
     * Recovery: User can fix the URL and try again
     */
    data class InvalidUrl(
        val url: String,
        val reason: String
    ) : TabError() {
        override val userMessage = "Invalid web address: $reason"
        override val technicalDetails = "URL validation failed for: $url"
        override val isRecoverable = true // User can fix URL
    }

    /**
     * Network connectivity failure when loading tab
     *
     * Recovery: User can retry when connection is restored
     */
    data class NetworkError(
        val originalUrl: String,
        override val cause: Throwable?
    ) : TabError() {
        override val userMessage = "Cannot connect to $originalUrl. Check your internet connection."
        override val technicalDetails = "Network failure: ${cause?.message ?: "Unknown"}"
        override val isRecoverable = true // Retry possible
    }

    /**
     * WebView initialization failure - critical system error
     *
     * Recovery: Requires app restart
     */
    data class WebViewCreationFailed(
        override val cause: Throwable?
    ) : TabError() {
        override val userMessage = "Cannot create browser tab. Please restart the app."
        override val technicalDetails = "WebView initialization failed: ${cause?.message}"
        override val isRecoverable = false // Requires app restart
    }

    /**
     * Missing required permission for tab operation
     *
     * Recovery: User can grant permission in system settings
     */
    data class PermissionDenied(
        val permission: String
    ) : TabError() {
        override val userMessage = "Permission required: $permission"
        override val technicalDetails = "Missing permission: $permission"
        override val isRecoverable = true // User can grant permission
    }

    /**
     * Database operation failed (save/update)
     *
     * Recovery: User can retry the operation
     */
    data class DatabaseOperationFailed(
        val operation: String,
        override val cause: Throwable?
    ) : TabError() {
        override val userMessage = "Failed to save tab data. Please try again."
        override val technicalDetails = "Database $operation failed: ${cause?.message}"
        override val isRecoverable = true // Retry possible
    }

    /**
     * Tab not found in database
     *
     * Recovery: Non-recoverable - tab was likely already closed
     */
    data class TabNotFound(
        val tabId: String
    ) : TabError() {
        override val userMessage = "Tab no longer exists. It may have been closed."
        override val technicalDetails = "Tab ID not found: $tabId"
        override val isRecoverable = false // Tab is gone
    }
}
