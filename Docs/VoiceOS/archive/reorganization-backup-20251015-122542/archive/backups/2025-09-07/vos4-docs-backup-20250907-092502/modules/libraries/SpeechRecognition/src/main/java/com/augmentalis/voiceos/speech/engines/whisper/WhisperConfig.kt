/**
 * WhisperConfig.kt - Configuration management component for Whisper engine
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * Centralized configuration management for Whisper speech recognition engine.
 * Handles all settings, parameters, and configuration validation.
 */
package com.augmentalis.voiceos.speech.engines.whisper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.augmentalis.speechrecognition.SpeechConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Advanced Whisper configuration with comprehensive settings
 */
data class WhisperEngineConfig(
    // Model configuration
    val modelSize: WhisperModelSize = WhisperModelSize.BASE,
    val enableGPU: Boolean = true,
    
    // Processing configuration
    val processingMode: WhisperProcessingMode = WhisperProcessingMode.HYBRID,
    val enableLanguageDetection: Boolean = true,
    val enableWordTimestamps: Boolean = true,
    
    // Translation configuration
    val enableTranslation: Boolean = false,
    val targetTranslationLanguage: String = "en",
    
    // Advanced features
    val enableSpeakerDiarization: Boolean = false,
    val enableRealTimeTranscription: Boolean = true,
    
    // Audio processing
    val noiseReductionLevel: Float = 0.7f, // 0.0 to 1.0
    val vadSensitivity: Float = 0.5f, // Voice Activity Detection sensitivity
    val audioBufferMs: Int = 1500, // Audio buffer length in milliseconds
    
    // Inference parameters
    val temperature: Float = 0.0f, // Sampling temperature for creativity (0.0-2.0)
    val temperatureInc: Float = 0.2f, // Temperature increment for fallback
    val beamSize: Int = 5, // Beam search size (1-10)
    val bestOf: Int = 5, // Number of candidates to consider (1-10)
    val maxSegmentLength: Int = 30, // Max segment length in seconds
    
    // Quality thresholds
    val compressionRatioThreshold: Float = 2.4f, // Compression ratio threshold
    val logprobThreshold: Float = -1.0f, // Log probability threshold
    val noSpeechThreshold: Float = 0.6f, // No speech detection threshold
    val confidenceThreshold: Float = 0.7f, // Minimum confidence for results
    
    // Performance settings
    val maxConcurrentInferences: Int = 1, // Maximum concurrent inference operations
    val inferenceTimeoutMs: Long = 30000L, // 30 seconds timeout
    val enablePerformanceOptimizations: Boolean = true,
    
    // Language settings
    val defaultLanguage: String = "auto", // Auto-detect or specific language code
    val supportedLanguages: Set<String> = setOf("auto", "en", "es", "fr", "de", "it", "pt", "nl", "ru", "zh", "ja", "ko")
) {
    
    companion object {
        /**
         * Create default configuration optimized for the current device
         */
        fun getDefault(): WhisperEngineConfig = WhisperEngineConfig()
        
        /**
         * Create configuration optimized for low-latency real-time processing
         */
        fun getRealTimeOptimized(): WhisperEngineConfig = WhisperEngineConfig(
            modelSize = WhisperModelSize.TINY,
            processingMode = WhisperProcessingMode.REAL_TIME,
            audioBufferMs = 500,
            beamSize = 1,
            bestOf = 1,
            enableWordTimestamps = false,
            enableSpeakerDiarization = false,
            maxConcurrentInferences = 1,
            inferenceTimeoutMs = 5000L
        )
        
        /**
         * Create configuration optimized for high accuracy batch processing
         */
        fun getAccuracyOptimized(): WhisperEngineConfig = WhisperEngineConfig(
            modelSize = WhisperModelSize.SMALL,
            processingMode = WhisperProcessingMode.BATCH,
            audioBufferMs = 3000,
            beamSize = 10,
            bestOf = 10,
            enableWordTimestamps = true,
            enableSpeakerDiarization = true,
            temperature = 0.0f,
            compressionRatioThreshold = 2.4f
        )
        
        /**
         * Create configuration optimized for low-end devices
         */
        fun getLowResourceOptimized(): WhisperEngineConfig = WhisperEngineConfig(
            modelSize = WhisperModelSize.TINY,
            processingMode = WhisperProcessingMode.HYBRID,
            enableGPU = false,
            audioBufferMs = 1000,
            beamSize = 3,
            bestOf = 3,
            enableWordTimestamps = false,
            enableSpeakerDiarization = false,
            enablePerformanceOptimizations = true,
            maxConcurrentInferences = 1
        )
    }
    
    /**
     * Validate configuration parameters
     */
    fun validate(): ConfigValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate ranges
        if (noiseReductionLevel !in 0f..1f) {
            errors.add("noiseReductionLevel must be between 0.0 and 1.0")
        }
        
        if (vadSensitivity !in 0f..1f) {
            errors.add("vadSensitivity must be between 0.0 and 1.0")
        }
        
        if (temperature !in 0f..2f) {
            errors.add("temperature must be between 0.0 and 2.0")
        }
        
        if (beamSize !in 1..10) {
            errors.add("beamSize must be between 1 and 10")
        }
        
        if (bestOf !in 1..10) {
            errors.add("bestOf must be between 1 and 10")
        }
        
        if (confidenceThreshold !in 0f..1f) {
            errors.add("confidenceThreshold must be between 0.0 and 1.0")
        }
        
        if (audioBufferMs < 100 || audioBufferMs > 10000) {
            errors.add("audioBufferMs must be between 100 and 10000")
        }
        
        if (maxSegmentLength < 1 || maxSegmentLength > 60) {
            errors.add("maxSegmentLength must be between 1 and 60 seconds")
        }
        
        if (!supportedLanguages.contains(defaultLanguage)) {
            errors.add("defaultLanguage '$defaultLanguage' is not in supportedLanguages")
        }
        
        // Performance warnings
        if (modelSize == WhisperModelSize.LARGE && processingMode == WhisperProcessingMode.REAL_TIME) {
            warnings.add("Large model with real-time mode may cause performance issues")
        }
        
        if (enableSpeakerDiarization && modelSize == WhisperModelSize.TINY) {
            warnings.add("Speaker diarization may not work well with tiny model")
        }
        
        if (beamSize > 5 && processingMode == WhisperProcessingMode.REAL_TIME) {
            warnings.add("High beam size may increase latency in real-time mode")
        }
        
        if (maxConcurrentInferences > 1) {
            warnings.add("Multiple concurrent inferences may cause memory issues")
        }
        
        return ConfigValidationResult(errors, warnings)
    }
    
    /**
     * Get estimated memory usage in MB
     */
    fun getEstimatedMemoryUsage(): Int {
        var baseMemory = modelSize.memoryUsageMB
        
        // Add overhead for features
        if (enableWordTimestamps) baseMemory += 50
        if (enableSpeakerDiarization) baseMemory += 100
        if (enableTranslation) baseMemory += 200
        
        // Add processing overhead
        baseMemory += when (processingMode) {
            WhisperProcessingMode.REAL_TIME -> 100
            WhisperProcessingMode.BATCH -> 200
            WhisperProcessingMode.HYBRID -> 150
        }
        
        return baseMemory
    }
}

/**
 * Configuration validation result
 */
data class ConfigValidationResult(
    val errors: List<String>,
    val warnings: List<String>
) {
    val isValid: Boolean get() = errors.isEmpty()
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
}

/**
 * Configuration change listener
 */
typealias ConfigChangeListener = (WhisperEngineConfig, WhisperEngineConfig) -> Unit

/**
 * Manages Whisper engine configuration with persistence and validation.
 * Provides centralized configuration management with device-specific optimizations.
 */
class WhisperConfig(private val context: Context) {
    
    companion object {
        private const val TAG = "WhisperConfig"
        private const val PREFS_NAME = "whisper_engine_config"
        
        // Preference keys
        private const val KEY_MODEL_SIZE = "model_size"
        private const val KEY_PROCESSING_MODE = "processing_mode"
        private const val KEY_ENABLE_GPU = "enable_gpu"
        private const val KEY_NOISE_REDUCTION = "noise_reduction_level"
        private const val KEY_VAD_SENSITIVITY = "vad_sensitivity"
        private const val KEY_ENABLE_LANGUAGE_DETECTION = "enable_language_detection"
        private const val KEY_ENABLE_WORD_TIMESTAMPS = "enable_word_timestamps"
        private const val KEY_ENABLE_TRANSLATION = "enable_translation"
        private const val KEY_TARGET_LANGUAGE = "target_language"
        private const val KEY_CONFIDENCE_THRESHOLD = "confidence_threshold"
        private const val KEY_BEAM_SIZE = "beam_size"
        private const val KEY_TEMPERATURE = "temperature"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val configMutex = Mutex()
    
    // Current configuration
    private var currentConfig: WhisperEngineConfig = loadConfigFromPrefs()
    
    // Change listeners
    private val changeListeners = mutableListOf<ConfigChangeListener>()
    
    /**
     * Get current configuration
     */
    suspend fun getCurrentConfig(): WhisperEngineConfig = configMutex.withLock {
        return currentConfig
    }
    
    /**
     * Update configuration with validation
     */
    suspend fun updateConfig(newConfig: WhisperEngineConfig): ConfigValidationResult = configMutex.withLock {
        val validationResult = newConfig.validate()
        
        if (validationResult.isValid) {
            val oldConfig = currentConfig
            currentConfig = newConfig
            saveConfigToPrefs(newConfig)
            
            // Notify listeners
            changeListeners.forEach { listener ->
                try {
                    listener(oldConfig, newConfig)
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying config change listener", e)
                }
            }
            
            Log.i(TAG, "Configuration updated successfully")
            if (validationResult.hasWarnings) {
                Log.w(TAG, "Configuration warnings: ${validationResult.warnings.joinToString(", ")}")
            }
        } else {
            Log.e(TAG, "Configuration validation failed: ${validationResult.errors.joinToString(", ")}")
        }
        
        return validationResult
    }
    
    /**
     * Reset to default configuration
     */
    suspend fun resetToDefault(): WhisperEngineConfig = configMutex.withLock {
        val defaultConfig = WhisperEngineConfig.getDefault()
        val validationResult = updateConfig(defaultConfig)
        
        if (!validationResult.isValid) {
            Log.e(TAG, "Default configuration is invalid! This should not happen.")
        }
        
        return defaultConfig
    }
    
    /**
     * Apply device-specific optimizations
     */
    suspend fun applyDeviceOptimizations(): WhisperEngineConfig = configMutex.withLock {
        val optimizedConfig = getDeviceOptimizedConfig()
        updateConfig(optimizedConfig)
        return optimizedConfig
    }
    
    /**
     * Get configuration optimized for current device
     */
    private fun getDeviceOptimizedConfig(): WhisperEngineConfig {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val maxMemoryMB = maxMemory / (1024 * 1024)
        
        // Determine optimal configuration based on available memory
        return when {
            maxMemoryMB < 512 -> {
                Log.i(TAG, "Low memory device detected (${maxMemoryMB}MB), using low resource config")
                WhisperEngineConfig.getLowResourceOptimized()
            }
            
            maxMemoryMB < 1024 -> {
                Log.i(TAG, "Medium memory device detected (${maxMemoryMB}MB), using balanced config")
                currentConfig.copy(
                    modelSize = WhisperModelSize.TINY,
                    processingMode = WhisperProcessingMode.HYBRID,
                    beamSize = 3,
                    bestOf = 3
                )
            }
            
            maxMemoryMB < 2048 -> {
                Log.i(TAG, "High memory device detected (${maxMemoryMB}MB), using standard config")
                currentConfig.copy(
                    modelSize = WhisperModelSize.BASE,
                    processingMode = WhisperProcessingMode.HYBRID
                )
            }
            
            else -> {
                Log.i(TAG, "Very high memory device detected (${maxMemoryMB}MB), using high-end config")
                currentConfig.copy(
                    modelSize = WhisperModelSize.SMALL,
                    processingMode = WhisperProcessingMode.BATCH,
                    beamSize = 5,
                    bestOf = 5,
                    enableWordTimestamps = true
                )
            }
        }
    }
    
    /**
     * Create configuration from base SpeechConfig
     */
    fun createFromSpeechConfig(speechConfig: SpeechConfig): WhisperEngineConfig {
        return currentConfig.copy(
            confidenceThreshold = speechConfig.confidenceThreshold,
            enableLanguageDetection = true, // Always enable for Whisper
            defaultLanguage = speechConfig.language
        )
    }
    
    /**
     * Add configuration change listener
     */
    fun addChangeListener(listener: ConfigChangeListener) {
        synchronized(changeListeners) {
            changeListeners.add(listener)
        }
    }
    
    /**
     * Remove configuration change listener
     */
    fun removeChangeListener(listener: ConfigChangeListener) {
        synchronized(changeListeners) {
            changeListeners.remove(listener)
        }
    }
    
    /**
     * Get configuration as map for debugging
     */
    fun getConfigMap(): Map<String, Any> {
        return mapOf(
            "modelSize" to currentConfig.modelSize.modelName,
            "processingMode" to currentConfig.processingMode.name,
            "enableGPU" to currentConfig.enableGPU,
            "noiseReductionLevel" to currentConfig.noiseReductionLevel,
            "vadSensitivity" to currentConfig.vadSensitivity,
            "confidenceThreshold" to currentConfig.confidenceThreshold,
            "beamSize" to currentConfig.beamSize,
            "temperature" to currentConfig.temperature,
            "enableLanguageDetection" to currentConfig.enableLanguageDetection,
            "enableWordTimestamps" to currentConfig.enableWordTimestamps,
            "enableTranslation" to currentConfig.enableTranslation,
            "estimatedMemoryMB" to currentConfig.getEstimatedMemoryUsage()
        )
    }
    
    /**
     * Load configuration from SharedPreferences
     */
    private fun loadConfigFromPrefs(): WhisperEngineConfig {
        return try {
            WhisperEngineConfig(
                modelSize = WhisperModelSize.fromString(
                    prefs.getString(KEY_MODEL_SIZE, WhisperModelSize.BASE.modelName) ?: WhisperModelSize.BASE.modelName
                ),
                processingMode = WhisperProcessingMode.valueOf(
                    prefs.getString(KEY_PROCESSING_MODE, WhisperProcessingMode.HYBRID.name) ?: WhisperProcessingMode.HYBRID.name
                ),
                enableGPU = prefs.getBoolean(KEY_ENABLE_GPU, true),
                noiseReductionLevel = prefs.getFloat(KEY_NOISE_REDUCTION, 0.7f),
                vadSensitivity = prefs.getFloat(KEY_VAD_SENSITIVITY, 0.5f),
                enableLanguageDetection = prefs.getBoolean(KEY_ENABLE_LANGUAGE_DETECTION, true),
                enableWordTimestamps = prefs.getBoolean(KEY_ENABLE_WORD_TIMESTAMPS, true),
                enableTranslation = prefs.getBoolean(KEY_ENABLE_TRANSLATION, false),
                targetTranslationLanguage = prefs.getString(KEY_TARGET_LANGUAGE, "en") ?: "en",
                confidenceThreshold = prefs.getFloat(KEY_CONFIDENCE_THRESHOLD, 0.7f),
                beamSize = prefs.getInt(KEY_BEAM_SIZE, 5),
                temperature = prefs.getFloat(KEY_TEMPERATURE, 0.0f)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error loading config from preferences, using default", e)
            WhisperEngineConfig.getDefault()
        }
    }
    
    /**
     * Save configuration to SharedPreferences
     */
    private fun saveConfigToPrefs(config: WhisperEngineConfig) {
        try {
            prefs.edit().apply {
                putString(KEY_MODEL_SIZE, config.modelSize.modelName)
                putString(KEY_PROCESSING_MODE, config.processingMode.name)
                putBoolean(KEY_ENABLE_GPU, config.enableGPU)
                putFloat(KEY_NOISE_REDUCTION, config.noiseReductionLevel)
                putFloat(KEY_VAD_SENSITIVITY, config.vadSensitivity)
                putBoolean(KEY_ENABLE_LANGUAGE_DETECTION, config.enableLanguageDetection)
                putBoolean(KEY_ENABLE_WORD_TIMESTAMPS, config.enableWordTimestamps)
                putBoolean(KEY_ENABLE_TRANSLATION, config.enableTranslation)
                putString(KEY_TARGET_LANGUAGE, config.targetTranslationLanguage)
                putFloat(KEY_CONFIDENCE_THRESHOLD, config.confidenceThreshold)
                putInt(KEY_BEAM_SIZE, config.beamSize)
                putFloat(KEY_TEMPERATURE, config.temperature)
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving config to preferences", e)
        }
    }
    
    /**
     * Clear all configuration preferences
     */
    fun clearPreferences() {
        prefs.edit().clear().apply()
        Log.i(TAG, "Configuration preferences cleared")
    }
}