/**
 * VoiceRecognitionManager.kt - Speech recognition lifecycle management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-17
 *
 * Responsibility: Manages speech recognition engine initialization, lifecycle, and command processing
 */
package com.augmentalis.voiceoscore.accessibility.recognition

import android.util.Log
import com.augmentalis.speechrecognition.SpeechEngine
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceoscore.accessibility.speech.SpeechConfigurationData
import com.augmentalis.voiceoscore.accessibility.speech.SpeechEngineManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages voice recognition initialization, state, and command event processing
 */
class VoiceRecognitionManager(
    private val speechEngineManager: SpeechEngineManager,
    private val onCommandReceived: (command: String, confidence: Float) -> Unit
) {
    companion object {
        private const val TAG = "VoiceRecognitionManager"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Volatile
    private var isInitialized = false

    /**
     * Initialize speech recognition engine and start monitoring
     */
    fun initialize() {
        Log.i(TAG, "Initializing voice recognition...")

        // Start engine initialization asynchronously with Vivoka
        // TODO: Make engine selection user-configurable (Vivoka/Vosk)
        speechEngineManager.initializeEngine(SpeechEngine.VIVOKA)

        // ARCHITECTURE: Split into two separate collectors
        // 1. State collection: Monitor engine lifecycle (initialization, listening status)
        // 2. Command event collection: Process voice commands as discrete events

        scope.launch {
            // Collector 1: Monitor engine state for lifecycle management
            launch {
                speechEngineManager.speechState.collectLatest { state ->
                    Log.d(TAG, "Engine state = $state")

                    // Only start listening when engine is fully initialized
                    if (state.isInitialized && !state.isListening && !isInitialized) {
                        isInitialized = true
                        delay(200) // Small delay to ensure engine is fully ready
                        startListening()
                    }
                }
            }

            // Collector 2: Process command events (guarantees every command is received)
            launch {
                speechEngineManager.commandEvents.collect { event ->
                    Log.i(TAG, "Command event received - command='${event.command}', confidence=${event.confidence}, timestamp=${event.timestamp}")

                    // Validate command before processing
                    if (event.confidence > 0.5f && event.command.isNotBlank()) {
                        Log.i(TAG, "Processing command: '${event.command}' (confidence=${event.confidence})")
                        onCommandReceived(event.command, event.confidence)
                    } else {
                        Log.d(TAG, "Command rejected: confidence too low (${event.confidence}) or empty command")
                    }
                }
            }
        }

        Log.i(TAG, "Voice recognition initialized successfully")
    }

    /**
     * Start listening for voice commands
     */
    fun startListening() {
        try {
            Log.d(TAG, "Starting voice recognition...")
            speechEngineManager.startListening()
            Log.i(TAG, "Voice recognition started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recognition", e)
        }
    }

    /**
     * Stop listening for voice commands
     */
    fun stopListening() {
        try {
            Log.d(TAG, "Stopping voice recognition...")
            speechEngineManager.stopListening()
            Log.i(TAG, "Voice recognition stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping voice recognition", e)
        }
    }

    /**
     * Update speech engine configuration
     */
    fun updateConfiguration(config: SpeechConfigurationData) {
        try {
            Log.d(TAG, "Updating speech configuration: language=${config.language}, mode=${config.mode}")
            speechEngineManager.updateConfiguration(config)
            Log.i(TAG, "Speech configuration updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating speech configuration", e)
        }
    }

    /**
     * Start voice recognition with specified language and recognizer type
     */
    fun startVoiceRecognition(language: String, recognizerType: String): Boolean {
        return try {
            Log.i(TAG, "startVoiceRecognition(language=$language, type=$recognizerType)")

            val mode = when (recognizerType.lowercase()) {
                "continuous" -> SpeechMode.DYNAMIC_COMMAND
                "command" -> SpeechMode.DYNAMIC_COMMAND
                "system" -> SpeechMode.DYNAMIC_COMMAND
                "static" -> SpeechMode.STATIC_COMMAND
                else -> {
                    Log.w(TAG, "Unknown recognizer type: $recognizerType, using DYNAMIC_COMMAND")
                    SpeechMode.DYNAMIC_COMMAND
                }
            }

            // Update speech configuration with new language
            updateConfiguration(
                SpeechConfigurationData(
                    language = language,
                    mode = mode,
                    enableVAD = true,
                    confidenceThreshold = 4000F,
                    maxRecordingDuration = 30000,
                    timeoutDuration = 5000,
                    enableProfanityFilter = false
                )
            )

            // Start listening
            startListening()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recognition", e)
            false
        }
    }

    /**
     * Stop currently active voice recognition
     */
    fun stopVoiceRecognition(): Boolean {
        return try {
            Log.i(TAG, "stopVoiceRecognition()")
            stopListening()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping voice recognition", e)
            false
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up VoiceRecognitionManager...")
            stopListening()
            scope.cancel()
            isInitialized = false
            Log.i(TAG, "VoiceRecognitionManager cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up VoiceRecognitionManager", e)
        }
    }
}
