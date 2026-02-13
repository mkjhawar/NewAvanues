/**
 * DeveloperPreferences.kt - DataStore keys for developer/debug settings
 *
 * Read by VoiceAvanueAccessibilityService to construct ServiceConfiguration.
 * Modified via DeveloperSettingsScreen (hidden behind 4-tap entry).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.augmentalis.foundation.settings.models.DeveloperSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * All developer-tunable preference keys.
 * Stored in the single avanues_settings DataStore.
 */
object DeveloperPreferencesKeys {
    // Voice Timings
    val STT_TIMEOUT_MS = longPreferencesKey("dev_stt_timeout_ms")
    val END_OF_SPEECH_DELAY_MS = longPreferencesKey("dev_end_of_speech_delay_ms")
    val PARTIAL_RESULT_INTERVAL_MS = longPreferencesKey("dev_partial_result_interval_ms")
    val CONFIDENCE_THRESHOLD = floatPreferencesKey("dev_confidence_threshold")

    // Feature Flags
    val DEBUG_MODE = booleanPreferencesKey("dev_debug_mode")
    val VERBOSE_LOGGING = booleanPreferencesKey("dev_verbose_logging")
    val DEBUG_OVERLAY = booleanPreferencesKey("dev_debug_overlay")
    val SCANNER_VERBOSITY = intPreferencesKey("dev_scanner_verbosity")
    val AUTO_START_LISTENING = booleanPreferencesKey("dev_auto_start_listening")
    val SYNONYMS_ENABLED = booleanPreferencesKey("dev_synonyms_enabled")

    // Engine Selection
    val STT_ENGINE = stringPreferencesKey("dev_stt_engine")
    val VOICE_LANGUAGE = stringPreferencesKey("dev_voice_language")

    // Timing / Debounce
    val CONTENT_CHANGE_DEBOUNCE_MS = longPreferencesKey("dev_content_change_debounce_ms")
    val SCROLL_EVENT_DEBOUNCE_MS = longPreferencesKey("dev_scroll_event_debounce_ms")
    val SCREEN_CHANGE_DELAY_MS = longPreferencesKey("dev_screen_change_delay_ms")

    // Developer mode activation
    val DEVELOPER_MODE_ACTIVATED = booleanPreferencesKey("dev_mode_activated")
}

// DeveloperSettings data class now in Foundation: com.augmentalis.foundation.settings.models.DeveloperSettings

/**
 * Repository for developer preferences.
 */
class DeveloperPreferencesRepository(private val context: Context) {

    val settings: Flow<DeveloperSettings> = context.avanuesDataStore.data.map { prefs ->
        DeveloperSettings(
            sttTimeoutMs = prefs[DeveloperPreferencesKeys.STT_TIMEOUT_MS] ?: 10000L,
            endOfSpeechDelayMs = prefs[DeveloperPreferencesKeys.END_OF_SPEECH_DELAY_MS] ?: 1500L,
            partialResultIntervalMs = prefs[DeveloperPreferencesKeys.PARTIAL_RESULT_INTERVAL_MS] ?: 300L,
            confidenceThreshold = prefs[DeveloperPreferencesKeys.CONFIDENCE_THRESHOLD] ?: 0.45f,
            debugMode = prefs[DeveloperPreferencesKeys.DEBUG_MODE] ?: true,
            verboseLogging = prefs[DeveloperPreferencesKeys.VERBOSE_LOGGING] ?: false,
            debugOverlay = prefs[DeveloperPreferencesKeys.DEBUG_OVERLAY] ?: false,
            scannerVerbosity = prefs[DeveloperPreferencesKeys.SCANNER_VERBOSITY] ?: 0,
            autoStartListening = prefs[DeveloperPreferencesKeys.AUTO_START_LISTENING] ?: false,
            synonymsEnabled = prefs[DeveloperPreferencesKeys.SYNONYMS_ENABLED] ?: true,
            sttEngine = prefs[DeveloperPreferencesKeys.STT_ENGINE] ?: "VIVOKA",
            voiceLanguage = prefs[DeveloperPreferencesKeys.VOICE_LANGUAGE] ?: "en-US",
            contentChangeDebounceMs = prefs[DeveloperPreferencesKeys.CONTENT_CHANGE_DEBOUNCE_MS] ?: 300L,
            scrollEventDebounceMs = prefs[DeveloperPreferencesKeys.SCROLL_EVENT_DEBOUNCE_MS] ?: 150L,
            screenChangeDelayMs = prefs[DeveloperPreferencesKeys.SCREEN_CHANGE_DELAY_MS] ?: 200L,
            developerModeActivated = prefs[DeveloperPreferencesKeys.DEVELOPER_MODE_ACTIVATED] ?: false
        )
    }

    suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        context.avanuesDataStore.edit { prefs ->
            prefs[key] = value
        }
    }

    suspend fun activateDeveloperMode() {
        update(DeveloperPreferencesKeys.DEVELOPER_MODE_ACTIVATED, true)
    }
}
