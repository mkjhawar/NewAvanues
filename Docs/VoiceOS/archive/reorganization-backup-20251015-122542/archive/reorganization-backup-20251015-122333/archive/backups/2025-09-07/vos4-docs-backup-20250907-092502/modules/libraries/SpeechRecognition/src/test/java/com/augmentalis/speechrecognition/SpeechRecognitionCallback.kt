/**
 * SpeechRecognitionCallback.kt - Test double for speech recognition callbacks
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Test interface for speech recognition callbacks, used in unit tests
 * to avoid dependencies on production callback implementations.
 */
package com.augmentalis.speechrecognition

import com.augmentalis.voiceos.speech.api.RecognitionResult

/**
 * Test callback interface for speech recognition events
 * This is a test double that provides a simple interface for testing
 */
interface SpeechRecognitionCallback {
    /**
     * Called when recognition produces a final result
     */
    fun onResult(result: RecognitionResult)
    
    /**
     * Called when recognition produces a partial result
     */
    fun onPartialResult(partialText: String)
    
    /**
     * Called when recognition encounters an error
     */
    fun onError(error: String)
}