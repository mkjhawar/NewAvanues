package com.augmentalis.ava.features.chat.tts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for TTS settings management.
 *
 * Responsibilities:
 * - Manage TTS settings state
 * - Provide voice selection options
 * - Handle settings updates
 * - Coordinate with TTSManager and TTSPreferences
 *
 * Dependencies injected via Hilt:
 * @param ttsManager TTS engine manager
 * @param ttsPreferences TTS preferences storage
 */
@HiltViewModel
class TTSViewModel @Inject constructor(
    private val ttsManager: TTSManager,
    private val ttsPreferences: TTSPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "TTSViewModel"
    }

    // ==================== State ====================

    /**
     * Current TTS settings
     */
    private val _settings = MutableStateFlow(ttsPreferences.getSettings())
    val settings: StateFlow<TTSSettings> = _settings.asStateFlow()

    /**
     * TTS initialization state
     */
    val isInitialized: StateFlow<Boolean> = ttsManager.isInitialized

    /**
     * Currently speaking state
     */
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    /**
     * Available voices
     */
    val availableVoices: StateFlow<List<VoiceInfo>> = ttsManager.availableVoices

    /**
     * Initialization error
     */
    val initError: StateFlow<String?> = ttsManager.initError

    /**
     * Loading state for async operations
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Error message state
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ==================== Initialization ====================

    init {
        Log.d(TAG, "TTSViewModel initialized")
        observePreferences()
    }

    /**
     * Observe preferences changes and sync to TTSManager.
     */
    private fun observePreferences() {
        viewModelScope.launch {
            ttsPreferences.settings.collect { settings ->
                _settings.value = settings
                ttsManager.updateSettings(settings)
                Log.d(TAG, "Settings updated: $settings")
            }
        }
    }

    // ==================== Public Methods ====================

    /**
     * Toggle TTS enabled state.
     */
    fun toggleEnabled() {
        viewModelScope.launch {
            val newEnabled = ttsPreferences.toggleEnabled()
            Log.d(TAG, "TTS enabled toggled: $newEnabled")
        }
    }

    /**
     * Toggle auto-speak assistant responses.
     */
    fun toggleAutoSpeak() {
        viewModelScope.launch {
            val newAutoSpeak = ttsPreferences.toggleAutoSpeak()
            Log.d(TAG, "Auto-speak toggled: $newAutoSpeak")
        }
    }

    /**
     * Set speech rate.
     *
     * @param rate Speech rate (0.5 - 2.0)
     */
    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            ttsPreferences.setSpeechRate(rate)
            Log.d(TAG, "Speech rate set: $rate")
        }
    }

    /**
     * Set pitch.
     *
     * @param pitch Pitch (0.5 - 2.0)
     */
    fun setPitch(pitch: Float) {
        viewModelScope.launch {
            ttsPreferences.setPitch(pitch)
            Log.d(TAG, "Pitch set: $pitch")
        }
    }

    /**
     * Set selected voice.
     *
     * @param voiceId Voice ID (null = system default)
     */
    fun setSelectedVoice(voiceId: String?) {
        viewModelScope.launch {
            ttsPreferences.setSelectedVoice(voiceId)
            Log.d(TAG, "Voice selected: $voiceId")
        }
    }

    /**
     * Test speak a sample text with current settings.
     */
    fun testSpeak() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val sampleText = "Hello! This is AVA's text to speech engine. How do I sound?"

                when (val result = ttsManager.speak(sampleText)) {
                    is com.augmentalis.ava.core.common.Result.Success -> {
                        Log.d(TAG, "Test speak started successfully")
                    }
                    is com.augmentalis.ava.core.common.Result.Error -> {
                        _errorMessage.value = "Test speak failed: ${result.message}"
                        Log.e(TAG, "Test speak failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Test speak exception: ${e.message}"
                Log.e(TAG, "Test speak exception", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Stop current speech.
     */
    fun stopSpeaking() {
        ttsManager.stop()
        Log.d(TAG, "Speech stopped")
    }

    /**
     * Reset settings to defaults.
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            ttsPreferences.resetToDefaults()
            Log.d(TAG, "Settings reset to defaults")
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // ==================== Cleanup ====================

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "TTSViewModel cleared")
        // Note: TTSManager is singleton and managed by app lifecycle
        // Don't shutdown here - it's handled by app component
    }
}
