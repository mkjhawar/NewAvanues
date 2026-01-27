/**
 * VivokaState.kt - State tracking and management for Vivoka VSDK engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 *
 * Extracted from monolithic VivokaEngine.kt as part of SOLID refactoring
 * Manages all state flags, transitions, and persistence for recovery
 */
package com.augmentalis.speechrecognition.vivoka

import android.util.Log
import com.augmentalis.speechrecognition.ServiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex

/**
 * Manages state for the Vivoka engine including flags, transitions, and persistence
 */
class VivokaState {

    companion object {
        private const val TAG = "VivokaState"
    }

    // Thread-safe state flags using @Volatile
    @Volatile private var isInitialized = false
    @Volatile private var isInitiallyConfigured = false
    @Volatile private var isListening = false
    @Volatile private var isDictationActive = false
    @Volatile private var isVoiceEnabled = false
    @Volatile private var isVoiceSleeping = false
    @Volatile private var isRecovering = false
    @Volatile private var isInGracefulDegradation = false

    // Timing state
    @Volatile private var lastExecutedCommandTime = System.currentTimeMillis()
    @Volatile private var silenceStartTime = 0L

    // Recovery state
    @Volatile private var retryCount = 0
    @Volatile private var lastMemoryCheck = System.currentTimeMillis()
    private var persistedState: Map<String, Any>? = null

    // Model state
    private var currentModelPath: String? = null
    private var recognizedText: String? = null

    // Service state management - simple enum value
    @Volatile private var currentServiceState: ServiceState = ServiceState.UNINITIALIZED
    private var stateMessage: String? = null

    // State flow for external observation
    private val _stateFlow = MutableStateFlow(getCurrentStateSnapshot())
    val stateFlow: StateFlow<VivokaStateSnapshot> = _stateFlow

    // Thread synchronization
    private val stateMutex = Mutex()

    /**
     * State snapshot for external observation
     */
    data class VivokaStateSnapshot(
        val isInitialized: Boolean,
        val isListening: Boolean,
        val isDictationActive: Boolean,
        val isVoiceEnabled: Boolean,
        val isVoiceSleeping: Boolean,
        val isRecovering: Boolean,
        val serviceState: ServiceState,
        val currentModelPath: String?,
        val recognizedText: String?,
        val lastExecutedCommandTime: Long
    )

    /**
     * Initialize state for new session
     */
    fun initialize(voiceEnabled: Boolean) {
        Log.d(TAG, "Initializing state - voiceEnabled: $voiceEnabled")

        isInitialized = true
        isInitiallyConfigured = true
        isListening = false
        isDictationActive = false
        isVoiceEnabled = voiceEnabled
        isVoiceSleeping = false
        isRecovering = false
        isInGracefulDegradation = false

        lastExecutedCommandTime = System.currentTimeMillis()
        silenceStartTime = 0L
        retryCount = 0

        setServiceState(ServiceState.READY)
        updateStateFlow()

        Log.i(TAG, "State initialized successfully")
    }

    /**
     * Start listening state
     */
    fun startListening() {
        if (!isInitialized) {
            Log.e(TAG, "Cannot start listening - not initialized")
            return
        }

        isListening = true
        setServiceState(ServiceState.LISTENING)
        lastExecutedCommandTime = System.currentTimeMillis()

        updateStateFlow()
        Log.d(TAG, "Started listening")
    }

    /**
     * Stop listening state
     */
    fun stopListening() {
        isListening = false
        isDictationActive = false
        silenceStartTime = 0L

        setServiceState(ServiceState.READY)
        updateStateFlow()

        Log.d(TAG, "Stopped listening")
    }

    /**
     * Enter dictation mode
     */
    fun enterDictationMode() {
        if (!isListening) {
            Log.w(TAG, "Cannot enter dictation mode - not listening")
            return
        }

        isDictationActive = true
        silenceStartTime = 0L
        setServiceState(ServiceState.PROCESSING)

        updateStateFlow()
        Log.d(TAG, "Entered dictation mode")
    }

    /**
     * Exit dictation mode
     */
    fun exitDictationMode() {
        isDictationActive = false
        silenceStartTime = 0L

        if (isListening) {
            setServiceState(ServiceState.LISTENING)
        } else {
            setServiceState(ServiceState.READY)
        }

        updateStateFlow()
        Log.d(TAG, "Exited dictation mode")
    }

    /**
     * Enter voice sleep mode
     */
    fun enterVoiceSleepMode() {
        Log.d(TAG, "Entering voice sleep mode")

        isVoiceSleeping = true
        setServiceState(ServiceState.PAUSED)

        updateStateFlow()
    }

    /**
     * Exit voice sleep mode
     */
    fun exitVoiceSleepMode() {
        Log.d(TAG, "Exiting voice sleep mode")

        isVoiceSleeping = false
        lastExecutedCommandTime = System.currentTimeMillis()

        if (isListening) {
            setServiceState(ServiceState.LISTENING)
        } else {
            setServiceState(ServiceState.READY)
        }

        updateStateFlow()
    }

    /**
     * Update voice enabled state
     */
    fun setVoiceEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting voice enabled: $enabled")

        isVoiceEnabled = enabled

        if (enabled) {
            isVoiceSleeping = false
            lastExecutedCommandTime = System.currentTimeMillis()
        }

        updateStateFlow()
    }

    /**
     * Set current model path
     */
    fun setCurrentModelPath(path: String?) {
        currentModelPath = path
        updateStateFlow()
        Log.d(TAG, "Current model path updated: $path")
    }

    /**
     * Set recognized text
     */
    fun setRecognizedText(text: String?) {
        recognizedText = text
        updateStateFlow()
    }

    /**
     * Update last executed command time
     */
    fun updateLastExecutedCommandTime() {
        lastExecutedCommandTime = System.currentTimeMillis()
        updateStateFlow()
    }

    /**
     * Update silence detection timing
     */
    fun updateSilenceStartTime() {
        silenceStartTime = System.currentTimeMillis()
    }

    /**
     * Clear silence detection
     */
    fun clearSilenceStartTime() {
        silenceStartTime = 0L
    }

    /**
     * Enter recovery mode
     */
    fun enterRecoveryMode() {
        Log.w(TAG, "Entering recovery mode")

        isRecovering = true
        setServiceState(ServiceState.ERROR, "Recovery in progress")

        updateStateFlow()
    }

    /**
     * Exit recovery mode
     */
    fun exitRecoveryMode(successful: Boolean) {
        Log.i(TAG, "Exiting recovery mode - successful: $successful")

        isRecovering = false
        retryCount = 0

        if (successful) {
            setServiceState(ServiceState.READY)
            isInGracefulDegradation = false
        } else {
            setServiceState(ServiceState.ERROR, "Recovery failed")
        }

        updateStateFlow()
    }

    /**
     * Enter graceful degradation mode
     */
    fun enterGracefulDegradation() {
        Log.w(TAG, "Entering graceful degradation mode")

        isInGracefulDegradation = true
        isInitialized = false
        isInitiallyConfigured = false
        setServiceState(ServiceState.ERROR, "Running in degraded mode")

        updateStateFlow()
    }

    /**
     * Update retry count for recovery operations
     */
    fun updateRetryCount(count: Int) {
        retryCount = count
        updateStateFlow()
    }

    /**
     * Update memory check timestamp
     */
    fun updateMemoryCheckTime() {
        lastMemoryCheck = System.currentTimeMillis()
    }

    /**
     * Persist current state for recovery
     */
    fun persistCurrentState() {
        try {
            persistedState = mapOf<String, Any>(
                "isInitialized" to isInitialized,
                "isListening" to isListening,
                "isDictationActive" to isDictationActive,
                "isVoiceEnabled" to isVoiceEnabled,
                "isVoiceSleeping" to isVoiceSleeping,
                "lastExecutedCommandTime" to lastExecutedCommandTime,
                "currentModelPath" to (currentModelPath ?: ""),
                "timestamp" to System.currentTimeMillis()
            )
            Log.d(TAG, "State persisted for recovery")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist state", e)
        }
    }

    /**
     * Restore state from persisted data
     */
    fun restoreFromPersistedState(): Boolean {
        return try {
            persistedState?.let { state ->
                val timestamp = state["timestamp"] as? Long ?: 0L
                val currentTime = System.currentTimeMillis()

                // Only restore if state is recent (within 1 minute)
                if (currentTime - timestamp < 60000) {
                    isVoiceEnabled = state["isVoiceEnabled"] as? Boolean ?: false
                    isVoiceSleeping = state["isVoiceSleeping"] as? Boolean ?: false
                    currentModelPath = state["currentModelPath"] as? String

                    // Don't restore listening/dictation states as they require active audio pipeline

                    updateStateFlow()
                    Log.i(TAG, "State restored from persisted data")
                    true
                } else {
                    Log.w(TAG, "Persisted state too old, not restoring")
                    false
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore persisted state", e)
            false
        }
    }

    /**
     * Reset all state to initial values
     */
    fun reset() {
        Log.d(TAG, "Resetting all state")

        isInitialized = false
        isInitiallyConfigured = false
        isListening = false
        isDictationActive = false
        isVoiceEnabled = false
        isVoiceSleeping = false
        isRecovering = false
        isInGracefulDegradation = false

        lastExecutedCommandTime = System.currentTimeMillis()
        silenceStartTime = 0L
        retryCount = 0

        currentModelPath = null
        recognizedText = null
        persistedState = null

        setServiceState(ServiceState.STOPPED)
        updateStateFlow()
    }

    /**
     * Update the state flow with current state
     */
    private fun updateStateFlow() {
        _stateFlow.value = getCurrentStateSnapshot()
    }

    /**
     * Get current state snapshot
     */
    private fun getCurrentStateSnapshot(): VivokaStateSnapshot {
        return VivokaStateSnapshot(
            isInitialized = isInitialized,
            isListening = isListening,
            isDictationActive = isDictationActive,
            isVoiceEnabled = isVoiceEnabled,
            isVoiceSleeping = isVoiceSleeping,
            isRecovering = isRecovering,
            serviceState = currentServiceState,
            currentModelPath = currentModelPath,
            recognizedText = recognizedText,
            lastExecutedCommandTime = lastExecutedCommandTime
        )
    }

    // Getters for state flags
    fun isInitialized(): Boolean = isInitialized
    fun isInitiallyConfigured(): Boolean = isInitiallyConfigured
    fun isListening(): Boolean = isListening
    fun isDictationActive(): Boolean = isDictationActive
    fun isVoiceEnabled(): Boolean = isVoiceEnabled
    fun isVoiceSleeping(): Boolean = isVoiceSleeping
    fun isRecovering(): Boolean = isRecovering
    fun isInGracefulDegradation(): Boolean = isInGracefulDegradation

    // Getters for timing state
    fun getLastExecutedCommandTime(): Long = lastExecutedCommandTime
    fun getSilenceStartTime(): Long = silenceStartTime
    fun getRetryCount(): Int = retryCount
    fun getLastMemoryCheck(): Long = lastMemoryCheck

    // Getters for data state
    fun getCurrentModelPath(): String? = currentModelPath
    fun getRecognizedText(): String? = recognizedText
    fun getServiceState(): ServiceState = currentServiceState
    fun getStateMessage(): String? = stateMessage

    /**
     * Helper to set service state with optional message
     */
    private fun setServiceState(state: ServiceState, message: String? = null) {
        currentServiceState = state
        stateMessage = message
    }

    /**
     * Check if enough time has passed since last command for timeout
     */
    fun shouldTimeout(timeoutMinutes: Int): Boolean {
        val currentTime = System.currentTimeMillis()
        val difference = currentTime - lastExecutedCommandTime
        val differenceInMinutes = difference / 60000
        return differenceInMinutes >= timeoutMinutes
    }

    /**
     * Check if enough time has passed since silence started for dictation timeout
     */
    fun shouldStopDictation(timeoutMs: Long): Boolean {
        if (silenceStartTime == 0L || !isDictationActive) return false
        val currentTime = System.currentTimeMillis()
        return (currentTime - silenceStartTime) >= timeoutMs
    }

    /**
     * Get comprehensive state summary for debugging
     */
    fun getStateSummary(): Map<String, Any> {
        return mapOf(
            "isInitialized" to isInitialized,
            "isInitiallyConfigured" to isInitiallyConfigured,
            "isListening" to isListening,
            "isDictationActive" to isDictationActive,
            "isVoiceEnabled" to isVoiceEnabled,
            "isVoiceSleeping" to isVoiceSleeping,
            "isRecovering" to isRecovering,
            "isInGracefulDegradation" to isInGracefulDegradation,
            "serviceState" to currentServiceState,
            "currentModelPath" to (currentModelPath ?: "none"),
            "retryCount" to retryCount,
            "timeSinceLastCommand" to (System.currentTimeMillis() - lastExecutedCommandTime),
            "hasPersistedState" to (persistedState != null)
        )
    }
}
