/**
 * SpeechRecorder.kt - Voice input recorder for manual command assignment
 *
 * Part of Metadata Quality Overlay & Manual Command Assignment feature (VOS-META-001)
 * Created: 2025-12-03
 *
 * Wraps Android Speech Recognition API for capturing voice commands from users.
 * Provides callbacks for recognition results, errors, and partial results.
 */
package com.augmentalis.voiceoscore.commands

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Speech Recorder for Voice Command Assignment
 *
 * Manages speech recognition session for capturing user-spoken voice commands.
 * Provides real-time feedback on recognition status and results.
 *
 * ## Usage:
 * ```kotlin
 * val recorder = SpeechRecorder(context)
 *
 * // Observe state
 * lifecycleScope.launch {
 *     recorder.state.collect { state ->
 *         when (state) {
 *             is RecordingState.Ready -> showRecordButton()
 *             is RecordingState.Listening -> showListeningIndicator()
 *             is RecordingState.Processing -> showProcessingIndicator()
 *             is RecordingState.Result -> handleResult(state.text, state.confidence)
 *             is RecordingState.Error -> showError(state.message)
 *         }
 *     }
 * }
 *
 * // Start recording
 * recorder.startListening()
 *
 * // Stop recording
 * recorder.stopListening()
 *
 * // Clean up
 * recorder.destroy()
 * ```
 */
class SpeechRecorder(
    private val context: Context
) {
    companion object {
        private const val TAG = "SpeechRecorder"
    }

    private var recognizer: SpeechRecognizer? = null
    private var isListening = false

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Ready)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
            _state.value = RecordingState.Listening()
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech")
            _state.value = RecordingState.Listening()
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Update volume level for visual feedback
            val currentState = _state.value
            if (currentState is RecordingState.Listening) {
                _state.value = currentState.copy(volumeLevel = rmsdB)
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Not used
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "End of speech")
            _state.value = RecordingState.Processing
            isListening = false
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error: $error"
            }

            Log.e(TAG, "Recognition error: $errorMessage")
            _state.value = RecordingState.Error(errorMessage)
            isListening = false
        }

        override fun onResults(results: Bundle?) {
            results?.let {
                val matches = it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val scores = it.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                if (matches != null && matches.isNotEmpty()) {
                    val bestMatch = matches[0]
                    val confidence = scores?.getOrNull(0)?.toDouble() ?: 1.0

                    Log.i(TAG, "Recognition result: '$bestMatch' (confidence: $confidence)")
                    _state.value = RecordingState.Result(bestMatch, confidence)
                } else {
                    Log.w(TAG, "No recognition matches")
                    _state.value = RecordingState.Error("No speech recognized")
                }
            }
            isListening = false
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.let {
                val matches = it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val partial = matches[0]
                    Log.d(TAG, "Partial result: $partial")
                    val currentState = _state.value
                    if (currentState is RecordingState.Listening) {
                        _state.value = currentState.copy(partialText = partial)
                    } else {
                        _state.value = RecordingState.Listening(partialText = partial)
                    }
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Not used
        }
    }

    /**
     * Start listening for speech input.
     *
     * Requires RECORD_AUDIO permission.
     */
    fun startListening() {
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available")
            _state.value = RecordingState.Error("Speech recognition not available")
            return
        }

        try {
            // Create recognizer if needed
            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                recognizer?.setRecognitionListener(recognitionListener)
            }

            // Create recognition intent
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }

            // Start recognition
            recognizer?.startListening(intent)
            isListening = true
            _state.value = RecordingState.Listening()

            Log.i(TAG, "Started listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            _state.value = RecordingState.Error("Failed to start recording: ${e.message}")
        }
    }

    /**
     * Stop listening for speech input.
     */
    fun stopListening() {
        if (!isListening) {
            return
        }

        try {
            recognizer?.stopListening()
            isListening = false
            Log.i(TAG, "Stopped listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        }
    }

    /**
     * Cancel current recognition session.
     */
    fun cancel() {
        if (!isListening) {
            return
        }

        try {
            recognizer?.cancel()
            isListening = false
            _state.value = RecordingState.Ready
            Log.i(TAG, "Canceled recognition")
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling speech recognition", e)
        }
    }

    /**
     * Reset to ready state.
     */
    fun reset() {
        cancel()
        _state.value = RecordingState.Ready
    }

    /**
     * Destroy recognizer and release resources.
     *
     * Call this when done using the recorder (e.g., in onDestroy).
     */
    fun destroy() {
        try {
            recognizer?.destroy()
            recognizer = null
            isListening = false
            _state.value = RecordingState.Ready
            Log.i(TAG, "Destroyed recognizer")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognition", e)
        }
    }
}

/**
 * Recording state sealed class
 */
sealed class RecordingState {
    /**
     * Ready to start recording
     */
    data object Ready : RecordingState()

    /**
     * Currently listening for speech
     *
     * @property volumeLevel Audio level (0.0-10.0)
     * @property partialText Partial recognition result
     */
    data class Listening(
        val volumeLevel: Float = 0f,
        val partialText: String? = null
    ) : RecordingState()

    /**
     * Processing speech (after user stops speaking)
     */
    data object Processing : RecordingState()

    /**
     * Recognition result received
     *
     * @property text Recognized text
     * @property confidence Confidence score (0.0-1.0)
     */
    data class Result(
        val text: String,
        val confidence: Double
    ) : RecordingState()

    /**
     * Error occurred
     *
     * @property message Error message
     */
    data class Error(
        val message: String
    ) : RecordingState()
}
