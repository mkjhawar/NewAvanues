/**
 * SpeechEngine.kt - Speech recognition engine enumeration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-08-28
 * Updated: 2026-01-18 - Migrated to KMP commonMain
 */
package com.augmentalis.speechrecognition

/**
 * Available speech recognition engines.
 * Platform-specific engines may not be available on all platforms.
 */
enum class SpeechEngine {
    /**
     * VOSK - Open-source offline speech recognition
     * - Offline capable
     * - Multiple language models
     * - ~30MB memory usage
     * - Available: Android, Desktop
     */
    VOSK,

    /**
     * Vivoka - VSDK-based hybrid recognition
     * - Offline/Online hybrid
     * - Wake word support
     * - ~60MB memory usage
     * - Available: Android only
     */
    VIVOKA,

    /**
     * Android STT - Android's built-in native speech-to-text
     * - Online only (requires Google services)
     * - Uses device's native SpeechRecognizer
     * - ~20MB memory usage
     * - Available: Android only
     */
    ANDROID_STT,

    /**
     * Google Cloud - Google Cloud Speech API
     * - Online only
     * - Advanced features
     * - Requires API key
     * - ~15MB memory usage
     * - Available: All platforms
     */
    GOOGLE_CLOUD,

    /**
     * Whisper - OpenAI Whisper speech recognition
     * - Offline capable
     * - Multiple model sizes (tiny to large)
     * - Advanced features: language detection, translation, word timestamps
     * - GPU acceleration support
     * - ~39MB to 1550MB depending on model size
     * - Available: Android, Desktop
     */
    WHISPER,

    /**
     * Azure - Microsoft Azure Cognitive Services Speech Recognition
     * - Online only (cloud-based)
     * - Enterprise-grade accuracy
     * - Advanced features: speaker recognition, translation, custom models
     * - Requires Azure subscription key and region
     * - ~10MB memory usage
     * - Available: All platforms
     */
    AZURE,

    /**
     * Apple Speech - iOS/macOS native speech recognition
     * - Online/Offline hybrid
     * - Uses Apple Speech Framework
     * - Available: iOS, macOS only
     */
    APPLE_SPEECH,

    /**
     * Web Speech API - Browser-based speech recognition
     * - Online only
     * - Uses browser's native SpeechRecognition API
     * - Available: JS/Web only
     */
    WEB_SPEECH;

    /**
     * Check if engine is offline capable
     */
    fun isOfflineCapable(): Boolean {
        return this in listOf(VOSK, VIVOKA, WHISPER, APPLE_SPEECH)
    }

    /**
     * Check if engine requires API key
     */
    fun requiresApiKey(): Boolean {
        return this in listOf(GOOGLE_CLOUD, AZURE)
    }

    /**
     * Get human-readable name
     */
    fun getDisplayName(): String {
        return when (this) {
            VOSK -> "VOSK Offline"
            VIVOKA -> "Vivoka VSDK"
            ANDROID_STT -> "Android STT"
            GOOGLE_CLOUD -> "Google Cloud Speech"
            WHISPER -> "OpenAI Whisper"
            AZURE -> "Azure Cognitive Services"
            APPLE_SPEECH -> "Apple Speech"
            WEB_SPEECH -> "Web Speech API"
        }
    }

    /**
     * Get estimated memory usage in MB
     */
    fun getEstimatedMemoryUsage(): Int {
        return when (this) {
            VOSK -> 30
            VIVOKA -> 60
            ANDROID_STT -> 20
            GOOGLE_CLOUD -> 15
            WHISPER -> 230
            AZURE -> 10
            APPLE_SPEECH -> 25
            WEB_SPEECH -> 5
        }
    }
}
