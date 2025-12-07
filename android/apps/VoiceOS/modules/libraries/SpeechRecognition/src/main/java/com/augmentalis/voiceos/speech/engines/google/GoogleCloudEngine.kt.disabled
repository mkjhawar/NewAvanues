/**
 * GoogleCloudEngine.kt - SOLID refactored Google Cloud Speech-to-Text engine
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * Refactored into 7 SOLID components plus shared utilities for maintainability and testability:
 * 1. GoogleConfig - API configuration management
 * 2. GoogleAuth - Authentication and client management
 * 3. GoogleStreaming - Streaming recognition management
 * 4. GoogleTranscript - Transcript processing and command matching
 * 5. GoogleNetwork - Network handling and API calls
 * 6. ErrorRecoveryManager - Unified error handling and recovery (shared)
 * 7. VoiceStateManager - Centralized voice state management (shared)
 * 8. GoogleCloudEngine - Main orchestrator (this file)
 * 
 * Maintains 100% functional equivalency with the original monolithic implementation.
 */
package com.augmentalis.voiceos.speech.engines.google

import android.content.Context
import android.util.Log
import com.augmentalis.voiceos.speech.api.OnSpeechErrorListener
import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.speechrecognition.SpeechEngine
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceos.speech.engines.common.TimeoutManager
import com.augmentalis.voiceos.speech.engines.common.*
import kotlinx.coroutines.*

/**
 * Google Cloud Speech-to-Text Engine - Refactored SOLID Implementation
 * 
 * Features:
 * - Streaming recognition up to 5 minutes
 * - 125+ language support
 * - Word-level confidence scores
 * - Enhanced command matching with learning
 * - Unified error recovery with ErrorRecoveryManager
 * - Centralized voice state management with VoiceStateManager
 * - Performance monitoring
 * - Modular SOLID architecture
 */
class GoogleCloudEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleCloudEngine"
        
        // Singleton instance
        @Volatile
        private var instance: GoogleCloudEngine? = null
        
        @JvmStatic
        fun getInstance(context: Context): GoogleCloudEngine {
            return instance ?: synchronized(this) {
                instance ?: GoogleCloudEngine(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    // ===== Coroutine Scope =====
    private val engineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + 
        CoroutineName("GoogleCloudEngine")
    )
    
    // ===== SOLID Components =====
    private val config = GoogleConfig(SpeechConfig.googleCloud(""))
    private val auth = GoogleAuth()
    private val performanceMonitor = PerformanceMonitor("GoogleCloud")
    private val serviceState = ServiceState()
    private val streaming = GoogleStreaming(performanceMonitor)
    private val transcript = GoogleTranscript(context)
    private val network = GoogleNetwork(performanceMonitor)
    private val errorRecoveryManager = ErrorRecoveryManager("GoogleCloud", context)
    private val voiceStateManager = VoiceStateManager(context, "GoogleCloud")
    
    // ===== Shared Components =====
    private val timeoutManager = TimeoutManager(engineScope)
    
    // ===== Listeners =====
    private var resultListener: OnSpeechResultListener? = null
    private var errorListener: OnSpeechErrorListener? = null
    
    /**
     * Initialize the engine with UniversalInitializationManager protection
     * CRITICAL FIX: Thread-safe initialization with retry logic and race condition prevention
     */
    suspend fun initialize(speechConfig: SpeechConfig): Result<Unit> {
        Log.i(TAG, "Starting GoogleCloudEngine initialization with universal protection")
        
        val initConfig = UniversalInitializationManager.InitializationConfig(
            engineName = "GoogleCloudEngine",
            maxRetries = 3,
            initialDelayMs = 1000L,
            maxDelayMs = 8000L,
            backoffMultiplier = 2.0,
            jitterMs = 500L,
            timeoutMs = 30000L,
            allowDegradedMode = true // Google Cloud can fallback to other engines
        )
        
        val result = UniversalInitializationManager.instance.initializeEngine(
            config = initConfig,
            context = context
        ) { ctx ->
            performActualInitialization(ctx, speechConfig)
        }
        
        return when {
            result.success && result.state == UniversalInitializationManager.InitializationState.INITIALIZED -> {
                Log.i(TAG, "GoogleCloudEngine initialized successfully in ${result.totalDuration}ms")
                Result.success(Unit)
            }
            
            result.success && result.degradedMode -> {
                Log.w(TAG, "GoogleCloudEngine running in degraded mode: ${result.error}")
                Result.success(Unit) // Still usable in degraded mode
            }
            
            else -> {
                Log.e(TAG, "GoogleCloudEngine initialization failed: ${result.error}")
                Result.failure(Exception("Initialization failed: ${result.error}"))
            }
        }
    }
    
    /**
     * Perform the actual initialization logic (thread-safe, single execution)
     */
    @Suppress("UNUSED_PARAMETER")
    private suspend fun performActualInitialization(context: Context, speechConfig: SpeechConfig): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Performing actual GoogleCloudEngine initialization with SOLID components...")
                
                serviceState.setState(ServiceState.State.INITIALIZING, "Initializing Google Cloud Speech...")
                
                // Initialize configuration
                config.initialize().onFailure { 
                    Log.e(TAG, "Config initialization failed: ${it.message}")
                    return@withContext false
                }
                
                // Initialize authentication
                val apiKey = speechConfig.cloudApiKey 
                if (apiKey == null) {
                    Log.e(TAG, "Google Cloud API key required")
                    return@withContext false
                }
                
                auth.initialize(apiKey).onFailure { 
                    Log.e(TAG, "Authentication initialization failed: ${it.message}")
                    return@withContext false
                }
                
                // Initialize streaming components
                streaming.initialize().onFailure { 
                    Log.e(TAG, "Streaming initialization failed: ${it.message}")
                    return@withContext false
                }
                
                // Initialize transcript processor
                transcript.initialize().onFailure { 
                    Log.e(TAG, "Transcript initialization failed: ${it.message}")
                    return@withContext false
                }
                
                // Initialize voice state manager
                voiceStateManager.initialize()
                
                // Initialize error recovery manager
                initializeErrorRecovery()
                
                // Update configuration
                config.updateConfiguration(speechConfig).onFailure { 
                    Log.e(TAG, "Configuration update failed: ${it.message}")
                    return@withContext false
                }
                
                // Set transcript mode
                transcript.setMode(speechConfig.mode)
                
                serviceState.setState(ServiceState.State.INITIALIZED)
                Log.i(TAG, "GoogleCloudEngine actual initialization completed successfully")
                
                // Update voice state to ready after successful initialization
                voiceStateManager.setVoiceEnabled(true)
                
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Actual initialization failed", e)
                serviceState.setState(ServiceState.State.ERROR, "Initialization failed: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Start streaming recognition
     */
    fun startListening(): Result<Unit> {
        return try {
            // Check voice state first
            if (!voiceStateManager.isReady()) {
                return Result.failure(
                    IllegalStateException("Voice system not ready: enabled=${voiceStateManager.isVoiceEnabled()}, sleeping=${voiceStateManager.isVoiceSleeping()}")
                )
            }
            
            // Check state
            val currentState = serviceState.currentState
            if (currentState != ServiceState.State.INITIALIZED && 
                currentState != ServiceState.State.READY) {
                return Result.failure(
                    IllegalStateException("Cannot start in state: $currentState")
                )
            }
            
            // Validate components
            val client = auth.getClient() ?: return Result.failure(
                IllegalStateException("Authentication not completed")
            )
            
            val recognitionConfig = config.getCurrentRecognitionConfig() ?: return Result.failure(
                IllegalStateException("Recognition configuration not available")
            )
            
            // Start streaming with recognition callback
            streaming.startStreaming(
                recognitionConfig = recognitionConfig,
                onResult = { audioData: ByteArray, config: Any? ->
                    handleRecognitionRequest(client, audioData, config)
                },
                onError = { _: Int, message: String ->
                    handleErrorWithRecovery(ErrorRecoveryManager.ErrorType.RECOGNITION_ERROR, message)
                },
                onTimeout = {
                    handleTimeout()
                }
            )
            
            // Start timeout management
            timeoutManager.startTimeout(
                duration = config.getSpeechConfig().timeoutDuration,
                onTimeout = { handleTimeout() }
            )
            
            serviceState.setState(ServiceState.State.LISTENING)
            Log.i(TAG, "Started listening with SOLID components")
            
            // Update voice state - we're actively listening for commands
            voiceStateManager.updateCommandExecutionTime()
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stop streaming recognition
     */
    fun stopListening(): Result<Unit> {
        return try {
            // Stop streaming
            streaming.stopStreaming()
            
            // Cancel timeout
            timeoutManager.cancelTimeout()
            
            // Update state
            serviceState.setState(ServiceState.State.READY)
            
            Log.i(TAG, "Stopped listening")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop listening", e)
            Result.failure(e)
        }
    }
    
    /**
     * Set static commands for enhanced matching
     */
    fun setStaticCommands(commands: List<String>) {
        transcript.setStaticCommands(commands)
        
        // Update phrase hints in configuration
        config.updatePhraseHints(commands)
        
        Log.i(TAG, "Set ${commands.size} static commands")
    }
    
    /**
     * Set dynamic commands from UI
     */
    fun setDynamicCommands(commands: List<String>) {
        transcript.setDynamicCommands(commands)
        
        // Update phrase hints in configuration
        config.updatePhraseHints(commands)
        
        Log.i(TAG, "Set ${commands.size} dynamic commands")
    }
    
    /**
     * Change recognition mode
     */
    fun changeMode(mode: SpeechMode): Result<Unit> {
        return try {
            val wasListening = serviceState.isListening()
            
            // Stop current recognition if active
            if (wasListening) {
                stopListening()
            }
            
            // Update configuration mode
            config.changeMode(mode).onFailure { 
                return Result.failure(it)
            }
            
            // Update transcript processor mode
            transcript.setMode(mode)
            
            // Restart if it was active
            if (wasListening) {
                startListening()
            }
            
            Log.i(TAG, "Successfully changed to mode: $mode")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to change mode to $mode", e)
            Result.failure(e)
        }
    }
    
    /**
     * Set result listener
     */
    fun setResultListener(listener: OnSpeechResultListener) {
        this.resultListener = listener
        serviceState.setListener(listener)
    }
    
    /**
     * Set error listener
     */
    fun setErrorListener(listener: OnSpeechErrorListener) {
        this.errorListener = listener
    }
    
    /**
     * Handle recognition request from streaming component
     */
    private fun handleRecognitionRequest(
        client: Any?, // GoogleCloudSpeechLite
        audioData: ByteArray,
        recognitionConfig: Any? // GoogleCloudSpeechLite.RecognitionConfig
    ) {
        engineScope.launch {
            try {
                // Perform network call
                network.performRecognition(audioData, recognitionConfig, client)
                    .onSuccess { result ->
                        if (result.isNotEmpty()) {
                            processRecognitionResult(result)
                        }
                    }
                    .onFailure { error ->
                        handleErrorWithRecovery(ErrorRecoveryManager.ErrorType.NETWORK_ERROR, error.message ?: "Network recognition failed", error)
                    }
                    
            } catch (e: Exception) {
                handleErrorWithRecovery(ErrorRecoveryManager.ErrorType.RECOGNITION_ERROR, "Recognition request failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Process recognition result through transcript component
     */
    private suspend fun processRecognitionResult(text: String) {
        try {
            // Process through transcript component
            val result = transcript.processRecognitionResult(
                text = text,
                confidence = 0.8f, // Default confidence for Google Cloud
                isFinal = true,
                alternatives = emptyList(),
                metadata = mapOf(
                    "language" to config.getSpeechConfig().language,
                    "engine" to SpeechEngine.GOOGLE_CLOUD.name
                )
            )
            
            // Send result to listener if accepted
            result?.let { processedResult ->
                withContext(Dispatchers.Main) {
                    resultListener?.invoke(processedResult)
                }
                
                // Update voice state on successful recognition
                voiceStateManager.updateCommandExecutionTime()
                
                // Reset timeout on successful result
                timeoutManager.resetTimeout(
                    onTimeout = { handleTimeout() }
                )
            }
            
        } catch (e: Exception) {
            handleErrorWithRecovery(ErrorRecoveryManager.ErrorType.RECOGNITION_ERROR, "Result processing failed: ${e.message}", e)
        }
    }
    
    /**
     * Handle recognition timeout
     */
    private fun handleTimeout() {
        Log.d(TAG, "Recognition timeout occurred")
        
        engineScope.launch {
            val errorContext = ErrorRecoveryManager.ErrorContext(
                type = ErrorRecoveryManager.ErrorType.TIMEOUT_ERROR,
                message = "Recognition timeout"
            )
            
            val recoveryResult = errorRecoveryManager.handleError(errorContext)
            
            if (recoveryResult.success && streaming.isStreaming()) {
                // Restart streaming for timeout recovery
                val recognitionConfig = config.getCurrentRecognitionConfig()
                if (recognitionConfig != null) {
                    streaming.restartStreaming(recognitionConfig)
                }
            } else {
                stopListening()
            }
        }
    }
    
    /**
     * Initialize error recovery manager with callbacks
     */
    private fun initializeErrorRecovery() {
        errorRecoveryManager.setCallbacks(
            onSuccess = { errorType ->
                Log.i(TAG, "Recovery succeeded for error type: $errorType")
            },
            onFailed = { errorType, message ->
                Log.e(TAG, "Recovery failed for error type: $errorType - $message")
                errorListener?.invoke(message, getErrorCodeForType(errorType))
            },
            onDegrade = { inDegradedMode ->
                Log.i(TAG, "Degraded mode: $inDegradedMode")
            }
        )
    }
    
    /**
     * Handle error with recovery using ErrorRecoveryManager
     */
    private fun handleErrorWithRecovery(
        errorType: ErrorRecoveryManager.ErrorType, 
        message: String, 
        throwable: Throwable? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        engineScope.launch {
            val errorContext = ErrorRecoveryManager.ErrorContext(
                type = errorType,
                message = message,
                throwable = throwable,
                metadata = metadata
            )
            
            val recoveryResult = errorRecoveryManager.handleError(errorContext)
            
            // Handle recovery result based on strategy used
            when (recoveryResult.strategyUsed) {
                ErrorRecoveryManager.RecoveryStrategy.REINITIALIZE -> {
                    handleEngineRestart()
                }
                ErrorRecoveryManager.RecoveryStrategy.RETRY_WITH_BACKOFF,
                ErrorRecoveryManager.RecoveryStrategy.RETRY_IMMEDIATE -> {
                    // Restart recognition if we were listening
                    if (serviceState.isListening()) {
                        val recognitionConfig = config.getCurrentRecognitionConfig()
                        if (recognitionConfig != null) {
                            streaming.restartStreaming(recognitionConfig)
                        }
                    }
                }
                else -> {
                    // Other strategies are handled by ErrorRecoveryManager internally
                    Log.d(TAG, "Recovery strategy applied: ${recoveryResult.strategyUsed}")
                }
            }
        }
    }
    
    /**
     * Handle full engine restart
     */
    private fun handleEngineRestart() {
        engineScope.launch {
            try {
                Log.i(TAG, "Performing engine restart for error recovery")
                
                val wasListening = serviceState.isListening()
                val currentConfig = config.getSpeechConfig()
                
                // Stop current operation
                stopListening()
                
                // Re-initialize components
                initialize(currentConfig)
                
                // Restart listening if it was active
                if (wasListening) {
                    delay(1000) // Brief delay before restart
                    startListening()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Engine restart failed", e)
                handleErrorWithRecovery(ErrorRecoveryManager.ErrorType.INITIALIZATION_FAILED, "Engine restart failed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Get comprehensive statistics from all components
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "engine" to "GoogleCloud-SOLID",
            "state" to serviceState.currentState.name,
            "config" to config.getStats(),
            "auth" to auth.getAuthStats(),
            "streaming" to streaming.getStreamingStats(),
            "transcript" to transcript.getCacheStats(),
            "network" to network.getNetworkStats(),
            "errors" to errorRecoveryManager.getErrorStatistics(),
            "performance" to performanceMonitor.getMetrics(),
            "voiceState" to voiceStateManager.getCurrentState()
        )
    }
    
    /**
     * Get performance metrics
     */
    fun getPerformanceMetrics(): Map<String, Any> {
        val metrics = performanceMonitor.getMetrics()
        return mapOf(
            "averageLatency" to metrics.averageLatency,
            "successRate" to metrics.successRate,
            "totalRecognitions" to metrics.totalRecognitions,
            "sessionDuration" to metrics.sessionDuration,
            "memoryUsage" to metrics.currentMemoryUsage
        )
    }
    
    /**
     * Update configuration
     */
    suspend fun updateConfiguration(newConfig: SpeechConfig): Result<Unit> {
        return config.updateConfiguration(newConfig).also { result ->
            if (result.isSuccess) {
                transcript.updateForLanguageChange()
            }
        }
    }
    
    /**
     * Get current mode
     */
    fun getCurrentMode(): SpeechMode = config.getCurrentMode()
    
    /**
     * Enable/disable voice system
     */
    fun setVoiceEnabled(enabled: Boolean): Boolean {
        return voiceStateManager.setVoiceEnabled(enabled)
    }
    
    /**
     * Put voice system to sleep
     */
    fun enterSleepMode(): Boolean {
        val result = voiceStateManager.enterSleepMode()
        if (result && serviceState.isListening()) {
            stopListening()
        }
        return result
    }
    
    /**
     * Wake voice system from sleep
     */
    fun exitSleepMode(): Boolean {
        return voiceStateManager.exitSleepMode()
    }
    
    /**
     * Check if voice system is ready
     */
    fun isVoiceReady(): Boolean = voiceStateManager.isReady()
    
    /**
     * Get voice state information
     */
    fun getVoiceState(): VoiceStateManager.VoiceState = voiceStateManager.getCurrentState()
    
    /**
     * Map ErrorRecoveryManager error types to error codes for backward compatibility
     */
    private fun getErrorCodeForType(errorType: ErrorRecoveryManager.ErrorType): Int {
        return when (errorType) {
            ErrorRecoveryManager.ErrorType.AUTHENTICATION_ERROR -> 1001
            ErrorRecoveryManager.ErrorType.NETWORK_ERROR -> 1002
            ErrorRecoveryManager.ErrorType.SERVICE_UNAVAILABLE -> 1003
            ErrorRecoveryManager.ErrorType.RECOGNITION_ERROR -> 1004
            ErrorRecoveryManager.ErrorType.AUDIO_ERROR -> 1005
            ErrorRecoveryManager.ErrorType.TIMEOUT_ERROR -> 1006
            ErrorRecoveryManager.ErrorType.INITIALIZATION_FAILED -> 1007
            else -> 1999 // Unknown error
        }
    }
    
    /**
     * Clean shutdown of all components
     */
    fun shutdown() {
        try {
            Log.i(TAG, "Shutting down GoogleCloudEngine with all SOLID components...")
            
            // Stop recognition
            stopListening()
            
            // Shutdown components in reverse order
            streaming.shutdown()
            network.shutdown()
            errorRecoveryManager.destroy()
            transcript.shutdown()
            auth.shutdown()
            voiceStateManager.destroy()
            
            // Cancel timeout manager
            timeoutManager.cancelTimeout()
            
            // Cancel engine scope
            engineScope.cancel()
            
            // Destroy performance monitor
            performanceMonitor.destroy()
            
            // Update state
            serviceState.setState(ServiceState.State.SHUTDOWN)
            
            // Disable voice state
            voiceStateManager.setVoiceEnabled(false)
            
            Log.i(TAG, "GoogleCloudEngine SOLID components shutdown complete")
            
        } catch (e: Exception) {
            Log.e(TAG, "Shutdown error", e)
        }
    }
}