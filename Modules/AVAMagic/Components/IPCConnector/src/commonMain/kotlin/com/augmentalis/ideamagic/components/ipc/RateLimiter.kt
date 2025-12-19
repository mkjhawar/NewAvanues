package com.augmentalis.avanues.avamagic.components.ipc

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Rate Limiter
 *
 * Token bucket rate limiter to prevent service abuse.
 *
 * ## Algorithm
 * - Tokens are added at rate of `maxRequestsPerSecond`
 * - Bucket can hold up to `burstSize` tokens
 * - Each request consumes 1 token
 * - Request is rejected if no tokens available
 *
 * @since 1.0.0
 */
class RateLimiter(
    private val config: RateLimitConfig = RateLimitConfig()
) {
    private val mutex = Mutex()
    private var tokens: Double = config.burstSize.toDouble()
    private var lastRefillTime: Long = System.currentTimeMillis()

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
     * Acquire token with timeout
     *
     * @param timeoutMs Maximum time to wait
     * @return true if token acquired within timeout
     */
    suspend fun acquire(timeoutMs: Long = 1000): Boolean {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (tryAcquire()) {
                return true
            }
            kotlinx.coroutines.delay(10)  // Wait 10ms before retry
        }

        return false
    }

    /**
     * Get time until next token available
     *
     * @return Milliseconds until token available
     */
    suspend fun timeUntilNextToken(): Long {
        return mutex.withLock {
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
        val now = System.currentTimeMillis()
        val elapsedMs = now - lastRefillTime

        if (elapsedMs > 0) {
            val tokensToAdd = (elapsedMs / 1000.0) * config.maxRequestsPerSecond
            tokens = minOf(tokens + tokensToAdd, config.burstSize.toDouble())
            lastRefillTime = now
        }
    }

    /**
     * Get current token count
     */
    suspend fun getTokens(): Double {
        return mutex.withLock {
            refillTokens()
            tokens
        }
    }

    /**
     * Reset rate limiter
     */
    suspend fun reset() {
        mutex.withLock {
            tokens = config.burstSize.toDouble()
            lastRefillTime = System.currentTimeMillis()
        }
    }
}
