/**
 * AndroidSTTConfig.kt - Configuration management for Android STT Engine
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 * Updated: 2026-01-18 - Migrated to KMP androidMain
 */
package com.augmentalis.speechrecognition

import android.content.Context
import android.util.Log

/**
 * Manages configuration settings for the Android STT Engine.
 * Provides validation, language mapping, and voice state management.
 */
class AndroidSTTConfig(private val context: Context) {

    companion object {
        private const val TAG = "AndroidSTTConfig"
        private const val DEFAULT_TIMEOUT_MINUTES = 5
        private const val DEFAULT_DICTATION_TIMEOUT = 2000
        private const val MIN_DICTATION_TIMEOUT = 1000
        private const val MAX_DICTATION_TIMEOUT = 10000

        // BCP-47 language mapping
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
            "it-IT" to "it-IT",
            "ja-JP" to "ja-JP",
            "ko-KR" to "ko-KR",
            "zh-CN" to "zh-CN",
            "zh-TW" to "zh-TW",
            "pt-BR" to "pt-BR",
            "ru-RU" to "ru-RU",
            "nl-NL" to "nl-NL",
            "hi-IN" to "hi-IN",
            "ar-SA" to "ar-001",
            "tr-TR" to "tr-TR",
            "vi-VN" to "vi-VN",
            "th-TH" to "th-TH",
            "id-ID" to "id-ID",
            "ms-MY" to "ms-MY"
        )
    }

    // Current configuration
    private var currentConfig: SpeechConfig? = null
    private var speechMode = SpeechMode.DYNAMIC_COMMAND
    private var languageBcpTag: String = "en-US"

    // Voice state
    private var isVoiceEnabled = false
    private var isVoiceSleeping = false
    private var isDictationActive = false
    private var voiceTimeoutMinutes = DEFAULT_TIMEOUT_MINUTES
    private var dictationTimeout = DEFAULT_DICTATION_TIMEOUT

    /**
     * Initialize configuration
     */
    fun initialize(config: SpeechConfig): Boolean {
        Log.i(TAG, "Initializing AndroidSTTConfig")

        // Validate configuration
        if (!validateConfig(config)) {
            Log.e(TAG, "Configuration validation failed")
            return false
        }

        currentConfig = config

        // Map language
        languageBcpTag = LANGUAGE_MAP[config.language] ?: config.language
        Log.d(TAG, "Language mapped: ${config.language} -> $languageBcpTag")

        // Set timeouts
        voiceTimeoutMinutes = config.voiceTimeoutMinutes.toInt()
        dictationTimeout = validateDictationTimeout(config.dictationTimeout.toInt())

        // Reset voice state
        isVoiceEnabled = false
        isVoiceSleeping = false
        isDictationActive = false
        speechMode = config.mode

        Log.i(TAG, "AndroidSTTConfig initialized")
        return true
    }

    /**
     * Validate configuration
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
            Log.e(TAG, "Validation errors: ${errors.joinToString("; ")}")
            return false
        }

        return true
    }

    /**
     * Validate dictation timeout
     */
    private fun validateDictationTimeout(timeout: Int): Int {
        return when {
            timeout < MIN_DICTATION_TIMEOUT -> {
                Log.w(TAG, "Dictation timeout too low, using minimum")
                MIN_DICTATION_TIMEOUT
            }
            timeout > MAX_DICTATION_TIMEOUT -> {
                Log.w(TAG, "Dictation timeout too high, using maximum")
                MAX_DICTATION_TIMEOUT
            }
            else -> timeout
        }
    }

    // Getters
    fun getConfig(): SpeechConfig? = currentConfig
    fun getLanguage(): String = languageBcpTag
    fun getSpeechMode(): SpeechMode = speechMode
    fun getVoiceTimeoutMinutes(): Int = voiceTimeoutMinutes
    fun getDictationTimeoutMs(): Int = dictationTimeout

    // Voice state
    fun isVoiceEnabled(): Boolean = isVoiceEnabled
    fun setVoiceEnabled(enabled: Boolean) { isVoiceEnabled = enabled }

    fun isVoiceSleeping(): Boolean = isVoiceSleeping
    fun setVoiceSleeping(sleeping: Boolean) { isVoiceSleeping = sleeping }

    fun isDictationActive(): Boolean = isDictationActive
    fun setDictationActive(active: Boolean) { isDictationActive = active }

    fun setSpeechMode(mode: SpeechMode) { speechMode = mode }

    // Command checking
    fun isMuteCommand(command: String): Boolean {
        return currentConfig?.muteCommand?.equals(command, ignoreCase = true) ?: false
    }

    fun isUnmuteCommand(command: String): Boolean {
        return currentConfig?.unmuteCommand?.equals(command, ignoreCase = true) ?: false
    }

    fun isStartDictationCommand(command: String): Boolean {
        return currentConfig?.startDictationCommand?.equals(command, ignoreCase = true) ?: false
    }

    fun isStopDictationCommand(command: String): Boolean {
        return currentConfig?.stopDictationCommand?.equals(command, ignoreCase = true) ?: false
    }

    // Language support
    fun getSupportedLanguages(): List<String> = LANGUAGE_MAP.keys.toList()
    fun isLanguageSupported(language: String): Boolean = LANGUAGE_MAP.containsKey(language)

    /**
     * Get configuration summary
     */
    fun getConfigSummary(): String {
        return currentConfig?.let { config ->
            """AndroidSTTConfig:
                ├── Language: ${config.language} -> $languageBcpTag
                ├── Mode: $speechMode
                ├── Voice timeout: ${voiceTimeoutMinutes}min
                ├── Dictation timeout: ${dictationTimeout}ms
                ├── Mute: "${config.muteCommand}"
                ├── Unmute: "${config.unmuteCommand}"
                └── Voice enabled: $isVoiceEnabled
            """.trimIndent()
        } ?: "No configuration loaded"
    }

    /**
     * Reset to defaults
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
        Log.d(TAG, "AndroidSTTConfig reset")
    }
}
