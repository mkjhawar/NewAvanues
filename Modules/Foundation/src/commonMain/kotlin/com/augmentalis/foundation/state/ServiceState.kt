package com.augmentalis.foundation.state

import kotlinx.coroutines.flow.StateFlow

/**
 * ServiceState - Cross-module service lifecycle state.
 *
 * Used by modules (VoiceOSCore, VoiceCursor, WebAvanue) to expose their
 * runtime state to the dashboard and other observers.
 *
 * Usage in a module:
 * ```
 * class VoiceCursorStateProvider : ServiceStateProvider {
 *     private val _state = MutableStateFlow<ServiceState>(ServiceState.Stopped)
 *     override val state: StateFlow<ServiceState> = _state.asStateFlow()
 *     override val moduleId: String = "voicecursor"
 *     override val displayName: String = "VoiceCursor"
 *
 *     fun onServiceStarted() { _state.value = ServiceState.Running(mapOf("dwell" to "1.5s")) }
 *     fun onServiceStopped() { _state.value = ServiceState.Stopped }
 * }
 * ```
 *
 * Usage in dashboard:
 * ```
 * val providers: List<ServiceStateProvider> = // injected
 * providers.forEach { provider ->
 *     val currentState by provider.state.collectAsState()
 *     PulseDot(state = currentState)
 *     Text(provider.displayName)
 * }
 * ```
 */
sealed class ServiceState {

    /** Service is running and operational. */
    data class Running(
        val metadata: Map<String, String> = emptyMap()
    ) : ServiceState()

    /** Service is ready but not actively processing (e.g., WebAvanue browser available). */
    data class Ready(
        val metadata: Map<String, String> = emptyMap()
    ) : ServiceState()

    /** Service is stopped / not running. */
    data object Stopped : ServiceState()

    /** Service encountered an error. */
    data class Error(
        val message: String,
        val recoverable: Boolean = true
    ) : ServiceState()

    /** Service is running but in a degraded state. */
    data class Degraded(
        val reason: String,
        val metadata: Map<String, String> = emptyMap()
    ) : ServiceState()

    val isActive: Boolean
        get() = this is Running || this is Ready || this is Degraded
}

/**
 * ServiceStateProvider - Interface for modules to expose their service state.
 *
 * Each module that provides a background service (VoiceOSCore, VoiceCursor, etc.)
 * implements this interface. The dashboard collects all providers and observes
 * their state flows.
 */
interface ServiceStateProvider {
    /** Unique identifier for this service (e.g., "voiceoscore", "voicecursor"). */
    val moduleId: String

    /** User-facing display name (e.g., "VoiceAvanue", "VoiceCursor"). */
    val displayName: String

    /** Brief description of what this service does. */
    val description: String

    /** Observable state flow. Emits on every state change. */
    val state: StateFlow<ServiceState>

    /** Optional metadata about the current state (e.g., command count, tab count). */
    val metadata: StateFlow<Map<String, String>>
        get() = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())
}

/**
 * LastHeardCommand - Represents the most recent voice command processed.
 *
 * Displayed on the dashboard as real-time feedback.
 */
data class LastHeardCommand(
    val phrase: String,
    val confidence: Float,
    val timestampMs: Long,
    val wasExecuted: Boolean = true
) {
    companion object {
        val NONE = LastHeardCommand("", 0f, 0L, false)
    }
}
