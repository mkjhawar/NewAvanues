/**
 * VoiceUIButton.kt - Voice-enabled button component
 * 
 * Core button component with voice command support
 * Author: VOS4 Development Team
 * Created: 2025-08-30
 */

package com.augmentalis.voiceui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Voice-enabled button with automatic command registration
 */
@Composable
fun VoiceUIButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    voiceCommand: String = text.lowercase().replace(" ", "_"),
    voiceAlternatives: List<String> = emptyList(),
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    // Register voice command
    DisposableEffect(voiceCommand) {
        VoiceCommandRegistry.register(voiceCommand, onClick)
        voiceAlternatives.forEach { alt ->
            VoiceCommandRegistry.register(alt, onClick)
        }
        
        onDispose {
            VoiceCommandRegistry.unregister(voiceCommand)
            voiceAlternatives.forEach { alt ->
                VoiceCommandRegistry.unregister(alt)
            }
        }
    }
    
    // Render button
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    ) {
        Text(text)
    }
}

/**
 * Voice command registry for managing active commands
 */
object VoiceCommandRegistry {
    private val commands = mutableMapOf<String, () -> Unit>()
    
    fun register(command: String, action: () -> Unit) {
        commands[command.lowercase()] = action
    }
    
    fun unregister(command: String) {
        commands.remove(command.lowercase())
    }
    
    fun execute(command: String): Boolean {
        val action = commands[command.lowercase()]
        return if (action != null) {
            action()
            true
        } else {
            false
        }
    }
    
    fun getCommands(): Set<String> = commands.keys
}