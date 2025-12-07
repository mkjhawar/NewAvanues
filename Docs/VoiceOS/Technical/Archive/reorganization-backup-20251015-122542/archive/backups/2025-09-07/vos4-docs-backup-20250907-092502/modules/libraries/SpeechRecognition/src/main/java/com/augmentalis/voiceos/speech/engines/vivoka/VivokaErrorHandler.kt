/**
 * VivokaErrorHandler.kt - Error handling and recovery for Vivoka VSDK engine
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * 
 * Extracted from monolithic VivokaEngine.kt as part of SOLID refactoring
 * Handles errors, recovery strategies, and graceful degradation
 */
package com.augmentalis.voiceos.speech.engines.vivoka

import android.util.Log
import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
import com.augmentalis.voiceos.speech.engines.common.ErrorRecoveryManager
import com.augmentalis.voiceos.speech.engines.common.ServiceState
import com.augmentalis.voiceos.speech.engines.common.SpeechError
import kotlinx.coroutines.*

/**
 * Manages error handling and recovery strategies for the Vivoka engine
 */
class VivokaErrorHandler(
    private val coroutineScope: CoroutineScope,
    private val context: android.content.Context
) {
    
    companion object {
        private const val TAG = "VivokaErrorHandler"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_RETRY_DELAY = 1000L // 1 second
        private const val MAX_RETRY_DELAY = 8000L // 8 seconds
        private const val MEMORY_THRESHOLD_MB = 50L
        private const val STATE_PERSISTENCE_INTERVAL = 10000L // 10 seconds
    }
    
    // Shared error recovery manager
    private val errorRecoveryManager = ErrorRecoveryManager("vivoka", context)
    
    // Error state
    @Volatile
    private var isRecovering = false
    @Volatile
    private var retryCount = 0
    @Volatile
    private var lastMemoryCheck = System.currentTimeMillis()
    @Volatile
    private var isInGracefulDegradation = false
    
    // Recovery state persistence
    private var persistedState: Map<String, Any>? = null
    
    // Error listeners
    private var errorListener: OnSpeechErrorListener? = null
    private var serviceState: ServiceState? = null
    
    // Recovery callbacks
    private var onInitializationRecovery: (suspend () -> Boolean)? = null
    private var onAudioRecovery: (suspend () -> Boolean)? = null
    private var onModelRecovery: (suspend () -> Boolean)? = null
    private var onMemoryRecovery: (suspend () -> Boolean)? = null
    
    /**
     * Initialize error handler with callbacks
     */
    fun initialize(
        serviceState: ServiceState,
        errorListener: OnSpeechErrorListener?,
        onInitRecovery: (suspend () -> Boolean)? = null,
        onAudioRecovery: (suspend () -> Boolean)? = null,
        onModelRecovery: (suspend () -> Boolean)? = null,
        onMemoryRecovery: (suspend () -> Boolean)? = null
    ) {
        this.serviceState = serviceState
        this.errorListener = errorListener
        this.onInitializationRecovery = onInitRecovery
        this.onAudioRecovery = onAudioRecovery
        this.onModelRecovery = onModelRecovery
        this.onMemoryRecovery = onMemoryRecovery
        
        Log.d(TAG, "Error handler initialized")
    }
    
    /**
     * Handle VSDK errors with recovery mechanisms
     */
    suspend fun handleVSDKError(codeString: String?, message: String?) {
        try {
            Log.e(TAG, "VSDK error - Code: $codeString, Message: $message")
            
            // Check memory before attempting recovery
            checkMemoryAndCleanup()
            
            val errorType = categorizeError(codeString, message)
            
            // Determine recovery strategy based on error type
            val shouldAttemptRecovery = shouldAttemptRecovery(errorType)
            
            if (shouldAttemptRecovery && !isRecovering) {
                Log.w(TAG, "Attempting automatic recovery from VSDK error: $codeString")
                
                val recoverySuccess = attemptRecovery(errorType, message)
                
                if (recoverySuccess) {
                    Log.i(TAG, "Automatic recovery successful for error: $codeString")
                    return
                }
            }
            
            // If recovery failed or not attempted, report error
            val errorCode = getErrorCode(errorType)
            errorListener?.invoke("Vivoka error: $message", errorCode)
            serviceState?.setState(ServiceState.State.ERROR, message)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during VSDK error handling", e)
            errorListener?.invoke("Error handling failed: ${e.message}", SpeechError.RECOGNITION_ERROR)
            serviceState?.setState(ServiceState.State.ERROR, "Error handling failed")
        }
    }
    
    /**
     * Categorize error type based on error code and message
     */
    private fun categorizeError(codeString: String?, message: String?): ErrorType {
        return when {
            codeString?.contains("audio", ignoreCase = true) == true -> ErrorType.AUDIO_PIPELINE_ERROR
            codeString?.contains("model", ignoreCase = true) == true -> ErrorType.MODEL_LOADING_ERROR
            codeString?.contains("memory", ignoreCase = true) == true -> ErrorType.MEMORY_ERROR
            codeString?.contains("network", ignoreCase = true) == true -> ErrorType.NETWORK_ERROR
            codeString?.contains("init", ignoreCase = true) == true -> ErrorType.INITIALIZATION_ERROR
            message?.contains("audio", ignoreCase = true) == true -> ErrorType.AUDIO_PIPELINE_ERROR
            message?.contains("model", ignoreCase = true) == true -> ErrorType.MODEL_LOADING_ERROR
            message?.contains("memory", ignoreCase = true) == true -> ErrorType.MEMORY_ERROR
            else -> ErrorType.RECOGNITION_ERROR
        }
    }
    
    /**
     * Determine if recovery should be attempted
     */
    private fun shouldAttemptRecovery(errorType: ErrorType): Boolean {
        return when (errorType) {
            ErrorType.AUDIO_PIPELINE_ERROR -> true
            ErrorType.MODEL_LOADING_ERROR -> true
            ErrorType.MEMORY_ERROR -> true
            ErrorType.INITIALIZATION_ERROR -> retryCount < MAX_RETRY_ATTEMPTS
            ErrorType.RECOGNITION_ERROR -> retryCount < MAX_RETRY_ATTEMPTS
            ErrorType.NETWORK_ERROR -> false // Don't auto-recover network errors
        }
    }
    
    /**
     * Attempt recovery based on error type
     */
    private suspend fun attemptRecovery(errorType: ErrorType, @Suppress("UNUSED_PARAMETER") details: String?): Boolean {
        return try {
            isRecovering = true
            retryCount++
            
            Log.i(TAG, "Attempting recovery for ${errorType} (attempt $retryCount)")
            
            val success = when (errorType) {
                ErrorType.AUDIO_PIPELINE_ERROR -> recoverAudioPipeline()
                ErrorType.MODEL_LOADING_ERROR -> recoverModelLoading()
                ErrorType.MEMORY_ERROR -> recoverFromMemoryError()
                ErrorType.INITIALIZATION_ERROR -> recoverInitialization()
                ErrorType.RECOGNITION_ERROR -> recoverFromGeneralError()
                ErrorType.NETWORK_ERROR -> false // Don't auto-recover
            }
            
            if (success) {
                retryCount = 0 // Reset on successful recovery
                serviceState?.setState(ServiceState.State.READY, "Recovered from error")
            }
            
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "Recovery attempt failed", e)
            false
        } finally {
            isRecovering = false
        }
    }
    
    /**
     * Recover from audio pipeline errors
     */
    private suspend fun recoverAudioPipeline(): Boolean {
        Log.i(TAG, "Attempting audio pipeline recovery")
        
        return onAudioRecovery?.invoke() ?: run {
            Log.w(TAG, "No audio recovery callback registered")
            false
        }
    }
    
    /**
     * Recover from model loading errors
     */
    private suspend fun recoverModelLoading(): Boolean {
        Log.i(TAG, "Attempting model loading recovery")
        
        return onModelRecovery?.invoke() ?: run {
            Log.w(TAG, "No model recovery callback registered")
            false
        }
    }
    
    /**
     * Recover from memory errors
     */
    private suspend fun recoverFromMemoryError(): Boolean {
        Log.i(TAG, "Attempting memory error recovery")
        
        return onMemoryRecovery?.invoke() ?: run {
            // Default memory recovery
            try {
                // Force garbage collection
                System.gc()
                delay(2000) // Wait for cleanup
                
                Log.i(TAG, "Default memory recovery completed")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Default memory recovery failed", e)
                false
            }
        }
    }
    
    /**
     * Recover from initialization errors
     */
    private suspend fun recoverInitialization(): Boolean {
        Log.i(TAG, "Attempting initialization recovery")
        
        return onInitializationRecovery?.invoke() ?: run {
            Log.w(TAG, "No initialization recovery callback registered")
            false
        }
    }
    
    /**
     * Recover from general recognition errors
     */
    private suspend fun recoverFromGeneralError(): Boolean {
        Log.i(TAG, "Attempting general error recovery")
        
        // Try audio recovery first, then model recovery
        return recoverAudioPipeline() || recoverModelLoading()
    }
    
    /**
     * Retry operation with exponential backoff
     */
    suspend fun <T> retryWithExponentialBackoff(
        operation: String,
        maxRetries: Int = MAX_RETRY_ATTEMPTS,
        block: suspend () -> T
    ): T {
        retryCount = 0
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                retryCount = attempt
                Log.d(TAG, "Attempting $operation (attempt ${attempt + 1}/$maxRetries)")
                return block()
            } catch (e: Exception) {
                lastException = e
                retryCount = attempt + 1
                
                if (attempt < maxRetries - 1) {
                    val delay = minOf(
                        BASE_RETRY_DELAY * (1L shl attempt),
                        MAX_RETRY_DELAY
                    )
                    Log.w(TAG, "$operation failed (attempt ${attempt + 1}), retrying in ${delay}ms", e)
                    delay(delay)
                } else {
                    Log.e(TAG, "$operation failed after $maxRetries attempts", e)
                }
            }
        }
        
        // If we get here, all retries failed
        val errorCode = SpeechError.INITIALIZATION_ERROR
        errorListener?.invoke("$operation failed after $maxRetries attempts: ${lastException?.message}", errorCode)
        serviceState?.setState(ServiceState.State.ERROR, lastException?.message)
        throw lastException ?: Exception("$operation failed after $maxRetries attempts")
    }
    
    /**
     * Check memory usage and cleanup if necessary
     */
    fun checkMemoryAndCleanup() {
        try {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastMemoryCheck < 5000) return // Check at most every 5 seconds
            
            lastMemoryCheck = currentTime
            
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            
            Log.d(TAG, "Memory usage: ${usedMemory}MB")
            
            if (usedMemory > MEMORY_THRESHOLD_MB) {
                Log.w(TAG, "High memory usage detected (${usedMemory}MB), performing cleanup")
                
                // Force garbage collection
                System.gc()
                
                val newUsedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                Log.i(TAG, "Memory cleanup completed: ${usedMemory}MB → ${newUsedMemory}MB")
                
                if (newUsedMemory > MEMORY_THRESHOLD_MB) {
                    Log.e(TAG, "Memory still high after cleanup, may need engine restart")
                    errorListener?.invoke("High memory usage: ${newUsedMemory}MB", SpeechError.MEMORY_ERROR)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during memory check", e)
        }
    }
    
    /**
     * Persist current error recovery state
     */
    fun persistCurrentState(additionalState: Map<String, Any> = emptyMap()) {
        try {
            persistedState = mapOf(
                "isRecovering" to isRecovering,
                "retryCount" to retryCount,
                "isInGracefulDegradation" to isInGracefulDegradation,
                "timestamp" to System.currentTimeMillis()
            ) + additionalState
            
            Log.d(TAG, "Error recovery state persisted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist error recovery state", e)
        }
    }
    
    /**
     * Try graceful degradation when all recovery attempts fail
     */
    suspend fun tryGracefulDegradation(originalError: Exception): Boolean {
        Log.w(TAG, "Attempting graceful degradation due to: ${originalError.message}")
        
        return try {
            isInGracefulDegradation = true
            
            // Set degraded state
            serviceState?.setState(ServiceState.State.DEGRADED, "Running in degraded mode")
            
            Log.w(TAG, "Graceful degradation successful - engine running in limited mode")
            errorListener?.invoke("Engine running in degraded mode: ${originalError.message}", 
                                 SpeechError.INITIALIZATION_ERROR)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Graceful degradation also failed", e)
            serviceState?.setState(ServiceState.State.ERROR, "Complete initialization failure")
            false
        }
    }
    
    /**
     * Restore from persisted error state
     */
    fun restoreFromPersistedState(): Boolean {
        return try {
            persistedState?.let { state ->
                val timestamp = state["timestamp"] as? Long ?: 0L
                val currentTime = System.currentTimeMillis()
                
                // Only restore if state is recent (within 1 minute)
                if (currentTime - timestamp < 60000) {
                    isInGracefulDegradation = state["isInGracefulDegradation"] as? Boolean ?: false
                    // Don't restore isRecovering or retryCount as they should start fresh
                    
                    Log.i(TAG, "Error recovery state restored from persisted data")
                    true
                } else {
                    Log.w(TAG, "Persisted error state too old, not restoring")
                    false
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore persisted error state", e)
            false
        }
    }
    
    /**
     * Get error recovery status
     */
    fun getRecoveryStatus(): Map<String, Any> {
        return mapOf(
            "isRecovering" to isRecovering,
            "retryCount" to retryCount,
            "isInDegradedMode" to isInGracefulDegradation,
            "lastMemoryCheck" to lastMemoryCheck,
            "hasPersistedState" to (persistedState != null),
            "maxRetryAttempts" to MAX_RETRY_ATTEMPTS,
            "memoryThresholdMB" to MEMORY_THRESHOLD_MB
        )
    }
    
    /**
     * Reset error handler state
     */
    fun reset() {
        Log.d(TAG, "Resetting error handler")
        
        isRecovering = false
        retryCount = 0
        isInGracefulDegradation = false
        persistedState = null
        lastMemoryCheck = System.currentTimeMillis()
    }
    
    /**
     * Get error code for error type
     */
    private fun getErrorCode(errorType: ErrorType): Int {
        return when (errorType) {
            ErrorType.INITIALIZATION_ERROR -> SpeechError.INITIALIZATION_ERROR
            ErrorType.RECOGNITION_ERROR -> SpeechError.RECOGNITION_ERROR
            ErrorType.MODEL_LOADING_ERROR -> SpeechError.MODEL_LOADING_ERROR
            ErrorType.AUDIO_PIPELINE_ERROR -> SpeechError.AUDIO_PIPELINE_ERROR
            ErrorType.MEMORY_ERROR -> SpeechError.MEMORY_ERROR
            ErrorType.NETWORK_ERROR -> SpeechError.NETWORK_ERROR
        }
    }
    
    // Public getters for state
    fun isRecovering(): Boolean = isRecovering
    fun getRetryCount(): Int = retryCount
    fun isInGracefulDegradation(): Boolean = isInGracefulDegradation
    
    /**
     * Error type enumeration
     */
    private enum class ErrorType {
        INITIALIZATION_ERROR,
        RECOGNITION_ERROR,
        MODEL_LOADING_ERROR,
        AUDIO_PIPELINE_ERROR,
        MEMORY_ERROR,
        NETWORK_ERROR
    }
}