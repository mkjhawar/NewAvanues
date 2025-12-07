/**
 * SdkInitializationManager.kt - Universal SDK Initialization Framework
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-06
 * 
 * Thread-safe singleton managing initialization for all speech engines
 * Prevents concurrent initialization attempts and provides robust error recovery
 */
package com.augmentalis.voiceos.speech.engines.common

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Universal SDK Initialization Manager
 * Provides thread-safe, retry-enabled initialization for all speech engines
 */
object SdkInitializationManager {
    
    private const val TAG = "SdkInitManager"
    
    enum class InitializationState {
        NOT_INITIALIZED,    // Initial state
        INITIALIZING,       // Initialization in progress  
        INITIALIZED,        // Successfully initialized
        FAILED,            // Initialization failed
        DEGRADED           // Partial initialization (fallback mode)
    }
    
    data class InitializationContext(
        val sdkName: String,
        val configPath: String,
        val context: Context,
        val requiredAssets: List<String> = emptyList(),
        val initializationTimeout: Long = 30000L,
        val maxRetries: Int = 3,
        val backoffMultiplier: Double = 2.0,
        val baseDelayMs: Long = 1000L,
        val metadata: Map<String, Any> = emptyMap()
    )
    
    data class InitializationResult(
        val success: Boolean,
        val state: InitializationState,
        val error: String? = null,
        val degradedMode: Boolean = false,
        val initializationTime: Long = 0L,
        val retryCount: Int = 0,
        val metadata: Map<String, Any> = emptyMap()
    )
    
    // Thread-safe state management
    private val stateManager = ConcurrentHashMap<String, InitializationState>()
    private val initializationJobs = ConcurrentHashMap<String, Deferred<InitializationResult>>()
    private val stateLocks = ConcurrentHashMap<String, Mutex>()
    private val initializationTimes = ConcurrentHashMap<String, Long>()
    private val failureCount = ConcurrentHashMap<String, Int>()
    
    /**
     * Initialize SDK with comprehensive error handling and retry logic
     */
    suspend fun initializeSDK(
        context: InitializationContext,
        initializationLogic: suspend (InitializationContext) -> InitializationResult
    ): InitializationResult {
        
        val lock = stateLocks.getOrPut(context.sdkName) { Mutex() }
        
        return lock.withLock {
            val currentState = stateManager[context.sdkName] ?: InitializationState.NOT_INITIALIZED
            
            Log.d(TAG, "${context.sdkName}: Current state = $currentState")
            
            when (currentState) {
                InitializationState.NOT_INITIALIZED -> {
                    performInitialization(context, initializationLogic)
                }
                InitializationState.INITIALIZING -> {
                    // Wait for existing initialization to complete
                    waitForInitialization(context.sdkName)
                }
                InitializationState.INITIALIZED -> {
                    Log.d(TAG, "${context.sdkName}: Already initialized")
                    InitializationResult(
                        success = true, 
                        state = currentState,
                        initializationTime = initializationTimes[context.sdkName] ?: 0L
                    )
                }
                InitializationState.FAILED -> {
                    // Attempt recovery if enough time has passed
                    attemptRecovery(context, initializationLogic)
                }
                InitializationState.DEGRADED -> {
                    Log.d(TAG, "${context.sdkName}: Running in degraded mode")
                    InitializationResult(
                        success = true, 
                        state = currentState, 
                        degradedMode = true,
                        initializationTime = initializationTimes[context.sdkName] ?: 0L
                    )
                }
            }
        }
    }
    
    /**
     * Perform actual initialization with retry mechanism
     */
    private suspend fun performInitialization(
        context: InitializationContext,
        initializationLogic: suspend (InitializationContext) -> InitializationResult
    ): InitializationResult {
        
        Log.i(TAG, "${context.sdkName}: Starting initialization")
        val startTime = System.currentTimeMillis()
        
        // Update state to initializing
        stateManager[context.sdkName] = InitializationState.INITIALIZING
        
        // Create initialization job
        val job = CoroutineScope(Dispatchers.IO).async {
            executeWithRetry(context, initializationLogic)
        }
        
        initializationJobs[context.sdkName] = job
        
        return try {
            val result = job.await()
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            // Update final state
            stateManager[context.sdkName] = result.state
            initializationTimes[context.sdkName] = duration
            
            if (result.success) {
                failureCount.remove(context.sdkName) // Reset failure count on success
                Log.i(TAG, "${context.sdkName}: Initialization completed successfully in ${duration}ms")
            } else {
                val count = failureCount.getOrPut(context.sdkName) { 0 } + 1
                failureCount[context.sdkName] = count
                Log.e(TAG, "${context.sdkName}: Initialization failed (attempt #$count): ${result.error}")
            }
            
            result.copy(initializationTime = duration)
            
        } catch (e: Exception) {
            Log.e(TAG, "${context.sdkName}: Initialization job failed", e)
            stateManager[context.sdkName] = InitializationState.FAILED
            
            InitializationResult(
                success = false,
                state = InitializationState.FAILED,
                error = "Initialization job failed: ${e.message}",
                initializationTime = System.currentTimeMillis() - startTime
            )
        } finally {
            initializationJobs.remove(context.sdkName)
        }
    }
    
    /**
     * Wait for existing initialization to complete
     */
    private suspend fun waitForInitialization(sdkName: String): InitializationResult {
        Log.d(TAG, "$sdkName: Waiting for existing initialization to complete")
        
        val job = initializationJobs[sdkName]
        return if (job != null) {
            try {
                val result = job.await()
                Log.d(TAG, "$sdkName: Existing initialization completed with result: ${result.success}")
                result
            } catch (e: Exception) {
                Log.e(TAG, "$sdkName: Existing initialization job failed", e)
                InitializationResult(
                    success = false,
                    state = InitializationState.FAILED,
                    error = "Existing initialization failed: ${e.message}"
                )
            }
        } else {
            // Job completed between state check and job retrieval
            val currentState = stateManager[sdkName] ?: InitializationState.NOT_INITIALIZED
            InitializationResult(
                success = currentState == InitializationState.INITIALIZED,
                state = currentState
            )
        }
    }
    
    /**
     * Attempt recovery from failed state
     */
    private suspend fun attemptRecovery(
        context: InitializationContext,
        initializationLogic: suspend (InitializationContext) -> InitializationResult  
    ): InitializationResult {
        
        val failures = failureCount[context.sdkName] ?: 0
        
        // Don't attempt recovery too frequently or after too many failures
        if (failures >= 10) {
            Log.w(TAG, "${context.sdkName}: Too many failures ($failures), not attempting recovery")
            return InitializationResult(
                success = false,
                state = InitializationState.FAILED,
                error = "Recovery abandoned after $failures failures"
            )
        }
        
        Log.i(TAG, "${context.sdkName}: Attempting recovery from failed state (failure count: $failures)")
        
        // Clear failed state and retry
        stateManager[context.sdkName] = InitializationState.NOT_INITIALIZED
        
        return performInitialization(context, initializationLogic)
    }
    
    /**
     * Execute initialization with retry mechanism and exponential backoff
     */
    private suspend fun executeWithRetry(
        context: InitializationContext,
        operation: suspend (InitializationContext) -> InitializationResult
    ): InitializationResult {
        
        var lastResult: InitializationResult? = null
        var delay = context.baseDelayMs
        
        repeat(context.maxRetries) { attempt ->
            try {
                Log.d(TAG, "${context.sdkName}: Initialization attempt ${attempt + 1}/${context.maxRetries}")
                
                val result = withTimeout(context.initializationTimeout) {
                    operation(context)
                }
                
                if (result.success) {
                    Log.d(TAG, "${context.sdkName}: Successful on attempt ${attempt + 1}")
                    return result.copy(retryCount = attempt)
                } else {
                    lastResult = result
                    Log.w(TAG, "${context.sdkName}: Failed attempt ${attempt + 1}: ${result.error}")
                }
                
            } catch (e: TimeoutException) {
                lastResult = InitializationResult(
                    success = false,
                    state = InitializationState.FAILED,
                    error = "Initialization timeout after ${context.initializationTimeout}ms"
                )
                Log.w(TAG, "${context.sdkName}: Timeout on attempt ${attempt + 1}")
                
            } catch (e: Exception) {
                lastResult = InitializationResult(
                    success = false,
                    state = InitializationState.FAILED,
                    error = "Initialization exception: ${e.message}"
                )
                Log.w(TAG, "${context.sdkName}: Exception on attempt ${attempt + 1}", e)
            }
            
            // Don't delay after the last attempt
            if (attempt < context.maxRetries - 1) {
                Log.d(TAG, "${context.sdkName}: Waiting ${delay}ms before retry ${attempt + 2}")
                delay(delay)
                delay = (delay * context.backoffMultiplier).toLong()
            }
        }
        
        Log.e(TAG, "${context.sdkName}: All initialization attempts failed")
        return lastResult ?: InitializationResult(
            success = false,
            state = InitializationState.FAILED,
            error = "All ${context.maxRetries} initialization attempts failed",
            retryCount = context.maxRetries
        )
    }
    
    /**
     * Get current initialization state
     */
    fun getInitializationState(sdkName: String): InitializationState {
        return stateManager[sdkName] ?: InitializationState.NOT_INITIALIZED
    }
    
    /**
     * Force reset initialization state (for testing or manual recovery)
     */
    suspend fun resetInitializationState(sdkName: String) {
        val lock = stateLocks.getOrPut(sdkName) { Mutex() }
        lock.withLock {
            Log.w(TAG, "$sdkName: Force resetting initialization state")
            stateManager[sdkName] = InitializationState.NOT_INITIALIZED
            initializationJobs[sdkName]?.cancel()
            initializationJobs.remove(sdkName)
            initializationTimes.remove(sdkName)
        }
    }
    
    /**
     * Get initialization statistics  
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "initialized_sdks" to stateManager.filterValues { it == InitializationState.INITIALIZED }.keys,
            "failed_sdks" to stateManager.filterValues { it == InitializationState.FAILED }.keys,
            "degraded_sdks" to stateManager.filterValues { it == InitializationState.DEGRADED }.keys,
            "failure_counts" to failureCount.toMap(),
            "initialization_times" to initializationTimes.toMap()
        )
    }
    
    /**
     * Cleanup all state (for testing)
     */
    suspend fun cleanup() {
        Log.d(TAG, "Cleaning up all initialization state")
        
        // Cancel all running jobs
        initializationJobs.values.forEach { it.cancel() }
        
        // Clear all state
        stateManager.clear()
        initializationJobs.clear()
        initializationTimes.clear()
        failureCount.clear()
        stateLocks.clear()
    }
}