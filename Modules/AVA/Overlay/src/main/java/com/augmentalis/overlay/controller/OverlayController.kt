// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/controller/OverlayController.kt
// created: 2025-11-01 22:30:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 1 - Core Infrastructure
// agent: Engineer | mode: ACT

package com.augmentalis.overlay.controller

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central controller for overlay state management.
 *
 * Manages overlay expansion state, voice orb position, transcript display,
 * AI responses, and contextual suggestions using Kotlin StateFlow.
 *
 * @author Manoj Jhawar
 */
class OverlayController {

    // Callback for suggestion execution (set by integration bridge)
    var onSuggestionExecute: ((Suggestion) -> Unit)? = null

    // Overlay state
    private val _state = MutableStateFlow(OverlayState.Docked)
    val state: StateFlow<OverlayState> = _state.asStateFlow()

    private val _expanded = MutableStateFlow(false)
    val expanded: StateFlow<Boolean> = _expanded.asStateFlow()

    // Voice orb position (draggable)
    private val _orbPosition = MutableStateFlow(Offset(24f, 320f))
    val orbPosition: StateFlow<Offset> = _orbPosition.asStateFlow()

    // Transcript from voice input
    private val _transcript = MutableStateFlow<String?>(null)
    val transcript: StateFlow<String?> = _transcript.asStateFlow()

    // AI response text
    private val _response = MutableStateFlow<String?>(null)
    val response: StateFlow<String?> = _response.asStateFlow()

    // Contextual suggestions
    private val _suggestions = MutableStateFlow(
        listOf(
            Suggestion("Copy", "copy"),
            Suggestion("Translate", "translate"),
            Suggestion("Search", "search"),
            Suggestion("Summarize", "summarize")
        )
    )
    val suggestions: StateFlow<List<Suggestion>> = _suggestions.asStateFlow()

    /**
     * Expand the overlay panel
     */
    fun expand() {
        _expanded.value = true
        _state.value = OverlayState.Listening
    }

    /**
     * Collapse the overlay panel to orb only
     */
    fun collapse() {
        _expanded.value = false
        _state.value = OverlayState.Docked
        _transcript.value = null
        _response.value = null
    }

    /**
     * Start voice listening
     */
    fun startListening() {
        _state.value = OverlayState.Listening
        _expanded.value = true
        _transcript.value = null
        _response.value = null
    }

    /**
     * Update transcript from voice recognizer
     */
    fun onTranscript(text: String) {
        _transcript.value = text
        _state.value = OverlayState.Processing
    }

    /**
     * Update response from AI
     */
    fun onResponse(text: String) {
        _response.value = text
        _state.value = OverlayState.Responding
    }

    /**
     * Update suggestions based on context
     */
    fun updateSuggestions(newSuggestions: List<Suggestion>) {
        _suggestions.value = newSuggestions
    }

    /**
     * Update voice orb position from drag gesture
     */
    fun updateOrbPosition(delta: Offset) {
        _orbPosition.value = _orbPosition.value + delta
    }

    /**
     * Set absolute orb position
     */
    fun setOrbPosition(position: Offset) {
        _orbPosition.value = position
    }

    /**
     * Execute a suggestion action
     */
    fun executeSuggestion(suggestion: Suggestion) {
        // Delegate to integration bridge
        onSuggestionExecute?.invoke(suggestion)
    }

    /**
     * Handle error state
     */
    fun onError(message: String) {
        _response.value = "Error: $message"
        _state.value = OverlayState.Error
    }

    /**
     * Reset to idle state
     */
    fun reset() {
        _state.value = OverlayState.Docked
        _expanded.value = false
        _transcript.value = null
        _response.value = null
    }
}

/**
 * Overlay display states
 */
enum class OverlayState {
    Docked,      // Orb only, idle
    Listening,   // Voice active, waveform animation
    Processing,  // NLU classification in progress
    Responding,  // Showing AI response
    Error        // Error message displayed
}

/**
 * Suggestion chip data model
 */
data class Suggestion(
    val label: String,
    val action: String,
    val icon: String? = null
)

/**
 * Convert OverlayState to OrbState for UI
 */
fun OverlayState.toOrbState(): OrbState = when (this) {
    OverlayState.Docked -> OrbState.Idle
    OverlayState.Listening -> OrbState.Listening
    OverlayState.Processing -> OrbState.Processing
    OverlayState.Responding -> OrbState.Speaking
    OverlayState.Error -> OrbState.Idle
}

/**
 * Voice orb visual states
 */
enum class OrbState {
    Idle,        // Gentle pulse
    Listening,   // Waveform animation
    Processing,  // Rotating spinner
    Speaking     // Pulsing glow
}
