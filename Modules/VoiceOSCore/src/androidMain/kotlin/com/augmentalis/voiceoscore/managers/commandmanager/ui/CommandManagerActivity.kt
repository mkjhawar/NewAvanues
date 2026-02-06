/**
 * CommandManagerActivity.kt - Main UI for Command Management
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Provides comprehensive command management interface with glassmorphism design
 * 
 * UI Layout:
 * ┌─────────────────────────────────────────┐
 * │ ╔═══════════════════════════════════════╗ │
 * │ ║           COMMAND MANAGER             ║ │
 * │ ╚═══════════════════════════════════════╝ │
 * │                                         │
 * │ ┌─────────────────────────────────────┐ │
 * │ │        COMMAND STATS CARD           │ │
 * │ │ Total: 127    Success: 98%          │ │
 * │ │ Failed: 3     Avg Time: 45ms        │ │
 * │ └─────────────────────────────────────┘ │
 * │                                         │
 * │ ┌─────────────────────────────────────┐ │
 * │ │        QUICK TEST PANEL             │ │
 * │ │ [Test Command Input Field]          │ │
 * │ │ [Voice Test] [Execute Test]         │ │
 * │ └─────────────────────────────────────┘ │
 * │                                         │
 * │ ┌─────────────────────────────────────┐ │
 * │ │         COMMAND CATEGORIES          │ │
 * │ │ [Navigation] [Text] [Media] [System]│ │
 * │ │ [App Control] [Voice] [Gesture]     │ │
 * │ └─────────────────────────────────────┘ │
 * │                                         │
 * │ ┌─────────────────────────────────────┐ │
 * │ │         COMMAND HISTORY             │ │
 * │ │ • "go back" - Success (23ms)        │ │
 * │ │ • "volume up" - Success (15ms)      │ │
 * │ │ • "scroll down" - Success (31ms)    │ │
 * │ └─────────────────────────────────────┘ │
 * └─────────────────────────────────────────┘
 */
package com.augmentalis.voiceoscore.managers.commandmanager.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.augmentalis.voiceoscore.*
import com.augmentalis.voiceoscore.managers.commandmanager.processor.CommandProcessor
import com.augmentalis.datamanager.ui.glassMorphism
import com.augmentalis.datamanager.ui.DepthLevel
import com.augmentalis.datamanager.ui.GlassMorphismConfig
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main Command Manager Activity
 */
class CommandManagerActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            CommandManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF000000) // Dark background for glassmorphism
                ) {
                    CommandManagerScreen()
                }
            }
        }
    }
}

/**
 * Command Manager Theme
 */
@Composable
fun CommandManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF1976D2),
            onPrimary = Color.White,
            surface = Color(0xFF121212),
            onSurface = Color.White
        ),
        content = content
    )
}

/**
 * Main Command Manager Screen
 */
@Composable
fun CommandManagerScreen(
    viewModel: CommandViewModel = viewModel(
        factory = CommandViewModelFactory(LocalContext.current)
    )
) {
    val stats by viewModel.commandStats.observeAsState(CommandStats(0, 0, 0, 0, emptyList()))
    val history by viewModel.commandHistory.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        HeaderSection()
        
        // Error message
        errorMessage?.let { error ->
            ErrorCard(
                message = error,
                onDismiss = { viewModel.clearError() }
            )
        }
        
        // Command Stats Card
        CommandStatsCard(
            stats = stats,
            onRefresh = { viewModel.refreshStats() }
        )
        
        // Quick Test Panel
        QuickTestPanel(
            isLoading = isLoading,
            onTestCommand = { command, source -> viewModel.testCommand(command, source) },
            onVoiceTest = { viewModel.startVoiceTest() }
        )
        
        // Command Categories
        CommandCategoriesCard(
            onCategorySelected = { category -> viewModel.showCategoryCommands(category) }
        )
        
        // Command History
        CommandHistoryCard(
            history = history,
            onClearHistory = { viewModel.clearHistory() }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Header Section
 */
@Composable
private fun HeaderSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = CommandGlassConfigs.Primary,
                depth = DepthLevel(0.8f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = "Command Manager",
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF1976D2)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Command Manager",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Configure, test, and monitor voice commands",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Error Card
 */
@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = CommandGlassConfigs.Error,
                depth = DepthLevel(0.7f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = CommandColors.StatusError,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Command Stats Card
 */
@Composable
internal fun CommandStatsCard(
    stats: CommandStats,
    onRefresh: () -> Unit
) {
    val successRate = if (stats.totalCommands > 0) {
        (stats.successfulCommands.toFloat() / stats.totalCommands * 100).toInt()
    } else 0
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = CommandGlassConfigs.Success,
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
                    text = "Command Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total",
                    value = "${stats.totalCommands}",
                    icon = Icons.Default.AllInbox
                )
                
                StatItem(
                    label = "Success Rate",
                    value = "$successRate%",
                    icon = Icons.Default.CheckCircle,
                    valueColor = CommandColors.StatusActive
                )
                
                StatItem(
                    label = "Failed",
                    value = "${stats.failedCommands}",
                    icon = Icons.Default.Error,
                    valueColor = if (stats.failedCommands > 0) CommandColors.StatusError else Color.White
                )
                
                StatItem(
                    label = "Avg Time",
                    value = "${stats.averageExecutionTime}ms",
                    icon = Icons.Default.Timer
                )
            }
        }
    }
}

/**
 * Stat Item Component
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = valueColor
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * Quick Test Panel
 */
@Composable
internal fun QuickTestPanel(
    isLoading: Boolean,
    onTestCommand: (String, CommandSource) -> Unit,
    onVoiceTest: () -> Unit
) {
    var testCommand by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = CommandGlassConfigs.Info,
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Quick Test",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Test Command Input
            OutlinedTextField(
                value = testCommand,
                onValueChange = { testCommand = it },
                label = { Text("Enter command to test", color = Color.White.copy(alpha = 0.7f)) },
                placeholder = { Text("e.g., go back, volume up, scroll down") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.7f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (testCommand.isNotBlank()) {
                            onTestCommand(testCommand, CommandSource.TEXT)
                        }
                    }
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Test Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Voice Test Button
                Button(
                    onClick = onVoiceTest,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CommandColors.CategoryVoice.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Test",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Voice Test")
                }
                
                // Execute Test Button
                Button(
                    onClick = {
                        if (testCommand.isNotBlank()) {
                            onTestCommand(testCommand, CommandSource.TEXT)
                        }
                    },
                    enabled = testCommand.isNotBlank() && !isLoading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CommandColors.StatusActive.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Execute",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Execute")
                }
            }
        }
    }
}

/**
 * Command Categories Card
 */
@Composable
internal fun CommandCategoriesCard(
    onCategorySelected: (CommandCategory) -> Unit
) {
    val categories = listOf(
        CommandCategory.NAVIGATION to Icons.Default.Navigation,
        CommandCategory.ACCESSIBILITY to Icons.Default.TextFields,
        CommandCategory.MEDIA to Icons.Default.PlayArrow,
        CommandCategory.SYSTEM to Icons.Default.Settings,
        CommandCategory.APP_LAUNCH to Icons.Default.Apps,
        CommandCategory.VOICE_CONTROL to Icons.Default.Mic,
        CommandCategory.CUSTOM to Icons.Default.Build
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = CommandGlassConfigs.Primary,
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Command Categories",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Categories Grid
            for (row in categories.chunked(4)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((category, icon) in row) {
                        CategoryButton(
                            category = category,
                            icon = icon,
                            modifier = Modifier.weight(1f),
                            onClick = { onCategorySelected(category) }
                        )
                    }
                    // Fill remaining space if needed
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                if (row != categories.chunked(4).last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Category Button Component
 */
@Composable
private fun CategoryButton(
    category: CommandCategory,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val categoryColor = when (category) {
        CommandCategory.NAVIGATION -> CommandColors.CategoryNavigation
        CommandCategory.ACCESSIBILITY -> CommandColors.CategoryText
        CommandCategory.MEDIA -> CommandColors.CategoryMedia
        CommandCategory.SYSTEM -> CommandColors.CategorySystem
        CommandCategory.APP_LAUNCH -> CommandColors.CategoryApp
        CommandCategory.VOICE_CONTROL -> CommandColors.CategoryVoice
        CommandCategory.CUSTOM -> CommandColors.CategoryCustom
        else -> CommandColors.CategoryCustom
    }
    
    Card(
        modifier = modifier
            .clickable { onClick() }
            .glassMorphism(
                config = GlassMorphismConfig(
                    tintColor = categoryColor,
                    cornerRadius = 12.dp
                ),
                depth = DepthLevel(0.4f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = category.name,
                modifier = Modifier.size(24.dp),
                tint = categoryColor
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = category.name.lowercase().replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase() else it.toString() 
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Command History Card
 */
@Composable
internal fun CommandHistoryCard(
    history: List<CommandHistoryEntry>,
    onClearHistory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = CommandGlassConfigs.Info.copy(backgroundOpacity = 0.05f),
                depth = DepthLevel(0.4f)
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
                    text = "Recent Commands",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text(
                            "Clear",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (history.isEmpty()) {
                Text(
                    text = "No commands executed yet. Try testing a command above!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history.take(10)) { entry ->
                        CommandHistoryItem(entry)
                    }
                }
            }
        }
    }
}

/**
 * Command History Item
 */
@Composable
private fun CommandHistoryItem(entry: CommandHistoryEntry) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Icon
        Icon(
            imageVector = if (entry.result.success) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = if (entry.result.success) "Success" else "Failed",
            modifier = Modifier.size(16.dp),
            tint = if (entry.result.success) CommandColors.StatusActive else CommandColors.StatusError
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Command Text
        Text(
            text = "\"${entry.command.text}\"",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Execution Time
        Text(
            text = "${entry.result.executionTime}ms",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Timestamp
        Text(
            text = timeFormat.format(Date(entry.timestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

