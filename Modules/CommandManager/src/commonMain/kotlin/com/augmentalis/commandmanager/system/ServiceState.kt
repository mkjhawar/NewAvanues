/**
 * ServiceState.kt - Service state management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP state management for VoiceOS service lifecycle.
 */
package com.augmentalis.commandmanager

import kotlinx.coroutines.flow.*

/**
 * State of the VoiceOS service.
 */
sealed class ServiceState {
    /**
     * Service is not initialized.
     */
    data object Uninitialized : ServiceState()

    /**
     * Service is initializing.
     */
    data class Initializing(
        val progress: Float = 0f,
        val stage: String = ""
    ) : ServiceState()

    /**
     * Service is ready to process commands.
     */
    data class Ready(
        val speechEngineActive: Boolean = false,
        val handlerCount: Int = 0
    ) : ServiceState()

    /**
     * Service is listening for voice commands.
     */
    data class Listening(
        val speechEngine: String = "",
        val wakeWordEnabled: Boolean = false
    ) : ServiceState()

    /**
     * Service is processing a command.
     */
    data class Processing(
        val command: String = "",
        val confidence: Float = 0f
    ) : ServiceState()

    /**
     * Service encountered an error.
     */
    data class Error(
        val message: String,
        val recoverable: Boolean = true
    ) : ServiceState()

    /**
     * Service is paused.
     */
    data class Paused(
        val reason: String = ""
    ) : ServiceState()

    /**
     * Service is stopping.
     */
    data object Stopping : ServiceState()

    /**
     * Service is stopped.
     */
    data object Stopped : ServiceState()

    /**
     * Whether the service is in a state that can process commands.
     */
    val canProcessCommands: Boolean
        get() = this is Ready || this is Listening
}

/**
 * Manages VoiceOS service state.
 */
class ServiceStateManager {

    private val _state = MutableStateFlow<ServiceState>(ServiceState.Uninitialized)
    val state: StateFlow<ServiceState> = _state.asStateFlow()

    /**
     * Transition to a new state.
     *
     * @param newState The new state
     * @return true if transition was valid
     */
    fun transition(newState: ServiceState): Boolean {
        val currentState = _state.value

        // Validate transition
        val isValid = isValidTransition(currentState, newState)
        if (isValid) {
            _state.value = newState
        }

        return isValid
    }

    /**
     * Check if transition is valid.
     */
    private fun isValidTransition(from: ServiceState, to: ServiceState): Boolean {
        return when (from) {
            is ServiceState.Uninitialized -> to is ServiceState.Initializing || to is ServiceState.Stopped
            is ServiceState.Initializing -> to is ServiceState.Ready || to is ServiceState.Error
            is ServiceState.Ready -> to is ServiceState.Listening || to is ServiceState.Processing ||
                    to is ServiceState.Paused || to is ServiceState.Stopping || to is ServiceState.Error
            is ServiceState.Listening -> to is ServiceState.Ready || to is ServiceState.Processing ||
                    to is ServiceState.Paused || to is ServiceState.Error || to is ServiceState.Stopping
            is ServiceState.Processing -> to is ServiceState.Ready || to is ServiceState.Listening ||
                    to is ServiceState.Error
            is ServiceState.Error -> to is ServiceState.Initializing || to is ServiceState.Stopping ||
                    to is ServiceState.Ready
            is ServiceState.Paused -> to is ServiceState.Ready || to is ServiceState.Listening ||
                    to is ServiceState.Stopping
            is ServiceState.Stopping -> to is ServiceState.Stopped
            is ServiceState.Stopped -> to is ServiceState.Initializing
        }
    }

    /**
     * Force transition (bypass validation).
     */
    fun forceTransition(newState: ServiceState) {
        _state.value = newState
    }

    /**
     * Reset to uninitialized state.
     */
    fun reset() {
        _state.value = ServiceState.Uninitialized
    }

    /**
     * Check if service can process commands.
     */
    fun canProcessCommands(): Boolean = _state.value.canProcessCommands
}

/**
 * Configuration for the VoiceOS service.
 */
data class ServiceConfiguration(
    val voiceLanguage: String = "en-US",
    val confidenceThreshold: Float = 0.7f,
    val enableWakeWord: Boolean = true,
    val wakeWord: String = "hey voice",
    val enableHapticFeedback: Boolean = true,
    val enableAudioFeedback: Boolean = true,
    val fingerprintGesturesEnabled: Boolean = false,
    val speechEngine: String = "ANDROID_STT",
    val autoStartListening: Boolean = true,
    val debugMode: Boolean = false,
    /** Enable synonym expansion for voice command matching */
    val synonymsEnabled: Boolean = true,
    /** Language for synonym expansion (ISO 639-1), defaults to voice language */
    val synonymLanguage: String? = null
) {
    companion object {
        val DEFAULT = ServiceConfiguration()
    }

    /** Get the effective synonym language, falling back to voice language prefix */
    fun effectiveSynonymLanguage(): String {
        return synonymLanguage ?: voiceLanguage.substringBefore("-")
    }
}
