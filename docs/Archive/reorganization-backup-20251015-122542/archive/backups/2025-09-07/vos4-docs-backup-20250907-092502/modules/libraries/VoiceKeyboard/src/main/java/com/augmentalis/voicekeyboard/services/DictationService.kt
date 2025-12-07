/**
 * DictationService.kt - Dictation service implementation
 * Direct implementation - no unnecessary abstractions
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.services

import android.content.Context
import com.augmentalis.voicekeyboard.interfaces.DictationManager
import com.augmentalis.voicekeyboard.voice.DictationHandler
import kotlinx.coroutines.flow.StateFlow

/**
 * Service implementation for dictation management
 * Wraps DictationHandler to implement the interface
 */
class DictationService(
    context: Context,
    onDictationResult: (String) -> Unit,
    onDictationStateChanged: (Boolean) -> Unit
) : DictationManager {
    
    private val dictationHandler = DictationHandler(
        context = context,
        onDictationResult = onDictationResult,
        onDictationStateChanged = onDictationStateChanged
    )
    
    override val isDictationActive: StateFlow<Boolean>
        get() = dictationHandler.isDictationActive
    
    init {
        dictationHandler.initialize()
    }
    
    override fun startDictation(): Boolean {
        return dictationHandler.handleDictationStart()
    }
    
    override fun stopDictation(): Boolean {
        return dictationHandler.handleDictationEnd()
    }
    
    override fun processVoiceCommand(command: String): Boolean {
        return dictationHandler.processVoiceCommand(command)
    }
    
    override fun updateKeyboardVisibility(isVisible: Boolean) {
        dictationHandler.updateKeyboardVisibility(isVisible)
    }
    
    override fun destroy() {
        dictationHandler.destroy()
    }
}