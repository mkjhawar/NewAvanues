package com.augmentalis.avanues.avamagic.components.ipc

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Circuit Breaker
 *
 * Prevents cascading failures by opening circuit after repeated errors.
 *
 * ## State Machine
 * ```
 * CLOSED (normal) → OPEN (failing) → HALF_OPEN (testing) → CLOSED
 *       ↓                                    ↓
 *   failures++                          success → CLOSED
 *                                       failure → OPEN
 * ```
 *
 * @since 1.0.0
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
     * @param operation Operation to execute
     * @return Result of operation or circuit open error
     */
    suspend fun <T> execute(operation: suspend () -> T): Result<T> {
        mutex.withLock {
            when (state) {
                CircuitState.OPEN -> {
                    // Check if enough time has passed to try again
                    if (System.currentTimeMillis() - lastFailureTime >= config.timeoutMs) {
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
            lastFailureTime = System.currentTimeMillis()

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
     * Manually open circuit
     */
    suspend fun open() {
        mutex.withLock {
            state = CircuitState.OPEN
            lastFailureTime = System.currentTimeMillis()
        }
    }

    /**
     * Manually close circuit
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
     */
    suspend fun getState(): CircuitState {
        return mutex.withLock { state }
    }

    /**
     * Get failure count
     */
    suspend fun getFailureCount(): Int {
        return mutex.withLock { failureCount }
    }

    /**
     * Reset circuit breaker
     */
    suspend fun reset() {
        mutex.withLock {
            state = CircuitState.CLOSED
            failureCount = 0
            successCount = 0
            lastFailureTime = 0
        }
    }
}
