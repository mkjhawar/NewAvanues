/**
 * AndroidListener.kt - RecognitionListener implementation for AndroidSTTEngine
 * 
 * Extracted from AndroidSTTEngine as part of SOLID refactoring
 * Handles all Android RecognitionListener callbacks:
 * - Speech recognition event handling
 * - Audio level monitoring
 * - Partial and final results processing
 * - Error handling delegation
 * - State management integration
 * 
 * © Augmentalis Inc, Intelligent Devices LLC, Manoj Jhawar, Aman Jhawar
 */
package com.augmentalis.voiceos.speech.engines.android

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log
import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.augmentalis.voiceos.speech.engines.common.ServiceState
import com.augmentalis.voiceos.speech.engines.common.PerformanceMonitor

/**
 * Implementation of Android's RecognitionListener for AndroidSTTEngine.
 * Manages all speech recognition callbacks and delegates to appropriate handlers.
 */
class AndroidListener(
    private val serviceState: ServiceState,
    private val performanceMonitor: PerformanceMonitor
) : RecognitionListener {
    
    companion object {
        private const val TAG = "AndroidListener"
        private const val SILENCE_THRESHOLD = -2.0f
    }
    
    // Callback interfaces
    private var onReadyCallback: (() -> Unit)? = null
    private var onBeginSpeechCallback: (() -> Unit)? = null
    private var onEndSpeechCallback: (() -> Unit)? = null
    private var onResultsCallback: ((List<String>) -> Unit)? = null
    private var onPartialResultsCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((Int, String) -> Unit)? = null
    private var onRmsChangedCallback: ((Float) -> Unit)? = null
    private var onBufferReceivedCallback: ((ByteArray?) -> Unit)? = null
    private var onEventCallback: ((Int, Bundle?) -> Unit)? = null
    
    // Internal state
    private var isListening = false
    private var sessionStartTime = 0L
    private var lastAudioLevel = 0f
    private var silenceDetected = false
    
    // Audio monitoring for dictation silence detection
    private var silenceStartTime = 0L
    
    /**
     * Set callback for when ready for speech
     */
    fun setOnReadyCallback(callback: () -> Unit) {
        onReadyCallback = callback
    }
    
    /**
     * Set callback for beginning of speech
     */
    fun setOnBeginSpeechCallback(callback: () -> Unit) {
        onBeginSpeechCallback = callback
    }
    
    /**
     * Set callback for end of speech
     */
    fun setOnEndSpeechCallback(callback: () -> Unit) {
        onEndSpeechCallback = callback
    }
    
    /**
     * Set callback for final results
     */
    fun setOnResultsCallback(callback: (List<String>) -> Unit) {
        onResultsCallback = callback
    }
    
    /**
     * Set callback for partial results
     */
    fun setOnPartialResultsCallback(callback: (String) -> Unit) {
        onPartialResultsCallback = callback
    }
    
    /**
     * Set callback for errors
     */
    fun setOnErrorCallback(callback: (Int, String) -> Unit) {
        onErrorCallback = callback
    }
    
    /**
     * Set callback for RMS (volume) changes
     */
    fun setOnRmsChangedCallback(callback: (Float) -> Unit) {
        onRmsChangedCallback = callback
    }
    
    /**
     * Set callback for audio buffer received
     */
    fun setOnBufferReceivedCallback(callback: (ByteArray?) -> Unit) {
        onBufferReceivedCallback = callback
    }
    
    /**
     * Set callback for recognition events
     */
    fun setOnEventCallback(callback: (Int, Bundle?) -> Unit) {
        onEventCallback = callback
    }
    
    // RecognitionListener implementation
    
    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "onReadyForSpeech")
        isListening = true
        sessionStartTime = System.currentTimeMillis()
        serviceState.updateState(ServiceState.State.LISTENING)
        performanceMonitor.startSession()
        
        onReadyCallback?.invoke()
    }
    
    override fun onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech")
        serviceState.updateState(ServiceState.State.PROCESSING)
        silenceDetected = false
        silenceStartTime = 0L
        
        onBeginSpeechCallback?.invoke()
    }
    
    override fun onRmsChanged(rmsdB: Float) {
        lastAudioLevel = rmsdB
        
        // Silence detection for dictation mode
        if (rmsdB < SILENCE_THRESHOLD) {
            if (silenceStartTime == 0L) {
                silenceStartTime = System.currentTimeMillis()
            }
            silenceDetected = true
        } else {
            silenceStartTime = 0L
            silenceDetected = false
        }
        
        Log.v(TAG, "Audio level: $rmsdB dB (silence: $silenceDetected)")
        onRmsChangedCallback?.invoke(rmsdB)
    }
    
    override fun onBufferReceived(buffer: ByteArray?) {
        Log.v(TAG, "onBufferReceived: ${buffer?.size ?: 0} bytes")
        onBufferReceivedCallback?.invoke(buffer)
    }
    
    override fun onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech")
        onEndSpeechCallback?.invoke()
    }
    
    override fun onError(error: Int) {
        Log.e(TAG, "Recognition error: $error (${getErrorString(error)})")
        
        isListening = false
        performanceMonitor.recordRecognition(sessionStartTime, false)
        
        val errorMessage = getErrorString(error)
        
        // Update state based on error type
        when (error) {
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_SERVER,
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                serviceState.updateState(ServiceState.State.ERROR, errorMessage)
            }
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                // These are recoverable - don't change state to ERROR
                Log.d(TAG, "Recoverable error: $errorMessage")
            }
            else -> {
                if (error != SpeechRecognizer.ERROR_CLIENT) {
                    serviceState.updateState(ServiceState.State.ERROR, errorMessage)
                }
            }
        }
        
        onErrorCallback?.invoke(error, errorMessage)
    }
    
    override fun onResults(results: Bundle?) {
        Log.d(TAG, "onResults received")
        isListening = false
        
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
        
        if (!matches.isNullOrEmpty()) {
            Log.d(TAG, "Recognition results: ${matches.size} matches")
            
            // Record successful recognition
            performanceMonitor.recordRecognition(sessionStartTime, true, matches[0])
            
            // Log results with confidence if available
            matches.forEachIndexed { index, match ->
                val confidence = confidences?.getOrNull(index) ?: 1.0f
                Log.d(TAG, "Result $index: '$match' (confidence: $confidence)")
            }
            
            onResultsCallback?.invoke(matches)
        } else {
            Log.w(TAG, "No recognition results")
            performanceMonitor.recordRecognition(sessionStartTime, false)
        }
        
        serviceState.updateState(ServiceState.State.READY)
    }
    
    override fun onPartialResults(partialResults: Bundle?) {
        val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partialText = partialMatches?.firstOrNull()
        
        if (!partialText.isNullOrBlank()) {
            Log.v(TAG, "Partial result: '$partialText'")
            onPartialResultsCallback?.invoke(partialText)
        }
    }
    
    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(TAG, "onEvent: type=$eventType, params=${params?.keySet()?.joinToString()}")
        onEventCallback?.invoke(eventType, params)
    }
    
    /**
     * Convert error code to human-readable string
     */
    private fun getErrorString(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests"
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported"
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language unavailable"
            else -> "Unknown error ($errorCode)"
        }
    }
    
    /**
     * Check if currently listening
     */
    fun isListening(): Boolean = isListening
    
    /**
     * Get last audio level
     */
    fun getLastAudioLevel(): Float = lastAudioLevel
    
    /**
     * Check if silence is currently detected
     */
    fun isSilenceDetected(): Boolean = silenceDetected
    
    /**
     * Get silence duration in milliseconds
     */
    fun getSilenceDuration(): Long {
        return if (silenceStartTime > 0) {
            System.currentTimeMillis() - silenceStartTime
        } else {
            0L
        }
    }
    
    /**
     * Get session statistics
     */
    fun getSessionStats(): SessionStats {
        val sessionDuration = if (sessionStartTime > 0) {
            System.currentTimeMillis() - sessionStartTime
        } else {
            0L
        }
        
        return SessionStats(
            isListening = isListening,
            sessionDuration = sessionDuration,
            lastAudioLevel = lastAudioLevel,
            silenceDetected = silenceDetected,
            silenceDuration = getSilenceDuration()
        )
    }
    
    /**
     * Reset internal state
     */
    fun reset() {
        isListening = false
        sessionStartTime = 0L
        lastAudioLevel = 0f
        silenceDetected = false
        silenceStartTime = 0L
        
        Log.d(TAG, "AndroidListener reset")
    }
    
    /**
     * Data class for session statistics
     */
    data class SessionStats(
        val isListening: Boolean,
        val sessionDuration: Long,
        val lastAudioLevel: Float,
        val silenceDetected: Boolean,
        val silenceDuration: Long
    )
}