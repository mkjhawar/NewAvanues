/**
 * GoogleConfig.kt - Google Cloud Speech API configuration management
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * Handles all configuration aspects for Google Cloud Speech Recognition
 * including recognition configs, language settings, and mode management
 */
package com.augmentalis.voiceos.speech.engines.google

import android.util.Log
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechMode
// import com.augmentalis.speechrecognition.engines.GoogleCloudSpeechLite // Not implemented yet

/**
 * Manages all Google Cloud Speech configuration settings.
 * Provides mode-specific configurations and validation.
 */
class GoogleConfig(private val initialConfig: SpeechConfig) {
    
    companion object {
        private const val TAG = "GoogleConfig"
        
        // Recognition settings
        const val SAMPLE_RATE_HERTZ = 16000
        const val MAX_ALTERNATIVES = 3
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.7f
        
        // Timeout values
        const val DEFAULT_TIMEOUT_MS = 15000L
        const val NETWORK_TIMEOUT_MS = 10000L
        
        // Phrase hints limits
        const val MAX_PHRASE_HINTS = 100
        const val DEFAULT_CONTEXT_BOOST_LEVEL = 10.0f
    }
    
    // Current configuration
    private var config: SpeechConfig = initialConfig
    
    // Recognition configurations for different modes
    private var commandModeConfig: Any? = null // GoogleCloudSpeechLite.RecognitionConfig
    private var dictationModeConfig: Any? = null // GoogleCloudSpeechLite.RecognitionConfig
    
    // Current mode and config
    private var currentMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND
    private var currentRecognitionConfig: Any? = null // GoogleCloudSpeechLite.RecognitionConfig
    
    // Phrase hints and boosting
    private var enhancedPhraseHints = mutableListOf<String>()
    private var contextBoostLevel = DEFAULT_CONTEXT_BOOST_LEVEL
    private var maxPhraseHints = MAX_PHRASE_HINTS
    
    /**
     * Initialize configurations
     */
    fun initialize(): Result<Unit> {
        return try {
            Log.i(TAG, "Initializing Google Cloud configuration...")
            
            // Validate configuration
            config.validate().onFailure { 
                return Result.failure(it)
            }
            
            // Check for API key
            if (config.cloudApiKey.isNullOrBlank()) {
                return Result.failure(
                    IllegalArgumentException("Google Cloud API key required")
                )
            }
            
            // Create recognition configurations for different modes
            commandModeConfig = createRecognitionConfig(config, SpeechMode.DYNAMIC_COMMAND)
            dictationModeConfig = createRecognitionConfig(config, SpeechMode.DICTATION)
            currentRecognitionConfig = commandModeConfig  // Start with command mode
            
            Log.i(TAG, "Google Cloud configuration initialized successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Configuration initialization failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get current speech configuration
     */
    fun getSpeechConfig(): SpeechConfig = config
    
    /**
     * Get API key
     */
    fun getApiKey(): String? = config.cloudApiKey
    
    /**
     * Get current recognition configuration
     */
    fun getCurrentRecognitionConfig(): Any? = currentRecognitionConfig // GoogleCloudSpeechLite.RecognitionConfig
    
    /**
     * Get command mode configuration
     */
    fun getCommandModeConfig(): Any? = commandModeConfig // GoogleCloudSpeechLite.RecognitionConfig
    
    /**
     * Get dictation mode configuration
     */
    fun getDictationModeConfig(): Any? = dictationModeConfig // GoogleCloudSpeechLite.RecognitionConfig
    
    /**
     * Get current mode
     */
    fun getCurrentMode(): SpeechMode = currentMode
    
    /**
     * Change recognition mode
     */
    fun changeMode(mode: SpeechMode): Result<Unit> {
        return try {
            if (currentMode == mode) {
                Log.d(TAG, "Already in mode: $mode")
                return Result.success(Unit)
            }
            
            Log.i(TAG, "Changing configuration from $currentMode to $mode")
            
            currentMode = mode
            
            // Switch recognition config
            currentRecognitionConfig = when (mode) {
                SpeechMode.DICTATION, SpeechMode.FREE_SPEECH -> dictationModeConfig
                else -> commandModeConfig
            }
            
            Log.i(TAG, "Successfully changed configuration to mode: $mode")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to change mode to $mode", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update configuration with language support
     */
    fun updateConfiguration(newConfig: SpeechConfig): Result<Unit> {
        return try {
            val oldLanguage = config.language
            config = newConfig
            
            // If language changed, reload configurations
            if (oldLanguage != newConfig.language) {
                Log.i(TAG, "Language changed from $oldLanguage to ${newConfig.language} - reloading configurations")
                
                // Recreate recognition configs
                commandModeConfig = createRecognitionConfig(config, SpeechMode.DYNAMIC_COMMAND)
                dictationModeConfig = createRecognitionConfig(config, SpeechMode.DICTATION)
                currentRecognitionConfig = when (currentMode) {
                    SpeechMode.DICTATION, SpeechMode.FREE_SPEECH -> dictationModeConfig
                    else -> commandModeConfig
                }
            }
            
            Log.i(TAG, "Configuration updated successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update configuration", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update enhanced phrase hints with intelligent boosting and prioritization
     */
    fun updatePhraseHints(phrases: List<String>) {
        if (phrases.isEmpty()) return
        
        try {
            Log.i(TAG, "Updating enhanced phrase hints with ${phrases.size} phrases...")
            
            // Smart phrase selection and prioritization
            val prioritizedPhrases = prioritizePhrases(phrases).take(maxPhraseHints)
            
            // Store phrase hints for command matching
            enhancedPhraseHints.clear()
            enhancedPhraseHints.addAll(prioritizedPhrases)
            
            Log.i(TAG, "Updated phrase hints: ${prioritizedPhrases.size} total phrases stored for command matching")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update enhanced phrase hints", e)
        }
    }
    
    /**
     * Get enhanced phrase hints
     */
    fun getPhraseHints(): List<String> = enhancedPhraseHints.toList()
    
    /**
     * Get context boost level
     */
    fun getContextBoostLevel(): Float = contextBoostLevel
    
    /**
     * Set context boost level
     */
    fun setContextBoostLevel(level: Float) {
        contextBoostLevel = level
    }
    
    /**
     * Create recognition configuration with mode-specific settings
     */
    private fun createRecognitionConfig(
        @Suppress("UNUSED_PARAMETER") speechConfig: SpeechConfig, 
        @Suppress("UNUSED_PARAMETER") speechMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND
    ): Any? { // GoogleCloudSpeechLite.RecognitionConfig
        // TODO: Implement when GoogleCloudSpeechLite is available
        /*return GoogleCloudSpeechLite.RecognitionConfig(
            encoding = GoogleCloudSpeechLite.AudioEncoding.LINEAR16,
            sampleRateHertz = SAMPLE_RATE_HERTZ,
            languageCode = config.language,
            maxAlternatives = MAX_ALTERNATIVES,
            profanityFilter = config.enableProfanityFilter,
            enableWordTimeOffsets = true,
            enableAutomaticPunctuation = true,
            model = if (mode == SpeechMode.DICTATION) 
                GoogleCloudSpeechLite.Model.LATEST_LONG 
            else 
                GoogleCloudSpeechLite.Model.COMMAND_AND_SEARCH,
            useEnhanced = true
        )*/
        return null
    }
    
    /**
     * Prioritize phrases for optimal Cloud Speech recognition
     */
    private fun prioritizePhrases(phrases: List<String>): List<String> {
        return phrases
            .filter { it.isNotBlank() && it.length <= 100 }  // Google Cloud limits
            .map { it.trim() }
            .distinct()
            .sortedWith(compareBy<String> {
                // Prioritize by length (shorter phrases get recognized better)
                it.length
            }.thenBy {
                // Then by word count (fewer words = more precise)
                it.split("\\s+").size
            }.thenBy {
                // Finally alphabetically for consistency
                it
            })
    }
    
    /**
     * Validate current configuration
     */
    fun validate(): Result<Unit> {
        return try {
            // Validate API key
            if (config.cloudApiKey.isNullOrBlank()) {
                return Result.failure(
                    IllegalArgumentException("Google Cloud API key is required")
                )
            }
            
            // Validate configurations exist
            if (commandModeConfig == null || dictationModeConfig == null) {
                return Result.failure(
                    IllegalStateException("Recognition configurations not initialized")
                )
            }
            
            // Validate current config
            if (currentRecognitionConfig == null) {
                return Result.failure(
                    IllegalStateException("Current recognition configuration not set")
                )
            }
            
            Log.d(TAG, "Configuration validation successful")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Configuration validation failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get configuration statistics
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "currentMode" to currentMode.name,
            "language" to config.language,
            "enableProfanityFilter" to config.enableProfanityFilter,
            "timeoutDuration" to config.timeoutDuration,
            "contextBoostLevel" to contextBoostLevel,
            "phraseHintsCount" to enhancedPhraseHints.size,
            "maxPhraseHints" to maxPhraseHints,
            "hasApiKey" to (config.cloudApiKey?.isNotBlank() == true),
            "sampleRateHz" to SAMPLE_RATE_HERTZ,
            "maxAlternatives" to MAX_ALTERNATIVES
        )
    }
    
    /**
     * Clear all configurations
     */
    fun clear() {
        enhancedPhraseHints.clear()
        commandModeConfig = null
        dictationModeConfig = null
        currentRecognitionConfig = null
        contextBoostLevel = DEFAULT_CONTEXT_BOOST_LEVEL
    }
}