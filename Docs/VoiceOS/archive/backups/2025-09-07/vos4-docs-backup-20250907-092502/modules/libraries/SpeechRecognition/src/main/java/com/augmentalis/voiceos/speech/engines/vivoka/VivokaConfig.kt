/**
 * VivokaConfig.kt - Configuration management for Vivoka VSDK engine
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * 
 * Extracted from monolithic VivokaEngine.kt as part of SOLID refactoring
 * Handles configuration initialization, validation, and management
 */
package com.augmentalis.voiceos.speech.engines.vivoka

import android.content.Context
import android.util.Log
import com.augmentalis.speechrecognition.SpeechConfig
import java.io.File

/**
 * Manages configuration for the Vivoka engine including paths, models, and validation
 */
class VivokaConfig(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "VivokaConfig"
        private const val VSDK_CONFIG = "vsdk.json"
        private const val ASSETS_SUBDIR = "vsdk"
    }
    
    // Configuration data
    private lateinit var speechConfig: SpeechConfig
    private var assetsPath: String = ""
    private var configPath: String = ""
    private var modelPath: String = ""
    private var dictationModelPath: String = ""
    
    // Initialization state
    @Volatile
    private var isConfigured = false
    
    /**
     * Initialize configuration with SpeechConfig
     */
    fun initialize(config: SpeechConfig): Boolean {
        return try {
            Log.d(TAG, "Initializing Vivoka configuration")
            
            this.speechConfig = config
            
            // Setup asset paths
            setupAssetPaths()
            
            // Determine model paths based on language
            setupModelPaths(config.language)
            
            // Validate configuration
            if (validateConfiguration()) {
                isConfigured = true
                Log.i(TAG, "Vivoka configuration initialized successfully")
                Log.d(TAG, "Assets path: $assetsPath")
                Log.d(TAG, "Config path: $configPath")
                Log.d(TAG, "Model path: $modelPath")
                Log.d(TAG, "Dictation model path: $dictationModelPath")
                true
            } else {
                Log.e(TAG, "Configuration validation failed")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize configuration", e)
            false
        }
    }
    
    /**
     * Setup asset and configuration paths
     */
    private fun setupAssetPaths() {
        assetsPath = "${context.filesDir.absolutePath}/$ASSETS_SUBDIR"
        configPath = File(assetsPath, VSDK_CONFIG).absolutePath
        
        Log.d(TAG, "Asset paths configured - assets: $assetsPath, config: $configPath")
    }
    
    /**
     * Setup model paths based on language configuration
     */
    private fun setupModelPaths(language: String) {
        modelPath = getModelPath(language)
        dictationModelPath = getDictationModelPath(language)
        
        Log.d(TAG, "Model paths configured for language '$language'")
        Log.d(TAG, "Command model: $modelPath")
        Log.d(TAG, "Dictation model: $dictationModelPath")
    }
    
    /**
     * Validate the current configuration
     */
    private fun validateConfiguration(): Boolean {
        // Check if paths are set
        if (assetsPath.isEmpty() || configPath.isEmpty() || modelPath.isEmpty()) {
            Log.e(TAG, "Invalid paths in configuration")
            return false
        }
        
        // Check if speech config is properly initialized
        if (!::speechConfig.isInitialized) {
            Log.e(TAG, "SpeechConfig not initialized")
            return false
        }
        
        // Validate language support
        if (!isSupportedLanguage(speechConfig.language)) {
            Log.w(TAG, "Language ${speechConfig.language} may not be fully supported, using English fallback")
        }
        
        return true
    }
    
    /**
     * Get ASR model name for language
     */
    fun getAsrModelName(language: String): String {
        return when (language.lowercase()) {
            "en", "en-us", "en_us" -> "asr_eng"
            "fr", "fr-fr", "fr_fr" -> "asr_fra"
            "de", "de-de", "de_de" -> "asr_deu"
            "es", "es-es", "es_es" -> "asr_spa"
            "it", "it-it", "it_it" -> "asr_ita"
            "pt", "pt-pt", "pt_pt" -> "asr_por"
            "nl", "nl-nl", "nl_nl" -> "asr_nld"
            "ru", "ru-ru", "ru_ru" -> "asr_rus"
            "zh", "zh-cn", "zh_cn" -> "asr_chn"
            "ja", "ja-jp", "ja_jp" -> "asr_jpn"
            else -> {
                Log.w(TAG, "Unsupported language $language, defaulting to English")
                "asr_eng"
            }
        }
    }
    
    /**
     * Get model path for language
     */
    private fun getModelPath(language: String): String {
        return when (language.lowercase()) {
            "en", "en-us", "en_us" -> "VoxMobileVoiceASR_en-US_v2_3_3"
            "fr", "fr-fr", "fr_fr" -> "VoxMobileVoiceASR_fr-FR_v2_3_3"
            "de", "de-de", "de_de" -> "VoxMobileVoiceASR_de-DE_v2_3_3"
            "es", "es-es", "es_es" -> "VoxMobileVoiceASR_es-ES_v2_3_3"
            "it", "it-it", "it_it" -> "VoxMobileVoiceASR_it-IT_v2_3_3"
            "pt", "pt-pt", "pt_pt" -> "VoxMobileVoiceASR_pt-PT_v2_3_3"
            "nl", "nl-nl", "nl_nl" -> "VoxMobileVoiceASR_nl-NL_v2_3_3"
            "ru", "ru-ru", "ru_ru" -> "VoxMobileVoiceASR_ru-RU_v2_3_3"
            "zh", "zh-cn", "zh_cn" -> "VoxMobileVoiceASR_zh-CN_v2_3_3"
            "ja", "ja-jp", "ja_jp" -> "VoxMobileVoiceASR_ja-JP_v2_3_3"
            else -> {
                Log.w(TAG, "Unsupported language $language, defaulting to English model")
                "VoxMobileVoiceASR_en-US_v2_3_3"
            }
        }
    }
    
    /**
     * Get dictation model path for language
     */
    private fun getDictationModelPath(language: String): String {
        return when (language.lowercase()) {
            "en", "en-us", "en_us" -> "VoxMobileVoiceASR_en-US_Freespeech_v2_3_3"
            "fr", "fr-fr", "fr_fr" -> "VoxMobileVoiceASR_fr-FR_Freespeech_v2_3_3"
            "de", "de-de", "de_de" -> "VoxMobileVoiceASR_de-DE_Freespeech_v2_3_3"
            "es", "es-es", "es_es" -> "VoxMobileVoiceASR_es-ES_Freespeech_v2_3_3"
            "it", "it-it", "it_it" -> "VoxMobileVoiceASR_it-IT_Freespeech_v2_3_3"
            "pt", "pt-pt", "pt_pt" -> "VoxMobileVoiceASR_pt-PT_Freespeech_v2_3_3"
            "nl", "nl-nl", "nl_nl" -> "VoxMobileVoiceASR_nl-NL_Freespeech_v2_3_3"
            "ru", "ru-ru", "ru_ru" -> "VoxMobileVoiceASR_ru-RU_Freespeech_v2_3_3"
            "zh", "zh-cn", "zh_cn" -> "VoxMobileVoiceASR_zh-CN_Freespeech_v2_3_3"
            "ja", "ja-jp", "ja_jp" -> "VoxMobileVoiceASR_ja-JP_Freespeech_v2_3_3"
            else -> {
                Log.w(TAG, "Unsupported language $language, defaulting to English dictation model")
                "VoxMobileVoiceASR_en-US_Freespeech_v2_3_3"
            }
        }
    }
    
    /**
     * Check if language is supported
     */
    private fun isSupportedLanguage(language: String): Boolean {
        val supportedLanguages = setOf(
            "en", "en-us", "en_us",
            "fr", "fr-fr", "fr_fr",
            "de", "de-de", "de_de",
            "es", "es-es", "es_es",
            "it", "it-it", "it_it",
            "pt", "pt-pt", "pt_pt",
            "nl", "nl-nl", "nl_nl",
            "ru", "ru-ru", "ru_ru",
            "zh", "zh-cn", "zh_cn",
            "ja", "ja-jp", "ja_jp"
        )
        return supportedLanguages.contains(language.lowercase())
    }
    
    /**
     * Get dictation timeout in milliseconds with validation
     */
    fun getDictationTimeout(): Long {
        if (!isConfigured) {
            Log.w(TAG, "Configuration not initialized, using default timeout")
            return 2000L
        }
        
        // Ensure timeout is between 1-10 seconds, default to 2 seconds
        val timeoutSeconds = (speechConfig.dictationTimeout / 1000).toInt()
        return if (timeoutSeconds in 1..10) {
            speechConfig.dictationTimeout
        } else {
            Log.w(TAG, "Invalid dictation timeout ${speechConfig.dictationTimeout}ms, using default 2000ms")
            2000L // Default 2 seconds
        }
    }
    
    // Getters for configuration values
    fun getSpeechConfig(): SpeechConfig = speechConfig
    fun getAssetsPath(): String = assetsPath
    fun getConfigPath(): String = configPath
    fun getModelPath(): String = modelPath
    fun getDictationModelPath(): String = dictationModelPath
    fun isInitialized(): Boolean = isConfigured
    
    /**
     * Update language configuration at runtime
     */
    fun updateLanguage(newLanguage: String): Boolean {
        return try {
            if (!isConfigured) {
                Log.e(TAG, "Cannot update language - configuration not initialized")
                return false
            }
            
            Log.i(TAG, "Updating language from ${speechConfig.language} to $newLanguage")
            
            // Update speech config
            speechConfig = speechConfig.copy(language = newLanguage)
            
            // Update model paths
            setupModelPaths(newLanguage)
            
            Log.i(TAG, "Language updated successfully to $newLanguage")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update language", e)
            false
        }
    }
    
    /**
     * Reset configuration to uninitialized state
     */
    fun reset() {
        Log.d(TAG, "Resetting configuration")
        isConfigured = false
        assetsPath = ""
        configPath = ""
        modelPath = ""
        dictationModelPath = ""
    }
    
    /**
     * Get configuration summary for logging/debugging
     */
    fun getConfigSummary(): Map<String, Any> {
        return mapOf(
            "isConfigured" to isConfigured,
            "language" to (if (isConfigured) speechConfig.language else "not_set"),
            "assetsPath" to assetsPath,
            "modelPath" to modelPath,
            "dictationModelPath" to dictationModelPath,
            "voiceEnabled" to (if (isConfigured) speechConfig.voiceEnabled else false),
            "confidenceThreshold" to (if (isConfigured) speechConfig.confidenceThreshold else 0f),
            "dictationTimeout" to (if (isConfigured) getDictationTimeout() else 0L)
        )
    }
}