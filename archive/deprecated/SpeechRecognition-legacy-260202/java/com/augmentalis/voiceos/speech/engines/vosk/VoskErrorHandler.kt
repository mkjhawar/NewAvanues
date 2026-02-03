/**
 * VoskErrorHandler.kt - Error handling and recovery for VOSK engine
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * SOLID Principle: Single Responsibility
 * - Handles VOSK-specific errors and recovery mechanisms
 * - Manages error classification and escalation
 * - Provides automatic recovery strategies
 */
package com.augmentalis.voiceos.speech.engines.vosk

import android.util.Log
import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
import com.augmentalis.voiceos.speech.engines.common.ErrorRecoveryManager
import com.augmentalis.voiceos.speech.engines.common.ServiceState
import kotlinx.coroutines.*

/**
 * Error handler for VOSK engine.
 * Handles error classification, recovery strategies, and error escalation.
 */
class VoskErrorHandler(
    private val serviceState: ServiceState,
    private val errorRecoveryManager: ErrorRecoveryManager
) {
    
    companion object {
        private const val TAG = "VoskErrorHandler"
        private const val MAX_RECOVERY_ATTEMPTS = 3
        private const val RECOVERY_DELAY_BASE_MS = 1000L
        private const val ERROR_COOLDOWN_MS = 5000L
        private const val MAX_ERROR_HISTORY = 100
    }
    
    // Error listener
    private var errorListener: OnSpeechErrorListener? = null
    
    // Error tracking
    private val errorHistory = mutableListOf<ErrorEvent>()
    private var totalErrorCount = 0L
    private var recoveryAttempts = 0
    private var lastErrorTime = 0L
    private var lastRecoveryTime = 0L
    
    // Error categories
    private val criticalErrors = setOf(
        "MODEL_LOAD_FAILED",
        "RECOGNIZER_CREATION_FAILED",
        "STORAGE_INIT_FAILED"
    )
    
    private val recoverableErrors = setOf(
        "TIMEOUT",
        "AUDIO_ERROR",
        "RECOGNITION_FAILED",
        "GRAMMAR_BUILD_FAILED"
    )
    
    // Recovery scope
    private val recoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var recoveryJob: Job? = null
    
    /**
     * Set error listener for external error reporting
     */
    fun setErrorListener(listener: OnSpeechErrorListener) {
        this.errorListener = listener
    }
    
    /**
     * Handle VOSK timeout error
     */
    fun handleTimeout() {
        val errorMsg = "Recognition timeout"
        val errorCode = ErrorCode.TIMEOUT
        
        recordError("TIMEOUT", errorMsg, null)
        Log.w(TAG, "Handling timeout error")
        
        // Update service state
        serviceState.setState(ServiceState.State.ERROR, errorMsg)
        
        // Attempt automatic recovery
        attemptRecovery(errorCode, errorMsg) {
            // Recovery action: Reset recognition state
            serviceState.setState(ServiceState.State.READY, "Recovered from timeout")
        }
        
        // Notify error listener
        notifyErrorListener(errorMsg, errorCode.code)
    }
    
    /**
     * Handle VOSK recognition error
     */
    fun handleRecognitionError(exception: Exception?) {
        val errorMsg = "Recognition error: ${exception?.message ?: "Unknown error"}"
        val errorCode = ErrorCode.RECOGNITION_ERROR
        
        recordError("RECOGNITION_ERROR", errorMsg, exception)
        Log.e(TAG, "Handling recognition error", exception)
        
        // Update service state
        serviceState.setState(ServiceState.State.ERROR, errorMsg)
        
        // Attempt automatic recovery
        attemptRecovery(errorCode, errorMsg) {
            // Recovery action: Reinitialize recognizer
            serviceState.setState(ServiceState.State.INITIALIZING, "Recovering from recognition error")
        }
        
        // Notify error listener
        notifyErrorListener(errorMsg, errorCode.code)
    }
    
    /**
     * Handle model loading error
     */
    fun handleModelError(errorMessage: String, exception: Exception?) {
        val errorMsg = "Model error: $errorMessage"
        val errorCode = ErrorCode.MODEL_ERROR
        
        recordError("MODEL_LOAD_FAILED", errorMsg, exception)
        Log.e(TAG, "Handling model error: $errorMessage", exception)
        
        // This is a critical error - no automatic recovery
        serviceState.setState(ServiceState.State.ERROR, errorMsg)
        
        // Check if recovery is possible
        if (canAttemptRecovery(errorCode)) {
            attemptRecovery(errorCode, errorMsg) {
                // Recovery action: Try fallback model or degraded mode
                serviceState.setState(ServiceState.State.DEGRADED, "Running in fallback mode")
            }
        }
        
        // Notify error listener
        notifyErrorListener(errorMsg, errorCode.code)
    }
    
    /**
     * Handle recognizer initialization error
     */
    fun handleRecognizerError(errorMessage: String, exception: Exception?) {
        val errorMsg = "Recognizer error: $errorMessage"
        val errorCode = ErrorCode.RECOGNIZER_ERROR
        
        recordError("RECOGNIZER_CREATION_FAILED", errorMsg, exception)
        Log.e(TAG, "Handling recognizer error: $errorMessage", exception)
        
        // Update service state
        serviceState.setState(ServiceState.State.ERROR, errorMsg)
        
        // Attempt recovery with fallback
        attemptRecovery(errorCode, errorMsg) {
            // Recovery action: Try single recognizer mode
            serviceState.setState(ServiceState.State.DEGRADED, "Running in single recognizer mode")
        }
        
        // Notify error listener
        notifyErrorListener(errorMsg, errorCode.code)
    }
    
    /**
     * Handle storage error
     */
    fun handleStorageError(errorMessage: String, exception: Exception?) {
        val errorMsg = "Storage error: $errorMessage"
        val errorCode = ErrorCode.STORAGE_ERROR
        
        recordError("STORAGE_ERROR", errorMsg, exception)
        Log.e(TAG, "Handling storage error: $errorMessage", exception)
        
        // Storage errors are not critical for recognition
        // Continue operation without persistent storage
        
        // Attempt recovery
        attemptRecovery(errorCode, errorMsg) {
            // Recovery action: Continue without persistent storage
            Log.w(TAG, "Continuing without persistent storage")
        }
        
        // Don't notify error listener for storage errors unless critical
    }
    
    /**
     * Handle grammar generation error
     */
    fun handleGrammarError(errorMessage: String, exception: Exception?) {
        val errorMsg = "Grammar error: $errorMessage"
        val errorCode = ErrorCode.GRAMMAR_ERROR
        
        recordError("GRAMMAR_BUILD_FAILED", errorMsg, exception)
        Log.e(TAG, "Handling grammar error: $errorMessage", exception)
        
        // Grammar errors are recoverable - disable grammar constraints
        attemptRecovery(errorCode, errorMsg) {
            // Recovery action: Disable grammar constraints
            Log.w(TAG, "Disabling grammar constraints due to error")
            serviceState.setState(ServiceState.State.DEGRADED, "Running without grammar constraints")
        }
        
        // Don't notify error listener for recoverable grammar errors
    }
    
    /**
     * Attempt automatic error recovery
     */
    private fun attemptRecovery(errorCode: ErrorCode, errorMessage: String, recoveryAction: suspend () -> Unit) {
        if (!canAttemptRecovery(errorCode)) {
            Log.w(TAG, "Cannot attempt recovery for error: $errorCode")
            return
        }
        
        recoveryJob?.cancel()
        recoveryJob = recoveryScope.launch {
            try {
                val delayMs = calculateRecoveryDelay()
                Log.d(TAG, "Attempting recovery in ${delayMs}ms (attempt ${recoveryAttempts + 1}/$MAX_RECOVERY_ATTEMPTS)")
                
                delay(delayMs)
                
                recoveryAttempts++
                lastRecoveryTime = System.currentTimeMillis()
                
                // Execute recovery action
                recoveryAction()
                
                // Use shared error recovery manager for additional recovery logic
                val errorContext = ErrorRecoveryManager.ErrorContext(
                    type = mapErrorCodeToType(errorCode),
                    message = errorMessage
                )
                errorRecoveryManager.handleError(errorContext)
                
                Log.i(TAG, "Recovery attempt completed for error: $errorCode")
                
            } catch (e: Exception) {
                Log.e(TAG, "Recovery failed for error: $errorCode", e)
                recordError("RECOVERY_FAILED", "Recovery failed: ${e.message}", e)
                
                // If max attempts reached, escalate
                if (recoveryAttempts >= MAX_RECOVERY_ATTEMPTS) {
                    escalateError(errorCode, errorMessage)
                }
            }
        }
    }
    
    /**
     * Check if recovery can be attempted for this error type
     */
    private fun canAttemptRecovery(errorCode: ErrorCode): Boolean {
        // Check if we've exceeded max attempts
        if (recoveryAttempts >= MAX_RECOVERY_ATTEMPTS) {
            return false
        }
        
        // Check cooldown period
        val timeSinceLastError = System.currentTimeMillis() - lastErrorTime
        if (timeSinceLastError < ERROR_COOLDOWN_MS) {
            return false
        }
        
        // Check if error type is recoverable
        return when (errorCode) {
            ErrorCode.TIMEOUT,
            ErrorCode.AUDIO_ERROR,
            ErrorCode.RECOGNITION_ERROR,
            ErrorCode.GRAMMAR_ERROR,
            ErrorCode.STORAGE_ERROR -> true
            
            ErrorCode.MODEL_ERROR,
            ErrorCode.RECOGNIZER_ERROR -> recoveryAttempts < 2 // Limited attempts for critical errors
            
            else -> false
        }
    }
    
    /**
     * Calculate recovery delay with exponential backoff
     */
    private fun calculateRecoveryDelay(): Long {
        return RECOVERY_DELAY_BASE_MS * (1 shl recoveryAttempts.coerceAtMost(4))
    }
    
    /**
     * Map VoskErrorHandler ErrorCode to ErrorRecoveryManager ErrorType
     */
    private fun mapErrorCodeToType(errorCode: ErrorCode): ErrorRecoveryManager.ErrorType {
        return when (errorCode) {
            ErrorCode.TIMEOUT -> ErrorRecoveryManager.ErrorType.TIMEOUT_ERROR
            ErrorCode.RECOGNITION_ERROR -> ErrorRecoveryManager.ErrorType.RECOGNITION_ERROR
            ErrorCode.MODEL_ERROR -> ErrorRecoveryManager.ErrorType.MODEL_LOAD_FAILED
            ErrorCode.RECOGNIZER_ERROR -> ErrorRecoveryManager.ErrorType.INITIALIZATION_FAILED
            ErrorCode.STORAGE_ERROR -> ErrorRecoveryManager.ErrorType.UNKNOWN_ERROR
            ErrorCode.GRAMMAR_ERROR -> ErrorRecoveryManager.ErrorType.UNKNOWN_ERROR
            ErrorCode.AUDIO_ERROR -> ErrorRecoveryManager.ErrorType.AUDIO_ERROR
            ErrorCode.UNKNOWN_ERROR -> ErrorRecoveryManager.ErrorType.UNKNOWN_ERROR
        }
    }

    /**
     * Escalate error when recovery fails
     */
    private fun escalateError(errorCode: ErrorCode, errorMessage: String) {
        Log.e(TAG, "Escalating error after failed recovery: $errorCode - $errorMessage")
        
        recordError("ERROR_ESCALATED", "Recovery failed, escalating: $errorMessage", null)
        
        // Set service to error state
        serviceState.setState(ServiceState.State.ERROR, "Unrecoverable error: $errorMessage")
        
        // Notify error listener with escalation flag
        notifyErrorListener("Unrecoverable error: $errorMessage", -99)
        
        // Reset recovery attempts for future errors
        recoveryAttempts = 0
    }
    
    /**
     * Record error for diagnostics and analysis
     */
    private fun recordError(type: String, message: String, exception: Exception?) {
        totalErrorCount++
        lastErrorTime = System.currentTimeMillis()
        
        val errorEvent = ErrorEvent(
            errorNumber = totalErrorCount,
            type = type,
            message = message,
            exception = exception?.javaClass?.simpleName,
            timestamp = lastErrorTime,
            recoveryAttempts = recoveryAttempts,
            stackTrace = exception?.stackTrace?.take(5)?.joinToString("\n") { it.toString() }
        )
        
        errorHistory.add(errorEvent)
        
        // Keep history within bounds
        if (errorHistory.size > MAX_ERROR_HISTORY) {
            errorHistory.removeAt(0)
        }
    }
    
    /**
     * Notify external error listener
     */
    private fun notifyErrorListener(message: String, code: Int) {
        try {
            errorListener?.let { listener ->
                // Run on main thread for UI updates
                recoveryScope.launch(Dispatchers.Main) {
                    listener(message, code)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying error listener", e)
        }
    }
    
    /**
     * Reset error state and recovery attempts
     */
    fun resetErrorState() {
        recoveryAttempts = 0
        lastRecoveryTime = 0L
        recoveryJob?.cancel()
        recoveryJob = null
        
        Log.d(TAG, "Error state reset")
    }
    
    /**
     * Get error statistics
     */
    fun getErrorStats(): ErrorStats {
        val recentErrorCount = errorHistory.count { 
            (System.currentTimeMillis() - it.timestamp) < 3600000L // Last hour
        }
        
        return ErrorStats(
            totalErrorCount = totalErrorCount,
            recentErrorCount = recentErrorCount,
            recoveryAttempts = recoveryAttempts,
            lastErrorTime = lastErrorTime,
            lastRecoveryTime = lastRecoveryTime,
            errorHistorySize = errorHistory.size
        )
    }
    
    /**
     * Get diagnostic information
     */
    fun getDiagnostics(): Map<String, Any> {
        val stats = getErrorStats()
        
        return mapOf(
            "totalErrorCount" to stats.totalErrorCount,
            "recentErrorCount" to stats.recentErrorCount,
            "currentRecoveryAttempts" to stats.recoveryAttempts,
            "maxRecoveryAttempts" to MAX_RECOVERY_ATTEMPTS,
            "lastErrorTime" to stats.lastErrorTime,
            "timeSinceLastError" to (System.currentTimeMillis() - stats.lastErrorTime),
            "lastRecoveryTime" to stats.lastRecoveryTime,
            "errorHistorySize" to stats.errorHistorySize,
            "hasActiveRecovery" to (recoveryJob?.isActive == true),
            "errorCooldownMs" to ERROR_COOLDOWN_MS
        )
    }
    
    /**
     * Get error history
     */
    fun getErrorHistory(): List<ErrorEvent> = errorHistory.toList()
    
    /**
     * Get recent errors (last hour)
     */
    fun getRecentErrors(): List<ErrorEvent> {
        val cutoffTime = System.currentTimeMillis() - 3600000L // 1 hour
        return errorHistory.filter { it.timestamp > cutoffTime }
    }
    
    /**
     * Clean up error handler resources
     */
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up error handler resources...")
            
            // Cancel recovery job
            recoveryJob?.cancel()
            
            // Cancel scope
            recoveryScope.coroutineContext.cancelChildren()
            
            // Clear references
            errorListener = null
            
            Log.d(TAG, "Error handler cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during error handler cleanup", e)
        }
    }
    
    /**
     * Error codes for classification
     */
    enum class ErrorCode(val code: Int) {
        TIMEOUT(-2),
        RECOGNITION_ERROR(-1),
        MODEL_ERROR(-10),
        RECOGNIZER_ERROR(-11),
        STORAGE_ERROR(-12),
        GRAMMAR_ERROR(-13),
        AUDIO_ERROR(-14),
        UNKNOWN_ERROR(-99)
    }
    
    /**
     * Data class for error statistics
     */
    data class ErrorStats(
        val totalErrorCount: Long,
        val recentErrorCount: Int,
        val recoveryAttempts: Int,
        val lastErrorTime: Long,
        val lastRecoveryTime: Long,
        val errorHistorySize: Int
    )
    
    /**
     * Data class for error event tracking
     */
    data class ErrorEvent(
        val errorNumber: Long,
        val type: String,
        val message: String,
        val exception: String?,
        val timestamp: Long,
        val recoveryAttempts: Int,
        val stackTrace: String?
    )
}