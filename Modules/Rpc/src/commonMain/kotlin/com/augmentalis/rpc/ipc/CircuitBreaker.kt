package com.augmentalis.rpc.ipc

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Circuit Breaker
 *
 * Prevents cascading failures by opening circuit after repeated errors.
 * Implements the circuit breaker pattern for fault tolerance.
 *
 * ## State Machine
 * ```
 * CLOSED (normal) --> OPEN (failing) --> HALF_OPEN (testing) --> CLOSED
 *       |                                      |
 *   failures++                            success -> CLOSED
 *                                         failure -> OPEN
 * ```
 *
 * ## Usage
 * ```kotlin
 * val circuitBreaker = CircuitBreaker(CircuitBreakerConfig(
 *     failureThreshold = 5,
 *     successThreshold = 2,
 *     timeoutMs = 60000
 * ))
 *
 * val result = circuitBreaker.execute {
 *     // Your operation that might fail
 *     remoteService.call()
 * }
 *
 * when {
 *     result.isSuccess -> handleSuccess(result.getOrNull())
 *     result.isFailure -> handleFailure(result.exceptionOrNull())
 * }
 * ```
 *
 * @since 1.0.0
 * @author Avanues Platform Team
 */
class CircuitBreaker(
    private val config: CircuitBreakerConfig = CircuitBreakerConfig()
) {
    private val mutex = Mutex()
    private var state: CircuitState = CircuitState.CLOSED
    private var failureCount: Int = 0
    private var successCount: Int = 0
    private var lastFailureTime: Long = 0

    /**
     * Execute operation with circuit breaker protection
     *
     * @param operation Suspending operation to execute
     * @return Result of operation or circuit open error
     */
    suspend fun <T> execute(operation: suspend () -> T): Result<T> {
        mutex.withLock {
            when (state) {
                CircuitState.OPEN -> {
                    // Check if enough time has passed to try again
                    if (currentTimeMillis() - lastFailureTime >= config.timeoutMs) {
                        state = CircuitState.HALF_OPEN
                        successCount = 0
                    } else {
                        return Result.failure(Exception("Circuit breaker is OPEN"))
                    }
                }
                CircuitState.HALF_OPEN -> {
                    // Allow limited requests to test if service recovered
                }
                CircuitState.CLOSED -> {
                    // Normal operation
                }
            }
        }

        return try {
            val result = operation()
            onSuccess()
            Result.success(result)
        } catch (e: Exception) {
            onFailure()
            Result.failure(e)
        }
    }

    /**
     * Record successful operation
     */
    private suspend fun onSuccess() {
        mutex.withLock {
            failureCount = 0

            when (state) {
                CircuitState.HALF_OPEN -> {
                    successCount++
                    if (successCount >= config.successThreshold) {
                        state = CircuitState.CLOSED
                        successCount = 0
                    }
                }
                else -> {
                    // Already closed or just opened
                }
            }
        }
    }

    /**
     * Record failed operation
     */
    private suspend fun onFailure() {
        mutex.withLock {
            failureCount++
            lastFailureTime = currentTimeMillis()

            when (state) {
                CircuitState.CLOSED -> {
                    if (failureCount >= config.failureThreshold) {
                        state = CircuitState.OPEN
                    }
                }
                CircuitState.HALF_OPEN -> {
                    state = CircuitState.OPEN
                    successCount = 0
                }
                CircuitState.OPEN -> {
                    // Already open
                }
            }
        }
    }

    /**
     * Manually open circuit (emergency kill switch)
     */
    suspend fun open() {
        mutex.withLock {
            state = CircuitState.OPEN
            lastFailureTime = currentTimeMillis()
        }
    }

    /**
     * Manually close circuit (reset)
     */
    suspend fun close() {
        mutex.withLock {
            state = CircuitState.CLOSED
            failureCount = 0
            successCount = 0
        }
    }

    /**
     * Get current circuit state
     *
     * @return Current CircuitState
     */
    suspend fun getState(): CircuitState {
        return mutex.withLock { state }
    }

    /**
     * Get current failure count
     *
     * @return Number of consecutive failures
     */
    suspend fun getFailureCount(): Int {
        return mutex.withLock { failureCount }
    }

    /**
     * Reset circuit breaker to initial state
     */
    suspend fun reset() {
        mutex.withLock {
            state = CircuitState.CLOSED
            failureCount = 0
            successCount = 0
            lastFailureTime = 0
        }
    }

    /**
     * Check if circuit allows requests
     *
     * @return true if requests are allowed (CLOSED or HALF_OPEN state)
     */
    suspend fun isAllowingRequests(): Boolean {
        return mutex.withLock {
            state != CircuitState.OPEN ||
                    (currentTimeMillis() - lastFailureTime >= config.timeoutMs)
        }
    }

    /**
     * Get time until circuit transitions from OPEN to HALF_OPEN
     *
     * @return Milliseconds until transition, or 0 if not in OPEN state
     */
    suspend fun getTimeUntilHalfOpen(): Long {
        return mutex.withLock {
            if (state == CircuitState.OPEN) {
                val elapsed = currentTimeMillis() - lastFailureTime
                maxOf(0, config.timeoutMs - elapsed)
            } else {
                0
            }
        }
    }
}

/**
 * Platform-agnostic current time in milliseconds
 */
expect fun currentTimeMillis(): Long
