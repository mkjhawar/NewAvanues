/**
 * MessageHandler.kt - Simplified message debouncing with user settings
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-06
 */
package com.augmentalis.voiceoscore.managers.localizationmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Message types for different visual styles
 */
enum class MessageType(
    val backgroundColor: Color,
    val textColor: Color,
    val icon: ImageVector
) {
    ERROR(Color(0xFFE57373), Color.White, Icons.Default.Error),
    SUCCESS(Color(0xFF81C784), Color.White, Icons.Default.CheckCircle),
    WARNING(Color(0xFFFFB74D), Color.White, Icons.Default.Warning),
    INFO(Color(0xFF64B5F6), Color.White, Icons.Default.Info)
}

/**
 * Enhanced message handler with user-configurable debounce timing
 * 
 * This is the simplified solution that automatically cancels previous
 * coroutines when a new message appears - no manual Job management needed!
 */
@Composable
fun MessageHandler(
    message: String?,
    messageType: MessageType = MessageType.INFO,
    debounceDuration: Long = 2000L,
    onClearMessage: () -> Unit
) {
    // Simple and effective - LaunchedEffect automatically cancels previous coroutine
    LaunchedEffect(message) {
        if (message?.isNotEmpty() == true) {
            // Enhanced debounce logic based on message type and user preference
            val actualDelay = when {
                debounceDuration == 0L -> return@LaunchedEffect // Instant - no auto-clear
                messageType == MessageType.ERROR -> maxOf(debounceDuration, 1500L) // Errors need more time
                messageType == MessageType.SUCCESS -> minOf(debounceDuration, 3000L) // Success can be shorter
                else -> debounceDuration
            }
            
            delay(actualDelay)
            onClearMessage()
        }
    }
    
    // Display the message if it exists
    if (message?.isNotEmpty() == true) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = messageType.backgroundColor.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = messageType.icon,
                    contentDescription = messageType.name,
                    tint = messageType.textColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = message,
                    color = messageType.textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                // Optional: Add dismiss button for instant dismissal
                if (debounceDuration > 1000L) {
                    IconButton(
                        onClick = onClearMessage,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Dismiss",
                            tint = messageType.textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Convenience composables for specific message types
 */
@Composable
fun ErrorMessage(
    message: String?,
    debounceDuration: Long = 2000L,
    onClear: () -> Unit
) {
    MessageHandler(
        message = message,
        messageType = MessageType.ERROR,
        debounceDuration = debounceDuration,
        onClearMessage = onClear
    )
}

@Composable
fun SuccessMessage(
    message: String?,
    debounceDuration: Long = 2000L,
    onClear: () -> Unit
) {
    MessageHandler(
        message = message,
        messageType = MessageType.SUCCESS,
        debounceDuration = debounceDuration,
        onClearMessage = onClear
    )
}

@Composable
fun WarningMessage(
    message: String?,
    debounceDuration: Long = 2000L,
    onClear: () -> Unit
) {
    MessageHandler(
        message = message,
        messageType = MessageType.WARNING,
        debounceDuration = debounceDuration,
        onClearMessage = onClear
    )
}

@Composable
fun InfoMessage(
    message: String?,
    debounceDuration: Long = 2000L,
    onClear: () -> Unit
) {
    MessageHandler(
        message = message,
        messageType = MessageType.INFO,
        debounceDuration = debounceDuration,
        onClearMessage = onClear
    )
}