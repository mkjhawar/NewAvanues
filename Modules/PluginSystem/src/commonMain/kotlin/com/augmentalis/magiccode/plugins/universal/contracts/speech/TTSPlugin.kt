/**
 * TTSPlugin.kt - Text-to-speech plugin interface
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the contract for text-to-speech plugins in the Universal Plugin system.
 * Implementations can provide system TTS, cloud-based synthesis, or custom engines.
 */
package com.augmentalis.magiccode.plugins.universal.contracts.speech

import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

// =============================================================================
// Voice Types
// =============================================================================

/**
 * Gender classification for TTS voices.
 *
 * Used for voice selection and filtering.
 *
 * @since 1.0.0
 */
enum class VoiceGender {
    /** Male voice */
    MALE,

    /** Female voice */
    FEMALE,

    /** Gender-neutral voice */
    NEUTRAL
}

/**
 * Represents an available TTS voice.
 *
 * Contains metadata about a voice that can be used for selection
 * and display in UI voice pickers.
 *
 * @property id Unique identifier for this voice
 * @property name Human-readable voice name (e.g., "Samantha", "Daniel")
 * @property locale BCP-47 locale tag (e.g., "en-US", "de-DE")
 * @property gender Voice gender classification
 * @property isOnline Whether this voice requires network connectivity
 * @since 1.0.0
 */
@Serializable
data class Voice(
    val id: String,
    val name: String,
    val locale: String,
    val gender: VoiceGender,
    val isOnline: Boolean
) {
    /**
     * Check if this voice supports a specific language.
     *
     * @param language BCP-47 language tag
     * @return true if voice locale matches the language
     */
    fun supportsLanguage(language: String): Boolean {
        return locale.startsWith(language.substringBefore("-"))
    }

    /**
     * Get display name with locale info.
     *
     * @return Formatted display name
     */
    fun displayName(): String = "$name ($locale)"

    companion object {
        /**
         * Create a default voice for a locale.
         *
         * @param locale BCP-47 locale tag
         * @return Default voice for the locale
         */
        fun default(locale: String = "en-US"): Voice {
            return Voice(
                id = "default_$locale",
                name = "Default",
                locale = locale,
                gender = VoiceGender.NEUTRAL,
                isOnline = false
            )
        }
    }
}

// =============================================================================
// TTS Configuration
// =============================================================================

/**
 * Audio format specifications for synthesized audio.
 *
 * @since 1.0.0
 */
enum class AudioFormat {
    /** 16-bit PCM audio */
    PCM_16BIT,

    /** WAV format with header */
    WAV,

    /** MP3 encoded audio */
    MP3,

    /** Opus encoded audio */
    OPUS,

    /** OGG Vorbis encoded audio */
    OGG_VORBIS
}

/**
 * Configuration for text-to-speech synthesis.
 *
 * Controls the speech output characteristics including rate, pitch,
 * volume, and audio format.
 *
 * ## Usage
 * ```kotlin
 * val config = TTSConfig(
 *     rate = 1.0f,      // Normal speed
 *     pitch = 1.2f,     // Slightly higher pitch
 *     volume = 0.8f,    // 80% volume
 *     audioFormat = AudioFormat.WAV
 * )
 * ttsPlugin.speak("Hello, world!", config)
 * ```
 *
 * @property rate Speech rate multiplier (0.5 = half speed, 2.0 = double speed)
 * @property pitch Pitch multiplier (0.5 = lower, 2.0 = higher)
 * @property volume Volume level from 0.0 (silent) to 1.0 (full)
 * @property audioFormat Output audio format for synthesis
 * @since 1.0.0
 */
@Serializable
data class TTSConfig(
    val rate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val audioFormat: AudioFormat = AudioFormat.PCM_16BIT
) {
    init {
        require(rate in 0.1f..4.0f) { "Rate must be between 0.1 and 4.0" }
        require(pitch in 0.1f..4.0f) { "Pitch must be between 0.1 and 4.0" }
        require(volume in 0.0f..1.0f) { "Volume must be between 0.0 and 1.0" }
    }

    companion object {
        /** Default configuration with normal rate, pitch, and volume */
        val DEFAULT = TTSConfig()

        /** Slower speech for accessibility */
        val SLOW = TTSConfig(rate = 0.75f)

        /** Faster speech for experienced users */
        val FAST = TTSConfig(rate = 1.5f)

        /** Whisper-like configuration */
        val WHISPER = TTSConfig(rate = 0.9f, volume = 0.5f)
    }
}

// =============================================================================
// Audio Data
// =============================================================================

/**
 * Synthesized audio data output.
 *
 * Contains the raw audio bytes and format information for
 * playback or further processing.
 *
 * @property data Raw audio bytes in the specified format
 * @property format Audio format of the data
 * @property sampleRate Sample rate in Hz
 * @property channelCount Number of audio channels (1 = mono, 2 = stereo)
 * @property durationMs Duration of the audio in milliseconds
 * @since 1.0.0
 */
@Serializable
data class AudioData(
    val data: ByteArray,
    val format: AudioFormat,
    val sampleRate: Int,
    val channelCount: Int,
    val durationMs: Long
) {
    /**
     * Check if this audio is mono.
     */
    val isMono: Boolean get() = channelCount == 1

    /**
     * Check if this audio is stereo.
     */
    val isStereo: Boolean get() = channelCount == 2

    /**
     * Get the size of the audio data in bytes.
     */
    val sizeBytes: Int get() = data.size

    /**
     * Get approximate bitrate of the audio.
     *
     * @return Bitrate in bits per second
     */
    fun bitrate(): Long {
        return if (durationMs > 0) {
            (data.size * 8 * 1000L) / durationMs
        } else 0L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AudioData

        if (!data.contentEquals(other.data)) return false
        if (format != other.format) return false
        if (sampleRate != other.sampleRate) return false
        if (channelCount != other.channelCount) return false
        if (durationMs != other.durationMs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channelCount
        result = 31 * result + durationMs.hashCode()
        return result
    }

    companion object {
        /** Empty audio data */
        val EMPTY = AudioData(
            data = ByteArray(0),
            format = AudioFormat.PCM_16BIT,
            sampleRate = 16000,
            channelCount = 1,
            durationMs = 0
        )

        /** Standard sample rate for speech (16kHz) */
        const val SAMPLE_RATE_16K = 16000

        /** High-quality sample rate (22.05kHz) */
        const val SAMPLE_RATE_22K = 22050

        /** CD-quality sample rate (44.1kHz) */
        const val SAMPLE_RATE_44K = 44100
    }
}

// =============================================================================
// TTS Plugin Interface
// =============================================================================

/**
 * Text-to-speech plugin interface.
 *
 * Extends [UniversalPlugin] to provide speech synthesis functionality.
 * Implementations can use various TTS backends including:
 * - System TTS engines (Android TTS, iOS AVSpeechSynthesizer)
 * - Cloud services (Google Cloud TTS, Amazon Polly, Azure TTS)
 * - Custom neural TTS models
 *
 * ## Design Goals
 * - **Accessibility-First**: Designed for voice-based feedback
 * - **Multi-Voice**: Support for multiple voices and languages
 * - **Flexible Output**: Both direct playback and audio data synthesis
 * - **Observable**: Speaking state via StateFlow
 *
 * ## Implementation Example
 * ```kotlin
 * class AndroidTTSPlugin : TTSPlugin {
 *     override val pluginId = "com.augmentalis.tts.android"
 *     override val pluginName = "Android TTS Engine"
 *     override val version = "1.0.0"
 *
 *     private val _isSpeaking = MutableStateFlow(false)
 *     override val isSpeaking: StateFlow<Boolean> = _isSpeaking
 *
 *     override val supportedVoices: List<Voice> by lazy {
 *         // Query Android TTS for available voices
 *         textToSpeech.voices.map { voice ->
 *             Voice(
 *                 id = voice.name,
 *                 name = voice.name,
 *                 locale = voice.locale.toLanguageTag(),
 *                 gender = mapGender(voice),
 *                 isOnline = voice.isNetworkConnectionRequired
 *             )
 *         }
 *     }
 *
 *     override suspend fun speak(text: String, config: TTSConfig): Result<Unit> {
 *         _isSpeaking.value = true
 *         // ... synthesis and playback
 *         _isSpeaking.value = false
 *         return Result.success(Unit)
 *     }
 *
 *     // ... implement other methods
 * }
 * ```
 *
 * ## Capability Registration
 * TTS plugins should register the capability:
 * `com.augmentalis.capability.speech.tts`
 *
 * @since 1.0.0
 * @see UniversalPlugin
 * @see Voice
 * @see TTSConfig
 */
interface TTSPlugin : UniversalPlugin {

    // =========================================================================
    // Voice Properties
    // =========================================================================

    /**
     * List of voices available for synthesis.
     *
     * Includes both offline and online voices. Check [Voice.isOnline]
     * to filter for offline-capable voices.
     *
     * This list may be populated lazily after [initialize] completes.
     *
     * @see Voice
     */
    val supportedVoices: List<Voice>

    /**
     * Currently selected voice for synthesis.
     *
     * May be null if no voice has been explicitly selected.
     * In that case, the engine uses a platform-appropriate default.
     *
     * @see setVoice
     */
    val currentVoice: Voice?

    /**
     * Observable speaking state.
     *
     * Emits `true` when the engine is actively speaking,
     * `false` when idle or stopped.
     *
     * ## Usage
     * ```kotlin
     * ttsPlugin.isSpeaking.collect { speaking ->
     *     if (speaking) {
     *         showSpeakingIndicator()
     *     } else {
     *         hideSpeakingIndicator()
     *     }
     * }
     * ```
     */
    val isSpeaking: StateFlow<Boolean>

    // =========================================================================
    // Synthesis Operations
    // =========================================================================

    /**
     * Speak text aloud using the current voice and configuration.
     *
     * Synthesizes the text and plays it through the device audio.
     * This method suspends until playback completes or is interrupted.
     *
     * ## Interruption
     * If [speak] is called while already speaking:
     * - Previous speech is stopped immediately
     * - New speech begins
     *
     * ## SSML Support
     * Some implementations may support SSML markup for advanced control.
     * Check implementation documentation for SSML support details.
     *
     * @param text Text to speak (plain text or SSML if supported)
     * @param config Speech configuration (rate, pitch, volume)
     * @return Result indicating success or failure
     * @see TTSConfig
     * @see stop
     */
    suspend fun speak(text: String, config: TTSConfig = TTSConfig()): Result<Unit>

    /**
     * Synthesize text to audio data without playing.
     *
     * Generates audio data that can be saved, streamed, or processed
     * by other components. Does not play the audio.
     *
     * ## Use Cases
     * - Caching frequently-used phrases
     * - Streaming audio to remote devices
     * - Audio processing pipelines
     * - Generating audio files
     *
     * @param text Text to synthesize
     * @param config Speech configuration (rate, pitch, volume, format)
     * @return AudioData containing the synthesized audio
     * @see AudioData
     * @see TTSConfig
     */
    suspend fun synthesize(text: String, config: TTSConfig = TTSConfig()): AudioData

    /**
     * Stop any active speech playback.
     *
     * Immediately interrupts current speech. After calling stop:
     * - [isSpeaking] emits `false`
     * - Any suspended [speak] call returns
     * - Audio playback stops immediately
     *
     * If not currently speaking, this method does nothing.
     */
    fun stop()

    /**
     * Set the voice to use for synthesis.
     *
     * The voice must be one of the voices in [supportedVoices].
     * The selected voice persists until changed or the plugin is shutdown.
     *
     * @param voiceId ID of the voice to select
     * @return Result indicating success or failure (e.g., voice not found)
     * @see supportedVoices
     * @see currentVoice
     */
    fun setVoice(voiceId: String): Result<Unit>
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Check if the TTS engine has any offline voices.
 *
 * @return true if at least one offline voice is available
 */
fun TTSPlugin.hasOfflineVoices(): Boolean {
    return supportedVoices.any { !it.isOnline }
}

/**
 * Get all offline voices.
 *
 * @return List of voices that don't require network
 */
fun TTSPlugin.getOfflineVoices(): List<Voice> {
    return supportedVoices.filter { !it.isOnline }
}

/**
 * Get voices for a specific locale.
 *
 * @param locale BCP-47 locale tag
 * @return List of voices matching the locale
 */
fun TTSPlugin.getVoicesForLocale(locale: String): List<Voice> {
    return supportedVoices.filter { it.locale == locale }
}

/**
 * Get voices for a specific language (any region).
 *
 * @param language BCP-47 language code (e.g., "en", "de")
 * @return List of voices for the language
 */
fun TTSPlugin.getVoicesForLanguage(language: String): List<Voice> {
    return supportedVoices.filter { it.supportsLanguage(language) }
}

/**
 * Get voices by gender.
 *
 * @param gender Voice gender to filter by
 * @return List of voices matching the gender
 */
fun TTSPlugin.getVoicesByGender(gender: VoiceGender): List<Voice> {
    return supportedVoices.filter { it.gender == gender }
}

/**
 * Find a voice by ID.
 *
 * @param voiceId Voice ID to find
 * @return Voice or null if not found
 */
fun TTSPlugin.findVoice(voiceId: String): Voice? {
    return supportedVoices.find { it.id == voiceId }
}

/**
 * Speak text with default configuration.
 *
 * Convenience method using [TTSConfig.DEFAULT].
 *
 * @param text Text to speak
 * @return Result indicating success or failure
 */
suspend fun TTSPlugin.speak(text: String): Result<Unit> {
    return speak(text, TTSConfig.DEFAULT)
}

/**
 * Check if currently speaking.
 *
 * @return true if speech is in progress
 */
fun TTSPlugin.isCurrentlySpeaking(): Boolean {
    return isSpeaking.value
}

/**
 * Set voice by finding a match for the given locale.
 *
 * Finds the first voice matching the locale and sets it.
 *
 * @param locale BCP-47 locale tag
 * @return Result indicating success or failure
 */
fun TTSPlugin.setVoiceForLocale(locale: String): Result<Unit> {
    val voice = getVoicesForLocale(locale).firstOrNull()
        ?: return Result.failure(IllegalArgumentException("No voice found for locale: $locale"))
    return setVoice(voice.id)
}
