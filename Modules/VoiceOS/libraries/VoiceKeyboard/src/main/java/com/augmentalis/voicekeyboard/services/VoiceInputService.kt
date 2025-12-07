/**
 * VoiceInputService.kt - Voice input service implementation
 * Direct implementation - no unnecessary abstractions
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.services

import android.content.Context
import com.augmentalis.voicekeyboard.interfaces.VoiceInputListener
import com.augmentalis.voicekeyboard.voice.VoiceInputHandler

/**
 * Service implementation for voice input operations
 * Wraps VoiceInputHandler to implement the interface
 */
class VoiceInputService(private val context: Context) : VoiceInputListener {
    
    private val voiceHandler = VoiceInputHandler(context)
    
    override fun startListening(onResult: (String) -> Unit) {
        voiceHandler.startListening(onResult)
    }
    
    override fun startContinuousListening(onResult: (String) -> Unit) {
        voiceHandler.startContinuousListening(onResult)
    }
    
    override fun stopListening() {
        voiceHandler.stopListening()
    }
    
    override fun isListening(): Boolean {
        return voiceHandler.isListening.value
    }
    
    override fun setLanguage(languageCode: String) {
        // Convert language code to Locale and set
        val parts = languageCode.split("_")
        val locale = if (parts.size > 1) {
            java.util.Locale(parts[0], parts[1])
        } else {
            java.util.Locale(parts[0])
        }
        voiceHandler.setLanguage(locale)
    }
    
    override fun release() {
        voiceHandler.release()
    }
}