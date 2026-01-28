@file:Suppress("UNCHECKED_CAST", "USELESS_CAST") // Required for generic type transformations

/**
 * VoiceOSResult.kt - Standardized error handling for VoiceOS (KMP Library)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-09
 * Extracted to KMP Library: 2025-11-16
 *
 * Kotlin Multiplatform compatible result type for type-safe error handling.
 * Works on Android, iOS, Desktop, Web, and Native platforms.
 *
 * ## Solution
 * Sealed class result type inspired by Rust's Result<T, E> and Kotlin's runCatching:
 * - Type-safe success/failure handling
 * - Structured error information
 * - Forces explicit error handling (no silent failures)
 * - Consistent API across all VoiceOS operations
 *
 * ## Usage
 * ```kotlin
 * // Function definition
 * suspend fun processCommand(input: String): VoiceOSResult<CommandData> {
 *     return try {
 *         val data = performOperation()
 *         VoiceOSResult.Success(data, "Command processed successfully")
 *     } catch (e: Exception) {
 *         VoiceOSResult.Failure(VoiceOSError(
 *             code = "COMMAND_PROCESSING_ERROR",
 *             message = "Failed to process command",
 *             cause = e
 *         ))
 *     }
 * }
 *
 * // Usage with when expression
 * when (val result = processCommand(input)) {
 *     is VoiceOSResult.Success -> {
 *         useData(result.data)
 *     }
 *     is VoiceOSResult.Failure -> {
 *         handleError(result.error)
 *     }
 *     is VoiceOSResult.NotFound -> {
 *         showNotFoundMessage()
 *     }
 *     is VoiceOSResult.PermissionDenied -> {
 *         requestPermission(result.permission)
 *     }
 *     is VoiceOSResult.Timeout -> {
 *         retryOperation()
 *     }
 * }
 *
 * // Extension functions for convenience
 * result.onSuccess { data -> /* use data */ }
 * result.onFailure { error -> /* handle error */ }
 * val dataOrNull = result.getOrNull()
 * val dataOrDefault = result.getOrDefault(defaultValue)
 * ```
 */
package com.augmentalis.voiceoscore

/**
 * Result type for VoiceOS operations
 *
 * Provides type-safe success/failure handling with structured error information.
 * All VoiceOS operations should return this type instead of nullable values or exceptions.
 *
 * @param T Type of successful result data
 */
sealed class VoiceOSResult<out T> {

    /**
     * Operation succeeded with data
     *
     * @property data The successful result data
     * @property message Optional human-readable success message
     */
    data class Success<T>(
        val data: T,
        val message: String? = null
    ) : VoiceOSResult<T>()

    /**
     * Operation failed with error
     *
     * @property error Structured error information
     */
    data class Failure(
        val error: VoiceOSError
    ) : VoiceOSResult<Nothing>()

    /**
     * Resource not found
     *
     * Used for database queries, element lookups, etc.
     *
     * @property identifier What was being searched for (e.g., "element hash abc123")
     * @property resourceType Type of resource (e.g., "Element", "App", "Command")
     */
    data class NotFound(
        val identifier: String,
        val resourceType: String = "Resource"
    ) : VoiceOSResult<Nothing>()

    /**
     * Permission denied
     *
     * Used for accessibility service, storage, etc.
     *
     * @property permission Permission that was denied (e.g., "WRITE_EXTERNAL_STORAGE")
     * @property reason Human-readable reason why permission is needed
     */
    data class PermissionDenied(
        val permission: String,
        val reason: String? = null
    ) : VoiceOSResult<Nothing>()

    /**
     * Operation timed out
     *
     * Used for long-running operations, network requests, etc.
     *
     * @property operation Name of operation that timed out
     * @property durationMs How long the operation ran before timeout
     * @property timeoutMs Configured timeout threshold
     */
    data class Timeout(
        val operation: String,
        val durationMs: Long,
        val timeoutMs: Long
    ) : VoiceOSResult<Nothing>()

    /**
     * Check if result is successful
     *
     * @return true if Success, false otherwise
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Check if result is failure
     *
     * @return true if any failure type, false if Success
     */
    fun isFailure(): Boolean = !isSuccess()

    /**
     * Get data if successful, null otherwise
     *
     * @return Data if Success, null for any failure type
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Get data if successful, default value otherwise
     *
     * @param defaultValue Value to return on failure
     * @return Data if Success, defaultValue for any failure type
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> defaultValue
    }

    /**
     * Get data if successful, throw exception otherwise
     *
     * @return Data if Success
     * @throws VoiceOSException if any failure type
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw VoiceOSException(
            message = error.message,
            cause = error.cause,
            errorCode = error.code
        )
        is NotFound -> throw VoiceOSException(
            message = "$resourceType not found: $identifier",
            errorCode = "NOT_FOUND"
        )
        is PermissionDenied -> throw VoiceOSException(
            message = "Permission denied: $permission",
            errorCode = "PERMISSION_DENIED"
        )
        is Timeout -> throw VoiceOSException(
            message = "$operation timed out after ${durationMs}ms (limit: ${timeoutMs}ms)",
            errorCode = "TIMEOUT"
        )
    }

    /**
     * Execute block if successful
     *
     * @param block Lambda to execute with data
     * @return this for chaining
     */
    inline fun onSuccess(block: (T) -> Unit): VoiceOSResult<T> {
        if (this is Success<T>) {
            block(data)
        }
        return this
    }

    /**
     * Execute block if failed
     *
     * @param block Lambda to execute with error
     * @return this for chaining
     */
    inline fun onFailure(block: (VoiceOSError) -> Unit): VoiceOSResult<T> {
        when (this) {
            is Failure -> block(error)
            is NotFound -> block(
                VoiceOSError(
                    code = "NOT_FOUND",
                    message = "$resourceType not found: $identifier"
                )
            )
            is PermissionDenied -> block(
                VoiceOSError(
                    code = "PERMISSION_DENIED",
                    message = "Permission denied: $permission"
                )
            )
            is Timeout -> block(
                VoiceOSError(
                    code = "TIMEOUT",
                    message = "$operation timed out after ${durationMs}ms"
                )
            )
            is Success -> { /* no-op */ }
        }
        return this
    }

    /**
     * Transform successful result
     *
     * @param transform Function to transform data
     * @return New VoiceOSResult with transformed data, or original failure
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <R> map(transform: (T) -> R): VoiceOSResult<R> = when (this) {
        is Success -> Success(transform(data), message)
        is Failure -> this as VoiceOSResult<R>
        is NotFound -> this as VoiceOSResult<R>
        is PermissionDenied -> this as VoiceOSResult<R>
        is Timeout -> this as VoiceOSResult<R>
    }

    /**
     * Transform successful result with another VoiceOSResult
     *
     * @param transform Function to transform data into new result
     * @return Transformed result, or original failure
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <R> flatMap(transform: (T) -> VoiceOSResult<R>): VoiceOSResult<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this as VoiceOSResult<R>
        is NotFound -> this as VoiceOSResult<R>
        is PermissionDenied -> this as VoiceOSResult<R>
        is Timeout -> this as VoiceOSResult<R>
    }
}

/**
 * Structured error information
 *
 * Provides consistent error reporting across VoiceOS.
 *
 * @property code Error code for programmatic handling (e.g., "DATABASE_ERROR", "NETWORK_ERROR")
 * @property message Human-readable error message
 * @property cause Original exception that caused the error (optional)
 * @property context Additional context data for debugging (optional)
 */
data class VoiceOSError(
    val code: String,
    val message: String,
    val cause: Throwable? = null,
    val context: Map<String, Any> = emptyMap()
) {
    /**
     * Get full error message with cause
     *
     * @return Error message with cause chain
     */
    fun getFullMessage(): String {
        val builder = StringBuilder(message)
        if (cause != null) {
            builder.append("\nCause: ${cause.message}")
            var current = cause.cause
            var depth = 1
            while (current != null && depth < 5) {
                builder.append("\n  → ${current.message}")
                current = current.cause
                depth++
            }
        }
        return builder.toString()
    }

    /**
     * Get error with additional context
     *
     * @param additionalContext Context to add
     * @return New VoiceOSError with merged context
     */
    fun withContext(additionalContext: Map<String, Any>): VoiceOSError {
        return copy(context = context + additionalContext)
    }
}

/**
 * Extension function to wrap exceptions as VoiceOSResult
 *
 * Usage:
 * ```kotlin
 * fun riskyOperation(): VoiceOSResult<Data> = runCatchingResult {
 *     // code that might throw
 *     performOperation()
 * }
 * ```
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> runCatchingResult(block: () -> T): VoiceOSResult<T> {
    return try {
        VoiceOSResult.Success(block(), null)
    } catch (e: Exception) {
        VoiceOSResult.Failure(
            VoiceOSError(
                code = "EXCEPTION",
                message = e.message ?: "Unknown error",
                cause = e
            )
        ) as VoiceOSResult<T>
    }
}
