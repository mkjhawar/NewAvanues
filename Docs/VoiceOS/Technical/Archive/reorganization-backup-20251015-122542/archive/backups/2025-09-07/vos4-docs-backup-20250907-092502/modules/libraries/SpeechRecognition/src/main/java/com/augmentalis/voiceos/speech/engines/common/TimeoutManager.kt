/**
 * TimeoutManager.kt - Unified timeout management for all engines
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 */
package com.augmentalis.voiceos.speech.engines.common

import kotlinx.coroutines.*

/**
 * Manages timeouts for speech recognition operations across all engines.
 * Provides consistent timeout behavior with proper coroutine lifecycle management.
 */
class TimeoutManager(
    private val scope: CoroutineScope,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private var timeoutJob: Job? = null
    private var currentTimeout: Long = 0L
    private var startTime: Long = 0L
    private var isActive = false
    
    /**
     * Start a new timeout timer
     * Cancels any existing timeout before starting
     * 
     * @param duration Timeout duration in milliseconds (must be positive)
     * @param onTimeout Callback to execute when timeout occurs
     * @param dispatcher Dispatcher to run callback on (default: defaultDispatcher)
     */
    fun startTimeout(
        duration: Long, 
        onTimeout: () -> Unit,
        dispatcher: CoroutineDispatcher = defaultDispatcher
    ) {
        // Validate duration
        if (duration <= 0) {
            throw IllegalArgumentException("Timeout duration must be positive, got: $duration")
        }
        
        cancelTimeout()
        currentTimeout = duration
        startTime = System.currentTimeMillis()
        isActive = true
        
        timeoutJob = scope.launch {
            try {
                delay(duration)
                if (isActive) {
                    withContext(dispatcher) {
                        onTimeout()
                    }
                }
            } catch (e: CancellationException) {
                // Normal cancellation, ignore
            }
        }
    }
    
    /**
     * Cancel current timeout
     */
    fun cancelTimeout() {
        isActive = false
        timeoutJob?.cancel()
        timeoutJob = null
        currentTimeout = 0L
        startTime = 0L
    }
    
    /**
     * Reset timeout with same duration
     * Useful for extending timeout on partial results
     * 
     * @param duration New timeout duration (uses previous if not specified)
     * @param onTimeout Callback to execute when timeout occurs
     * @param dispatcher Dispatcher to run callback on (default: defaultDispatcher)
     */
    fun resetTimeout(
        duration: Long = currentTimeout, 
        onTimeout: () -> Unit,
        dispatcher: CoroutineDispatcher = defaultDispatcher
    ) {
        if (duration > 0) {
            startTimeout(duration, onTimeout, dispatcher)
        }
    }
    
    /**
     * Extend current timeout by additional duration
     * 
     * @param additionalMs Additional milliseconds to add to current timeout (must be positive)
     * @param onTimeout Callback to execute when timeout occurs
     * @param dispatcher Dispatcher to run callback on (default: defaultDispatcher)
     */
    fun extendTimeout(
        additionalMs: Long, 
        onTimeout: () -> Unit,
        dispatcher: CoroutineDispatcher = defaultDispatcher
    ) {
        if (additionalMs <= 0) {
            throw IllegalArgumentException("Additional time must be positive, got: $additionalMs")
        }
        
        if (isActive && timeoutJob?.isActive == true) {
            val newDuration = currentTimeout + additionalMs
            startTimeout(newDuration, onTimeout, dispatcher)
        }
    }
    
    /**
     * Check if timeout is currently active
     */
    fun isTimeoutActive(): Boolean = isActive && timeoutJob?.isActive == true
    
    /**
     * Get remaining time in milliseconds
     * Returns 0 if no timeout is active
     */
    fun getRemainingTime(): Long {
        return if (isTimeoutActive() && startTime > 0) {
            val elapsed = System.currentTimeMillis() - startTime
            val remaining = currentTimeout - elapsed
            if (remaining > 0) remaining else 0L
        } else {
            0L
        }
    }
    
    /**
     * Cleanup resources
     * Should be called when manager is no longer needed
     */
    fun cleanup() {
        cancelTimeout()
    }
}