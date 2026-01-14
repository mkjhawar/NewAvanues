/**
 * VoiceRecognitionManagerIPC.kt - Integration manager for voice recognition (IPC-based)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-28
 */
package com.augmentalis.voiceoscore.accessibility.recognition

import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
import kotlinx.coroutines.*

/**
 * IPC-based manager that integrates VoiceRecognitionBinder with the VoiceAccessibility service
 * Example usage for voice recognition integration via inter-process communication
 */
class VoiceRecognitionManagerIPC(
    private val actionCoordinator: ActionCoordinator
) {
    
    companion object {
        private const val TAG = "VoiceRecognitionManagerIPC"
        private const val DEFAULT_ENGINE = "google"
        private const val DEFAULT_LANGUAGE = "en-US"
        private const val CONNECTION_TIMEOUT_MS = 10000L
    }
    
    private var voiceRecognitionBinder: VoiceRecognitionBinder? = null
    private var isInitialized = false
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * Initialize voice recognition integration
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return
        }

        Log.d(TAG, "Initializing VoiceRecognitionManagerIPC")
        
        try {
            // Create the binder with ActionCoordinator
            voiceRecognitionBinder = VoiceRecognitionBinder(actionCoordinator)
            
            // Connect to the service
            managerScope.launch {
                connectToService(context)
            }
            
            isInitialized = true
            Log.i(TAG, "VoiceRecognitionManagerIPC initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing VoiceRecognitionManagerIPC", e)
        }
    }
    
    /**
     * Connect to the voice recognition service
     */
    private suspend fun connectToService(context: Context) {
        Log.d(TAG, "Connecting to VoiceRecognitionService")
        
        val binder = voiceRecognitionBinder ?: return
        
        try {
            val connected = withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
                // Attempt connection in background
                withContext(Dispatchers.IO) {
                    binder.connect(context)
                }
            }
            
            if (connected == true) {
                Log.i(TAG, "Connected to VoiceRecognitionService")
                
                // Wait a bit for service to be fully ready
                delay(500)
                
                // Log available engines
                val engines = binder.getAvailableEngines()
                Log.d(TAG, "Available engines: ${engines.joinToString(", ")}")
                
            } else {
                Log.w(TAG, "Failed to connect to VoiceRecognitionService within timeout")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to service", e)
        }
    }
    
    /**
     * Start voice recognition with default settings
     */
    fun startListening(): Boolean {
        return startListening(DEFAULT_ENGINE, DEFAULT_LANGUAGE)
    }
    
    /**
     * Start voice recognition with specific engine and language
     */
    fun startListening(engine: String, language: String): Boolean {
        val binder = voiceRecognitionBinder
        if (binder == null) {
            Log.w(TAG, "Voice recognition not initialized")
            return false
        }
        
        if (!binder.isConnected()) {
            Log.w(TAG, "Service not connected")
            return false
        }
        
        Log.d(TAG, "Starting voice recognition: engine=$engine, language=$language")
        
        return try {
            binder.startListening(engine, language)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recognition", e)
            false
        }
    }
    
    /**
     * Stop voice recognition
     */
    fun stopListening(): Boolean {
        val binder = voiceRecognitionBinder ?: return false
        
        Log.d(TAG, "Stopping voice recognition")
        
        return try {
            binder.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping voice recognition", e)
            false
        }
    }
    
    /**
     * Check if voice recognition is active
     */
    fun isListening(): Boolean {
        return voiceRecognitionBinder?.isRecognizing() ?: false
    }
    
    /**
     * Check if service is connected
     */
    fun isServiceConnected(): Boolean {
        return voiceRecognitionBinder?.isConnected() ?: false
    }
    
    /**
     * Get current recognition state
     */
    fun getCurrentState(): String {
        val binder = voiceRecognitionBinder ?: return "Not initialized"

        return when (binder.getCurrentState()) {
            0 -> "IDLE"           // STATE_IDLE
            1 -> "LISTENING"      // STATE_LISTENING
            2 -> "PROCESSING"     // STATE_PROCESSING
            3 -> "ERROR"          // STATE_ERROR
            else -> "UNKNOWN"
        }
    }
    
    /**
     * Get available recognition engines
     */
    fun getAvailableEngines(): List<String> {
        return voiceRecognitionBinder?.getAvailableEngines() ?: emptyList()
    }
    
    /**
     * Get service status information
     */
    fun getServiceStatus(): String {
        return voiceRecognitionBinder?.getServiceStatus() ?: "Service not available"
    }
    
    /**
     * Get debug information
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("VoiceRecognitionManagerIPC Debug Info")
            appendLine("Initialized: $isInitialized")

            val binder = voiceRecognitionBinder
            if (binder != null) {
                appendLine("Binder Status:")
                append(binder.getDebugInfo().prependIndent("  "))
            } else {
                appendLine("Binder: Not created")
            }
        }
    }
    
    /**
     * Dispose and clean up resources
     */
    fun dispose() {
        Log.d(TAG, "Disposing VoiceRecognitionManagerIPC")

        try {
            voiceRecognitionBinder?.dispose()
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing binder", e)
        }

        voiceRecognitionBinder = null
        isInitialized = false

        managerScope.cancel()

        Log.d(TAG, "VoiceRecognitionManagerIPC disposed")
    }
}