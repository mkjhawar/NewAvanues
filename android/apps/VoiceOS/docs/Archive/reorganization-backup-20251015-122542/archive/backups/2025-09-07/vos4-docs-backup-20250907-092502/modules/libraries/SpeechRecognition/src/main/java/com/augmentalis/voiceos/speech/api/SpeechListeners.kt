/**
 * SpeechListeners.kt - Speech recognition listener types (functional)
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 * 
 * Using functional types instead of interfaces per VOS4 standards
 */
package com.augmentalis.voiceos.speech.api

/**
 * Functional type for speech result callbacks
 * Replaces interface to comply with VOS4 zero-overhead architecture
 */
typealias OnSpeechResultListener = (result: RecognitionResult) -> Unit

/**
 * Functional type for error callbacks
 */
typealias OnSpeechErrorListener = (error: String, code: Int) -> Unit

/**
 * Functional type for state change callbacks
 */
typealias OnStateChangeListener = (state: String, message: String?) -> Unit

/**
 * Combined listener holder for speech services
 * Direct implementation - no interface
 */
class SpeechListenerManager {
    var onResult: OnSpeechResultListener? = null
    var onError: OnSpeechErrorListener? = null
    var onStateChange: OnStateChangeListener? = null
    
    fun notifyResult(result: RecognitionResult) {
        onResult?.invoke(result)
    }
    
    fun notifyError(error: String, code: Int = -1) {
        onError?.invoke(error, code)
    }
    
    fun notifyStateChange(state: String, message: String? = null) {
        onStateChange?.invoke(state, message)
    }
    
    fun clear() {
        onResult = null
        onError = null
        onStateChange = null
    }
}