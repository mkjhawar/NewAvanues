/**
 * VoiceRecognitionManager.kt - Integration manager for voice recognition
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
import com.augmentalis.voiceoscore.utils.ConditionalLogger
import kotlinx.coroutines.*

/**
 * Manager class that integrates VoiceRecognitionBinder with the VoiceAccessibility service
 * Example usage for voice recognition integration
 */
class VoiceRecognitionManager(
    private val actionCoordinator: ActionCoordinator
) {
    
    companion object {
        private const val TAG = "VoiceRecognitionManager"
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
            ConditionalLogger.w(TAG) { "Already initialized" }
            return
        }
        
        ConditionalLogger.d(TAG) { "Initializing VoiceRecognitionManager" }
        
        try {
            // Create the binder with ActionCoordinator
            voiceRecognitionBinder = VoiceRecognitionBinder(actionCoordinator)
            
            // Connect to the service
            managerScope.launch {
                connectToService(context)
            }
            
            isInitialized = true
            ConditionalLogger.i(TAG) { "VoiceRecognitionManager initialized successfully" }
            
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error initializing VoiceRecognitionManager" }
        }
    }
    
    /**
     * Connect to the voice recognition service
     */
    private suspend fun connectToService(context: Context) {
        ConditionalLogger.d(TAG) { "Connecting to VoiceRecognitionService" }
        
        val binder = voiceRecognitionBinder ?: return
        
        try {
            val connected = withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
                // Attempt connection in background
                withContext(Dispatchers.IO) {
                    binder.connect(context)
                }
            }
            
            if (connected == true) {
                ConditionalLogger.i(TAG) { "Connected to VoiceRecognitionService" }
                
                // Wait a bit for service to be fully ready
                delay(500)
                
                // Log available engines
                val engines = binder.getAvailableEngines()
                ConditionalLogger.d(TAG) { "Available engines: ${engines.joinToString(", ")}" }
                
            } else {
                ConditionalLogger.w(TAG) { "Failed to connect to VoiceRecognitionService within timeout" }
            }
            
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error connecting to service" }
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
            ConditionalLogger.w(TAG) { "Voice recognition not initialized" }
            return false
        }
        
        if (!binder.isConnected()) {
            ConditionalLogger.w(TAG) { "Service not connected" }
            return false
        }
        
        ConditionalLogger.d(TAG) { "Starting voice recognition: engine=$engine, language=$language" }
        
        return try {
            binder.startListening(engine, language)
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error starting voice recognition" }
            false
        }
    }
    
    /**
     * Stop voice recognition
     */
    fun stopListening(): Boolean {
        val binder = voiceRecognitionBinder ?: return false
        
        ConditionalLogger.d(TAG) { "Stopping voice recognition" }
        
        return try {
            binder.stopListening()
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error stopping voice recognition" }
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
            0 -> "IDLE"
            1 -> "LISTENING"
            2 -> "PROCESSING"
            3 -> "ERROR"
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
            appendLine("VoiceRecognitionManager Debug Info")
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
        ConditionalLogger.d(TAG) { "Disposing VoiceRecognitionManager" }
        
        try {
            voiceRecognitionBinder?.dispose()
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error disposing binder" }
        }
        
        voiceRecognitionBinder = null
        isInitialized = false
        
        managerScope.cancel()
        
        ConditionalLogger.d(TAG) { "VoiceRecognitionManager disposed" }
    }
}