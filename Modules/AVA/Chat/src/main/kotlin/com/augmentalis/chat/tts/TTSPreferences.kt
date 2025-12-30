package com.augmentalis.chat.tts

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TTS preferences manager using SharedPreferences.
 *
 * Manages user preferences for text-to-speech functionality:
 * - Global enable/disable
 * - Auto-speak assistant responses
 * - Voice selection
 * - Speech rate and pitch
 * - Punctuation announcement
 *
 * Thread-safe: All operations synchronized via SharedPreferences
 * Reactive: Exposes StateFlow for reactive UI updates
 *
 * @param context Application context for SharedPreferences
 */
class TTSPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // ==================== Reactive State ====================

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<TTSSettings> = _settings.asStateFlow()

    // ==================== Public API ====================

    /**
     * Get current TTS settings.
     *
     * @return Current TTSSettings
     */
    fun getSettings(): TTSSettings {
        return _settings.value
    }

    /**
     * Update TTS settings.
     *
     * @param settings New settings to save
     */
    fun updateSettings(settings: TTSSettings) {
        prefs.edit().apply {
            putBoolean(KEY_ENABLED, settings.enabled)
            putBoolean(KEY_AUTO_SPEAK, settings.autoSpeak)
            putString(KEY_SELECTED_VOICE, settings.selectedVoice)
            putFloat(KEY_SPEECH_RATE, settings.speechRate)
            putFloat(KEY_PITCH, settings.pitch)
            putBoolean(KEY_SPEAK_PUNCTUATION, settings.speakPunctuation)
            apply()
        }

        _settings.value = settings
    }

    /**
     * Toggle TTS enabled state.
     *
     * @return New enabled state
     */
    fun toggleEnabled(): Boolean {
        val newEnabled = !_settings.value.enabled
        updateSettings(_settings.value.copy(enabled = newEnabled))
        return newEnabled
    }

    /**
     * Toggle auto-speak state.
     *
     * @return New auto-speak state
     */
    fun toggleAutoSpeak(): Boolean {
        val newAutoSpeak = !_settings.value.autoSpeak
        updateSettings(_settings.value.copy(autoSpeak = newAutoSpeak))
        return newAutoSpeak
    }

    /**
     * Set speech rate.
     *
     * @param rate Speech rate (0.5 - 2.0)
     */
    fun setSpeechRate(rate: Float) {
        val clampedRate = rate.coerceIn(TTSManager.MIN_SPEECH_RATE, TTSManager.MAX_SPEECH_RATE)
        updateSettings(_settings.value.copy(speechRate = clampedRate))
    }

    /**
     * Set pitch.
     *
     * @param pitch Pitch (0.5 - 2.0)
     */
    fun setPitch(pitch: Float) {
        val clampedPitch = pitch.coerceIn(TTSManager.MIN_PITCH, TTSManager.MAX_PITCH)
        updateSettings(_settings.value.copy(pitch = clampedPitch))
    }

    /**
     * Set selected voice.
     *
     * @param voiceId Voice ID (null = system default)
     */
    fun setSelectedVoice(voiceId: String?) {
        updateSettings(_settings.value.copy(selectedVoice = voiceId))
    }

    /**
     * Reset settings to defaults.
     */
    fun resetToDefaults() {
        updateSettings(TTSSettings.DEFAULT)
    }

    /**
     * Clear all TTS preferences.
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        _settings.value = TTSSettings.DEFAULT
    }

    // ==================== Private Methods ====================

    /**
     * Load settings from SharedPreferences.
     */
    private fun loadSettings(): TTSSettings {
        return TTSSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            autoSpeak = prefs.getBoolean(KEY_AUTO_SPEAK, false),
            selectedVoice = prefs.getString(KEY_SELECTED_VOICE, null),
            speechRate = prefs.getFloat(KEY_SPEECH_RATE, 1.0f),
            pitch = prefs.getFloat(KEY_PITCH, 1.0f),
            speakPunctuation = prefs.getBoolean(KEY_SPEAK_PUNCTUATION, false)
        )
    }

    companion object {
        private const val PREFS_NAME = "tts_preferences"
        private const val KEY_ENABLED = "tts_enabled"
        private const val KEY_AUTO_SPEAK = "auto_speak"
        private const val KEY_SELECTED_VOICE = "selected_voice"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_PITCH = "pitch"
        private const val KEY_SPEAK_PUNCTUATION = "speak_punctuation"

        @Volatile
        private var INSTANCE: TTSPreferences? = null

        /**
         * Get singleton instance of TTSPreferences.
         *
         * @param context Application context
         * @return TTSPreferences instance
         */
        fun getInstance(context: Context): TTSPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TTSPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
