package com.augmentalis.avamagic.ipc

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Rate Limiter
 *
 * Token bucket rate limiter to prevent service abuse and ensure fair resource usage.
 *
 * ## Algorithm
 * - Tokens are added at rate of `maxRequestsPerSecond`
 * - Bucket can hold up to `burstSize` tokens
 * - Each request consumes 1 token
 * - Request is rejected if no tokens available
 *
 * ## Usage
 * ```kotlin
 * val rateLimiter = RateLimiter(RateLimitConfig(
 *     maxRequestsPerSecond = 10,
 *     burstSize = 20
 * ))
 *
 * if (rateLimiter.tryAcquire()) {
 *     // Proceed with request
 *     makeRequest()
 * } else {
 *     // Rate limited - wait or reject
 *     val waitTime = rateLimiter.timeUntilNextToken()
 *     println("Rate limited, retry in ${waitTime}ms")
 * }
 * ```
 *
 * @since 1.0.0
 * @author Avanues Platform Team
 */
class RateLimiter(
    private val config: RateLimitConfig = RateLimitConfig()
) {
    private val mutex = Mutex()
    private var tokens: Double = config.burstSize.toDouble()
    private var lastRefillTime: Long = currentTimeMillis()

    /**
     * Try to acquire a token
     *
     * @return true if token acquired, false if rate limit exceeded
     */
    suspend fun tryAcquire(): Boolean {
        return mutex.withLock {
            refillTokens()

            if (tokens >= 1.0) {
                tokens -= 1.0
                true
            } else {
                false
            }
        }
    }

    /**
     * Try to acquire multiple tokens
     *
     * @param count Number of tokens to acquire
     * @return true if all tokens acquired, false if not enough tokens
     */
    suspend fun tryAcquire(count: Int): Boolean {
        return mutex.withLock {
            refillTokens()

            if (tokens >= count.toDouble()) {
                tokens -= count.toDouble()
                true
            } else {
                false
            }
        }
    }

    /**
     * Acquire token with timeout
     *
     * Blocks until token is available or timeout expires.
     *
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return true if token acquired within timeout
     */
    suspend fun acquire(timeoutMs: Long = 1000): Boolean {
        val startTime = currentTimeMillis()

        while (currentTimeMillis() - startTime < timeoutMs) {
            if (tryAcquire()) {
                return true
            }
            delay(10)  // Wait 10ms before retry
        }

        return false
    }

    /**
     * Get time until next token available
     *
     * @return Milliseconds until token available, 0 if tokens available now
     */
    suspend fun timeUntilNextToken(): Long {
        return mutex.withLock {
            refillTokens()

            if (tokens >= 1.0) {
                0
            } else {
                val tokensNeeded = 1.0 - tokens
                val tokensPerMs = config.maxRequestsPerSecond / 1000.0
                (tokensNeeded / tokensPerMs).toLong()
            }
        }
    }

    /**
     * Refill tokens based on time elapsed
     */
    private fun refillTokens() {
        val now = currentTimeMillis()
        val elapsedMs = now - lastRefillTime

        if (elapsedMs > 0) {
            val tokensToAdd = (elapsedMs / 1000.0) * config.maxRequestsPerSecond
            tokens = minOf(tokens + tokensToAdd, config.burstSize.toDouble())
            lastRefillTime = now
        }
    }

    /**
     * Get current token count
     *
     * @return Number of tokens currently available (may be fractional)
     */
    suspend fun getTokens(): Double {
        return mutex.withLock {
            refillTokens()
            tokens
        }
    }

    /**
     * Get available tokens as integer
     *
     * @return Number of whole tokens available
     */
    suspend fun getAvailableTokens(): Int {
        return mutex.withLock {
            refillTokens()
            tokens.toInt()
        }
    }

    /**
     * Reset rate limiter to full capacity
     */
    suspend fun reset() {
        mutex.withLock {
            tokens = config.burstSize.toDouble()
            lastRefillTime = currentTimeMillis()
        }
    }

    /**
     * Check if any tokens are available
     *
     * @return true if at least one token is available
     */
    suspend fun hasTokens(): Boolean {
        return mutex.withLock {
            refillTokens()
            tokens >= 1.0
        }
    }

    /**
     * Get rate limiter statistics
     *
     * @return Current statistics snapshot
     */
    suspend fun getStats(): RateLimiterStats {
        return mutex.withLock {
            refillTokens()
            RateLimiterStats(
                currentTokens = tokens,
                maxTokens = config.burstSize,
                refillRate = config.maxRequestsPerSecond,
                lastRefillTime = lastRefillTime
            )
        }
    }
}

/**
 * Rate limiter statistics
 */
data class RateLimiterStats(
    val currentTokens: Double,
    val maxTokens: Int,
    val refillRate: Int,
    val lastRefillTime: Long
)
