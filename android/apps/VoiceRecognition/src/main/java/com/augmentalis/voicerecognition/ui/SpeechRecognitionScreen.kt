/**
 * SpeechRecognitionScreen.kt - Main UI screen for speech recognition
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-28
 * 
 * Purpose: Main UI screen with VoiceCursor-style glassmorphism and ARVision theming
 * Direct implementation following VOS4 architecture standards
 */
package com.augmentalis.voicerecognition.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.augmentalis.speechrecognition.SpeechEngine
import com.augmentalis.voicerecognition.viewmodel.SpeechViewModel
import kotlinx.coroutines.launch

/**
 * Main speech recognition screen with VoiceCursor UI style
 */
@Composable
fun SpeechRecognitionScreen(
    viewModel: SpeechViewModel,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showConfigScreen by remember { mutableStateOf(false) }
    
    // Show configuration screen if requested
    if (showConfigScreen) {
        val currentConfig = SpeechConfigurationData(
            language = "en-US",
            confidenceThreshold = 0.7f,
            timeoutDuration = 5000,
            maxRecordingDuration = 30000,
            enableProfanityFilter = false,
            enableVAD = true
        )
        
        ConfigurationScreen(
            currentConfig = currentConfig,
            onConfigChanged = { config ->
                viewModel.updateConfiguration(config)
            },
            onBackPressed = {
                showConfigScreen = false
            }
        )
        return
    }
    
    // Glass morphism configuration
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
        // Title Header with Settings Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Voice Recognition",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D1D1F)
            )
            
            IconButton(
                onClick = { showConfigScreen = true },
                modifier = Modifier.semantics { contentDescription = "Voice: click Speech Settings" }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Text(
            text = "Speech Recognition System",
            fontSize = 16.sp,
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Permission Check
        if (!hasAudioPermission) {
            PermissionRequestCard(
                glassMorphismConfig = glassMorphismConfig,
                onRequestPermission = onRequestPermission
            )
        } else {
            // Engine Selection Panel
            EngineSelectionPanel(
                glassMorphismConfig = glassMorphismConfig,
                selectedEngine = uiState.selectedEngine,
                onEngineSelected = { engine ->
                    scope.launch {
                        viewModel.initializeEngine(engine)
                    }
                }
            )
            
            // Status Panel
            StatusPanel(
                glassMorphismConfig = glassMorphismConfig,
                engineStatus = uiState.engineStatus,
                isInitialized = uiState.isInitialized,
                errorMessage = uiState.errorMessage
            )
            
            // Recording Control
            RecordingControlPanel(
                glassMorphismConfig = glassMorphismConfig,
                isListening = uiState.isListening,
                isInitialized = uiState.isInitialized,
                onStartListening = { viewModel.startListening() },
                onStopListening = { viewModel.stopListening() }
            )
            
            // Transcript Display
            TranscriptPanel(
                glassMorphismConfig = glassMorphismConfig,
                currentTranscript = uiState.currentTranscript,
                fullTranscript = uiState.fullTranscript,
                confidence = uiState.confidence,
                onClearTranscript = { viewModel.clearTranscript() }
            )
        }
    }
}

/**
 * Permission request card
 */
@Composable
fun PermissionRequestCard(
    glassMorphismConfig: GlassMorphismConfig,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = glassMorphismConfig,
                depth = DepthLevel(1f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Microphone",
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFFF3B30)
            )
            
            Text(
                text = "Microphone Permission Required",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1D1D1F)
            )
            
            Text(
                text = "This app needs microphone access to recognize speech",
                fontSize = 14.sp,
                color = Color(0xFF8E8E93),
                textAlign = TextAlign.Center
            )
            
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Voice: click Grant Microphone Permission" }
            ) {
                Text("Grant Permission")
            }
        }
    }
}

/**
 * Engine selection panel
 */
@Composable
fun EngineSelectionPanel(
    glassMorphismConfig: GlassMorphismConfig,
    selectedEngine: SpeechEngine,
    onEngineSelected: (SpeechEngine) -> Unit
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
            text = "Speech Engine",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1D1D1F)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeechEngine.values().forEach { engine ->
                EngineChip(
                    engine = engine,
                    isSelected = engine == selectedEngine,
                    onClick = { onEngineSelected(engine) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Engine selection chip
 */
@Composable
fun EngineChip(
    engine: SpeechEngine,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        Color(0xFF007AFF)
    } else {
        Color(0x1A007AFF)
    }
    
    val textColor = if (isSelected) {
        Color.White
    } else {
        Color(0xFF007AFF)
    }
    
    val engineLabel = when (engine) {
        SpeechEngine.ANDROID_STT -> "Android"
        SpeechEngine.VOSK -> "VOSK"
        SpeechEngine.VIVOKA -> "Vivoka"
        SpeechEngine.GOOGLE_CLOUD -> "Cloud"
        SpeechEngine.WHISPER -> "Whisper"
        SpeechEngine.AZURE -> "Azure"
        SpeechEngine.APPLE_SPEECH -> "Apple"
        SpeechEngine.WEB_SPEECH -> "Web"
    }
    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .semantics { contentDescription = "Voice: select $engineLabel Speech Engine" }
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = engineLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * Status panel
 */
@Composable
fun StatusPanel(
    glassMorphismConfig: GlassMorphismConfig,
    engineStatus: String,
    isInitialized: Boolean,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = glassMorphismConfig,
                depth = DepthLevel(1f)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = when {
                            errorMessage != null -> Color(0xFFFF3B30)
                            isInitialized -> Color(0xFF34C759)
                            else -> Color(0xFFFF9500)
                        },
                        shape = CircleShape
                    )
            )
            
            Text(
                text = "Status",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1D1D1F)
            )
        }
        
        Text(
            text = errorMessage ?: engineStatus,
            fontSize = 14.sp,
            color = if (errorMessage != null) Color(0xFFFF3B30) else Color(0xFF8E8E93)
        )
    }
}

/**
 * Recording control panel
 */
@Composable
fun RecordingControlPanel(
    glassMorphismConfig: GlassMorphismConfig,
    isListening: Boolean,
    isInitialized: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = glassMorphismConfig,
                depth = DepthLevel(1f)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FloatingActionButton(
            onClick = {
                if (isListening) {
                    onStopListening()
                } else {
                    onStartListening()
                }
            },
            containerColor = if (isListening) Color(0xFFFF3B30) else Color(0xFF007AFF),
            modifier = Modifier
                .size(80.dp)
                .scale(if (isListening) scale else 1f)
                .semantics {
                    contentDescription = if (isListening) "Voice: click Stop Listening" else "Voice: click Start Listening"
                },
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop" else "Start",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        
        if (!isInitialized) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Initialize Engine First",
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Transcript display panel
 */
@Composable
fun TranscriptPanel(
    glassMorphismConfig: GlassMorphismConfig,
    currentTranscript: String,
    fullTranscript: String,
    confidence: Float,
    onClearTranscript: () -> Unit
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transcript",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1D1D1F)
            )
            
            if (fullTranscript.isNotEmpty() || currentTranscript.isNotEmpty()) {
                IconButton(
                    onClick = onClearTranscript,
                    modifier = Modifier.semantics { contentDescription = "Voice: click Clear Transcript" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color(0xFF007AFF)
                    )
                }
            }
        }
        
        // Confidence indicator
        if (confidence > 0) {
            LinearProgressIndicator(
                progress = { confidence },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF34C759),
                trackColor = Color(0xFFE5E5EA)
            )
            Text(
                text = "Confidence: ${(confidence * 100).toInt()}%",
                fontSize = 12.sp,
                color = Color(0xFF8E8E93)
            )
        }
        
        // Current transcript (partial)
        if (currentTranscript.isNotEmpty()) {
            Text(
                text = currentTranscript,
                fontSize = 16.sp,
                color = Color(0xFF8E8E93),
                fontWeight = FontWeight.Normal
            )
        }
        
        // Full transcript
        if (fullTranscript.isNotEmpty()) {
            HorizontalDivider(color = Color(0xFFE5E5EA))
            Text(
                text = fullTranscript,
                fontSize = 16.sp,
                color = Color(0xFF1D1D1F),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 300.dp)
                    .verticalScroll(rememberScrollState())
                    .background(
                        Color(0x0D007AFF),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            )
        } else if (currentTranscript.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Color(0x0D007AFF),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Transcript will appear here",
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}