/**
 * WhisperErrorHandler.kt - Error handling component for Whisper engine
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * Centralized error handling, recovery strategies, and diagnostic information.
 * Provides comprehensive error management for Whisper speech recognition engine.
 */
package com.augmentalis.voiceos.speech.engines.whisper

import android.content.Context
import android.util.Log
import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
import com.augmentalis.voiceos.speech.engines.common.ErrorRecoveryManager
import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

/**
 * Error codes specific to Whisper engine
 */
object WhisperErrorCode {
    const val INITIALIZATION_ERROR = 2001
    const val MODEL_LOAD_ERROR = 2003
    const val MODEL_DOWNLOAD_ERROR = 2004
    const val NATIVE_LIBRARY_ERROR = 2005
    const val AUDIO_RECORDING_ERROR = 2006
    const val AUDIO_PROCESSING_ERROR = 2007
    const val INFERENCE_ERROR = 2008
    const val INFERENCE_TIMEOUT = 2009
    const val CONFIGURATION_ERROR = 2010
    const val MEMORY_ERROR = 2011
    const val STORAGE_ERROR = 2012
    const val PERMISSION_ERROR = 2013
    const val INVALID_STATE_ERROR = 2014
    const val RESOURCE_EXHAUSTED = 2015
}

/**
 * Error severity levels
 */
enum class ErrorSeverity {
    LOW,        // Minor issues that don't affect functionality
    MEDIUM,     // Issues that may affect performance
    HIGH,       // Issues that significantly impact functionality
    CRITICAL    // Issues that render the engine unusable
}

/**
 * Error categories for better organization
 */
enum class ErrorCategory {
    INITIALIZATION,
    MODEL_MANAGEMENT,
    AUDIO_PROCESSING,
    INFERENCE,
    CONFIGURATION,
    SYSTEM_RESOURCE,
    PERMISSION,
    NETWORK,
    UNKNOWN
}

/**
 * Comprehensive error information
 */
data class WhisperError(
    val code: Int,
    val message: String,
    val severity: ErrorSeverity,
    val category: ErrorCategory,
    val exception: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val context: Map<String, Any> = emptyMap(),
    val recoveryStrategy: ErrorRecoveryStrategy? = null
)

/**
 * Error recovery strategies
 */
enum class ErrorRecoveryStrategy {
    RETRY_IMMEDIATE,
    RETRY_WITH_DELAY,
    RETRY_WITH_FALLBACK_CONFIG,
    RESET_COMPONENT,
    RESTART_ENGINE,
    DEGRADE_FUNCTIONALITY,
    NOTIFY_USER,
    NO_RECOVERY
}

/**
 * Recovery action result
 */
data class RecoveryResult(
    val success: Boolean,
    val message: String,
    val newState: String? = null
)

/**
 * Centralized error handling and recovery for Whisper engine.
 * Provides intelligent error classification, recovery strategies, and diagnostic reporting.
 */
class WhisperErrorHandler(
    private val context: Context,
    private val errorRecoveryManager: ErrorRecoveryManager,
    private val performanceMonitor: PerformanceMonitor
) {
    
    companion object {
        private const val TAG = "WhisperErrorHandler"
        private const val MAX_RECOVERY_ATTEMPTS = 3
        private const val RECOVERY_DELAY_BASE_MS = 1000L
        private const val ERROR_HISTORY_SIZE = 100
    }
    
    // Error tracking
    private val errorHistory = mutableListOf<WhisperError>()
    private val errorCounts = mutableMapOf<Int, AtomicInteger>()
    private val recoveryAttempts = mutableMapOf<String, AtomicInteger>()
    
    // Error listeners
    private var errorListener: OnSpeechErrorListener? = null
    private var criticalErrorCallback: ((WhisperError) -> Unit)? = null
    
    // Recovery scope
    private val recoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Handle an error with automatic classification and recovery
     */
    suspend fun handleError(
        code: Int,
        message: String,
        exception: Throwable? = null,
        context: Map<String, Any> = emptyMap()
    ): RecoveryResult {
        
        val error = createError(code, message, exception, context)
        recordError(error)
        
        Log.e(TAG, "Error occurred: ${error.message} (Code: ${error.code})", error.exception)
        
        // Notify listeners
        notifyErrorListeners(error)
        
        // Attempt recovery if strategy is available
        val recoveryResult = if (error.recoveryStrategy != null) {
            attemptRecovery(error)
        } else {
            RecoveryResult(false, "No recovery strategy available")
        }
        
        // Update performance metrics
        performanceMonitor.recordRecognition(
            System.currentTimeMillis(), 
            recoveryResult.success, 
            "Error: ${error.message}"
        )
        
        return recoveryResult
    }
    
    /**
     * Handle critical errors that require immediate attention
     */
    suspend fun handleCriticalError(
        code: Int,
        message: String,
        exception: Throwable? = null
    ): RecoveryResult {
        
        val error = createError(code, message, exception)
        
        // Force critical severity
        val criticalError = error.copy(severity = ErrorSeverity.CRITICAL)
        
        recordError(criticalError)
        Log.e(TAG, "CRITICAL ERROR: ${criticalError.message}", criticalError.exception)
        
        // Notify critical error callback
        criticalErrorCallback?.invoke(criticalError)
        
        // Attempt aggressive recovery
        return attemptRecovery(criticalError)
    }
    
    /**
     * Create error object with automatic classification
     */
    private fun createError(
        code: Int,
        message: String,
        exception: Throwable?,
        context: Map<String, Any> = emptyMap()
    ): WhisperError {
        
        val severity = classifyErrorSeverity(code, exception)
        val category = classifyErrorCategory(code)
        val recoveryStrategy = determineRecoveryStrategy(code, severity)
        
        return WhisperError(
            code = code,
            message = message,
            severity = severity,
            category = category,
            exception = exception,
            context = context,
            recoveryStrategy = recoveryStrategy
        )
    }
    
    /**
     * Classify error severity based on code and exception
     */
    private fun classifyErrorSeverity(code: Int, exception: Throwable?): ErrorSeverity {
        return when (code) {
            WhisperErrorCode.INITIALIZATION_ERROR,
            WhisperErrorCode.NATIVE_LIBRARY_ERROR,
            WhisperErrorCode.PERMISSION_ERROR -> ErrorSeverity.CRITICAL
            
            WhisperErrorCode.MODEL_LOAD_ERROR,
            WhisperErrorCode.AUDIO_RECORDING_ERROR,
            WhisperErrorCode.MEMORY_ERROR -> ErrorSeverity.HIGH
            
            WhisperErrorCode.INFERENCE_ERROR,
            WhisperErrorCode.AUDIO_PROCESSING_ERROR,
            WhisperErrorCode.CONFIGURATION_ERROR -> ErrorSeverity.MEDIUM
            
            WhisperErrorCode.INFERENCE_TIMEOUT,
            WhisperErrorCode.MODEL_DOWNLOAD_ERROR -> ErrorSeverity.LOW
            
            else -> {
                // Use exception type as fallback
                when (exception) {
                    is OutOfMemoryError -> ErrorSeverity.CRITICAL
                    is SecurityException -> ErrorSeverity.CRITICAL
                    is IllegalStateException -> ErrorSeverity.HIGH
                    is RuntimeException -> ErrorSeverity.MEDIUM
                    else -> ErrorSeverity.LOW
                }
            }
        }
    }
    
    /**
     * Classify error category
     */
    private fun classifyErrorCategory(code: Int): ErrorCategory {
        return when (code) {
            WhisperErrorCode.INITIALIZATION_ERROR,
            WhisperErrorCode.INVALID_STATE_ERROR -> ErrorCategory.INITIALIZATION
            
            WhisperErrorCode.MODEL_LOAD_ERROR,
            WhisperErrorCode.MODEL_DOWNLOAD_ERROR -> ErrorCategory.MODEL_MANAGEMENT
            
            WhisperErrorCode.AUDIO_RECORDING_ERROR,
            WhisperErrorCode.AUDIO_PROCESSING_ERROR -> ErrorCategory.AUDIO_PROCESSING
            
            WhisperErrorCode.INFERENCE_ERROR,
            WhisperErrorCode.INFERENCE_TIMEOUT -> ErrorCategory.INFERENCE
            
            WhisperErrorCode.CONFIGURATION_ERROR -> ErrorCategory.CONFIGURATION
            
            WhisperErrorCode.MEMORY_ERROR,
            WhisperErrorCode.STORAGE_ERROR,
            WhisperErrorCode.RESOURCE_EXHAUSTED -> ErrorCategory.SYSTEM_RESOURCE
            
            WhisperErrorCode.PERMISSION_ERROR -> ErrorCategory.PERMISSION
            
            else -> ErrorCategory.UNKNOWN
        }
    }
    
    /**
     * Determine appropriate recovery strategy
     */
    private fun determineRecoveryStrategy(code: Int, severity: ErrorSeverity): ErrorRecoveryStrategy {
        return when {
            severity == ErrorSeverity.CRITICAL -> {
                when (code) {
                    WhisperErrorCode.PERMISSION_ERROR -> ErrorRecoveryStrategy.NOTIFY_USER
                    WhisperErrorCode.NATIVE_LIBRARY_ERROR -> ErrorRecoveryStrategy.NO_RECOVERY
                    else -> ErrorRecoveryStrategy.RESTART_ENGINE
                }
            }
            
            severity == ErrorSeverity.HIGH -> {
                when (code) {
                    WhisperErrorCode.MODEL_LOAD_ERROR -> ErrorRecoveryStrategy.RETRY_WITH_FALLBACK_CONFIG
                    WhisperErrorCode.MEMORY_ERROR -> ErrorRecoveryStrategy.DEGRADE_FUNCTIONALITY
                    else -> ErrorRecoveryStrategy.RESET_COMPONENT
                }
            }
            
            severity == ErrorSeverity.MEDIUM -> {
                when (code) {
                    WhisperErrorCode.CONFIGURATION_ERROR -> ErrorRecoveryStrategy.RETRY_WITH_FALLBACK_CONFIG
                    else -> ErrorRecoveryStrategy.RETRY_WITH_DELAY
                }
            }
            
            else -> ErrorRecoveryStrategy.RETRY_IMMEDIATE
        }
    }
    
    /**
     * Attempt error recovery based on strategy
     */
    private suspend fun attemptRecovery(error: WhisperError): RecoveryResult {
        val strategy = error.recoveryStrategy ?: return RecoveryResult(false, "No recovery strategy")
        val attemptKey = "${error.code}_${error.category}"
        
        // Check if we've exceeded max recovery attempts
        val attempts = recoveryAttempts.getOrPut(attemptKey) { AtomicInteger(0) }
        if (attempts.get() >= MAX_RECOVERY_ATTEMPTS) {
            return RecoveryResult(false, "Maximum recovery attempts exceeded")
        }
        
        attempts.incrementAndGet()
        
        return try {
            when (strategy) {
                ErrorRecoveryStrategy.RETRY_IMMEDIATE -> {
                    RecoveryResult(true, "Retrying immediately")
                }
                
                ErrorRecoveryStrategy.RETRY_WITH_DELAY -> {
                    val delayMs = RECOVERY_DELAY_BASE_MS * attempts.get()
                    delay(delayMs)
                    RecoveryResult(true, "Retrying after ${delayMs}ms delay")
                }
                
                ErrorRecoveryStrategy.RETRY_WITH_FALLBACK_CONFIG -> {
                    RecoveryResult(true, "Retrying with fallback configuration", "fallback_config")
                }
                
                ErrorRecoveryStrategy.RESET_COMPONENT -> {
                    // Delegate to ErrorRecoveryManager
                    val success = errorRecoveryManager.resetComponent(error.category.name)
                    RecoveryResult(success, if (success) "Component reset successful" else "Component reset failed")
                }
                
                ErrorRecoveryStrategy.RESTART_ENGINE -> {
                    RecoveryResult(true, "Engine restart required", "restart_required")
                }
                
                ErrorRecoveryStrategy.DEGRADE_FUNCTIONALITY -> {
                    RecoveryResult(true, "Functionality degraded to maintain operation", "degraded_mode")
                }
                
                ErrorRecoveryStrategy.NOTIFY_USER -> {
                    RecoveryResult(false, "User intervention required")
                }
                
                ErrorRecoveryStrategy.NO_RECOVERY -> {
                    RecoveryResult(false, "Error is not recoverable")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recovery attempt failed", e)
            RecoveryResult(false, "Recovery failed: ${e.message}")
        }
    }
    
    /**
     * Record error in history and update statistics
     */
    private fun recordError(error: WhisperError) {
        synchronized(errorHistory) {
            errorHistory.add(error)
            
            // Trim history if too large
            if (errorHistory.size > ERROR_HISTORY_SIZE) {
                errorHistory.removeAt(0)
            }
        }
        
        // Update error count statistics
        errorCounts.getOrPut(error.code) { AtomicInteger(0) }.incrementAndGet()
    }
    
    /**
     * Notify error listeners
     */
    private fun notifyErrorListeners(error: WhisperError) {
        try {
            errorListener?.invoke(error.message, error.code)
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying error listener", e)
        }
    }
    
    /**
     * Get error statistics for debugging
     */
    fun getErrorStatistics(): Map<String, Any> {
        return mapOf(
            "totalErrors" to errorHistory.size,
            "errorsByCode" to errorCounts.mapValues { it.value.get() },
            "errorsByCategory" to errorHistory.groupingBy { it.category }.eachCount(),
            "errorsBySeverity" to errorHistory.groupingBy { it.severity }.eachCount(),
            "recentErrors" to errorHistory.takeLast(10).map {
                mapOf(
                    "code" to it.code,
                    "message" to it.message,
                    "severity" to it.severity.name,
                    "category" to it.category.name,
                    "timestamp" to it.timestamp
                )
            }
        )
    }
    
    /**
     * Get recovery statistics
     */
    fun getRecoveryStatistics(): Map<String, Any> {
        return mapOf(
            "recoveryAttempts" to recoveryAttempts.mapValues { it.value.get() },
            "maxAttemptsReached" to recoveryAttempts.count { it.value.get() >= MAX_RECOVERY_ATTEMPTS }
        )
    }
    
    /**
     * Check if error is recoverable
     */
    fun isRecoverable(error: WhisperError): Boolean {
        return error.recoveryStrategy != null && 
               error.recoveryStrategy != ErrorRecoveryStrategy.NO_RECOVERY &&
               error.severity != ErrorSeverity.CRITICAL
    }
    
    /**
     * Get error frequency for a specific code
     */
    fun getErrorFrequency(code: Int): Int {
        return errorCounts[code]?.get() ?: 0
    }
    
    /**
     * Clear error history and reset counters
     */
    fun clearErrorHistory() {
        synchronized(errorHistory) {
            errorHistory.clear()
        }
        errorCounts.clear()
        recoveryAttempts.clear()
        
        Log.i(TAG, "Error history cleared")
    }
    
    /**
     * Set error listener
     */
    fun setErrorListener(listener: OnSpeechErrorListener?) {
        errorListener = listener
    }
    
    /**
     * Set critical error callback
     */
    fun setCriticalErrorCallback(callback: ((WhisperError) -> Unit)?) {
        criticalErrorCallback = callback
    }
    
    /**
     * Generate diagnostic report
     */
    fun generateDiagnosticReport(): String {
        val errorStats = getErrorStatistics()
        val recoveryStats = getRecoveryStatistics()
        
        return """
            Whisper Error Handler Diagnostic Report
            =====================================
            
            Error Summary:
            - Total Errors: ${errorStats["totalErrors"]}
            - Recovery Attempts: ${recoveryStats["recoveryAttempts"]}
            - Max Attempts Reached: ${recoveryStats["maxAttemptsReached"]}
            
            Error Distribution by Severity:
            ${(errorStats["errorsBySeverity"] as? Map<*, *>)?.entries?.joinToString("\n") { "- ${it.key}: ${it.value}" } ?: "No data"}
            
            Error Distribution by Category:
            ${(errorStats["errorsByCategory"] as? Map<*, *>)?.entries?.joinToString("\n") { "- ${it.key}: ${it.value}" } ?: "No data"}
            
            Recent Errors:
            ${(errorStats["recentErrors"] as? List<*>)?.joinToString("\n") { error ->
                val e = error as Map<*, *>
                "- [${e["timestamp"]}] ${e["severity"]}: ${e["message"]} (Code: ${e["code"]})"
            } ?: "No recent errors"}
            
            Generated at: ${System.currentTimeMillis()}
        """.trimIndent()
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        recoveryScope.cancel()
        clearErrorHistory()
        errorListener = null
        criticalErrorCallback = null
        
        Log.i(TAG, "✅ WhisperErrorHandler destroyed")
    }
}