/**
 * SpeechTypes.kt - Shared types for speech plugins
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines common data types, enums, and configurations used across
 * all speech-related plugins (recognition, TTS, wake word detection).
 */
package com.augmentalis.magiccode.plugins.universal.contracts.speech

import com.augmentalis.magiccode.plugins.universal.currentTimeMillis
import kotlinx.serialization.Serializable

// =============================================================================
// Speech Capabilities
// =============================================================================

/**
 * Capabilities that a speech recognition engine may support.
 *
 * These capabilities allow consumers to discover and select appropriate
 * speech engines based on their feature requirements.
 *
 * ## Capability Categories
 * - **Connectivity**: OFFLINE, STREAMING
 * - **Recognition modes**: CONTINUOUS, WAKE_WORD
 * - **Audio processing**: NOISE_REDUCTION
 * - **Output quality**: PUNCTUATION, PROFANITY_FILTER
 * - **Customization**: CUSTOM_VOCABULARY
 * - **Identity**: SPEAKER_ID
 *
 * @since 1.0.0
 */
enum class SpeechCapability {
    /**
     * Engine can perform recognition without network connectivity.
     * Models are downloaded and run locally on device.
     */
    OFFLINE,

    /**
     * Engine supports continuous recognition without manual restart.
     * Ideal for hands-free voice control applications.
     */
    CONTINUOUS,

    /**
     * Engine supports wake word detection for activation.
     * Can listen passively and activate on specific phrases.
     */
    WAKE_WORD,

    /**
     * Engine supports custom vocabulary and phrase boosting.
     * Allows improving recognition for domain-specific terms.
     */
    CUSTOM_VOCABULARY,

    /**
     * Engine can identify different speakers.
     * Useful for multi-user environments or personalization.
     */
    SPEAKER_ID,

    /**
     * Engine supports real-time streaming recognition.
     * Results are returned incrementally as audio is processed.
     */
    STREAMING,

    /**
     * Engine includes noise reduction and audio enhancement.
     * Improves recognition accuracy in noisy environments.
     */
    NOISE_REDUCTION,

    /**
     * Engine adds punctuation to recognized text automatically.
     * Improves readability of transcription output.
     */
    PUNCTUATION,

    /**
     * Engine can filter or mask profane content.
     * Useful for family-friendly or professional applications.
     */
    PROFANITY_FILTER
}

// =============================================================================
// Recognition State
// =============================================================================

/**
 * Current state of speech recognition.
 *
 * Used by speech engines to communicate their operational status
 * to consumers via StateFlow.
 *
 * ## State Transitions
 * ```
 * IDLE -> LISTENING (startRecognition)
 * LISTENING -> PROCESSING (end of utterance)
 * PROCESSING -> LISTENING (continuous mode)
 * PROCESSING -> IDLE (single utterance mode)
 * ANY -> ERROR (on failure)
 * ERROR -> IDLE (recovery)
 * ```
 *
 * @since 1.0.0
 */
enum class RecognitionState {
    /**
     * Recognition is not active.
     * The engine is ready to start recognition.
     */
    IDLE,

    /**
     * Actively listening for speech input.
     * Audio is being captured and processed.
     */
    LISTENING,

    /**
     * Processing captured audio.
     * Final results are being computed.
     */
    PROCESSING,

    /**
     * An error has occurred.
     * Check engine logs or health status for details.
     */
    ERROR
}

// =============================================================================
// Recognition Configuration
// =============================================================================

/**
 * Configuration for speech recognition sessions.
 *
 * Specifies how the speech engine should behave during recognition.
 * Settings can affect accuracy, latency, and resource usage.
 *
 * ## Usage
 * ```kotlin
 * val config = RecognitionConfig(
 *     language = "en-US",
 *     continuous = true,
 *     partialResults = true,
 *     maxAlternatives = 5
 * )
 * speechEngine.startRecognition(config).collect { result ->
 *     if (result.isFinal) processCommand(result.text)
 * }
 * ```
 *
 * @property language BCP-47 language tag (e.g., "en-US", "de-DE")
 * @property continuous If true, keep recognizing after each utterance
 * @property partialResults If true, emit interim results during recognition
 * @property maxAlternatives Maximum number of alternative transcriptions to return
 * @property profanityFilter If true, mask or filter profane content
 * @since 1.0.0
 */
@Serializable
data class RecognitionConfig(
    val language: String = "en-US",
    val continuous: Boolean = true,
    val partialResults: Boolean = true,
    val maxAlternatives: Int = 3,
    val profanityFilter: Boolean = false
) {
    companion object {
        /**
         * Default configuration for general voice commands.
         */
        val DEFAULT = RecognitionConfig()

        /**
         * Configuration optimized for dictation with punctuation.
         */
        val DICTATION = RecognitionConfig(
            continuous = true,
            partialResults = true,
            maxAlternatives = 1,
            profanityFilter = false
        )

        /**
         * Configuration for single command recognition.
         */
        val SINGLE_COMMAND = RecognitionConfig(
            continuous = false,
            partialResults = true,
            maxAlternatives = 3
        )
    }
}

// =============================================================================
// Recognition Result
// =============================================================================

/**
 * Result from speech recognition.
 *
 * Represents either a partial (interim) or final transcription result.
 * For continuous recognition, multiple results are emitted over time.
 *
 * ## Partial vs Final Results
 * - **Partial results**: `isFinal = false`, may change as more audio is processed
 * - **Final results**: `isFinal = true`, represents the engine's best transcription
 *
 * @property text The recognized text (primary transcription)
 * @property confidence Confidence score from 0.0 (low) to 1.0 (high)
 * @property isFinal Whether this is a final or interim result
 * @property alternatives List of alternative transcriptions
 * @property timestamp When this result was generated (epoch millis)
 * @since 1.0.0
 */
@Serializable
data class RecognitionResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean,
    val alternatives: List<String> = emptyList(),
    val timestamp: Long = currentTimeMillis()
) {
    /**
     * Check if this result has high confidence.
     *
     * @param threshold Minimum confidence threshold (default 0.8)
     * @return true if confidence exceeds the threshold
     */
    fun isHighConfidence(threshold: Float = 0.8f): Boolean = confidence >= threshold

    /**
     * Get all transcriptions including primary text and alternatives.
     *
     * @return List with primary text first, followed by alternatives
     */
    fun allTranscriptions(): List<String> = listOf(text) + alternatives

    companion object {
        /**
         * Create an empty result (e.g., for silence).
         */
        val EMPTY = RecognitionResult(
            text = "",
            confidence = 0f,
            isFinal = true
        )

        /**
         * Create a final result with single transcription.
         *
         * @param text The recognized text
         * @param confidence Confidence score
         * @return Final recognition result
         */
        fun final(text: String, confidence: Float): RecognitionResult {
            return RecognitionResult(
                text = text,
                confidence = confidence,
                isFinal = true
            )
        }

        /**
         * Create a partial (interim) result.
         *
         * @param text The recognized text so far
         * @param confidence Current confidence score
         * @return Partial recognition result
         */
        fun partial(text: String, confidence: Float): RecognitionResult {
            return RecognitionResult(
                text = text,
                confidence = confidence,
                isFinal = false
            )
        }
    }
}

// =============================================================================
// Custom Vocabulary
// =============================================================================

/**
 * Custom vocabulary for improving recognition accuracy.
 *
 * Allows applications to specify domain-specific terms, proper nouns,
 * or unusual words that the speech engine should recognize better.
 *
 * ## Usage
 * ```kotlin
 * val vocabulary = CustomVocabulary(
 *     name = "app_commands",
 *     phrases = listOf("VoiceOS", "Augmentalis", "scroll down"),
 *     boosts = mapOf(
 *         "VoiceOS" to 1.5f,
 *         "Augmentalis" to 2.0f
 *     )
 * )
 * speechEngine.loadVocabulary(vocabulary)
 * ```
 *
 * @property name Identifier for this vocabulary (for management/updates)
 * @property phrases List of phrases to add to recognition vocabulary
 * @property boosts Optional confidence boosts for specific phrases (1.0 = normal)
 * @since 1.0.0
 */
@Serializable
data class CustomVocabulary(
    val name: String,
    val phrases: List<String>,
    val boosts: Map<String, Float> = emptyMap()
) {
    /**
     * Get the boost factor for a phrase.
     *
     * @param phrase The phrase to look up
     * @return Boost factor (1.0 if not specified)
     */
    fun getBoost(phrase: String): Float = boosts[phrase] ?: 1.0f

    /**
     * Check if this vocabulary contains a specific phrase.
     *
     * @param phrase The phrase to check
     * @return true if the phrase is in this vocabulary
     */
    fun contains(phrase: String): Boolean = phrase in phrases

    /**
     * Merge with another vocabulary.
     *
     * @param other Vocabulary to merge with
     * @return New vocabulary containing phrases from both
     */
    fun merge(other: CustomVocabulary): CustomVocabulary {
        return CustomVocabulary(
            name = "${this.name}+${other.name}",
            phrases = (this.phrases + other.phrases).distinct(),
            boosts = this.boosts + other.boosts
        )
    }

    companion object {
        /**
         * Create an empty vocabulary.
         *
         * @param name Vocabulary name
         * @return Empty vocabulary
         */
        fun empty(name: String = "empty"): CustomVocabulary {
            return CustomVocabulary(name = name, phrases = emptyList())
        }

        /**
         * Create a vocabulary from a list of phrases with default boost.
         *
         * @param name Vocabulary name
         * @param phrases List of phrases
         * @return Vocabulary with no boost modifications
         */
        fun fromPhrases(name: String, vararg phrases: String): CustomVocabulary {
            return CustomVocabulary(name = name, phrases = phrases.toList())
        }
    }
}

// =============================================================================
// Audio Sample
// =============================================================================

/**
 * Raw audio sample data for calibration or processing.
 *
 * Represents a segment of audio data with associated format information.
 * Used primarily for calibration and custom model training.
 *
 * @property data Raw audio bytes (PCM format expected)
 * @property sampleRate Sample rate in Hz (e.g., 16000, 44100)
 * @property channelCount Number of audio channels (1 = mono, 2 = stereo)
 * @since 1.0.0
 */
@Serializable
data class AudioSample(
    val data: ByteArray,
    val sampleRate: Int,
    val channelCount: Int
) {
    /**
     * Calculate the duration of this audio sample.
     *
     * @return Duration in milliseconds
     */
    fun durationMs(): Long {
        // Assuming 16-bit samples (2 bytes per sample)
        val bytesPerSample = 2 * channelCount
        val totalSamples = data.size / bytesPerSample
        return (totalSamples * 1000L) / sampleRate
    }

    /**
     * Check if this sample is mono.
     */
    val isMono: Boolean get() = channelCount == 1

    /**
     * Check if this sample is stereo.
     */
    val isStereo: Boolean get() = channelCount == 2

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AudioSample

        if (!data.contentEquals(other.data)) return false
        if (sampleRate != other.sampleRate) return false
        if (channelCount != other.channelCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channelCount
        return result
    }

    companion object {
        /** Standard sample rate for speech recognition (16kHz) */
        const val SAMPLE_RATE_16K = 16000

        /** CD-quality sample rate (44.1kHz) */
        const val SAMPLE_RATE_44K = 44100

        /** Mono audio channel */
        const val MONO = 1

        /** Stereo audio channels */
        const val STEREO = 2
    }
}

// =============================================================================
// Calibration Result
// =============================================================================

/**
 * Result from audio/speech calibration.
 *
 * Contains information about the calibration process outcome
 * and any adjustments made to the recognition engine.
 *
 * ## Calibration Use Cases
 * - Noise level calibration for environment adaptation
 * - Speaker enrollment for voice profiles
 * - Microphone sensitivity adjustment
 *
 * @property success Whether calibration completed successfully
 * @property message Human-readable status or error message
 * @property adjustments Map of parameter adjustments made (e.g., "noiseFloor" to -50.0f)
 * @since 1.0.0
 */
@Serializable
data class CalibrationResult(
    val success: Boolean,
    val message: String,
    val adjustments: Map<String, Float> = emptyMap()
) {
    /**
     * Get a specific adjustment value.
     *
     * @param key Adjustment parameter name
     * @return Adjustment value or null if not present
     */
    fun getAdjustment(key: String): Float? = adjustments[key]

    companion object {
        /**
         * Create a successful calibration result.
         *
         * @param message Success message
         * @param adjustments Map of adjustments made
         * @return Successful calibration result
         */
        fun success(
            message: String = "Calibration completed successfully",
            adjustments: Map<String, Float> = emptyMap()
        ): CalibrationResult {
            return CalibrationResult(
                success = true,
                message = message,
                adjustments = adjustments
            )
        }

        /**
         * Create a failed calibration result.
         *
         * @param message Error message describing the failure
         * @return Failed calibration result
         */
        fun failure(message: String): CalibrationResult {
            return CalibrationResult(
                success = false,
                message = message
            )
        }

        /**
         * Create a failed calibration result from exception.
         *
         * @param error The exception that caused the failure
         * @return Failed calibration result with error details
         */
        fun fromError(error: Throwable): CalibrationResult {
            return CalibrationResult(
                success = false,
                message = error.message ?: "Calibration failed: ${error::class.simpleName}"
            )
        }
    }
}
