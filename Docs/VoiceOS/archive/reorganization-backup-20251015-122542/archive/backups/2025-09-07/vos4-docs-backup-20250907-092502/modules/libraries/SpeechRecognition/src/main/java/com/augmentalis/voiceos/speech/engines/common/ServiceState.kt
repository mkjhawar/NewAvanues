/**
 * ServiceState.kt - Unified service state management for all engines
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 */
package com.augmentalis.voiceos.speech.engines.common

import com.augmentalis.voiceos.speech.api.OnSpeechResultListener
import com.augmentalis.voiceos.speech.api.OnStateChangeListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages service state across all speech recognition engines.
 * Provides consistent state tracking and transition management.
 */
class ServiceState {
    // State flow for observers
    private val _state = MutableStateFlow(State.UNINITIALIZED)
    val state: StateFlow<State> = _state.asStateFlow()
    
    // Current state accessor
    val currentState: State
        get() = _state.value
    
    // State metadata
    private var lastStateChange: Long = System.currentTimeMillis()
    private var lastError: String? = null
    private var stateHistory = mutableListOf<StateTransition>()
    private val maxHistorySize = 100
    private val historyRetentionMs = 3600000L // Keep 1 hour of history
    
    // Listener for state changes (used by engines)
    private var resultListener: OnSpeechResultListener? = null
    private var stateChangeListener: OnStateChangeListener? = null
    
    /**
     * Service states - Extended for engine compatibility
     */
    enum class State {
        UNINITIALIZED,    // Engine not initialized
        NOT_INITIALIZED,  // Alias for UNINITIALIZED (engine compatibility)
        INITIALIZING,     // Engine is initializing
        INITIALIZED,      // Successfully initialized (engine compatibility)
        READY,           // Ready to start recognition
        LISTENING,       // Actively listening for speech
        FREE_SPEECH,     // Free speech/dictation mode (Vivoka compatibility)
        PROCESSING,      // Processing audio data
        PAUSED,          // Temporarily paused
        SLEEPING,        // Engine in sleep mode (engine compatibility)
        DEGRADED,        // Engine running in limited/degraded mode
        ERROR,           // Error state
        DESTROYING,      // Engine is destroying (engine compatibility)
        SHUTDOWN         // Engine shut down
    }
    
    /**
     * Update service state with validation
     */
    fun setState(newState: State, error: String? = null): Boolean {
        val currentState = _state.value
        
        // Validate state transition
        if (!isValidTransition(currentState, newState)) {
            return false
        }
        
        // Record state change
        val timestamp = System.currentTimeMillis()
        stateHistory.add(StateTransition(currentState, newState, timestamp, error))
        
        // Trim history by size and age
        trimHistory(timestamp)
        
        // Additional size check
        if (stateHistory.size > maxHistorySize) {
            stateHistory.removeAt(0)
        }
        
        // Update state
        _state.value = newState
        lastStateChange = timestamp
        
        // Store error if in error state
        if (newState == State.ERROR) {
            lastError = error
        } else {
            lastError = null
        }
        
        return true
    }
    
    /**
     * Check if state transition is valid
     */
    private fun isValidTransition(from: State, to: State): Boolean {
        return when (from) {
            State.UNINITIALIZED, State.NOT_INITIALIZED -> to in listOf(State.INITIALIZING, State.DEGRADED, State.ERROR, State.SHUTDOWN)
            State.INITIALIZING -> to in listOf(State.READY, State.INITIALIZED, State.DEGRADED, State.ERROR, State.SHUTDOWN)
            State.INITIALIZED -> to in listOf(State.READY, State.LISTENING, State.ERROR, State.SHUTDOWN)
            State.READY -> to in listOf(State.LISTENING, State.FREE_SPEECH, State.PAUSED, State.SLEEPING, State.ERROR, State.SHUTDOWN)
            State.LISTENING -> to in listOf(State.PROCESSING, State.FREE_SPEECH, State.READY, State.PAUSED, State.ERROR, State.SHUTDOWN)
            State.FREE_SPEECH -> to in listOf(State.LISTENING, State.READY, State.PAUSED, State.ERROR, State.SHUTDOWN)
            State.PROCESSING -> to in listOf(State.LISTENING, State.READY, State.ERROR, State.SHUTDOWN)
            State.PAUSED -> to in listOf(State.READY, State.LISTENING, State.ERROR, State.SHUTDOWN)
            State.SLEEPING -> to in listOf(State.READY, State.LISTENING, State.ERROR, State.SHUTDOWN)
            State.DEGRADED -> to in listOf(State.INITIALIZING, State.READY, State.ERROR, State.SHUTDOWN)
            State.ERROR -> to in listOf(State.INITIALIZING, State.DEGRADED, State.SHUTDOWN)
            State.DESTROYING -> to in listOf(State.SHUTDOWN)
            State.SHUTDOWN -> false // Cannot transition from shutdown
        }
    }
    
    /**
     * Check if engine is in a usable state
     */
    fun isUsable(): Boolean {
        return _state.value in listOf(State.READY, State.LISTENING, State.PROCESSING, State.FREE_SPEECH)
    }
    
    /**
     * Check if engine is listening
     */
    fun isListening(): Boolean {
        return _state.value == State.LISTENING
    }
    
    /**
     * Check if engine is ready
     */
    fun isReady(): Boolean {
        return _state.value == State.READY
    }
    
    /**
     * Check if engine has error
     */
    fun hasError(): Boolean {
        return _state.value == State.ERROR
    }
    
    /**
     * Get last error message
     */
    fun getLastError(): String? {
        return lastError
    }
    
    /**
     * Get time since last state change in milliseconds
     */
    fun getTimeSinceLastChange(): Long {
        return System.currentTimeMillis() - lastStateChange
    }
    
    /**
     * Get state history
     */
    fun getHistory(): List<StateTransition> {
        return stateHistory.toList()
    }
    
    /**
     * Clear state history
     */
    fun clearHistory() {
        stateHistory.clear()
    }
    
    /**
     * Reset to initial state
     */
    fun reset() {
        _state.value = State.UNINITIALIZED
        lastError = null
        lastStateChange = System.currentTimeMillis()
        stateHistory.clear()
    }
    
    /**
     * Set result listener (for engine compatibility)
     */
    fun setListener(listener: OnSpeechResultListener) {
        this.resultListener = listener
    }
    
    /**
     * Update state with message (for engine compatibility)
     * Maps to setState internally
     */
    fun updateState(state: State, message: String? = null) {
        setState(state, message)
        // Notify listener if set
        stateChangeListener?.invoke(state.name, message)
    }
    
    /**
     * Trim history to remove old entries
     */
    private fun trimHistory(currentTime: Long) {
        val cutoffTime = currentTime - historyRetentionMs
        stateHistory.removeAll { it.timestamp < cutoffTime }
    }
    
    /**
     * Data class for state transitions
     */
    data class StateTransition(
        val fromState: State,
        val toState: State,
        val timestamp: Long,
        val error: String? = null
    )
}