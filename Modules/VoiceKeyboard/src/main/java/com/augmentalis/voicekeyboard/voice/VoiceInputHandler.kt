/**
 * VoiceInputHandler.kt - Handles voice input for the keyboard
 * 
 * Author: Manoj Jhawar
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.augmentalis.voicekeyboard.utils.KeyboardConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Handles voice input functionality for the keyboard
 * Integrates with Android's SpeechRecognizer and VOS4's voice services
 * 
 * SOLID Principles:
 * - Single Responsibility: Only handles voice recognition
 * - Open/Closed: Extensible through callbacks
 * - Dependency Inversion: Depends on recognition interface
 */
class VoiceInputHandler(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceInputHandler"
        private const val PARTIAL_RESULTS_DELAY = 500L
        private const val ERROR_RETRY_DELAY = 1000L
        private const val MAX_RETRY_COUNT = 3
    }
    
    // Recognition state
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText
    
    // Speech recognizer
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionIntent: Intent? = null
    
    // Callbacks
    private var onResultCallback: ((String) -> Unit)? = null
    private var onPartialResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((Int, String) -> Unit)? = null
    
    // State management
    private var isContinuousMode = false
    private var retryCount = 0
    private var currentLanguage = Locale.getDefault()
    
    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var partialResultJob: Job? = null
    
    init {
        initializeSpeechRecognizer()
    }
    
    /**
     * Initialize speech recognizer
     */
    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available on this device")
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                _isListening.value = true
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended")
                if (!isContinuousMode) {
                    _isListening.value = false
                }
            }
            
            override fun onError(error: Int) {
                handleRecognitionError(error)
            }
            
            override fun onResults(results: Bundle?) {
                handleRecognitionResults(results)
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                handlePartialResults(partialResults)
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "Recognition event: $eventType")
            }
        })
        
        // Setup recognition intent
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage.toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
    }
    
    /**
     * Start listening for voice input
     */
    fun startListening(onResult: (String) -> Unit) {
        if (_isListening.value) {
            Log.w(TAG, "Already listening")
            return
        }
        
        Log.d(TAG, "Starting voice input")
        
        onResultCallback = onResult
        isContinuousMode = false
        retryCount = 0
        
        startRecognition()
    }
    
    /**
     * Start continuous listening mode
     */
    fun startContinuousListening(onResult: (String) -> Unit) {
        if (_isListening.value) {
            Log.w(TAG, "Already listening")
            return
        }
        
        Log.d(TAG, "Starting continuous voice input")
        
        onResultCallback = onResult
        isContinuousMode = true
        retryCount = 0
        
        startRecognition()
    }
    
    /**
     * Stop listening
     */
    fun stopListening() {
        Log.d(TAG, "Stopping voice input")
        
        _isListening.value = false
        isContinuousMode = false
        
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        
        partialResultJob?.cancel()
    }
    
    /**
     * Set language for recognition
     */
    fun setLanguage(locale: Locale) {
        currentLanguage = locale
        
        recognitionIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString())
        
        // Restart recognition if active
        if (_isListening.value) {
            stopListening()
            startRecognition()
        }
    }
    
    /**
     * Set partial results callback
     */
    fun setPartialResultCallback(callback: (String) -> Unit) {
        onPartialResultCallback = callback
    }
    
    /**
     * Set error callback
     */
    fun setErrorCallback(callback: (Int, String) -> Unit) {
        onErrorCallback = callback
    }
    
    /**
     * Start speech recognition
     */
    private fun startRecognition() {
        try {
            speechRecognizer?.startListening(recognitionIntent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recognition", e)
            _isListening.value = false
            onErrorCallback?.invoke(-1, "Failed to start recognition: ${e.message}")
        }
    }
    
    /**
     * Handle recognition results
     */
    private fun handleRecognitionResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            Log.d(TAG, "Recognized: $text")
            
            _recognizedText.value = text
            onResultCallback?.invoke(text)
            
            // Continue listening if in continuous mode
            if (isContinuousMode) {
                scope.launch {
                    delay(100)
                    startRecognition()
                }
            } else {
                _isListening.value = false
            }
        }
    }
    
    /**
     * Handle partial recognition results
     */
    private fun handlePartialResults(partialResults: Bundle?) {
        val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        
        if (!partialMatches.isNullOrEmpty()) {
            val partialText = partialMatches[0]
            Log.v(TAG, "Partial: $partialText")
            
            // Debounce partial results
            partialResultJob?.cancel()
            partialResultJob = scope.launch {
                delay(PARTIAL_RESULTS_DELAY)
                onPartialResultCallback?.invoke(partialText)
            }
        }
    }
    
    /**
     * Handle recognition errors
     */
    private fun handleRecognitionError(errorCode: Int) {
        val errorMessage = when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Unknown error: $errorCode"
        }
        
        Log.e(TAG, "Recognition error: $errorMessage")
        
        // Handle specific errors
        when (errorCode) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                // No speech detected, just stop
                if (isContinuousMode) {
                    // Restart in continuous mode
                    scope.launch {
                        delay(ERROR_RETRY_DELAY)
                        startRecognition()
                    }
                } else {
                    _isListening.value = false
                }
            }
            
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER -> {
                // Network issues, retry
                if (retryCount < MAX_RETRY_COUNT) {
                    retryCount++
                    Log.d(TAG, "Retrying recognition (attempt $retryCount)")
                    
                    scope.launch {
                        delay(ERROR_RETRY_DELAY)
                        startRecognition()
                    }
                } else {
                    _isListening.value = false
                    onErrorCallback?.invoke(errorCode, errorMessage)
                }
            }
            
            else -> {
                _isListening.value = false
                onErrorCallback?.invoke(errorCode, errorMessage)
            }
        }
    }
    
    /**
     * Check if voice input is available
     */
    fun isVoiceInputAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * Get supported languages
     */
    fun getSupportedLanguages(): List<Locale> {
        // TODO: Query actual supported languages
        return listOf(
            Locale.US,
            Locale.UK,
            Locale("es", "ES"),
            Locale.FRANCE,
            Locale.GERMANY,
            Locale.ITALY,
            Locale("pt", "BR"),
            Locale.JAPAN,
            Locale.KOREA,
            Locale.CHINA
        )
    }
    
    /**
     * Release resources
     */
    fun release() {
        Log.d(TAG, "Releasing voice input handler")
        
        stopListening()
        
        speechRecognizer?.destroy()
        speechRecognizer = null
        
        scope.cancel()
    }
}