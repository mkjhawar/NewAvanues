/**
 * SpeechEngine.kt - Available speech recognition engines
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * KMP migration of VoiceOSCore SpeechEngine enum.
 * Defines all supported speech recognition engines across platforms.
 */
package com.augmentalis.voiceoscoreng.speech

/**
 * Available speech recognition engines.
 *
 * Platform availability:
 * - VOSK: Android, Desktop (offline)
 * - ANDROID_STT: Android only (online)
 * - GOOGLE_CLOUD: All platforms (online, requires API key)
 * - WHISPER: All platforms (offline, requires model)
 * - AZURE: All platforms (online, requires subscription)
 * - APPLE_SPEECH: iOS/macOS only (native)
 * - VIVOKA: Android only (hybrid, commercial license)
 */
enum class SpeechEngine {
    /**
     * VOSK - Open-source offline speech recognition
     * - Offline capable
     * - Multiple language models
     * - ~30MB memory usage
     * - Platforms: Android, Desktop
     */
    VOSK,

    /**
     * Android STT - Android's built-in native speech-to-text
     * - Online only (requires Google services)
     * - Uses device's native SpeechRecognizer
     * - ~20MB memory usage
     * - Platforms: Android only
     */
    ANDROID_STT,

    /**
     * Google Cloud - Google Cloud Speech API
     * - Online only
     * - Advanced features
     * - Requires API key
     * - ~15MB memory usage
     * - Platforms: All
     */
    GOOGLE_CLOUD,

    /**
     * Whisper - OpenAI Whisper speech recognition
     * - Offline capable
     * - Multiple model sizes (tiny to large)
     * - Advanced features: language detection, translation, word timestamps
     * - GPU acceleration support
     * - ~39MB to 1550MB depending on model size
     * - Platforms: All (requires native bindings)
     */
    WHISPER,

    /**
     * Azure - Microsoft Azure Cognitive Services Speech Recognition
     * - Online only (cloud-based)
     * - Enterprise-grade accuracy
     * - Advanced features: speaker recognition, translation, custom models
     * - Requires Azure subscription key and region
     * - ~10MB memory usage
     * - Platforms: All
     */
    AZURE,

    /**
     * Apple Speech - Native iOS/macOS speech recognition
     * - Uses Speech.framework
     * - Online and offline modes
     * - Native integration with iOS/macOS
     * - ~25MB memory usage
     * - Platforms: iOS, macOS only
     */
    APPLE_SPEECH,

    /**
     * Vivoka - VSDK-based hybrid recognition
     * - Offline/Online hybrid
     * - Wake word support
     * - ~60MB memory usage
     * - Platforms: Android only (commercial license)
     */
    VIVOKA;

    /**
     * Check if engine is offline capable
     */
    fun isOfflineCapable(): Boolean = this in listOf(VOSK, WHISPER, APPLE_SPEECH, VIVOKA)

    /**
     * Check if engine requires API key
     */
    fun requiresApiKey(): Boolean = this in listOf(GOOGLE_CLOUD, AZURE)

    /**
     * Check if engine requires commercial license
     */
    fun requiresCommercialLicense(): Boolean = this == VIVOKA

    /**
     * Get human-readable display name
     */
    fun getDisplayName(): String = when (this) {
        VOSK -> "VOSK Offline"
        ANDROID_STT -> "Android STT"
        GOOGLE_CLOUD -> "Google Cloud Speech"
        WHISPER -> "OpenAI Whisper"
        AZURE -> "Azure Cognitive Services"
        APPLE_SPEECH -> "Apple Speech"
        VIVOKA -> "Vivoka VSDK"
    }

    /**
     * Get estimated memory usage in MB
     */
    fun getEstimatedMemoryUsage(): Int = when (this) {
        VOSK -> 30
        ANDROID_STT -> 20
        GOOGLE_CLOUD -> 15
        WHISPER -> 230  // Base model, varies 39-1550MB
        AZURE -> 10
        APPLE_SPEECH -> 25
        VIVOKA -> 60
    }

    companion object {
        /**
         * Get engines available on Android platform
         */
        fun androidEngines(): List<SpeechEngine> = listOf(
            VOSK, ANDROID_STT, GOOGLE_CLOUD, WHISPER, AZURE, VIVOKA
        )

        /**
         * Get engines available on iOS platform
         */
        fun iosEngines(): List<SpeechEngine> = listOf(
            APPLE_SPEECH, GOOGLE_CLOUD, AZURE, WHISPER
        )

        /**
         * Get engines available on Desktop platforms
         */
        fun desktopEngines(): List<SpeechEngine> = listOf(
            VOSK, GOOGLE_CLOUD, WHISPER, AZURE
        )

        /**
         * Get default engine for platform
         */
        fun defaultForPlatform(platform: Platform): SpeechEngine = when (platform) {
            Platform.ANDROID -> ANDROID_STT
            Platform.IOS -> APPLE_SPEECH
            Platform.MACOS -> APPLE_SPEECH
            Platform.WINDOWS -> VOSK
            Platform.LINUX -> VOSK
        }
    }
}

/**
 * Platform identifiers for engine selection
 */
enum class Platform {
    ANDROID,
    IOS,
    MACOS,
    WINDOWS,
    LINUX
}
