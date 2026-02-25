/**
 * KeyboardPreferences.kt - Keyboard preferences management
 * 
 * Author: Manoj Jhawar
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.augmentalis.voicekeyboard.ui.KeyboardTheme
import com.augmentalis.voicekeyboard.utils.KeyboardConstants

/**
 * Manages keyboard preferences and settings
 * 
 * SOLID Principles:
 * - Single Responsibility: Only manages preferences
 * - Open/Closed: Extensible through new preference methods
 */
class KeyboardPreferences(context: Context) {
    
    companion object {
        // Preference keys
        private const val PREF_VOICE_INPUT_ENABLED = "pref_voice_input_enabled"
        private const val PREF_AUTO_VOICE_INPUT = "pref_auto_voice_input"
        private const val PREF_DICTATION_TIMEOUT = "pref_dictation_timeout"
        private const val PREF_DICTATION_START_COMMAND = "pref_dictation_start_command"
        private const val PREF_DICTATION_STOP_COMMAND = "pref_dictation_stop_command"
        
        private const val PREF_GESTURE_TYPING_ENABLED = "pref_gesture_typing_enabled"
        private const val PREF_SWIPE_GESTURES_ENABLED = "pref_swipe_gestures_enabled"
        
        private const val PREF_AUTO_CAPITALIZATION = "pref_auto_capitalization"
        private const val PREF_AUTO_CORRECTION = "pref_auto_correction"
        private const val PREF_SUGGESTIONS_ENABLED = "pref_suggestions_enabled"
        private const val PREF_NEXT_WORD_PREDICTION = "pref_next_word_prediction"
        
        private const val PREF_SOUND_ON_KEYPRESS = "pref_sound_on_keypress"
        private const val PREF_VIBRATE_ON_KEYPRESS = "pref_vibrate_on_keypress"
        private const val PREF_KEY_PREVIEW_POPUP = "pref_key_preview_popup"
        
        private const val PREF_KEYBOARD_THEME = "pref_keyboard_theme"
        private const val PREF_KEY_HEIGHT = "pref_key_height"
        private const val PREF_KEYBOARD_LANGUAGE = "pref_keyboard_language"
        
        // Default values
        private const val DEFAULT_DICTATION_TIMEOUT = 5 // seconds
        private const val DEFAULT_KEY_HEIGHT = 60 // dp
    }
    
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    
    // Voice input preferences
    
    fun isVoiceInputEnabled(): Boolean {
        return prefs.getBoolean(PREF_VOICE_INPUT_ENABLED, true)
    }
    
    fun setVoiceInputEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_VOICE_INPUT_ENABLED, enabled).apply()
    }
    
    fun isAutoVoiceInputEnabled(): Boolean {
        return prefs.getBoolean(PREF_AUTO_VOICE_INPUT, false)
    }
    
    fun setAutoVoiceInputEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_VOICE_INPUT, enabled).apply()
    }
    
    fun getDictationTimeout(): Int {
        return prefs.getInt(PREF_DICTATION_TIMEOUT, DEFAULT_DICTATION_TIMEOUT)
    }
    
    fun setDictationTimeout(seconds: Int) {
        prefs.edit().putInt(PREF_DICTATION_TIMEOUT, seconds).apply()
    }
    
    fun getDictationStartCommand(): String {
        return prefs.getString(
            PREF_DICTATION_START_COMMAND,
            KeyboardConstants.DEFAULT_DICTATION_START_COMMAND
        ) ?: KeyboardConstants.DEFAULT_DICTATION_START_COMMAND
    }
    
    fun setDictationStartCommand(command: String) {
        prefs.edit().putString(PREF_DICTATION_START_COMMAND, command).apply()
    }
    
    fun getDictationStopCommand(): String {
        return prefs.getString(
            PREF_DICTATION_STOP_COMMAND,
            KeyboardConstants.DEFAULT_DICTATION_STOP_COMMAND
        ) ?: KeyboardConstants.DEFAULT_DICTATION_STOP_COMMAND
    }
    
    fun setDictationStopCommand(command: String) {
        prefs.edit().putString(PREF_DICTATION_STOP_COMMAND, command).apply()
    }
    
    // Gesture typing preferences
    
    fun isGestureTypingEnabled(): Boolean {
        return prefs.getBoolean(PREF_GESTURE_TYPING_ENABLED, true)
    }
    
    fun setGestureTypingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_GESTURE_TYPING_ENABLED, enabled).apply()
    }
    
    fun isSwipeEnabled(): Boolean {
        return prefs.getBoolean(PREF_SWIPE_GESTURES_ENABLED, true)
    }
    
    fun setSwipeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_SWIPE_GESTURES_ENABLED, enabled).apply()
    }
    
    // Text processing preferences
    
    fun isAutoCapitalizationEnabled(): Boolean {
        return prefs.getBoolean(PREF_AUTO_CAPITALIZATION, true)
    }
    
    fun setAutoCapitalizationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_CAPITALIZATION, enabled).apply()
    }
    
    fun isAutoCorrectionEnabled(): Boolean {
        return prefs.getBoolean(PREF_AUTO_CORRECTION, true)
    }
    
    fun setAutoCorrectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_CORRECTION, enabled).apply()
    }
    
    fun areSuggestionsEnabled(): Boolean {
        return prefs.getBoolean(PREF_SUGGESTIONS_ENABLED, true)
    }
    
    fun setSuggestionsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_SUGGESTIONS_ENABLED, enabled).apply()
    }
    
    fun isNextWordPredictionEnabled(): Boolean {
        return prefs.getBoolean(PREF_NEXT_WORD_PREDICTION, true)
    }
    
    fun setNextWordPredictionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_NEXT_WORD_PREDICTION, enabled).apply()
    }
    
    // Feedback preferences
    
    fun isSoundOnKeypressEnabled(): Boolean {
        return prefs.getBoolean(PREF_SOUND_ON_KEYPRESS, false)
    }
    
    fun setSoundOnKeypressEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_SOUND_ON_KEYPRESS, enabled).apply()
    }
    
    fun isVibrateOnKeypressEnabled(): Boolean {
        return prefs.getBoolean(PREF_VIBRATE_ON_KEYPRESS, true)
    }
    
    fun setVibrateOnKeypressEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_VIBRATE_ON_KEYPRESS, enabled).apply()
    }
    
    fun isKeyPreviewPopupEnabled(): Boolean {
        return prefs.getBoolean(PREF_KEY_PREVIEW_POPUP, true)
    }
    
    fun setKeyPreviewPopupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_KEY_PREVIEW_POPUP, enabled).apply()
    }
    
    // Appearance preferences
    
    fun getKeyboardTheme(): KeyboardTheme {
        val themeName = prefs.getString(PREF_KEYBOARD_THEME, KeyboardTheme.SYSTEM.name)
        return try {
            KeyboardTheme.valueOf(themeName ?: KeyboardTheme.SYSTEM.name)
        } catch (e: IllegalArgumentException) {
            KeyboardTheme.SYSTEM
        }
    }
    
    fun setKeyboardTheme(theme: KeyboardTheme) {
        prefs.edit().putString(PREF_KEYBOARD_THEME, theme.name).apply()
    }
    
    fun getKeyHeight(): Int {
        return prefs.getInt(PREF_KEY_HEIGHT, DEFAULT_KEY_HEIGHT)
    }
    
    fun setKeyHeight(height: Int) {
        prefs.edit().putInt(PREF_KEY_HEIGHT, height).apply()
    }
    
    fun getKeyboardLanguage(): String {
        return prefs.getString(PREF_KEYBOARD_LANGUAGE, "en_US") ?: "en_US"
    }
    
    fun setKeyboardLanguage(language: String) {
        prefs.edit().putString(PREF_KEYBOARD_LANGUAGE, language).apply()
    }
    
    // Reset preferences
    
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
    
    // Export/Import preferences
    
    fun exportPreferences(): Map<String, Any?> {
        return prefs.all
    }
    
    fun importPreferences(preferences: Map<String, Any?>) {
        val editor = prefs.edit()
        
        preferences.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    editor.putStringSet(key, value as Set<String>)
                }
            }
        }
        
        editor.apply()
    }
}