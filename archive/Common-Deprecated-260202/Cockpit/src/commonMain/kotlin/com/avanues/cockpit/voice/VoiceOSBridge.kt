package com.avanues.cockpit.voice

import com.avanues.cockpit.voice.commands.VoiceCommand

/**
 * Interface for communication with the VoiceOS accessibility service.
 * This is the primary input method for Cockpit.
 */
interface VoiceOSBridge {
    // Cockpit -> VoiceOS
    suspend fun requestVoiceInput(): VoiceCommand
    suspend fun announceAction(action: String)
    suspend fun requestAccessibilityInfo(windowId: String): AccessibilityNode?

    // VoiceOS -> Cockpit
    fun onVoiceCommand(command: VoiceCommand)
    // Note: AccessibilityEvent and GazeTarget would be defined in their respect packages or imported
    // For now we will define simple placeholders or comments if types are missing
    fun onAccessibilityEvent(event: Any) // Placeholder for AccessibilityEvent
    fun onGazeTarget(target: Any) // Placeholder for GazeTarget
}

// Placeholder data classes if not yet defined elsewhere
data class AccessibilityNode(
    val id: String,
    val text: String?,
    val contentDescription: String?,
    val actions: List<String>
)
