/**
 * VoiceRecognitionBinder.kt - Manages binding to VoiceRecognitionService
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-08-28
 */
package com.augmentalis.voiceoscore.accessibility.recognition

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
import com.augmentalis.voicerecognition.IRecognitionCallback
import com.augmentalis.voicerecognition.IVoiceRecognitionService
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages binding to VoiceRecognitionService and routes commands to ActionCoordinator
 * Direct implementation with robust service lifecycle management
 */
class VoiceRecognitionBinder(
    private val actionCoordinator: ActionCoordinator
) {
    
    companion object {
        private const val TAG = "VoiceRecognitionBinder"
        private const val SERVICE_PACKAGE = "com.augmentalis.voicerecognition"
        private const val SERVICE_CLASS = "com.augmentalis.voicerecognition.service.VoiceRecognitionService"
        private const val MAX_RECONNECTION_ATTEMPTS = 5
        private const val RECONNECTION_DELAY_MS = 1000L
        private const val COMMAND_TIMEOUT_MS = 5000L
        
        // Recognition states
        private const val STATE_IDLE = 0
        private const val STATE_LISTENING = 1
        private const val STATE_PROCESSING = 2
        private const val STATE_ERROR = 3
    }
    
    // Service binding state
    private var recognitionService: IVoiceRecognitionService? = null
    private val isConnected = AtomicBoolean(false)
    private val isBinding = AtomicBoolean(false)
    private val reconnectionAttempts = AtomicInteger(0)
    
    // Context and lifecycle
    private var context: Context? = null
    private var isDisposed = AtomicBoolean(false)
    
    // Command queue for offline operations
    private val pendingCommands = ConcurrentLinkedQueue<PendingCommand>()
    
    // Coroutine scope for async operations
    private val binderScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob()
    )
    
    // Current recognition state
    private var currentState = AtomicInteger(STATE_IDLE)
    private var lastEngine: String? = null
    private var lastLanguage: String? = null
    
    data class PendingCommand(
        val engine: String,
        val language: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = 
            System.currentTimeMillis() - timestamp > COMMAND_TIMEOUT_MS
    }
    
    /**
     * Recognition callback implementation
     */
    private val recognitionCallback = object : IRecognitionCallback.Stub() {
        
        override fun onRecognitionResult(text: String?, confidence: Float, isFinal: Boolean) {
            if (text.isNullOrBlank() || isDisposed.get()) return
            
            Log.d(TAG, "Recognition result: '$text', confidence: $confidence, final: $isFinal")
            
            try {
                if (isFinal) {
                    // Route final results to ActionCoordinator
                    processRecognizedCommand(text)
                } else {
                    // Handle partial results (optional UI feedback)
                    onPartialResult(text)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing recognition result", e)
            }
        }
        
        override fun onError(errorCode: Int, message: String?) {
            Log.e(TAG, "Recognition error: code=$errorCode, message=$message")
            
            currentState.set(STATE_ERROR)
            
            // Notify user if needed based on error severity
            when (errorCode) {
                android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                android.speech.SpeechRecognizer.ERROR_NETWORK,
                android.speech.SpeechRecognizer.ERROR_SERVER -> {
                    // Network errors - could retry
                    Log.w(TAG, "Network error in recognition, may retry")
                }
                android.speech.SpeechRecognizer.ERROR_NO_MATCH -> {
                    // No speech detected - normal condition
                    Log.d(TAG, "No speech detected")
                }
                else -> {
                    // Other errors - log for debugging
                    Log.w(TAG, "Recognition error: $errorCode - $message")
                }
            }
        }
        
        override fun onStateChanged(state: Int, message: String?) {
            Log.d(TAG, "State changed: $state, message: $message")
            currentState.set(state)
            
            when (state) {
                STATE_IDLE -> Log.d(TAG, "Recognition idle")
                STATE_LISTENING -> Log.d(TAG, "Recognition listening")
                STATE_PROCESSING -> Log.d(TAG, "Recognition processing")
                STATE_ERROR -> Log.w(TAG, "Recognition error state: $message")
            }
        }
        
        override fun onPartialResult(partialText: String?) {
            if (partialText.isNullOrBlank() || isDisposed.get()) return
            
            Log.v(TAG, "Partial result: '$partialText'")
            
            // Optional: Display partial text in UI
            // This could be used for live feedback to user
            // Implementation depends on UI requirements
        }
    }
    
    /**
     * Service connection implementation
     */
    private val serviceConnection = object : ServiceConnection {
        
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected: $name")
            
            try {
                recognitionService = IVoiceRecognitionService.Stub.asInterface(service)
                isConnected.set(true)
                isBinding.set(false)
                reconnectionAttempts.set(0)
                
                // Register our callback
                recognitionService?.registerCallback(recognitionCallback)
                
                // Process any pending commands
                processPendingCommands()
                
                Log.i(TAG, "VoiceRecognitionService bound successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to service", e)
                isConnected.set(false)
                isBinding.set(false)
                scheduleReconnection()
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "Service disconnected: $name")
            
            recognitionService = null
            isConnected.set(false)
            currentState.set(STATE_ERROR)
            
            // Schedule reconnection if not manually disconnected
            if (!isDisposed.get()) {
                scheduleReconnection()
            }
        }
        
        override fun onBindingDied(name: ComponentName?) {
            Log.e(TAG, "Service binding died: $name")
            
            recognitionService = null
            isConnected.set(false)
            currentState.set(STATE_ERROR)
            
            // Immediate reconnection attempt for binding death
            if (!isDisposed.get()) {
                scheduleReconnection()
            }
        }
        
        override fun onNullBinding(name: ComponentName?) {
            Log.e(TAG, "Null binding for service: $name")
            isBinding.set(false)
            scheduleReconnection()
        }
    }
    
    /**
     * Connect to the VoiceRecognitionService
     */
    fun connect(context: Context): Boolean {
        if (isDisposed.get()) {
            Log.w(TAG, "Cannot connect - binder is disposed")
            return false
        }
        
        if (isConnected.get() || isBinding.get()) {
            Log.d(TAG, "Already connected or connecting")
            return isConnected.get()
        }
        
        this.context = context
        
        Log.d(TAG, "Connecting to VoiceRecognitionService")
        
        return try {
            val intent = Intent().apply {
                setClassName(SERVICE_PACKAGE, SERVICE_CLASS)
            }
            
            isBinding.set(true)
            val bound = context.bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
            )
            
            if (!bound) {
                Log.e(TAG, "Failed to bind to VoiceRecognitionService")
                isBinding.set(false)
            }
            
            bound
        } catch (e: Exception) {
            Log.e(TAG, "Error binding to service", e)
            isBinding.set(false)
            false
        }
    }
    
    /**
     * Disconnect from the VoiceRecognitionService
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from VoiceRecognitionService")
        
        try {
            // Stop any active recognition
            stopListening()
            
            // Unregister callback
            recognitionService?.unregisterCallback(recognitionCallback)
            
            // Unbind service
            context?.unbindService(serviceConnection)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            recognitionService = null
            isConnected.set(false)
            isBinding.set(false)
            context = null
            currentState.set(STATE_IDLE)
        }
    }
    
    /**
     * Start voice recognition
     */
    fun startListening(engine: String, language: String): Boolean {
        if (isDisposed.get()) {
            Log.w(TAG, "Cannot start listening - binder is disposed")
            return false
        }
        
        lastEngine = engine
        lastLanguage = language
        
        if (!isConnected.get()) {
            Log.w(TAG, "Service not connected - queuing command")
            pendingCommands.offer(PendingCommand(engine, language))
            return false
        }
        
        return try {
            Log.d(TAG, "Starting recognition with engine: $engine, language: $language")
            
            val result = recognitionService?.startRecognition(engine, language, 0) ?: false
            
            if (result) {
                currentState.set(STATE_LISTENING)
                Log.i(TAG, "Recognition started successfully")
            } else {
                Log.w(TAG, "Failed to start recognition")
            }
            
            result
        } catch (e: RemoteException) {
            Log.e(TAG, "Error starting recognition", e)
            handleServiceError()
            false
        }
    }
    
    /**
     * Stop voice recognition
     */
    fun stopListening(): Boolean {
        if (!isConnected.get()) {
            Log.w(TAG, "Service not connected - cannot stop listening")
            return false
        }
        
        return try {
            Log.d(TAG, "Stopping recognition")
            
            val result = recognitionService?.stopRecognition() ?: false
            
            if (result) {
                currentState.set(STATE_IDLE)
                Log.d(TAG, "Recognition stopped successfully")
            } else {
                Log.w(TAG, "Failed to stop recognition")
            }
            
            result
        } catch (e: RemoteException) {
            Log.e(TAG, "Error stopping recognition", e)
            handleServiceError()
            false
        }
    }
    
    /**
     * Check if service is connected
     */
    fun isConnected(): Boolean = isConnected.get()
    
    /**
     * Check if currently recognizing
     */
    fun isRecognizing(): Boolean {
        return try {
            recognitionService?.isRecognizing() ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "Error checking recognition state", e)
            handleServiceError()
            false
        }
    }
    
    /**
     * Get current recognition state
     */
    fun getCurrentState(): Int = currentState.get()
    
    /**
     * Get available engines
     */
    fun getAvailableEngines(): List<String> {
        if (!isConnected.get()) {
            Log.w(TAG, "Service not connected - cannot get engines")
            return emptyList()
        }
        
        return try {
            recognitionService?.availableEngines ?: emptyList()
        } catch (e: RemoteException) {
            Log.e(TAG, "Error getting available engines", e)
            handleServiceError()
            emptyList()
        }
    }
    
    /**
     * Get service status
     */
    fun getServiceStatus(): String {
        if (!isConnected.get()) {
            return "Service not connected"
        }
        
        return try {
            recognitionService?.status ?: "Unknown"
        } catch (e: RemoteException) {
            Log.e(TAG, "Error getting service status", e)
            handleServiceError()
            "Error getting status"
        }
    }
    
    /**
     * Process recognized command by routing to ActionCoordinator
     */
    private fun processRecognizedCommand(command: String) {
        Log.d(TAG, "Processing recognized command: '$command'")
        
        binderScope.launch {
            try {
                // Route to ActionCoordinator for processing
                val handled = actionCoordinator.processCommand(command)
                
                if (handled) {
                    Log.d(TAG, "Command handled successfully: '$command'")
                } else {
                    Log.w(TAG, "Command not handled: '$command'")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing command: '$command'", e)
            }
        }
    }
    
    /**
     * Process pending commands after reconnection
     */
    private fun processPendingCommands() {
        Log.d(TAG, "Processing ${pendingCommands.size} pending commands")
        
        while (pendingCommands.isNotEmpty()) {
            val command = pendingCommands.poll()
            if (command != null && !command.isExpired()) {
                startListening(command.engine, command.language)
                break // Process one at a time
            }
        }
        
        // Clear any expired commands
        val iterator = pendingCommands.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().isExpired()) {
                iterator.remove()
            }
        }
    }
    
    /**
     * Schedule reconnection attempt
     */
    private fun scheduleReconnection() {
        val attempts = reconnectionAttempts.incrementAndGet()
        if (attempts > MAX_RECONNECTION_ATTEMPTS) {
            Log.e(TAG, "Max reconnection attempts reached, giving up")
            return
        }
        
        Log.w(TAG, "Scheduling reconnection attempt $attempts/$MAX_RECONNECTION_ATTEMPTS")
        
        binderScope.launch {
            delay(RECONNECTION_DELAY_MS * attempts) // Exponential backoff
            
            if (!isDisposed.get() && !isConnected.get()) {
                context?.let { ctx ->
                    Log.d(TAG, "Attempting reconnection...")
                    connect(ctx)
                }
            }
        }
    }
    
    /**
     * Handle service errors and attempt recovery
     */
    private fun handleServiceError() {
        Log.w(TAG, "Handling service error")
        
        recognitionService = null
        isConnected.set(false)
        currentState.set(STATE_ERROR)
        
        if (!isDisposed.get()) {
            scheduleReconnection()
        }
    }
    
    /**
     * Dispose the binder and clean up resources
     */
    fun dispose() {
        Log.d(TAG, "Disposing VoiceRecognitionBinder")
        
        isDisposed.set(true)
        
        try {
            disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error during dispose", e)
        }
        
        // Cancel all pending operations
        binderScope.cancel()
        
        // Clear pending commands
        pendingCommands.clear()
        
        Log.d(TAG, "VoiceRecognitionBinder disposed")
    }
    
    /**
     * Get debug information
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("VoiceRecognitionBinder Debug Info")
            appendLine("Connected: ${isConnected.get()}")
            appendLine("Binding: ${isBinding.get()}")
            appendLine("Current State: ${getCurrentStateString()}")
            appendLine("Reconnection Attempts: ${reconnectionAttempts.get()}/$MAX_RECONNECTION_ATTEMPTS")
            appendLine("Pending Commands: ${pendingCommands.size}")
            appendLine("Last Engine: $lastEngine")
            appendLine("Last Language: $lastLanguage")
            appendLine("Service Status: ${getServiceStatus()}")
            if (isConnected.get()) {
                appendLine("Available Engines: ${getAvailableEngines().joinToString(", ")}")
            }
        }
    }
    
    private fun getCurrentStateString(): String {
        return when (currentState.get()) {
            STATE_IDLE -> "IDLE"
            STATE_LISTENING -> "LISTENING" 
            STATE_PROCESSING -> "PROCESSING"
            STATE_ERROR -> "ERROR"
            else -> "UNKNOWN"
        }
    }
}