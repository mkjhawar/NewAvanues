/**
 * SpeechViewModel.kt - ViewModel for speech recognition
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Purpose: Manages state and interaction with SpeechRecognition library
 * Direct implementation following VOS4 zero-overhead architecture
 */
package com.augmentalis.voicerecognition.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.speechrecognition.SpeechEngine
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.augmentalis.voiceos.speech.api.SpeechListenerManager
import com.augmentalis.voiceos.speech.engines.android.AndroidSTTEngine
import com.augmentalis.voiceos.speech.engines.vosk.VoskEngine
import com.augmentalis.voiceos.speech.engines.vivoka.VivokaEngine
// GoogleCloudEngine temporarily disabled
import com.augmentalis.voiceos.speech.engines.whisper.WhisperEngine
import com.augmentalis.voicerecognition.ui.SpeechConfigurationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import android.util.Log

/**
 * UI state for speech recognition
 */
data class SpeechUiState(
    val isListening: Boolean = false,
    val selectedEngine: SpeechEngine = SpeechEngine.ANDROID_STT,
    val currentTranscript: String = "",
    val fullTranscript: String = "",
    val errorMessage: String? = null,
    val isInitialized: Boolean = false,
    val engineStatus: String = "Not initialized",
    val confidence: Float = 0f
)

/**
 * ViewModel for speech recognition functionality
 */
class SpeechViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(SpeechUiState())
    val uiState: StateFlow<SpeechUiState> = _uiState.asStateFlow()
    
    // ENHANCED FIX: Thread-safe engine management with additional protections
    private var currentEngine: Any? = null
    private val engineMutex = Mutex()
    private val isInitializing = AtomicBoolean(false)
    private val initializationMutex = Mutex()
    private val engineCleanupMutex = Mutex()
    private val engineSwitchingMutex = Mutex()
    
    // Additional race condition protections
    private val isDestroying = AtomicBoolean(false)
    private val lastInitializationAttempt = AtomicLong(0L)
    private val initializationAttempts = AtomicLong(0L)
    
    // Engine state tracking for better error recovery
    private var lastSuccessfulEngine: SpeechEngine? = null
    private var engineInitializationHistory = mutableMapOf<SpeechEngine, Long>()
    
    private val listenerManager = SpeechListenerManager()
    private var currentConfiguration = SpeechConfigurationData()
    
    init {
        setupListeners()
    }
    
    /**
     * Setup speech recognition listeners
     */
    private fun setupListeners() {
        listenerManager.onResult = { result ->
            handleSpeechResult(result)
        }
        
        listenerManager.onError = { error, code ->
            _uiState.value = _uiState.value.copy(
                errorMessage = "Error ($code): $error",
                isListening = false,
                engineStatus = "Error occurred"
            )
        }
        
        listenerManager.onStateChange = { state, message ->
            _uiState.value = _uiState.value.copy(
                engineStatus = if (message != null) "$state: $message" else state
            )
        }
    }
    
    /**
     * Handle speech recognition results
     */
    private fun handleSpeechResult(result: RecognitionResult) {
        val currentText = result.text
        val confidence = result.confidence
        
        _uiState.value = _uiState.value.copy(
            currentTranscript = currentText,
            confidence = confidence,
            errorMessage = null
        )
        
        // Append to full transcript if this is a final result
        if (result.isFinal) {
            val fullText = if (_uiState.value.fullTranscript.isEmpty()) {
                currentText
            } else {
                "${_uiState.value.fullTranscript} $currentText"
            }
            
            _uiState.value = _uiState.value.copy(
                fullTranscript = fullText,
                currentTranscript = ""
            )
        }
    }
    
    /**
     * Initialize speech engine with enhanced thread-safe race condition prevention
     * ENHANCED FIX: Prevents multiple concurrent initialization attempts with comprehensive protection
     */
    fun initializeEngine(engine: SpeechEngine) {
        viewModelScope.launch {
            // Check if system is being destroyed
            if (isDestroying.get()) {
                Log.w("SpeechViewModel", "Cannot initialize ${engine.name} - ViewModel is being destroyed")
                return@launch
            }
            
            val currentTime = System.currentTimeMillis()
            lastInitializationAttempt.set(currentTime)
            initializationAttempts.incrementAndGet()
            
            // Prevent too frequent initialization attempts
            val lastAttempt = engineInitializationHistory[engine] ?: 0L
            if (currentTime - lastAttempt < 1000L) { // 1 second minimum between attempts
                Log.w("SpeechViewModel", "Initialization attempt too frequent for ${engine.name}, waiting...")
                delay(1000L - (currentTime - lastAttempt))
            }
            
            // Prevent concurrent initialization attempts
            if (!isInitializing.compareAndSet(false, true)) {
                Log.w("SpeechViewModel", "Initialization already in progress for ${engine.name}")
                return@launch
            }
            
            engineSwitchingMutex.withLock {
                initializationMutex.withLock {
                    try {
                        Log.i("SpeechViewModel", "Starting enhanced thread-safe initialization of ${engine.name} (attempt #${initializationAttempts.get()})")
                        
                        engineInitializationHistory[engine] = currentTime
                        
                        // Enhanced cleanup of previous engine
                        cleanupPreviousEngine()
                        
                        _uiState.value = _uiState.value.copy(
                            selectedEngine = engine,
                            engineStatus = "Initializing ${engine.name}...",
                            errorMessage = null,
                            isInitialized = false
                        )
                        
                        // Create and initialize engine with proper error handling
                        val newEngine = createEngineInstance(engine)
                        val initSuccess = initializeEngineInstanceWithRetry(newEngine, engine)
                        
                        if (initSuccess) {
                            // Update engine reference safely
                            engineMutex.withLock {
                                currentEngine = newEngine
                                lastSuccessfulEngine = engine
                            }
                            
                            setupEngineListeners(newEngine)
                            
                            _uiState.value = _uiState.value.copy(
                                isInitialized = true,
                                engineStatus = "${engine.name} ready",
                                errorMessage = null
                            )
                            
                            Log.i("SpeechViewModel", "${engine.name} initialized successfully")
                        } else {
                            // Attempt fallback to last successful engine if available
                            handleInitializationFailure(engine)
                        }
                        
                    } catch (e: Exception) {
                        Log.e("SpeechViewModel", "Exception during ${engine.name} initialization", e)
                        handleInitializationException(engine, e)
                    } finally {
                        isInitializing.set(false)
                    }
                }
            }
        }
    }
    
    /**
     * Enhanced cleanup of previous engine with proper resource management
     */
    private suspend fun cleanupPreviousEngine() {
        engineCleanupMutex.withLock {
            try {
                Log.d("SpeechViewModel", "Enhanced cleanup of previous engine")
                
                // Stop any ongoing operations
                stopListening()
                
                // Clean up current engine if exists
                val engine = currentEngine
                if (engine != null) {
                    Log.d("SpeechViewModel", "Cleaning up engine: ${engine::class.simpleName}")
                    
                    // Engine-specific cleanup
                    try {
                        when (engine) {
                            is AndroidSTTEngine -> {
                                // AndroidSTT cleanup is handled internally
                            }
                            is VoskEngine -> {
                                // VoskEngine cleanup
                                engine.destroy()
                            }
                            is VivokaEngine -> {
                                // VivokaEngine cleanup
                                engine.destroy()
                            }
                            is WhisperEngine -> {
                                // WhisperEngine cleanup
                                engine.destroy()
                            }
                            else -> {
                                Log.w("SpeechViewModel", "Unknown engine type for cleanup: ${engine::class.simpleName}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("SpeechViewModel", "Engine cleanup failed (non-critical): ${e.message}")
                    }
                }
                
                currentEngine = null
                
                // Small delay to ensure cleanup is complete
                delay(200)
                
            } catch (e: Exception) {
                Log.e("SpeechViewModel", "Enhanced cleanup failed", e)
                // Continue with initialization even if cleanup failed
            }
        }
    }
    
    /**
     * Initialize engine instance with retry logic
     */
    private suspend fun initializeEngineInstanceWithRetry(engineInstance: Any, engineType: SpeechEngine): Boolean {
        val maxRetries = 2
        var lastError: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                Log.d("SpeechViewModel", "Engine initialization attempt ${attempt + 1}/$maxRetries for ${engineType.name}")
                
                val result = initializeEngineInstance(engineInstance, engineType)
                if (result) {
                    Log.i("SpeechViewModel", "${engineType.name} initialized successfully on attempt ${attempt + 1}")
                    return true
                }
            } catch (e: Exception) {
                lastError = e
                Log.w("SpeechViewModel", "Engine initialization attempt ${attempt + 1} failed: ${e.message}")
                
                if (attempt < maxRetries - 1) {
                    delay(1000L * (attempt + 1)) // Progressive delay
                }
            }
        }
        
        Log.e("SpeechViewModel", "All initialization attempts failed for ${engineType.name}", lastError)
        return false
    }
    
    /**
     * Handle initialization failure with fallback strategy
     */
    private suspend fun handleInitializationFailure(engine: SpeechEngine) {
        Log.w("SpeechViewModel", "Handling initialization failure for ${engine.name}")
        
        val fallbackEngine = lastSuccessfulEngine
        if (fallbackEngine != null && fallbackEngine != engine) {
            Log.i("SpeechViewModel", "Attempting fallback to last successful engine: ${fallbackEngine.name}")
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to initialize ${engine.name}, falling back to ${fallbackEngine.name}",
                isInitialized = false,
                engineStatus = "Falling back..."
            )
            
            // Small delay before fallback attempt
            delay(500)
            
            // Recursive call to initialize fallback (but prevent infinite loops)
            if (initializationAttempts.get() < 5L) {
                initializeEngine(fallbackEngine)
            } else {
                Log.e("SpeechViewModel", "Too many initialization attempts, stopping fallback")
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Multiple initialization failures - please restart the app",
                    isInitialized = false,
                    engineStatus = "Multiple failures"
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to initialize ${engine.name}",
                isInitialized = false,
                engineStatus = "Initialization failed"
            )
        }
    }
    
    /**
     * Handle initialization exceptions with detailed logging
     */
    private fun handleInitializationException(engine: SpeechEngine, exception: Exception) {
        Log.e("SpeechViewModel", "Exception during ${engine.name} initialization", exception)
        
        val errorMessage = when {
            exception.message?.contains("Cannot call 'Vsdk.init' multiple times") == true ->
                "Vivoka SDK already initialized - please restart the app"
            exception.message?.contains("timeout") == true ->
                "Engine initialization timed out - please try again"
            exception.message?.contains("permission") == true ->
                "Permissions required for ${engine.name}"
            else ->
                "Initialization error: ${exception.message}"
        }
        
        _uiState.value = _uiState.value.copy(
            errorMessage = errorMessage,
            isInitialized = false,
            engineStatus = "Initialization failed"
        )
    }
    
    /**
     * Create engine instance based on selection
     */
    private fun createEngineInstance(engine: SpeechEngine): Any {
        return when (engine) {
            SpeechEngine.ANDROID_STT -> AndroidSTTEngine(getApplication())
            SpeechEngine.VOSK -> VoskEngine(getApplication())
            SpeechEngine.VIVOKA -> VivokaEngine(getApplication())
            SpeechEngine.GOOGLE_CLOUD -> {
                Log.w("SpeechViewModel", "GoogleCloudEngine disabled, using Android STT fallback")
                AndroidSTTEngine(getApplication())
            }
            SpeechEngine.WHISPER -> WhisperEngine(getApplication())
        }
    }
    
    /**
     * Initialize engine instance with proper async handling
     */
    private suspend fun initializeEngineInstance(engineInstance: Any, engineType: SpeechEngine): Boolean {
        return try {
            val config = createConfig(engineType)
            
            when (engineInstance) {
                is AndroidSTTEngine -> {
                    engineInstance.initialize(getApplication(), config)
                }
                is VoskEngine -> {
                    engineInstance.initialize(config)
                }
                is VivokaEngine -> {
                    engineInstance.initialize(config)
                }
                is WhisperEngine -> {
                    engineInstance.initialize(config)
                }
                else -> {
                    Log.e("SpeechViewModel", "Unknown engine type: $engineInstance")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("SpeechViewModel", "Engine initialization failed", e)
            false
        }
    }
    
    /**
     * Setup engine listeners safely
     */
    private fun setupEngineListeners(engineInstance: Any) {
        when (engineInstance) {
            is AndroidSTTEngine -> {
                engineInstance.setResultListener { result -> 
                    listenerManager.onResult?.invoke(result) 
                }
                engineInstance.setErrorListener { error, code -> 
                    listenerManager.onError?.invoke(error, code) 
                }
            }
            is VoskEngine -> {
                engineInstance.setResultListener { result -> 
                    listenerManager.onResult?.invoke(result) 
                }
                engineInstance.setErrorListener { error, code -> 
                    listenerManager.onError?.invoke(error, code) 
                }
            }
            is VivokaEngine -> {
                engineInstance.setResultListener { result -> 
                    listenerManager.onResult?.invoke(result) 
                }
                engineInstance.setErrorListener { error, code -> 
                    listenerManager.onError?.invoke(error, code) 
                }
            }
            is WhisperEngine -> {
                engineInstance.setResultListener { result -> 
                    listenerManager.onResult?.invoke(result) 
                }
                engineInstance.setErrorListener { error, code -> 
                    listenerManager.onError?.invoke(error, code) 
                }
            }
            else -> {
                Log.w("SpeechViewModel", "Unknown engine type for listener setup: $engineInstance")
            }
        }
    }
    
    /**
     * Create configuration for engine
     */
    private fun createConfig(@Suppress("UNUSED_PARAMETER") engine: SpeechEngine): SpeechConfig {
        // Engine parameter will be used when engine-specific config is needed
        return SpeechConfig(
            language = currentConfiguration.language,
            mode = currentConfiguration.mode,
            enableVAD = currentConfiguration.enableVAD,
            confidenceThreshold = currentConfiguration.confidenceThreshold,
            maxRecordingDuration = currentConfiguration.maxRecordingDuration,
            timeoutDuration = currentConfiguration.timeoutDuration,
            enableProfanityFilter = currentConfiguration.enableProfanityFilter,
            engine = engine
        )
    }
    
    /**
     * Update configuration settings
     */
    fun updateConfiguration(config: SpeechConfigurationData) {
        currentConfiguration = config
        // Re-initialize current engine with new config if needed
        currentEngine?.let {
            _uiState.value.selectedEngine.let { engine ->
                initializeEngine(engine)
            }
        }
    }
    
    /**
     * Start listening with thread safety
     * CRITICAL FIX: Prevents concurrent access to currentEngine
     */
    fun startListening() {
        if (!_uiState.value.isInitialized) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Engine not initialized"
            )
            return
        }
        
        viewModelScope.launch {
            engineMutex.withLock {
                try {
                    val engine = currentEngine
                    if (engine == null) {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "No engine available",
                            isListening = false
                        )
                        return@withLock
                    }
                    
                    Log.d("SpeechViewModel", "Starting listening with engine: ${engine::class.simpleName}")
                    
                    when (engine) {
                        is AndroidSTTEngine -> engine.startListening(currentConfiguration.mode)
                        is VoskEngine -> engine.startListening()
                        is VivokaEngine -> engine.startListening()
                        // GoogleCloudEngine disabled - handled by fallback
                        is WhisperEngine -> engine.startListening()
                        else -> {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Engine type not supported: ${engine::class.simpleName}",
                                isListening = false
                            )
                            return@withLock
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isListening = true,
                        engineStatus = "Listening...",
                        errorMessage = null
                    )
                    
                } catch (e: Exception) {
                    Log.e("SpeechViewModel", "Failed to start listening", e)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to start: ${e.message}",
                        isListening = false
                    )
                }
            }
        }
    }
    
    /**
     * Stop listening with thread safety
     * CRITICAL FIX: Thread-safe access to currentEngine
     */
    fun stopListening() {
        viewModelScope.launch {
            engineMutex.withLock {
                try {
                    val engine = currentEngine
                    if (engine != null) {
                        Log.d("SpeechViewModel", "Stopping listening with engine: ${engine::class.simpleName}")
                        
                        when (engine) {
                            is AndroidSTTEngine -> engine.stopListening()
                            is VoskEngine -> engine.stopListening()
                            is VivokaEngine -> engine.stopListening()
                            // GoogleCloudEngine disabled - handled by fallback
                            is WhisperEngine -> engine.stopListening()
                            else -> {
                                Log.w("SpeechViewModel", "Unknown engine type for stop: ${engine::class.simpleName}")
                            }
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isListening = false,
                        engineStatus = "Stopped"
                    )
                    
                } catch (e: Exception) {
                    Log.e("SpeechViewModel", "Error stopping listening", e)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error stopping: ${e.message}",
                        isListening = false
                    )
                }
            }
        }
    }
    
    /**
     * Clear transcript
     */
    fun clearTranscript() {
        _uiState.value = _uiState.value.copy(
            currentTranscript = "",
            fullTranscript = "",
            confidence = 0f
        )
    }
    
    /**
     * Clean up resources with enhanced protection against race conditions
     * ENHANCED FIX: Comprehensive cleanup with destruction state tracking
     */
    override fun onCleared() {
        super.onCleared()
        
        // Set destruction flag to prevent new operations
        isDestroying.set(true)
        
        viewModelScope.launch {
            try {
                Log.i("SpeechViewModel", "Enhanced cleanup starting...")
                
                // Stop all operations first
                stopListening()
                
                // Wait for any ongoing initialization to complete (with timeout)
                val maxWaitTime = 3000L // 3 seconds
                val startTime = System.currentTimeMillis()
                
                while (isInitializing.get() && (System.currentTimeMillis() - startTime) < maxWaitTime) {
                    delay(100)
                }
                
                if (isInitializing.get()) {
                    Log.w("SpeechViewModel", "Initialization still in progress during cleanup, forcing cleanup")
                }
                
                // Enhanced cleanup
                cleanupPreviousEngine()
                
                // Clear state
                engineInitializationHistory.clear()
                lastSuccessfulEngine = null
                
                Log.i("SpeechViewModel", "Enhanced cleanup completed")
                
            } catch (e: Exception) {
                Log.e("SpeechViewModel", "Error during enhanced cleanup", e)
            }
        }
    }
    
    /**
     * Get initialization diagnostics for debugging
     */
    fun getInitializationDiagnostics(): Map<String, Any> {
        return mapOf(
            "total_attempts" to initializationAttempts.get(),
            "last_attempt" to lastInitializationAttempt.get(),
            "is_initializing" to isInitializing.get(),
            "is_destroying" to isDestroying.get(),
            "last_successful_engine" to (lastSuccessfulEngine?.name ?: "none"),
            "engine_history" to engineInitializationHistory.mapKeys { it.key.name },
            "current_engine" to (currentEngine?.javaClass?.simpleName ?: "none")
        )
    }
}