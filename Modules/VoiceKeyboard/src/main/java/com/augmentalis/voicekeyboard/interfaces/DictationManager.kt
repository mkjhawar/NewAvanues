/**
 * DictationManager.kt - Interface for dictation management
 * Direct implementation - no unnecessary abstractions
 * 
 * Author: Manoj Jhawar
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.interfaces

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for managing dictation functionality
 */
interface DictationManager {
    val isDictationActive: StateFlow<Boolean>
    
    fun startDictation(): Boolean
    fun stopDictation(): Boolean
    fun processVoiceCommand(command: String): Boolean
    fun updateKeyboardVisibility(isVisible: Boolean)
    fun destroy()
}