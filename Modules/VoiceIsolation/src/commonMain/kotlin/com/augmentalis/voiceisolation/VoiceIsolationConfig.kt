package com.augmentalis.voiceisolation

import kotlinx.serialization.Serializable

/**
 * Configuration for voice isolation processing.
 *
 * All settings are user-adjustable via Settings UI or voice commands.
 * Default: enabled = true for A/B testing (user can toggle OFF to compare).
 */
@Serializable
data class VoiceIsolationConfig(
    /**
     * Master toggle for voice isolation (default: ON for A/B testing).
     * When disabled, audio passes through unchanged.
     */
    val enabled: Boolean = true,

    /**
     * Enable noise suppression to reduce background noise.
     * Removes ambient sounds like fans, traffic, crowd noise.
     */
    val noiseSuppression: Boolean = true,

    /**
     * Enable echo cancellation.
     * Removes audio feedback from speakers playing back to microphone.
     * Typically needed for speakerphone or smart speaker scenarios.
     */
    val echoCancellation: Boolean = false,

    /**
     * Enable automatic gain control.
     * Normalizes volume levels for consistent loudness.
     */
    val automaticGainControl: Boolean = true,

    /**
     * Noise suppression intensity (0.0 to 1.0).
     * Higher values = more aggressive noise removal.
     * Default: 0.7 (balanced - removes most noise without distortion)
     */
    val noiseSuppressionLevel: Float = 0.7f,

    /**
     * Gain level adjustment (0.0 to 1.0).
     * Controls how much the AGC boosts/attenuates audio.
     * Default: 0.5 (moderate gain)
     */
    val gainLevel: Float = 0.5f,

    /**
     * Processing mode that trades off latency vs quality.
     */
    val mode: ProcessingMode = ProcessingMode.BALANCED
) {
    init {
        require(noiseSuppressionLevel in 0.0f..1.0f) {
            "noiseSuppressionLevel must be between 0.0 and 1.0"
        }
        require(gainLevel in 0.0f..1.0f) {
            "gainLevel must be between 0.0 and 1.0"
        }
    }

    companion object {
        /**
         * Default configuration with all features enabled.
         */
        val DEFAULT = VoiceIsolationConfig()

        /**
         * Minimal configuration for lowest latency.
         */
        val LOW_LATENCY = VoiceIsolationConfig(
            noiseSuppression = true,
            echoCancellation = false,
            automaticGainControl = false,
            mode = ProcessingMode.LOW_LATENCY
        )

        /**
         * Maximum quality configuration for noisy environments.
         */
        val HIGH_QUALITY = VoiceIsolationConfig(
            noiseSuppression = true,
            echoCancellation = true,
            automaticGainControl = true,
            noiseSuppressionLevel = 0.9f,
            mode = ProcessingMode.HIGH_QUALITY
        )

        /**
         * Disabled configuration (passthrough).
         */
        val DISABLED = VoiceIsolationConfig(enabled = false)
    }
}
