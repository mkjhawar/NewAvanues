package com.augmentalis.voiceisolation

/**
 * Voice isolation processing modes that trade off latency vs quality.
 */
enum class ProcessingMode {
    /**
     * Minimal processing for lowest latency.
     * Best for: Real-time voice commands where speed is critical.
     */
    LOW_LATENCY,

    /**
     * Balanced processing (default).
     * Best for: Most voice recognition scenarios.
     */
    BALANCED,

    /**
     * Maximum processing for best audio quality.
     * Best for: Noisy environments, transcription, translation.
     */
    HIGH_QUALITY
}
