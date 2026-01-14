/**
 * VoiceInputListener.kt - Interface for voice input operations
 * Direct implementation - no unnecessary abstractions
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.interfaces

/**
 * Interface for handling voice input operations
 */
interface VoiceInputListener {
    fun startListening(onResult: (String) -> Unit)
    fun startContinuousListening(onResult: (String) -> Unit)
    fun stopListening()
    fun isListening(): Boolean
    fun setLanguage(languageCode: String)
    fun release()
}