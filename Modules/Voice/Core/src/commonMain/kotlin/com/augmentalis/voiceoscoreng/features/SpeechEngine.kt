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
package com.augmentalis.voiceoscoreng.features

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
    fun isOfflineCapable(): Boolean =
        SpeechEngineRegistry.getCapabilities(this)?.isOfflineCapable ?: false

    /**
     * Check if engine requires API key
     */
    fun requiresApiKey(): Boolean =
        SpeechEngineRegistry.getCapabilities(this)?.requiresApiKey ?: false

    /**
     * Check if engine requires commercial license
     */
    fun requiresCommercialLicense(): Boolean =
        SpeechEngineRegistry.getCapabilities(this)?.requiresCommercialLicense ?: false

    /**
     * Get human-readable display name
     */
    fun getDisplayName(): String =
        SpeechEngineRegistry.getCapabilities(this)?.displayName ?: name

    /**
     * Get estimated memory usage in MB
     */
    fun getEstimatedMemoryUsage(): Int =
        SpeechEngineRegistry.getCapabilities(this)?.memoryUsageMB ?: 50

    companion object {
        /**
         * Get engines available on Android platform
         */
        fun androidEngines(): List<SpeechEngine> =
            SpeechEngineRegistry.getEnginesForPlatform(Platform.ANDROID)

        /**
         * Get engines available on iOS platform
         */
        fun iosEngines(): List<SpeechEngine> =
            SpeechEngineRegistry.getEnginesForPlatform(Platform.IOS)

        /**
         * Get engines available on Desktop platforms
         */
        fun desktopEngines(): List<SpeechEngine> =
            SpeechEngineRegistry.getEnginesForPlatform(Platform.WINDOWS) +
            SpeechEngineRegistry.getEnginesForPlatform(Platform.LINUX)

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
 * Speech engine capabilities configuration.
 * Allows extending engine properties without modifying the enum.
 */
data class SpeechEngineCapabilities(
    val isOfflineCapable: Boolean,
    val requiresApiKey: Boolean,
    val requiresCommercialLicense: Boolean,
    val displayName: String,
    val memoryUsageMB: Int,
    val supportedPlatforms: Set<Platform>
)

/**
 * Registry for speech engine capabilities.
 * Open for extension - new capabilities can be registered without modifying the enum.
 */
object SpeechEngineRegistry {
    private val capabilities = mutableMapOf<SpeechEngine, SpeechEngineCapabilities>()

    init {
        // Register default capabilities
        register(SpeechEngine.VOSK, SpeechEngineCapabilities(
            isOfflineCapable = true,
            requiresApiKey = false,
            requiresCommercialLicense = false,
            displayName = "VOSK Offline",
            memoryUsageMB = 30,
            supportedPlatforms = setOf(Platform.ANDROID, Platform.WINDOWS, Platform.LINUX)
        ))
        register(SpeechEngine.ANDROID_STT, SpeechEngineCapabilities(
            isOfflineCapable = false,
            requiresApiKey = false,
            requiresCommercialLicense = false,
            displayName = "Android STT",
            memoryUsageMB = 20,
            supportedPlatforms = setOf(Platform.ANDROID)
        ))
        register(SpeechEngine.GOOGLE_CLOUD, SpeechEngineCapabilities(
            isOfflineCapable = false,
            requiresApiKey = true,
            requiresCommercialLicense = false,
            displayName = "Google Cloud Speech",
            memoryUsageMB = 15,
            supportedPlatforms = Platform.entries.toSet()
        ))
        register(SpeechEngine.WHISPER, SpeechEngineCapabilities(
            isOfflineCapable = true,
            requiresApiKey = false,
            requiresCommercialLicense = false,
            displayName = "OpenAI Whisper",
            memoryUsageMB = 230,
            supportedPlatforms = Platform.entries.toSet()
        ))
        register(SpeechEngine.AZURE, SpeechEngineCapabilities(
            isOfflineCapable = false,
            requiresApiKey = true,
            requiresCommercialLicense = false,
            displayName = "Azure Cognitive Services",
            memoryUsageMB = 10,
            supportedPlatforms = Platform.entries.toSet()
        ))
        register(SpeechEngine.APPLE_SPEECH, SpeechEngineCapabilities(
            isOfflineCapable = true,
            requiresApiKey = false,
            requiresCommercialLicense = false,
            displayName = "Apple Speech",
            memoryUsageMB = 25,
            supportedPlatforms = setOf(Platform.IOS, Platform.MACOS)
        ))
        register(SpeechEngine.VIVOKA, SpeechEngineCapabilities(
            isOfflineCapable = true,
            requiresApiKey = false,
            requiresCommercialLicense = true,
            displayName = "Vivoka VSDK",
            memoryUsageMB = 60,
            supportedPlatforms = setOf(Platform.ANDROID)
        ))
    }

    /**
     * Register capabilities for an engine.
     * Allows overriding default capabilities or adding new engines.
     */
    fun register(engine: SpeechEngine, capabilities: SpeechEngineCapabilities) {
        this.capabilities[engine] = capabilities
    }

    /**
     * Get capabilities for an engine.
     */
    fun getCapabilities(engine: SpeechEngine): SpeechEngineCapabilities? = capabilities[engine]

    /**
     * Get all registered engines.
     */
    fun getAllEngines(): Set<SpeechEngine> = capabilities.keys

    /**
     * Get engines for a specific platform.
     */
    fun getEnginesForPlatform(platform: Platform): List<SpeechEngine> =
        capabilities.filter { platform in it.value.supportedPlatforms }.keys.toList()

    /**
     * Get offline-capable engines.
     */
    fun getOfflineEngines(): List<SpeechEngine> =
        capabilities.filter { it.value.isOfflineCapable }.keys.toList()
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
