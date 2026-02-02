/**
 * WhisperNative.kt - Native integration component for Whisper engine
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * Handles native Whisper library integration, inference calls, and result processing.
 * Integrates with whisper-cpp bindings for high-performance speech recognition.
 */
package com.augmentalis.voiceos.speech.engines.whisper

import android.content.Context
import android.util.Log
import com.augmentalis.voiceos.speech.api.WordTimestamp
import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Whisper recognition result from native layer
 */
data class WhisperResult(
    val text: String,
    val confidence: Float,
    val language: String,
    val segments: List<WhisperSegment>,
    val translation: String? = null
)

/**
 * Whisper segment with timing information
 */
data class WhisperSegment(
    val text: String,
    val startTime: Float,
    val endTime: Float,
    val confidence: Float,
    val words: List<WordTimestamp>
)

/**
 * Native inference parameters
 */
data class WhisperInferenceParams(
    val temperature: Float = 0.0f,
    val temperatureInc: Float = 0.2f,
    val beamSize: Int = 5,
    val bestOf: Int = 5,
    val maxSegmentLength: Int = 30,
    val compressionRatioThreshold: Float = 2.4f,
    val logprobThreshold: Float = -1.0f,
    val noSpeechThreshold: Float = 0.6f,
    val enableWordTimestamps: Boolean = true,
    val enableLanguageDetection: Boolean = true,
    val enableTranslation: Boolean = false,
    val targetLanguage: String = "en",
    val nThreads: Int = 4
)

/**
 * Manages native Whisper library integration and inference operations.
 * Provides high-level interface to whisper-cpp native bindings.
 */
class WhisperNative(
    private val context: Context,
    private val performanceMonitor: PerformanceMonitor
) {
    
    companion object {
        private const val TAG = "WhisperNative"
        private const val INFERENCE_TIMEOUT_MS = 30000L // 30 seconds
        private const val MIN_AUDIO_LENGTH_MS = 100L // Minimum audio length
        private const val MAX_AUDIO_LENGTH_MS = 30000L // Maximum audio length (30s)
        
        /**
         * Check if Whisper native library is available
         */
        fun isAvailable(): Boolean {
            return try {
                // Check if Whisper native library is available
                // This would typically check if the native library can be loaded
                // For now, we'll return a placeholder value
                System.loadLibrary("whisper") // This will throw if library not found
                true
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Whisper native library not found: ${e.message}")
                false
            } catch (e: Exception) {
                Log.w(TAG, "WhisperNative library check failed: ${e.message}")
                false
            }
        }
    }
    
    // Native library state
    private val isInitialized = AtomicBoolean(false)
    private val inferenceInProgress = AtomicBoolean(false)
    private val inferenceMutex = Mutex()
    
    // Whisper context and model
    private var whisperContext: Long = 0L // Native context pointer
    private var currentModelPath: String? = null
    
    // Default inference parameters
    private var defaultParams = WhisperInferenceParams()
    
    // Callbacks
    private var onInferenceStarted: (() -> Unit)? = null
    private var onInferenceCompleted: ((WhisperResult) -> Unit)? = null
    private var onInferenceError: ((String, Throwable?) -> Unit)? = null
    
    /**
     * Initialize native Whisper integration
     */
    suspend fun initialize(): Boolean {
        return try {
            if (isInitialized.get()) {
                Log.d(TAG, "WhisperNative already initialized")
                return true
            }
            
            // Check if native library is available
            if (!isAvailable()) {
                Log.e(TAG, "Whisper native library not available")
                return false
            }
            
            // Initialize native library
            val success = initializeNative()
            
            if (success) {
                isInitialized.set(true)
                Log.i(TAG, "✅ WhisperNative initialized successfully")
            } else {
                Log.e(TAG, "❌ WhisperNative initialization failed")
            }
            
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing WhisperNative", e)
            false
        }
    }
    
    /**
     * Load model into native context
     */
    suspend fun loadModel(modelPath: String): Boolean = inferenceMutex.withLock {
        return try {
            if (!isInitialized.get()) {
                Log.e(TAG, "WhisperNative not initialized")
                return false
            }
            
            // Release existing context if any
            if (whisperContext != 0L) {
                releaseContext()
            }
            
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "Loading Whisper model into native context: $modelPath")
            
            // Load model and create context
            whisperContext = loadModelNative(modelPath)
            
            if (whisperContext != 0L) {
                currentModelPath = modelPath
                val duration = System.currentTimeMillis() - startTime
                performanceMonitor.recordSlowOperation("model_loading", duration, 5000L)
                Log.i(TAG, "✅ Model loaded into native context in ${duration}ms")
                true
            } else {
                Log.e(TAG, "❌ Failed to load model into native context")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model into native context", e)
            false
        }
    }
    
    /**
     * Run inference on audio data
     */
    suspend fun runInference(
        audioData: FloatArray, 
        params: WhisperInferenceParams = defaultParams
    ): WhisperResult? = inferenceMutex.withLock {
        
        if (!isInitialized.get() || whisperContext == 0L) {
            Log.e(TAG, "WhisperNative not ready for inference")
            onInferenceError?.invoke("Native context not ready", null)
            return null
        }
        
        if (inferenceInProgress.get()) {
            Log.w(TAG, "Inference already in progress")
            return null
        }
        
        return try {
            val startTime = System.currentTimeMillis()
            inferenceInProgress.set(true)
            onInferenceStarted?.invoke()
            
            // Validate audio data
            if (!validateAudioData(audioData)) {
                onInferenceError?.invoke("Invalid audio data", null)
                return null
            }
            
            Log.d(TAG, "Running Whisper inference on ${audioData.size} samples")
            
            // Run inference with timeout
            val result = withTimeout(INFERENCE_TIMEOUT_MS) {
                runInferenceNative(whisperContext, audioData, params)
            }
            
            val duration = System.currentTimeMillis() - startTime
            performanceMonitor.recordRecognition(startTime, result != null, result?.text)
            
            if (result != null) {
                Log.d(TAG, "✅ Inference completed in ${duration}ms: '${result.text}' (confidence: ${result.confidence})")
                onInferenceCompleted?.invoke(result)
            } else {
                Log.w(TAG, "❌ Inference returned null result in ${duration}ms")
                onInferenceError?.invoke("Inference returned no result", null)
            }
            
            result
            
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Inference timed out after ${INFERENCE_TIMEOUT_MS}ms")
            onInferenceError?.invoke("Inference timeout", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error during inference", e)
            onInferenceError?.invoke("Inference failed: ${e.message}", e)
            null
        } finally {
            inferenceInProgress.set(false)
        }
    }
    
    /**
     * Validate audio data before inference
     */
    private fun validateAudioData(audioData: FloatArray): Boolean {
        // Check for null or empty data
        if (audioData.isEmpty()) {
            Log.e(TAG, "Audio data is empty")
            return false
        }
        
        // Check minimum length (assuming 16kHz sample rate)
        val durationMs = (audioData.size * 1000) / 16000
        if (durationMs < MIN_AUDIO_LENGTH_MS) {
            Log.e(TAG, "Audio too short: ${durationMs}ms (minimum: ${MIN_AUDIO_LENGTH_MS}ms)")
            return false
        }
        
        // Check maximum length
        if (durationMs > MAX_AUDIO_LENGTH_MS) {
            Log.w(TAG, "Audio very long: ${durationMs}ms (maximum recommended: ${MAX_AUDIO_LENGTH_MS}ms)")
            // Don't fail, just warn - long audio might still be processed
        }
        
        // Check for valid sample range
        var validSamples = 0
        var zeroSamples = 0
        
        for (sample in audioData) {
            if (sample.isNaN() || sample.isInfinite()) {
                Log.e(TAG, "Invalid audio sample found: $sample")
                return false
            }
            
            if (sample == 0f) {
                zeroSamples++
            } else if (sample >= -1f && sample <= 1f) {
                validSamples++
            }
        }
        
        // Check if audio is all silent
        if (zeroSamples == audioData.size) {
            Log.w(TAG, "Audio appears to be silent")
            return false
        }
        
        // Check for reasonable signal
        val validPercentage = (validSamples.toFloat() / audioData.size.toFloat()) * 100f
        if (validPercentage < 50f) {
            Log.w(TAG, "Low percentage of valid audio samples: ${validPercentage}%")
        }
        
        Log.d(TAG, "Audio validation passed: ${audioData.size} samples, ${durationMs}ms duration")
        return true
    }
    
    /**
     * Update inference parameters
     */
    fun setInferenceParams(params: WhisperInferenceParams) {
        defaultParams = params
        Log.d(TAG, "Inference parameters updated: $params")
    }
    
    /**
     * Get current inference parameters
     */
    fun getInferenceParams(): WhisperInferenceParams = defaultParams
    
    /**
     * Check if inference is currently running
     */
    fun isInferenceInProgress(): Boolean = inferenceInProgress.get()
    
    /**
     * Get native context information
     */
    fun getNativeInfo(): Map<String, Any> {
        return mapOf(
            "isInitialized" to isInitialized.get(),
            "hasContext" to (whisperContext != 0L),
            "currentModel" to (currentModelPath ?: "none"),
            "inferenceInProgress" to inferenceInProgress.get(),
            "isAvailable" to isAvailable()
        )
    }
    
    /**
     * Set callbacks for inference events
     */
    fun setCallbacks(
        onStarted: (() -> Unit)? = null,
        onCompleted: ((WhisperResult) -> Unit)? = null,
        onError: ((String, Throwable?) -> Unit)? = null
    ) {
        onInferenceStarted = onStarted
        onInferenceCompleted = onCompleted
        onInferenceError = onError
    }
    
    /**
     * Release native context
     */
    private fun releaseContext() {
        if (whisperContext != 0L) {
            try {
                releaseContextNative(whisperContext)
                whisperContext = 0L
                currentModelPath = null
                Log.d(TAG, "Native context released")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing native context", e)
            }
        }
    }
    
    /**
     * Destroy and release all resources
     */
    suspend fun destroy() {
        try {
            // Cancel any ongoing inference
            inferenceInProgress.set(false)
            
            // Release native context
            releaseContext()
            
            // Clear callbacks
            onInferenceStarted = null
            onInferenceCompleted = null
            onInferenceError = null
            
            isInitialized.set(false)
            
            Log.i(TAG, "✅ WhisperNative destroyed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during WhisperNative destroy", e)
        }
    }
    
    // ===== NATIVE METHOD STUBS =====
    // These would be implemented in the actual native layer
    
    /**
     * Initialize native Whisper library
     */
    private fun initializeNative(): Boolean {
        return try {
            // In real implementation, this would call JNI method
            // For now, delegate to companion object isAvailable()
            isAvailable()
        } catch (e: Exception) {
            Log.e(TAG, "Native initialization failed", e)
            false
        }
    }
    
    /**
     * Load model into native context (native method stub)
     */
    private fun loadModelNative(@Suppress("UNUSED_PARAMETER") modelPath: String): Long {
        return try {
            // In real implementation, this would be a native method
            // that returns a pointer to the whisper context
            
            // For now, simulate success/failure based on availability
            if (isAvailable()) {
                System.currentTimeMillis() // Return fake pointer (non-zero)
            } else {
                0L // Return null pointer
            }
        } catch (e: Exception) {
            Log.e(TAG, "Native model loading failed", e)
            0L
        }
    }
    
    /**
     * Run inference on native layer (native method stub)
     */
    private suspend fun runInferenceNative(
        @Suppress("UNUSED_PARAMETER") contextPtr: Long, 
        @Suppress("UNUSED_PARAMETER") audioData: FloatArray, 
        @Suppress("UNUSED_PARAMETER") params: WhisperInferenceParams
    ): WhisperResult? = withContext(Dispatchers.Default) {
        
        return@withContext try {
            // In real implementation, this would be a native method
            // For now, return null to indicate incomplete implementation
            
            if (isAvailable()) {
                // This is where the actual whisper-cpp inference would happen
                Log.w(TAG, "Native inference not fully implemented - returning null")
                null
            } else {
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Native inference failed", e)
            null
        }
    }
    
    /**
     * Release native context (native method stub)
     */
    private fun releaseContextNative(context: Long) {
        try {
            // In real implementation, this would be a native method
            // that releases the whisper context
            Log.d(TAG, "Released native context: $context")
        } catch (e: Exception) {
            Log.e(TAG, "Native context release failed", e)
        }
    }
}