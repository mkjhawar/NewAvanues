package com.augmentalis.webavanue

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Retry policy for network operations with exponential backoff.
 *
 * Features:
 * - Configurable maximum attempts
 * - Exponential backoff with configurable factor
 * - Maximum delay cap to prevent excessive waits
 * - Attempt number provided to operation for logging
 *
 * Example usage:
 * ```kotlin
 * val policy = RetryPolicy(maxAttempts = 3, initialDelay = 1.seconds)
 * val result = policy.execute { attempt ->
 *     println("Attempt $attempt")
 *     networkOperation()
 * }
 * ```
 *
 * @param maxAttempts Maximum number of retry attempts (default: 3)
 * @param initialDelay Initial delay before first retry (default: 1 second)
 * @param maxDelay Maximum delay between retries (default: 10 seconds)
 * @param factor Exponential backoff factor (default: 2.0 for doubling)
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = 1.seconds,
    val maxDelay: Duration = 10.seconds,
    val factor: Double = 2.0
) {
    init {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
        require(initialDelay.isPositive()) { "initialDelay must be positive" }
        require(maxDelay >= initialDelay) { "maxDelay must be >= initialDelay" }
        require(factor >= 1.0) { "factor must be >= 1.0" }
    }

    /**
     * Execute an operation with retry logic.
     *
     * The operation will be attempted up to [maxAttempts] times.
     * On failure, the policy will wait with exponential backoff before retrying.
     *
     * @param operation The operation to execute, receives attempt number (1-indexed)
     * @return Result of the operation (success or final failure)
     */
    suspend fun <T> execute(
        operation: suspend (attempt: Int) -> Result<T>
    ): Result<T> {
        var currentDelay = initialDelay
        var lastError: Throwable? = null

        repeat(maxAttempts) { attemptIndex ->
            val attemptNumber = attemptIndex + 1

            operation(attemptNumber).fold(
                onSuccess = { value ->
                    return Result.success(value)
                },
                onFailure = { error ->
                    lastError = error

                    // Don't delay after last attempt
                    if (attemptNumber < maxAttempts) {
                        // Log retry attempt (in production, use proper logging)
                        println("RetryPolicy: Attempt $attemptNumber failed: ${error.message}. Retrying in ${currentDelay}...")

                        delay(currentDelay)

                        // Calculate next delay with exponential backoff
                        currentDelay = (currentDelay * factor).coerceAtMost(maxDelay)
                    }
                }
            )
        }

        // All attempts exhausted
        return Result.failure(
            lastError ?: Exception("Retry failed after $maxAttempts attempts")
        )
    }

    /**
     * Execute an operation with retry logic and custom retry predicate.
     *
     * The predicate determines whether to retry based on the error.
     * Useful for only retrying specific error types (e.g., network errors but not validation errors).
     *
     * @param shouldRetry Predicate that returns true if the error is retryable
     * @param operation The operation to execute, receives attempt number (1-indexed)
     * @return Result of the operation (success or final failure)
     */
    suspend fun <T> executeIf(
        shouldRetry: (Throwable) -> Boolean,
        operation: suspend (attempt: Int) -> Result<T>
    ): Result<T> {
        var currentDelay = initialDelay
        var lastError: Throwable? = null

        repeat(maxAttempts) { attemptIndex ->
            val attemptNumber = attemptIndex + 1

            operation(attemptNumber).fold(
                onSuccess = { value ->
                    return Result.success(value)
                },
                onFailure = { error ->
                    lastError = error

                    // Check if error is retryable
                    if (!shouldRetry(error)) {
                        // Non-retryable error - fail immediately
                        return Result.failure(error)
                    }

                    // Don't delay after last attempt
                    if (attemptNumber < maxAttempts) {
                        println("RetryPolicy: Attempt $attemptNumber failed: ${error.message}. Retrying in ${currentDelay}...")

                        delay(currentDelay)
                        currentDelay = (currentDelay * factor).coerceAtMost(maxDelay)
                    }
                }
            )
        }

        return Result.failure(
            lastError ?: Exception("Retry failed after $maxAttempts attempts")
        )
    }

    companion object {
        /**
         * Aggressive retry policy for critical operations.
         * 5 attempts with 500ms initial delay.
         */
        val AGGRESSIVE = RetryPolicy(
            maxAttempts = 5,
            initialDelay = 0.5.seconds,
            maxDelay = 5.seconds,
            factor = 1.5
        )

        /**
         * Standard retry policy for normal network operations.
         * 3 attempts with 1 second initial delay.
         */
        val STANDARD = RetryPolicy(
            maxAttempts = 3,
            initialDelay = 1.seconds,
            maxDelay = 10.seconds,
            factor = 2.0
        )

        /**
         * Conservative retry policy for expensive operations.
         * 2 attempts with 2 second initial delay.
         */
        val CONSERVATIVE = RetryPolicy(
            maxAttempts = 2,
            initialDelay = 2.seconds,
            maxDelay = 15.seconds,
            factor = 3.0
        )
    }
}
