/**
 * AndroidConfig.kt - Configuration management for AndroidSTTEngine
 * 
 * Extracted from AndroidSTTEngine as part of SOLID refactoring
 * Handles all configuration-related logic including:
 * - Configuration validation
 * - Language mapping
 * - Timeout management
 * - Voice state configuration
 * 
 * © Augmentalis Inc, Intelligent Devices LLC, Manoj Jhawar, Aman Jhawar
 */
package com.augmentalis.voiceos.speech.engines.android

import android.content.Context
import android.util.Log
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechMode
import java.util.Locale

/**
 * Manages configuration settings and validation for AndroidSTTEngine.
 * Provides centralized configuration management with validation and defaults.
 */
class AndroidConfig(private val context: Context) {
    
    companion object {
        private const val TAG = "AndroidConfig"
        private const val DEFAULT_TIMEOUT_MINUTES = 5
        private const val DEFAULT_DICTATION_TIMEOUT = 2000
        private const val MIN_DICTATION_TIMEOUT = 1000
        private const val MAX_DICTATION_TIMEOUT = 10000
        
        // Language code mapping (BCP-47 tags)
        private val LANGUAGE_MAP = mapOf(
            "en-US" to "en-US",
            "en-GB" to "en-GB",
            "en-AU" to "en-AU",
            "en-CA" to "en-CA",
            "en-IN" to "en-IN",
            "fr-FR" to "fr-FR",
            "fr-CA" to "fr-CA",
            "de-DE" to "de-DE",
            "es-ES" to "es-ES",
            "es-MX" to "es-MX",
            "es-AR" to "es-AR",
            "it-IT" to "it-IT",
            "ja-JP" to "ja-JP",
            "ko-KR" to "ko-KR",
            "zh-CN" to "zh-CN",
            "zh-TW" to "zh-TW",
            "zh-HK" to "zh-HK",
            "pt-BR" to "pt-BR",
            "pt-PT" to "pt-PT",
            "ru-RU" to "ru-RU",
            "nl-NL" to "nl-NL",
            "pl-PL" to "pl-PL",
            "hi-IN" to "hi-IN",
            "ar-SA" to "ar-001",
            "th-TH" to "th-TH",
            "sv-SE" to "sv-SE",
            "da-DK" to "da-DK",
            "no-NO" to "nb-NO",
            "fi-FI" to "fi-FI",
            "tr-TR" to "tr-TR",
            "el-GR" to "el-GR",
            "he-IL" to "iw-IL",
            "hu-HU" to "hu-HU",
            "cs-CZ" to "cs-CZ",
            "sk-SK" to "sk-SK",
            "ro-RO" to "ro-RO",
            "uk-UA" to "uk-UA",
            "bg-BG" to "bg-BG",
            "hr-HR" to "hr-HR",
            "sr-RS" to "sr-RS",
            "sl-SI" to "sl-SI",
            "lt-LT" to "lt-LT",
            "lv-LV" to "lv-LV",
            "et-EE" to "et-EE",
            "vi-VN" to "vi-VN",
            "id-ID" to "id-ID",
            "ms-MY" to "ms-MY",
            "bn-IN" to "bn-IN",
            "ta-IN" to "ta-IN",
            "te-IN" to "te-IN",
            "ml-IN" to "ml-IN",
            "kn-IN" to "kn-IN",
            "gu-IN" to "gu-IN",
            "mr-IN" to "mr-IN"
        )
    }
    
    // Configuration data
    private var currentConfig: SpeechConfig? = null
    private var speechMode = SpeechMode.DYNAMIC_COMMAND
    private var languageBcpTag: String = "en-US"
    
    // Voice state configuration
    private var isVoiceEnabled = false
    private var isVoiceSleeping = false
    private var isDictationActive = false
    private var voiceTimeoutMinutes = DEFAULT_TIMEOUT_MINUTES
    private var dictationTimeout = DEFAULT_DICTATION_TIMEOUT
    
    /**
     * Initialize and validate configuration
     */
    fun initialize(config: SpeechConfig): Boolean {
        Log.i(TAG, "Initializing AndroidConfig")
        
        // Validate required commands
        if (!validateConfig(config)) {
            Log.e(TAG, "Configuration validation failed")
            return false
        }
        
        // Store validated config
        currentConfig = config
        
        // Map language
        languageBcpTag = LANGUAGE_MAP[config.language] ?: config.language
        Log.d(TAG, "Language mapped: ${config.language} -> $languageBcpTag")
        
        // Set timeout values
        voiceTimeoutMinutes = config.voiceTimeoutMinutes.toInt()
        dictationTimeout = validateDictationTimeout(config.dictationTimeout.toInt())
        
        // Reset voice state
        isVoiceEnabled = false
        isVoiceSleeping = false
        isDictationActive = false
        speechMode = SpeechMode.DYNAMIC_COMMAND
        
        Log.i(TAG, "AndroidConfig initialized successfully")
        return true
    }
    
    /**
     * Validate configuration parameters
     */
    private fun validateConfig(config: SpeechConfig): Boolean {
        val errors = mutableListOf<String>()
        
        if (config.muteCommand.isBlank()) {
            errors.add("muteCommand cannot be empty")
        }
        
        if (config.unmuteCommand.isBlank()) {
            errors.add("unmuteCommand cannot be empty")
        }
        
        if (config.startDictationCommand.isBlank()) {
            errors.add("startDictationCommand cannot be empty")
        }
        
        if (config.stopDictationCommand.isBlank()) {
            errors.add("stopDictationCommand cannot be empty")
        }
        
        if (config.voiceTimeoutMinutes < 1 || config.voiceTimeoutMinutes > 60) {
            errors.add("voiceTimeoutMinutes must be between 1 and 60")
        }
        
        if (errors.isNotEmpty()) {
            Log.e(TAG, "Configuration validation errors: ${errors.joinToString("; ")}")
            return false
        }
        
        return true
    }
    
    /**
     * Validate and normalize dictation timeout
     */
    private fun validateDictationTimeout(timeout: Int): Int {
        return when {
            timeout < MIN_DICTATION_TIMEOUT -> {
                Log.w(TAG, "Dictation timeout too low ($timeout), using minimum ($MIN_DICTATION_TIMEOUT)")
                MIN_DICTATION_TIMEOUT
            }
            timeout > MAX_DICTATION_TIMEOUT -> {
                Log.w(TAG, "Dictation timeout too high ($timeout), using maximum ($MAX_DICTATION_TIMEOUT)")
                MAX_DICTATION_TIMEOUT
            }
            else -> timeout
        }
    }
    
    // Getters for configuration values
    fun getConfig(): SpeechConfig? = currentConfig
    fun getLanguage(): String = languageBcpTag
    fun getSpeechMode(): SpeechMode = speechMode
    fun getVoiceTimeoutMinutes(): Int = voiceTimeoutMinutes
    fun getDictationTimeoutMs(): Int = dictationTimeout
    
    // Voice state management
    fun isVoiceEnabled(): Boolean = isVoiceEnabled
    fun setVoiceEnabled(enabled: Boolean) { isVoiceEnabled = enabled }
    
    fun isVoiceSleeping(): Boolean = isVoiceSleeping
    fun setVoiceSleeping(sleeping: Boolean) { isVoiceSleeping = sleeping }
    
    fun isDictationActive(): Boolean = isDictationActive
    fun setDictationActive(active: Boolean) { isDictationActive = active }
    
    fun setSpeechMode(mode: SpeechMode) { speechMode = mode }
    
    // Command checking
    fun isMuteCommand(command: String): Boolean {
        return currentConfig?.let { 
            it.muteCommand.equals(command, ignoreCase = true)
        } ?: false
    }
    
    fun isUnmuteCommand(command: String): Boolean {
        return currentConfig?.let { 
            it.unmuteCommand.equals(command, ignoreCase = true)
        } ?: false
    }
    
    fun isStartDictationCommand(command: String): Boolean {
        return currentConfig?.let { 
            it.startDictationCommand.equals(command, ignoreCase = true)
        } ?: false
    }
    
    fun isStopDictationCommand(command: String): Boolean {
        return currentConfig?.let { 
            it.stopDictationCommand.equals(command, ignoreCase = true)
        } ?: false
    }
    
    // Language support
    fun getSupportedLanguages(): List<String> = LANGUAGE_MAP.keys.toList()
    
    fun isLanguageSupported(language: String): Boolean = LANGUAGE_MAP.containsKey(language)
    
    /**
     * Get configuration summary for logging
     */
    fun getConfigSummary(): String {
        return currentConfig?.let { config ->
            """AndroidConfig Summary:
                ├── Language: ${config.language} -> $languageBcpTag
                ├── Mode: $speechMode
                ├── Voice timeout: ${voiceTimeoutMinutes}min
                ├── Dictation timeout: ${dictationTimeout}ms
                ├── Mute command: "${config.muteCommand}"
                ├── Unmute command: "${config.unmuteCommand}"
                ├── Start dictation: "${config.startDictationCommand}"
                └── Stop dictation: "${config.stopDictationCommand}"
            """.trimIndent()
        } ?: "No configuration loaded"
    }
    
    /**
     * Reset configuration to defaults
     */
    fun reset() {
        currentConfig = null
        speechMode = SpeechMode.DYNAMIC_COMMAND
        languageBcpTag = "en-US"
        isVoiceEnabled = false
        isVoiceSleeping = false
        isDictationActive = false
        voiceTimeoutMinutes = DEFAULT_TIMEOUT_MINUTES
        dictationTimeout = DEFAULT_DICTATION_TIMEOUT
        
        Log.d(TAG, "AndroidConfig reset to defaults")
    }
}