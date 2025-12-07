/**
 * CommandTestingScreen.kt - Voice command testing interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-08-28
 */
package com.augmentalis.voiceoscore.accessibility.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceoscore.accessibility.ui.utils.glassMorphism
import com.augmentalis.voiceoscore.accessibility.ui.utils.GlassMorphismConfig
import com.augmentalis.voiceoscore.accessibility.ui.utils.DepthLevel
import com.augmentalis.voiceoscore.accessibility.ui.utils.ThemeUtils

/**
 * Data class for command test result
 */
data class CommandTestResult(
    val command: String,
    val timestamp: Long,
    val success: Boolean,
    val executionTime: Long,
    val result: String,
    val handlerUsed: String
)

/**
 * Main command testing screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandTestingScreen(
    onExecuteCommand: (String) -> CommandTestResult,
    onBack: () -> Unit
) {
    var commandText by remember { mutableStateOf("") }
    var testResults by remember { mutableStateOf<List<CommandTestResult>>(emptyList()) }
    var isExecuting by remember { mutableStateOf(false) }
    var showPredefinedCommands by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        TestingHeader(onBack = onBack)
        
        // Command input section
        CommandInputSection(
            commandText = commandText,
            onCommandTextChange = { commandText = it },
            onExecuteCommand = {
                if (commandText.isNotBlank() && !isExecuting) {
                    isExecuting = true
                    val result = onExecuteCommand(commandText)
                    testResults = listOf(result) + testResults.take(9) // Keep last 10 results
                    commandText = ""
                    isExecuting = false
                    keyboardController?.hide()
                }
            },
            isExecuting = isExecuting,
            focusRequester = focusRequester
        )
        
        // Quick commands toggle
        QuickCommandsToggle(
            showPredefinedCommands = showPredefinedCommands,
            onToggle = { showPredefinedCommands = !showPredefinedCommands }
        )
        
        // Predefined commands (if shown)
        if (showPredefinedCommands) {
            PredefinedCommandsSection(
                onCommandSelect = { command ->
                    commandText = command
                    showPredefinedCommands = false
                }
            )
        }
        
        // Test results
        TestResultsSection(
            testResults = testResults,
            onClearResults = { testResults = emptyList() }
        )
    }
}

/**
 * Testing screen header
 */
@Composable
fun TestingHeader(onBack: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.12f,
                    borderOpacity = 0.25f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF4CAF50),
                    tintOpacity = 0.18f
                ),
                depth = DepthLevel(0.8f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .glassMorphism(
                        config = GlassMorphismConfig(
                            cornerRadius = 12.dp,
                            backgroundOpacity = 0.1f,
                            borderOpacity = 0.2f,
                            borderWidth = 1.dp,
                            tintColor = Color(0xFF2196F3),
                            tintOpacity = 0.15f
                        ),
                        depth = DepthLevel(0.4f)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF2196F3)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Command Testing",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ThemeUtils.getTextColor(),
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Test voice commands and view real-time results",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ThemeUtils.getSecondaryTextColor()
                )
            }
        }
    }
}

/**
 * Command input section with execute button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandInputSection(
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    onExecuteCommand: () -> Unit,
    isExecuting: Boolean,
    focusRequester: FocusRequester
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF2196F3),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Enter Command",
                style = MaterialTheme.typography.titleLarge,
                color = ThemeUtils.getTextColor(),
                fontWeight = FontWeight.SemiBold
            )
            
            OutlinedTextField(
                value = commandText,
                onValueChange = onCommandTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        text = "Type a voice command...",
                        color = ThemeUtils.getSecondaryTextColor()
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ThemeUtils.getTextColor(),
                    unfocusedTextColor = ThemeUtils.getTextColor(),
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = ThemeUtils.getSecondaryTextColor(),
                    cursorColor = Color(0xFF2196F3)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onExecuteCommand() }),
                enabled = !isExecuting,
                maxLines = 3
            )
            
            Button(
                onClick = onExecuteCommand,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .glassMorphism(
                        config = GlassMorphismConfig(
                            cornerRadius = 12.dp,
                            backgroundOpacity = 0.15f,
                            borderOpacity = 0.3f,
                            borderWidth = 1.dp,
                            tintColor = Color(0xFF4CAF50),
                            tintOpacity = 0.2f
                        ),
                        depth = DepthLevel(0.4f)
                    ),
                enabled = commandText.isNotBlank() && !isExecuting,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Executing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Execute",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Execute Command", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Toggle for showing predefined commands
 */
@Composable
fun QuickCommandsToggle(
    showPredefinedCommands: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = 0.08f,
                    borderOpacity = 0.15f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF673AB7),
                    tintOpacity = 0.12f
                ),
                depth = DepthLevel(0.3f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Quick Commands",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF673AB7)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Quick Commands",
                    style = MaterialTheme.typography.titleMedium,
                    color = ThemeUtils.getTextColor(),
                    fontWeight = FontWeight.Medium
                )
            }
            
            Icon(
                imageVector = if (showPredefinedCommands) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (showPredefinedCommands) "Hide" else "Show",
                tint = ThemeUtils.getSecondaryTextColor()
            )
        }
    }
}

/**
 * Predefined commands section
 */
@Composable
fun PredefinedCommandsSection(onCommandSelect: (String) -> Unit) {
    val predefinedCommands = listOf(
        "open settings" to "App Navigation",
        "scroll down" to "UI Interaction",
        "go back" to "Navigation",
        "tap submit button" to "UI Interaction", 
        "open notifications" to "System Control",
        "increase volume" to "Device Control",
        "type hello world" to "Input Control",
        "take screenshot" to "Device Control",
        "open recent apps" to "System Control",
        "close current app" to "App Control"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF673AB7),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.5f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Example Commands",
                style = MaterialTheme.typography.titleMedium,
                color = ThemeUtils.getTextColor(),
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            predefinedCommands.chunked(2).forEach { commandPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commandPair.forEach { (command, category) ->
                        PredefinedCommandItem(
                            modifier = Modifier.weight(1f),
                            command = command,
                            category = category,
                            onClick = { onCommandSelect(command) }
                        )
                    }
                    // Fill remaining space if odd number
                    if (commandPair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Individual predefined command item
 */
@Composable
fun PredefinedCommandItem(
    modifier: Modifier = Modifier,
    command: String,
    category: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 8.dp,
                    backgroundOpacity = 0.08f,
                    borderOpacity = 0.15f,
                    borderWidth = 0.5.dp,
                    tintColor = getCategoryColor(category),
                    tintOpacity = 0.1f
                ),
                depth = DepthLevel(0.2f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = command,
                style = MaterialTheme.typography.bodyMedium,
                color = ThemeUtils.getTextColor(),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = category,
                style = MaterialTheme.typography.bodySmall,
                color = getCategoryColor(category),
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Test results section with history
 */
@Composable
fun TestResultsSection(
    testResults: List<CommandTestResult>,
    onClearResults: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 16.dp,
                    backgroundOpacity = 0.1f,
                    borderOpacity = 0.2f,
                    borderWidth = 1.dp,
                    tintColor = Color(0xFF009688),
                    tintOpacity = 0.15f
                ),
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Test Results (${testResults.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemeUtils.getTextColor(),
                    fontWeight = FontWeight.SemiBold
                )
                
                if (testResults.isNotEmpty()) {
                    IconButton(
                        onClick = onClearResults,
                        modifier = Modifier
                            .glassMorphism(
                                config = GlassMorphismConfig(
                                    cornerRadius = 8.dp,
                                    backgroundOpacity = 0.1f,
                                    borderOpacity = 0.2f,
                                    borderWidth = 1.dp,
                                    tintColor = Color(0xFFFF5722),
                                    tintOpacity = 0.15f
                                ),
                                depth = DepthLevel(0.3f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Results",
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (testResults.isEmpty()) {
                Text(
                    text = "No test results yet. Execute a command to see results here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeUtils.getSecondaryTextColor(),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(testResults) { result ->
                        TestResultItem(result = result)
                    }
                }
            }
        }
    }
}

/**
 * Individual test result item
 */
@Composable
fun TestResultItem(result: CommandTestResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 12.dp,
                    backgroundOpacity = 0.08f,
                    borderOpacity = 0.15f,
                    borderWidth = 1.dp,
                    tintColor = if (result.success) Color(0xFF4CAF50) else Color(0xFFFF5722),
                    tintOpacity = 0.12f
                ),
                depth = DepthLevel(0.3f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.command,
                        style = MaterialTheme.typography.titleMedium,
                        color = ThemeUtils.getTextColor(),
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Text(
                        text = "Handler: ${result.handlerUsed}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeUtils.getSecondaryTextColor()
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = if (result.success) "Success" else "Failed",
                        modifier = Modifier.size(16.dp),
                        tint = if (result.success) Color(0xFF4CAF50) else Color(0xFFFF5722)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "${result.executionTime}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeUtils.getSecondaryTextColor(),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            if (result.result.isNotBlank()) {
                Text(
                    text = result.result,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (result.success) ThemeUtils.getTextColor() else Color(0xFFFF5722),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
            
            Text(
                text = "Executed ${formatTimestamp(result.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = ThemeUtils.getSecondaryTextColor().copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}

// Helper functions
private fun getCategoryColor(category: String): Color {
    return when (category) {
        "App Navigation" -> Color(0xFF2196F3)
        "UI Interaction" -> Color(0xFF4CAF50)
        "Navigation" -> Color(0xFFFF9800)
        "System Control" -> Color(0xFF9C27B0)
        "Device Control" -> Color(0xFFFF5722)
        "Input Control" -> Color(0xFF00BCD4)
        "App Control" -> Color(0xFF795548)
        else -> Color(0xFF607D8B)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}