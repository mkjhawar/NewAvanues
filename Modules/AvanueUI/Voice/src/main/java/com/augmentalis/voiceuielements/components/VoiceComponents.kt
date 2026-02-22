/**
 * VoiceComponents.kt - Voice UI Components for Compose
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-28
 * 
 * Reusable voice UI components
 */
package com.augmentalis.voiceuielements.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceuielements.models.VoiceStatus
import com.augmentalis.voiceuielements.models.GlassmorphismConfig

@Composable
fun VoiceCommandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Text(text = text)
    }
}

@Composable
fun VoiceStatusCard(
    status: VoiceStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (status.isListening) "Listening" else "Idle",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Engine: ${status.recognitionEngine}")
            Text(text = "Language: ${status.currentLanguage}")
            Text(text = "Confidence: ${(status.confidence * 100).toInt()}%")
            if (status.lastCommand.isNotEmpty()) {
                Text(text = "Last: ${status.lastCommand}")
            }
        }
    }
}

@Composable
fun GlassmorphismCard(
    glassmorphismConfig: GlassmorphismConfig,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(glassmorphismConfig.cornerRadius.dp))
            .background(
                Color.White.copy(alpha = glassmorphismConfig.alpha)
            )
            .blur(radius = glassmorphismConfig.blurRadius.dp)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun VoiceWaveform(
    isAnimating: Boolean,
    amplitude: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Simple waveform representation
    Box(
        modifier = modifier
            .testTag("voice_waveform")
            .size(200.dp, 50.dp)
            .background(
                if (isAnimating) color.copy(alpha = amplitude) else Color.Gray,
                RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isAnimating) "~~~~~" else "-----",
            color = Color.White
        )
    }
}