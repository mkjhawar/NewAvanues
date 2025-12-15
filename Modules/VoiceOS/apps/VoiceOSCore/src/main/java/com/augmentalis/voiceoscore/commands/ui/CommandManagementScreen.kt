/**
 * CommandManagementScreen.kt - Compose UI for command management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-15
 *
 * P3 Task 2.1: Version-aware command list UI with deprecation warnings
 */

package com.augmentalis.voiceoscore.commands.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Main command management screen.
 *
 * Displays all voice commands across all apps with:
 * - Version badges
 * - Deprecation warnings
 * - Confidence indicators
 * - Usage statistics
 *
 * @param uiState Current UI state from ViewModel
 * @param onCommandClick Callback when user clicks a command
 * @param onAppClick Callback when user clicks an app header
 * @param onRefresh Callback when user pulls to refresh
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandManagementScreen(
    uiState: CommandListUiState,
    onCommandClick: (Long) -> Unit = {},
    onAppClick: (String) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Commands") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading -> LoadingView()
                uiState.error != null -> ErrorView(uiState.error)
                uiState.commandGroups.isEmpty() -> EmptyView()
                else -> CommandGroupsList(
                    groups = uiState.commandGroups,
                    onCommandClick = onCommandClick,
                    onAppClick = onAppClick
                )
            }
        }
    }
}

/**
 * Loading state view with progress indicator.
 */
@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading commands...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Error state view with retry option.
 */
@Composable
private fun ErrorView(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Empty state view when no commands exist.
 */
@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VoiceChat,
                contentDescription = "No commands",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Commands Yet",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Voice commands will appear here after you use LearnApp to explore applications",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Lazy list of command groups with sticky headers.
 *
 * Groups commands by app with sticky app headers.
 * Each group shows:
 * - App name
 * - Command count
 * - Deprecated count (if > 0)
 * - List of commands
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommandGroupsList(
    groups: List<CommandGroupUiModel>,
    onCommandClick: (Long) -> Unit,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        groups.forEach { group ->
            stickyHeader(key = group.packageName) {
                AppHeader(
                    appName = group.appName,
                    commandCount = group.commands.size,
                    deprecatedCount = group.commands.count { it.isDeprecated },
                    onClick = { onAppClick(group.packageName) }
                )
            }

            items(
                items = group.commands,
                key = { it.id }
            ) { command ->
                CommandItem(
                    command = command,
                    onClick = { onCommandClick(command.id) }
                )
            }

            // Spacing between groups
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Sticky header for app group.
 *
 * Shows app name, command count, and deprecation badge.
 */
@Composable
private fun AppHeader(
    appName: String,
    commandCount: Int,
    deprecatedCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$commandCount command${if (commandCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            if (deprecatedCount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$deprecatedCount deprecated",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Command list item with version badge and deprecation warning.
 */
@Composable
private fun CommandItem(
    command: CommandUiModel,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (command.isDeprecated) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        } else {
            Color.Transparent
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Command text and usage count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Command text
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = command.commandText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (command.isUserApproved) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (command.isUserApproved) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "User approved",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Badges row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Version badge
                        CommandVersionBadge(
                            versionName = command.versionName,
                            isDeprecated = command.isDeprecated
                        )

                        // Confidence badge
                        ConfidenceBadge(confidence = command.confidencePercentage)

                        // Imminent deletion indicator
                        if (command.isDeletionImminent) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Soon",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Usage count
                Text(
                    text = "${command.usageCount}×",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Deprecation warning (if deprecated)
            if (command.isDeprecated) {
                Spacer(modifier = Modifier.height(10.dp))
                DeprecationWarning(daysUntilDeletion = command.daysUntilDeletion)
            }
        }
    }
}

/**
 * Version badge component.
 *
 * Shows command version with color-coding:
 * - Red: Deprecated command
 * - Blue: Active command
 */
@Composable
private fun CommandVersionBadge(
    versionName: String,
    isDeprecated: Boolean
) {
    val backgroundColor = if (isDeprecated) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = if (isDeprecated) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = "v$versionName",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

/**
 * Confidence badge component.
 *
 * Color-coded by confidence level:
 * - Green: ≥90% (high confidence)
 * - Orange: 70-89% (medium confidence)
 * - Red: <70% (low confidence)
 */
@Composable
private fun ConfidenceBadge(confidence: Int) {
    val color = when {
        confidence >= 90 -> Color(0xFF4CAF50) // Green
        confidence >= 70 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = "$confidence%",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * Deprecation warning with deletion countdown.
 *
 * Shows warning icon and days remaining until command is deleted.
 */
@Composable
private fun DeprecationWarning(daysUntilDeletion: Int?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Deprecated",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = when {
                    daysUntilDeletion == null -> "Deprecated command"
                    daysUntilDeletion == 0 -> "⚠️ Will be deleted soon"
                    daysUntilDeletion == 1 -> "Will be deleted in 1 day"
                    daysUntilDeletion < 7 -> "⚠️ Will be deleted in $daysUntilDeletion days"
                    else -> "Will be deleted in $daysUntilDeletion days"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
