/**
 * DialogStateMachine.kt - State machine for dialog management
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-04
 * 
 * Simple state machine for managing dialog states in the voice accessibility system.
 * Handles transitions between IDLE, LISTENING, PROCESSING, and RESPONDING states.
 */
package com.augmentalis.voiceos.accessibility.state

import android.util.Log
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentHashMap

/**
 * State machine for dialog management with basic states
 */
class DialogStateMachine {
    
    companion object {
        private const val TAG = "DialogStateMachine"
    }
    
    /**
     * Available dialog states
     */
    enum class DialogState {
        IDLE,        // Ready for new input
        LISTENING,   // Actively listening for voice input
        PROCESSING,  // Processing received input
        RESPONDING   // Providing response/feedback
    }
    
    /**
     * State transition events
     */
    enum class StateEvent {
        START_LISTENING,   // Begin listening for input
        STOP_LISTENING,    // Stop listening
        INPUT_RECEIVED,    // Input has been received and needs processing
        PROCESSING_DONE,   // Processing completed
        RESPONSE_STARTED,  // Response/feedback started
        RESPONSE_COMPLETE, // Response/feedback completed
        ERROR_OCCURRED,    // Error occurred, return to idle
        RESET             // Force reset to idle state
    }
    
    // Current state (thread-safe)
    private val currentState = AtomicReference(DialogState.IDLE)
    
    // State transition listeners
    private val stateListeners = ConcurrentHashMap<String, (DialogState, DialogState) -> Unit>()
    
    // Transition history for debugging
    private val transitionHistory = mutableListOf<StateTransition>()
    private val maxHistorySize = 50
    
    /**
     * Data class for tracking state transitions
     */
    data class StateTransition(
        val fromState: DialogState,
        val toState: DialogState,
        val event: StateEvent,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Get current state
     */
    fun getCurrentState(): DialogState = currentState.get()
    
    /**
     * Process a state event and transition if valid
     * @param event The event to process
     * @return true if transition was successful, false if invalid
     */
    fun processEvent(event: StateEvent): Boolean {
        val currentState = this.currentState.get()
        val newState = getNextState(currentState, event)
        
        if (newState == null) {
            Log.w(TAG, "Invalid transition: $currentState -> $event")
            return false
        }
        
        if (newState == currentState) {
            Log.d(TAG, "No state change needed for event: $event in state: $currentState")
            return true
        }
        
        // Perform the transition
        val previousState = this.currentState.getAndSet(newState)
        
        // Record transition
        recordTransition(previousState, newState, event)
        
        // Notify listeners
        notifyStateListeners(previousState, newState)
        
        Log.d(TAG, "State transition: $previousState -> $newState (event: $event)")
        return true
    }
    
    /**
     * Determine the next state based on current state and event
     */
    private fun getNextState(currentState: DialogState, event: StateEvent): DialogState? {
        return when (currentState) {
            DialogState.IDLE -> when (event) {
                StateEvent.START_LISTENING -> DialogState.LISTENING
                StateEvent.RESET -> DialogState.IDLE
                else -> null
            }
            
            DialogState.LISTENING -> when (event) {
                StateEvent.STOP_LISTENING -> DialogState.IDLE
                StateEvent.INPUT_RECEIVED -> DialogState.PROCESSING
                StateEvent.ERROR_OCCURRED -> DialogState.IDLE
                StateEvent.RESET -> DialogState.IDLE
                else -> null
            }
            
            DialogState.PROCESSING -> when (event) {
                StateEvent.PROCESSING_DONE -> DialogState.RESPONDING
                StateEvent.ERROR_OCCURRED -> DialogState.IDLE
                StateEvent.RESET -> DialogState.IDLE
                else -> null
            }
            
            DialogState.RESPONDING -> when (event) {
                StateEvent.RESPONSE_COMPLETE -> DialogState.IDLE
                StateEvent.START_LISTENING -> DialogState.LISTENING // Allow direct transition to listening
                StateEvent.ERROR_OCCURRED -> DialogState.IDLE
                StateEvent.RESET -> DialogState.IDLE
                else -> null
            }
        }
    }
    
    /**
     * Check if the state machine is in a specific state
     */
    fun isInState(state: DialogState): Boolean = currentState.get() == state
    
    /**
     * Check if the state machine is busy (not idle)
     */
    fun isBusy(): Boolean = currentState.get() != DialogState.IDLE
    
    /**
     * Check if the state machine can accept new input
     */
    fun canAcceptInput(): Boolean {
        val state = currentState.get()
        return state == DialogState.IDLE || state == DialogState.RESPONDING
    }
    
    /**
     * Force reset to idle state
     */
    fun reset() {
        val previousState = currentState.getAndSet(DialogState.IDLE)
        if (previousState != DialogState.IDLE) {
            recordTransition(previousState, DialogState.IDLE, StateEvent.RESET)
            notifyStateListeners(previousState, DialogState.IDLE)
            Log.i(TAG, "State machine reset: $previousState -> IDLE")
        }
    }
    
    /**
     * Add a state change listener
     */
    fun addStateListener(key: String, listener: (DialogState, DialogState) -> Unit) {
        stateListeners[key] = listener
    }
    
    /**
     * Remove a state change listener
     */
    fun removeStateListener(key: String) {
        stateListeners.remove(key)
    }
    
    /**
     * Get transition history for debugging
     */
    fun getTransitionHistory(): List<StateTransition> {
        return synchronized(transitionHistory) {
            transitionHistory.toList()
        }
    }
    
    /**
     * Get recent transitions (last N)
     */
    fun getRecentTransitions(count: Int = 5): List<StateTransition> {
        return synchronized(transitionHistory) {
            transitionHistory.takeLast(count)
        }
    }
    
    /**
     * Clear transition history
     */
    fun clearHistory() {
        synchronized(transitionHistory) {
            transitionHistory.clear()
        }
    }
    
    /**
     * Record a state transition in history
     */
    private fun recordTransition(fromState: DialogState, toState: DialogState, event: StateEvent) {
        synchronized(transitionHistory) {
            transitionHistory.add(StateTransition(fromState, toState, event))
            
            // Trim history if it gets too large
            if (transitionHistory.size > maxHistorySize) {
                transitionHistory.removeAt(0)
            }
        }
    }
    
    /**
     * Notify all state listeners of a transition
     */
    private fun notifyStateListeners(fromState: DialogState, toState: DialogState) {
        stateListeners.values.forEach { listener ->
            try {
                listener(fromState, toState)
            } catch (e: Exception) {
                Log.e(TAG, "Error in state listener", e)
            }
        }
    }
    
    /**
     * Get debug information about current state and recent activity
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("DialogStateMachine Debug Info")
            appendLine("Current State: ${currentState.get()}")
            appendLine("Is Busy: ${isBusy()}")
            appendLine("Can Accept Input: ${canAcceptInput()}")
            appendLine("Listeners: ${stateListeners.size}")
            
            val recentTransitions = getRecentTransitions(3)
            if (recentTransitions.isNotEmpty()) {
                appendLine("Recent Transitions:")
                recentTransitions.forEach { transition ->
                    appendLine("  ${transition.fromState} -> ${transition.toState} (${transition.event})")
                }
            }
        }
    }
}