/**
 * DeveloperSettings.kt - Cross-platform developer settings data model
 *
 * Pure Kotlin data class for developer/debug settings.
 * Read by VoiceOSCore to construct ServiceConfiguration.
 * Modified via DeveloperSettingsScreen (hidden behind 4-tap entry).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.settings.models

/**
 * Developer settings state with defaults.
 *
 * Lives in Foundation commonMain so all platforms share the same
 * settings shape, defaults, and validation constraints.
 */
data class DeveloperSettings(
    // Voice Timings
    val sttTimeoutMs: Long = DEFAULT_STT_TIMEOUT_MS,
    val endOfSpeechDelayMs: Long = DEFAULT_END_OF_SPEECH_DELAY_MS,
    val partialResultIntervalMs: Long = DEFAULT_PARTIAL_RESULT_INTERVAL_MS,
    val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD,

    // Feature Flags
    val debugMode: Boolean = true,
    val verboseLogging: Boolean = false,
    val debugOverlay: Boolean = false,
    val scannerVerbosity: Int = 0,
    val autoStartListening: Boolean = false,
    val synonymsEnabled: Boolean = true,

    // Engine Selection
    val sttEngine: String = DEFAULT_STT_ENGINE,
    val voiceLanguage: String = DEFAULT_VOICE_LANGUAGE,

    // Timing / Debounce
    val contentChangeDebounceMs: Long = DEFAULT_CONTENT_CHANGE_DEBOUNCE_MS,
    val scrollEventDebounceMs: Long = DEFAULT_SCROLL_EVENT_DEBOUNCE_MS,
    val screenChangeDelayMs: Long = DEFAULT_SCREEN_CHANGE_DELAY_MS,

    // Cockpit Debug
    val forceShellMode: String = "",      // Empty = no override, else CLASSIC/AVANUE_VIEWS/LENS/CANVAS
    val showShellDebugOverlay: Boolean = false,

    // Activation
    val developerModeActivated: Boolean = false
) {
    companion object {
        const val DEFAULT_STT_TIMEOUT_MS = 10000L
        const val DEFAULT_END_OF_SPEECH_DELAY_MS = 1500L
        const val DEFAULT_PARTIAL_RESULT_INTERVAL_MS = 300L
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.45f

        const val DEFAULT_STT_ENGINE = "VIVOKA"
        const val DEFAULT_VOICE_LANGUAGE = "en-US"

        const val DEFAULT_CONTENT_CHANGE_DEBOUNCE_MS = 300L
        const val DEFAULT_SCROLL_EVENT_DEBOUNCE_MS = 150L
        const val DEFAULT_SCREEN_CHANGE_DELAY_MS = 200L
    }
}
