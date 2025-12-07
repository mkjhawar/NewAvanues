/**
 * VoiceUIText.kt - Voice-enabled text component
 * 
 * Text display with voice announcement support
 * Author: VOS4 Development Team
 * Created: 2025-08-30
 */

package com.augmentalis.voiceui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * Voice-enabled text component
 */
@Composable
fun VoiceUIText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    textAlign: TextAlign? = null,
    voiceAnnounce: Boolean = false,
    voiceCommand: String? = null
) {
    // Register voice command if provided
    if (voiceCommand != null) {
        DisposableEffect(voiceCommand) {
            VoiceCommandRegistry.register(voiceCommand) {
                // Announce the text when command is triggered
                VoiceAnnouncementHandler.announce(text)
            }
            
            onDispose {
                VoiceCommandRegistry.unregister(voiceCommand)
            }
        }
    }
    
    // Auto-announce on first appearance if requested
    if (voiceAnnounce) {
        LaunchedEffect(text) {
            VoiceAnnouncementHandler.announce(text)
        }
    }
    
    Text(
        text = text,
        modifier = modifier,
        style = style,
        textAlign = textAlign
    )
}

/**
 * Voice announcement handler
 */
object VoiceAnnouncementHandler {
    fun announce(text: String) {
        // This would connect to the text-to-speech service
        // For now, just a placeholder
    }
}