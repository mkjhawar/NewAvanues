/**
 * SpeechConfig.kt - Speech recognition configuration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * KMP migration of VoiceOSCore SpeechConfig.
 * Unified configuration for all speech recognition engines.
 */
package com.augmentalis.voiceoscore

/**
 * Unified configuration for all speech recognition engines.
 *
 * Uses Kotlin data class features for immutability and easy modification.
 * Configuration is cross-platform - engine adapters interpret settings
 * appropriate to their capabilities.
 */
data class SpeechConfig(
    // ═══════════════════════════════════════════════════════════════════
    // Core Settings (ALL engines)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Recognition language (BCP-47 format)
     * Examples: "en-US", "es-ES", "de-DE", "ja-JP"
     */
    val language: String = DEFAULT_LANGUAGE,

    /**
     * Speech mode determining recognition behavior
     */
    val mode: SpeechMode = SpeechMode.DEFAULT,

    /**
     * Enable Voice Activity Detection
     * When true, engine detects speech start/end automatically
     */
    val enableVAD: Boolean = true,

    /**
     * Minimum confidence threshold (0.0 - 1.0)
     * Results below this are filtered out
     */
    val confidenceThreshold: Float = 0.7f,

    // ═══════════════════════════════════════════════════════════════════
    // Timing Settings
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Maximum recording duration in milliseconds
     * Recognition stops after this time even if speech continues
     */
    val maxRecordingDuration: Long = 30_000L,

    /**
     * Silence timeout in milliseconds
     * How long to wait for speech before timing out
     */
    val silenceTimeout: Long = 5_000L,

    /**
     * End-of-speech timeout in milliseconds
     * How long to wait after last speech before finalizing
     */
    val endOfSpeechTimeout: Long = 2_000L,

    // ═══════════════════════════════════════════════════════════════════
    // Feature Flags
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Enable profanity filtering (engine-dependent)
     */
    val enableProfanityFilter: Boolean = false,

    /**
     * Request interim/partial results during recognition
     */
    val enableInterimResults: Boolean = true,

    /**
     * Request word-level timestamps (engine-dependent)
     */
    val enableWordTimestamps: Boolean = false,

    /**
     * Prefer offline mode when available
     */
    val preferOffline: Boolean = false,

    // ═══════════════════════════════════════════════════════════════════
    // Engine-Specific Settings (Optional)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * API key for cloud engines (Google Cloud, Azure)
     */
    val apiKey: String? = null,

    /**
     * API region for cloud engines (Azure)
     */
    val apiRegion: String? = null,

    /**
     * Path to offline model (VOSK, Whisper)
     */
    val modelPath: String? = null,

    /**
     * Model size/variant for engines with multiple models
     */
    val modelSize: ModelSize = ModelSize.MEDIUM,

    // ═══════════════════════════════════════════════════════════════════
    // VoiceOS-Specific Settings
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Command to mute voice recognition
     */
    val muteCommand: String = "mute voice",

    /**
     * Command to unmute/wake voice recognition
     */
    val wakeCommand: String = "wake up voice",

    /**
     * Command to start dictation mode
     */
    val startDictationCommand: String = "start dictation",

    /**
     * Command to stop dictation mode
     */
    val stopDictationCommand: String = "stop dictation"
) {
    /**
     * Validate configuration for consistency
     */
    fun validate(): Result<Unit> {
        return when {
            language.isBlank() ->
                Result.failure(IllegalArgumentException("Language cannot be blank"))

            confidenceThreshold !in 0f..1f ->
                Result.failure(IllegalArgumentException("Confidence threshold must be 0.0-1.0"))

            silenceTimeout < 500L ->
                Result.failure(IllegalArgumentException("Silence timeout must be at least 500ms"))

            maxRecordingDuration < silenceTimeout ->
                Result.failure(IllegalArgumentException("Max recording must be >= silence timeout"))

            endOfSpeechTimeout < 500L ->
                Result.failure(IllegalArgumentException("End-of-speech timeout must be at least 500ms"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Check if configuration requires network
     */
    fun requiresNetwork(): Boolean = !preferOffline && apiKey != null

    /**
     * Check if configuration requires model download
     */
    fun requiresModel(): Boolean = modelPath != null || preferOffline

    // ═══════════════════════════════════════════════════════════════════
    // Fluent Modification Methods
    // ═══════════════════════════════════════════════════════════════════

    fun withLanguage(lang: String) = copy(language = lang)
    fun withMode(m: SpeechMode) = copy(mode = m)
    fun withVAD(enabled: Boolean) = copy(enableVAD = enabled)
    fun withConfidence(threshold: Float) = copy(confidenceThreshold = threshold.coerceIn(0f, 1f))
    fun withTimeout(ms: Long) = copy(silenceTimeout = ms)
    fun withMaxRecording(ms: Long) = copy(maxRecordingDuration = ms)
    fun withApiKey(key: String) = copy(apiKey = key)
    fun withApiRegion(region: String) = copy(apiRegion = region)
    fun withModelPath(path: String) = copy(modelPath = path)
    fun withModelSize(size: ModelSize) = copy(modelSize = size)
    fun withOfflinePreference(prefer: Boolean) = copy(preferOffline = prefer)
    fun withInterimResults(enabled: Boolean) = copy(enableInterimResults = enabled)
    fun withProfanityFilter(enabled: Boolean) = copy(enableProfanityFilter = enabled)

    companion object {
        const val DEFAULT_LANGUAGE = "en-US"

        /**
         * Default configuration for voice commands
         */
        fun forVoiceCommands() = SpeechConfig(
            mode = SpeechMode.COMBINED_COMMAND,
            confidenceThreshold = 0.75f,
            enableInterimResults = false,
            silenceTimeout = 3_000L,
            endOfSpeechTimeout = 1_500L
        )

        /**
         * Configuration for dictation/text entry
         */
        fun forDictation() = SpeechConfig(
            mode = SpeechMode.DICTATION,
            confidenceThreshold = 0.6f,
            enableInterimResults = true,
            maxRecordingDuration = 60_000L,
            silenceTimeout = 10_000L,
            endOfSpeechTimeout = 3_000L
        )

        /**
         * Configuration for offline-only operation
         */
        fun forOffline(modelPath: String) = SpeechConfig(
            preferOffline = true,
            modelPath = modelPath,
            confidenceThreshold = 0.7f
        )

        /**
         * Configuration for Google Cloud
         */
        fun forGoogleCloud(apiKey: String) = SpeechConfig(
            apiKey = apiKey,
            preferOffline = false
        )

        /**
         * Configuration for Azure
         */
        fun forAzure(apiKey: String, region: String) = SpeechConfig(
            apiKey = apiKey,
            apiRegion = region,
            preferOffline = false
        )
    }
}

/**
 * Model size variants for engines with multiple models
 */
enum class ModelSize {
    /**
     * Tiny model - fastest, lowest accuracy (~39MB for Whisper)
     */
    TINY,

    /**
     * Small model - fast, good accuracy (~244MB for Whisper)
     */
    SMALL,

    /**
     * Medium model - balanced (~769MB for Whisper)
     */
    MEDIUM,

    /**
     * Large model - best accuracy, slower (~1550MB for Whisper)
     */
    LARGE;

    /**
     * Get approximate model size in MB
     */
    fun getApproximateSizeMB(): Int = when (this) {
        TINY -> 39
        SMALL -> 244
        MEDIUM -> 769
        LARGE -> 1550
    }
}
