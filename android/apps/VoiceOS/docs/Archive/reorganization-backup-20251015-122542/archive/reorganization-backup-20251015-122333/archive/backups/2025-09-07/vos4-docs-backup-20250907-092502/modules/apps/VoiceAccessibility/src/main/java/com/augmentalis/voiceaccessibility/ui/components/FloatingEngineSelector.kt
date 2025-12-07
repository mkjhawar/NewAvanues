/**
 * FloatingEngineSelector.kt
 * 
 * Purpose: Floating UI for quick engine selection during testing
 * Shows engine initials in a side container for easy switching
 */
package com.augmentalis.voiceaccessibility.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Data class representing an engine option
 */
data class EngineOption(
    val id: String,
    val initial: String,
    val fullName: String,
    val color: Color
)

/**
 * Floating engine selector for testing
 * Displays engine initials in circles for quick selection
 */
@Composable
fun FloatingEngineSelector(
    selectedEngine: String,
    onEngineSelected: (String) -> Unit,
    onInitiate: (String) -> Unit,
    isRecognizing: Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    
    // Define available engines with colors
    val engines = remember {
        listOf(
            EngineOption("vivoka", "V", "Vivoka", Color(0xFF4CAF50)),      // Green
            EngineOption("vosk", "K", "Vosk", Color(0xFF2196F3)),          // Blue
            EngineOption("android_stt", "A", "Android", Color(0xFFFF9800)), // Orange
            EngineOption("whisper", "W", "Whisper", Color(0xFF9C27B0)),    // Purple
            EngineOption("google_cloud", "G", "Google", Color(0xFFF44336))  // Red
        )
    }
    
    // Find current engine
    val currentEngine = engines.find { it.id == selectedEngine } ?: engines[0]
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 100.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Expanded engine selector
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                Card(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Engines",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        HorizontalDivider(modifier = Modifier.width(40.dp))
                        
                        // Engine selection buttons
                        engines.forEach { engine ->
                            EngineButton(
                                engine = engine,
                                isSelected = engine.id == selectedEngine,
                                onClick = {
                                    onEngineSelected(engine.id)
                                }
                            )
                        }
                        
                        HorizontalDivider(modifier = Modifier.width(40.dp))
                        
                        // Initiate button
                        InitiateButton(
                            isRecognizing = isRecognizing,
                            onClick = {
                                scope.launch {
                                    onInitiate(selectedEngine)
                                }
                            }
                        )
                    }
                }
            }
            
            // Floating toggle button (always visible)
            FloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.size(56.dp),
                containerColor = currentEngine.color,
                contentColor = Color.White
            ) {
                if (isExpanded) {
                    Text(
                        currentEngine.initial,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Select Engine"
                    )
                }
            }
        }
    }
}

/**
 * Individual engine button
 */
@Composable
private fun EngineButton(
    engine: EngineOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) engine.color else engine.color.copy(alpha = 0.3f)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                engine.initial,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Text(
                    "●",
                    color = Color.White,
                    fontSize = 8.sp,
                    modifier = Modifier.offset(y = (-4).dp)
                )
            }
        }
    }
}

/**
 * Initiate/Stop button
 */
@Composable
private fun InitiateButton(
    isRecognizing: Boolean,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        containerColor = if (isRecognizing) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = if (isRecognizing) "Stop" else "Start",
            tint = Color.White
        )
    }
}

/**
 * Preview for development
 */
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun FloatingEngineSelectorPreview() {
    var selectedEngine by remember { mutableStateOf("vivoka") }
    var isRecognizing by remember { mutableStateOf(false) }
    
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)
        ) {
            FloatingEngineSelector(
                selectedEngine = selectedEngine,
                onEngineSelected = { selectedEngine = it },
                onInitiate = { 
                    isRecognizing = !isRecognizing
                },
                isRecognizing = isRecognizing
            )
        }
    }
}