package com.augmentalis.devicemanager.audio

/**
 * Audio device details
 */
data class AudioDevice(
    val id: Int,
    val name: String,
    val type: String,
    val isInput: Boolean,
    val isOutput: Boolean,
    val channelMasks: List<Int>,
    val channelCounts: List<Int>,
    val sampleRates: List<Int>,
    val encodings: List<Int>,
    val address: String,
    val isBluetooth: Boolean,
    val isUsb: Boolean,
    val isBuiltIn: Boolean
)

/**
 * Audio profile for different use cases
 */
enum class AudioProfile {
    DEFAULT,
    VOICE_CALL,
    VIDEO_CALL,
    VOICE_RECOGNITION,
    MEDIA_PLAYBACK,
    RECORDING,
    GAMING
}

/**
 * Equalizer presets
 */
enum class EqualizerPreset(val value: Int) {
    NORMAL(0),
    CLASSICAL(1),
    DANCE(2),
    FLAT(3),
    FOLK(4),
    HEAVY_METAL(5),
    HIP_HOP(6),
    JAZZ(7),
    POP(8),
    ROCK(9)
}

/**
 * Reverb presets for environmental audio
 */
enum class ReverbPreset {
    NONE,
    SMALL_ROOM,
    LARGE_ROOM,
    HALL,
    OUTDOOR
}

/**
 * Audio latency information
 */
data class AudioLatency(
    val outputLatencyMs: Int,
    val outputFramesPerBuffer: Int,
    val sampleRate: Int,
    val hasLowLatencySupport: Boolean,
    val hasProAudioSupport: Boolean
)

/**
 * Audio effect configuration
 */
data class EffectConfig(
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val equalizerPreset: EqualizerPreset? = null,
    val reverbPreset: ReverbPreset = ReverbPreset.NONE
)

/**
 * Audio enhancement configuration
 */
data class EnhancementConfig(
    val echoCancellation: Boolean = false,
    val noiseSuppression: Boolean = false,
    val automaticGainControl: Boolean = false
)

/**
 * Spatial audio configuration
 */
data class SpatialConfig(
    val enabled: Boolean = false,
    val headTracking: Boolean = false,
    val binauralMode: Boolean = false
)
