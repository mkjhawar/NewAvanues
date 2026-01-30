package com.augmentalis.voiceisolation

/**
 * Current state of voice isolation processing.
 */
data class VoiceIsolationState(
    /**
     * Whether voice isolation is currently enabled.
     */
    val isEnabled: Boolean,

    /**
     * Whether processing is currently active (initialized with audio session).
     */
    val isActive: Boolean,

    /**
     * Current configuration being used.
     */
    val config: VoiceIsolationConfig,

    /**
     * Feature availability on current device.
     */
    val availability: FeatureAvailability
)

/**
 * Availability of individual voice isolation features on the current device.
 */
data class FeatureAvailability(
    /**
     * Whether noise suppression is available on this device.
     */
    val noiseSuppression: Boolean,

    /**
     * Whether echo cancellation is available on this device.
     */
    val echoCancellation: Boolean,

    /**
     * Whether automatic gain control is available on this device.
     */
    val automaticGainControl: Boolean
) {
    /**
     * Whether any voice isolation feature is available.
     */
    val anyAvailable: Boolean
        get() = noiseSuppression || echoCancellation || automaticGainControl

    /**
     * Whether all voice isolation features are available.
     */
    val allAvailable: Boolean
        get() = noiseSuppression && echoCancellation && automaticGainControl

    companion object {
        /**
         * All features available.
         */
        val ALL_AVAILABLE = FeatureAvailability(
            noiseSuppression = true,
            echoCancellation = true,
            automaticGainControl = true
        )

        /**
         * No features available (stub platforms).
         */
        val NONE_AVAILABLE = FeatureAvailability(
            noiseSuppression = false,
            echoCancellation = false,
            automaticGainControl = false
        )
    }
}
