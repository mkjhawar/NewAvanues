package com.augmentalis.Avanues.web.universal.voice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

            // Device-adaptive parameters for smart glasses, tablets, and phones
            val deviceType = remember { DeviceDetector.detectDeviceType() }
            val isTabletDevice = remember { DeviceDetector.isTablet() }
            val params = remember(deviceType, isTabletDevice) {
                DeviceAdaptiveParameters.forDeviceType(deviceType, isTabletDevice)
            }

            // Apply device-specific sizing
            val dialogWidth = if (isLandscape) params.dialogWidthLandscape else params.dialogWidthPortrait
            val dialogHeight = if (isLandscape) params.dialogHeightLandscape else params.dialogHeightPortrait
            val contentPadding = if (isLandscape) params.contentPaddingLandscape else params.contentPaddingPortrait
            val headerSpacing = if (isLandscape) params.headerSpacingLandscape else params.headerSpacingPortrait

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
                            style = MaterialTheme.typography.headlineSmall,
                            fontSize = params.headerTextSize,
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
                                params = params,
                                onCategorySelected = { category ->
                                    selectedCategory = category
                                }
                            )
                        } else {
                            CommandsView(
                                category = currentCategory,
                                isLandscape = isLandscape,
                                params = params,
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
    params: DeviceAdaptiveParameters,
    onCategorySelected: (VoiceCommandCategory) -> Unit
) {
    val columns = if (isLandscape) params.categoriesColumnsLandscape else 1

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(params.gridHorizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(params.gridVerticalSpacing),
        modifier = Modifier.fillMaxSize()
    ) {
        items(VoiceCommandCategory.entries.toList()) { category ->
            CategoryButton(
                category = category,
                params = params,
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
    params: DeviceAdaptiveParameters,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(params.categoryButtonHeight),
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
                horizontalArrangement = Arrangement.spacedBy(params.itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OceanComponents.Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    variant = IconVariant.Primary,
                    modifier = Modifier.size(params.iconSize)
                )

                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = params.categoryTextSize
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
    params: DeviceAdaptiveParameters,
    onBack: () -> Unit,
    onCommandClick: (String) -> Unit
) {
    val columns = if (isLandscape) params.commandsColumnsLandscape else 1

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Back button
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(bottom = params.itemSpacing)
        ) {
            OceanComponents.Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                variant = IconVariant.Primary,
                modifier = Modifier.size(params.backButtonIconSize)
            )
            Spacer(modifier = Modifier.width(OceanDesignTokens.Spacing.sm))
            Text(
                text = "Back to categories",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = params.categoryTextSize
            )
        }

        // Commands grid - adaptive columns for landscape
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(params.gridHorizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(params.gridVerticalSpacing),
            modifier = Modifier.fillMaxSize()
        ) {
            items(category.commands) { command ->
                CommandItemCard(
                    command = command,
                    params = params,
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
    params: DeviceAdaptiveParameters,
    onClick: () -> Unit
) {
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
                .padding(params.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(params.itemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Command badge - fixed minimum width for consistency
            Surface(
                color = OceanDesignTokens.Surface.primary,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.widthIn(min = params.badgeMinWidth)
            ) {
                Text(
                    text = command.command,
                    modifier = Modifier.padding(
                        horizontal = OceanDesignTokens.Spacing.sm,
                        vertical = 6.dp
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = params.commandTextSize,
                    color = OceanDesignTokens.Text.onPrimary,
                    maxLines = 1
                )
            }

            // Description
            Text(
                text = command.description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = params.descriptionTextSize,
                color = OceanDesignTokens.Text.secondary,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }
    }
}
