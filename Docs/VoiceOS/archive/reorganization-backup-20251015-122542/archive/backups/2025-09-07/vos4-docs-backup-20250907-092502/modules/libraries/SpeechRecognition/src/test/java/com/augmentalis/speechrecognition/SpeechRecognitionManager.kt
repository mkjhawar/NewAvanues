/**
 * SpeechRecognitionManager.kt - Test double for speech recognition manager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Test implementation of a speech recognition manager that provides
 * a simplified interface for testing speech recognition functionality
 * without requiring actual speech engines or Android dependencies.
 */
package com.augmentalis.speechrecognition

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.augmentalis.voiceos.speech.api.RecognitionResult
import kotlinx.coroutines.*

/**
 * Test implementation of speech recognition manager
 * This is a test double that simulates speech recognition operations
 */
class SpeechRecognitionManager(private val context: Context) {
    
    // LiveData for observing status changes
    private val _recognitionStatus = MutableLiveData<RecognitionStatus>(RecognitionStatus.IDLE)
    val recognitionStatus: LiveData<RecognitionStatus> = _recognitionStatus
    
    // LiveData for observing recognition results
    private val _recognitionResult = MutableLiveData<RecognitionResult?>()
    val recognitionResult: LiveData<RecognitionResult?> = _recognitionResult
    
    // LiveData for observing error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    // Internal state
    private var currentCallback: SpeechRecognitionCallback? = null
    private var currentConfig: SpeechConfig? = null
    private var isInitialized = false
    private var listening = false
    private var currentEngine: SpeechEngine? = null
    
    // Coroutine scope for simulating async operations
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * Initialize the speech recognition manager with a configuration
     */
    suspend fun initialize(config: SpeechConfig): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                _recognitionStatus.postValue(RecognitionStatus.INITIALIZING)
                
                // Validate configuration
                val validationResult = validateConfiguration(config)
                if (!validationResult.isValid) {
                    _errorMessage.postValue("Invalid configuration: ${validationResult.errors.firstOrNull()}")
                    _recognitionStatus.postValue(RecognitionStatus.ERROR)
                    return@withContext false
                }
                
                // Simulate initialization delay
                delay(100)
                
                currentConfig = config
                currentEngine = config.engine
                isInitialized = true
                _recognitionStatus.postValue(RecognitionStatus.READY)
                
                true
            } catch (e: Exception) {
                _errorMessage.postValue("Initialization failed: ${e.message}")
                _recognitionStatus.postValue(RecognitionStatus.ERROR)
                false
            }
        }
    }
    
    /**
     * Start listening for speech
     */
    suspend fun startListening(callback: SpeechRecognitionCallback): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                if (!isInitialized) {
                    _errorMessage.postValue("Manager not initialized")
                    return@withContext false
                }
                
                if (listening) {
                    _errorMessage.postValue("Already listening")
                    return@withContext false
                }
                
                // Check if engine is available for testing
                currentEngine?.let { engine ->
                    if (!isEngineAvailable(engine)) {
                        _errorMessage.postValue("Engine $engine not available in test environment")
                        return@withContext false
                    }
                }
                
                currentCallback = callback
                listening = true
                _recognitionStatus.postValue(RecognitionStatus.LISTENING)
                
                // Simulate timeout handling
                currentConfig?.timeoutDuration?.let { timeout ->
                    scope.launch {
                        delay(timeout)
                        if (listening) {
                            _recognitionStatus.postValue(RecognitionStatus.TIMEOUT)
                            currentCallback?.onError("Recognition timed out")
                            _errorMessage.postValue("Recognition timed out")
                            stopListening()
                        }
                    }
                }
                
                true
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to start listening: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Stop listening for speech
     */
    fun stopListening(): Boolean {
        return try {
            listening = false
            currentCallback = null
            _recognitionStatus.value = RecognitionStatus.IDLE
            true
        } catch (e: Exception) {
            _errorMessage.value = "Failed to stop listening: ${e.message}"
            false
        }
    }
    
    /**
     * Check if currently listening
     */
    fun isListening(): Boolean = listening
    
    /**
     * Switch to a different engine
     */
    suspend fun switchEngine(config: SpeechConfig): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (listening) {
                    stopListening()
                }
                
                _recognitionStatus.postValue(RecognitionStatus.INITIALIZING)
                delay(50) // Simulate engine switch delay
                
                initialize(config)
            } catch (e: Exception) {
                _errorMessage.postValue("Engine switch failed: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Simulate receiving a recognition result (for testing)
     */
    fun onRecognitionResult(result: RecognitionResult) {
        currentConfig?.let { config ->
            if (result.confidence >= config.confidenceThreshold) {
                if (result.isFinal) {
                    currentCallback?.onResult(result)
                } else {
                    currentCallback?.onPartialResult(result.text)
                }
                _recognitionResult.value = result
            }
        } ?: run {
            currentCallback?.onResult(result)
            _recognitionResult.value = result
        }
    }
    
    /**
     * Simulate receiving a recognition error (for testing)
     */
    fun onRecognitionError(error: String) {
        currentCallback?.onError(error)
        _errorMessage.value = error
        _recognitionStatus.value = RecognitionStatus.ERROR
    }
    
    /**
     * Check if an engine is available for testing
     */
    fun isEngineAvailable(engine: SpeechEngine): Boolean {
        return when (engine) {
            SpeechEngine.VOSK -> true
            SpeechEngine.ANDROID_STT -> true
            SpeechEngine.WHISPER -> true
            SpeechEngine.VIVOKA -> false  // Not available in tests due to SDK dependency
            SpeechEngine.GOOGLE_CLOUD -> false  // Not available in tests due to API requirements
        }
    }
    
    /**
     * Validate configuration
     */
    fun validateConfiguration(config: SpeechConfig): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (config.confidenceThreshold < 0f || config.confidenceThreshold > 1f) {
            errors.add("Invalid confidence threshold: ${config.confidenceThreshold}")
        }
        
        if (config.timeoutDuration < 0) {
            errors.add("Invalid timeout duration: ${config.timeoutDuration}")
        }
        
        if (config.engine == SpeechEngine.GOOGLE_CLOUD && config.cloudApiKey.isNullOrBlank()) {
            errors.add("Google Cloud engine requires API key")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        listening = false
        currentCallback = null
        _recognitionStatus.value = RecognitionStatus.IDLE
        scope.cancel()
    }
    
    /**
     * Validation result data class
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )
}