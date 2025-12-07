package com.augmentalis.voiceui.windows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.augmentalis.voiceui.api.*
import com.augmentalis.voiceui.dsl.MagicScreen
import com.augmentalis.voiceui.layout.MagicColumn
import com.augmentalis.voiceui.layout.MagicPadding
import com.augmentalis.voiceui.theme.*
import com.augmentalis.voiceui.widgets.*
import com.augmentalis.voiceui.widgets.MagicRow

/**
 * MagicWindowExamples - Demonstration of freeform window capabilities
 */

@Composable
fun MagicWindowShowcase() {
    var showChatWindow by remember { mutableStateOf(false) }
    var showSettingsWindow by remember { mutableStateOf(false) }
    var showCodeEditor by remember { mutableStateOf(false) }
    var showMediaPlayer by remember { mutableStateOf(false) }
    var showNotification by remember { mutableStateOf(false) }
    
    val theme = remember { MagicThemeData() }
    
    // Main screen with window launcher
    MagicScreen(
        name = "Window Showcase",
        description = "Freeform window system demonstration"
    ) {
        MagicColumn(gap = 16.dp) {
            MagicCard(
                modifier = Modifier.fillMaxWidth(),
                theme = theme
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Magic Window System",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Launch different window types to see the freeform window system in action",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            // Window launcher buttons
            MagicRow(spacing = 12.dp) {
                MagicButton(
                    text = "Chat Window",
                    onClick = { showChatWindow = true },
                    icon = Icons.Default.Email
                )
                MagicButton(
                    text = "Settings",
                    onClick = { showSettingsWindow = true },
                    icon = Icons.Default.Settings
                )
            }
            
            MagicRow(spacing = 12.dp) {
                MagicButton(
                    text = "Code Editor",
                    onClick = { showCodeEditor = true },
                    icon = Icons.Default.Build
                )
                MagicButton(
                    text = "Media Player",
                    onClick = { showMediaPlayer = true },
                    icon = Icons.Default.PlayArrow
                )
            }
            
            MagicButton(
                text = "Show Notification",
                onClick = { showNotification = true },
                icon = Icons.Default.Notifications,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    
    // Render windows
    if (showChatWindow) {
        ChatWindow(
            theme = theme,
            onClose = { showChatWindow = false }
        )
    }
    
    if (showSettingsWindow) {
        SettingsWindow(
            theme = theme,
            onClose = { showSettingsWindow = false }
        )
    }
    
    if (showCodeEditor) {
        CodeEditorWindow(
            theme = theme,
            onClose = { showCodeEditor = false }
        )
    }
    
    if (showMediaPlayer) {
        MediaPlayerWindow(
            theme = theme,
            onClose = { showMediaPlayer = false }
        )
    }
    
    if (showNotification) {
        NotificationWindow(
            theme = theme,
            onClose = { showNotification = false }
        )
    }
}

@Composable
private fun ChatWindow(
    theme: MagicThemeData,
    onClose: () -> Unit
) {
    AnimatedMagicWindow(
        title = "Magic Chat",
        position = Offset(100f, 100f),
        size = DpSize(400.dp, 500.dp),
        animation = WindowAnimation.SCALE,
        theme = theme,
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Chat messages area
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = theme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChatMessage(
                        sender = "AI Assistant",
                        message = "Hello! How can I help you today?",
                        isAi = true,
                        theme = theme
                    )
                    ChatMessage(
                        sender = "User",
                        message = "I need help with the freeform windows",
                        isAi = false,
                        theme = theme
                    )
                    ChatMessage(
                        sender = "AI Assistant",
                        message = "The MagicWindow system supports dragging, resizing, minimizing, and maximizing. Try dragging this window around!",
                        isAi = true,
                        theme = theme
                    )
                }
            }
            
            // Input area
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(
                    onClick = {},
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send")
                }
            }
        }
    }
}

@Composable
private fun ChatMessage(
    sender: String,
    message: String,
    isAi: Boolean,
    theme: MagicThemeData
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAi) {
                    theme.primary.copy(alpha = 0.1f)
                } else {
                    theme.secondary.copy(alpha = 0.1f)
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    sender,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isAi) theme.primary else theme.secondary
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsWindow(
    theme: MagicThemeData,
    onClose: () -> Unit
) {
    AnimatedMagicWindow(
        title = "Settings",
        position = Offset(520f, 100f),
        size = DpSize(350.dp, 400.dp),
        animation = WindowAnimation.FADE,
        config = MagicWindowPresets.dialog,
        theme = theme,
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingSection(
                title = "Appearance",
                icon = Icons.Default.Settings,
                theme = theme
            ) {
                SwitchRow("Dark Mode", true, {})
                SwitchRow("Animations", true, {})
                SwitchRow("Glassmorphism", true, {})
            }
            
            SettingSection(
                title = "Window Behavior",
                icon = Icons.Default.Settings,
                theme = theme
            ) {
                SwitchRow("Snap to Edges", true, {})
                SwitchRow("Show Shadows", true, {})
                SwitchRow("Enable Transparency", false, {})
            }
            
            SettingSection(
                title = "Voice Commands",
                icon = Icons.Default.Settings,
                theme = theme
            ) {
                SwitchRow("Voice Control", true, {})
                SwitchRow("Audio Feedback", false, {})
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: MagicThemeData,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = theme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = theme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun CodeEditorWindow(
    theme: MagicThemeData,
    onClose: () -> Unit
) {
    AnimatedMagicWindow(
        title = "Magic Code Editor",
        position = Offset(200f, 150f),
        size = DpSize(600.dp, 400.dp),
        animation = WindowAnimation.SLIDE,
        theme = theme,
        onClose = onClose
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.surface)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Settings, "Save", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Settings, "Copy", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Settings, "Paste", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Undo", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Redo", modifier = Modifier.size(20.dp))
                }
            }
            
            // Code area
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2B2D30)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    CodeLine("@Composable", Color(0xFFCC7832))
                    CodeLine("fun MagicWindow(", Color(0xFFA9B7C6))
                    CodeLine("    title: String,", Color(0xFFA9B7C6))
                    CodeLine("    theme: MagicThemeData", Color(0xFFA9B7C6))
                    CodeLine(") {", Color(0xFFA9B7C6))
                    CodeLine("    // Magic happens here", Color(0xFF629755))
                    CodeLine("    Box {", Color(0xFFA9B7C6))
                    CodeLine("        content()", Color(0xFFA9B7C6))
                    CodeLine("    }", Color(0xFFA9B7C6))
                    CodeLine("}", Color(0xFFA9B7C6))
                }
            }
        }
    }
}

@Composable
private fun CodeLine(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun MediaPlayerWindow(
    theme: MagicThemeData,
    onClose: () -> Unit
) {
    AnimatedMagicWindow(
        title = "Media Player",
        position = Offset(300f, 200f),
        size = DpSize(450.dp, 300.dp),
        animation = WindowAnimation.BOUNCE,
        config = MagicWindowPresets.toolWindow,
        theme = theme,
        onClose = onClose
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album art area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(theme.gradientStart, theme.gradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }
            }
            
            // Track info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Magic Symphony",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "VoiceUI Orchestra",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.textSecondary
                )
            }
            
            // Progress bar
            LinearProgressIndicator(
                progress = { 0.3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = theme.primary,
                trackColor = theme.primary.copy(alpha = 0.2f)
            )
            
            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous")
                }
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(56.dp)
                        .background(theme.primary, CircleShape)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
                }
            }
        }
    }
}

@Composable
private fun NotificationWindow(
    theme: MagicThemeData,
    onClose: () -> Unit
) {
    AnimatedMagicWindow(
        title = "",
        position = Offset(1500f, 50f),
        size = DpSize(300.dp, 100.dp),
        animation = WindowAnimation.SLIDE,
        config = MagicWindowPresets.notification,
        theme = theme,
        onClose = onClose
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            theme.primary.copy(alpha = 0.9f),
                            theme.secondary.copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Success!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Window system is working perfectly",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    "Close",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
    
    // Auto-close after 3 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        onClose()
    }
}