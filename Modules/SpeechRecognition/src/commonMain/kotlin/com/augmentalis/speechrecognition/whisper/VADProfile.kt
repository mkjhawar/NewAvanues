/**
 * VADProfile.kt - Preset Voice Activity Detection profiles
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Provides tuned VAD parameter bundles for common speech recognition scenarios.
 * Each profile configures silence timeout, minimum speech duration, hangover frames,
 * VAD sensitivity, and adaptive threshold alpha for optimal behavior in its target use case.
 *
 * Profiles are mapped from SpeechMode via [forSpeechMode], allowing engines to
 * automatically select the best VAD tuning based on the current recognition mode.
 */
package com.augmentalis.speechrecognition.whisper

import com.augmentalis.speechrecognition.SpeechMode

/**
 * Preset VAD configurations for common speech recognition scenarios.
 *
 * Each profile bundles all VAD-tunable parameters into a coherent set
 * optimized for a specific use case:
 *
 * - [COMMAND]: Short silence timeout (400ms), fast adaptation — optimized for
 *   responsive voice command recognition where the user speaks a short phrase
 *   and expects immediate action.
 *
 * - [CONVERSATION]: Moderate silence timeout (700ms), balanced adaptation —
 *   the default profile that matches the original hardcoded constants. Suitable
 *   for general-purpose speech recognition and mixed-mode use.
 *
 * - [DICTATION]: Long silence timeout (1200ms), slow adaptation — optimized for
 *   continuous speech input where natural pauses between sentences should not
 *   trigger premature chunk emission.
 *
 * @param silenceTimeoutMs Duration of silence (ms) before a speech chunk is finalized
 * @param minSpeechDurationMs Minimum speech duration (ms) to consider as valid utterance
 * @param hangoverFrames Number of silence frames tolerated within speech before entering hangover
 * @param vadSensitivity VAD sensitivity (0.0-1.0), controls noise floor multiplier
 * @param thresholdAlpha Adaptive threshold smoothing factor (lower = slower noise floor adaptation)
 * @param minThreshold Minimum threshold floor to prevent silence from zeroing out detection
 */
enum class VADProfile(
    val silenceTimeoutMs: Long,
    val minSpeechDurationMs: Long,
    val hangoverFrames: Int,
    val vadSensitivity: Float,
    val thresholdAlpha: Float,
    val minThreshold: Float
) {
    /**
     * Optimized for voice commands: short phrases, fast response.
     * Aggressive silence detection (400ms) with fast-adapting noise floor.
     */
    COMMAND(
        silenceTimeoutMs = 400,
        minSpeechDurationMs = 200,
        hangoverFrames = 3,
        vadSensitivity = 0.5f,
        thresholdAlpha = 0.03f,
        minThreshold = 0.001f
    ),

    /**
     * Balanced for general conversation and mixed-mode recognition.
     * Matches the original WhisperVAD hardcoded defaults exactly.
     */
    CONVERSATION(
        silenceTimeoutMs = 700,
        minSpeechDurationMs = 300,
        hangoverFrames = 5,
        vadSensitivity = 0.6f,
        thresholdAlpha = 0.02f,
        minThreshold = 0.001f
    ),

    /**
     * Optimized for continuous dictation: long pauses tolerated.
     * Slow-adapting noise floor for stable detection in extended sessions.
     */
    DICTATION(
        silenceTimeoutMs = 1200,
        minSpeechDurationMs = 400,
        hangoverFrames = 8,
        vadSensitivity = 0.7f,
        thresholdAlpha = 0.015f,
        minThreshold = 0.0008f
    );

    companion object {
        /** Default profile — matches original WhisperVAD behavior */
        val DEFAULT = CONVERSATION

        /**
         * Select the appropriate VAD profile for a given SpeechMode.
         *
         * Mapping:
         * - STATIC_COMMAND, DYNAMIC_COMMAND → COMMAND (fast response)
         * - DICTATION → DICTATION (long pauses tolerated)
         * - FREE_SPEECH, HYBRID → CONVERSATION (balanced)
         */
        fun forSpeechMode(mode: SpeechMode): VADProfile = when (mode) {
            SpeechMode.STATIC_COMMAND -> COMMAND
            SpeechMode.DYNAMIC_COMMAND -> COMMAND
            SpeechMode.DICTATION -> DICTATION
            SpeechMode.FREE_SPEECH -> CONVERSATION
            SpeechMode.HYBRID -> CONVERSATION
        }
    }
}
