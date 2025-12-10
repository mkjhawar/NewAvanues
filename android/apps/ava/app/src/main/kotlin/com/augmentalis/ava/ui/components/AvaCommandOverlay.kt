/**
 * Voice Command Overlay for AVA
 *
 * Provides a zero-space, voice-first command interface that slides up on demand.
 * All commands are visible in a uniform grid with cascading navigation.
 *
 * Command hierarchy:
 * - Master: Chat, Teach, Models, Voice, Settings
 * - Chat: New, History, Clear, Export, Templates
 * - Teach: Add Example, View Examples, Categories, Import
 * - Models: Download, Select, Configure, Test
 * - Voice: Enable NLU, Threshold, Language, Test
 * - Settings: Theme, Privacy, Storage, About
 *
 * Created: 2025-11-08
 * Author: AVA Team
 * Based on: Voice Command Overlay Pattern (NewAvanue/Browser module)
 */

package com.augmentalis.ava.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AVA command events
 */
sealed class AvaCommand {
    // Chat commands
    object NewConversation : AvaCommand()
    object ShowHistory : AvaCommand()
    object ClearChat : AvaCommand()
    object ExportChat : AvaCommand()
    object ShowTemplates : AvaCommand()
    object StopGeneration : AvaCommand()

    // Teach commands
    object AddExample : AvaCommand()
    object ViewExamples : AvaCommand()
    object ManageCategories : AvaCommand()
    object ImportExamples : AvaCommand()
    object ExportExamples : AvaCommand()

    // Model commands
    object DownloadModels : AvaCommand()
    object SelectModel : AvaCommand()
    object ModelSettings : AvaCommand()
    object RunTests : AvaCommand()
    object ViewModelInfo : AvaCommand()

    // Voice/NLU commands
    object ToggleNLU : AvaCommand()
    object SetConfidenceThreshold : AvaCommand()
    object SelectLanguage : AvaCommand()
    object TestVoice : AvaCommand()
    object ViewNLUStats : AvaCommand()
    object ReloadNLUData : AvaCommand()

    // Settings commands
    object ChangeTheme : AvaCommand()
    object PrivacySettings : AvaCommand()
    object ClearCache : AvaCommand()
    object ViewAbout : AvaCommand()
    object OpenSettings : AvaCommand()
}

/**
 * AVA Voice Command Overlay
 *
 * @param visible Whether overlay is visible
 * @param onCommand Callback when a command is triggered
 * @param onDismiss Callback when overlay is dismissed
 */
@Composable
fun AvaCommandOverlay(
    visible: Boolean,
    onCommand: (AvaCommand) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentLevel by remember { mutableStateOf("master") }
    var isListening by remember { mutableStateOf(false) }

    // Reset to master when overlay becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            currentLevel = "master"
            isListening = false
        }
    }

    // Slide animation
    val slideOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        label = "overlay_slide"
    )

    // Only render when partially visible
    if (slideOffset < 1f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .offset(y = (slideOffset * 1000).dp)
        ) {
            // Scrim overlay - dimmed background for focus
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.50f))
                    .clickable { onDismiss() } // Tap scrim to dismiss
            )

            // Command panel - solid Ocean gradient, no glass layering
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFF0F172A)) // Solid Ocean mid-tone
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            ) {
                // Header with navigation
                CommandHeader(
                    currentLevel = currentLevel,
                    isListening = isListening,
                    onBack = { currentLevel = getParentLevel(currentLevel) },
                    onVoiceToggle = { isListening = !isListening },
                    onDismiss = onDismiss
                )

                // Command content (switches based on level)
                when (currentLevel) {
                    "master" -> MasterCommands(
                        onNavigate = { level -> currentLevel = level }
                    )
                    "chat" -> ChatCommands(
                        onCommand = onCommand,
                        onDismiss = onDismiss
                    )
                    "teach" -> TeachCommands(
                        onCommand = onCommand,
                        onDismiss = onDismiss
                    )
                    "models" -> ModelsCommands(
                        onCommand = onCommand,
                        onDismiss = onDismiss
                    )
                    "voice" -> VoiceCommands(
                        onCommand = onCommand,
                        onDismiss = onDismiss
                    )
                    "settings" -> SettingsCommands(
                        onCommand = onCommand,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

/**
 * Command header - compact with drag handle
 */
@Composable
private fun CommandHeader(
    currentLevel: String,
    isListening: Boolean,
    onBack: () -> Unit,
    onVoiceToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showBackButton = currentLevel != "master"
    val title = getLevelTitle(currentLevel)

    Column(modifier = modifier.fillMaxWidth()) {
        // Drag handle indicator
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
                .size(width = 32.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.3f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Back button (conditional)
            if (showBackButton) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // Voice toggle button - primary action
            IconButton(
                onClick = onVoiceToggle,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (isListening) Color(0xFFEF4444) else Color(0xFF3B82F6)
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = if (isListening) "Stop listening" else "Start listening",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Command chip/button - Compact solid style
 * Compact: 56x52dp, Icon: 20dp, no glass layering
 */
@Composable
private fun CommandChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .size(width = 56.dp, height = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E293B)) // Solid lighter Ocean tone
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Command data class - uses ImageVector for outlined icons
 */
private data class Command(
    val icon: ImageVector,
    val label: String,
    val action: () -> Unit
)

/**
 * Command grid layout - compact with minimal padding
 */
@Composable
private fun CommandGrid(
    commands: List<Command>,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Adaptive columns based on orientation
    val columns = if (isLandscape) 6 else 5

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        userScrollEnabled = false
    ) {
        items(commands) { command ->
            CommandChip(
                icon = command.icon,
                label = command.label,
                onClick = command.action
            )
        }
    }
}

/**
 * Master commands (top level categories) - WebAvanue style outlined icons
 */
@Composable
private fun MasterCommands(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember {
        listOf(
            Command(Icons.Outlined.Chat, "Chat") { onNavigate("chat") },
            Command(Icons.Outlined.School, "Teach") { onNavigate("teach") },
            Command(Icons.Outlined.Memory, "Models") { onNavigate("models") },
            Command(Icons.Outlined.Mic, "Voice") { onNavigate("voice") },
            Command(Icons.Outlined.Settings, "Settings") { onNavigate("settings") }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}

/**
 * Chat commands - outlined icons
 */
@Composable
private fun ChatCommands(
    onCommand: (AvaCommand) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onCommand, onDismiss) {
        listOf(
            Command(Icons.Outlined.Add, "New") {
                onCommand(AvaCommand.NewConversation)
                onDismiss()
            },
            Command(Icons.Outlined.History, "History") {
                onCommand(AvaCommand.ShowHistory)
                onDismiss()
            },
            Command(Icons.Outlined.Delete, "Clear") {
                onCommand(AvaCommand.ClearChat)
                onDismiss()
            },
            Command(Icons.Outlined.Share, "Export") {
                onCommand(AvaCommand.ExportChat)
                onDismiss()
            },
            Command(Icons.Outlined.List, "Templates") {
                onCommand(AvaCommand.ShowTemplates)
                onDismiss()
            },
            Command(Icons.Outlined.Stop, "Stop") {
                onCommand(AvaCommand.StopGeneration)
                onDismiss()
            }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}

/**
 * Teach commands - outlined icons
 */
@Composable
private fun TeachCommands(
    onCommand: (AvaCommand) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onCommand, onDismiss) {
        listOf(
            Command(Icons.Outlined.Add, "Add") {
                onCommand(AvaCommand.AddExample)
                onDismiss()
            },
            Command(Icons.Outlined.List, "View") {
                onCommand(AvaCommand.ViewExamples)
                onDismiss()
            },
            Command(Icons.Outlined.Folder, "Categories") {
                onCommand(AvaCommand.ManageCategories)
                onDismiss()
            },
            Command(Icons.Outlined.Download, "Import") {
                onCommand(AvaCommand.ImportExamples)
                onDismiss()
            },
            Command(Icons.Outlined.Upload, "Export") {
                onCommand(AvaCommand.ExportExamples)
                onDismiss()
            }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}

/**
 * Models commands - outlined icons
 */
@Composable
private fun ModelsCommands(
    onCommand: (AvaCommand) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onCommand, onDismiss) {
        listOf(
            Command(Icons.Outlined.Download, "Download") {
                onCommand(AvaCommand.DownloadModels)
                onDismiss()
            },
            Command(Icons.Outlined.SwapHoriz, "Select") {
                onCommand(AvaCommand.SelectModel)
                onDismiss()
            },
            Command(Icons.Outlined.Tune, "Config") {
                onCommand(AvaCommand.ModelSettings)
                onDismiss()
            },
            Command(Icons.Outlined.PlayArrow, "Test") {
                onCommand(AvaCommand.RunTests)
                onDismiss()
            },
            Command(Icons.Outlined.Info, "Info") {
                onCommand(AvaCommand.ViewModelInfo)
                onDismiss()
            }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}

/**
 * Voice/NLU commands - outlined icons
 */
@Composable
private fun VoiceCommands(
    onCommand: (AvaCommand) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onCommand, onDismiss) {
        listOf(
            Command(Icons.Outlined.Mic, "NLU") {
                onCommand(AvaCommand.ToggleNLU)
                onDismiss()
            },
            Command(Icons.Outlined.Speed, "Threshold") {
                onCommand(AvaCommand.SetConfidenceThreshold)
                onDismiss()
            },
            Command(Icons.Outlined.Language, "Language") {
                onCommand(AvaCommand.SelectLanguage)
                onDismiss()
            },
            Command(Icons.Outlined.RecordVoiceOver, "Test") {
                onCommand(AvaCommand.TestVoice)
                onDismiss()
            },
            Command(Icons.Outlined.Analytics, "Stats") {
                onCommand(AvaCommand.ViewNLUStats)
                onDismiss()
            },
            Command(Icons.Outlined.Refresh, "Reload") {
                onCommand(AvaCommand.ReloadNLUData)
                onDismiss()
            }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}

/**
 * Settings commands - outlined icons
 */
@Composable
private fun SettingsCommands(
    onCommand: (AvaCommand) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onCommand, onDismiss) {
        listOf(
            Command(Icons.Outlined.Palette, "Theme") {
                onCommand(AvaCommand.ChangeTheme)
                onDismiss()
            },
            Command(Icons.Outlined.Lock, "Privacy") {
                onCommand(AvaCommand.PrivacySettings)
                onDismiss()
            },
            Command(Icons.Outlined.Delete, "Cache") {
                onCommand(AvaCommand.ClearCache)
                onDismiss()
            },
            Command(Icons.Outlined.Info, "About") {
                onCommand(AvaCommand.ViewAbout)
                onDismiss()
            },
            Command(Icons.Outlined.Settings, "Settings") {
                onCommand(AvaCommand.OpenSettings)
                onDismiss()
            }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}

/**
 * Helper: Get title for current level
 */
private fun getLevelTitle(level: String): String {
    return when (level) {
        "master" -> "AVA Commands"
        "chat" -> "Chat Commands"
        "teach" -> "Teach AVA"
        "models" -> "Model Management"
        "voice" -> "Voice & NLU"
        "settings" -> "Settings"
        else -> "AVA"
    }
}

/**
 * Helper: Get parent level for back navigation
 */
private fun getParentLevel(currentLevel: String): String {
    return when (currentLevel) {
        "chat", "teach", "models", "voice", "settings" -> "master"
        else -> "master"
    }
}
