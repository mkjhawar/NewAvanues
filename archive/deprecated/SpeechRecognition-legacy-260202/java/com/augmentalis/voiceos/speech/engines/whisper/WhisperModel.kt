/**
 * WhisperModel.kt - Model management component for Whisper engine
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * Handles model loading, management, and configuration for Whisper speech recognition.
 * Separated from monolithic WhisperEngine for better maintainability and testability.
 */
package com.augmentalis.voiceos.speech.engines.whisper

import android.content.Context
import android.util.Log
import com.augmentalis.voiceos.speech.engines.whisper.WhisperNative
import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Whisper model sizes with their capabilities
 */
enum class WhisperModelSize(
    val modelName: String,
    val approximateSize: String,
    val languages: Int,
    val speedMultiplier: Float,
    val memoryUsageMB: Int
) {
    TINY("tiny", "39 MB", 99, 32f, 150),
    BASE("base", "74 MB", 99, 16f, 230),
    SMALL("small", "244 MB", 99, 6f, 500),
    MEDIUM("medium", "769 MB", 99, 2f, 1200),
    LARGE("large", "1550 MB", 99, 1f, 2500);
    
    companion object {
        fun fromString(name: String): WhisperModelSize {
            return values().find { it.modelName.equals(name, ignoreCase = true) } ?: BASE
        }
    }
}

/**
 * Model loading states
 */
sealed class ModelState {
    object Unloaded : ModelState()
    object Loading : ModelState()
    object Loaded : ModelState()
    data class Error(val message: String, val exception: Throwable? = null) : ModelState()
}

/**
 * Manages Whisper model loading, validation, and lifecycle.
 * Handles model downloads, path management, and native library integration.
 */
class WhisperModel(
    private val context: Context,
    private val performanceMonitor: PerformanceMonitor
) {
    
    companion object {
        private const val TAG = "WhisperModel"
        private const val MODEL_LOAD_TIMEOUT_MS = 30000L // 30 seconds
    }
    
    // Model management
    private lateinit var modelManager: WhisperModelManager
    private lateinit var whisperAndroid: WhisperAndroid
    private val modelMutex = Mutex()
    
    // State tracking
    @Volatile private var currentState: ModelState = ModelState.Unloaded
    @Volatile private var currentModelSize: WhisperModelSize? = null
    @Volatile private var currentModelPath: String? = null
    @Volatile private var isModelLoaded = false
    
    // Callbacks
    private var onStateChanged: ((ModelState) -> Unit)? = null
    private var onModelLoaded: ((WhisperModelSize, String) -> Unit)? = null
    private var onModelError: ((String, Throwable?) -> Unit)? = null
    
    /**
     * Initialize model management system
     */
    suspend fun initialize(): Boolean {
        return try {
            modelManager = WhisperModelManager(context)
            whisperAndroid = WhisperAndroid(context)
            
            // Check if native library is available
            if (!WhisperAndroid.isAvailable()) {
                val error = "Whisper native library not available"
                Log.e(TAG, error)
                updateState(ModelState.Error(error))
                return false
            }
            
            Log.d(TAG, "✅ Whisper model management initialized")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model management", e)
            updateState(ModelState.Error("Initialization failed: ${e.message}", e))
            false
        }
    }
    
    /**
     * Load specified Whisper model
     */
    suspend fun loadModel(modelSize: WhisperModelSize): Boolean = modelMutex.withLock {
        val startTime = System.currentTimeMillis()
        
        try {
            updateState(ModelState.Loading)
            Log.i(TAG, "Loading Whisper model: ${modelSize.modelName}")
            
            // First check if model is already downloaded
            var modelPath = modelManager.getModelPath(modelSize)
            
            if (modelPath == null) {
                // Handle model download
                modelPath = downloadModelIfNeeded(modelSize)
                if (modelPath == null) {
                    val error = "Failed to obtain model path after download"
                    updateState(ModelState.Error(error))
                    return false
                }
            }
            
            // Validate model file
            if (!validateModelFile(modelPath)) {
                val error = "Model file validation failed: $modelPath"
                updateState(ModelState.Error(error))
                return false
            }
            
            // Load into native library
            val success = loadModelNative(modelPath)
            
            if (success) {
                currentModelSize = modelSize
                currentModelPath = modelPath
                isModelLoaded = true
                updateState(ModelState.Loaded)
                
                // Record performance metrics
                val duration = System.currentTimeMillis() - startTime
                performanceMonitor.recordSlowOperation("model_loading", duration, 10000L) // 10s threshold
                
                onModelLoaded?.invoke(modelSize, modelPath)
                Log.i(TAG, "✅ Model loaded: ${modelSize.modelName} (${modelSize.approximateSize}) in ${duration}ms")
                true
            } else {
                val error = "Native model loading failed"
                updateState(ModelState.Error(error))
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${modelSize.modelName}", e)
            updateState(ModelState.Error("Model loading failed: ${e.message}", e))
            false
        }
    }
    
    /**
     * Download model if needed with fallback handling
     */
    private suspend fun downloadModelIfNeeded(modelSize: WhisperModelSize): String? {
        return try {
            // Check for device-specific recommendations
            val recommendedModel = modelManager.getRecommendedModel()
            val actualModelSize = if (recommendedModel != modelSize) {
                Log.w(TAG, "Model $modelSize not supported on this device, using recommended: $recommendedModel")
                recommendedModel
            } else {
                modelSize
            }
            
            // Check storage space
            if (!modelManager.hasEnoughSpace(actualModelSize)) {
                throw Exception("Insufficient storage space for model ${actualModelSize.modelName}")
            }
            
            Log.i(TAG, "Downloading Whisper model: ${actualModelSize.modelName}")
            
            // Start download
            modelManager.downloadModel(actualModelSize)
            
            // Wait for download completion with timeout
            var downloadComplete = false
            
            withTimeout(MODEL_LOAD_TIMEOUT_MS * 2) { // Allow extra time for download
                modelManager.downloadState.collect { state ->
                    when (state) {
                        is ModelDownloadState.Completed -> {
                            downloadComplete = true
                        }
                        is ModelDownloadState.Error -> {
                            throw Exception("Model download failed: ${state.message}")
                        }
                        is ModelDownloadState.Downloading -> {
                            Log.d(TAG, "Downloading: ${state.progress}% (${state.downloadedMB}MB / ${state.totalMB}MB)")
                        }
                        else -> {}
                    }
                    if (downloadComplete) return@collect
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Model download failed", e)
            onModelError?.invoke("Download failed: ${e.message}", e)
            null
        }
    }
    
    /**
     * Validate model file integrity
     */
    private fun validateModelFile(modelPath: String): Boolean {
        return try {
            val modelFile = File(modelPath)
            
            // Check file exists
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file does not exist: $modelPath")
                return false
            }
            
            // Check file is not empty
            if (modelFile.length() == 0L) {
                Log.e(TAG, "Model file is empty: $modelPath")
                return false
            }
            
            // Check minimum file size (models should be at least 1MB)
            if (modelFile.length() < 1024 * 1024) {
                Log.e(TAG, "Model file too small: $modelPath (${modelFile.length()} bytes)")
                return false
            }
            
            // Check file is readable
            if (!modelFile.canRead()) {
                Log.e(TAG, "Cannot read model file: $modelPath")
                return false
            }
            
            Log.d(TAG, "Model file validation passed: $modelPath (${modelFile.length()} bytes)")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Model file validation error", e)
            false
        }
    }
    
    /**
     * Load model into native Whisper context
     */
    private suspend fun loadModelNative(modelPath: String): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            Log.d(TAG, "Loading model into native context: $modelPath")
            
            // Check if WhisperNative is available
            if (!WhisperNative.isAvailable()) {
                Log.e(TAG, "WhisperNative library not available")
                return@withContext false
            }
            
            // Load through WhisperAndroid with timeout
            val loaded = withTimeout(MODEL_LOAD_TIMEOUT_MS) {
                whisperAndroid.loadModel(modelPath)
            }
            
            if (loaded) {
                Log.i(TAG, "✅ Native model loaded successfully")
            } else {
                Log.e(TAG, "❌ Failed to load model in native library")
            }
            
            loaded
            
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Model loading timed out after ${MODEL_LOAD_TIMEOUT_MS}ms")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model natively", e)
            false
        }
    }
    
    /**
     * Change to a different model size
     */
    suspend fun changeModel(newModelSize: WhisperModelSize): Boolean {
        if (currentModelSize == newModelSize && isModelLoaded) {
            Log.d(TAG, "Model ${newModelSize.modelName} already loaded")
            return true
        }
        
        // Unload current model first
        unloadModel()
        
        // Load new model
        return loadModel(newModelSize)
    }
    
    /**
     * Unload current model
     */
    suspend fun unloadModel(): Boolean = modelMutex.withLock {
        return try {
            if (!isModelLoaded) {
                Log.d(TAG, "No model to unload")
                return true
            }
            
            // Release native resources
            if (::whisperAndroid.isInitialized) {
                whisperAndroid.release()
            }
            
            // Reset state
            currentModelSize = null
            currentModelPath = null
            isModelLoaded = false
            updateState(ModelState.Unloaded)
            
            Log.i(TAG, "✅ Model unloaded successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading model", e)
            false
        }
    }
    
    /**
     * Get current model information
     */
    fun getCurrentModel(): WhisperModelSize? = currentModelSize
    
    /**
     * Get current model path
     */
    fun getCurrentModelPath(): String? = currentModelPath
    
    /**
     * Check if model is loaded and ready
     */
    fun isLoaded(): Boolean = isModelLoaded && currentState is ModelState.Loaded
    
    /**
     * Get current model state
     */
    fun getState(): ModelState = currentState
    
    /**
     * Get model memory usage estimate
     */
    fun getEstimatedMemoryUsage(): Int {
        return currentModelSize?.memoryUsageMB ?: 0
    }
    
    /**
     * Get model performance characteristics
     */
    fun getModelStats(): Map<String, Any> {
        return mapOf(
            "modelSize" to (currentModelSize?.modelName ?: "none"),
            "modelPath" to (currentModelPath ?: "none"),
            "isLoaded" to isModelLoaded,
            "state" to currentState.javaClass.simpleName,
            "estimatedMemoryMB" to getEstimatedMemoryUsage(),
            "languages" to (currentModelSize?.languages ?: 0),
            "speedMultiplier" to (currentModelSize?.speedMultiplier ?: 0f)
        )
    }
    
    /**
     * Set model state change callback
     */
    fun setOnStateChanged(callback: (ModelState) -> Unit) {
        onStateChanged = callback
    }
    
    /**
     * Set model loaded callback
     */
    fun setOnModelLoaded(callback: (WhisperModelSize, String) -> Unit) {
        onModelLoaded = callback
    }
    
    /**
     * Set error callback
     */
    fun setOnModelError(callback: (String, Throwable?) -> Unit) {
        onModelError = callback
    }
    
    /**
     * Update model state and notify observers
     */
    private fun updateState(newState: ModelState) {
        currentState = newState
        onStateChanged?.invoke(newState)
        
        // Log state changes
        when (newState) {
            is ModelState.Loaded -> Log.i(TAG, "Model state: LOADED")
            is ModelState.Loading -> Log.i(TAG, "Model state: LOADING")
            is ModelState.Unloaded -> Log.i(TAG, "Model state: UNLOADED")
            is ModelState.Error -> {
                Log.e(TAG, "Model state: ERROR - ${newState.message}")
                onModelError?.invoke(newState.message, newState.exception)
            }
        }
    }
    
    /**
     * Release all resources
     */
    suspend fun destroy() {
        try {
            unloadModel()
            
            if (::modelManager.isInitialized) {
                // Cancel any ongoing downloads
                // modelManager.cancelDownload() // If such method exists
            }
            
            onStateChanged = null
            onModelLoaded = null
            onModelError = null
            
            Log.i(TAG, "✅ WhisperModel destroyed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during WhisperModel destroy", e)
        }
    }
}