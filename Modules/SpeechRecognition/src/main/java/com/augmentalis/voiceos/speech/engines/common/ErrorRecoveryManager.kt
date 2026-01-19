/**
 * ErrorRecoveryManager.kt - Unified error handling and recovery for all speech engines
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * 
 * Consolidated error handling, retry logic, and recovery strategies
 * Reduces ~200 lines of duplicated code across engines
 */
package com.augmentalis.voiceos.speech.engines.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.math.pow

/**
 * Centralized error handling and recovery management for all engines.
 * Provides retry strategies, graceful degradation, and recovery coordination.
 */
class ErrorRecoveryManager(
    private val engineName: String,
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ErrorRecoveryManager"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY = 1000L // 1 second
        private const val MAX_RETRY_DELAY = 30000L // 30 seconds
        private const val BACKOFF_MULTIPLIER = 2.0
        private const val MEMORY_PRESSURE_THRESHOLD = 0.85 // 85% memory usage
        private const val ASSET_CHECK_INTERVAL = 5000L // 5 seconds
    }
    
    // Retry tracking
    private val retryAttempts = mutableMapOf<ErrorType, AtomicInteger>()
    private val lastErrorTime = mutableMapOf<ErrorType, AtomicLong>()
    private val errorCounts = mutableMapOf<ErrorType, AtomicInteger>()
    
    // Recovery state
    private var isInDegradedMode = false
    private var recoveryInProgress = false
    private val recoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Callbacks
    private var onRecoverySuccess: ((ErrorType) -> Unit)? = null
    private var onRecoveryFailed: ((ErrorType, String) -> Unit)? = null
    private var onDegradedMode: ((Boolean) -> Unit)? = null
    
    enum class ErrorType {
        INITIALIZATION_FAILED,
        MODEL_LOAD_FAILED,
        ASSET_MISSING,
        ASSET_CORRUPT,
        NETWORK_ERROR,
        AUTHENTICATION_ERROR,
        MEMORY_PRESSURE,
        RECOGNITION_ERROR,
        AUDIO_ERROR,
        PERMISSION_DENIED,
        SERVICE_UNAVAILABLE,
        TIMEOUT_ERROR,
        UNKNOWN_ERROR
    }
    
    enum class RecoveryStrategy {
        RETRY_WITH_BACKOFF,
        RETRY_IMMEDIATE,
        GRACEFUL_DEGRADATION,
        CLEAR_CACHE_AND_RETRY,
        RELOAD_MODEL,
        REINITIALIZE,
        WAIT_AND_RETRY,
        SWITCH_TO_OFFLINE,
        REQUEST_PERMISSION,
        NO_RECOVERY
    }
    
    data class ErrorContext(
        val type: ErrorType,
        val message: String,
        val throwable: Throwable? = null,
        val metadata: Map<String, Any> = emptyMap()
    )
    
    data class RecoveryResult(
        val success: Boolean,
        val message: String,
        val attemptsMade: Int,
        val strategyUsed: RecoveryStrategy
    )
    
    /**
     * Handle error with automatic recovery
     */
    suspend fun handleError(
        context: ErrorContext,
        customStrategy: RecoveryStrategy? = null
    ): RecoveryResult = withContext(Dispatchers.IO) {
        
        Log.e(TAG, "[$engineName] Error: ${context.type} - ${context.message}", context.throwable)
        
        // Track error
        trackError(context.type)
        
        // Determine recovery strategy
        val strategy = customStrategy ?: determineRecoveryStrategy(context)
        
        // Execute recovery
        when (strategy) {
            RecoveryStrategy.RETRY_WITH_BACKOFF -> retryWithBackoff(context)
            RecoveryStrategy.RETRY_IMMEDIATE -> retryImmediate(context)
            RecoveryStrategy.GRACEFUL_DEGRADATION -> enterDegradedMode(context)
            RecoveryStrategy.CLEAR_CACHE_AND_RETRY -> clearCacheAndRetry(context)
            RecoveryStrategy.RELOAD_MODEL -> reloadModel(context)
            RecoveryStrategy.REINITIALIZE -> reinitialize(context)
            RecoveryStrategy.WAIT_AND_RETRY -> waitAndRetry(context)
            RecoveryStrategy.SWITCH_TO_OFFLINE -> switchToOfflineMode(context)
            RecoveryStrategy.REQUEST_PERMISSION -> requestPermission(context)
            RecoveryStrategy.NO_RECOVERY -> noRecovery(context)
        }
    }
    
    /**
     * Determine best recovery strategy based on error type and context
     */
    private fun determineRecoveryStrategy(context: ErrorContext): RecoveryStrategy {
        return when (context.type) {
            ErrorType.INITIALIZATION_FAILED -> {
                if (getRetryCount(context.type) < MAX_RETRY_ATTEMPTS) {
                    RecoveryStrategy.RETRY_WITH_BACKOFF
                } else {
                    RecoveryStrategy.GRACEFUL_DEGRADATION
                }
            }
            ErrorType.MODEL_LOAD_FAILED -> RecoveryStrategy.RELOAD_MODEL
            ErrorType.ASSET_MISSING -> RecoveryStrategy.CLEAR_CACHE_AND_RETRY
            ErrorType.ASSET_CORRUPT -> RecoveryStrategy.CLEAR_CACHE_AND_RETRY
            ErrorType.NETWORK_ERROR -> {
                if (isNetworkAvailable()) {
                    RecoveryStrategy.RETRY_WITH_BACKOFF
                } else {
                    RecoveryStrategy.SWITCH_TO_OFFLINE
                }
            }
            ErrorType.AUTHENTICATION_ERROR -> RecoveryStrategy.WAIT_AND_RETRY
            ErrorType.MEMORY_PRESSURE -> RecoveryStrategy.GRACEFUL_DEGRADATION
            ErrorType.RECOGNITION_ERROR -> RecoveryStrategy.RETRY_IMMEDIATE
            ErrorType.AUDIO_ERROR -> RecoveryStrategy.REINITIALIZE
            ErrorType.PERMISSION_DENIED -> RecoveryStrategy.REQUEST_PERMISSION
            ErrorType.SERVICE_UNAVAILABLE -> RecoveryStrategy.WAIT_AND_RETRY
            ErrorType.TIMEOUT_ERROR -> RecoveryStrategy.RETRY_WITH_BACKOFF
            ErrorType.UNKNOWN_ERROR -> RecoveryStrategy.NO_RECOVERY
        }
    }
    
    /**
     * Retry with exponential backoff
     */
    private suspend fun retryWithBackoff(context: ErrorContext): RecoveryResult {
        val attempts = getRetryCount(context.type)
        
        if (attempts >= MAX_RETRY_ATTEMPTS) {
            return RecoveryResult(
                success = false,
                message = "Max retry attempts reached",
                attemptsMade = attempts,
                strategyUsed = RecoveryStrategy.RETRY_WITH_BACKOFF
            )
        }
        
        val delay = calculateBackoffDelay(attempts)
        Log.i(TAG, "[$engineName] Retrying ${context.type} after ${delay}ms (attempt ${attempts + 1})")
        
        delay(delay)
        incrementRetryCount(context.type)
        
        // Callback to trigger actual retry in engine
        return RecoveryResult(
            success = true,
            message = "Retry scheduled",
            attemptsMade = attempts + 1,
            strategyUsed = RecoveryStrategy.RETRY_WITH_BACKOFF
        )
    }
    
    /**
     * Calculate exponential backoff delay
     */
    private fun calculateBackoffDelay(attemptNumber: Int): Long {
        val delay = (INITIAL_RETRY_DELAY * BACKOFF_MULTIPLIER.pow(attemptNumber)).toLong()
        return min(delay, MAX_RETRY_DELAY)
    }
    
    /**
     * Retry immediately without delay
     */
    private suspend fun retryImmediate(context: ErrorContext): RecoveryResult {
        val attempts = getRetryCount(context.type)
        incrementRetryCount(context.type)
        
        Log.i(TAG, "[$engineName] Retrying ${context.type} immediately (attempt ${attempts + 1})")
        
        return RecoveryResult(
            success = true,
            message = "Immediate retry",
            attemptsMade = attempts + 1,
            strategyUsed = RecoveryStrategy.RETRY_IMMEDIATE
        )
    }
    
    /**
     * Enter graceful degradation mode
     */
    private suspend fun enterDegradedMode(context: ErrorContext): RecoveryResult {
        if (!isInDegradedMode) {
            isInDegradedMode = true
            Log.w(TAG, "[$engineName] Entering degraded mode due to ${context.type}")
            onDegradedMode?.invoke(true)
        }
        
        return RecoveryResult(
            success = true,
            message = "Operating in degraded mode",
            attemptsMade = 0,
            strategyUsed = RecoveryStrategy.GRACEFUL_DEGRADATION
        )
    }
    
    /**
     * Clear cache and retry
     */
    private suspend fun clearCacheAndRetry(context: ErrorContext): RecoveryResult {
        Log.i(TAG, "[$engineName] Clearing cache and retrying for ${context.type}")
        
        // Clear relevant caches based on metadata
        val cacheDir = context.metadata["cacheDir"] as? String
        if (cacheDir != null) {
            try {
                File(cacheDir).deleteRecursively()
                Log.d(TAG, "[$engineName] Cache cleared: $cacheDir")
            } catch (e: Exception) {
                Log.e(TAG, "[$engineName] Failed to clear cache", e)
            }
        }
        
        return retryWithBackoff(context)
    }
    
    /**
     * Reload model
     */
    private suspend fun reloadModel(context: ErrorContext): RecoveryResult {
        Log.i(TAG, "[$engineName] Reloading model for ${context.type}")
        
        // Model reload is engine-specific, trigger callback
        return RecoveryResult(
            success = true,
            message = "Model reload requested",
            attemptsMade = 1,
            strategyUsed = RecoveryStrategy.RELOAD_MODEL
        )
    }
    
    /**
     * Reinitialize engine
     */
    private suspend fun reinitialize(context: ErrorContext): RecoveryResult {
        Log.i(TAG, "[$engineName] Reinitializing engine for ${context.type}")
        
        return RecoveryResult(
            success = true,
            message = "Reinitialization requested",
            attemptsMade = 1,
            strategyUsed = RecoveryStrategy.REINITIALIZE
        )
    }
    
    /**
     * Wait and retry
     */
    private suspend fun waitAndRetry(context: ErrorContext): RecoveryResult {
        val waitTime = 5000L // 5 seconds
        Log.i(TAG, "[$engineName] Waiting ${waitTime}ms before retry for ${context.type}")
        
        delay(waitTime)
        
        return RecoveryResult(
            success = true,
            message = "Retry after wait",
            attemptsMade = 1,
            strategyUsed = RecoveryStrategy.WAIT_AND_RETRY
        )
    }
    
    /**
     * Switch to offline mode
     */
    private suspend fun switchToOfflineMode(context: ErrorContext): RecoveryResult {
        Log.i(TAG, "[$engineName] Switching to offline mode due to ${context.type}")
        
        return RecoveryResult(
            success = true,
            message = "Switched to offline mode",
            attemptsMade = 0,
            strategyUsed = RecoveryStrategy.SWITCH_TO_OFFLINE
        )
    }
    
    /**
     * Request permission
     */
    private suspend fun requestPermission(context: ErrorContext): RecoveryResult {
        val permission = context.metadata["permission"] as? String
        Log.i(TAG, "[$engineName] Permission required: $permission")
        
        return RecoveryResult(
            success = false,
            message = "Permission required: $permission",
            attemptsMade = 0,
            strategyUsed = RecoveryStrategy.REQUEST_PERMISSION
        )
    }
    
    /**
     * No recovery possible
     */
    private suspend fun noRecovery(context: ErrorContext): RecoveryResult {
        Log.e(TAG, "[$engineName] No recovery possible for ${context.type}")
        
        return RecoveryResult(
            success = false,
            message = "No recovery available",
            attemptsMade = 0,
            strategyUsed = RecoveryStrategy.NO_RECOVERY
        )
    }
    
    /**
     * Check if network is available
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
    
    /**
     * Check memory pressure
     */
    fun checkMemoryPressure(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val percentage = usedMemory.toFloat() / maxMemory.toFloat()
        
        return percentage >= MEMORY_PRESSURE_THRESHOLD
    }
    
    /**
     * Validate asset integrity
     */
    suspend fun validateAssets(assetPaths: List<String>): Boolean = withContext(Dispatchers.IO) {
        assetPaths.all { path ->
            val file = File(path)
            val exists = file.exists()
            val readable = file.canRead()
            val hasContent = file.length() > 0
            
            if (!exists || !readable || !hasContent) {
                Log.e(TAG, "[$engineName] Asset validation failed: $path (exists=$exists, readable=$readable, size=${file.length()})")
                false
            } else {
                true
            }
        }
    }
    
    /**
     * Track error occurrence
     */
    private fun trackError(type: ErrorType) {
        errorCounts.getOrPut(type) { AtomicInteger(0) }.incrementAndGet()
        lastErrorTime[type] = AtomicLong(System.currentTimeMillis())
    }
    
    /**
     * Get retry count for error type
     */
    private fun getRetryCount(type: ErrorType): Int {
        return retryAttempts.getOrPut(type) { AtomicInteger(0) }.get()
    }
    
    /**
     * Increment retry count
     */
    private fun incrementRetryCount(type: ErrorType) {
        retryAttempts.getOrPut(type) { AtomicInteger(0) }.incrementAndGet()
    }
    
    /**
     * Reset retry count
     */
    fun resetRetryCount(type: ErrorType) {
        retryAttempts[type]?.set(0)
    }
    
    /**
     * Reset all retry counts
     */
    fun resetAllRetryCounts() {
        retryAttempts.forEach { (_, count) -> count.set(0) }
    }

    /**
     * Reset component based on category name
     */
    fun resetComponent(categoryName: String): Boolean {
        return try {
            Log.i(TAG, "[$engineName] Resetting component: $categoryName")
            
            when (categoryName.uppercase()) {
                "INITIALIZATION" -> {
                    resetAllRetryCounts()
                    exitDegradedMode()
                    true
                }
                "MODEL_MANAGEMENT" -> {
                    resetRetryCount(ErrorType.MODEL_LOAD_FAILED)
                    true
                }
                "AUDIO_PROCESSING" -> {
                    resetRetryCount(ErrorType.AUDIO_ERROR)
                    resetRetryCount(ErrorType.RECOGNITION_ERROR)
                    true
                }
                "INFERENCE" -> {
                    resetRetryCount(ErrorType.RECOGNITION_ERROR)
                    resetRetryCount(ErrorType.TIMEOUT_ERROR)
                    true
                }
                "NETWORK" -> {
                    resetRetryCount(ErrorType.NETWORK_ERROR)
                    resetRetryCount(ErrorType.SERVICE_UNAVAILABLE)
                    true
                }
                "SYSTEM_RESOURCE" -> {
                    resetRetryCount(ErrorType.MEMORY_PRESSURE)
                    exitDegradedMode()
                    true
                }
                else -> {
                    Log.w(TAG, "[$engineName] Unknown component category: $categoryName")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$engineName] Failed to reset component $categoryName", e)
            false
        }
    }
    
    /**
     * Exit degraded mode
     */
    fun exitDegradedMode() {
        if (isInDegradedMode) {
            isInDegradedMode = false
            Log.i(TAG, "[$engineName] Exiting degraded mode")
            onDegradedMode?.invoke(false)
        }
    }
    
    /**
     * Get error statistics
     */
    fun getErrorStatistics(): Map<String, Any> {
        return mapOf(
            "errorCounts" to errorCounts.mapValues { it.value.get() },
            "retryAttempts" to retryAttempts.mapValues { it.value.get() },
            "isInDegradedMode" to isInDegradedMode,
            "lastErrors" to lastErrorTime.mapValues { it.value.get() }
        )
    }
    
    /**
     * Set recovery callbacks
     */
    fun setCallbacks(
        onSuccess: ((ErrorType) -> Unit)? = null,
        onFailed: ((ErrorType, String) -> Unit)? = null,
        onDegrade: ((Boolean) -> Unit)? = null
    ) {
        onRecoverySuccess = onSuccess
        onRecoveryFailed = onFailed
        onDegradedMode = onDegrade
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        recoveryScope.cancel()
        Log.i(TAG, "[$engineName] ErrorRecoveryManager destroyed")
    }
}