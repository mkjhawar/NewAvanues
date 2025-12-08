/**
 * MagicCommandOverlay - Voice Command Overlay for Magic System
 *
 * A zero-space, voice-first command UI pattern for Magic apps and plugins.
 *
 * Features:
 * - Maximum screen space (0dp when hidden)
 * - Full command discoverability via uniform grid
 * - Cascading navigation (Master → Categories → Commands)
 * - Responsive design (portrait/landscape adaptive)
 * - Generic event handling for any Magic app/plugin
 *
 * Usage:
 * ```kotlin
 * var showCommands by remember { mutableStateOf(false) }
 *
 * Box {
 *     // Your content
 *
 *     // Trigger FAB
 *     FloatingActionButton(onClick = { showCommands = true }) {
 *         Icon(Icons.Default.Mic, "Voice Commands")
 *     }
 *
 *     // Command overlay
 *     MagicCommandOverlay(
 *         visible = showCommands,
 *         commandCategories = yourCategories,
 *         onCommand = { command -> handleCommand(command) },
 *         onDismiss = { showCommands = false }
 *     )
 * }
 * ```
 *
 * @see docs/templates/commandoverlay/Guide-Voice-Command-Overlay-Pattern.md
 *
 * Created: 2025-11-08
 * Author: Manoj Jhawar, manoj@ideahq.net
 */

package com.augmentalis.avanues.avamagic.components.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Voice command data structure
 *
 * @param icon Icon emoji or text (e.g., "📝", "🔍")
 * @param label Command label shown to user
 * @param action Callback when command is executed
 */
data class MagicCommand(
    val icon: String,
    val label: String,
    val action: () -> Unit
)

/**
 * Command category data structure
 *
 * @param id Unique category identifier (e.g., "files", "edit")
 * @param icon Category icon emoji or text
 * @param label Category label shown in master view
 * @param commands List of commands in this category
 */
data class MagicCommandCategory(
    val id: String,
    val icon: String,
    val label: String,
    val commands: List<MagicCommand>
)

/**
 * Voice Command Overlay for Magic System
 *
 * Provides zero-space voice command UI with:
 * - Cascading navigation (Master → Categories → Commands)
 * - Uniform grid layout (all commands visible, no scrolling)
 * - Slide-up animation
 * - Voice toggle button
 * - Responsive portrait/landscape support
 *
 * @param visible Whether overlay is visible
 * @param commandCategories List of command categories to display
 * @param onCommand Callback when a command is executed
 * @param onDismiss Callback when overlay is dismissed
 * @param modifier Compose modifier
 * @param enableVoiceButton Whether to show voice toggle button (default: true)
 * @param onVoiceToggle Callback when voice button is toggled (optional)
 */
@Composable
fun MagicCommandOverlay(
    visible: Boolean,
    commandCategories: List<MagicCommandCategory>,
    onCommand: (MagicCommand) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    enableVoiceButton: Boolean = true,
    onVoiceToggle: ((Boolean) -> Unit)? = null
) {
    // ============================================
    // STATE MANAGEMENT
    // ============================================

    var currentLevel by remember { mutableStateOf("master") }
    var isListening by remember { mutableStateOf(false) }

    // Reset to master when overlay becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            currentLevel = "master"
            isListening = false
        }
    }

    // ============================================
    // ANIMATION
    // ============================================

    val slideOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        label = "magic_overlay_slide"
    )

    // ============================================
    // UI RENDERING
    // ============================================

    // Only render when visible or animating
    if (slideOffset < 1f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .offset(y = (slideOffset * 1000).dp)
        ) {
            // Semi-transparent background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable(enabled = false) { }
            )

            // Command content at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                // Header with navigation
                MagicCommandHeader(
                    currentLevel = currentLevel,
                    isListening = isListening,
                    categories = commandCategories,
                    enableVoiceButton = enableVoiceButton,
                    onBack = { currentLevel = "master" },
                    onVoiceToggle = {
                        isListening = !isListening
                        onVoiceToggle?.invoke(isListening)
                    },
                    onDismiss = onDismiss
                )

                // Command content (switches based on level)
                when (currentLevel) {
                    "master" -> MagicMasterCommands(
                        categories = commandCategories,
                        onNavigate = { categoryId -> currentLevel = categoryId }
                    )
                    else -> {
                        // Find and display category commands
                        commandCategories.find { it.id == currentLevel }?.let { category ->
                            MagicCategoryCommands(
                                category = category,
                                onCommand = { command ->
                                    onCommand(command)
                                    command.action()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// HEADER COMPONENT
// ============================================

/**
 * Command header with navigation controls
 */
@Composable
private fun MagicCommandHeader(
    currentLevel: String,
    isListening: Boolean,
    categories: List<MagicCommandCategory>,
    enableVoiceButton: Boolean,
    onBack: () -> Unit,
    onVoiceToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showBackButton = currentLevel != "master"
    val title = when (currentLevel) {
        "master" -> "Magic Commands"
        else -> categories.find { it.id == currentLevel }?.label ?: "Commands"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back button (conditional)
            if (showBackButton) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to main menu",
                        tint = Color.White
                    )
                }
            }

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // Voice button (optional)
            if (enableVoiceButton) {
                IconButton(
                    onClick = onVoiceToggle,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isListening) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop listening" else "Start voice input",
                        tint = Color.White
                    )
                }
            }

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close commands",
                    tint = Color.White
                )
            }
        }
    }
}

// ============================================
// MASTER COMMANDS (CATEGORIES)
// ============================================

/**
 * Master command view showing all categories
 */
@Composable
private fun MagicMasterCommands(
    categories: List<MagicCommandCategory>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Responsive sizing
    val (buttonMinSize, buttonHeight) = if (isLandscape) {
        90.dp to 60.dp
    } else {
        110.dp to 70.dp
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = buttonMinSize),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false // All commands visible at once
    ) {
        items(categories) { category ->
            MagicCommandChip(
                icon = category.icon,
                label = category.label,
                height = buttonHeight,
                onClick = { onNavigate(category.id) }
            )
        }
    }
}

// ============================================
// CATEGORY COMMANDS
// ============================================

/**
 * Category command view showing specific commands
 */
@Composable
private fun MagicCategoryCommands(
    category: MagicCommandCategory,
    onCommand: (MagicCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    val (buttonMinSize, buttonHeight) = if (isLandscape) {
        90.dp to 60.dp
    } else {
        110.dp to 70.dp
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = buttonMinSize),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(category.commands) { command ->
            MagicCommandChip(
                icon = command.icon,
                label = command.label,
                height = buttonHeight,
                onClick = { onCommand(command) }
            )
        }
    }
}

// ============================================
// COMMAND CHIP (BUTTON)
// ============================================

/**
 * Individual command button chip
 */
@Composable
private fun MagicCommandChip(
    icon: String,
    label: String,
    height: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Text(
                text = icon,
                fontSize = 24.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Label
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
