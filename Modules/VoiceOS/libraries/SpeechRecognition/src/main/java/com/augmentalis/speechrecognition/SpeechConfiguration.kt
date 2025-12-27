/**
 * SpeechConfiguration.kt - Complete configuration system for speech recognition
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-28
 * 
 * Merged from:
 * - config/SpeechConfig.kt
 * - models/SpeechModels.kt
 * 
 * This consolidation follows VOS4's single-file pattern, reducing unnecessary
 * directory nesting for closely related configuration components.
 */
package com.augmentalis.speechrecognition

import com.augmentalis.voiceos.speech.engines.vivoka.model.VivokaLanguageRepository

/**
 * Available speech recognition engines.
 * Extended to include Whisper engine.
 */
enum class SpeechEngine {
    /**
     * VOSK - Open-source offline speech recognition
     * - Offline capable
     * - Multiple language models
     * - ~30MB memory usage
     */
    VOSK,
    
    /**
     * Vivoka - VSDK-based hybrid recognition
     * - Offline/Online hybrid
     * - Wake word support
     * - ~60MB memory usage
     */
    VIVOKA,
    
    /**
     * Android STT - Android's built-in native speech-to-text
     * - Online only (requires Google services)
     * - Uses device's native SpeechRecognizer
     * - ~20MB memory usage
     */
    ANDROID_STT,
    
    /**
     * Google Cloud - Google Cloud Speech API
     * - Online only
     * - Advanced features
     * - Requires API key
     * - ~15MB memory usage
     */
    GOOGLE_CLOUD,
    
    /**
     * Whisper - OpenAI Whisper speech recognition
     * - Offline capable
     * - Multiple model sizes (tiny to large)
     * - Advanced features: language detection, translation, word timestamps
     * - GPU acceleration support
     * - ~39MB to 1550MB depending on model size
     */
    WHISPER;
    
    /**
     * Check if engine is offline capable
     */
    fun isOfflineCapable(): Boolean {
        return this in listOf(VOSK, VIVOKA, WHISPER)
    }
    
    /**
     * Check if engine requires API key
     */
    fun requiresApiKey(): Boolean {
        return this == GOOGLE_CLOUD
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
        }
    }
}

/**
 * Recognition modes that determine how speech is processed.
 */
enum class SpeechMode {
    /**
     * Static command mode - matches against predefined commands only
     * - Highest accuracy for known commands
     * - Limited vocabulary
     * - Fast processing
     */
    STATIC_COMMAND,
    
    /**
     * Dynamic command mode - matches against UI-scraped and contextual commands
     * - Adapts to current screen content
     * - Medium vocabulary size
     * - Good balance of accuracy and flexibility
     */
    DYNAMIC_COMMAND,
    
    /**
     * Dictation mode - continuous speech recognition
     * - Large vocabulary
     * - Sentence-level processing
     * - Punctuation support (engine dependent)
     */
    DICTATION,
    
    /**
     * Free speech mode - unrestricted speech recognition
     * - Largest vocabulary
     * - No command matching
     * - Lowest constraints
     */
    FREE_SPEECH,
    
    /**
     * Hybrid mode - Vivoka-specific mode
     * - Switches between online/offline automatically
     * - Best quality based on connectivity
     */
    HYBRID;
    
    /**
     * Check if mode uses command matching
     */
    fun usesCommandMatching(): Boolean {
        return this in listOf(STATIC_COMMAND, DYNAMIC_COMMAND)
    }
    
    /**
     * Check if mode supports continuous recognition
     */
    fun supportsContinuous(): Boolean {
        return this in listOf(DICTATION, FREE_SPEECH)
    }
    
    /**
     * Get recommended confidence threshold for this mode
     */
    fun getRecommendedConfidenceThreshold(): Float {
        return when (this) {
            STATIC_COMMAND -> 0.8f    // High confidence for commands
            DYNAMIC_COMMAND -> 0.7f    // Medium confidence for dynamic
            DICTATION -> 0.6f          // Lower for continuous speech
            FREE_SPEECH -> 0.5f        // Lowest for unrestricted
            HYBRID -> 0.7f             // Medium confidence for hybrid
        }
    }
    
    /**
     * Get human-readable description
     */
    fun getDescription(): String {
        return when (this) {
            STATIC_COMMAND -> "Predefined commands only"
            DYNAMIC_COMMAND -> "Screen-based commands"
            DICTATION -> "Continuous speech input"
            FREE_SPEECH -> "Unrestricted speech"
            HYBRID -> "Auto online/offline switching"
        }
    }
}

/**
 * Unified configuration for all speech recognition engines.
 * Uses Kotlin data class features for immutability and easy modification.
 * 
 * This replaces the old SpeechRecognitionConfig + Builder pattern with
 * a simpler, more efficient approach using copy() and default parameters.
 */
data class SpeechConfig(
    // Common configuration for ALL engines
    val language: String = VivokaLanguageRepository.LANGUAGE_CODE_ENGLISH_USA,
    val mode: SpeechMode = SpeechMode.DYNAMIC_COMMAND,
    val enableVAD: Boolean = true,
    val confidenceThreshold: Float = 4000F,
    val maxRecordingDuration: Long = 30000,
    val timeoutDuration: Long = 5000,  // General timeout in milliseconds
    val dictationTimeout: Long = 2000,  // Dictation silence timeout in milliseconds (1-10 seconds)
    val voiceTimeoutMinutes: Long = 5,  // Voice auto-sleep timeout in minutes
    val enableProfanityFilter: Boolean = false,
    val staticCommandsPath: String = "static_commands/",
    val voiceEnabled: Boolean = true,  // Enable voice recognition (for auto-sleep feature)
    
    // Special commands (for Vivoka and command-based engines)
    val muteCommand: String = "mute voice",
    val unmuteCommand: String = "wake up voice",
    val startDictationCommand: String = "start dictation",
    val stopDictationCommand: String = "stop dictation",
    
    // Engine selection
    val engine: SpeechEngine = SpeechEngine.VOSK,
    
    // Engine-specific optional configs
    val cloudApiKey: String? = null,  // For GoogleCloud only
    val modelPath: String? = null,    // For offline engines (VOSK/Vivoka)
    
    // TTS (Text-to-Speech) configuration
    val enableTTS: Boolean = false,                    // Enable TTS feedback
    val ttsRate: Float = 1.0f,                        // Speech rate (0.1-3.0)
    val ttsPitch: Float = 1.0f,                       // Speech pitch (0.1-2.0)
    val ttsVolume: Float = 1.0f,                      // Speech volume (0.0-1.0)
    val ttsLanguage: String = language,               // TTS language (defaults to recognition language)
    val ttsFeedbackLevel: String = "NORMAL",          // Feedback level: SILENT, MINIMAL, NORMAL, VERBOSE
    val ttsVoice: String? = null                      // Specific voice name (optional)
) {
    companion object {
        // Default configuration
        fun default() = SpeechConfig()
        
        // Engine-specific factory methods (NO language parameter - uses config language)
        fun vosk() = SpeechConfig(engine = SpeechEngine.VOSK)
        
        fun vivoka() = SpeechConfig(engine = SpeechEngine.VIVOKA)
        
        fun googleSTT() = SpeechConfig(engine = SpeechEngine.ANDROID_STT)
        
        fun googleCloud(apiKey: String) = SpeechConfig(
            engine = SpeechEngine.GOOGLE_CLOUD,
            cloudApiKey = apiKey
        )
    }
    
    // Fluent modification methods (returns new instance - immutable)
    fun withLanguage(lang: String) = copy(language = lang)
    fun withEngine(eng: SpeechEngine) = copy(engine = eng)
    fun withMode(m: SpeechMode) = copy(mode = m)
    fun withVAD(enabled: Boolean) = copy(enableVAD = enabled)
    fun withConfidenceThreshold(threshold: Float) = copy(confidenceThreshold = threshold)
    fun withTimeout(ms: Long) = copy(timeoutDuration = ms)
    fun withMaxRecording(ms: Long) = copy(maxRecordingDuration = ms)
    fun withApiKey(key: String) = copy(cloudApiKey = key)
    fun withModelPath(path: String) = copy(modelPath = path)
    fun withStaticCommandsPath(path: String) = copy(staticCommandsPath = path)
    
    // TTS fluent modification methods
    fun withTTS(enabled: Boolean) = copy(enableTTS = enabled)
    fun withTTSRate(rate: Float) = copy(ttsRate = rate.coerceIn(0.1f, 3.0f))
    fun withTTSPitch(pitch: Float) = copy(ttsPitch = pitch.coerceIn(0.1f, 2.0f))
    fun withTTSVolume(volume: Float) = copy(ttsVolume = volume.coerceIn(0.0f, 1.0f))
    fun withTTSLanguage(lang: String) = copy(ttsLanguage = lang)
    fun withTTSFeedbackLevel(level: String) = copy(ttsFeedbackLevel = level)
    fun withTTSVoice(voice: String) = copy(ttsVoice = voice)
    
    // Single validation point
    fun validate(): Result<Unit> {
        if (language.isBlank()) {
            return Result.failure(IllegalArgumentException("Language cannot be blank"))
        }
        else return if (engine == SpeechEngine.VIVOKA && confidenceThreshold !in 1000f..10000f) {
            Result.failure(IllegalArgumentException("Confidence threshold must be between 1000 and 10000"))
        } else if ( engine != SpeechEngine.VIVOKA && confidenceThreshold !in 0f..1f) {
            Result.failure(IllegalArgumentException("Confidence threshold must be between 0 and 1"))
        } else if (engine == SpeechEngine.GOOGLE_CLOUD && cloudApiKey.isNullOrBlank()) {
            Result.failure(IllegalArgumentException("Google Cloud requires API key"))
        } else if (timeoutDuration < 1000) {
            Result.failure(IllegalArgumentException("Timeout must be at least 1000ms"))
        } else if (maxRecordingDuration < timeoutDuration) {
            Result.failure(IllegalArgumentException("Max recording must be >= timeout"))
        } else if (ttsRate !in 0.1f..3.0f) {
            Result.failure(IllegalArgumentException("TTS rate must be between 0.1 and 3.0"))
        } else if (ttsPitch !in 0.1f..2.0f) {
            Result.failure(IllegalArgumentException("TTS pitch must be between 0.1 and 2.0"))
        } else if (ttsVolume !in 0.0f..1.0f) {
            Result.failure(IllegalArgumentException("TTS volume must be between 0.0 and 1.0"))
        } else if (ttsFeedbackLevel !in listOf("SILENT", "MINIMAL", "NORMAL", "VERBOSE")) {
            Result.failure(IllegalArgumentException("TTS feedback level must be SILENT, MINIMAL, NORMAL, or VERBOSE"))
        } else {
            Result.success(Unit)
        }
    }
    
    /**
     * Check if this configuration requires network connectivity
     */
    fun requiresNetwork(): Boolean {
        return engine in listOf(
            SpeechEngine.ANDROID_STT,
            SpeechEngine.GOOGLE_CLOUD
        )
    }
    
    /**
     * Check if this configuration requires model download
     */
    fun requiresModelDownload(): Boolean {
        return engine in listOf(
            SpeechEngine.VOSK,
            SpeechEngine.VIVOKA
        ) && modelPath.isNullOrBlank()
    }
    
    /**
     * Get estimated memory usage in MB
     */
    fun getEstimatedMemoryUsage(): Int {
        return when (engine) {
            SpeechEngine.VOSK -> 30
            SpeechEngine.VIVOKA -> 60
            SpeechEngine.ANDROID_STT -> 20
            SpeechEngine.GOOGLE_CLOUD -> 15
            SpeechEngine.WHISPER -> 230  // Base model default, can vary from 150MB to 2500MB
        }
    }
    
    /**
     * Convert to readable string for logging
     */
    override fun toString(): String {
        return "SpeechConfig(" +
            "engine=$engine, " +
            "language=$language, " +
            "mode=$mode, " +
            "confidence=$confidenceThreshold, " +
            "timeout=${timeoutDuration}ms" +
            ")"
    }
}