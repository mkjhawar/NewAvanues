package com.avanues.cockpit.interactions

import com.avanues.cockpit.voice.commands.VoiceCommand
import com.avanues.cockpit.core.workspace.Vector3D

/**
 * Central manager for routing user inputs (Voice, Touch, Gaze, etc.)
 */
class InputManager {
    
    // Handlers would be injected or initialized here
    // val voiceHandler: VoiceInteractionHandler
    // val touchHandler: TouchInteractionHandler

    fun onVoiceCommand(command: VoiceCommand) {
        // Route to appropriate handler or workspace manager
        println("Received voice command: $command")
    }

    fun onTouchStart(position: Vector3D) {
        // Handle touch start
    }
    
    // Additional input methods...
}
