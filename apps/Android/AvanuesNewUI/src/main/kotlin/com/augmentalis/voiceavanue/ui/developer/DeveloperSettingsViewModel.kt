/**
 * DeveloperSettingsViewModel.kt - ViewModel for developer settings screen
 *
 * Wraps DeveloperPreferencesRepository with reactive state and
 * update methods for each tunable parameter.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.developer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.voiceavanue.data.DeveloperPreferencesKeys
import com.augmentalis.voiceavanue.data.DeveloperPreferencesRepository
import com.augmentalis.foundation.settings.models.DeveloperSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeveloperSettingsViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    private val repository = DeveloperPreferencesRepository(context)

    val settings: StateFlow<DeveloperSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DeveloperSettings()
        )

    // Voice Timings
    fun updateSttTimeout(ms: Long) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.STT_TIMEOUT_MS, ms)
    }

    fun updateEndOfSpeechDelay(ms: Long) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.END_OF_SPEECH_DELAY_MS, ms)
    }

    fun updatePartialResultInterval(ms: Long) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.PARTIAL_RESULT_INTERVAL_MS, ms)
    }

    fun updateConfidenceThreshold(value: Float) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.CONFIDENCE_THRESHOLD, value)
    }

    // Feature Flags
    fun updateDebugMode(enabled: Boolean) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.DEBUG_MODE, enabled)
    }

    fun updateVerboseLogging(enabled: Boolean) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.VERBOSE_LOGGING, enabled)
    }

    fun updateDebugOverlay(enabled: Boolean) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.DEBUG_OVERLAY, enabled)
    }

    fun updateScannerVerbosity(level: Int) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.SCANNER_VERBOSITY, level)
    }

    fun updateAutoStartListening(enabled: Boolean) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.AUTO_START_LISTENING, enabled)
    }

    fun updateSynonymsEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.SYNONYMS_ENABLED, enabled)
    }

    // Engine Selection
    fun updateSttEngine(engine: String) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.STT_ENGINE, engine)
    }

    fun updateVoiceLanguage(language: String) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.VOICE_LANGUAGE, language)
    }

    // Timing / Debounce
    fun updateContentChangeDebounce(ms: Long) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.CONTENT_CHANGE_DEBOUNCE_MS, ms)
    }

    fun updateScrollEventDebounce(ms: Long) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.SCROLL_EVENT_DEBOUNCE_MS, ms)
    }

    fun updateScreenChangeDelay(ms: Long) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.SCREEN_CHANGE_DELAY_MS, ms)
    }

    // Cockpit Debug
    fun updateForceShellMode(mode: String) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.FORCE_SHELL_MODE, mode)
    }

    fun updateShowShellDebugOverlay(enabled: Boolean) = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.SHOW_SHELL_DEBUG_OVERLAY, enabled)
    }

    // Reset all to defaults
    fun resetToDefaults() = viewModelScope.launch {
        repository.updateKey(DeveloperPreferencesKeys.STT_TIMEOUT_MS, 10000L)
        repository.updateKey(DeveloperPreferencesKeys.END_OF_SPEECH_DELAY_MS, 1500L)
        repository.updateKey(DeveloperPreferencesKeys.PARTIAL_RESULT_INTERVAL_MS, 300L)
        repository.updateKey(DeveloperPreferencesKeys.CONFIDENCE_THRESHOLD, 0.7f)
        repository.updateKey(DeveloperPreferencesKeys.DEBUG_MODE, true)
        repository.updateKey(DeveloperPreferencesKeys.VERBOSE_LOGGING, false)
        repository.updateKey(DeveloperPreferencesKeys.DEBUG_OVERLAY, false)
        repository.updateKey(DeveloperPreferencesKeys.SCANNER_VERBOSITY, 0)
        repository.updateKey(DeveloperPreferencesKeys.AUTO_START_LISTENING, false)
        repository.updateKey(DeveloperPreferencesKeys.SYNONYMS_ENABLED, true)
        repository.updateKey(DeveloperPreferencesKeys.STT_ENGINE, "VIVOKA")
        repository.updateKey(DeveloperPreferencesKeys.VOICE_LANGUAGE, "en-US")
        repository.updateKey(DeveloperPreferencesKeys.CONTENT_CHANGE_DEBOUNCE_MS, 300L)
        repository.updateKey(DeveloperPreferencesKeys.SCROLL_EVENT_DEBOUNCE_MS, 150L)
        repository.updateKey(DeveloperPreferencesKeys.SCREEN_CHANGE_DELAY_MS, 200L)
        repository.updateKey(DeveloperPreferencesKeys.FORCE_SHELL_MODE, "")
        repository.updateKey(DeveloperPreferencesKeys.SHOW_SHELL_DEBUG_OVERLAY, false)
    }
}
