/**
 * VoiceStatus.kt - Voice recognition status data model
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team  
 * Created: 2025-01-28
 * 
 * Data model for voice recognition status
 */
package com.augmentalis.voiceuielements.models

data class VoiceStatus(
    val isListening: Boolean = false,
    val recognitionEngine: String = "",
    val currentLanguage: String = "English",
    val confidence: Float = 0.0f,
    val lastCommand: String = ""
)

data class GlassmorphismConfig(
    val blurRadius: Float = 20f,
    val alpha: Float = 0.8f,
    val cornerRadius: Float = 12f
)