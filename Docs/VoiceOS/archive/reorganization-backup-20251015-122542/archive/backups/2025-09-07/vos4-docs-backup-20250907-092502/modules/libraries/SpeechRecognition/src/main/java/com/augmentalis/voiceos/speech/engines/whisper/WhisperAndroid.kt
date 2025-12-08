/**
 * WhisperAndroid.kt - Wrapper for Whisper Android library
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC  
 * Author: Manoj Jhawar
 * Created: 2025-08-31
 * 
 * Provides interface to the whisper-android library using ggerganov's implementation
 */
package com.augmentalis.voiceos.speech.engines.whisper

import android.content.Context
import android.util.Log
import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Whisper Android wrapper
 * Interfaces with the native Whisper library through JNI
 */
class WhisperAndroid(private val context: Context) {
    
    companion object {
        private const val TAG = "WhisperAndroid"
        
        init {
            try {
                System.loadLibrary("whisper")
                Log.d(TAG, "Whisper native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load Whisper native library", e)
            }
        }
        
        // Check if library is available
        @JvmStatic
        fun isAvailable(): Boolean {
            return try {
                // Try to call a simple native method to verify library is loaded
                getWhisperVersion()
                true
            } catch (e: Throwable) {
                Log.w(TAG, "Whisper native library not available: ${e.message}")
                false
            }
        }
        
        // Native methods
        @JvmStatic
        private external fun getWhisperVersion(): String
        
        @JvmStatic
        private external fun initContext(modelPath: String): Long
        
        @JvmStatic
        private external fun freeContext(contextPtr: Long)
        
        @JvmStatic
        private external fun transcribe(
            contextPtr: Long,
            audioData: FloatArray,
            audioLength: Int,
            language: String?,
            translate: Boolean,
            maxTokens: Int,
            temperature: Float,
            speedUp: Boolean,
            threads: Int,
            maxContext: Int,
            singleSegment: Boolean,
            printSpecial: Boolean,
            printProgress: Boolean,
            printRealtime: Boolean,
            printTimestamps: Boolean,
            tokenTimestamps: Boolean,
            offsetMs: Int,
            duration: Int,
            wordThreshold: Float,
            initialPrompt: String?
        ): String
        
        @JvmStatic
        private external fun getSegments(contextPtr: Long): Array<Segment>
        
        @JvmStatic
        private external fun benchmarkModel(contextPtr: Long, threads: Int): Long
    }
    
    // Whisper context pointer
    private var contextPtr: Long = 0L
    private var isInitialized = false
    private var currentModelPath: String? = null
    
    /**
     * Load a Whisper model (synchronous version for WhisperEngine)
     */
    fun loadModel(modelPath: String): Boolean {
        return try {
            // Check if model file exists
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file not found: $modelPath")
                return false
            }
            
            // Free existing context if any
            if (contextPtr != 0L) {
                freeContext(contextPtr)
            }
            
            // Initialize new context
            contextPtr = initContext(modelPath)
            
            if (contextPtr != 0L) {
                isInitialized = true
                currentModelPath = modelPath
                Log.i(TAG, "✅ Whisper initialized with model: $modelPath")
                true
            } else {
                Log.e(TAG, "Failed to initialize Whisper context")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading Whisper model", e)
            false
        }
    }
    
    /**
     * Initialize Whisper with a model (async version)
     */
    suspend fun initModel(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if model file exists
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file not found: $modelPath")
                return@withContext false
            }
            
            // Free existing context if any
            if (contextPtr != 0L) {
                freeContext(contextPtr)
            }
            
            // Initialize new context
            contextPtr = initContext(modelPath)
            
            if (contextPtr != 0L) {
                isInitialized = true
                currentModelPath = modelPath
                Log.i(TAG, "✅ Whisper initialized with model: $modelPath")
                true
            } else {
                Log.e(TAG, "Failed to initialize Whisper context")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Whisper", e)
            false
        }
    }
    
    /**
     * Transcribe audio data
     */
    suspend fun transcribe(
        audioData: FloatArray,
        config: TranscriptionConfig = TranscriptionConfig()
    ): TranscriptionResult? = withContext(Dispatchers.IO) {
        
        if (!isInitialized || contextPtr == 0L) {
            Log.e(TAG, "Whisper not initialized")
            return@withContext null
        }
        
        try {
            val startTime = System.currentTimeMillis()
            
            // Call native transcribe
            val text = transcribe(
                contextPtr = contextPtr,
                audioData = audioData,
                audioLength = audioData.size,
                language = config.language,
                translate = config.translate,
                maxTokens = config.maxTokens,
                temperature = config.temperature,
                speedUp = config.speedUp,
                threads = config.threads,
                maxContext = config.maxContext,
                singleSegment = config.singleSegment,
                printSpecial = false,
                printProgress = false,
                printRealtime = false,
                printTimestamps = false,
                tokenTimestamps = config.tokenTimestamps,
                offsetMs = config.offsetMs,
                duration = config.duration,
                wordThreshold = config.wordThreshold,
                initialPrompt = config.initialPrompt
            )
            
            // Get segments if needed
            val segments = if (config.returnSegments) {
                try {
                    getSegments(contextPtr).toList()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get segments", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            
            TranscriptionResult(
                text = text,
                segments = segments,
                language = config.language ?: detectLanguage(text),
                processingTimeMs = processingTime
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            null
        }
    }
    
    /**
     * Simple language detection based on text
     */
    private fun detectLanguage(text: String): String {
        // Basic language detection - in production, use proper detection
        return when {
            text.matches(Regex("[\\u4e00-\\u9fff]+")) -> "zh" // Chinese
            text.matches(Regex("[\\u3040-\\u309f\\u30a0-\\u30ff]+")) -> "ja" // Japanese
            text.matches(Regex("[\\u0600-\\u06ff]+")) -> "ar" // Arabic
            text.matches(Regex("[\\u0400-\\u04ff]+")) -> "ru" // Russian
            else -> "en" // Default to English
        }
    }
    
    /**
     * Benchmark the model
     */
    suspend fun benchmark(threads: Int = 4): Long = withContext(Dispatchers.IO) {
        if (!isInitialized || contextPtr == 0L) {
            Log.e(TAG, "Whisper not initialized")
            return@withContext -1L
        }
        
        try {
            benchmarkModel(contextPtr, threads)
        } catch (e: Exception) {
            Log.e(TAG, "Benchmark failed", e)
            -1L
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        if (contextPtr != 0L) {
            try {
                freeContext(contextPtr)
                Log.d(TAG, "Whisper context released")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing Whisper context", e)
            } finally {
                contextPtr = 0L
                isInitialized = false
                currentModelPath = null
            }
        }
    }
    
    /**
     * Check if initialized
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Get current model path
     */
    fun getCurrentModelPath(): String? = currentModelPath
}

/**
 * Transcription configuration
 */
data class TranscriptionConfig(
    val language: String? = null,           // Target language (null for auto-detect)
    val translate: Boolean = false,         // Translate to English
    val maxTokens: Int = 0,                // Max tokens (0 for default)
    val temperature: Float = 0.0f,         // Sampling temperature
    val speedUp: Boolean = false,          // Speed up processing
    val threads: Int = 4,                   // Number of threads
    val maxContext: Int = -1,              // Max context (-1 for default)
    val singleSegment: Boolean = false,    // Force single segment
    val tokenTimestamps: Boolean = false,  // Token-level timestamps
    val offsetMs: Int = 0,                 // Time offset in ms
    val duration: Int = 0,                 // Duration to process (0 for all)
    val wordThreshold: Float = 0.01f,      // Word probability threshold
    val initialPrompt: String? = null,     // Initial prompt for context
    val returnSegments: Boolean = true     // Return detailed segments
)

/**
 * Transcription result
 */
data class TranscriptionResult(
    val text: String,
    val segments: List<Segment> = emptyList(),
    val language: String = "en",
    val processingTimeMs: Long = 0
)

/**
 * Audio segment with timing
 */
data class Segment(
    val text: String,
    val startTime: Long,  // Start time in milliseconds
    val endTime: Long,    // End time in milliseconds
    val tokens: IntArray = intArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Segment

        if (text != other.text) return false
        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (!tokens.contentEquals(other.tokens)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + tokens.contentHashCode()
        return result
    }
}

/**
 * Convert PCM16 byte array to float array for Whisper
 */
fun convertPCM16ToFloat(pcmData: ByteArray): FloatArray {
    val samples = pcmData.size / 2
    val floatArray = FloatArray(samples)
    val byteBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)
    
    for (i in 0 until samples) {
        val sample = byteBuffer.getShort(i * 2)
        floatArray[i] = sample.toFloat() / 32768.0f
    }
    
    return floatArray
}

/**
 * Resample audio from one sample rate to another
 */
fun resampleAudio(input: FloatArray, fromRate: Int, toRate: Int): FloatArray {
    if (fromRate == toRate) return input
    
    val ratio = toRate.toFloat() / fromRate.toFloat()
    val outputSize = (input.size * ratio).toInt()
    val output = FloatArray(outputSize)
    
    for (i in output.indices) {
        val srcIndex = i / ratio
        val srcIndexInt = srcIndex.toInt()
        val fraction = srcIndex - srcIndexInt
        
        output[i] = if (srcIndexInt < input.size - 1) {
            // Linear interpolation
            input[srcIndexInt] * (1 - fraction) + input[srcIndexInt + 1] * fraction
        } else {
            input.last()
        }
    }
    
    return output
}
