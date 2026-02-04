/**
 * CommandRateLimiter.kt - Token bucket rate limiter for voice commands
 *
 * YOLO Phase 3 - Medium Priority Issue #29: Lack of Rate Limiting
 *
 * Problem Solved:
 * - No rate limiting on voice commands allows spam/abuse
 * - Potential DoS through rapid command submission
 * - Battery drain from excessive command processing
 * - No protection against malicious/buggy voice input
 *
 * Solution:
 * - Token bucket algorithm with configurable limits
 * - Per-user and global rate limiting
 * - Graceful degradation with cooldown periods
 * - Thread-safe implementation
 * - Monitoring and metrics support
 *
 * Usage:
 * ```kotlin
 * val limiter = CommandRateLimiter()
 *
 * if (limiter.allowCommand("user123")) {
 *     processCommand(command)
 * } else {
 *     showRateLimitError()
 * }
 * ```
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (Code Quality Expert Agent)
 * Created: 2025-11-09
 */
package com.avanues.utils

import android.os.SystemClock
import com.augmentalis.voiceos.constants.VoiceOSConstants.RateLimit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * CommandRateLimiter - Token bucket rate limiter for voice commands
 *
 * Implements token bucket algorithm with per-user tracking and global limits.
 * Thread-safe and optimized for high-frequency voice command scenarios.
 *
 * Features:
 * - Token bucket algorithm for smooth rate limiting
 * - Per-user limits (60 commands/min, 1000 commands/hour)
 * - Global system limits
 * - Automatic token refill
 * - Cooldown enforcement after limit breach
 * - Metrics and monitoring support
 */
class CommandRateLimiter(
    private val maxCommandsPerMinute: Int = RateLimit.MAX_COMMANDS_PER_MINUTE,
    private val maxCommandsPerHour: Int = RateLimit.MAX_COMMANDS_PER_HOUR,
    private val cooldownMs: Long = RateLimit.RATE_LIMIT_COOLDOWN_MS
) {

    companion object {
        private const val TAG = "CommandRateLimiter"

        /** Refill interval for tokens (milliseconds) */
        private const val REFILL_INTERVAL_MS = 1000L // 1 second

        /** Sliding window size for hourly limit (milliseconds) */
        private val HOUR_WINDOW_MS = RateLimit.RATE_LIMIT_WINDOW_MS * 60 // 60 minutes
    }

    /**
     * User rate limit state
     *
     * Tracks token buckets and command history per user.
     */
    private data class UserState(
        val minuteBucket: TokenBucket,
        val hourBucket: TokenBucket,
        @Volatile var lastCooldownStart: Long = 0L,
        @Volatile var consecutiveViolations: Int = 0
    )

    /**
     * Token bucket for rate limiting
     *
     * Simple token bucket with automatic refill.
     */
    private class TokenBucket(
        private val capacity: Int,
        private val refillRate: Int,
        private val refillIntervalMs: Long
    ) {
        private val tokens = AtomicInteger(capacity)
        private val lastRefillTime = AtomicLong(SystemClock.uptimeMillis())

        /**
         * Try to consume a token
         *
         * @return true if token consumed, false if bucket empty
         */
        fun tryConsume(): Boolean {
            refill()

            while (true) {
                val current = tokens.get()
                if (current <= 0) return false

                if (tokens.compareAndSet(current, current - 1)) {
                    return true
                }
            }
        }

        /**
         * Refill tokens based on elapsed time
         */
        private fun refill() {
            val now = SystemClock.uptimeMillis()
            val lastRefill = lastRefillTime.get()
            val elapsed = now - lastRefill

            if (elapsed >= refillIntervalMs) {
                val intervalsElapsed = (elapsed / refillIntervalMs).toInt()
                val tokensToAdd = minOf(intervalsElapsed * refillRate, capacity)

                while (true) {
                    val current = tokens.get()
                    val newValue = minOf(current + tokensToAdd, capacity)

                    if (tokens.compareAndSet(current, newValue)) {
                        lastRefillTime.compareAndSet(lastRefill, now)
                        break
                    }
                }
            }
        }

        /**
         * Get current token count
         */
        fun availableTokens(): Int {
            refill()
            return tokens.get()
        }

        /**
         * Reset bucket to full capacity
         */
        fun reset() {
            tokens.set(capacity)
            lastRefillTime.set(SystemClock.uptimeMillis())
        }
    }

    /** Per-user rate limit tracking */
    private val userStates = ConcurrentHashMap<String, UserState>()

    /** Global command counter for monitoring */
    private val globalCommandCount = AtomicInteger(0)

    /** Global commands in last minute */
    private val globalRecentCommands = ConcurrentHashMap<Long, AtomicInteger>()

    /**
     * Check if command should be allowed
     *
     * Implements token bucket algorithm with per-user and global limits.
     *
     * @param userId User identifier (use "anonymous" for unauthenticated)
     * @return true if command allowed, false if rate limited
     */
    fun allowCommand(userId: String = "anonymous"): Boolean {
        android.util.Log.d(TAG, "allowCommand() called for user: $userId")
        val now = SystemClock.uptimeMillis()

        // Check global rate limit
        if (!checkGlobalLimit()) {
            ConditionalLogger.w(TAG) { "Global rate limit exceeded" }
            return false
        }

        // Get or create user state
        val state = userStates.getOrPut(userId) {
            UserState(
                minuteBucket = TokenBucket(maxCommandsPerMinute, maxCommandsPerMinute, REFILL_INTERVAL_MS * 60),
                hourBucket = TokenBucket(maxCommandsPerHour, maxCommandsPerHour, HOUR_WINDOW_MS)
            )
        }

        // Check if in cooldown period
        if (state.lastCooldownStart > 0) {
            val cooldownRemaining = cooldownMs - (now - state.lastCooldownStart)
            if (cooldownRemaining > 0) {
                ConditionalLogger.d(TAG) {
                    "User $userId in cooldown for ${cooldownRemaining}ms"
                }
                return false
            } else {
                // Cooldown expired, reset
                state.lastCooldownStart = 0L
                state.consecutiveViolations = 0
                state.minuteBucket.reset()
                state.hourBucket.reset()
            }
        }

        // Try to consume tokens from both buckets
        val minuteAllowed = state.minuteBucket.tryConsume()
        val hourAllowed = state.hourBucket.tryConsume()

        if (!minuteAllowed || !hourAllowed) {
            // Rate limit exceeded
            state.consecutiveViolations++

            if (state.consecutiveViolations >= 3) {
                // Multiple violations - enforce cooldown
                state.lastCooldownStart = now
                ConditionalLogger.w(TAG) {
                    "User $userId rate limited - cooldown for ${cooldownMs}ms"
                }
            }

            ConditionalLogger.d(TAG) {
                "User $userId rate limited (minute: $minuteAllowed, hour: $hourAllowed)"
            }
            return false
        }

        // Command allowed
        state.consecutiveViolations = 0
        globalCommandCount.incrementAndGet()

        // Track in global recent commands
        val minuteKey = now / (60 * 1000) // Minute bucket
        globalRecentCommands.getOrPut(minuteKey) { AtomicInteger(0) }.incrementAndGet()

        // Cleanup old global command buckets (keep last hour only)
        cleanupOldBuckets(minuteKey)

        return true
    }

    /**
     * Check global system-wide rate limit
     *
     * Prevents system overload from aggregate command load.
     *
     * @return true if global limit not exceeded
     */
    private fun checkGlobalLimit(): Boolean {
        val now = SystemClock.uptimeMillis()
        val currentMinute = now / (60 * 1000)

        // Sum commands in last minute
        val recentCommands = globalRecentCommands.entries
            .filter { (minute, _) -> currentMinute - minute < 1 }
            .sumOf { it.value.get() }

        // Global limit: 10x individual limit
        return recentCommands < (maxCommandsPerMinute * 10)
    }

    /**
     * Cleanup old command tracking buckets
     */
    private fun cleanupOldBuckets(currentMinute: Long) {
        val cutoff = currentMinute - 60 // Keep last hour
        globalRecentCommands.keys.removeIf { it < cutoff }
    }

    /**
     * Reset rate limit for specific user
     *
     * Use for testing or explicit user cooldown reset.
     *
     * @param userId User to reset
     */
    fun resetUser(userId: String) {
        android.util.Log.d(TAG, "resetUser() called for user: $userId")
        userStates.remove(userId)
        ConditionalLogger.i(TAG) { "Reset rate limit for user: $userId" }
    }

    /**
     * Reset all rate limits
     *
     * Use for testing or system-wide reset.
     */
    fun resetAll() {
        android.util.Log.d(TAG, "resetAll() called")
        userStates.clear()
        globalRecentCommands.clear()
        globalCommandCount.set(0)
        ConditionalLogger.i(TAG) { "Reset all rate limits" }
    }

    /**
     * Get remaining tokens for user
     *
     * @param userId User to check
     * @return Pair of (minute tokens, hour tokens)
     */
    fun getRemainingTokens(userId: String): Pair<Int, Int> {
        val state = userStates[userId] ?: return Pair(maxCommandsPerMinute, maxCommandsPerHour)

        return Pair(
            state.minuteBucket.availableTokens(),
            state.hourBucket.availableTokens()
        )
    }

    /**
     * Get cooldown remaining for user
     *
     * @param userId User to check
     * @return Milliseconds remaining in cooldown, or 0 if not in cooldown
     */
    fun getCooldownRemaining(userId: String): Long {
        val state = userStates[userId] ?: return 0L

        if (state.lastCooldownStart == 0L) return 0L

        val now = SystemClock.uptimeMillis()
        val remaining = cooldownMs - (now - state.lastCooldownStart)

        return maxOf(0L, remaining)
    }

    /**
     * Get metrics for monitoring
     *
     * @return Map of metric name to value
     */
    fun getMetrics(): Map<String, Any> {
        android.util.Log.d(TAG, "getMetrics() called")
        val now = SystemClock.uptimeMillis()
        val currentMinute = now / (60 * 1000)

        val recentCommands = globalRecentCommands.entries
            .filter { (minute, _) -> currentMinute - minute < 1 }
            .sumOf { it.value.get() }

        return mapOf(
            "totalCommands" to globalCommandCount.get(),
            "commandsLastMinute" to recentCommands,
            "activeUsers" to userStates.size,
            "usersInCooldown" to userStates.values.count { it.lastCooldownStart > 0 },
            "maxCommandsPerMinute" to maxCommandsPerMinute,
            "maxCommandsPerHour" to maxCommandsPerHour
        )
    }

    /**
     * Get detailed user metrics
     *
     * @param userId User to check
     * @return Map of metric name to value, or null if user not found
     */
    fun getUserMetrics(userId: String): Map<String, Any>? {
        android.util.Log.d(TAG, "getUserMetrics() called for user: $userId")
        val state = userStates[userId] ?: return null

        return mapOf(
            "minuteTokens" to state.minuteBucket.availableTokens(),
            "hourTokens" to state.hourBucket.availableTokens(),
            "cooldownRemaining" to getCooldownRemaining(userId),
            "consecutiveViolations" to state.consecutiveViolations
        )
    }
}
