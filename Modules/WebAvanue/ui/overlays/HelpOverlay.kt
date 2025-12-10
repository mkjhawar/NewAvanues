/**
 * HelpOverlay.kt - Help system overlay for WebAvanue browser command discovery
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: WebAvanue Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-09-03
 * Updated: 2025-12-10 - Moved from VoiceOSCore to WebAvanue module
 */
package com.augmentalis.webavanue.ui.overlays

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.webavanue.ui.utils.*

/**
 * Help overlay for WebAvanue browser voice command discovery
 * Shows available voice commands for controlling the browser
 */
class HelpOverlay(
    context: Context,
    private val onDismiss: () -> Unit
) : BaseOverlay(context, OverlayType.FULLSCREEN) {
    
    companion object {
        private const val TAG = "HelpOverlay"
    }
    
    private var _selectedCategory by mutableStateOf(HelpCategory.NAVIGATION)
    private var _searchQuery by mutableStateOf("")
    
    @Composable
    override fun OverlayContent() {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Adjust padding and width based on orientation
        val horizontalPadding = if (isLandscape) 12.dp else 24.dp
        val verticalPadding = if (isLandscape) 12.dp else 24.dp
        val contentWidth = if (isLandscape) 0.85f else 1f // 85% width in landscape

        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(contentWidth)
                        .fillMaxHeight()
                        .glassMorphism(
                            config = GlassMorphismConfig(
                                cornerRadius = if (isLandscape) 16.dp else 0.dp,
                                backgroundOpacity = 0.95f,
                                borderOpacity = if (isLandscape) 0.2f else 0f,
                                borderWidth = if (isLandscape) 1.dp else 0.dp,
                                tintColor = Color.Black,
                                tintOpacity = 0.8f
                            ),
                            depth = DepthLevel(1f)
                        )
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                ) {
                    // Header
                    HelpHeader(
                        onDismiss = onDismiss,
                        isLandscape = isLandscape
                    )

                    Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 24.dp))

                    // Category selection
                    CategorySelector(
                        selectedCategory = _selectedCategory,
                        onCategorySelected = { _selectedCategory = it }
                    )

                    Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 24.dp))

                    // Command list
                    CommandList(
                        category = _selectedCategory,
                        searchQuery = _searchQuery,
                        isLandscape = isLandscape
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpHeader(
    onDismiss: () -> Unit,
    isLandscape: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Voice Commands",
                style = if (isLandscape) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.headlineMedium
                },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Say any command to control your device",
                style = if (isLandscape) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .glassMorphism(
                    config = GlassMorphismDefaults.Button.copy(
                        tintColor = Color(0xFFFF5722)
                    ),
                    depth = DepthLevel(0.4f)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Help",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: HelpCategory,
    onCategorySelected: (HelpCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(HelpCategory.values()) { category ->
            CategoryChip(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: HelpCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .glassMorphism(
                config = GlassMorphismConfig(
                    cornerRadius = 20.dp,
                    backgroundOpacity = if (isSelected) 0.25f else 0.1f,
                    borderOpacity = if (isSelected) 0.4f else 0.2f,
                    borderWidth = 1.dp,
                    tintColor = category.color,
                    tintOpacity = if (isSelected) 0.3f else 0.15f
                ),
                depth = DepthLevel(if (isSelected) 0.8f else 0.4f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
            )
            
            Text(
                text = category.title,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CommandList(
    category: HelpCategory,
    @Suppress("UNUSED_PARAMETER") searchQuery: String,
    isLandscape: Boolean = false
) {
    val commands = remember(category) { getCommandsForCategory(category) }
    val spacing = if (isLandscape) 8.dp else 12.dp

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        items(commands) { commandGroup ->
            CommandGroupCard(
                commandGroup = commandGroup,
                isLandscape = isLandscape
            )
        }
    }
}

@Composable
private fun CommandGroupCard(
    commandGroup: CommandGroup,
    isLandscape: Boolean = false
) {
    val cardPadding = if (isLandscape) 10.dp else 16.dp
    val itemSpacing = if (isLandscape) 4.dp else 6.dp
    val titleSpacing = if (isLandscape) 6.dp else 8.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassMorphism(
                config = GlassMorphismDefaults.Card,
                depth = DepthLevel(0.6f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(cardPadding)
        ) {
            Text(
                text = commandGroup.title,
                style = if (isLandscape) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(titleSpacing))

            commandGroup.commands.forEach { command ->
                CommandItem(
                    command = command,
                    isLandscape = isLandscape
                )
                Spacer(modifier = Modifier.height(itemSpacing))
            }
        }
    }
}

@Composable
private fun CommandItem(
    command: VoiceCommand,
    isLandscape: Boolean = false
) {
    val phraseFontSize = if (isLandscape) 13.sp else 14.sp
    val descriptionFontSize = if (isLandscape) 11.sp else 12.sp
    val moreFontSize = if (isLandscape) 9.sp else 10.sp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "\"${command.phrase}\"",
                color = Color(0xFF4CAF50),
                fontSize = phraseFontSize,
                fontWeight = FontWeight.Medium
            )

            if (command.description.isNotEmpty()) {
                Text(
                    text = command.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = descriptionFontSize
                )
            }
        }

        if (command.alternatives.isNotEmpty()) {
            Text(
                text = "+${command.alternatives.size} more",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = moreFontSize
            )
        }
    }
}

/**
 * Help categories for organizing commands
 */
enum class HelpCategory(
    val title: String,
    val icon: ImageVector,
    val color: Color
) {
    NAVIGATION("Navigation", Icons.Default.Navigation, Color(0xFF2196F3)),
    SELECTION("Selection", Icons.Default.TouchApp, Color(0xFF4CAF50)),
    SYSTEM("System", Icons.Default.Settings, Color(0xFF9C27B0)),
    APPS("Apps", Icons.Default.Apps, Color(0xFFFF9800)),
    CURSOR("Cursor", Icons.Default.CenterFocusWeak, Color(0xFF00BCD4)),
    INPUT("Input", Icons.Default.Keyboard, Color(0xFF795548)),
    HELP("Help", Icons.AutoMirrored.Filled.Help, Color(0xFF607D8B))
}

/**
 * Command group for organizing related commands
 */
data class CommandGroup(
    val title: String,
    val commands: List<VoiceCommand>
)

/**
 * Individual voice command
 */
data class VoiceCommand(
    val phrase: String,
    val description: String,
    val alternatives: List<String> = emptyList()
)

/**
 * Get commands for a specific category
 */
private fun getCommandsForCategory(category: HelpCategory): List<CommandGroup> {
    return when (category) {
        HelpCategory.NAVIGATION -> listOf(
            CommandGroup("Basic Navigation", listOf(
                VoiceCommand("go back", "Navigate to previous screen", listOf("back", "previous")),
                VoiceCommand("go home", "Go to home screen", listOf("home", "main screen")),
                VoiceCommand("scroll up", "Scroll up on current screen"),
                VoiceCommand("scroll down", "Scroll down on current screen"),
                VoiceCommand("page up", "Go to previous page"),
                VoiceCommand("page down", "Go to next page")
            ))
        )
        
        HelpCategory.SELECTION -> listOf(
            CommandGroup("Element Selection", listOf(
                VoiceCommand("show numbers", "Display numbered overlays on elements"),
                VoiceCommand("click number [1-9]", "Click on numbered element"),
                VoiceCommand("select text", "Enter text selection mode"),
                VoiceCommand("select all", "Select all text or content")
            ))
        )
        
        HelpCategory.SYSTEM -> listOf(
            CommandGroup("System Controls", listOf(
                VoiceCommand("open notifications", "Open notification panel"),
                VoiceCommand("open settings", "Open system settings"),
                VoiceCommand("volume up", "Increase volume"),
                VoiceCommand("volume down", "Decrease volume"),
                VoiceCommand("take screenshot", "Capture screen")
            ))
        )
        
        HelpCategory.APPS -> listOf(
            CommandGroup("App Management", listOf(
                VoiceCommand("open [app name]", "Launch specific application"),
                VoiceCommand("switch apps", "Show recent applications"),
                VoiceCommand("close app", "Close current application"),
                VoiceCommand("minimize app", "Minimize current application")
            ))
        )
        
        HelpCategory.CURSOR -> listOf(
            CommandGroup("Cursor Control", listOf(
                VoiceCommand("show cursor", "Display cursor overlay"),
                VoiceCommand("hide cursor", "Hide cursor overlay"),
                VoiceCommand("cursor up", "Move cursor up"),
                VoiceCommand("cursor down", "Move cursor down"),
                VoiceCommand("cursor left", "Move cursor left"),
                VoiceCommand("cursor right", "Move cursor right"),
                VoiceCommand("cursor click", "Click at cursor position")
            ))
        )
        
        HelpCategory.INPUT -> listOf(
            CommandGroup("Text Input", listOf(
                VoiceCommand("type [text]", "Enter text using voice"),
                VoiceCommand("delete", "Delete selected text or character"),
                VoiceCommand("cut", "Cut selected text"),
                VoiceCommand("copy", "Copy selected text"),
                VoiceCommand("paste", "Paste from clipboard")
            ))
        )
        
        HelpCategory.HELP -> listOf(
            CommandGroup("Help System", listOf(
                VoiceCommand("show help", "Display this help overlay"),
                VoiceCommand("hide help", "Close help overlay"),
                VoiceCommand("what can I say", "Show available commands"),
                VoiceCommand("repeat command", "Repeat last recognized command")
            ))
        )
    }
}