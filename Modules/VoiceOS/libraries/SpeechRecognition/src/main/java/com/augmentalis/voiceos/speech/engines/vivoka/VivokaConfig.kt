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
import com.augmentalis.voiceos.speech.engines.vivoka.model.Model
import com.augmentalis.voiceos.speech.engines.vivoka.model.Root
import com.augmentalis.voiceos.speech.engines.vivoka.model.VivokaLanguageRepository
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

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

    // Path resolver for multi-location fallback
    private val pathResolver = VivokaPathResolver(context)

    // Configuration data
    private lateinit var speechConfig: SpeechConfig
    private var assetsPath: String = ""
    private var configPath: String = ""
    private var modelPath: String = ""

    var dynamicCommandLanguage: String = "en"
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
            dynamicCommandLanguage = config.language
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
     * Setup asset and configuration paths with multi-location fallback
     * Checks external folders before defaulting to internal storage
     */
    private fun setupAssetPaths() {
        // Use path resolver to check multiple locations
        Log.d(TAG, "Searching for VSDK in locations:\n${pathResolver.getSearchPathsForLogging()}" )

        val vsdkDir = pathResolver.resolveVsdkPath()
        assetsPath = vsdkDir.absolutePath

        // Fix: vsdk.json is located in config/ subdirectory
        configPath = File(assetsPath, "config/$VSDK_CONFIG").absolutePath

        Log.i(TAG, "Asset paths configured - assets: $assetsPath" )
        Log.i(TAG, "Config path: $configPath" )

        // Log if using external fallback
        if (!assetsPath.contains(context.filesDir.absolutePath)) {
            Log.i(TAG, "Using EXTERNAL VSDK location (pre-deployed or fallback)" )
        }
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
        return VivokaLanguageRepository.getAsr(language)
    }
    
    /**
     * Get model path for language
     */
    private fun getModelPath(language: String): String {
        return VivokaLanguageRepository.getModelAsr(language).first()
    }
    
    /**
     * Get dictation model path for language
     */
    private fun getDictationModelPath(language: String): String {
        return VivokaLanguageRepository.getDictationLanguage(language).first()
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
            dynamicCommandLanguage = speechConfig.language
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

    fun mergeJsonFiles(downloadedFile: String): String? {
        try {// Step 1: Read the existing JSON from internal storage
            val vsdkFile = File(configPath)
            val vsdkContent = FileReader(vsdkFile).readText()
            val gson = Gson()
            // Load and parse local english file from assets
            val vsdkContentRoot = gson.fromJson(vsdkContent, Root::class.java)

            Log.i(TAG, "mergeJsonFiles: vsdkContentRoot = $vsdkContentRoot")

            // Load and parse download files
            val downloadRoot = gson.fromJson(downloadedFile, Root::class.java)

            Log.i(TAG, "mergeJsonFiles: frenchRoot = $downloadRoot")
            // Merge acmods
            vsdkContentRoot.csdk.asr.recognizers.rec.acmods.addAll(downloadRoot.csdk.asr.recognizers.rec.acmods)

            // Merge models from French and Spanish into English
            downloadRoot.csdk.asr.models.forEach { (name, value) ->
                value.let {
                    vsdkContentRoot.csdk.asr.models[name] = mergeModels(
                        vsdkContentRoot.csdk.asr.models[name],
                        it
                    )
                }
            }

            // Save the final merged JSON as final.json in internal storage
            val finalJsonString = gson.toJson(vsdkContentRoot)
            Log.i(TAG, "mergeJsonFiles: finalJsonString = $finalJsonString")
            // Step 4: Write the modified JSON back to the file
            FileOutputStream(vsdkFile).use {
                it.write(finalJsonString.toByteArray())  // The 4 spaces indentation makes it nicely formatted
            }
            return vsdkFile.path
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Helper function to merge two Model objects
    private fun mergeModels(existing: Model?, newModel: Model): Model {
        val mergedExtraModels = (existing?.extraModels ?: mutableMapOf()).apply {
            putAll(newModel.extraModels ?: emptyMap())
        }
        val mergedSettings = (existing?.settings ?: mutableMapOf()).apply {
            putAll(newModel.settings ?: emptyMap())
        }
        return Model(
            type = newModel.type,
            file = newModel.file,
            acmod = newModel.acmod,
            extraModels = mergedExtraModels,
            settings = mergedSettings,
            slots = newModel.slots ?: existing?.slots,
            lexicon = newModel.lexicon ?: existing?.lexicon
        )
    }
}