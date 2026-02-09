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
import com.augmentalis.voiceavanue.data.DeveloperSettings
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
        repository.update(DeveloperPreferencesKeys.STT_TIMEOUT_MS, ms)
    }

    fun updateEndOfSpeechDelay(ms: Long) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.END_OF_SPEECH_DELAY_MS, ms)
    }

    fun updatePartialResultInterval(ms: Long) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.PARTIAL_RESULT_INTERVAL_MS, ms)
    }

    fun updateConfidenceThreshold(value: Float) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.CONFIDENCE_THRESHOLD, value)
    }

    // Feature Flags
    fun updateDebugMode(enabled: Boolean) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.DEBUG_MODE, enabled)
    }

    fun updateVerboseLogging(enabled: Boolean) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.VERBOSE_LOGGING, enabled)
    }

    fun updateDebugOverlay(enabled: Boolean) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.DEBUG_OVERLAY, enabled)
    }

    fun updateScannerVerbosity(level: Int) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.SCANNER_VERBOSITY, level)
    }

    fun updateAutoStartListening(enabled: Boolean) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.AUTO_START_LISTENING, enabled)
    }

    fun updateSynonymsEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.SYNONYMS_ENABLED, enabled)
    }

    // Engine Selection
    fun updateSttEngine(engine: String) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.STT_ENGINE, engine)
    }

    fun updateVoiceLanguage(language: String) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.VOICE_LANGUAGE, language)
    }

    // Timing / Debounce
    fun updateContentChangeDebounce(ms: Long) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.CONTENT_CHANGE_DEBOUNCE_MS, ms)
    }

    fun updateScrollEventDebounce(ms: Long) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.SCROLL_EVENT_DEBOUNCE_MS, ms)
    }

    fun updateScreenChangeDelay(ms: Long) = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.SCREEN_CHANGE_DELAY_MS, ms)
    }

    // Reset all to defaults
    fun resetToDefaults() = viewModelScope.launch {
        repository.update(DeveloperPreferencesKeys.STT_TIMEOUT_MS, 10000L)
        repository.update(DeveloperPreferencesKeys.END_OF_SPEECH_DELAY_MS, 1500L)
        repository.update(DeveloperPreferencesKeys.PARTIAL_RESULT_INTERVAL_MS, 300L)
        repository.update(DeveloperPreferencesKeys.CONFIDENCE_THRESHOLD, 0.7f)
        repository.update(DeveloperPreferencesKeys.DEBUG_MODE, true)
        repository.update(DeveloperPreferencesKeys.VERBOSE_LOGGING, false)
        repository.update(DeveloperPreferencesKeys.DEBUG_OVERLAY, false)
        repository.update(DeveloperPreferencesKeys.SCANNER_VERBOSITY, 0)
        repository.update(DeveloperPreferencesKeys.AUTO_START_LISTENING, false)
        repository.update(DeveloperPreferencesKeys.SYNONYMS_ENABLED, true)
        repository.update(DeveloperPreferencesKeys.STT_ENGINE, "VIVOKA")
        repository.update(DeveloperPreferencesKeys.VOICE_LANGUAGE, "en-US")
        repository.update(DeveloperPreferencesKeys.CONTENT_CHANGE_DEBOUNCE_MS, 300L)
        repository.update(DeveloperPreferencesKeys.SCROLL_EVENT_DEBOUNCE_MS, 150L)
        repository.update(DeveloperPreferencesKeys.SCREEN_CHANGE_DELAY_MS, 200L)
    }
}
