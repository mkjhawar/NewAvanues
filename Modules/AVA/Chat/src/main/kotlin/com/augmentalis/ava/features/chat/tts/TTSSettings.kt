package com.augmentalis.ava.features.chat.tts

/**
 * TTS settings data class for AVA AI Phase 1.2.
 *
 * Represents user preferences for text-to-speech functionality.
 * Persisted via TTSPreferences (SharedPreferences wrapper).
 *
 * @param enabled Global TTS enable/disable toggle
 * @param autoSpeak Automatically speak assistant responses
 * @param selectedVoice Selected voice ID (null = system default)
 * @param speechRate Speech rate multiplier (0.5x - 2.0x)
 * @param pitch Pitch multiplier (0.5x - 2.0x)
 * @param speakPunctuation Announce punctuation marks
 */
data class TTSSettings(
    val enabled: Boolean = true,
    val autoSpeak: Boolean = false,
    val selectedVoice: String? = null,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val speakPunctuation: Boolean = false
) {
    companion object {
        /**
         * Default TTS settings.
         */
        val DEFAULT = TTSSettings()
    }
}
