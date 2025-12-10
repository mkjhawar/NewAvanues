package com.augmentalis.Avanues.web.universal.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.OceanTheme
import com.augmentalis.Avanues.web.universal.presentation.design.OceanComponents
import com.augmentalis.Avanues.web.universal.presentation.design.OceanDesignTokens
import com.augmentalis.Avanues.web.universal.presentation.design.IconVariant
import com.augmentalis.webavanue.domain.model.BrowserSettings

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
            CommandItem("go back", "Navigate back", Icons.Default.ArrowBack),
            CommandItem("go forward", "Navigate forward", Icons.Default.ArrowForward),
            CommandItem("go home", "Go to home page", Icons.Default.Home),
            CommandItem("refresh", "Reload page", Icons.Default.Refresh),
            CommandItem("go to [url]", "Navigate to URL", Icons.Default.Link)
        )
    ),
    SCROLLING(
        title = "Scrolling",
        icon = Icons.Default.Swipe,
        commands = listOf(
            CommandItem("scroll up", "Scroll page up", Icons.Default.ArrowUpward),
            CommandItem("scroll down", "Scroll page down", Icons.Default.ArrowDownward),
            CommandItem("scroll to top", "Scroll to page top", Icons.Default.VerticalAlignTop),
            CommandItem("scroll to bottom", "Scroll to page bottom", Icons.Default.VerticalAlignBottom),
            CommandItem("freeze scroll", "Toggle scroll freeze", Icons.Default.Lock)
        )
    ),
    TABS(
        title = "Tabs",
        icon = Icons.Default.Tab,
        commands = listOf(
            CommandItem("new tab", "Open new tab", Icons.Default.Add),
            CommandItem("close tab", "Close current tab", Icons.Default.Close),
            CommandItem("next tab", "Switch to next tab", Icons.Default.NavigateNext),
            CommandItem("previous tab", "Switch to previous tab", Icons.Default.NavigateBefore),
            CommandItem("show tabs", "Show 3D tabs view", Icons.Default.Dashboard)
        )
    ),
    ZOOM(
        title = "Zoom",
        icon = Icons.Default.ZoomIn,
        commands = listOf(
            CommandItem("zoom in", "Increase zoom level", Icons.Default.ZoomIn),
            CommandItem("zoom out", "Decrease zoom level", Icons.Default.ZoomOut),
            CommandItem("reset zoom", "Reset to default zoom", Icons.Default.ZoomOutMap)
        )
    ),
    MODES(
        title = "Modes",
        icon = Icons.Default.DesktopWindows,
        commands = listOf(
            CommandItem("desktop mode", "Switch to desktop mode", Icons.Default.DesktopWindows),
            CommandItem("mobile mode", "Switch to mobile mode", Icons.Default.PhoneAndroid)
        )
    ),
    FEATURES(
        title = "Features",
        icon = Icons.Default.Star,
        commands = listOf(
            CommandItem("show favorites", "Show favorites shelf", Icons.Default.Favorite),
            CommandItem("bookmark this", "Bookmark current page", Icons.Default.BookmarkAdd),
            CommandItem("open bookmarks", "Open bookmarks list", Icons.Default.Bookmarks),
            CommandItem("downloads", "Open downloads", Icons.Default.Download),
            CommandItem("history", "Open history", Icons.Default.History),
            CommandItem("settings", "Open settings", Icons.Default.Settings),
            CommandItem("search [query]", "Search for query", Icons.Default.Search),
            CommandItem("show help", "Show voice commands", Icons.Default.HelpOutline)
        )
    )
}

/**
 * Individual command item
 */
data class CommandItem(
    val command: String,
    val description: String,
    val icon: ImageVector
)

/**
 * VoiceCommandsDialog - Interactive voice commands reference
 *
 * FR-011: Voice Commands UI
 * - Shows all available voice commands grouped by category
 * - Each category has a submenu with specific commands
 * - Commands are clickable and execute when tapped
 * - Auto-dismiss for action commands, stay open for input commands
 * - Voice navigation: Say category name to view its commands
 * - Say "back" to return to categories view
 *
 * @param settings Browser settings for auto-close configuration
 * @param onDismiss Callback when dialog is dismissed
 * @param onVoiceCommand Optional voice command text for navigation
 * @param onCommandExecute Callback to execute a command (command text) -> should dismiss
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandsDialog(
    settings: BrowserSettings,
    onDismiss: () -> Unit,
    onVoiceCommand: String? = null,
    onCommandExecute: (String) -> Boolean = { false }
) {
    var selectedCategory by remember { mutableStateOf<VoiceCommandCategory?>(null) }

    // Handle voice-based navigation
    LaunchedEffect(onVoiceCommand) {
        onVoiceCommand?.lowercase()?.let { command ->
            when {
                command == "back" -> selectedCategory = null
                command.contains("navigation") -> selectedCategory = VoiceCommandCategory.NAVIGATION
                command.contains("scroll") -> selectedCategory = VoiceCommandCategory.SCROLLING
                command.contains("tab") -> selectedCategory = VoiceCommandCategory.TABS
                command.contains("zoom") -> selectedCategory = VoiceCommandCategory.ZOOM
                command.contains("mode") -> selectedCategory = VoiceCommandCategory.MODES
                command.contains("feature") -> selectedCategory = VoiceCommandCategory.FEATURES
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints {
            // Detect landscape orientation
            val isLandscape = maxWidth > maxHeight

            // Optimize for landscape: Material Design compliant sizing
            // Portrait: 90% × 85% - users expect dominant dialog
            // Landscape: 80% × 80% - provides breathing room, clear tap-outside area
            val dialogWidth = if (isLandscape) 0.80f else 0.90f
            val dialogHeight = if (isLandscape) 0.80f else 0.85f
            val contentPadding = if (isLandscape) OceanDesignTokens.Spacing.md else OceanDesignTokens.Spacing.xl
            val headerSpacing = if (isLandscape) OceanDesignTokens.Spacing.md else OceanDesignTokens.Spacing.xl

            Surface(
                modifier = Modifier
                    .fillMaxWidth(dialogWidth)
                    .fillMaxHeight(dialogHeight),
                color = OceanDesignTokens.Surface.elevated,
                shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.xxl),
                tonalElevation = OceanDesignTokens.Elevation.lg,
                shadowElevation = OceanDesignTokens.Elevation.xxl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                ) {
                    // Current category snapshot
                    val currentCategory = selectedCategory

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentCategory?.title ?: "Voice Commands",
                            style = if (isLandscape) {
                                MaterialTheme.typography.titleLarge
                            } else {
                                MaterialTheme.typography.headlineSmall
                            },
                            color = OceanDesignTokens.Text.primary
                        )

                        OceanComponents.IconButton(onClick = onDismiss) {
                            OceanComponents.Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                variant = IconVariant.Secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(headerSpacing))

                    // Content with navigation animation
                    Box(modifier = Modifier.weight(1f)) {
                        if (currentCategory == null) {
                            CategoriesView(
                                isLandscape = isLandscape,
                                onCategorySelected = { category ->
                                    selectedCategory = category
                                }
                            )
                        } else {
                            CommandsView(
                                category = currentCategory,
                                isLandscape = isLandscape,
                                onBack = { selectedCategory = null },
                                onCommandClick = { command ->
                                    val shouldDismiss = onCommandExecute(command)
                                    if (shouldDismiss) {
                                        onDismiss()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Categories view - Shows all command categories
 * Adapts to landscape with multi-column grid layout
 */
@Composable
private fun CategoriesView(
    isLandscape: Boolean,
    onCategorySelected: (VoiceCommandCategory) -> Unit
) {
    val columns = if (isLandscape) 3 else 1
    val horizontalSpacing = if (isLandscape) 6.dp else 8.dp
    val verticalSpacing = if (isLandscape) 8.dp else 12.dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        modifier = Modifier.fillMaxSize()
    ) {
        items(VoiceCommandCategory.entries.toList()) { category ->
            CategoryButton(
                category = category,
                isLandscape = isLandscape,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

/**
 * Category button - Shows category with icon and arrow
 */
@Composable
private fun CategoryButton(
    category: VoiceCommandCategory,
    isLandscape: Boolean,
    onClick: () -> Unit
) {
    val buttonHeight = if (isLandscape) 56.dp else 64.dp
    val iconSize = if (isLandscape) 24.dp else 28.dp
    val textStyle = if (isLandscape) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
    val iconSpacing = if (isLandscape) OceanDesignTokens.Spacing.md else OceanDesignTokens.Spacing.lg

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = OceanDesignTokens.Surface.elevated,
            contentColor = OceanDesignTokens.Text.primary
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(iconSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OceanComponents.Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    variant = IconVariant.Primary,
                    modifier = Modifier.size(iconSize)
                )

                Text(
                    text = category.title,
                    style = textStyle
                )
            }

            OceanComponents.Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View commands",
                variant = IconVariant.Secondary
            )
        }
    }
}

/**
 * Commands view - Shows all commands for a category
 * Adapts to landscape with multi-column grid layout
 * Commands are clickable and execute when tapped
 */
@Composable
private fun CommandsView(
    category: VoiceCommandCategory,
    isLandscape: Boolean,
    onBack: () -> Unit,
    onCommandClick: (String) -> Unit
) {
    val backButtonPadding = if (isLandscape) OceanDesignTokens.Spacing.md else OceanDesignTokens.Spacing.lg
    val columns = if (isLandscape) 2 else 1
    val horizontalSpacing = if (isLandscape) 6.dp else 8.dp
    val verticalSpacing = if (isLandscape) 8.dp else 12.dp

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Back button
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(bottom = backButtonPadding)
        ) {
            OceanComponents.Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                variant = IconVariant.Primary,
                modifier = Modifier.size(if (isLandscape) 18.dp else 20.dp)
            )
            Spacer(modifier = Modifier.width(OceanDesignTokens.Spacing.sm))
            Text(
                text = "Back to categories",
                style = if (isLandscape) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
            )
        }

        // Commands grid - adaptive columns for landscape
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            modifier = Modifier.fillMaxSize()
        ) {
            items(category.commands) { command ->
                CommandItemCard(
                    command = command,
                    isLandscape = isLandscape,
                    onClick = { onCommandClick(command.command) }
                )
            }
        }
    }
}

/**
 * Command item card - Shows individual command with description
 * Clickable - tapping executes the command
 */
@Composable
private fun CommandItemCard(
    command: CommandItem,
    isLandscape: Boolean,
    onClick: () -> Unit
) {
    val cardPadding = if (isLandscape) OceanDesignTokens.Spacing.sm else OceanDesignTokens.Spacing.md
    val itemSpacing = if (isLandscape) OceanDesignTokens.Spacing.sm else OceanDesignTokens.Spacing.md
    val badgeMinWidth = if (isLandscape) 90.dp else 100.dp
    val commandTextStyle = if (isLandscape) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    val descriptionTextStyle = if (isLandscape) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = OceanDesignTokens.Surface.elevated,
        shape = MaterialTheme.shapes.small,
        tonalElevation = OceanDesignTokens.Elevation.sm,
        shadowElevation = OceanDesignTokens.Elevation.md
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Command badge - fixed minimum width for consistency
            Surface(
                color = OceanDesignTokens.Surface.primary,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.widthIn(min = badgeMinWidth)
            ) {
                Text(
                    text = command.command,
                    modifier = Modifier.padding(
                        horizontal = OceanDesignTokens.Spacing.sm,
                        vertical = if (isLandscape) 4.dp else OceanDesignTokens.Spacing.sm
                    ),
                    style = commandTextStyle,
                    color = OceanDesignTokens.Text.onPrimary,
                    maxLines = 1
                )
            }

            // Description
            Text(
                text = command.description,
                style = descriptionTextStyle,
                color = OceanDesignTokens.Text.secondary,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }
    }
}
