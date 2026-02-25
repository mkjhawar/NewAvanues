/**
 * KeyboardPreferencesManager.kt - Interface for keyboard preferences
 * Direct implementation - no unnecessary abstractions
 * 
 * Author: Manoj Jhawar
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.interfaces

import com.augmentalis.voicekeyboard.ui.KeyboardTheme

/**
 * Interface for managing keyboard preferences
 */
interface KeyboardPreferencesManager {
    fun isVoiceInputEnabled(): Boolean
    fun isAutoVoiceInputEnabled(): Boolean
    fun isGestureTypingEnabled(): Boolean
    fun isSwipeEnabled(): Boolean
    fun isAutoCapitalizationEnabled(): Boolean
    fun areSuggestionsEnabled(): Boolean
    fun getKeyboardTheme(): KeyboardTheme
    fun getDictationTimeout(): Int
    fun getDictationStartCommand(): String
    fun getDictationStopCommand(): String
}