/**
 * VoskState.kt - State management and mode switching for VOSK engine
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-03
 * 
 * SOLID Principle: Single Responsibility
 * - Manages VOSK-specific state transitions and mode switching
 * - Handles dictation mode timing and silence detection
 * - Provides state validation and recovery mechanisms
 */
package com.augmentalis.voiceos.speech.engines.vosk

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceos.speech.engines.common.ServiceState
import kotlinx.coroutines.*

/**
 * State manager for VOSK engine.
 * Handles VOSK-specific state transitions, mode switching,
 * and advanced features like dictation silence detection.
 */
class VoskState(
    private val serviceState: ServiceState,
    private val config: VoskConfig
) {
    
    companion object {
        private const val TAG = "VoskState"
        private const val SILENCE_CHECK_INTERVAL = 500L // milliseconds
        private const val DICTATION_SILENCE_TIMEOUT = 2000L // 2 seconds
        private const val DEFAULT_TIMEOUT_MINUTES = 30L
    }
    
    // VOSK-specific state flags
    @Volatile
    private var isDictationActive = false
    @Volatile
    private var isAvaVoiceEnabled = false
    @Volatile
    private var isAvaVoiceSleeping = false
    @Volatile
    private var isServiceInitialized = false
    @Volatile
    private var isInitiallyConfigured = false
    
    // Mode and timing
    private var currentMode = SpeechMode.DYNAMIC_COMMAND
    private var lastExecutedCommandTime = System.currentTimeMillis()
    private var lastModeSwitch = System.currentTimeMillis()
    private var modeSwitchCount = 0
    
    // Silence detection for dictation
    private var silenceStartTime = 0L
    private val silenceCheckHandler = Handler(Looper.getMainLooper())
    private val silenceCheckRunnable = object : Runnable {
        override fun run() {
            handleSilenceCheck()
        }
    }
    
    // Timeout management
    private val stateScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timeoutJob: Job? = null
    
    // State history for diagnostics
    private val stateHistory = mutableListOf<StateTransition>()
    private var stateTransitionCount = 0L
    
    /**
     * Initialize state manager
     */
    fun initialize(): Boolean {
        return try {
            Log.i(TAG, "Initializing VOSK state manager...")
            
            // Reset all flags to initial state
            resetToInitialState()
            
            // Set up initial service state
            serviceState.setState(ServiceState.State.UNINITIALIZED)
            
            Log.i(TAG, "VOSK state manager initialized successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize state manager: ${e.message}", e)
            false
        }
    }
    
    /**
     * Switch to dictation mode with silence monitoring
     */
    fun switchToDictationMode(): Boolean {
        return try {
            if (isDictationActive) {
                Log.d(TAG, "Already in dictation mode")
                return true
            }
            
            Log.d(TAG, "Switching to dictation mode...")
            
            // Update state
            isDictationActive = true
            currentMode = SpeechMode.DICTATION
            silenceStartTime = 0L
            lastModeSwitch = System.currentTimeMillis()
            modeSwitchCount++
            
            // Start silence monitoring
            startSilenceMonitoring()
            
            // Update service state
            serviceState.setState(ServiceState.State.FREE_SPEECH, "Dictation mode active")
            
            recordStateTransition("SWITCH_TO_DICTATION", null)
            Log.d(TAG, "Successfully switched to dictation mode")
            true
            
        } catch (e: Exception) {
            val errorMsg = "Failed to switch to dictation mode: ${e.message}"
            recordStateTransition("SWITCH_TO_DICTATION_FAILED", errorMsg)
            Log.e(TAG, errorMsg, e)
            false
        }
    }
    
    /**
     * Switch to command mode
     */
    fun switchToCommandMode(): Boolean {
        return try {
            if (!isDictationActive && currentMode == SpeechMode.DYNAMIC_COMMAND) {
                Log.d(TAG, "Already in command mode")
                return true
            }
            
            Log.d(TAG, "Switching to command mode...")
            
            // Stop silence monitoring
            stopSilenceMonitoring()
            
            // Update state
            isDictationActive = false
            currentMode = SpeechMode.DYNAMIC_COMMAND
            lastModeSwitch = System.currentTimeMillis()
            modeSwitchCount++
            
            // Update service state
            serviceState.setState(ServiceState.State.READY, "Command mode active")
            
            recordStateTransition("SWITCH_TO_COMMAND", null)
            Log.d(TAG, "Successfully switched to command mode")
            true
            
        } catch (e: Exception) {
            val errorMsg = "Failed to switch to command mode: ${e.message}"
            recordStateTransition("SWITCH_TO_COMMAND_FAILED", errorMsg)
            Log.e(TAG, errorMsg, e)
            false
        }
    }
    
    /**
     * Handle service initialization completion
     */
    fun markServiceInitialized() {
        isServiceInitialized = true
        isInitiallyConfigured = true
        serviceState.setState(ServiceState.State.INITIALIZED, "Service initialized")
        recordStateTransition("SERVICE_INITIALIZED", null)
        Log.d(TAG, "Service marked as initialized")
    }
    
    /**
     * Handle voice activation/deactivation
     */
    fun setVoiceEnabled(enabled: Boolean) {
        if (isAvaVoiceEnabled != enabled) {
            isAvaVoiceEnabled = enabled
            lastExecutedCommandTime = System.currentTimeMillis()
            
            if (enabled) {
                isAvaVoiceSleeping = false
                serviceState.setState(ServiceState.State.READY, "Voice enabled")
                startTimeoutMonitoring()
                recordStateTransition("VOICE_ENABLED", null)
            } else {
                stopTimeoutMonitoring()
                serviceState.setState(ServiceState.State.PAUSED, "Voice disabled")
                recordStateTransition("VOICE_DISABLED", null)
            }
            
            Log.d(TAG, "Voice ${if (enabled) "enabled" else "disabled"}")
        }
    }
    
    /**
     * Put voice service to sleep
     */
    fun putVoiceToSleep() {
        if (!isAvaVoiceSleeping) {
            isAvaVoiceSleeping = true
            serviceState.setState(ServiceState.State.SLEEPING, "Voice sleeping due to timeout")
            recordStateTransition("VOICE_SLEEPING", null)
            Log.d(TAG, "Voice service put to sleep")
        }
    }
    
    /**
     * Wake up voice service
     */
    fun wakeUpVoice() {
        if (isAvaVoiceSleeping) {
            isAvaVoiceSleeping = false
            lastExecutedCommandTime = System.currentTimeMillis()
            serviceState.setState(ServiceState.State.READY, "Voice awakened")
            recordStateTransition("VOICE_AWAKENED", null)
            Log.d(TAG, "Voice service awakened")
        }
    }
    
    /**
     * Update command execution time (resets timeout)
     */
    fun updateCommandExecutionTime() {
        lastExecutedCommandTime = System.currentTimeMillis()
        
        // Wake up if sleeping
        if (isAvaVoiceSleeping) {
            wakeUpVoice()
        }
        
        Log.d(TAG, "Command execution time updated")
    }
    
    /**
     * Handle partial results for silence detection
     */
    fun handlePartialResult(hypothesis: String) {
        if (isDictationActive) {
            if (hypothesis.isBlank()) {
                if (silenceStartTime == 0L) {
                    silenceStartTime = System.currentTimeMillis()
                    Log.d(TAG, "Silence started in dictation mode")
                }
            } else {
                silenceStartTime = 0L
                Log.d(TAG, "Speech detected in dictation mode, resetting silence timer")
            }
        }
    }
    
    /**
     * Start silence monitoring for dictation mode
     */
    private fun startSilenceMonitoring() {
        silenceStartTime = 0L
        silenceCheckHandler.post(silenceCheckRunnable)
        Log.d(TAG, "Started silence monitoring for dictation mode")
    }
    
    /**
     * Stop silence monitoring
     */
    private fun stopSilenceMonitoring() {
        silenceCheckHandler.removeCallbacks(silenceCheckRunnable)
        silenceStartTime = 0L
        Log.d(TAG, "Stopped silence monitoring")
    }
    
    /**
     * Handle silence check for dictation timeout
     */
    private fun handleSilenceCheck() {
        if (isDictationActive) {
            val currentTime = System.currentTimeMillis()
            
            if (silenceStartTime > 0 && (currentTime - silenceStartTime >= DICTATION_SILENCE_TIMEOUT)) {
                Log.d(TAG, "Dictation silence timeout reached, switching to command mode")
                switchToCommandMode()
            } else {
                // Schedule next check
                silenceCheckHandler.postDelayed(silenceCheckRunnable, SILENCE_CHECK_INTERVAL)
            }
        }
    }
    
    /**
     * Start timeout monitoring for voice service
     */
    private fun startTimeoutMonitoring() {
        timeoutJob?.cancel()
        timeoutJob = stateScope.launch {
            val timeoutMinutes = config.getTimeoutMinutes()
            
            while (isAvaVoiceEnabled && !isAvaVoiceSleeping) {
                delay(30000L) // Check every 30 seconds
                
                val currentTime = System.currentTimeMillis()
                val difference = currentTime - lastExecutedCommandTime
                val differenceInMinutes = difference / (60 * 1000)
                
                if (differenceInMinutes >= timeoutMinutes) {
                    Log.d(TAG, "Voice timeout reached after $differenceInMinutes minutes")
                    putVoiceToSleep()
                    break
                }
            }
        }
    }
    
    /**
     * Stop timeout monitoring
     */
    private fun stopTimeoutMonitoring() {
        timeoutJob?.cancel()
        timeoutJob = null
    }
    
    /**
     * Check if unmute command was received
     */
    fun isUnmuteCommand(command: String): Boolean {
        val normalizedCommand = command.lowercase().trim()
        val unmuteCommands = listOf("unmute", "wake up", "hello", "hey")
        
        return isAvaVoiceSleeping && unmuteCommands.any { 
            normalizedCommand.equals(it, ignoreCase = true) 
        }
    }
    
    /**
     * Record state transition for diagnostics
     */
    private fun recordStateTransition(transition: String, error: String?) {
        stateTransitionCount++
        val stateTransition = StateTransition(
            transitionNumber = stateTransitionCount,
            transition = transition,
            fromMode = currentMode.name,
            timestamp = System.currentTimeMillis(),
            error = error
        )
        
        stateHistory.add(stateTransition)
        
        // Keep only last 50 transitions
        if (stateHistory.size > 50) {
            stateHistory.removeAt(0)
        }
    }
    
    /**
     * Reset to initial state
     */
    private fun resetToInitialState() {
        isDictationActive = false
        isAvaVoiceEnabled = false
        isAvaVoiceSleeping = false
        isServiceInitialized = false
        isInitiallyConfigured = false
        currentMode = SpeechMode.DYNAMIC_COMMAND
        lastExecutedCommandTime = System.currentTimeMillis()
        silenceStartTime = 0L
        modeSwitchCount = 0
        
        // Stop any ongoing operations
        stopSilenceMonitoring()
        stopTimeoutMonitoring()
    }
    
    /**
     * Clean up state resources
     */
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up state resources...")
            
            // Stop monitoring
            stopSilenceMonitoring()
            stopTimeoutMonitoring()
            
            // Cancel scope
            stateScope.coroutineContext.cancelChildren()
            
            // Reset state
            resetToInitialState()
            
            // Clear history
            stateHistory.clear()
            stateTransitionCount = 0L
            
            Log.d(TAG, "State cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during state cleanup: ${e.message}", e)
        }
    }
    
    // Getters for state information
    fun isDictationActive(): Boolean = isDictationActive
    fun isVoiceEnabled(): Boolean = isAvaVoiceEnabled
    fun isVoiceSleeping(): Boolean = isAvaVoiceSleeping
    fun isServiceInitialized(): Boolean = isServiceInitialized
    fun isInitiallyConfigured(): Boolean = isInitiallyConfigured
    fun getCurrentMode(): SpeechMode = currentMode
    fun getLastExecutionTime(): Long = lastExecutedCommandTime
    fun getLastModeSwitchTime(): Long = lastModeSwitch
    fun getModeSwitchCount(): Int = modeSwitchCount
    fun getSilenceStartTime(): Long = silenceStartTime
    
    /**
     * Get current state summary
     */
    fun getStateSummary(): StateSummary {
        return StateSummary(
            currentMode = currentMode,
            isDictationActive = isDictationActive,
            isVoiceEnabled = isAvaVoiceEnabled,
            isVoiceSleeping = isAvaVoiceSleeping,
            isServiceInitialized = isServiceInitialized,
            lastExecutionTime = lastExecutedCommandTime,
            lastModeSwitchTime = lastModeSwitch,
            modeSwitchCount = modeSwitchCount,
            silenceStartTime = silenceStartTime
        )
    }
    
    /**
     * Get diagnostic information
     */
    fun getDiagnostics(): Map<String, Any> {
        return mapOf(
            "currentMode" to currentMode.name,
            "isDictationActive" to isDictationActive,
            "isVoiceEnabled" to isAvaVoiceEnabled,
            "isVoiceSleeping" to isAvaVoiceSleeping,
            "isServiceInitialized" to isServiceInitialized,
            "isInitiallyConfigured" to isInitiallyConfigured,
            "lastExecutionTime" to lastExecutedCommandTime,
            "timeSinceLastExecution" to (System.currentTimeMillis() - lastExecutedCommandTime),
            "lastModeSwitchTime" to lastModeSwitch,
            "timeSinceLastModeSwitch" to (System.currentTimeMillis() - lastModeSwitch),
            "modeSwitchCount" to modeSwitchCount,
            "silenceStartTime" to silenceStartTime,
            "stateTransitionCount" to stateTransitionCount,
            "historySize" to stateHistory.size
        )
    }
    
    /**
     * Get state history
     */
    fun getStateHistory(): List<StateTransition> = stateHistory.toList()
    
    /**
     * Data class for state summary
     */
    data class StateSummary(
        val currentMode: SpeechMode,
        val isDictationActive: Boolean,
        val isVoiceEnabled: Boolean,
        val isVoiceSleeping: Boolean,
        val isServiceInitialized: Boolean,
        val lastExecutionTime: Long,
        val lastModeSwitchTime: Long,
        val modeSwitchCount: Int,
        val silenceStartTime: Long
    )
    
    /**
     * Data class for state transition tracking
     */
    data class StateTransition(
        val transitionNumber: Long,
        val transition: String,
        val fromMode: String,
        val timestamp: Long,
        val error: String?
    )
}