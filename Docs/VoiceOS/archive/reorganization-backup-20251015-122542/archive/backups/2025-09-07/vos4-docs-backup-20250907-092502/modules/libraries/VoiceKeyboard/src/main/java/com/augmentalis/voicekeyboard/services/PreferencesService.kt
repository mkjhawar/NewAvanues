/**
 * PreferencesService.kt - Preferences service implementation
 * Direct implementation - no unnecessary abstractions
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.services

import android.content.Context
import com.augmentalis.voicekeyboard.interfaces.KeyboardPreferencesManager
import com.augmentalis.voicekeyboard.preferences.KeyboardPreferences
import com.augmentalis.voicekeyboard.ui.KeyboardTheme

/**
 * Service implementation for keyboard preferences
 * Wraps KeyboardPreferences to implement the interface
 */
class PreferencesService(context: Context) : KeyboardPreferencesManager {
    
    private val preferences = KeyboardPreferences(context)
    
    override fun isVoiceInputEnabled(): Boolean = preferences.isVoiceInputEnabled()
    
    override fun isAutoVoiceInputEnabled(): Boolean = preferences.isAutoVoiceInputEnabled()
    
    override fun isGestureTypingEnabled(): Boolean = preferences.isGestureTypingEnabled()
    
    override fun isSwipeEnabled(): Boolean = preferences.isSwipeEnabled()
    
    override fun isAutoCapitalizationEnabled(): Boolean = preferences.isAutoCapitalizationEnabled()
    
    override fun areSuggestionsEnabled(): Boolean = preferences.areSuggestionsEnabled()
    
    override fun getKeyboardTheme(): KeyboardTheme = preferences.getKeyboardTheme()
    
    override fun getDictationTimeout(): Int = preferences.getDictationTimeout()
    
    override fun getDictationStartCommand(): String = preferences.getDictationStartCommand()
    
    override fun getDictationStopCommand(): String = preferences.getDictationStopCommand()
}