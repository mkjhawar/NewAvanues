/**
 * VoiceUIElements.kt - Additional UI element definitions for VoiceUI
 * Main definitions are in VoiceUIDesigner.kt
 */

package com.augmentalis.voiceui.designer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Audio properties for spatial audio
 */
data class AudioProperties(
    val spatialX: Float,
    val spatialY: Float, 
    val spatialZ: Float,
    val volume: Float = 1.0f,
    val pitch: Float = 1.0f
)
