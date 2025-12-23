/**
 * GoogleEngineAdapter.kt - Adapter for Google Speech Recognition (Android STT)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-22
 *
 * Part of SOLID Refactoring Phase 2: Open/Closed Principle (Factory Pattern)
 * Plan: VoiceOS-Plan-SOLID-Refactoring-5221222-V1.md
 *
 * PURPOSE:
 * Adapter that wraps Android's built-in SpeechRecognizer to conform to ISpeechEngine interface.
 * Provides Google Cloud Speech recognition through Android's native APIs.
 *
 * DESIGN PATTERN: Adapter Pattern
 * - Adapts Android SpeechRecognizer to match ISpeechEngine
 * - Single Responsibility: Only handles Google STT adaptation
 * - No business logic - pure delegation
 *
 * NOTE: This is a simplified implementation. Full Android SpeechRecognizer
 * integration would require RecognitionListener callbacks and Intent handling.
 */
package com.augmentalis.voiceoscore.accessibility.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.voiceos.speech.api.RecognitionResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Adapter for Google speech recognition (Android SpeechRecognizer)
 *
 * Wraps Android's built-in SpeechRecognizer to provide a unified ISpeechEngine interface.
 * Uses Google's cloud-based speech recognition services.
 *
 * REQUIREMENTS:
 * - Requires network connectivity (online-only)
 * - Requires RECORD_AUDIO permission
 * - Requires Google Play Services (most Android devices)
 *
 * LIMITATIONS:
 * - No offline support
 * - Limited customization compared to Vivoka
 * - No dynamic command vocabulary (free-form recognition)
 *
 * THREAD SAFETY:
 * - Thread-safe: All operations delegated to Android SpeechRecognizer
 * - Callbacks invoked on main thread
 *
 * @property context Android application context
 *
 * @see ISpeechEngine
 * @see SpeechRecognizer
 */
class GoogleEngineAdapter(
    private val context: Context
) : ISpeechEngine {

    companion object {
        private const val TAG = "GoogleEngineAdapter"
    }

    /**
     * Android SpeechRecognizer instance
     */
    private var speechRecognizer: SpeechRecognizer? = null

    /**
     * Speech recognition intent
     */
    private var recognitionIntent: Intent? = null

    /**
     * Track initialization state
     */
    private var isInitialized: Boolean = false

    /**
     * Track listening state
     */
    private var isListening: Boolean = false

    /**
     * Result callback
     */
    private var resultCallback: ((RecognitionResult) -> Unit)? = null

    /**
     * Error callback
     */
    private var errorCallback: ((String, Int) -> Unit)? = null

    /**
     * Current language code
     */
    private var languageCode: String = "en-US"

    /**
     * Initialize Google speech recognition
     *
     * Creates SpeechRecognizer instance and configures recognition intent.
     *
     * @param config Speech configuration
     * @return true if initialization succeeded
     */
    override suspend fun initialize(config: SpeechConfig): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            Log.d(TAG, "Initializing Google speech recognition")

            // Check if speech recognition is available
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.e(TAG, "Speech recognition not available on this device")
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            // Store language
            languageCode = config.language

            // Create SpeechRecognizer
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

            // Set recognition listener
            speechRecognizer?.setRecognitionListener(createRecognitionListener())

            // Create recognition intent
            recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, config.timeoutDuration)
            }

            isInitialized = true
            Log.i(TAG, "Google speech recognition initialized successfully")
            continuation.resume(true)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Google speech recognition", e)
            isInitialized = false
            continuation.resume(false)
        }
    }

    /**
     * Start listening for speech
     *
     * @throws IllegalStateException if engine not initialized
     */
    override fun startListening() {
        checkInitialized()

        try {
            Log.d(TAG, "Starting Google speech recognition")
            speechRecognizer?.startListening(recognitionIntent)
            isListening = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Google speech recognition", e)
            isListening = false
            throw e
        }
    }

    /**
     * Stop listening for speech
     */
    override fun stopListening() {
        try {
            Log.d(TAG, "Stopping Google speech recognition")
            speechRecognizer?.stopListening()
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Google speech recognition", e)
            isListening = false
        }
    }

    /**
     * Update dynamic commands
     *
     * NOTE: Google SpeechRecognizer does not support dynamic command vocabulary.
     * It performs free-form recognition and returns the most likely transcription.
     * Command matching must be done at a higher level (e.g., in SpeechEngineManager).
     *
     * @param commands List of commands (ignored - not supported by Google STT)
     */
    override suspend fun updateCommands(commands: List<String>) {
        Log.w(TAG, "Google SpeechRecognizer does not support dynamic commands (free-form recognition)")
        // No-op: Google doesn't support command vocabularies
    }

    /**
     * Update engine configuration
     *
     * For Google SpeechRecognizer, we can update the recognition intent
     * with new language settings.
     *
     * @param config New configuration data
     * @throws IllegalStateException if engine not initialized
     */
    override fun updateConfiguration(config: SpeechConfigurationData) {
        checkInitialized()

        Log.d(TAG, "Updating Google speech recognition configuration")

        // Update language in recognition intent
        languageCode = config.language
        recognitionIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
        recognitionIntent?.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, config.timeoutDuration)
    }

    /**
     * Check if engine is currently recognizing
     *
     * @return true if actively listening
     */
    override fun isRecognizing(): Boolean {
        return isInitialized && isListening
    }

    /**
     * Get the underlying SpeechRecognizer instance
     *
     * @return SpeechRecognizer instance
     */
    override fun getEngine(): Any? {
        return speechRecognizer
    }

    /**
     * Set result listener for speech recognition results
     *
     * @param listener Callback for recognition results
     */
    fun setResultListener(listener: (RecognitionResult) -> Unit) {
        resultCallback = listener
    }

    /**
     * Set error listener for speech recognition errors
     *
     * @param listener Callback for errors
     */
    fun setErrorListener(listener: (String, Int) -> Unit) {
        errorCallback = listener
    }

    /**
     * Clean up Google speech recognition resources
     */
    override fun destroy() {
        try {
            Log.d(TAG, "Destroying Google speech recognition")
            stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            recognitionIntent = null
            isInitialized = false
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying Google speech recognition", e)
        }
    }

    /**
     * Create RecognitionListener for Android SpeechRecognizer callbacks
     */
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech detected")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed - can be used for volume visualization
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received - not typically used
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech detected")
                isListening = false
            }

            override fun onError(error: Int) {
                val errorMessage = getErrorMessage(error)
                Log.e(TAG, "Recognition error: $errorMessage (code: $error)")
                isListening = false
                errorCallback?.invoke(errorMessage, error)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                if (matches != null && matches.isNotEmpty()) {
                    val bestMatch = matches[0]
                    val bestConfidence = confidence?.getOrNull(0) ?: 0f

                    Log.d(TAG, "Recognition result: $bestMatch (confidence: $bestConfidence)")

                    val result = RecognitionResult(
                        text = bestMatch,
                        confidence = bestConfidence
                    )

                    resultCallback?.invoke(result)
                }

                isListening = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    Log.d(TAG, "Partial result: ${matches[0]}")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "Recognition event: $eventType")
            }
        }
    }

    /**
     * Convert error code to human-readable message
     */
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error ($errorCode)"
        }
    }

    /**
     * Check if engine is initialized, throw if not
     *
     * @throws IllegalStateException if not initialized
     */
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("Google speech recognition not initialized")
        }
    }
}
