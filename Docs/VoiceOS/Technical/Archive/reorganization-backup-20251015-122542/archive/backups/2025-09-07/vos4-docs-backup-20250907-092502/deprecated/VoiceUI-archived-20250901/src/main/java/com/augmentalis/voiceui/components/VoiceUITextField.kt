/**
 * VoiceUITextField.kt - Voice-enabled text field component
 * 
 * Text input with voice dictation support
 * Author: VOS4 Development Team
 * Created: 2025-08-30
 */

package com.augmentalis.voiceui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Voice-enabled text field with dictation support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceUITextField(
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    value: String = "",
    voiceCommand: String = "enter_${label.lowercase().replace(" ", "_")}",
    voiceDictation: Boolean = true,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = if (isPassword) {
        KeyboardOptions(keyboardType = KeyboardType.Password)
    } else {
        KeyboardOptions.Default
    }
) {
    var textValue by remember { mutableStateOf(value) }
    var isListening by remember { mutableStateOf(false) }
    
    // Register voice command for focusing this field
    DisposableEffect(voiceCommand) {
        VoiceCommandRegistry.register(voiceCommand) {
            // Focus this field for voice input
            isListening = true
        }
        
        onDispose {
            VoiceCommandRegistry.unregister(voiceCommand)
        }
    }
    
    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            textValue = newValue
            onValueChange(newValue)
        },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = keyboardOptions,
        trailingIcon = if (voiceDictation && !isPassword) {
            {
                IconButton(
                    onClick = { isListening = !isListening }
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = if (isListening) "Stop listening" else "Start listening"
                    )
                }
            }
        } else null
    )
    
    // Handle voice dictation
    if (isListening && voiceDictation) {
        LaunchedEffect(isListening) {
            // This would connect to the speech recognition service
            // For now, just simulate
            VoiceDictationHandler.startListening { recognizedText ->
                textValue = recognizedText
                onValueChange(recognizedText)
                isListening = false
            }
        }
    }
}

/**
 * Voice dictation handler
 */
object VoiceDictationHandler {
    fun startListening(onResult: (String) -> Unit) {
        // This would connect to the actual speech recognition service
        // For now, just a placeholder
        onResult("")
    }
    
    fun stopListening() {
        // Stop speech recognition
    }
}