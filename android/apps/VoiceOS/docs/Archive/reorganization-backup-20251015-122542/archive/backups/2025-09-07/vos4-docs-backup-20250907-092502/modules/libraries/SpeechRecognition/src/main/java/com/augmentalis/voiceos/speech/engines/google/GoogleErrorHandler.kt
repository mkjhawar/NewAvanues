/**
 * GoogleErrorHandler.kt - Google Cloud Speech error handling and recovery
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * Handles error detection, classification, recovery strategies, and error reporting
 * for Google Cloud Speech Recognition
 */
package com.augmentalis.voiceos.speech.engines.google

import android.util.Log
import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
import com.augmentalis.voiceos.speech.engines.common.ServiceState
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages error handling and recovery for Google Cloud Speech.
 * Provides error classification, recovery strategies, and detailed reporting.
 */
class GoogleErrorHandler(
    private val serviceState: ServiceState
) {
    
    companion object {
        private const val TAG = "GoogleErrorHandler"
        
        // Error codes
        const val ERROR_AUTH = 1001
        const val ERROR_NETWORK = 1002
        const val ERROR_QUOTA = 1003
        const val ERROR_STREAM = 1004
        const val ERROR_AUDIO = 1005
        const val ERROR_TIMEOUT = 1006
        const val ERROR_CONFIG = 1007
        const val ERROR_UNKNOWN = 1999
        
        // Recovery settings
        private const val MAX_RECOVERY_ATTEMPTS = 3
        private const val RECOVERY_DELAY_MS = 2000L
        private const val ERROR_COOLDOWN_MS = 5000L
        private const val MAX_ERROR_HISTORY = 50
    }
    
    // Error listener
    private var errorListener: OnSpeechErrorListener? = null
    
    // Error tracking
    private val errorHistory = mutableListOf<ErrorRecord>()
    private val errorCounts = mutableMapOf<Int, AtomicInteger>()
    private val lastErrorTime = AtomicLong(0)
    private val recoveryAttempts = AtomicInteger(0)
    
    // Recovery callbacks
    private var onRecoveryNeeded: ((ErrorType) -> Unit)? = null
    private var onRestartRequired: (() -> Unit)? = null
    
    // Coroutine scope
    private val errorScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + 
        CoroutineName("GoogleErrorHandler")
    )
    
    /**
     * Initialize error handler
     */
    fun initialize(
        errorListener: OnSpeechErrorListener,
        onRecovery: (ErrorType) -> Unit,
        onRestart: () -> Unit
    ) {
        this.errorListener = errorListener
        this.onRecoveryNeeded = onRecovery
        this.onRestartRequired = onRestart
        
        Log.i(TAG, "Error handler initialized")
    }
    
    /**
     * Handle error with automatic classification and recovery
     */
    fun handleError(error: Throwable, context: String = ""): ErrorResult {
        val errorType = classifyError(error)
        val errorCode = getErrorCode(errorType)
        val message = formatErrorMessage(errorType, error, context)
        
        // Record error
        recordError(errorCode, errorType, message, error)
        
        // Update service state
        serviceState.setState(ServiceState.State.ERROR, message)
        
        // Determine recovery strategy
        val recoveryStrategy = determineRecoveryStrategy(errorType)
        
        // Execute recovery if applicable
        val recoveryResult = if (recoveryStrategy != RecoveryStrategy.NONE) {
            executeRecovery(recoveryStrategy, errorType)
        } else {
            RecoveryResult.NOT_APPLICABLE
        }
        
        // Notify error listener
        errorScope.launch(Dispatchers.Main) {
            errorListener?.invoke(message, errorCode)
        }
        
        Log.e(TAG, "Error handled: $message (code: $errorCode, recovery: $recoveryResult)")
        
        return ErrorResult(
            errorType = errorType,
            errorCode = errorCode,
            message = message,
            recoveryStrategy = recoveryStrategy,
            recoveryResult = recoveryResult,
            shouldRetry = recoveryResult == RecoveryResult.SUCCESS
        )
    }
    
    /**
     * Handle timeout specifically
     */
    fun handleTimeout(context: String = "Recognition timeout"): ErrorResult {
        Log.d(TAG, "Handling timeout: $context")
        
        val error = Exception(context)
        return handleError(error, "TIMEOUT")
    }
    
    /**
     * Classify error type based on exception and message
     */
    private fun classifyError(error: Throwable): ErrorType {
        val message = error.message?.uppercase() ?: ""
        
        return when {
            // Authentication errors
            message.contains("UNAUTHENTICATED") ||
            message.contains("INVALID_API_KEY") ||
            message.contains("API_KEY_INVALID") -> ErrorType.AUTHENTICATION
            
            // Authorization errors
            message.contains("PERMISSION_DENIED") ||
            message.contains("FORBIDDEN") -> ErrorType.AUTHORIZATION
            
            // Quota/billing errors
            message.contains("RESOURCE_EXHAUSTED") ||
            message.contains("QUOTA_EXCEEDED") ||
            message.contains("BILLING") -> ErrorType.QUOTA_EXCEEDED
            
            // Network errors
            message.contains("UNAVAILABLE") ||
            message.contains("DEADLINE_EXCEEDED") ||
            message.contains("CONNECTION") ||
            message.contains("TIMEOUT") ||
            error is kotlinx.coroutines.TimeoutCancellationException -> ErrorType.NETWORK
            
            // Configuration errors
            message.contains("INVALID_ARGUMENT") ||
            message.contains("INVALID_CONFIG") -> ErrorType.CONFIGURATION
            
            // Audio/stream errors
            message.contains("AUDIO") ||
            message.contains("STREAM") ||
            message.contains("RECORDING") -> ErrorType.AUDIO_STREAM
            
            // Recognition specific errors
            message.contains("RECOGNITION") -> ErrorType.RECOGNITION
            
            else -> ErrorType.UNKNOWN
        }
    }
    
    /**
     * Get error code for error type
     */
    private fun getErrorCode(errorType: ErrorType): Int {
        return when (errorType) {
            ErrorType.AUTHENTICATION, ErrorType.AUTHORIZATION -> ERROR_AUTH
            ErrorType.NETWORK -> ERROR_NETWORK
            ErrorType.QUOTA_EXCEEDED -> ERROR_QUOTA
            ErrorType.AUDIO_STREAM -> ERROR_STREAM
            ErrorType.RECOGNITION -> ERROR_STREAM
            ErrorType.CONFIGURATION -> ERROR_CONFIG
            ErrorType.TIMEOUT -> ERROR_TIMEOUT
            ErrorType.UNKNOWN -> ERROR_UNKNOWN
        }
    }
    
    /**
     * Format user-friendly error message
     */
    private fun formatErrorMessage(errorType: ErrorType, @Suppress("UNUSED_PARAMETER") exception: Throwable, context: String): String {
        val baseMessage = when (errorType) {
            ErrorType.AUTHENTICATION -> "Authentication failed. Please check your API key."
            ErrorType.AUTHORIZATION -> "Access denied. Please check your permissions."
            ErrorType.QUOTA_EXCEEDED -> "Quota exceeded. Please check your Google Cloud limits."
            ErrorType.NETWORK -> "Network error. Please check your internet connection."
            ErrorType.CONFIGURATION -> "Configuration error. Please check your settings."
            ErrorType.AUDIO_STREAM -> "Audio processing error. Please check microphone access."
            ErrorType.RECOGNITION -> "Recognition error occurred."
            ErrorType.TIMEOUT -> "Request timed out. Please try again."
            ErrorType.UNKNOWN -> "An unexpected error occurred."
        }
        
        return if (context.isNotEmpty()) {
            "$baseMessage ($context)"
        } else {
            baseMessage
        }
    }
    
    /**
     * Determine appropriate recovery strategy
     */
    private fun determineRecoveryStrategy(errorType: ErrorType): RecoveryStrategy {
        return when (errorType) {
            ErrorType.NETWORK -> RecoveryStrategy.RETRY_WITH_BACKOFF
            ErrorType.AUDIO_STREAM -> RecoveryStrategy.RESTART_COMPONENT
            ErrorType.RECOGNITION -> RecoveryStrategy.RESTART_COMPONENT
            ErrorType.TIMEOUT -> RecoveryStrategy.RESTART_SESSION
            ErrorType.AUTHENTICATION -> RecoveryStrategy.REAUTHENTICATE
            ErrorType.CONFIGURATION -> RecoveryStrategy.RESET_CONFIG
            ErrorType.AUTHORIZATION, ErrorType.QUOTA_EXCEEDED -> RecoveryStrategy.NOTIFY_ONLY
            ErrorType.UNKNOWN -> RecoveryStrategy.RESTART_SESSION
        }
    }
    
    /**
     * Execute recovery strategy
     */
    private fun executeRecovery(strategy: RecoveryStrategy, errorType: ErrorType): RecoveryResult {
        val currentAttempts = recoveryAttempts.incrementAndGet()
        
        if (currentAttempts > MAX_RECOVERY_ATTEMPTS) {
            Log.w(TAG, "Maximum recovery attempts exceeded: $currentAttempts")
            return RecoveryResult.MAX_ATTEMPTS_EXCEEDED
        }
        
        Log.i(TAG, "Executing recovery strategy: $strategy (attempt $currentAttempts)")
        
        return try {
            when (strategy) {
                RecoveryStrategy.RETRY_WITH_BACKOFF -> {
                    errorScope.launch {
                        delay(RECOVERY_DELAY_MS * currentAttempts)
                        onRecoveryNeeded?.invoke(errorType)
                    }
                    RecoveryResult.SUCCESS
                }
                
                RecoveryStrategy.RESTART_COMPONENT -> {
                    onRecoveryNeeded?.invoke(errorType)
                    RecoveryResult.SUCCESS
                }
                
                RecoveryStrategy.RESTART_SESSION -> {
                    errorScope.launch {
                        delay(RECOVERY_DELAY_MS)
                        onRestartRequired?.invoke()
                    }
                    RecoveryResult.SUCCESS
                }
                
                RecoveryStrategy.REAUTHENTICATE -> {
                    onRecoveryNeeded?.invoke(errorType)
                    RecoveryResult.SUCCESS
                }
                
                RecoveryStrategy.RESET_CONFIG -> {
                    onRecoveryNeeded?.invoke(errorType)
                    RecoveryResult.SUCCESS
                }
                
                RecoveryStrategy.NOTIFY_ONLY -> {
                    RecoveryResult.NOT_APPLICABLE
                }
                
                RecoveryStrategy.NONE -> {
                    RecoveryResult.NOT_APPLICABLE
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recovery execution failed", e)
            RecoveryResult.FAILED
        }
    }
    
    /**
     * Record error in history
     */
    private fun recordError(errorCode: Int, errorType: ErrorType, message: String, error: Throwable) {
        val currentTime = System.currentTimeMillis()
        
        // Record in history
        synchronized(errorHistory) {
            errorHistory.add(ErrorRecord(
                timestamp = currentTime,
                errorCode = errorCode,
                errorType = errorType,
                message = message,
                exception = error.javaClass.simpleName,
                stackTrace = error.stackTrace.take(3).joinToString("; ") { "${it.className}.${it.methodName}:${it.lineNumber}" }
            ))
            
            // Trim history if too large
            while (errorHistory.size > MAX_ERROR_HISTORY) {
                errorHistory.removeAt(0)
            }
        }
        
        // Update counts
        errorCounts.getOrPut(errorCode) { AtomicInteger(0) }.incrementAndGet()
        lastErrorTime.set(currentTime)
    }
    
    /**
     * Check if error cooldown is active
     */
    fun isInCooldown(): Boolean {
        val timeSinceLastError = System.currentTimeMillis() - lastErrorTime.get()
        return timeSinceLastError < ERROR_COOLDOWN_MS
    }
    
    /**
     * Reset recovery attempts counter
     */
    fun resetRecoveryAttempts() {
        recoveryAttempts.set(0)
        Log.d(TAG, "Recovery attempts counter reset")
    }
    
    /**
     * Get error statistics
     */
    fun getErrorStats(): Map<String, Any> {
        return mapOf(
            "totalErrors" to errorHistory.size,
            "errorCounts" to errorCounts.mapKeys { getErrorTypeName(it.key) }
                .mapValues { it.value.get() },
            "lastErrorTime" to lastErrorTime.get(),
            "timeSinceLastError" to if (lastErrorTime.get() > 0) 
                System.currentTimeMillis() - lastErrorTime.get() else -1,
            "recoveryAttempts" to recoveryAttempts.get(),
            "isInCooldown" to isInCooldown(),
            "recentErrors" to errorHistory.takeLast(5).map { 
                "${it.errorType.name}: ${it.message}" 
            }
        )
    }
    
    /**
     * Get detailed error history
     */
    fun getErrorHistory(): List<ErrorRecord> {
        return synchronized(errorHistory) {
            errorHistory.toList()
        }
    }
    
    /**
     * Clear error history
     */
    fun clearErrorHistory() {
        synchronized(errorHistory) {
            errorHistory.clear()
        }
        errorCounts.clear()
        recoveryAttempts.set(0)
        Log.i(TAG, "Error history cleared")
    }
    
    /**
     * Get error type name for code
     */
    private fun getErrorTypeName(errorCode: Int): String {
        return when (errorCode) {
            ERROR_AUTH -> "Authentication"
            ERROR_NETWORK -> "Network"
            ERROR_QUOTA -> "Quota"
            ERROR_STREAM -> "Stream"
            ERROR_AUDIO -> "Audio"
            ERROR_TIMEOUT -> "Timeout"
            ERROR_CONFIG -> "Configuration"
            ERROR_UNKNOWN -> "Unknown"
            else -> "Other"
        }
    }
    
    /**
     * Log error summary report
     */
    fun logErrorReport() {
        val stats = getErrorStats()
        Log.i(TAG, """
            Error Handler Report:
            ├── Total Errors: ${stats["totalErrors"]}
            ├── Recent Errors: ${(stats["recentErrors"] as List<*>).joinToString("; ")}
            ├── Recovery Attempts: ${stats["recoveryAttempts"]}
            ├── In Cooldown: ${stats["isInCooldown"]}
            └── Error Counts: ${stats["errorCounts"]}
        """.trimIndent())
    }
    
    /**
     * Shutdown error handler
     */
    fun shutdown() {
        Log.i(TAG, "Shutting down error handler...")
        
        // Log final error report
        logErrorReport()
        
        // Cancel coroutines
        errorScope.cancel()
        
        // Clear references
        errorListener = null
        onRecoveryNeeded = null
        onRestartRequired = null
        
        Log.i(TAG, "Error handler shutdown complete")
    }
    
    // Enums and data classes
    
    enum class ErrorType {
        AUTHENTICATION,
        AUTHORIZATION,
        QUOTA_EXCEEDED,
        NETWORK,
        CONFIGURATION,
        AUDIO_STREAM,
        RECOGNITION,
        TIMEOUT,
        UNKNOWN
    }
    
    enum class RecoveryStrategy {
        NONE,
        RETRY_WITH_BACKOFF,
        RESTART_COMPONENT,
        RESTART_SESSION,
        REAUTHENTICATE,
        RESET_CONFIG,
        NOTIFY_ONLY
    }
    
    enum class RecoveryResult {
        SUCCESS,
        FAILED,
        MAX_ATTEMPTS_EXCEEDED,
        NOT_APPLICABLE
    }
    
    data class ErrorResult(
        val errorType: ErrorType,
        val errorCode: Int,
        val message: String,
        val recoveryStrategy: RecoveryStrategy,
        val recoveryResult: RecoveryResult,
        val shouldRetry: Boolean
    )
    
    data class ErrorRecord(
        val timestamp: Long,
        val errorCode: Int,
        val errorType: ErrorType,
        val message: String,
        val exception: String,
        val stackTrace: String
    )
}