/**
 * SettingsViewModel.kt - ViewModel for the Settings screen
 *
 * Manages VoiceOS application settings using VoiceOSSettingsDataStore.
 * Provides reactive access to settings through StateFlow and functions
 * to update individual settings. All operations are thread-safe and
 * automatically persisted to DataStore.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.voiceos.data.VoiceOSSettings
import com.augmentalis.voiceos.data.VoiceOSSettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 *
 * Manages VoiceOS application settings including voice engine selection,
 * continuous listening mode, visual feedback, and audio feedback. Settings
 * are automatically persisted and synchronized across app restarts.
 *
 * Example usage:
 * ```kotlin
 * val viewModel: SettingsViewModel by viewModel()
 * val settings by viewModel.settings.collectAsState()
 *
 * // Display current settings
 * Text("Voice Engine: ${settings.voiceEngine}")
 * Switch(
 *     checked = settings.continuousListening,
 *     onCheckedChange = { viewModel.setContinuousListening(it) }
 * )
 *
 * // Update settings
 * viewModel.setVoiceEngine("Google")
 * viewModel.setVisualFeedback(true)
 * ```
 *
 * @property settingsDataStore The DataStore manager for persistent settings
 */
class SettingsViewModel(
    private val settingsDataStore: VoiceOSSettingsDataStore
) : ViewModel() {

    /**
     * Current VoiceOS settings.
     *
     * This StateFlow emits the latest settings whenever any setting changes.
     * The flow is automatically updated when settings are modified through
     * the setter functions in this ViewModel.
     *
     * The StateFlow is kept alive as long as the ViewModel is active,
     * using WhileSubscribed with a 5-second timeout to avoid unnecessary
     * work when no observers are present.
     */
    val settings: StateFlow<VoiceOSSettings> = settingsDataStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VoiceOSSettings()
    )

    /**
     * Sets the voice recognition engine.
     *
     * Updates the voice engine setting to the specified value.
     * The change is automatically persisted to DataStore and
     * will be reflected in the [settings] StateFlow.
     *
     * @param engine The name of the voice engine (e.g., "Google", "Vivoka", "Default")
     */
    fun setVoiceEngine(engine: String) {
        viewModelScope.launch {
            settingsDataStore.updateVoiceEngine(engine)
        }
    }

    /**
     * Sets the continuous listening mode.
     *
     * When enabled, the system continuously listens for voice commands
     * without requiring a wake word for each command.
     *
     * @param enabled true to enable continuous listening, false to disable
     */
    fun setContinuousListening(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateContinuousListening(enabled)
        }
    }

    /**
     * Sets the visual feedback preference.
     *
     * When enabled, the system displays visual indicators for voice commands,
     * recognition status, and system responses.
     *
     * @param enabled true to enable visual feedback, false to disable
     */
    fun setVisualFeedback(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateVisualFeedback(enabled)
        }
    }

    /**
     * Sets the audio feedback preference.
     *
     * When enabled, the system provides audio cues for voice commands,
     * recognition results, and system responses.
     *
     * @param enabled true to enable audio feedback, false to disable
     */
    fun setAudioFeedback(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAudioFeedback(enabled)
        }
    }

    /**
     * Resets all settings to their default values.
     *
     * This is useful for troubleshooting or providing a "reset to defaults"
     * option in the settings UI. After calling this, settings will be:
     * - Voice Engine: "Default"
     * - Continuous Listening: false
     * - Visual Feedback: true
     * - Audio Feedback: true
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsDataStore.resetToDefaults()
        }
    }
}
