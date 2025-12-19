// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/controller/VoiceRecognizer.kt
// created: 2025-11-01 22:30:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 1 - Core Infrastructure
// agent: Engineer | mode: ACT

package com.augmentalis.overlay.controller

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Wrapper for Android SpeechRecognizer with partial results support.
 *
 * Provides voice input transcription with real-time partial results
 * for responsive UI updates during speech recognition.
 *
 * @param context Android context
 * @param onPartialResult Callback for partial transcription updates
 * @param onFinalResult Callback for final transcription result
 * @param onError Callback for recognition errors
 * @author Manoj Jhawar
 */
class VoiceRecognizer(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var isListening = false

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {
                // User started speaking
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Could be used for waveform visualization
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing microphone permission"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                onError(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onFinalResult(matches[0])
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onPartialResult(matches[0])
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Reserved for future events
            }
        })
    }

    /**
     * Start voice recognition with partial results
     */
    fun startListening() {
        if (isListening) {
            stopListening()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            onError("Failed to start voice recognition: ${e.message}")
        }
    }

    /**
     * Stop voice recognition
     */
    fun stopListening() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        }
    }

    /**
     * Cancel voice recognition
     */
    fun cancel() {
        if (isListening) {
            speechRecognizer.cancel()
            isListening = false
        }
    }

    /**
     * Release resources
     */
    fun release() {
        cancel()
        speechRecognizer.destroy()
    }

    /**
     * Check if currently listening
     */
    fun isActive(): Boolean = isListening
}
