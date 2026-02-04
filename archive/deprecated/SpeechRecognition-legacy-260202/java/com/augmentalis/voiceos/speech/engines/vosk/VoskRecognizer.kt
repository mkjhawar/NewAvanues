/**
 * VoskRecognizer.kt - Dual recognizer system for VOSK speech recognition engine
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * SOLID Principle: Single Responsibility
 * - Manages dual recognizer system (command and dictation)
 * - Handles recognizer creation, switching, and cleanup
 * - Provides thread-safe recognizer state management
 */
package com.augmentalis.voiceos.speech.engines.vosk

import android.util.Log
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceos.speech.engines.common.ServiceState
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import com.augmentalis.voiceisolation.VoiceIsolation

/**
 * Dual recognizer manager for VOSK engine.
 * Handles command and dictation recognizers with thread-safe switching.
 */
class VoskRecognizer(
    private val serviceState: ServiceState,
    private val config: VoskConfig,
    private val voiceIsolation: VoiceIsolation? = null
) {
    
    companion object {
        private const val TAG = "VoskRecognizer"
        private const val SAMPLE_RATE = 16000.0f
    }
    
    // Recognizer instances
    @Volatile
    private var commandRecognizer: Recognizer? = null
    @Volatile
    private var dictationRecognizer: Recognizer? = null
    @Volatile
    private var currentRecognizer: Recognizer? = null
    
    // Synchronization
    private val recognizerLock = Any()
    
    // Speech service
    @Volatile
    private var speechService: SpeechService? = null
    
    // State tracking
    @Volatile
    private var currentMode = SpeechMode.DYNAMIC_COMMAND
    @Volatile
    private var isInitialized = false
    private var lastModeSwitch = System.currentTimeMillis()
    private var modeSwitchCount = 0
    
    // Error tracking
    private var lastError: String? = null
    private val errorHistory = mutableListOf<RecognizerError>()

    // VoiceIsolation state
    @Volatile
    private var isVoiceIsolationInitialized = false
    
    /**
     * Initialize both recognizers
     */
    fun initialize(model: Model, grammarJson: String?): Boolean {
        return synchronized(recognizerLock) {
            try {
                Log.i(TAG, "Initializing dual recognizer system...")
                
                // Clean up any existing recognizers first
                cleanup()
                
                var tempCommandRecognizer: Recognizer? = null
                var tempDictationRecognizer: Recognizer? = null
                
                try {
                    // Create command recognizer with grammar constraints
                    tempCommandRecognizer = createCommandRecognizer(model, grammarJson)
                    
                    // Create dictation recognizer without constraints
                    tempDictationRecognizer = createDictationRecognizer(model)
                    
                    // Only assign if both succeed
                    commandRecognizer = tempCommandRecognizer
                    dictationRecognizer = tempDictationRecognizer
                    
                    // Start with command mode
                    switchToCommandMode()
                    isInitialized = true
                    
                    Log.i(TAG, "Dual recognizer system initialized successfully")
                    return@synchronized true
                    
                } catch (e: Exception) {
                    // Clean up on failure
                    safeCloseRecognizer(tempCommandRecognizer)
                    safeCloseRecognizer(tempDictationRecognizer)
                    throw e
                }
                
            } catch (e: Exception) {
                val errorMsg = "Failed to initialize recognizers: ${e.message}"
                recordError(errorMsg, e)
                Log.e(TAG, errorMsg, e)
                
                // Try fallback to single recognizer
                return@synchronized tryFallbackMode(model)
            }
        }
    }
    
    /**
     * Create command recognizer with grammar constraints
     */
    private fun createCommandRecognizer(model: Model, grammarJson: String?): Recognizer {
        return try {
            if (config.isGrammarConstraintsEnabled() && !grammarJson.isNullOrEmpty()) {
                Log.d(TAG, "Creating grammar-constrained command recognizer")
                Recognizer(model, SAMPLE_RATE, grammarJson)
            } else {
                Log.d(TAG, "Creating unconstrained command recognizer")
                Recognizer(model, SAMPLE_RATE)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Grammar-constrained recognizer failed, falling back to unconstrained: ${e.message}")
            Recognizer(model, SAMPLE_RATE)
        }
    }
    
    /**
     * Create dictation recognizer without constraints
     */
    private fun createDictationRecognizer(model: Model): Recognizer {
        return try {
            Log.d(TAG, "Creating dictation recognizer")
            Recognizer(model, SAMPLE_RATE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create dictation recognizer: ${e.message}")
            throw e
        }
    }
    
    /**
     * Switch to command recognition mode
     */
    fun switchToCommandMode(): Boolean {
        if (currentMode == SpeechMode.DYNAMIC_COMMAND) {
            return true // Already in command mode
        }
        
        return synchronized(recognizerLock) {
            try {
                Log.d(TAG, "Switching to command mode...")
                
                val wasListening = serviceState.isListening()
                if (wasListening) {
                    stopListening()
                }
                
                val cmdRecognizer = commandRecognizer
                if (cmdRecognizer != null) {
                    currentRecognizer = cmdRecognizer
                    currentMode = SpeechMode.DYNAMIC_COMMAND
                    lastModeSwitch = System.currentTimeMillis()
                    modeSwitchCount++
                    
                    // Recreate speech service with new recognizer
                    speechService = SpeechService(cmdRecognizer, SAMPLE_RATE)
                    
                    // Resume listening if we were listening before
                    if (wasListening && isInitialized) {
                        startListening(null) // Will use current listener
                    }
                    
                    Log.d(TAG, "Successfully switched to command mode")
                    true
                } else {
                    val errorMsg = "Command recognizer not available"
                    recordError(errorMsg, null)
                    Log.e(TAG, errorMsg)
                    false
                }
                
            } catch (e: Exception) {
                val errorMsg = "Failed to switch to command mode: ${e.message}"
                recordError(errorMsg, e)
                Log.e(TAG, errorMsg, e)
                false
            }
        }
    }
    
    /**
     * Switch to dictation recognition mode
     */
    fun switchToDictationMode(): Boolean {
        if (currentMode == SpeechMode.DICTATION) {
            return true // Already in dictation mode
        }
        
        return synchronized(recognizerLock) {
            try {
                Log.d(TAG, "Switching to dictation mode...")
                
                val wasListening = serviceState.isListening()
                if (wasListening) {
                    stopListening()
                }
                
                val dictRecognizer = dictationRecognizer
                if (dictRecognizer != null) {
                    currentRecognizer = dictRecognizer
                    currentMode = SpeechMode.DICTATION
                    lastModeSwitch = System.currentTimeMillis()
                    modeSwitchCount++
                    
                    // Recreate speech service with new recognizer
                    speechService = SpeechService(dictRecognizer, SAMPLE_RATE)
                    
                    // Resume listening if we were listening before
                    if (wasListening && isInitialized) {
                        startListening(null) // Will use current listener
                    }
                    
                    Log.d(TAG, "Successfully switched to dictation mode")
                    true
                } else {
                    val errorMsg = "Dictation recognizer not available"
                    recordError(errorMsg, null)
                    Log.e(TAG, errorMsg)
                    false
                }
                
            } catch (e: Exception) {
                val errorMsg = "Failed to switch to dictation mode: ${e.message}"
                recordError(errorMsg, e)
                Log.e(TAG, errorMsg, e)
                false
            }
        }
    }
    
    /**
     * Start listening with current recognizer
     */
    fun startListening(listener: org.vosk.android.RecognitionListener?): Boolean {
        return try {
            val service = speechService
            val recognizer = currentRecognizer
            
            if (service != null && recognizer != null && listener != null) {
                service.startListening(listener)
                Log.d(TAG, "Started listening in ${currentMode} mode")
                true
            } else {
                val errorMsg = "Cannot start listening - service: ${service != null}, recognizer: ${recognizer != null}, listener: ${listener != null}"
                recordError(errorMsg, null)
                Log.e(TAG, errorMsg)
                false
            }
        } catch (e: Exception) {
            val errorMsg = "Exception starting listening: ${e.message}"
            recordError(errorMsg, e)
            Log.e(TAG, errorMsg, e)
            false
        }
    }
    
    /**
     * Stop listening
     */
    fun stopListening(): Boolean {
        return try {
            speechService?.stop()
            Log.d(TAG, "Stopped listening")
            true
        } catch (e: Exception) {
            val errorMsg = "Exception stopping listening: ${e.message}"
            recordError(errorMsg, e)
            Log.e(TAG, errorMsg, e)
            false
        }
    }
    
    /**
     * Rebuild command recognizer with new grammar
     */
    fun rebuildCommandRecognizer(model: Model, grammarJson: String?): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "Cannot rebuild - recognizers not initialized")
            return false
        }
        
        return synchronized(recognizerLock) {
            try {
                Log.d(TAG, "Rebuilding command recognizer...")
                
                val wasListening = serviceState.isListening()
                val wasInCommandMode = currentMode == SpeechMode.DYNAMIC_COMMAND
                
                if (wasListening) {
                    stopListening()
                }
                
                // Create new command recognizer
                val oldRecognizer = commandRecognizer
                commandRecognizer = createCommandRecognizer(model, grammarJson)
                safeCloseRecognizer(oldRecognizer)
                
                // Update current recognizer if we were in command mode
                if (wasInCommandMode) {
                    currentRecognizer = commandRecognizer
                    val rec = commandRecognizer ?: throw IllegalStateException("Command recognizer is null after creation")
                    speechService = SpeechService(rec, SAMPLE_RATE)
                }
                
                // Resume listening if we were listening before
                if (wasListening && isInitialized) {
                    startListening(null) // Will use current listener
                }
                
                Log.d(TAG, "Command recognizer rebuilt successfully")
                true
                
            } catch (e: Exception) {
                val errorMsg = "Failed to rebuild command recognizer: ${e.message}"
                recordError(errorMsg, e)
                Log.e(TAG, errorMsg, e)
                false
            }
        }
    }
    
    /**
     * Try fallback to single recognizer mode
     */
    private fun tryFallbackMode(model: Model): Boolean {
        return try {
            Log.w(TAG, "Attempting fallback to single recognizer mode...")
            
            val fallbackRecognizer = Recognizer(model, SAMPLE_RATE)
            currentRecognizer = fallbackRecognizer
            commandRecognizer = fallbackRecognizer
            dictationRecognizer = null // No separate dictation recognizer
            
            speechService = SpeechService(fallbackRecognizer, SAMPLE_RATE)
            currentMode = SpeechMode.DYNAMIC_COMMAND
            isInitialized = true
            
            Log.w(TAG, "Fallback to single recognizer successful")
            true
            
        } catch (e: Exception) {
            val errorMsg = "Fallback mode also failed: ${e.message}"
            recordError(errorMsg, e)
            Log.e(TAG, errorMsg, e)
            false
        }
    }
    
    /**
     * Safe recognizer cleanup
     */
    private fun safeCloseRecognizer(recognizer: Recognizer?) {
        try {
            recognizer?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing recognizer: ${e.message}")
        }
    }
    
    /**
     * Safe speech service cleanup
     */
    private fun safeSpeechServiceCleanup() {
        try {
            speechService?.let { service ->
                service.stop()
                service.shutdown()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up speech service: ${e.message}")
        }
    }
    
    /**
     * Record error for diagnostics
     */
    private fun recordError(message: String, exception: Exception?) {
        lastError = message
        val error = RecognizerError(
            message = message,
            exception = exception?.javaClass?.simpleName,
            timestamp = System.currentTimeMillis(),
            mode = currentMode.name
        )
        errorHistory.add(error)
        
        // Keep only last 20 errors
        if (errorHistory.size > 20) {
            errorHistory.removeAt(0)
        }
    }
    
    /**
     * Clean up all resources
     */
    fun cleanup() {
        synchronized(recognizerLock) {
            try {
                Log.d(TAG, "Cleaning up recognizer resources...")

                // Stop speech service
                safeSpeechServiceCleanup()
                speechService = null

                // Release VoiceIsolation resources
                voiceIsolation?.release()
                isVoiceIsolationInitialized = false

                // Close recognizers
                safeCloseRecognizer(commandRecognizer)
                safeCloseRecognizer(dictationRecognizer)

                // Reset state
                commandRecognizer = null
                dictationRecognizer = null
                currentRecognizer = null
                isInitialized = false

                Log.d(TAG, "Recognizer cleanup completed")

            } catch (e: Exception) {
                Log.e(TAG, "Error during recognizer cleanup: ${e.message}")
            }
        }
    }
    
    // Getters for state and information
    fun getCurrentMode(): SpeechMode = currentMode
    fun isInitialized(): Boolean = isInitialized
    fun getCurrentRecognizer(): Recognizer? = currentRecognizer
    fun getCommandRecognizer(): Recognizer? = commandRecognizer
    fun getDictationRecognizer(): Recognizer? = dictationRecognizer
    fun getSpeechService(): SpeechService? = speechService
    fun getLastError(): String? = lastError
    fun getModeSwitchCount(): Int = modeSwitchCount
    fun getLastModeSwitchTime(): Long = lastModeSwitch
    fun hasError(): Boolean = lastError != null
    
    /**
     * Get recognizer status information
     */
    fun getStatus(): RecognizerStatus {
        return RecognizerStatus(
            isInitialized = isInitialized,
            currentMode = currentMode,
            hasCommandRecognizer = commandRecognizer != null,
            hasDictationRecognizer = dictationRecognizer != null,
            hasSpeechService = speechService != null,
            modeSwitchCount = modeSwitchCount,
            lastModeSwitchTime = lastModeSwitch,
            lastError = lastError
        )
    }
    
    /**
     * Initialize VoiceIsolation with the audio session ID.
     * Call this before starting recognition to enable hardware-level audio processing.
     *
     * Note: Vosk's SpeechService handles audio internally, so this should be called
     * with the audio session ID from the app's audio manager if available.
     *
     * @param audioSessionId The audio session ID from the audio system
     * @return true if initialization succeeded
     */
    fun initializeVoiceIsolation(audioSessionId: Int): Boolean {
        return try {
            voiceIsolation?.let { isolation ->
                val success = isolation.initialize(audioSessionId)
                isVoiceIsolationInitialized = success
                if (success) {
                    Log.d(TAG, "VoiceIsolation initialized with audio session $audioSessionId")
                } else {
                    Log.w(TAG, "VoiceIsolation initialization failed")
                }
                success
            } ?: run {
                Log.d(TAG, "VoiceIsolation not provided - skipping initialization")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VoiceIsolation", e)
            false
        }
    }

    /**
     * Check if VoiceIsolation is active and processing audio.
     */
    fun isVoiceIsolationActive(): Boolean {
        return voiceIsolation?.isEnabled() == true && isVoiceIsolationInitialized
    }

    /**
     * Get diagnostic information
     */
    fun getDiagnostics(): Map<String, Any> {
        return mapOf(
            "initialized" to isInitialized,
            "currentMode" to currentMode.name,
            "hasCommandRecognizer" to (commandRecognizer != null),
            "hasDictationRecognizer" to (dictationRecognizer != null),
            "hasCurrentRecognizer" to (currentRecognizer != null),
            "hasSpeechService" to (speechService != null),
            "modeSwitchCount" to modeSwitchCount,
            "lastModeSwitchTime" to lastModeSwitch,
            "timeSinceLastSwitch" to (System.currentTimeMillis() - lastModeSwitch),
            "errorCount" to errorHistory.size,
            "lastError" to (lastError ?: "none"),
            "grammarConstraintsEnabled" to config.isGrammarConstraintsEnabled(),
            "voiceIsolationEnabled" to (voiceIsolation?.isEnabled() == true),
            "voiceIsolationActive" to isVoiceIsolationActive()
        )
    }
    
    /**
     * Get error history
     */
    fun getErrorHistory(): List<RecognizerError> = errorHistory.toList()
    
    /**
     * Data class for recognizer status
     */
    data class RecognizerStatus(
        val isInitialized: Boolean,
        val currentMode: SpeechMode,
        val hasCommandRecognizer: Boolean,
        val hasDictationRecognizer: Boolean,
        val hasSpeechService: Boolean,
        val modeSwitchCount: Int,
        val lastModeSwitchTime: Long,
        val lastError: String?
    )
    
    /**
     * Data class for error tracking
     */
    data class RecognizerError(
        val message: String,
        val exception: String?,
        val timestamp: Long,
        val mode: String
    )
}