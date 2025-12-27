/**
 * VoiceStatusOverlay.kt - Voice recognition status indicator overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-09-03
 */
package com.augmentalis.voiceoscore.accessibility.ui.overlays

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.augmentalis.voiceoscore.accessibility.ui.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel

/**
 * Voice status overlay showing recognition state
 */
class VoiceStatusOverlay(context: Context) : BaseOverlay(context, OverlayType.POSITIONED) {
    
    
    companion object {
        private const val TAG = "VoiceStatusOverlay"
    }
    
    private var _status by mutableStateOf(VoiceStatus.INACTIVE)
    private var _confidence by mutableStateOf(0f)
    private var _lastCommand by mutableStateOf("")
    private var _errorMessage by mutableStateOf("")
    
    /**
     * Update voice recognition status
     */
    fun updateStatus(
        status: VoiceStatus,
        confidence: Float = 0f,
        lastCommand: String = "",
        errorMessage: String = ""
    ) {
        _status = status
        _confidence = confidence
        _lastCommand = lastCommand
        _errorMessage = errorMessage
    }
    
    /**
     * Show status temporarily (auto-hide after delay)
     */
    fun showTemporary(durationMs: Long = 3000L) {
        show()
        
        // Auto-hide after delay
        overlayScope.launch {
            delay(durationMs)
            if (_status != VoiceStatus.LISTENING) {
                hide()
            }
        }
    }
    
    @Composable
    override fun OverlayContent() {
        val infiniteTransition = rememberInfiniteTransition(label = "voice_animation")
        
        // Pulsing animation for listening state
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        
        // Scale animation for processing
        val processingScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "processing_scale"
        )
        
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .glassMorphism(
                        config = GlassMorphismConfig(
                            cornerRadius = 12.dp,
                            backgroundOpacity = 0.15f,
                            borderOpacity = 0.25f,
                            borderWidth = 1.dp,
                            tintColor = getStatusColor(_status),
                            tintOpacity = 0.2f
                        ),
                        depth = DepthLevel(0.8f)
                    )
                    .graphicsLayer {
                        scaleX = if (_status == VoiceStatus.PROCESSING) processingScale else 1f
                        scaleY = if (_status == VoiceStatus.PROCESSING) processingScale else 1f
                    },
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status icon with animation
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = getStatusColor(_status).copy(
                                    alpha = if (_status == VoiceStatus.LISTENING) pulseAlpha else 1f
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getStatusIcon(_status),
                            contentDescription = "Voice Status",
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }
                    
                    // Status content
                    Column {
                        Text(
                            text = getStatusText(_status),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Show confidence for successful recognition
                        if (_status == VoiceStatus.SUCCESS && _confidence > 0f) {
                            Text(
                                text = "${(_confidence * 100).toInt()}% confident",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                        
                        // Show last command
                        if (_lastCommand.isNotEmpty()) {
                            Text(
                                text = "\"$_lastCommand\"",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                        
                        // Show error message
                        if (_status == VoiceStatus.ERROR && _errorMessage.isNotEmpty()) {
                            Text(
                                text = _errorMessage,
                                color = Color(0xFFFF5722),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Get color for status
     */
    private fun getStatusColor(status: VoiceStatus): Color {
        return when (status) {
            VoiceStatus.INACTIVE -> Color.Gray
            VoiceStatus.LISTENING -> Color(0xFF2196F3) // Blue
            VoiceStatus.PROCESSING -> Color(0xFFFF9800) // Orange
            VoiceStatus.SUCCESS -> Color(0xFF4CAF50) // Green
            VoiceStatus.ERROR -> Color(0xFFFF5722) // Red
        }
    }
    
    /**
     * Get icon for status
     */
    private fun getStatusIcon(status: VoiceStatus): ImageVector {
        return when (status) {
            VoiceStatus.INACTIVE -> Icons.Default.MicOff
            VoiceStatus.LISTENING -> Icons.Default.Mic
            VoiceStatus.PROCESSING -> Icons.Default.Refresh
            VoiceStatus.SUCCESS -> Icons.Default.Check
            VoiceStatus.ERROR -> Icons.Default.Error
        }
    }
    
    /**
     * Get text for status
     */
    private fun getStatusText(status: VoiceStatus): String {
        return when (status) {
            VoiceStatus.INACTIVE -> "Voice Off"
            VoiceStatus.LISTENING -> "Listening..."
            VoiceStatus.PROCESSING -> "Processing..."
            VoiceStatus.SUCCESS -> "Command Recognized"
            VoiceStatus.ERROR -> "Recognition Error"
        }
    }
    
    override fun onOverlayShown() {
        super.onOverlayShown()
        // Position in top-right corner
        updatePosition(50, 50)
    }
    
    /**
     * Cleanup resources when overlay is destroyed
     */
    override fun dispose() {
        overlayScope.cancel()
        super.dispose()
    }
}

/**
 * Voice recognition status states
 */
enum class VoiceStatus {
    INACTIVE,    // Voice recognition is off
    LISTENING,   // Listening for voice input
    PROCESSING,  // Processing the voice input
    SUCCESS,     // Successfully recognized command
    ERROR        // Error in recognition
}