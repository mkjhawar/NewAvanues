/**
 * DeveloperSettingsCodec.kt - Codec for DeveloperSettings persistence
 *
 * Maps DeveloperSettings fields to/from SettingsKeys for platform-agnostic
 * persistence. Used by UserDefaultsSettingsStore (iOS) and
 * JavaPreferencesSettingsStore (Desktop).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.settings

import com.augmentalis.foundation.settings.models.DeveloperSettings

/**
 * Codec for [DeveloperSettings] ↔ key-value persistence.
 */
object DeveloperSettingsCodec : SettingsCodec<DeveloperSettings> {

    override val defaultValue: DeveloperSettings = DeveloperSettings()

    override fun decode(reader: PreferenceReader): DeveloperSettings {
        return DeveloperSettings(
            sttTimeoutMs = reader.getLong(SettingsKeys.DEV_STT_TIMEOUT_MS, DeveloperSettings.DEFAULT_STT_TIMEOUT_MS),
            endOfSpeechDelayMs = reader.getLong(SettingsKeys.DEV_END_OF_SPEECH_DELAY_MS, DeveloperSettings.DEFAULT_END_OF_SPEECH_DELAY_MS),
            partialResultIntervalMs = reader.getLong(SettingsKeys.DEV_PARTIAL_RESULT_INTERVAL_MS, DeveloperSettings.DEFAULT_PARTIAL_RESULT_INTERVAL_MS),
            confidenceThreshold = reader.getFloat(SettingsKeys.DEV_CONFIDENCE_THRESHOLD, DeveloperSettings.DEFAULT_CONFIDENCE_THRESHOLD),
            debugMode = reader.getBoolean(SettingsKeys.DEV_DEBUG_MODE, true),
            verboseLogging = reader.getBoolean(SettingsKeys.DEV_VERBOSE_LOGGING, false),
            debugOverlay = reader.getBoolean(SettingsKeys.DEV_DEBUG_OVERLAY, false),
            scannerVerbosity = reader.getInt(SettingsKeys.DEV_SCANNER_VERBOSITY, 0),
            autoStartListening = reader.getBoolean(SettingsKeys.DEV_AUTO_START_LISTENING, false),
            synonymsEnabled = reader.getBoolean(SettingsKeys.DEV_SYNONYMS_ENABLED, true),
            sttEngine = reader.getString(SettingsKeys.DEV_STT_ENGINE, DeveloperSettings.DEFAULT_STT_ENGINE),
            voiceLanguage = reader.getString(SettingsKeys.DEV_VOICE_LANGUAGE, DeveloperSettings.DEFAULT_VOICE_LANGUAGE),
            contentChangeDebounceMs = reader.getLong(SettingsKeys.DEV_CONTENT_CHANGE_DEBOUNCE_MS, DeveloperSettings.DEFAULT_CONTENT_CHANGE_DEBOUNCE_MS),
            scrollEventDebounceMs = reader.getLong(SettingsKeys.DEV_SCROLL_EVENT_DEBOUNCE_MS, DeveloperSettings.DEFAULT_SCROLL_EVENT_DEBOUNCE_MS),
            screenChangeDelayMs = reader.getLong(SettingsKeys.DEV_SCREEN_CHANGE_DELAY_MS, DeveloperSettings.DEFAULT_SCREEN_CHANGE_DELAY_MS),
            developerModeActivated = reader.getBoolean(SettingsKeys.DEV_MODE_ACTIVATED, false)
        )
    }

    override fun encode(value: DeveloperSettings, writer: PreferenceWriter) {
        writer.putLong(SettingsKeys.DEV_STT_TIMEOUT_MS, value.sttTimeoutMs)
        writer.putLong(SettingsKeys.DEV_END_OF_SPEECH_DELAY_MS, value.endOfSpeechDelayMs)
        writer.putLong(SettingsKeys.DEV_PARTIAL_RESULT_INTERVAL_MS, value.partialResultIntervalMs)
        writer.putFloat(SettingsKeys.DEV_CONFIDENCE_THRESHOLD, value.confidenceThreshold)
        writer.putBoolean(SettingsKeys.DEV_DEBUG_MODE, value.debugMode)
        writer.putBoolean(SettingsKeys.DEV_VERBOSE_LOGGING, value.verboseLogging)
        writer.putBoolean(SettingsKeys.DEV_DEBUG_OVERLAY, value.debugOverlay)
        writer.putInt(SettingsKeys.DEV_SCANNER_VERBOSITY, value.scannerVerbosity)
        writer.putBoolean(SettingsKeys.DEV_AUTO_START_LISTENING, value.autoStartListening)
        writer.putBoolean(SettingsKeys.DEV_SYNONYMS_ENABLED, value.synonymsEnabled)
        writer.putString(SettingsKeys.DEV_STT_ENGINE, value.sttEngine)
        writer.putString(SettingsKeys.DEV_VOICE_LANGUAGE, value.voiceLanguage)
        writer.putLong(SettingsKeys.DEV_CONTENT_CHANGE_DEBOUNCE_MS, value.contentChangeDebounceMs)
        writer.putLong(SettingsKeys.DEV_SCROLL_EVENT_DEBOUNCE_MS, value.scrollEventDebounceMs)
        writer.putLong(SettingsKeys.DEV_SCREEN_CHANGE_DELAY_MS, value.screenChangeDelayMs)
        writer.putBoolean(SettingsKeys.DEV_MODE_ACTIVATED, value.developerModeActivated)
    }
}
