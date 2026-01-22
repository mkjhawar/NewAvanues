/**
 * AndroidSTTListener.kt - RecognitionListener implementation for Android STT
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 * Updated: 2026-01-18 - Migrated to KMP androidMain
 */
package com.augmentalis.speechrecognition

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Implementation of Android's RecognitionListener.
 * Handles all speech recognition callbacks and delegates to appropriate handlers.
 */
class AndroidSTTListener(
    private val performanceMonitor: PerformanceMonitor
) : RecognitionListener {

    companion object {
        private const val TAG = "AndroidSTTListener"
        private const val SILENCE_THRESHOLD = -2.0f
    }

    // Callbacks
    private var onReadyCallback: (() -> Unit)? = null
    private var onBeginSpeechCallback: (() -> Unit)? = null
    private var onEndSpeechCallback: (() -> Unit)? = null
    private var onResultsCallback: ((List<String>) -> Unit)? = null
    private var onPartialResultsCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((Int, String) -> Unit)? = null
    private var onRmsChangedCallback: ((Float) -> Unit)? = null

    // State
    private var isListening = false
    private var sessionStartTime = 0L
    private var lastAudioLevel = 0f
    private var silenceDetected = false
    private var silenceStartTime = 0L

    // Callback setters
    fun setOnReadyCallback(callback: () -> Unit) { onReadyCallback = callback }
    fun setOnBeginSpeechCallback(callback: () -> Unit) { onBeginSpeechCallback = callback }
    fun setOnEndSpeechCallback(callback: () -> Unit) { onEndSpeechCallback = callback }
    fun setOnResultsCallback(callback: (List<String>) -> Unit) { onResultsCallback = callback }
    fun setOnPartialResultsCallback(callback: (String) -> Unit) { onPartialResultsCallback = callback }
    fun setOnErrorCallback(callback: (Int, String) -> Unit) { onErrorCallback = callback }
    fun setOnRmsChangedCallback(callback: (Float) -> Unit) { onRmsChangedCallback = callback }

    // RecognitionListener implementation

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "onReadyForSpeech")
        isListening = true
        sessionStartTime = System.currentTimeMillis()
        performanceMonitor.startSession()
        onReadyCallback?.invoke()
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech")
        silenceDetected = false
        silenceStartTime = 0L
        onBeginSpeechCallback?.invoke()
    }

    override fun onRmsChanged(rmsdB: Float) {
        lastAudioLevel = rmsdB

        if (rmsdB < SILENCE_THRESHOLD) {
            if (silenceStartTime == 0L) {
                silenceStartTime = System.currentTimeMillis()
            }
            silenceDetected = true
        } else {
            silenceStartTime = 0L
            silenceDetected = false
        }

        onRmsChangedCallback?.invoke(rmsdB)
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        Log.v(TAG, "onBufferReceived: ${buffer?.size ?: 0} bytes")
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech")
        onEndSpeechCallback?.invoke()
    }

    override fun onError(error: Int) {
        Log.e(TAG, "Recognition error: $error (${getErrorString(error)})")
        isListening = false
        performanceMonitor.recordRecognition(sessionStartTime, false)
        onErrorCallback?.invoke(error, getErrorString(error))
    }

    override fun onResults(results: Bundle?) {
        Log.d(TAG, "onResults")
        isListening = false

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

        if (!matches.isNullOrEmpty()) {
            Log.d(TAG, "Results: ${matches.size} matches")
            performanceMonitor.recordRecognition(sessionStartTime, true, matches[0])

            matches.forEachIndexed { index, match ->
                val confidence = confidences?.getOrNull(index) ?: 1.0f
                Log.d(TAG, "Result $index: '$match' (confidence: $confidence)")
            }

            onResultsCallback?.invoke(matches)
        } else {
            Log.w(TAG, "No results")
            performanceMonitor.recordRecognition(sessionStartTime, false)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partialText = partialMatches?.firstOrNull()

        if (!partialText.isNullOrBlank()) {
            Log.v(TAG, "Partial: '$partialText'")
            onPartialResultsCallback?.invoke(partialText)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(TAG, "onEvent: type=$eventType")
    }

    /**
     * Convert error code to message
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

    // State getters
    fun isListening(): Boolean = isListening
    fun getLastAudioLevel(): Float = lastAudioLevel
    fun isSilenceDetected(): Boolean = silenceDetected

    fun getSilenceDuration(): Long {
        return if (silenceStartTime > 0) {
            System.currentTimeMillis() - silenceStartTime
        } else 0L
    }

    /**
     * Reset state
     */
    fun reset() {
        isListening = false
        sessionStartTime = 0L
        lastAudioLevel = 0f
        silenceDetected = false
        silenceStartTime = 0L
        Log.d(TAG, "Listener reset")
    }
}
