/**
 * ConfigurationScreen.kt - Speech configuration settings UI
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Purpose: Configuration screen for speech recognition settings
 * Matches VoiceCursor settings UI style with glassmorphism
 */
package com.augmentalis.voicerecognition.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.speechrecognition.SpeechMode

/**
 * Speech configuration data class
 */
data class SpeechConfigurationData(
    val language: String = "en-US",
    val mode: SpeechMode = SpeechMode.DYNAMIC_COMMAND,
    val enableVAD: Boolean = true,
    val confidenceThreshold: Float = 0.7f,
    val maxRecordingDuration: Long = 30000,
    val timeoutDuration: Long = 5000,
    val enableProfanityFilter: Boolean = false
)

/**
 * Configuration screen for speech settings
 */
@Composable
fun ConfigurationScreen(
    currentConfig: SpeechConfigurationData,
    onConfigChanged: (SpeechConfigurationData) -> Unit,
    onBackPressed: () -> Unit
) {
    var config by remember { mutableStateOf(currentConfig) }
    
    val glassMorphismConfig = remember {
        GlassMorphismConfig(
            cornerRadius = 16.dp,
            backgroundOpacity = 0.9f,
            borderOpacity = 0.6f,
            borderWidth = 1.dp,
            tintColor = Color(0xFF007AFF),
            tintOpacity = 0.05f
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF2F2F7),
                        Color(0xFFFFFFFF)
                    )
                )
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF007AFF)
                )
            }
            
            Text(
                text = "Speech Configuration",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D1D1F),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // Language Selection
        ConfigPanel(
            title = "Language & Region",
            glassMorphismConfig = glassMorphismConfig
        ) {
            LanguageSelector(
                selectedLanguage = config.language,
                onLanguageSelected = { language ->
                    config = config.copy(language = language)
                    onConfigChanged(config)
                }
            )
        }
        
        // Recognition Mode
        ConfigPanel(
            title = "Recognition Mode",
            glassMorphismConfig = glassMorphismConfig
        ) {
            ModeSelector(
                selectedMode = config.mode,
                onModeSelected = { mode ->
                    config = config.copy(mode = mode)
                    onConfigChanged(config)
                }
            )
        }
        
        // Voice Activity Detection
        ConfigPanel(
            title = "Voice Detection",
            glassMorphismConfig = glassMorphismConfig
        ) {
            ConfigSwitch(
                title = "Voice Activity Detection",
                subtitle = "Automatically detect when speech starts and stops",
                isChecked = config.enableVAD,
                onCheckedChange = { enabled ->
                    config = config.copy(enableVAD = enabled)
                    onConfigChanged(config)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ConfigSwitch(
                title = "Profanity Filter",
                subtitle = "Filter inappropriate language from results",
                isChecked = config.enableProfanityFilter,
                onCheckedChange = { enabled ->
                    config = config.copy(enableProfanityFilter = enabled)
                    onConfigChanged(config)
                }
            )
        }
        
        // Confidence Threshold
        ConfigPanel(
            title = "Recognition Settings",
            glassMorphismConfig = glassMorphismConfig
        ) {
            ConfigSlider(
                title = "Confidence Threshold",
                value = config.confidenceThreshold,
                valueRange = 0.1f..1.0f,
                steps = 8,
                valueText = "${(config.confidenceThreshold * 100).toInt()}%",
                onValueChange = { threshold ->
                    config = config.copy(confidenceThreshold = threshold)
                    onConfigChanged(config)
                }
            )
        }
        
        // Timeouts
        ConfigPanel(
            title = "Timing Settings",
            glassMorphismConfig = glassMorphismConfig
        ) {
            ConfigSlider(
                title = "Max Recording Duration",
                value = config.maxRecordingDuration / 1000f,
                valueRange = 5f..60f,
                steps = 10,
                valueText = "${(config.maxRecordingDuration / 1000).toInt()}s",
                onValueChange = { seconds ->
                    config = config.copy(maxRecordingDuration = (seconds * 1000).toLong())
                    onConfigChanged(config)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ConfigSlider(
                title = "Timeout Duration",
                value = config.timeoutDuration / 1000f,
                valueRange = 1f..10f,
                steps = 8,
                valueText = "${(config.timeoutDuration / 1000).toInt()}s",
                onValueChange = { seconds ->
                    config = config.copy(timeoutDuration = (seconds * 1000).toLong())
                    onConfigChanged(config)
                }
            )
        }
        
        // Reset to defaults button
        Button(
            onClick = {
                config = SpeechConfigurationData()
                onConfigChanged(config)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007AFF)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reset to Defaults")
        }
    }
}

/**
 * Configuration panel container
 */
@Composable
fun ConfigPanel(
    title: String,
    glassMorphismConfig: GlassMorphismConfig,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = glassMorphismConfig,
                depth = DepthLevel(1f)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1D1D1F)
        )
        
        content()
    }
}

/**
 * Language selector component
 */
@Composable
fun LanguageSelector(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    val languages = listOf(
        "en-US" to "English (US)",
        "en-GB" to "English (UK)",
        "es-ES" to "Spanish",
        "fr-FR" to "French",
        "de-DE" to "German",
        "it-IT" to "Italian",
        "pt-BR" to "Portuguese",
        "zh-CN" to "Chinese",
        "ja-JP" to "Japanese",
        "ko-KR" to "Korean"
    )
    
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = languages.find { it.first == selectedLanguage }?.second ?: "Select Language"
    
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0x1A007AFF)
            )
        ) {
            Text(
                text = selectedLabel,
                color = Color(0xFF007AFF),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF007AFF)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { 
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label)
                            if (code == selectedLanguage) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onLanguageSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Mode selector component
 */
@Composable
fun ModeSelector(
    selectedMode: SpeechMode,
    onModeSelected: (SpeechMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SpeechMode.values().forEach { mode ->
            val isSelected = mode == selectedMode
            val backgroundColor = if (isSelected) Color(0xFF007AFF) else Color(0x1A007AFF)
            val textColor = if (isSelected) Color.White else Color(0xFF007AFF)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onModeSelected(mode) }
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = when (mode) {
                            SpeechMode.STATIC_COMMAND -> "Static Commands"
                            SpeechMode.DYNAMIC_COMMAND -> "Dynamic Commands"
                            SpeechMode.DICTATION -> "Free Dictation"
                            SpeechMode.FREE_SPEECH -> "Free Speech"
                            SpeechMode.HYBRID -> "Hybrid Mode"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    
                    Text(
                        text = when (mode) {
                            SpeechMode.STATIC_COMMAND -> "Fixed command set only"
                            SpeechMode.DYNAMIC_COMMAND -> "Context-aware commands"
                            SpeechMode.DICTATION -> "General text transcription"
                            SpeechMode.FREE_SPEECH -> "Unrestricted speech input"
                            SpeechMode.HYBRID -> "Commands and free speech"
                        },
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF8E8E93)
                    )
                }
            }
        }
    }
}

/**
 * Configuration switch item
 */
@Composable
fun ConfigSwitch(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1D1D1F)
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color(0xFF8E8E93)
            )
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF007AFF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE5E5EA)
            )
        )
    }
}

/**
 * Configuration slider item
 */
@Composable
fun ConfigSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1D1D1F)
            )
            
            Text(
                text = valueText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF007AFF)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF007AFF),
                activeTrackColor = Color(0xFF007AFF),
                inactiveTrackColor = Color(0xFFE5E5EA)
            )
        )
    }
}