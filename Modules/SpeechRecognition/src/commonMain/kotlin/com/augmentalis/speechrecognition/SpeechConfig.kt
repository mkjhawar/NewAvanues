/**
 * SpeechConfig.kt - Unified configuration for speech recognition
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-08-28
 * Updated: 2026-01-18 - Migrated to KMP commonMain
 */
package com.augmentalis.speechrecognition

/**
 * Language codes for speech recognition.
 * Based on BCP-47 language tags.
 */
object LanguageCodes {
    const val ENGLISH_US = "en-US"
    const val ENGLISH_UK = "en-GB"
    const val SPANISH = "es-ES"
    const val SPANISH_MEXICO = "es-MX"
    const val FRENCH = "fr-FR"
    const val GERMAN = "de-DE"
    const val ITALIAN = "it-IT"
    const val PORTUGUESE = "pt-BR"
    const val CHINESE_SIMPLIFIED = "zh-CN"
    const val CHINESE_TRADITIONAL = "zh-TW"
    const val JAPANESE = "ja-JP"
    const val KOREAN = "ko-KR"
    const val RUSSIAN = "ru-RU"
    const val ARABIC = "ar-SA"
    const val HINDI = "hi-IN"
    const val TURKISH = "tr-TR"
    const val VIETNAMESE = "vi-VN"
    const val THAI = "th-TH"
    const val INDONESIAN = "id-ID"
    const val MALAY = "ms-MY"

    val ALL = listOf(
        ENGLISH_US, ENGLISH_UK, SPANISH, SPANISH_MEXICO, FRENCH, GERMAN,
        ITALIAN, PORTUGUESE, CHINESE_SIMPLIFIED, CHINESE_TRADITIONAL,
        JAPANESE, KOREAN, RUSSIAN, ARABIC, HINDI, TURKISH,
        VIETNAMESE, THAI, INDONESIAN, MALAY
    )
}

/**
 * Unified configuration for all speech recognition engines.
 * Uses Kotlin data class features for immutability and easy modification.
 */
data class SpeechConfig(
    // Common configuration for ALL engines
    val language: String = LanguageCodes.ENGLISH_US,
    val mode: SpeechMode = SpeechMode.DYNAMIC_COMMAND,
    val engine: SpeechEngine = SpeechEngine.VOSK,
    val enableVAD: Boolean = true,
    val confidenceThreshold: Float = 0.45f,
    val maxRecordingDuration: Long = 30000,
    val timeoutDuration: Long = 5000,
    val dictationTimeout: Long = 2000,
    val voiceTimeoutMinutes: Long = 5,
    val enableProfanityFilter: Boolean = false,
    val staticCommandsPath: String = "static_commands/",
    val voiceEnabled: Boolean = true,

    // Special commands
    val muteCommand: String = "mute voice",
    val unmuteCommand: String = "wake up voice",
    val startDictationCommand: String = "start dictation",
    val stopDictationCommand: String = "stop dictation",

    // Engine-specific optional configs
    val cloudApiKey: String? = null,
    val modelPath: String? = null,
    val azureRegion: String? = null,

    // Google Cloud STT v2 configuration
    val gcpProjectId: String? = null,
    val gcpRecognizerMode: String? = null,  // "batch" or "streaming"

    // TTS (Text-to-Speech) configuration
    val enableTTS: Boolean = false,
    val ttsRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsVolume: Float = 1.0f,
    val ttsLanguage: String = language,
    val ttsFeedbackLevel: TTSFeedbackLevel = TTSFeedbackLevel.NORMAL,
    val ttsVoice: String? = null,

    // Fuzzy matching configuration
    val enableFuzzyMatching: Boolean = true,
    val fuzzyMatchThreshold: Float = 0.7f,
    val enableSemanticMatching: Boolean = true
) {
    /**
     * TTS feedback levels
     */
    enum class TTSFeedbackLevel {
        SILENT, MINIMAL, NORMAL, VERBOSE
    }

    companion object {
        fun default() = SpeechConfig()

        fun vosk(language: String = LanguageCodes.ENGLISH_US) = SpeechConfig(
            engine = SpeechEngine.VOSK,
            language = language
        )

        fun googleCloud(
            apiKey: String? = null,
            projectId: String,
            language: String = LanguageCodes.ENGLISH_US,
            streaming: Boolean = false
        ) = SpeechConfig(
            engine = SpeechEngine.GOOGLE_CLOUD,
            cloudApiKey = apiKey,
            gcpProjectId = projectId,
            gcpRecognizerMode = if (streaming) "streaming" else "batch",
            language = language
        )

        fun azure(apiKey: String, region: String, language: String = LanguageCodes.ENGLISH_US) = SpeechConfig(
            engine = SpeechEngine.AZURE,
            cloudApiKey = apiKey,
            azureRegion = region,
            language = language
        )

        fun whisper(modelPath: String? = null, language: String = LanguageCodes.ENGLISH_US) = SpeechConfig(
            engine = SpeechEngine.WHISPER,
            modelPath = modelPath,
            language = language
        )
    }

    // Fluent modification methods
    fun withLanguage(lang: String) = copy(language = lang)
    fun withEngine(eng: SpeechEngine) = copy(engine = eng)
    fun withMode(m: SpeechMode) = copy(mode = m)
    fun withVAD(enabled: Boolean) = copy(enableVAD = enabled)
    fun withConfidenceThreshold(threshold: Float) = copy(confidenceThreshold = threshold)
    fun withTimeout(ms: Long) = copy(timeoutDuration = ms)
    fun withMaxRecording(ms: Long) = copy(maxRecordingDuration = ms)
    fun withApiKey(key: String) = copy(cloudApiKey = key)
    fun withModelPath(path: String) = copy(modelPath = path)
    fun withProjectId(projectId: String) = copy(gcpProjectId = projectId)
    fun withStreamingMode(streaming: Boolean = true) = copy(gcpRecognizerMode = if (streaming) "streaming" else "batch")
    fun withStaticCommandsPath(path: String) = copy(staticCommandsPath = path)
    fun withFuzzyMatching(enabled: Boolean) = copy(enableFuzzyMatching = enabled)
    fun withSemanticMatching(enabled: Boolean) = copy(enableSemanticMatching = enabled)

    // TTS fluent modification methods
    fun withTTS(enabled: Boolean) = copy(enableTTS = enabled)
    fun withTTSRate(rate: Float) = copy(ttsRate = rate.coerceIn(0.1f, 3.0f))
    fun withTTSPitch(pitch: Float) = copy(ttsPitch = pitch.coerceIn(0.1f, 2.0f))
    fun withTTSVolume(volume: Float) = copy(ttsVolume = volume.coerceIn(0.0f, 1.0f))
    fun withTTSLanguage(lang: String) = copy(ttsLanguage = lang)
    fun withTTSFeedbackLevel(level: TTSFeedbackLevel) = copy(ttsFeedbackLevel = level)
    fun withTTSVoice(voice: String) = copy(ttsVoice = voice)

    /**
     * Validate configuration
     */
    fun validate(): Result<Unit> {
        return when {
            language.isBlank() -> Result.failure(IllegalArgumentException("Language cannot be blank"))
            confidenceThreshold !in 0f..1f -> Result.failure(IllegalArgumentException("Confidence threshold must be between 0 and 1"))
            engine == SpeechEngine.GOOGLE_CLOUD && gcpProjectId.isNullOrBlank() -> Result.failure(IllegalArgumentException("Google Cloud requires project ID"))
            engine == SpeechEngine.AZURE && cloudApiKey.isNullOrBlank() -> Result.failure(IllegalArgumentException("Azure requires API key"))
            engine == SpeechEngine.AZURE && azureRegion.isNullOrBlank() -> Result.failure(IllegalArgumentException("Azure requires region"))
            timeoutDuration < 1000 -> Result.failure(IllegalArgumentException("Timeout must be at least 1000ms"))
            maxRecordingDuration < timeoutDuration -> Result.failure(IllegalArgumentException("Max recording must be >= timeout"))
            ttsRate !in 0.1f..3.0f -> Result.failure(IllegalArgumentException("TTS rate must be between 0.1 and 3.0"))
            ttsPitch !in 0.1f..2.0f -> Result.failure(IllegalArgumentException("TTS pitch must be between 0.1 and 2.0"))
            ttsVolume !in 0.0f..1.0f -> Result.failure(IllegalArgumentException("TTS volume must be between 0.0 and 1.0"))
            else -> Result.success(Unit)
        }
    }

    /**
     * Check if this configuration requires network connectivity
     */
    fun requiresNetwork(): Boolean {
        return engine in listOf(
            SpeechEngine.ANDROID_STT,
            SpeechEngine.GOOGLE_CLOUD,
            SpeechEngine.AZURE,
            SpeechEngine.WEB_SPEECH
        )
    }

    /**
     * Check if this configuration requires model download
     */
    fun requiresModelDownload(): Boolean {
        return engine in listOf(
            SpeechEngine.VOSK,
            SpeechEngine.VIVOKA,
            SpeechEngine.WHISPER
        ) && modelPath.isNullOrBlank()
    }

    override fun toString(): String {
        return "SpeechConfig(" +
            "engine=$engine, " +
            "language=$language, " +
            "mode=$mode, " +
            "confidence=$confidenceThreshold, " +
            "fuzzy=$enableFuzzyMatching" +
            ")"
    }
}
