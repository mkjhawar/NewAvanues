/**
 * SpeechError.kt - Consolidated error codes for all speech recognition engines
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-04
 * 
 * Consolidated from duplicate SpeechError declarations across Vivoka engine files
 * to eliminate code duplication and provide a single source of truth for error codes
 */
package com.augmentalis.voiceos.speech.engines.common

/**
 * Central error code definitions for all speech recognition engines.
 * 
 * Error code ranges:
 * - 1001-1099: General initialization and configuration errors
 * - 1101-1199: Recognition and processing errors  
 * - 1201-1299: Model and asset loading errors
 * - 1301-1399: Audio pipeline errors
 * - 1401-1499: Memory and resource errors
 * - 1501-1599: Network and connectivity errors
 */
object SpeechErrorCodes {
    // General initialization and configuration errors (1001-1099)
    const val INITIALIZATION_ERROR = 1001
    const val CONFIGURATION_ERROR = 1002
    const val SERVICE_UNAVAILABLE = 1003
    
    // Recognition and processing errors (1101-1199) 
    const val RECOGNITION_ERROR = 1101
    const val PROCESSING_ERROR = 1102
    const val TIMEOUT_ERROR = 1103
    
    // Model and asset loading errors (1201-1299)
    const val MODEL_LOADING_ERROR = 1201
    const val ASSET_LOADING_ERROR = 1202
    const val MODEL_COMPILATION_ERROR = 1203
    const val ASSET_CORRUPTION_ERROR = 1204
    
    // Audio pipeline errors (1301-1399)
    const val AUDIO_PIPELINE_ERROR = 1301
    const val MICROPHONE_ERROR = 1302
    const val AUDIO_RECORDING_ERROR = 1303
    const val AUDIO_FORMAT_ERROR = 1304
    
    // Memory and resource errors (1401-1499)
    const val MEMORY_ERROR = 1005
    const val STORAGE_ERROR = 1402
    const val RESOURCE_EXHAUSTED = 1403
    
    // Network and connectivity errors (1501-1599)
    const val NETWORK_ERROR = 1006
    const val CONNECTION_ERROR = 1502
    const val AUTHENTICATION_ERROR = 1503
    const val API_ERROR = 1504
    
    /**
     * Get human-readable error message for error code
     */
    fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            INITIALIZATION_ERROR -> "Speech engine initialization failed"
            CONFIGURATION_ERROR -> "Speech engine configuration error"
            SERVICE_UNAVAILABLE -> "Speech recognition service unavailable"
            
            RECOGNITION_ERROR -> "Speech recognition failed"
            PROCESSING_ERROR -> "Audio processing error"
            TIMEOUT_ERROR -> "Recognition timeout"
            
            MODEL_LOADING_ERROR -> "Speech model loading failed"
            ASSET_LOADING_ERROR -> "Asset loading failed"
            MODEL_COMPILATION_ERROR -> "Model compilation failed"
            ASSET_CORRUPTION_ERROR -> "Asset corruption detected"
            
            AUDIO_PIPELINE_ERROR -> "Audio pipeline error"
            MICROPHONE_ERROR -> "Microphone access error"
            AUDIO_RECORDING_ERROR -> "Audio recording failed"
            AUDIO_FORMAT_ERROR -> "Unsupported audio format"
            
            MEMORY_ERROR -> "Memory allocation error"
            STORAGE_ERROR -> "Storage access error" 
            RESOURCE_EXHAUSTED -> "System resources exhausted"
            
            NETWORK_ERROR -> "Network connectivity error"
            CONNECTION_ERROR -> "Connection failed"
            AUTHENTICATION_ERROR -> "Authentication failed"
            API_ERROR -> "API request failed"
            
            else -> "Unknown error ($errorCode)"
        }
    }
    
    /**
     * Check if error code indicates a recoverable error
     */
    fun isRecoverable(errorCode: Int): Boolean {
        return when (errorCode) {
            // Recoverable errors
            MEMORY_ERROR,
            AUDIO_PIPELINE_ERROR,
            MICROPHONE_ERROR,
            AUDIO_RECORDING_ERROR,
            NETWORK_ERROR,
            CONNECTION_ERROR,
            TIMEOUT_ERROR -> true
            
            // Generally non-recoverable errors
            CONFIGURATION_ERROR,
            MODEL_COMPILATION_ERROR,
            ASSET_CORRUPTION_ERROR,
            AUTHENTICATION_ERROR -> false
            
            // Conditionally recoverable
            INITIALIZATION_ERROR -> true
            
            else -> false
        }
    }
    
    /**
     * Get error category for grouping similar errors
     */
    fun getErrorCategory(errorCode: Int): ErrorCategory {
        return when (errorCode) {
            in 1001..1099 -> ErrorCategory.INITIALIZATION
            in 1101..1199 -> ErrorCategory.RECOGNITION
            in 1201..1299 -> ErrorCategory.MODEL_ASSET
            in 1301..1399 -> ErrorCategory.AUDIO
            in 1401..1499 -> ErrorCategory.RESOURCE
            in 1501..1599 -> ErrorCategory.NETWORK
            else -> ErrorCategory.UNKNOWN
        }
    }
    
    enum class ErrorCategory {
        INITIALIZATION,
        RECOGNITION,
        MODEL_ASSET,
        AUDIO,
        RESOURCE,
        NETWORK,
        UNKNOWN
    }
}