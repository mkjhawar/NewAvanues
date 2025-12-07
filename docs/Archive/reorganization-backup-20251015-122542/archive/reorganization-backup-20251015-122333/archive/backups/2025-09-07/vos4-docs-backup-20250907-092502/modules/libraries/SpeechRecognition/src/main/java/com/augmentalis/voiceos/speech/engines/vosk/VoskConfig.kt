/**
 * VoskConfig.kt - Configuration management for VOSK speech recognition engine
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * SOLID Principle: Single Responsibility
 * - Manages VOSK-specific configuration settings
 * - Handles validation of configuration parameters
 * - Provides configuration state management
 */
package com.augmentalis.voiceos.speech.engines.vosk

import android.util.Log
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechMode

/**
 * Configuration manager for VOSK engine.
 * Handles all configuration-related functionality including validation,
 * state management, and VOSK-specific settings.
 */
class VoskConfig {
    
    companion object {
        private const val TAG = "VoskConfig"
        const val DEFAULT_SAMPLE_RATE = 16000.0f
        const val DEFAULT_CONFIDENCE = 5000
        const val EXACT_MATCH_CONFIDENCE = 9000
        const val SIMILARITY_THRESHOLD = 0.6
        const val CONFIDENCE_SCALE = 8000
        const val DEFAULT_TIMEOUT_MINUTES = 30L
        const val SILENCE_CHECK_INTERVAL = 500L // milliseconds
    }
    
    // Configuration state
    private lateinit var speechConfig: SpeechConfig
    private var isInitialized = false
    private var languageBcpTag: String = "en-US"
    private var useGrammarConstraints = true
    private var enableLearning = true
    private var enableVocabularyCache = true
    private var maxCacheSize = 1000
    private var responseDelay = 0L
    private var timeoutValue = DEFAULT_TIMEOUT_MINUTES
    
    // Configuration flags
    private var isConfigValid = false
    private var lastValidationTime = 0L
    private var validationErrors = mutableListOf<String>()
    
    /**
     * Initialize configuration with SpeechConfig
     */
    fun initialize(config: SpeechConfig): Boolean {
        return try {
            Log.i(TAG, "Initializing VOSK configuration...")
            
            this.speechConfig = config
            this.languageBcpTag = config.language
            
            // Validate configuration
            validateConfiguration(config)
            
            if (isConfigValid) {
                isInitialized = true
                Log.i(TAG, "VOSK configuration initialized successfully")
                true
            } else {
                Log.e(TAG, "VOSK configuration validation failed: ${validationErrors.joinToString()}")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing VOSK configuration", e)
            validationErrors.add("Initialization error: ${e.message}")
            false
        }
    }
    
    /**
     * Update configuration at runtime
     */
    fun updateConfiguration(config: SpeechConfig): Boolean {
        return try {
            Log.i(TAG, "Updating VOSK configuration...")
            
            val oldLanguage = languageBcpTag
            this.speechConfig = config
            this.languageBcpTag = config.language
            
            // Re-validate configuration
            validateConfiguration(config)
            
            if (isConfigValid) {
                // Check if language changed (important for model loading)
                val languageChanged = oldLanguage != languageBcpTag
                Log.i(TAG, "VOSK configuration updated successfully. Language changed: $languageChanged")
                true
            } else {
                Log.e(TAG, "VOSK configuration update validation failed: ${validationErrors.joinToString()}")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating VOSK configuration", e)
            validationErrors.add("Update error: ${e.message}")
            false
        }
    }
    
    /**
     * Validate configuration parameters
     */
    private fun validateConfiguration(config: SpeechConfig) {
        validationErrors.clear()
        lastValidationTime = System.currentTimeMillis()
        
        try {
            // Validate basic config
            if (config.language.isBlank()) {
                validationErrors.add("Language cannot be blank")
            }
            
            // Validate timeout duration
            val timeoutMs = config.timeoutDuration
            if (timeoutMs < 0) {
                validationErrors.add("Timeout duration cannot be negative")
            } else if (timeoutMs > 300000) { // 5 minutes max
                validationErrors.add("Timeout duration too long (max 5 minutes)")
            }
            
            // Validate language format
            if (!isValidLanguageTag(config.language)) {
                validationErrors.add("Invalid language tag format: ${config.language}")
            }
            
            // Validate model requirements
            if (!validateModelRequirements(config.language)) {
                validationErrors.add("Model requirements not met for language: ${config.language}")
            }
            
            isConfigValid = validationErrors.isEmpty()
            
        } catch (e: Exception) {
            validationErrors.add("Validation error: ${e.message}")
            isConfigValid = false
        }
    }
    
    /**
     * Validate language tag format (basic BCP-47 validation)
     */
    private fun isValidLanguageTag(language: String): Boolean {
        return try {
            // Basic format check: should contain 2-5 character language code
            val parts = language.split("-")
            parts.isNotEmpty() && parts[0].length in 2..5 && parts[0].all { it.isLetter() }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validate model requirements for the language
     */
    private fun validateModelRequirements(language: String): Boolean {
        return try {
            // For now, we primarily support English models
            // This can be extended for multi-language support
            language.startsWith("en", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Enable or disable grammar constraints
     */
    fun setGrammarConstraintsEnabled(enabled: Boolean) {
        useGrammarConstraints = enabled
        Log.d(TAG, "Grammar constraints ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Set response delay for testing/debugging
     */
    fun setResponseDelay(delayMs: Long) {
        responseDelay = if (delayMs >= 0) delayMs else 0L
        Log.d(TAG, "Response delay set to ${responseDelay}ms")
    }
    
    /**
     * Set maximum cache size
     */
    fun setMaxCacheSize(size: Int) {
        maxCacheSize = if (size > 0) size else 1000
        Log.d(TAG, "Max cache size set to $maxCacheSize")
    }
    
    /**
     * Enable or disable learning system
     */
    fun setLearningEnabled(enabled: Boolean) {
        enableLearning = enabled
        Log.d(TAG, "Learning system ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Enable or disable vocabulary caching
     */
    fun setVocabularyCacheEnabled(enabled: Boolean) {
        enableVocabularyCache = enabled
        Log.d(TAG, "Vocabulary cache ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Set timeout value in minutes
     */
    fun setTimeoutMinutes(minutes: Long) {
        timeoutValue = if (minutes > 0) minutes else DEFAULT_TIMEOUT_MINUTES
        Log.d(TAG, "Timeout set to ${timeoutValue} minutes")
    }
    
    // Getters for configuration values
    fun getSpeechConfig(): SpeechConfig = speechConfig
    fun getLanguage(): String = languageBcpTag
    fun getSampleRate(): Float = DEFAULT_SAMPLE_RATE
    fun isGrammarConstraintsEnabled(): Boolean = useGrammarConstraints
    fun getResponseDelay(): Long = responseDelay
    fun getMaxCacheSize(): Int = maxCacheSize
    fun isLearningEnabled(): Boolean = enableLearning
    fun isVocabularyCacheEnabled(): Boolean = enableVocabularyCache
    fun getTimeoutMinutes(): Long = timeoutValue
    fun getTimeoutDuration(): Long = speechConfig.timeoutDuration
    
    // Configuration state getters
    fun isInitialized(): Boolean = isInitialized
    fun isValid(): Boolean = isConfigValid
    fun getValidationErrors(): List<String> = validationErrors.toList()
    fun getLastValidationTime(): Long = lastValidationTime
    
    /**
     * Get configuration summary for debugging
     */
    fun getConfigSummary(): Map<String, Any> {
        return mapOf(
            "initialized" to isInitialized,
            "valid" to isConfigValid,
            "language" to languageBcpTag,
            "sampleRate" to DEFAULT_SAMPLE_RATE,
            "grammarConstraints" to useGrammarConstraints,
            "learningEnabled" to enableLearning,
            "vocabularyCacheEnabled" to enableVocabularyCache,
            "maxCacheSize" to maxCacheSize,
            "responseDelay" to responseDelay,
            "timeoutMinutes" to timeoutValue,
            "timeoutDurationMs" to (if (::speechConfig.isInitialized) speechConfig.timeoutDuration else 0L),
            "validationErrors" to validationErrors.size,
            "lastValidation" to lastValidationTime
        )
    }
    
    /**
     * Reset configuration to defaults
     */
    fun reset() {
        isInitialized = false
        languageBcpTag = "en-US"
        useGrammarConstraints = true
        enableLearning = true
        enableVocabularyCache = true
        maxCacheSize = 1000
        responseDelay = 0L
        timeoutValue = DEFAULT_TIMEOUT_MINUTES
        isConfigValid = false
        lastValidationTime = 0L
        validationErrors.clear()
        Log.d(TAG, "Configuration reset to defaults")
    }
    
    /**
     * Export configuration for backup/restore
     */
    fun exportConfig(): Map<String, Any> {
        return mapOf(
            "language" to languageBcpTag,
            "grammarConstraints" to useGrammarConstraints,
            "learningEnabled" to enableLearning,
            "vocabularyCacheEnabled" to enableVocabularyCache,
            "maxCacheSize" to maxCacheSize,
            "responseDelay" to responseDelay,
            "timeoutMinutes" to timeoutValue
        )
    }
    
    /**
     * Import configuration from backup
     */
    fun importConfig(config: Map<String, Any>): Boolean {
        return try {
            config["language"]?.let { languageBcpTag = it.toString() }
            config["grammarConstraints"]?.let { useGrammarConstraints = it as Boolean }
            config["learningEnabled"]?.let { enableLearning = it as Boolean }
            config["vocabularyCacheEnabled"]?.let { enableVocabularyCache = it as Boolean }
            config["maxCacheSize"]?.let { maxCacheSize = it as Int }
            config["responseDelay"]?.let { responseDelay = it as Long }
            config["timeoutMinutes"]?.let { timeoutValue = it as Long }
            
            Log.d(TAG, "Configuration imported successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error importing configuration", e)
            false
        }
    }
}