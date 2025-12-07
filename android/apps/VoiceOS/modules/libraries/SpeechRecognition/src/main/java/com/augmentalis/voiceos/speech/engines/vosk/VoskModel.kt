/**
 * VoskModel.kt - Model management for VOSK speech recognition engine
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * SOLID Principle: Single Responsibility
 * - Manages VOSK model loading and validation
 * - Handles model unpacking and asset management
 * - Provides model state tracking and error handling
 */
package com.augmentalis.voiceos.speech.engines.vosk

import android.content.Context
import android.util.Log
import com.augmentalis.voiceos.speech.engines.common.ServiceState
import com.augmentalis.voiceos.speech.engines.common.SpeechModelPathResolver
import kotlinx.coroutines.*
import kotlinx.coroutines.suspendCancellableCoroutine
import org.vosk.Model
import org.vosk.android.StorageService
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume

/**
 * Model manager for VOSK engine.
 * Handles all model-related functionality including loading, validation,
 * and asset management.
 */
class VoskModel(
    private val context: Context,
    private val serviceState: ServiceState
) {
    
    companion object {
        private const val TAG = "VoskModel"
        private const val MODEL_NAME = "model-en-us"
        private const val MODEL_DIRECTORY = "model"
        private const val MODEL_VALIDATION_TIMEOUT = 30000L // 30 seconds
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    // Path resolver for multi-location model search
    private val pathResolver = SpeechModelPathResolver(context, "vosk", MODEL_DIRECTORY)

    // Model state
    @Volatile
    private var voskModel: Model? = null
    @Volatile
    private var isModelLoaded = false
    @Volatile
    private var isModelValidated = false
    @Volatile
    private var modelLoadTime = 0L
    @Volatile
    private var lastError: String? = null
    
    // Model metadata
    private var modelPath: String? = null
    private var modelSize: Long = 0L
    private var modelVersion: String? = null
    private var loadAttempts = 0
    private val loadHistory = mutableListOf<LoadAttempt>()
    
    // Coroutine management
    private val modelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Initialize and load VOSK model asynchronously
     */
    suspend fun initialize(config: VoskConfig): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Initializing VOSK model...")
                serviceState.updateState(ServiceState.State.INITIALIZING, "Loading VOSK model...")
                
                val startTime = System.currentTimeMillis()
                loadAttempts++
                
                // Validate context and configuration
                if (!validateInitializationRequirements(config)) {
                    return@withContext false
                }
                
                // Unpack and load model
                val success = unpackAndLoadModel()
                
                if (success) {
                    modelLoadTime = System.currentTimeMillis() - startTime
                    isModelLoaded = true
                    isModelValidated = validateModel()
                    
                    recordLoadAttempt(true, null, modelLoadTime)
                    
                    Log.i(TAG, "VOSK model loaded successfully in ${modelLoadTime}ms")
                    serviceState.updateState(ServiceState.State.INITIALIZED, "Model loaded successfully")
                    true
                } else {
                    recordLoadAttempt(false, lastError, System.currentTimeMillis() - startTime)
                    Log.e(TAG, "Failed to load VOSK model: $lastError")
                    serviceState.updateState(ServiceState.State.ERROR, "Model loading failed: $lastError")
                    false
                }
                
            } catch (e: Exception) {
                val errorMsg = "Exception during model initialization: ${e.message}"
                lastError = errorMsg
                recordLoadAttempt(false, errorMsg, 0L)
                Log.e(TAG, errorMsg, e)
                serviceState.updateState(ServiceState.State.ERROR, errorMsg)
                false
            }
        }
    }
    
    /**
     * Validate initialization requirements
     */
    private fun validateInitializationRequirements(config: VoskConfig): Boolean {
        try {
            // Check if context is valid
            if (!isContextValid()) {
                lastError = "Invalid context provided"
                return false
            }
            
            // Check if configuration is valid
            if (!config.isValid()) {
                lastError = "Invalid configuration: ${config.getValidationErrors().joinToString()}"
                return false
            }
            
            // Check available storage space
            if (!hasEnoughStorage()) {
                lastError = "Insufficient storage space for model"
                return false
            }
            
            // Check if model files are accessible
            if (!areModelFilesAccessible()) {
                lastError = "Model files are not accessible"
                return false
            }
            
            return true
            
        } catch (e: Exception) {
            lastError = "Validation error: ${e.message}"
            return false
        }
    }
    
    /**
     * Unpack and load the VOSK model
     */
    private suspend fun unpackAndLoadModel(): Boolean {
        return suspendCancellableCoroutine<Boolean> { continuation ->
            try {
                Log.d(TAG, "Starting model unpacking process...")
                
                StorageService.unpack(
                    context,
                    MODEL_NAME,
                    MODEL_DIRECTORY,
                    { model ->
                        Log.i(TAG, "Model unpacked successfully")
                        this@VoskModel.voskModel = model
                        
                        // Extract model metadata
                        extractModelMetadata(model)
                        
                        continuation.resume(true)
                    },
                    { exception ->
                        exception.printStackTrace()
                        val errorMsg = "Failed to unpack model: ${exception.message}"
                        lastError = errorMsg
                        Log.e(TAG, errorMsg, exception)
                        continuation.resume(false)
                    }
                )
            } catch (e: Exception) {
                val errorMsg = "Exception during model unpacking: ${e.message}"
                lastError = errorMsg
                Log.e(TAG, errorMsg, e)
                continuation.resume(false)
            }
        }
    }
    
    /**
     * Validate the loaded model
     */
    private fun validateModel(): Boolean {
        return try {
            val model = voskModel
            if (model == null) {
                lastError = "Model is null after loading"
                return false
            }
            
            // Additional validation could be added here
            // For now, we assume if model loaded without exception, it's valid
            Log.d(TAG, "Model validation successful")
            true
            
        } catch (e: Exception) {
            lastError = "Model validation failed: ${e.message}"
            Log.e(TAG, lastError, e)
            false
        }
    }
    
    /**
     * Extract metadata from loaded model
     */
    private fun extractModelMetadata(@Suppress("UNUSED_PARAMETER") model: Model) {
        try {
            // Extract what we can about the model
            modelPath = calculateModelPath()
            modelSize = calculateModelSize()
            modelVersion = extractModelVersion()
            
            Log.d(TAG, "Model metadata extracted - Path: $modelPath, Size: ${modelSize}KB, Version: $modelVersion")
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract complete model metadata: ${e.message}")
        }
    }
    
    /**
     * Calculate model file path
     */
    private fun calculateModelPath(): String? {
        return try {
            // Use path resolver to find model directory with multi-location fallback
            Log.d(TAG, "Searching for VOSK model in locations:\n${pathResolver.getSearchPathsForLogging()}")

            // Validation function to check if directory is valid VOSK model
            val validationFunction: (File) -> Boolean = { dir ->
                // VOSK model directory should contain specific files
                dir.exists() && dir.isDirectory && dir.listFiles()?.isNotEmpty() == true
            }

            val modelDir = pathResolver.resolveModelPath(validationFunction)

            if (modelDir.exists()) {
                Log.i(TAG, "Found VOSK model at: ${modelDir.absolutePath}")
                modelDir.absolutePath
            } else {
                Log.d(TAG, "VOSK model not found, will use: ${modelDir.absolutePath}")
                modelDir.absolutePath
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to calculate model path: ${e.message}")
            null
        }
    }
    
    /**
     * Calculate model size
     */
    private fun calculateModelSize(): Long {
        return try {
            modelPath?.let { path ->
                val modelDir = File(path)
                if (modelDir.exists() && modelDir.isDirectory) {
                    calculateDirectorySize(modelDir) / 1024 // Return in KB
                } else {
                    0L
                }
            } ?: 0L
        } catch (e: Exception) {
            Log.w(TAG, "Failed to calculate model size: ${e.message}")
            0L
        }
    }
    
    /**
     * Calculate directory size recursively
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        try {
            if (directory.exists()) {
                directory.listFiles()?.forEach { file ->
                    size += if (file.isDirectory) {
                        calculateDirectorySize(file)
                    } else {
                        file.length()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error calculating directory size: ${e.message}")
        }
        return size
    }
    
    /**
     * Extract model version (placeholder - would need actual model file analysis)
     */
    private fun extractModelVersion(): String {
        return "unknown" // Would need to parse model files for actual version
    }
    
    /**
     * Check if context is valid
     */
    private fun isContextValid(): Boolean {
        return try {
            context.applicationContext != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if there's enough storage space
     */
    private fun hasEnoughStorage(): Boolean {
        return try {
            val filesDir = context.getExternalFilesDir(null) ?: context.filesDir
            val freeSpace = filesDir.freeSpace
            val requiredSpace = 100 * 1024 * 1024L // 100MB minimum
            
            freeSpace >= requiredSpace
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check storage space: ${e.message}")
            true // Assume sufficient space if check fails
        }
    }
    
    /**
     * Check if model files are accessible
     */
    private fun areModelFilesAccessible(): Boolean {
        return try {
            // Check if we can access the assets directory
            val assets = context.assets
            assets.list("")?.isNotEmpty() ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check model file accessibility: ${e.message}")
            true // Assume accessible if check fails
        }
    }
    
    /**
     * Record load attempt for diagnostics
     */
    private fun recordLoadAttempt(success: Boolean, error: String?, duration: Long) {
        val attempt = LoadAttempt(
            attemptNumber = loadAttempts,
            success = success,
            error = error,
            duration = duration,
            timestamp = System.currentTimeMillis()
        )
        loadHistory.add(attempt)
        
        // Keep only last 10 attempts
        if (loadHistory.size > 10) {
            loadHistory.removeAt(0)
        }
    }
    
    /**
     * Retry model loading with exponential backoff
     */
    suspend fun retryLoading(config: VoskConfig): Boolean {
        if (loadAttempts >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Maximum retry attempts reached ($MAX_RETRY_ATTEMPTS)")
            return false
        }
        
        // Exponential backoff: wait 2^attempt seconds
        val delayMs = (1L shl loadAttempts) * 1000L
        Log.i(TAG, "Retrying model load in ${delayMs}ms (attempt ${loadAttempts + 1}/$MAX_RETRY_ATTEMPTS)")
        
        delay(delayMs)
        return initialize(config)
    }
    
    /**
     * Clean up model resources
     */
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up VOSK model resources...")
            
            voskModel?.let { model ->
                try {
                    model.close()
                    Log.d(TAG, "VOSK model closed successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing VOSK model: ${e.message}")
                }
            }
            
            // Reset state
            voskModel = null
            isModelLoaded = false
            isModelValidated = false
            lastError = null
            
            // Cancel any pending operations
            modelScope.coroutineContext.cancelChildren()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during model cleanup: ${e.message}")
        }
    }
    
    // Getters for model state and information
    fun getModel(): Model? = voskModel
    fun isLoaded(): Boolean = isModelLoaded && voskModel != null
    fun isValidated(): Boolean = isModelValidated
    fun getLoadTime(): Long = modelLoadTime
    fun getLastError(): String? = lastError
    fun getModelPath(): String? = modelPath
    fun getModelSize(): Long = modelSize
    fun getModelVersion(): String? = modelVersion
    fun getLoadAttempts(): Int = loadAttempts
    fun getLoadHistory(): List<LoadAttempt> = loadHistory.toList()
    
    /**
     * Get model status information
     */
    fun getModelStatus(): ModelStatus {
        return ModelStatus(
            isLoaded = isModelLoaded,
            isValidated = isModelValidated,
            loadTime = modelLoadTime,
            modelPath = modelPath,
            modelSize = modelSize,
            modelVersion = modelVersion ?: "unknown",
            loadAttempts = loadAttempts,
            lastError = lastError
        )
    }
    
    /**
     * Get diagnostic information
     */
    fun getDiagnostics(): Map<String, Any> {
        return mapOf(
            "modelLoaded" to isModelLoaded,
            "modelValidated" to isModelValidated,
            "loadTime" to modelLoadTime,
            "modelPath" to (modelPath ?: "unknown"),
            "modelSizeKB" to modelSize,
            "modelVersion" to (modelVersion ?: "unknown"),
            "loadAttempts" to loadAttempts,
            "maxRetryAttempts" to MAX_RETRY_ATTEMPTS,
            "lastError" to (lastError ?: "none"),
            "loadHistorySize" to loadHistory.size,
            "hasModel" to (voskModel != null)
        )
    }
    
    /**
     * Data class for load attempt tracking
     */
    data class LoadAttempt(
        val attemptNumber: Int,
        val success: Boolean,
        val error: String?,
        val duration: Long,
        val timestamp: Long
    )
    
    /**
     * Data class for model status
     */
    data class ModelStatus(
        val isLoaded: Boolean,
        val isValidated: Boolean,
        val loadTime: Long,
        val modelPath: String?,
        val modelSize: Long,
        val modelVersion: String,
        val loadAttempts: Int,
        val lastError: String?
    )
}