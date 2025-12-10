/**
 * KeyboardServiceContainer.kt - Dependency injection container
 * Direct implementation - no unnecessary abstractions
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.di

import android.content.Context
import com.augmentalis.voicekeyboard.interfaces.*
import com.augmentalis.voicekeyboard.services.*

/**
 * Simple dependency injection container for keyboard services
 * Creates and manages service instances
 */
class KeyboardServiceContainer(private val context: Context) {
    
    // Lazy initialization of services
    private val _preferencesService by lazy { PreferencesService(context) }
    private val _textInputService by lazy { TextInputService() }
    private val _voiceInputService by lazy { VoiceInputService(context) }
    private val _gestureService by lazy { GestureService(context) }
    
    // Dictation service needs callbacks, so it's created on demand
    private var _dictationService: DictationService? = null
    
    /**
     * Get preferences manager
     */
    fun getPreferencesManager(): KeyboardPreferencesManager = _preferencesService
    
    /**
     * Get input processor
     */
    fun getInputProcessor(): InputProcessor = _textInputService
    
    /**
     * Get voice input listener
     */
    fun getVoiceInputListener(): VoiceInputListener = _voiceInputService
    
    /**
     * Get gesture processor
     */
    fun getGestureProcessor(): GestureProcessor = _gestureService
    
    /**
     * Get dictation manager with callbacks
     */
    fun getDictationManager(
        onDictationResult: (String) -> Unit,
        onDictationStateChanged: (Boolean) -> Unit
    ): DictationManager {
        if (_dictationService == null) {
            _dictationService = DictationService(context, onDictationResult, onDictationStateChanged)
        }
        return _dictationService!!
    }
    
    /**
     * Release all services
     */
    fun release() {
        _voiceInputService.release()
        _gestureService.release()
        _dictationService?.destroy()
        _dictationService = null
    }
}