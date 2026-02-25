package com.augmentalis.avanueui.ipc

/**
 * Unified IPC Error Types
 *
 * Consolidated error hierarchy for all IPC operations across the Avanues platform.
 * Errors are categorized by their nature and recoverability.
 *
 * Error Code Format: ERR:{category}{number}
 * - ERR:T001-T099 = Transient errors (retry possible)
 * - ERR:P001-P099 = Permanent errors (no retry)
 * - ERR:S001-S099 = Security errors
 * - ERR:R001-R099 = Resource errors
 *
 * Categories:
 * - **Transient**: Temporary failures that may succeed on retry
 * - **Permanent**: Failures that require user intervention or configuration changes
 * - **Security**: Authentication and authorization failures
 * - **Resource**: System resource limitations
 *
 * Sources:
 * - IPCConnector: ServiceUnavailable, Timeout, NetworkFailure, PermissionDenied,
 *                 ServiceNotFound, InvalidResponse, AuthenticationFailed,
 *                 SignatureVerificationFailed, ResourceExhausted, RateLimitExceeded
 * - UniversalIPC: TargetNotFound (merged with ServiceNotFound), SendFailed,
 *                 ParseError, NotRegistered
 *
 * Usage:
 * ```kotlin
 * when (val error = result.exceptionOrNull() as? IPCError) {
 *     is IPCError.ServiceUnavailable -> scheduleRetry(error.reason)
 *     is IPCError.PermissionDenied -> requestPermission(error.permission)
 *     is IPCError.Timeout -> handleTimeout(error.durationMs)
 *     else -> logUnexpectedError(error)
 * }
 *
 * // AVU serialization
 * val avuLine = error.toAvuLine()  // "ERR:T001|service_unavailable|Service starting"
 * val parsed = IPCError.fromAvuLine(avuLine)
 * ```
 *
 * @since 1.0.0
 * @author Avanues Platform Team
 */
sealed class IPCError : Exception() {

    /**
     * Error code for AVU serialization (ERR:{category}{number})
     */
    abstract val code: String

    /**
     * Serialize to AVU line format
     *
     * Format: ERR:{code}|{type}|{details}
     */
    abstract fun toAvuLine(): String

    // ============================================================
    // TRANSIENT ERRORS (ERR:T001-T099) - Retry may succeed
    // ============================================================

    /**
     * Target service is temporarily unavailable.
     *
     * Occurs when:
     * - Service is starting up or restarting
     * - Service is temporarily overloaded
     * - Network connectivity issues
     *
     * Recovery: Retry with exponential backoff
     *
     * @param reason Human-readable description of why service is unavailable
     */
    data class ServiceUnavailable(val reason: String) : IPCError() {
        override val code: String = "ERR:T001"
        override val message: String get() = "Service unavailable: $reason"
        override fun toAvuLine(): String = "$code|service_unavailable|$reason"
    }

    /**
     * Operation timed out waiting for response.
     *
     * Occurs when:
     * - Remote service takes too long to respond
     * - Network latency exceeds threshold
     * - Service is processing a long-running operation
     *
     * Recovery: Retry with longer timeout or check service health
     *
     * @param durationMs How long the operation waited before timing out (milliseconds)
     */
    data class Timeout(val durationMs: Long) : IPCError() {
        override val code: String = "ERR:T002"
        override val message: String get() = "Operation timed out after ${durationMs}ms"
        override fun toAvuLine(): String = "$code|timeout|$durationMs"
    }

    /**
     * Network-level communication failure.
     *
     * Occurs when:
     * - Network interface is down
     * - DNS resolution fails
     * - Connection is interrupted mid-transfer
     * - Socket errors occur
     *
     * Recovery: Check network connectivity, retry
     *
     * @param cause Underlying network exception (optional)
     */
    data class NetworkFailure(override val cause: Throwable? = null) : IPCError() {
        override val code: String = "ERR:T003"
        override val message: String get() = "Network failure: ${cause?.message ?: "Unknown network error"}"
        override fun toAvuLine(): String = "$code|network_failure|${cause?.message ?: "unknown"}"
    }

    /**
     * Failed to send message to target.
     *
     * Occurs when:
     * - Message serialization fails
     * - Transport layer rejects message
     * - Connection dropped during send
     * - Buffer overflow
     *
     * Recovery: Verify message format, check connection state, retry
     *
     * @param reason Human-readable description of send failure
     * @param cause Underlying exception (optional)
     */
    data class SendFailed(val reason: String, override val cause: Throwable? = null) : IPCError() {
        override val code: String = "ERR:T004"
        override val message: String get() = "Send failed: $reason"
        override fun toAvuLine(): String = "$code|send_failed|$reason"
    }

    // ============================================================
    // PERMANENT ERRORS (ERR:P001-P099) - Require configuration or user action
    // ============================================================

    /**
     * Required permission not granted.
     *
     * Occurs when:
     * - Android runtime permission not granted
     * - iOS entitlement missing
     * - System-level access denied
     *
     * Recovery: Request permission from user, update manifest/entitlements
     *
     * @param permission The permission that was denied (e.g., "android.permission.BIND_SERVICE")
     */
    data class PermissionDenied(val permission: String) : IPCError() {
        override val code: String = "ERR:P001"
        override val message: String get() = "Permission denied: $permission"
        override fun toAvuLine(): String = "$code|permission_denied|$permission"
    }

    /**
     * Target service or app not found.
     *
     * Occurs when:
     * - Target app is not installed
     * - Service component doesn't exist
     * - Package name is incorrect
     * - App ID is invalid
     *
     * Recovery: Verify target is installed, check package/app ID spelling
     *
     * Note: Merges IPCConnector's ServiceNotFound and UniversalIPC's TargetNotFound
     *
     * @param target The service package name or app ID that wasn't found
     */
    data class ServiceNotFound(val target: String) : IPCError() {
        override val code: String = "ERR:P002"
        override val message: String get() = "Service not found: $target"
        override fun toAvuLine(): String = "$code|service_not_found|$target"
    }

    /**
     * Response from service is invalid or malformed.
     *
     * Occurs when:
     * - Response doesn't match expected schema
     * - Required fields are missing
     * - Data types are incorrect
     * - Response is truncated
     *
     * Recovery: Check service version compatibility, validate response format
     *
     * @param details Description of what was invalid about the response
     */
    data class InvalidResponse(val details: String) : IPCError() {
        override val code: String = "ERR:P003"
        override val message: String get() = "Invalid response: $details"
        override fun toAvuLine(): String = "$code|invalid_response|$details"
    }

    /**
     * Failed to parse message content.
     *
     * Occurs when:
     * - JSON/XML parsing fails
     * - Serialization format mismatch
     * - Encoding issues (UTF-8, etc.)
     * - Protocol buffer decode failure
     *
     * Recovery: Verify message encoding, check serializer configuration
     *
     * @param parseMessage Description of the parse error
     * @param cause Underlying parsing exception (optional)
     */
    data class ParseError(
        val parseMessage: String,
        override val cause: Throwable? = null
    ) : IPCError() {
        override val code: String = "ERR:P004"
        override val message: String get() = "Parse error: $parseMessage"
        override fun toAvuLine(): String = "$code|parse_error|$parseMessage"
    }

    /**
     * App or service not registered with IPC system.
     *
     * Occurs when:
     * - Attempting to send before calling register()
     * - Registration was revoked or expired
     * - Service binding not established
     *
     * Recovery: Call register() before sending messages
     *
     * @param appId The app ID that is not registered
     */
    data class NotRegistered(val appId: String) : IPCError() {
        override val code: String = "ERR:P005"
        override val message: String get() = "App not registered: $appId"
        override fun toAvuLine(): String = "$code|not_registered|$appId"
    }

    // ============================================================
    // SECURITY ERRORS (ERR:S001-S099) - Authentication/authorization failures
    // ============================================================

    /**
     * Authentication with service failed.
     *
     * Occurs when:
     * - Invalid credentials provided
     * - Token expired or revoked
     * - Authentication handshake failed
     * - OAuth flow incomplete
     *
     * Recovery: Re-authenticate, refresh tokens, check credentials
     *
     * @param reason Description of why authentication failed
     */
    data class AuthenticationFailed(val reason: String) : IPCError() {
        override val code: String = "ERR:S001"
        override val message: String get() = "Authentication failed: $reason"
        override fun toAvuLine(): String = "$code|auth_failed|$reason"
    }

    /**
     * Package signature verification failed.
     *
     * Occurs when:
     * - App signature doesn't match expected certificate
     * - Package was tampered with
     * - Debug vs release signature mismatch
     * - Certificate chain validation fails
     *
     * Recovery: Verify app is from trusted source, check signing configuration
     *
     * @param packageName The package whose signature failed verification
     */
    data class SignatureVerificationFailed(val packageName: String) : IPCError() {
        override val code: String = "ERR:S002"
        override val message: String get() = "Signature verification failed for: $packageName"
        override fun toAvuLine(): String = "$code|signature_failed|$packageName"
    }

    // ============================================================
    // RESOURCE ERRORS (ERR:R001-R099) - System resource limitations
    // ============================================================

    /**
     * System resource has been exhausted.
     *
     * Occurs when:
     * - Too many open connections
     * - Memory limits exceeded
     * - File descriptor limits reached
     * - Thread pool exhausted
     *
     * Recovery: Close unused resources, increase limits, implement pooling
     *
     * @param resource Description of the exhausted resource
     */
    data class ResourceExhausted(val resource: String) : IPCError() {
        override val code: String = "ERR:R001"
        override val message: String get() = "Resource exhausted: $resource"
        override fun toAvuLine(): String = "$code|resource_exhausted|$resource"
    }

    /**
     * Request rate limit exceeded.
     *
     * Occurs when:
     * - Too many requests in time window
     * - API quota exceeded
     * - Throttling activated by service
     *
     * Recovery: Wait for retry period, implement request batching
     *
     * @param retryAfterMs Time to wait before retrying (milliseconds)
     */
    data class RateLimitExceeded(val retryAfterMs: Long) : IPCError() {
        override val code: String = "ERR:R002"
        override val message: String get() = "Rate limit exceeded, retry after ${retryAfterMs}ms"
        override fun toAvuLine(): String = "$code|rate_limit|$retryAfterMs"
    }

    // ============================================================
    // UTILITY METHODS
    // ============================================================

    /**
     * Check if this error is transient and may succeed on retry.
     */
    val isTransient: Boolean
        get() = code.startsWith("ERR:T")

    /**
     * Check if this error is permanent (no retry).
     */
    val isPermanent: Boolean
        get() = code.startsWith("ERR:P")

    /**
     * Check if this error is security-related.
     */
    val isSecurityError: Boolean
        get() = code.startsWith("ERR:S")

    /**
     * Check if this error is resource-related.
     */
    val isResourceError: Boolean
        get() = code.startsWith("ERR:R")

    /**
     * Suggested retry delay for transient errors.
     * Returns null for non-transient errors.
     */
    val suggestedRetryDelayMs: Long?
        get() = when (this) {
            is RateLimitExceeded -> retryAfterMs
            is Timeout -> durationMs * 2  // Double the timeout
            is ServiceUnavailable -> 1000L  // 1 second base delay
            is NetworkFailure -> 2000L  // 2 seconds for network issues
            is SendFailed -> 500L  // Quick retry for send failures
            else -> null
        }

    companion object {
        /**
         * Parse IPCError from AVU line format
         *
         * @param line AVU line in format: ERR:{code}|{type}|{details}
         * @return Parsed IPCError or null if invalid format
         */
        fun fromAvuLine(line: String): IPCError? {
            if (!line.startsWith("ERR:")) return null

            val parts = line.split("|")
            if (parts.size < 3) return null

            val code = parts[0]
            val details = parts.drop(2).joinToString("|") // Rejoin in case details had |

            return when (code) {
                // Transient errors
                "ERR:T001" -> ServiceUnavailable(details)
                "ERR:T002" -> Timeout(details.toLongOrNull() ?: 0L)
                "ERR:T003" -> NetworkFailure(Exception(details))
                "ERR:T004" -> SendFailed(details)

                // Permanent errors
                "ERR:P001" -> PermissionDenied(details)
                "ERR:P002" -> ServiceNotFound(details)
                "ERR:P003" -> InvalidResponse(details)
                "ERR:P004" -> ParseError(details)
                "ERR:P005" -> NotRegistered(details)

                // Security errors
                "ERR:S001" -> AuthenticationFailed(details)
                "ERR:S002" -> SignatureVerificationFailed(details)

                // Resource errors
                "ERR:R001" -> ResourceExhausted(details)
                "ERR:R002" -> RateLimitExceeded(details.toLongOrNull() ?: 0L)

                else -> null
            }
        }

        /**
         * Check if error code is transient (retry possible)
         */
        fun isTransientCode(code: String): Boolean = code.startsWith("ERR:T")

        /**
         * Check if error code is permanent (no retry)
         */
        fun isPermanentCode(code: String): Boolean = code.startsWith("ERR:P")

        /**
         * Check if error code is security-related
         */
        fun isSecurityCode(code: String): Boolean = code.startsWith("ERR:S")

        /**
         * Check if error code is resource-related
         */
        fun isResourceCode(code: String): Boolean = code.startsWith("ERR:R")
    }
}

/**
 * Extension to convert Result failure to IPCError
 */
fun <T> Result<T>.toIPCError(): IPCError? {
    return exceptionOrNull()?.let { throwable ->
        when (throwable) {
            is IPCError -> throwable
            else -> IPCError.SendFailed(throwable.message ?: "Unknown error", throwable)
        }
    }
}

/**
 * Type alias for backward compatibility with UniversalIPC's TargetNotFound
 */
@Deprecated(
    message = "Use IPCError.ServiceNotFound instead",
    replaceWith = ReplaceWith("IPCError.ServiceNotFound")
)
typealias TargetNotFound = IPCError.ServiceNotFound
