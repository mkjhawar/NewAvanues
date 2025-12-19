/**
 * VoiceOS Settings DataStore
 *
 * Provides persistent storage for VoiceOS application settings using Jetpack DataStore.
 * Offers reactive Flow-based API for settings observation and suspension-based update functions.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Extension property to create/access the DataStore instance.
 * This is the recommended approach per Android documentation.
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "voiceos_settings"
)

/**
 * Data class representing VoiceOS application settings.
 *
 * @property voiceEngine The selected voice recognition engine (default: "Default")
 * @property continuousListening Whether continuous listening mode is enabled (default: false)
 * @property visualFeedback Whether visual feedback is enabled (default: true)
 * @property audioFeedback Whether audio feedback is enabled (default: true)
 */
data class VoiceOSSettings(
    val voiceEngine: String = "Default",
    val continuousListening: Boolean = false,
    val visualFeedback: Boolean = true,
    val audioFeedback: Boolean = true
)

/**
 * DataStore manager for VoiceOS settings.
 *
 * Provides reactive access to application settings through Flow and
 * suspend functions for updating individual settings. All operations
 * are thread-safe and optimized for concurrent access.
 *
 * Example usage:
 * ```kotlin
 * // Observe settings
 * settingsDataStore.settings.collect { settings ->
 *     println("Voice engine: ${settings.voiceEngine}")
 * }
 *
 * // Update setting
 * settingsDataStore.updateVoiceEngine("Google")
 * ```
 *
 * @property context Application context for DataStore access
 */
class VoiceOSSettingsDataStore(private val context: Context) {

    /**
     * Preference keys for storing settings.
     */
    private object PreferencesKeys {
        val VOICE_ENGINE = stringPreferencesKey("voice_engine")
        val CONTINUOUS_LISTENING = booleanPreferencesKey("continuous_listening")
        val VISUAL_FEEDBACK = booleanPreferencesKey("visual_feedback")
        val AUDIO_FEEDBACK = booleanPreferencesKey("audio_feedback")
    }

    /**
     * Flow of current VoiceOS settings.
     *
     * Emits the latest settings whenever any setting changes.
     * Use this for reactive UI updates or business logic that depends on settings.
     *
     * @return Flow emitting [VoiceOSSettings] with current values
     */
    val settings: Flow<VoiceOSSettings> = context.settingsDataStore.data.map { preferences ->
        VoiceOSSettings(
            voiceEngine = preferences[PreferencesKeys.VOICE_ENGINE] ?: "Default",
            continuousListening = preferences[PreferencesKeys.CONTINUOUS_LISTENING] ?: false,
            visualFeedback = preferences[PreferencesKeys.VISUAL_FEEDBACK] ?: true,
            audioFeedback = preferences[PreferencesKeys.AUDIO_FEEDBACK] ?: true
        )
    }

    /**
     * Updates the voice recognition engine setting.
     *
     * @param engine The name of the voice engine to use (e.g., "Google", "Vivoka", "Default")
     */
    suspend fun updateVoiceEngine(engine: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.VOICE_ENGINE] = engine
        }
    }

    /**
     * Updates the continuous listening mode setting.
     *
     * When enabled, the system continuously listens for voice commands
     * without requiring a wake word for each command.
     *
     * @param enabled true to enable continuous listening, false to disable
     */
    suspend fun updateContinuousListening(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTINUOUS_LISTENING] = enabled
        }
    }

    /**
     * Updates the visual feedback setting.
     *
     * When enabled, the system displays visual indicators for voice commands,
     * recognition status, and system responses.
     *
     * @param enabled true to enable visual feedback, false to disable
     */
    suspend fun updateVisualFeedback(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.VISUAL_FEEDBACK] = enabled
        }
    }

    /**
     * Updates the audio feedback setting.
     *
     * When enabled, the system provides audio cues for voice commands,
     * recognition results, and system responses.
     *
     * @param enabled true to enable audio feedback, false to disable
     */
    suspend fun updateAudioFeedback(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_FEEDBACK] = enabled
        }
    }

    /**
     * Resets all settings to their default values.
     *
     * This is useful for troubleshooting or providing a "reset to defaults"
     * option in the settings UI.
     */
    suspend fun resetToDefaults() {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.VOICE_ENGINE] = "Default"
            preferences[PreferencesKeys.CONTINUOUS_LISTENING] = false
            preferences[PreferencesKeys.VISUAL_FEEDBACK] = true
            preferences[PreferencesKeys.AUDIO_FEEDBACK] = true
        }
    }

    /**
     * Clears all settings from the DataStore.
     *
     * After calling this, all settings will return to their default values.
     * This is primarily useful for testing or complete app reset scenarios.
     */
    suspend fun clearAll() {
        context.settingsDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
