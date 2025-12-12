package com.augmentalis.Avanues.web.universal.voice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.OceanTheme
import com.augmentalis.Avanues.web.universal.presentation.design.OceanDesignTokens

/**
 * Voice command category
 */
enum class VoiceCommandCategory(
    val title: String,
    val icon: ImageVector,
    val commands: List<CommandItem>
) {
    NAVIGATION(
        title = "Navigation",
        icon = Icons.Default.Navigation,
        commands = listOf(
            CommandItem("go back", "Navigate back"),
            CommandItem("go forward", "Navigate forward"),
            CommandItem("go home", "Go to home page"),
            CommandItem("refresh", "Reload page"),
            CommandItem("go to [url]", "Navigate to URL")
        )
    ),
    SCROLLING(
        title = "Scrolling",
        icon = Icons.Default.UnfoldMore,
        commands = listOf(
            CommandItem("scroll up", "Scroll up"),
            CommandItem("scroll down", "Scroll down"),
            CommandItem("scroll to top", "Scroll to page top"),
            CommandItem("scroll to bottom", "Scroll to page bottom"),
            CommandItem("page up", "Scroll one page up"),
            CommandItem("page down", "Scroll one page down")
        )
    ),
    TABS(
        title = "Tabs",
        icon = Icons.Default.Tab,
        commands = listOf(
            CommandItem("new tab", "Open new tab"),
            CommandItem("close tab", "Close current tab"),
            CommandItem("next tab", "Switch to next tab"),
            CommandItem("previous tab", "Switch to previous tab"),
            CommandItem("reopen tab", "Reopen closed tab")
        )
    ),
    ZOOM(
        title = "Zoom",
        icon = Icons.Default.ZoomIn,
        commands = listOf(
            CommandItem("zoom in", "Zoom in"),
            CommandItem("zoom out", "Zoom out"),
            CommandItem("reset zoom", "Reset zoom to 100%")
        )
    ),
    MODES(
        title = "Modes",
        icon = Icons.Default.Monitor,
        commands = listOf(
            CommandItem("desktop mode", "Enable desktop mode"),
            CommandItem("mobile mode", "Enable mobile mode"),
            CommandItem("reader mode", "Enable reader mode"),
            CommandItem("fullscreen", "Enter fullscreen")
        )
    ),
    FEATURES(
        title = "Features",
        icon = Icons.Default.Star,
        commands = listOf(
            CommandItem("bookmark", "Add to bookmarks"),
            CommandItem("history", "Show history"),
            CommandItem("downloads", "Show downloads"),
            CommandItem("settings", "Open settings"),
            CommandItem("help", "Show help")
        )
    )
}

/**
 * Command item data
 */
data class CommandItem(
    val command: String,
    val description: String
)

/**
 * Voice Commands Dialog
 * Shows available voice commands in a clean 2-column grid
 */
@Composable
fun VoiceCommandsDialog(
    onDismiss: () -> Unit,
    onCommandExecute: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<VoiceCommandCategory?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(min = 320.dp, max = 480.dp)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            color = OceanDesignTokens.Surface.default,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Commands",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OceanDesignTokens.Text.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OceanDesignTokens.Text.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                if (selectedCategory == null) {
                    // Categories grid - Always 2 columns
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(VoiceCommandCategory.entries.toList()) { category ->
                            CategoryButton(
                                category = category,
                                onClick = { selectedCategory = category }
                            )
                        }
                    }
                } else {
                    // Commands view with back button
                    CommandsView(
                        category = selectedCategory!!,
                        onBack = { selectedCategory = null },
                        onCommandClick = { command ->
                            onCommandExecute(command)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Category button - Clean, simple, fixed 2-column layout
 */
@Composable
private fun CategoryButton(
    category: VoiceCommandCategory,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = OceanDesignTokens.Surface.elevated,
            contentColor = OceanDesignTokens.Text.primary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = OceanDesignTokens.Icon.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * Commands view - Shows all commands in selected category
 */
@Composable
private fun CommandsView(
    category: VoiceCommandCategory,
    onBack: () -> Unit,
    onCommandClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Back button and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OceanDesignTokens.Text.secondary
                )
            }
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleLarge,
                color = OceanDesignTokens.Text.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Commands list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(category.commands) { command ->
                CommandItemCard(
                    command = command,
                    onClick = { onCommandClick(command.command) }
                )
            }
        }
    }
}

/**
 * Command item card - Clickable command with description
 */
@Composable
private fun CommandItemCard(
    command: CommandItem,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = OceanDesignTokens.Surface.elevated
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = command.command,
                    style = MaterialTheme.typography.titleSmall,
                    color = OceanDesignTokens.Icon.primary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = command.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanDesignTokens.Text.secondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = OceanDesignTokens.Text.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
