/**
 * Debouncer.kt - Debounce utility for event throttling
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-22
 *
 * Purpose: Provides debouncing functionality to prevent event flooding
 * Thread-safe implementation using coroutines
 */
package com.augmentalis.voiceoscore.accessibility.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Debouncer for throttling rapid events
 *
 * Ensures that an action is only executed after a specified delay has passed
 * without any new events. If a new event occurs during the delay, the timer resets.
 *
 * Thread-safe: Can be called from multiple threads safely
 *
 * Usage:
 * ```
 * val debouncer = Debouncer(scope, 300L)
 * debouncer.debounce {
 *     // This will only execute if 300ms pass without another call
 *     processEvent()
 * }
 * ```
 *
 * @property scope CoroutineScope for launching debounced actions
 * @property delayMs Delay in milliseconds before executing action
 */
class Debouncer(
    private val scope: CoroutineScope,
    private val delayMs: Long = Const.DEFAULT_DEBOUNCE_DELAY_MS
) {

    /**
     * Current pending job, cancelled when new event arrives
     */
    @Volatile
    private var pendingJob: Job? = null

    /**
     * Last event timestamp for tracking
     */
    private val lastEventTime = AtomicLong(0)

    /**
     * Counter for total events received
     */
    private val eventsReceived = AtomicLong(0)

    /**
     * Counter for events actually executed
     */
    private val eventsExecuted = AtomicLong(0)

    /**
     * Debounce an action
     *
     * Cancels any pending action and schedules a new one after the delay.
     * Only the last action within the delay window will execute.
     *
     * @param action The action to execute after debounce delay
     */
    fun debounce(action: suspend () -> Unit) {
        // Cancel any pending job
        pendingJob?.cancel()

        // Track event
        eventsReceived.incrementAndGet()
        lastEventTime.set(System.currentTimeMillis())

        // Schedule new job
        pendingJob = scope.launch {
            delay(delayMs)
            eventsExecuted.incrementAndGet()
            action()
        }
    }

    /**
     * Immediate execution without debouncing
     *
     * Cancels any pending debounced action and executes immediately.
     * Use when you need to bypass debouncing for high-priority events.
     *
     * @param action The action to execute immediately
     */
    fun immediate(action: suspend () -> Unit) {
        pendingJob?.cancel()
        pendingJob = null

        eventsReceived.incrementAndGet()
        eventsExecuted.incrementAndGet()
        lastEventTime.set(System.currentTimeMillis())

        scope.launch {
            action()
        }
    }

    /**
     * Cancel any pending action
     *
     * Cancels the currently scheduled action without executing it.
     * Next call to debounce() will start fresh.
     */
    fun cancel() {
        pendingJob?.cancel()
        pendingJob = null
    }

    /**
     * Check if action is currently pending
     *
     * @return true if an action is scheduled for execution
     */
    fun isPending(): Boolean = pendingJob?.isActive == true

    /**
     * Get time since last event in milliseconds
     *
     * @return Milliseconds since last debounce() or immediate() call
     */
    fun getTimeSinceLastEvent(): Long {
        val lastTime = lastEventTime.get()
        return if (lastTime == 0L) {
            Long.MAX_VALUE // No events yet
        } else {
            System.currentTimeMillis() - lastTime
        }
    }

    /**
     * Get debouncing statistics
     *
     * @return Statistics about events received vs executed
     */
    fun getStats(): DebouncerStats {
        return DebouncerStats(
            eventsReceived = eventsReceived.get(),
            eventsExecuted = eventsExecuted.get(),
            eventsSuppressed = eventsReceived.get() - eventsExecuted.get(),
            isPending = isPending(),
            timeSinceLastEvent = getTimeSinceLastEvent()
        )
    }

    /**
     * Reset all statistics
     */
    fun resetStats() {
        eventsReceived.set(0)
        eventsExecuted.set(0)
    }

    /**
     * Statistics data class for debouncer metrics
     */
    data class DebouncerStats(
        val eventsReceived: Long,
        val eventsExecuted: Long,
        val eventsSuppressed: Long,
        val isPending: Boolean,
        val timeSinceLastEvent: Long
    ) {
        /**
         * Calculate suppression rate as percentage
         */
        val suppressionRate: Double
            get() = if (eventsReceived == 0L) 0.0
                    else (eventsSuppressed.toDouble() / eventsReceived) * 100.0
    }
}

/**
 * Factory for creating debouncers with common configurations
 */
object DebouncerFactory {

    /**
     * Create debouncer for voice events
     * Uses standard delay for voice input processing
     */
    fun forVoiceEvents(scope: CoroutineScope): Debouncer {
        return Debouncer(scope, 300L)
    }

    /**
     * Create debouncer for accessibility events
     * Uses shorter delay for UI responsiveness
     */
    fun forAccessibilityEvents(scope: CoroutineScope): Debouncer {
        return Debouncer(scope, 150L)
    }

    /**
     * Create debouncer for configuration changes
     * Uses longer delay to batch multiple config updates
     */
    fun forConfigChanges(scope: CoroutineScope): Debouncer {
        return Debouncer(scope, 500L)
    }

    /**
     * Create debouncer for search/filter input
     * Uses standard delay for text input
     */
    fun forSearchInput(scope: CoroutineScope): Debouncer {
        return Debouncer(scope, 400L)
    }

    /**
     * Create debouncer with custom delay
     */
    fun custom(scope: CoroutineScope, delayMs: Long): Debouncer {
        return Debouncer(scope, delayMs)
    }
}
