/**
 * AndroidErrorHandler.kt - Error handling and recovery for AndroidSTTEngine
 * 
 * Extracted from AndroidSTTEngine as part of SOLID refactoring
 * Handles all error scenarios and recovery strategies:
 * - Error classification and analysis
 * - Recovery strategy selection
 * - Automatic retry logic
 * - Fallback mechanisms
 * - Error reporting and logging
 * 
 * © Augmentalis Inc, Intelligent Devices LLC, Manoj Jhawar, Aman Jhawar
 */
package com.augmentalis.voiceos.speech.engines.android

import android.speech.SpeechRecognizer
import android.util.Log
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceos.speech.engines.common.ServiceState
import com.augmentalis.voiceos.speech.engines.common.ErrorRecoveryManager
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages error handling and recovery strategies for AndroidSTTEngine.
 * Provides intelligent error classification and automated recovery.
 */
class AndroidErrorHandler(
    private val serviceState: ServiceState,
    private val errorRecoveryManager: ErrorRecoveryManager
) {
    
    companion object {
        private const val TAG = "AndroidErrorHandler"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_BASE_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 10000L
        private const val ERROR_COOLDOWN_MS = 30000L // 30 seconds between critical errors
    }
    
    // Error tracking
    private val errorCounts = mutableMapOf<ErrorType, AtomicInteger>()
    private val lastCriticalError = AtomicLong(0L)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Error callbacks
    private var onErrorCallback: ((Int, String, RecoveryAction) -> Unit)? = null
    private var onRecoveryCallback: ((ErrorType, RecoveryAction, Boolean) -> Unit)? = null
    
    /**
     * Error classification based on Android SpeechRecognizer error codes
     */
    enum class ErrorType {
        NETWORK_ERROR,          // Network connectivity issues
        SERVER_ERROR,           // Server-side problems
        AUDIO_ERROR,            // Audio recording issues
        LANGUAGE_ERROR,         // Language not supported/available
        RECOGNITION_ERROR,      // No speech match or timeout
        CLIENT_ERROR,           // Client-side issues
        PERMISSION_ERROR,       // Missing permissions
        RESOURCE_ERROR,         // System resource issues
        UNKNOWN_ERROR           // Unclassified errors
    }
    
    /**
     * Recovery actions that can be taken
     */
    enum class RecoveryAction {
        RETRY_IMMEDIATELY,      // Retry without delay
        RETRY_WITH_DELAY,       // Retry after delay
        RESTART_RECOGNITION,    // Restart recognition session
        SWITCH_TO_OFFLINE,      // Use offline recognition if available
        DEGRADE_GRACEFULLY,     // Reduce functionality
        REPORT_ONLY,            // Just report, no recovery
        IGNORE                  // Ignore recoverable errors
    }
    
    init {
        // Initialize error counters
        ErrorType.values().forEach { type ->
            errorCounts[type] = AtomicInteger(0)
        }
    }
    
    /**
     * Set error callback
     */
    fun setOnErrorCallback(callback: (Int, String, RecoveryAction) -> Unit) {
        onErrorCallback = callback
    }
    
    /**
     * Set recovery callback
     */
    fun setOnRecoveryCallback(callback: (ErrorType, RecoveryAction, Boolean) -> Unit) {
        onRecoveryCallback = callback
    }
    
    /**
     * Handle recognition error and determine recovery strategy
     */
    fun handleError(
        errorCode: Int,
        currentMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND,
        retryCount: Int = 0
    ): RecoveryAction {
        
        val errorType = classifyError(errorCode)
        val errorMessage = getErrorMessage(errorCode)
        
        // Increment error counter
        errorCounts[errorType]?.incrementAndGet()
        
        Log.w(TAG, "Handling error: $errorCode ($errorType) - $errorMessage (retry: $retryCount)")
        
        // Determine recovery action
        val recoveryAction = determineRecoveryAction(errorType, errorCode, retryCount)
        
        // Update service state if needed
        updateServiceStateForError(errorType, errorCode, errorMessage)
        
        // Execute recovery action
        executeRecoveryAction(errorType, recoveryAction, errorCode, currentMode)
        
        // Notify callback
        onErrorCallback?.invoke(errorCode, errorMessage, recoveryAction)
        
        return recoveryAction
    }
    
    /**
     * Classify error by type
     */
    private fun classifyError(errorCode: Int): ErrorType {
        return when (errorCode) {
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> ErrorType.NETWORK_ERROR
            
            SpeechRecognizer.ERROR_SERVER -> ErrorType.SERVER_ERROR
            
            SpeechRecognizer.ERROR_AUDIO -> ErrorType.AUDIO_ERROR
            
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> ErrorType.LANGUAGE_ERROR
            
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> ErrorType.RECOGNITION_ERROR
            
            SpeechRecognizer.ERROR_CLIENT -> ErrorType.CLIENT_ERROR
            
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> ErrorType.PERMISSION_ERROR
            
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> ErrorType.RESOURCE_ERROR
            
            else -> ErrorType.UNKNOWN_ERROR
        }
    }
    
    /**
     * Determine appropriate recovery action
     */
    private fun determineRecoveryAction(
        errorType: ErrorType,
        errorCode: Int,
        retryCount: Int
    ): RecoveryAction {
        
        // Check if we've exceeded retry limit
        if (retryCount >= MAX_RETRY_ATTEMPTS) {
            return when (errorType) {
                ErrorType.LANGUAGE_ERROR,
                ErrorType.PERMISSION_ERROR -> RecoveryAction.REPORT_ONLY
                else -> RecoveryAction.DEGRADE_GRACEFULLY
            }
        }
        
        // Check error cooldown for critical errors
        if (isCriticalError(errorType)) {
            val timeSinceLastCritical = System.currentTimeMillis() - lastCriticalError.get()
            if (timeSinceLastCritical < ERROR_COOLDOWN_MS) {
                Log.w(TAG, "Critical error cooldown active, reporting only")
                return RecoveryAction.REPORT_ONLY
            }
            lastCriticalError.set(System.currentTimeMillis())
        }
        
        return when (errorType) {
            ErrorType.NETWORK_ERROR -> {
                if (retryCount == 0) RecoveryAction.RETRY_WITH_DELAY
                else RecoveryAction.SWITCH_TO_OFFLINE
            }
            
            ErrorType.SERVER_ERROR -> {
                if (retryCount < 2) RecoveryAction.RETRY_WITH_DELAY
                else RecoveryAction.DEGRADE_GRACEFULLY
            }
            
            ErrorType.AUDIO_ERROR -> {
                RecoveryAction.RESTART_RECOGNITION
            }
            
            ErrorType.LANGUAGE_ERROR -> {
                RecoveryAction.REPORT_ONLY // Can't recover from unsupported language
            }
            
            ErrorType.RECOGNITION_ERROR -> {
                when (errorCode) {
                    SpeechRecognizer.ERROR_NO_MATCH -> RecoveryAction.IGNORE
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> RecoveryAction.RETRY_IMMEDIATELY
                    else -> RecoveryAction.RETRY_WITH_DELAY
                }
            }
            
            ErrorType.CLIENT_ERROR -> {
                RecoveryAction.IGNORE // Usually recoverable client errors
            }
            
            ErrorType.PERMISSION_ERROR -> {
                RecoveryAction.REPORT_ONLY // Needs user action
            }
            
            ErrorType.RESOURCE_ERROR -> {
                if (errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    RecoveryAction.RETRY_WITH_DELAY
                } else {
                    RecoveryAction.DEGRADE_GRACEFULLY
                }
            }
            
            ErrorType.UNKNOWN_ERROR -> {
                if (retryCount == 0) RecoveryAction.RETRY_WITH_DELAY
                else RecoveryAction.REPORT_ONLY
            }
        }
    }
    
    /**
     * Check if error is critical
     */
    private fun isCriticalError(errorType: ErrorType): Boolean {
        return when (errorType) {
            ErrorType.PERMISSION_ERROR,
            ErrorType.LANGUAGE_ERROR -> true
            else -> false
        }
    }
    
    /**
     * Update service state based on error
     */
    private fun updateServiceStateForError(errorType: ErrorType, errorCode: Int, errorMessage: String) {
        when (errorType) {
            ErrorType.NETWORK_ERROR,
            ErrorType.SERVER_ERROR,
            ErrorType.LANGUAGE_ERROR,
            ErrorType.PERMISSION_ERROR -> {
                serviceState.updateState(ServiceState.State.ERROR, errorMessage)
            }
            
            ErrorType.RESOURCE_ERROR -> {
                if (errorCode != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    serviceState.updateState(ServiceState.State.DEGRADED, errorMessage)
                }
            }
            
            ErrorType.RECOGNITION_ERROR,
            ErrorType.CLIENT_ERROR -> {
                // Don't change state for recoverable errors
            }
            
            else -> {
                serviceState.updateState(ServiceState.State.DEGRADED, errorMessage)
            }
        }
    }
    
    /**
     * Execute recovery action
     */
    private fun executeRecoveryAction(
        errorType: ErrorType,
        action: RecoveryAction,
        @Suppress("UNUSED_PARAMETER") errorCode: Int,
        @Suppress("UNUSED_PARAMETER") speechMode: SpeechMode
    ) {
        Log.i(TAG, "Executing recovery action: $action for error type: $errorType")
        
        scope.launch {
            val success = when (action) {
                RecoveryAction.RETRY_IMMEDIATELY -> {
                    // Handled by caller
                    true
                }
                
                RecoveryAction.RETRY_WITH_DELAY -> {
                    val delay = calculateRetryDelay(errorType)
                    delay(delay)
                    true
                }
                
                RecoveryAction.RESTART_RECOGNITION -> {
                    // Restart recognition
                    true
                }
                
                RecoveryAction.SWITCH_TO_OFFLINE -> {
                    // This would need to be handled by the engine
                    Log.i(TAG, "Switching to offline mode requested")
                    false // Not implemented in base Android STT
                }
                
                RecoveryAction.DEGRADE_GRACEFULLY -> {
                    serviceState.updateState(ServiceState.State.DEGRADED, "Operating in degraded mode")
                    true
                }
                
                RecoveryAction.REPORT_ONLY,
                RecoveryAction.IGNORE -> {
                    true // No action needed
                }
            }
            
            // Notify recovery callback
            onRecoveryCallback?.invoke(errorType, action, success)
        }
    }
    
    /**
     * Calculate retry delay based on error type
     */
    private fun calculateRetryDelay(errorType: ErrorType): Long {
        val baseDelay = when (errorType) {
            ErrorType.NETWORK_ERROR -> RETRY_BASE_DELAY_MS * 2 // Longer for network issues
            ErrorType.SERVER_ERROR -> RETRY_BASE_DELAY_MS * 3  // Even longer for server issues
            ErrorType.RESOURCE_ERROR -> RETRY_BASE_DELAY_MS / 2 // Shorter for resource conflicts
            else -> RETRY_BASE_DELAY_MS
        }
        
        // Add jitter to prevent thundering herd
        val jitter = (Math.random() * 0.5 + 0.75) // 75-125% of base delay
        return (baseDelay * jitter).toLong().coerceAtMost(MAX_RETRY_DELAY_MS)
    }
    
    /**
     * Get human-readable error message
     */
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_NETWORK -> "Network error occurred"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported"
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language unavailable"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timeout"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests"
            else -> "Unknown error ($errorCode)"
        }
    }
    
    /**
     * Get error statistics
     */
    fun getErrorStats(): ErrorStats {
        val totalErrors = errorCounts.values.sumOf { it.get() }
        val errorBreakdown = errorCounts.mapValues { it.value.get() }
        
        return ErrorStats(
            totalErrors = totalErrors,
            errorBreakdown = errorBreakdown,
            lastCriticalErrorTime = lastCriticalError.get()
        )
    }
    
    /**
     * Reset error counters
     */
    fun resetErrorStats() {
        errorCounts.values.forEach { it.set(0) }
        lastCriticalError.set(0L)
        Log.d(TAG, "Error statistics reset")
    }
    
    /**
     * Check if error handling is in cooldown
     */
    fun isInCooldown(): Boolean {
        val timeSinceLastCritical = System.currentTimeMillis() - lastCriticalError.get()
        return timeSinceLastCritical < ERROR_COOLDOWN_MS
    }
    
    /**
     * Destroy error handler
     */
    fun destroy() {
        scope.cancel()
        Log.d(TAG, "AndroidErrorHandler destroyed")
    }
    
    /**
     * Data class for error statistics
     */
    data class ErrorStats(
        val totalErrors: Int,
        val errorBreakdown: Map<ErrorType, Int>,
        val lastCriticalErrorTime: Long
    )
}