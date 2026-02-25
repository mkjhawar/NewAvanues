/**
 * UniversalInitializationManager.kt - Thread-safe initialization manager for all speech engines
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-09-07
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 *
 * Provides centralized initialization management with:
 * - Thread-safe singleton pattern
 * - Retry logic with exponential backoff
 * - State tracking and race condition prevention
 * - Resource cleanup and recovery
 * - Comprehensive error handling and diagnostics
 */
package com.augmentalis.speechrecognition

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Universal initialization manager for all speech engines
 * Thread-safe singleton with comprehensive state management
 */
class UniversalInitializationManager private constructor() {

    companion object {
        @JvmStatic
        val instance: UniversalInitializationManager by lazy { UniversalInitializationManager() }
        private const val TAG = "UniversalInitManager"

        // Default retry configuration
        private const val DEFAULT_MAX_RETRIES = 1
        private const val DEFAULT_INITIAL_DELAY_MS = 1000L
        private const val DEFAULT_MAX_DELAY_MS = 10000L
        private const val DEFAULT_BACKOFF_MULTIPLIER = 2.0
        private const val DEFAULT_JITTER_MS = 500L
        private const val DEFAULT_TIMEOUT_MS = 30000L
    }

    // Thread-safe state management
    private val initializationStates = ConcurrentHashMap<String, InitializationState>()
    private val initializationMutexes = ConcurrentHashMap<String, Mutex>()
    private val initializationJobs = ConcurrentHashMap<String, Job>()
    private val lastInitializationAttempt = ConcurrentHashMap<String, Long>()

    // Global state tracking
    private val isShuttingDown = AtomicBoolean(false)
    private val totalInitializationAttempts = AtomicLong(0)
    private val successfulInitializations = AtomicLong(0)
    private val failedInitializations = AtomicLong(0)

    /**
     * Initialization state for tracking engine states
     */
    enum class InitializationState {
        NOT_INITIALIZED,
        INITIALIZING,
        INITIALIZED,
        FAILED,
        DEGRADED,
        SHUTTING_DOWN,
        SHUTDOWN
    }

    /**
     * Configuration for initialization attempts
     */
    data class InitializationConfig(
        val engineName: String,
        val maxRetries: Int = DEFAULT_MAX_RETRIES,
        val initialDelayMs: Long = DEFAULT_INITIAL_DELAY_MS,
        val maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
        val backoffMultiplier: Double = DEFAULT_BACKOFF_MULTIPLIER,
        val jitterMs: Long = DEFAULT_JITTER_MS,
        val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        val allowDegradedMode: Boolean = false
    )

    /**
     * Result of initialization attempt
     */
    data class InitializationResult(
        val success: Boolean,
        val state: InitializationState,
        val engineName: String,
        val attempt: Int = 0,
        val totalDuration: Long = 0L,
        val error: String? = null,
        val degradedMode: Boolean = false,
        val metadata: Map<String, Any> = emptyMap()
    )

    /**
     * Initialize an engine with comprehensive error handling and retry logic
     */
    suspend fun initializeEngine(
        config: InitializationConfig,
        context: Context,
        initFunction: suspend (Context) -> Boolean
    ): InitializationResult {

        val startTime = System.currentTimeMillis()
        totalInitializationAttempts.incrementAndGet()

        Log.i(TAG, "Starting initialization for ${config.engineName}")

        // Check if system is shutting down
        if (isShuttingDown.get()) {
            return InitializationResult(
                success = false,
                state = InitializationState.SHUTTING_DOWN,
                engineName = config.engineName,
                error = "System is shutting down"
            )
        }

        // Get or create mutex for this engine
        val mutex = initializationMutexes.getOrPut(config.engineName) { Mutex() }

        return mutex.withLock {
            try {
                // Check current state
                val currentState = initializationStates[config.engineName] ?: InitializationState.NOT_INITIALIZED

                when (currentState) {
                    InitializationState.INITIALIZED -> {
                        Log.d(TAG, "${config.engineName} already initialized")
                        return@withLock InitializationResult(
                            success = true,
                            state = InitializationState.INITIALIZED,
                            engineName = config.engineName,
                            totalDuration = System.currentTimeMillis() - startTime
                        )
                    }

                    InitializationState.INITIALIZING -> {
                        Log.w(TAG, "${config.engineName} already initializing, waiting...")
                        return@withLock waitForInitializationCompletion(config, startTime)
                    }

                    InitializationState.DEGRADED -> {
                        Log.i(TAG, "${config.engineName} in degraded mode, attempting full initialization")
                        // Continue with initialization attempt
                    }

                    InitializationState.FAILED -> {
                        val lastAttempt = lastInitializationAttempt[config.engineName] ?: 0L
                        val timeSinceLastAttempt = System.currentTimeMillis() - lastAttempt

                        if (timeSinceLastAttempt < config.initialDelayMs) {
                            Log.w(TAG, "${config.engineName} failed recently, waiting before retry")
                            delay(config.initialDelayMs - timeSinceLastAttempt)
                        }
                    }

                    InitializationState.SHUTDOWN -> {
                        Log.e(TAG, "${config.engineName} is shutdown, cannot initialize")
                        return@withLock InitializationResult(
                            success = false,
                            state = InitializationState.SHUTDOWN,
                            engineName = config.engineName,
                            error = "Engine is shutdown"
                        )
                    }

                    else -> { /* Continue with initialization */ }
                }

                // Set state to initializing
                initializationStates[config.engineName] = InitializationState.INITIALIZING
                lastInitializationAttempt[config.engineName] = System.currentTimeMillis()

                // Perform initialization with retry logic
                val result = performInitializationWithRetry(config, context, initFunction, startTime)

                // Update final state
                initializationStates[config.engineName] = result.state

                if (result.success) {
                    successfulInitializations.incrementAndGet()
                    Log.i(TAG, "${config.engineName} initialized successfully in ${result.totalDuration}ms")
                } else {
                    failedInitializations.incrementAndGet()
                    Log.e(TAG, "${config.engineName} initialization failed: ${result.error}")
                }

                result

            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during ${config.engineName} initialization", e)
                initializationStates[config.engineName] = InitializationState.FAILED
                failedInitializations.incrementAndGet()

                InitializationResult(
                    success = false,
                    state = InitializationState.FAILED,
                    engineName = config.engineName,
                    totalDuration = System.currentTimeMillis() - startTime,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    /**
     * Perform initialization with retry logic and exponential backoff
     */
    private suspend fun performInitializationWithRetry(
        config: InitializationConfig,
        context: Context,
        initFunction: suspend (Context) -> Boolean,
        startTime: Long
    ): InitializationResult {

        var currentDelay = config.initialDelayMs
        var lastError: String? = null

        for (attempt in 1..config.maxRetries) {
            try {
                Log.d(TAG, "${config.engineName} initialization attempt $attempt/${config.maxRetries}")

                // Create timeout job
                val result = withTimeoutOrNull(config.timeoutMs) {
                    initFunction(context)
                }

                when (result) {
                    null -> {
                        lastError = "Initialization timed out after ${config.timeoutMs}ms"
                        Log.w(TAG, "${config.engineName} attempt $attempt timed out")
                    }

                    true -> {
                        Log.i(TAG, "${config.engineName} initialized successfully on attempt $attempt")
                        return InitializationResult(
                            success = true,
                            state = InitializationState.INITIALIZED,
                            engineName = config.engineName,
                            attempt = attempt,
                            totalDuration = System.currentTimeMillis() - startTime
                        )
                    }

                    false -> {
                        lastError = "Initialization function returned false"
                        Log.w(TAG, "${config.engineName} attempt $attempt failed")
                    }
                }

            } catch (e: Exception) {
                lastError = "Exception during initialization: ${e.message}"
                Log.w(TAG, "${config.engineName} attempt $attempt failed with exception", e)
            }

            // Don't delay after the last attempt
            if (attempt < config.maxRetries) {
                val jitter = Random.nextLong(-config.jitterMs, config.jitterMs)
                val delayWithJitter = (currentDelay + jitter).coerceAtLeast(0)

                Log.d(TAG, "${config.engineName} waiting ${delayWithJitter}ms before retry")
                delay(delayWithJitter)

                // Exponential backoff
                currentDelay = (currentDelay * config.backoffMultiplier).toLong()
                    .coerceAtMost(config.maxDelayMs)
            }
        }

        // All retries failed, try degraded mode if allowed
        if (config.allowDegradedMode) {
            Log.w(TAG, "${config.engineName} attempting degraded mode initialization")

            try {
                val degradedResult = withTimeoutOrNull(config.timeoutMs / 2) {
                    // Simplified initialization for degraded mode
                    false // Placeholder
                }

                if (degradedResult == true) {
                    Log.i(TAG, "${config.engineName} initialized in degraded mode")
                    return InitializationResult(
                        success = true,
                        state = InitializationState.DEGRADED,
                        engineName = config.engineName,
                        attempt = config.maxRetries + 1,
                        totalDuration = System.currentTimeMillis() - startTime,
                        degradedMode = true,
                        metadata = mapOf("degraded_reason" to (lastError ?: "Unknown error"))
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "${config.engineName} degraded mode initialization failed", e)
            }
        }

        // Complete failure
        return InitializationResult(
            success = false,
            state = InitializationState.FAILED,
            engineName = config.engineName,
            attempt = config.maxRetries,
            totalDuration = System.currentTimeMillis() - startTime,
            error = lastError ?: "Unknown error"
        )
    }

    /**
     * Wait for ongoing initialization to complete
     */
    private suspend fun waitForInitializationCompletion(
        config: InitializationConfig,
        startTime: Long
    ): InitializationResult {

        val maxWaitTime = config.timeoutMs
        val checkInterval = 100L
        var elapsed = 0L

        while (elapsed < maxWaitTime) {
            val currentState = initializationStates[config.engineName]

            when (currentState) {
                InitializationState.INITIALIZED -> {
                    return InitializationResult(
                        success = true,
                        state = InitializationState.INITIALIZED,
                        engineName = config.engineName,
                        totalDuration = System.currentTimeMillis() - startTime
                    )
                }

                InitializationState.DEGRADED -> {
                    return InitializationResult(
                        success = true,
                        state = InitializationState.DEGRADED,
                        engineName = config.engineName,
                        totalDuration = System.currentTimeMillis() - startTime,
                        degradedMode = true
                    )
                }

                InitializationState.FAILED -> {
                    return InitializationResult(
                        success = false,
                        state = InitializationState.FAILED,
                        engineName = config.engineName,
                        totalDuration = System.currentTimeMillis() - startTime,
                        error = "Concurrent initialization failed"
                    )
                }

                InitializationState.INITIALIZING -> {
                    delay(checkInterval)
                    elapsed += checkInterval
                }

                else -> {
                    return InitializationResult(
                        success = false,
                        state = currentState ?: InitializationState.FAILED,
                        engineName = config.engineName,
                        totalDuration = System.currentTimeMillis() - startTime,
                        error = "Unexpected state: $currentState"
                    )
                }
            }
        }

        // Timeout waiting for completion
        return InitializationResult(
            success = false,
            state = InitializationState.FAILED,
            engineName = config.engineName,
            totalDuration = System.currentTimeMillis() - startTime,
            error = "Timeout waiting for concurrent initialization"
        )
    }

    /**
     * Check if an engine is initialized
     */
    fun isInitialized(engineName: String): Boolean {
        val state = initializationStates[engineName]
        return state == InitializationState.INITIALIZED || state == InitializationState.DEGRADED
    }

    /**
     * Get current initialization state
     */
    fun getState(engineName: String): InitializationState {
        return initializationStates[engineName] ?: InitializationState.NOT_INITIALIZED
    }

    /**
     * Force shutdown an engine
     */
    suspend fun shutdownEngine(engineName: String) {
        val mutex = initializationMutexes[engineName] ?: return

        mutex.withLock {
            Log.i(TAG, "Shutting down engine: $engineName")

            // Cancel any ongoing initialization
            initializationJobs[engineName]?.cancel()
            initializationJobs.remove(engineName)

            // Update state
            initializationStates[engineName] = InitializationState.SHUTDOWN

            Log.i(TAG, "Engine $engineName shutdown complete")
        }
    }

    /**
     * Reset engine state (for testing or manual recovery)
     */
    suspend fun resetEngine(engineName: String) {
        val mutex = initializationMutexes[engineName] ?: return

        mutex.withLock {
            Log.w(TAG, "Resetting engine state: $engineName")

            // Cancel any ongoing operations
            initializationJobs[engineName]?.cancel()
            initializationJobs.remove(engineName)

            // Reset state
            initializationStates[engineName] = InitializationState.NOT_INITIALIZED
            lastInitializationAttempt.remove(engineName)

            Log.i(TAG, "Engine $engineName state reset")
        }
    }

    /**
     * Get comprehensive diagnostics
     */
    fun getDiagnostics(): Map<String, Any> {
        return mapOf(
            "total_attempts" to totalInitializationAttempts.get(),
            "successful_initializations" to successfulInitializations.get(),
            "failed_initializations" to failedInitializations.get(),
            "success_rate" to if (totalInitializationAttempts.get() > 0) {
                (successfulInitializations.get().toDouble() / totalInitializationAttempts.get() * 100).toInt()
            } else 0,
            "engine_states" to initializationStates.toMap(),
            "is_shutting_down" to isShuttingDown.get(),
            "active_engines" to initializationStates.filter {
                it.value == InitializationState.INITIALIZED || it.value == InitializationState.DEGRADED
            }.keys.toList()
        )
    }

    /**
     * Shutdown all engines and cleanup resources
     */
    suspend fun shutdown() {
        isShuttingDown.set(true)
        Log.i(TAG, "Shutting down Universal Initialization Manager")

        // Cancel all ongoing jobs
        initializationJobs.values.forEach { it.cancel() }
        initializationJobs.clear()

        // Update all states to shutdown
        for (engineName in initializationStates.keys) {
            initializationStates[engineName] = InitializationState.SHUTDOWN
        }

        Log.i(TAG, "Universal Initialization Manager shutdown complete")
    }
}
